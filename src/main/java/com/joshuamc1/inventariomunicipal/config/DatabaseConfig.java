package com.joshuamc1.inventariomunicipal.config;

import org.javalite.activejdbc.Base;
import com.joshuamc1.inventariomunicipal.core.Logs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final String ENVIRONMENT = System.getProperty("env", "development");

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró el archivo database.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando database.properties", e);
        }
    }

    public static void openConnection() {
        if (!Base.hasConnection()) {
            String prefix = ENVIRONMENT + ".";
            String driver = properties.getProperty(prefix + "driver");
            String url = properties.getProperty(prefix + "url");
            String user = properties.getProperty(prefix + "username", properties.getProperty(prefix + "user"));
            String password = properties.getProperty(prefix + "password");

            if (driver == null || driver.trim().isEmpty() || url == null || url.trim().isEmpty()) {
                throw new RuntimeException("Configuración incompleta para el entorno '" + ENVIRONMENT + "'. Se requieren las propiedades '" + prefix + "driver' y '" + prefix + "url'.");
            }

            try {
                Base.open(driver, url, user, password);
                Logs.info(DatabaseConfig.class, "Conexión abierta a la base de datos ({})", ENVIRONMENT);
            } catch (Exception e) {
                Logs.error(DatabaseConfig.class, "Error abriendo la conexión a la base de datos: {}", e.getMessage());
                throw new RuntimeException("Error abriendo la conexión a la base de datos: " + e.getMessage(), e);
            }
        }
    }

    public static void closeConnection() {
        if (Base.hasConnection()) {
            Base.close();
            Logs.info(DatabaseConfig.class, "Conexión cerrada ({})", ENVIRONMENT);
        }
    }
}
