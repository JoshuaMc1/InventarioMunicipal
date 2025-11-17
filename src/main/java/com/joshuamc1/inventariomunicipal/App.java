package com.joshuamc1.inventariomunicipal;

import com.joshuamc1.inventariomunicipal.config.DatabaseConfig;
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

        DatabaseConfig.openConnection();

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
