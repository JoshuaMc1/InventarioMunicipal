package com.joshuamc1.inventariomunicipal.services;

import com.joshuamc1.inventariomunicipal.config.MySQLConfigManager;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.models.Backup;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para gestión de backups de la base de datos
 */
public class BackupService {

    private static final String BACKUP_DIR_NAME = "backups";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final AuditService auditService = new AuditService();

    /**
     * Obtiene el directorio de backups
     */
    public Path getBackupDirectory() {
        String userHome = System.getProperty("user.home");
        Path baseDir = Paths.get(userHome, ".inventario_municipal");
        Path backupDir = baseDir.resolve(BACKUP_DIR_NAME);

        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                Logs.info(getClass(), "Directorio de backups creado: {}", backupDir);
            }
        } catch (IOException e) {
            Logs.error(getClass(), "Error creando directorio de backups", e);
        }

        return backupDir;
    }

    /**
     * Crea un backup de la base de datos
     * @return Path del archivo de backup creado
     * @throws Exception si hay un error
     */
    public Path createBackup() throws Exception {
        if (!MySQLConfigManager.hasConfig()) {
            throw new Exception("Configuración de MySQL no encontrada");
        }

        // Obtener datos de configuración
        String host = extractHost(MySQLConfigManager.getJdbcUrl());
        String port = extractPort(MySQLConfigManager.getJdbcUrl());
        String database = extractDatabase(MySQLConfigManager.getJdbcUrl());
        String username = MySQLConfigManager.getUsername();
        String password = MySQLConfigManager.getPassword();

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String filename = String.format("backup_%s_%s.sql.gz", database, timestamp);
        Path backupFile = getBackupDirectory().resolve(filename);

        Logs.info(getClass(), "Iniciando backup: {}", filename);

        // Construir comando mysqldump con compresión
        ProcessBuilder pb = new ProcessBuilder();

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // Windows - con compresión usando powershell
            String passwordArg = password.isEmpty() ? "" : String.format("-p%s", password);

            String command = password.isEmpty()
                ? String.format("mysqldump -h%s -P%s -u%s %s | gzip > \"%s\"",
                    host, port, username, database, backupFile.toAbsolutePath().toString())
                : String.format("mysqldump -h%s -P%s -u%s %s %s | gzip > \"%s\"",
                    host, port, username, passwordArg, database, backupFile.toAbsolutePath().toString());

            pb.command("cmd.exe", "/c", command);
        } else {
            // Linux/Mac - con compresión usando gzip
            String passwordArg = password.isEmpty() ? "" : String.format("-p%s", password);

            String command = password.isEmpty()
                ? String.format("mysqldump -h%s -P%s -u%s %s | gzip > '%s'",
                    host, port, username, database, backupFile.toAbsolutePath().toString())
                : String.format("mysqldump -h%s -P%s -u%s %s %s | gzip > '%s'",
                    host, port, username, passwordArg, database, backupFile.toAbsolutePath().toString());

            pb.command("sh", "-c", command);
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Leer salida del proceso
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String error = output.toString();
            Logs.error(getClass(), "Error en mysqldump: {}", error);

            // Registrar backup fallido (abrir conexión para guardar en BD)
            try {
                com.joshuamc1.inventariomunicipal.config.DatabaseConfig.openConnection();
                registrarBackup(filename, backupFile.toString(), 0L, "FAILED", error);
                com.joshuamc1.inventariomunicipal.config.DatabaseConfig.closeConnection();
            } catch (Exception e) {
                Logs.error(getClass(), "Error registrando backup fallido en BD", e);
            }

            throw new Exception("Error ejecutando mysqldump: " + error);
        }

        // Verificar que el archivo fue creado
        if (!Files.exists(backupFile)) {
            throw new Exception("El archivo de backup no fue creado");
        }

        long fileSize = Files.size(backupFile);

        // Los archivos .gz pueden ser muy pequeños si la BD está vacía o tiene pocos datos
        // Validar que al menos tenga el header gzip (mínimo ~100 bytes)
        if (fileSize < 100) {
            Logs.warn(getClass(), "Backup muy pequeño ({}), posible error", fileSize);
            throw new Exception("Backup demasiado pequeño, posible error en la compresión");
        }

        // Registrar backup exitoso (abrir conexión para guardar en BD)
        com.joshuamc1.inventariomunicipal.config.DatabaseConfig.openConnection();
        try {
            registrarBackup(filename, backupFile.toString(), fileSize, "SUCCESS", null);

            // Auditoría
            auditService.logAction(Session.getCurrentUserId(), "create_backup", "backups", null,
                String.format("{\"filename\":\"%s\",\"size\":%d}", filename, fileSize));

            Logs.info(getClass(), "Backup completado exitosamente: {} ({} bytes)", filename, fileSize);
        } finally {
            com.joshuamc1.inventariomunicipal.config.DatabaseConfig.closeConnection();
        }

        return backupFile;
    }

    /**
     * Extrae el host de la URL JDBC
     */
    private @NotNull String extractHost(@NotNull String jdbcUrl) {
        int start = jdbcUrl.indexOf("//") + 2;
        int end = jdbcUrl.indexOf(":", start);
        return jdbcUrl.substring(start, end);
    }

    /**
     * Extrae el puerto de la URL JDBC
     */
    private @NotNull String extractPort(@NotNull String jdbcUrl) {
        int start = jdbcUrl.indexOf(":", jdbcUrl.indexOf("//")) + 1;
        int end = jdbcUrl.indexOf("/", start);
        return jdbcUrl.substring(start, end);
    }

    /**
     * Extrae el nombre de la base de datos de la URL JDBC
     */
    private @NotNull String extractDatabase(@NotNull String jdbcUrl) {
        int start = jdbcUrl.lastIndexOf("/") + 1;
        int end = jdbcUrl.indexOf("?");
        if (end == -1) end = jdbcUrl.length();
        return jdbcUrl.substring(start, end);
    }

    /**
     * Registra el backup en la base de datos
     */
    private void registrarBackup(String filename, String filepath, long size, String status, String errorMessage) {
        try {
            Logs.info(getClass(), "Registrando backup en BD: filename={}, size={} bytes, status={}",
                filename, size, status);

            Backup backup = new Backup();
            backup.set("filename", filename);
            backup.set("filepath", filepath);
            backup.set("size_bytes", Long.valueOf(size)); // Asegurar que es Long
            backup.set("usuario_id", Session.getCurrentUserId());
            backup.set("status", status);
            if (errorMessage != null) {
                backup.set("error_message", errorMessage);
            }

            boolean saved = backup.saveIt();

            if (saved) {
                Logs.info(getClass(), "Backup registrado exitosamente con ID: {}", backup.getId());
                // Verificar que se guardó correctamente
                Long savedSize = backup.getLong("size_bytes");
                Logs.info(getClass(), "Tamaño guardado verificado: {} bytes", savedSize);
            } else {
                Logs.error(getClass(), "No se pudo guardar el backup en BD");
            }
        } catch (Exception e) {
            Logs.error(getClass(), "Error registrando backup en BD", e);
        }
    }

    /**
     * Lista todos los backups
     */
    public List<Backup> listBackups() {
        return Backup.findAll().orderBy("created_at DESC");
    }

    /**
     * Lista backups paginados
     */
    public List<Backup> listBackupsPaged(int offset, int limit) {
        List<Backup> backups = Backup.findAll()
            .orderBy("created_at DESC")
            .limit(limit)
            .offset(offset);

        // Log para debug
        if (!backups.isEmpty()) {
            Backup first = backups.get(0);
            Logs.info(getClass(), "Primer backup cargado - ID: {}, filename: {}, size_bytes DB: {}",
                first.getId(), first.getString("filename"), first.get("size_bytes"));
        }

        return backups;
    }

    /**
     * Cuenta total de backups
     */
    public long countBackups() {
        return Backup.count();
    }

    /**
     * Elimina un backup (archivo y registro)
     */
    public void deleteBackup(Integer backupId) throws Exception {
        Backup backup = Backup.findById(backupId);
        if (backup == null) {
            throw new Exception("Backup no encontrado");
        }

        String filepath = backup.getString("filepath");

        // Eliminar archivo físico
        try {
            Path file = Paths.get(filepath);
            if (Files.exists(file)) {
                Files.delete(file);
                Logs.info(getClass(), "Archivo de backup eliminado: {}", filepath);
            }
        } catch (IOException e) {
            Logs.error(getClass(), "Error eliminando archivo de backup", e);
            throw new Exception("Error eliminando archivo: " + e.getMessage());
        }

        // Auditoría
        auditService.logAction(Session.getCurrentUserId(), "delete_backup", "backups", backupId,
            String.format("{\"filename\":\"%s\"}", backup.getString("filename")));

        // Eliminar registro
        backup.delete();
    }

    /**
     * Elimina backups antiguos (mantener solo los últimos N)
     */
    public int cleanOldBackups(int keepLast) throws Exception {
        List<Backup> allBackups = Backup.findAll().orderBy("created_at DESC");

        int deleted = 0;
        for (int i = keepLast; i < allBackups.size(); i++) {
            Backup backup = allBackups.get(i);
            try {
                deleteBackup(backup.getInteger("id"));
                deleted++;
            } catch (Exception e) {
                Logs.error(getClass(), "Error eliminando backup antiguo: {}", backup.getString("filename"), e);
            }
        }

        return deleted;
    }
}

