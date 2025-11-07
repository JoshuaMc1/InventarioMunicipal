package com.joshuamc1.inventariomunicipal;

import com.joshuamc1.inventariomunicipal.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static javafx.application.Application.launch;

public class App extends Application {
    public void start(@NotNull Stage stage) {
        DatabaseConfig.openConnection();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Municipal Inventory System");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        DatabaseConfig.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
