package com.joshuamc1.inventariomunicipal.config;

import com.joshuamc1.inventariomunicipal.core.Logs;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Clase de configuración de la aplicación.
 * Centraliza variables que pueden cambiar por entorno, por ejemplo la carpeta segura y el nombre
 * del archivo de base de datos en producción.
 */
public class AppConfig {
    private static final String ENVIRONMENT = System.getProperty("env", "production");
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            // Registrar el error en lugar de imprimir la traza
            Logs.error(AppConfig.class, "Error cargando database.properties: {}", e.getMessage());
        }
    }

    /**
     * Devuelve el entorno actual (development|production...) según la propiedad del sistema.
     */
    public static String getEnvironment() {
        return ENVIRONMENT;
    }

    /**
     * Nombre de la carpeta segura dentro del home del usuario. Se puede sobrescribir con la propiedad
     * del sistema `app.secureDirName` o con `app.secureDirName` en database.properties.
     */
    public static String getSecureDirName() {
        return System.getProperty("app.secureDirName",
                properties.getProperty("app.secureDirName", ".inventario_municipal"));
    }

    /**
     * Nombre del archivo de base de datos en producción. Se puede sobrescribir con la propiedad
     * del sistema `app.productionDbName` o con `app.productionDbName` en database.properties.
     */
    public static String getProductionDbFileName() {
        return System.getProperty("app.productionDbName",
                properties.getProperty("app.productionDbName", "database.db"));
    }

    /**
     * Construye y devuelve la URL JDBC para la base de datos en producción. Crea la carpeta segura
     * dentro de `user.home` si no existe.
     */
    public static @NotNull String getProductionJdbcUrl() {
        String userHome = System.getProperty("user.home");
        String secureDir = getSecureDirName();
        Path dirPath = Paths.get(userHome, secureDir);
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta segura: " + dirPath, e);
        }

        Path dbPath = dirPath.resolve(getProductionDbFileName());
        return "jdbc:sqlite:" + dbPath;
    }

    /**
     * Obtiene la URL JDBC efectiva para el entorno solicitado. Para producción construye la ruta
     * segura; para otros entornos lee la propiedad `env.url` del archivo database.properties.
     */
    public static String getJdbcUrlForEnvironment(String env) {
        if ("production".equalsIgnoreCase(env)) {
            return getProductionJdbcUrl();
        }
        return properties.getProperty(env + ".url");
    }
}
