package com.joshuamc1.inventariomunicipal;

import com.joshuamc1.inventariomunicipal.config.DatabaseConfig;
import com.joshuamc1.inventariomunicipal.config.InitialSetupDialog;
import com.joshuamc1.inventariomunicipal.config.MigrationRunner;
import com.joshuamc1.inventariomunicipal.config.MySQLConfigDialog;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Utils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    public void start(@NotNull Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Logs.error(App.class, "Uncaught exception in thread {}: {}", t.getName(), e.getMessage());
            Utils.showAlert("Error inesperado", "Se produjo un error inesperado: " + e.getMessage());
        });

        // Verificar si la base de datos MySQL está configurada
        if (!MySQLConfigDialog.isConfigured()) {
            Logs.info(App.class, "Base de datos no configurada. Mostrando diálogo de configuración MySQL...");
            boolean configured = MySQLConfigDialog.showConfigDialog();

            if (!configured) {
                Logs.info(App.class, "Configuración de MySQL cancelada por el usuario. Cerrando aplicación.");
                System.exit(0);
                return;
            }
        }

        // Abrir conexión a la base de datos
        DatabaseConfig.openConnection();

        // Asegurar esquema mínimo (idempotente) antes de proceder
        try {
            MigrationRunner.runMigrations();
        } catch (Exception ex) {
            Logs.error(App.class, "Error ejecutando migraciones en inicio: {}", ex.getMessage());
        }

        // Verificar si es necesario mostrar el diálogo de configuración inicial para crear usuario root
        if (!InitialSetupDialog.checkAndShowIfNeeded()) {
            // Si el usuario canceló la configuración inicial, cerrar la aplicación
            Logs.info(App.class, "Configuración inicial cancelada por el usuario. Cerrando aplicación.");
            DatabaseConfig.closeConnection();
            System.exit(0);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Inventario Municipal - Login");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));

            stage.setOnCloseRequest(evt -> {
                boolean ok = Utils.confirmAction("Confirmar salida", "¿Desea cerrar la aplicación?");
                if (!ok) {
                    evt.consume();
                }
            });

            stage.show();
            Logs.info(App.class, "Interfaz mostrada");
        } catch (IOException e) {
            Logs.error(App.class, "Error loading FXML file: {}", e.getMessage());
            Utils.showAlert("Error", "No se pudo cargar la interfaz principal: " + e.getMessage());
        }
    }

    public void stop() {
        DatabaseConfig.closeConnection();
        Logs.info(App.class, "App stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
