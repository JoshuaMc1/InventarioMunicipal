package com.joshuamc1.inventariomunicipal.config;

import org.javalite.activejdbc.Base;

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
            String user = properties.getProperty(prefix + "username");
            String password = properties.getProperty(prefix + "password");

            Base.open(driver, url, user, password);
            System.out.println("🔗 Conexión abierta a la base de datos (" + ENVIRONMENT + ")");
        }
    }

    public static void closeConnection() {
        if (Base.hasConnection()) {
            Base.close();
            System.out.println("❌ Conexión cerrada (" + ENVIRONMENT + ")");
        }
    }
}
