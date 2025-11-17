package com.joshuamc1.inventariomunicipal.controllers;

import com.joshuamc1.inventariomunicipal.controllers.dashboard.MainController;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LoginController {
    private final UserService userService = new UserService();

    @FXML private TextField txtUsername;

    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Utils.showAlert("Error", "El usuario y la contraseña no pueden estar vacíos");
            return;
        }

        try {
            User user = userService.login(username, password);

            if (user == null) {
                Utils.showAlert("Error", "Usuario o contraseña incorrectos");
                Logs.error(this.getClass(), "Intento de inicio de sesión fallido para el usuario: " + username);
                return;
            }

            Utils.showAlert("Success", "Bienvenido " + user.getString("name") + "!");
            openMainWindow(user);
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error during login: {}", e.getMessage());
            Utils.showAlert("Error", "Ocurrió un error durante el inicio de sesión");
        }
    }

    private void openMainWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard/main.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Inventario Municipal - Dashboard");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
            MainController controller = loader.getController();
            controller.setLoggedUser(user);
            stage.setMaximized(true);

            stage.setOnCloseRequest(evt -> {
                boolean ok = Utils.confirmAction("Confirmar salida", "¿Desea cerrar la aplicación?");
                if (!ok) {
                    evt.consume();
                }
            });

            stage.show();

            Stage loginStage = (Stage) txtUsername.getScene().getWindow();
            loginStage.close();
        } catch (IOException e) {
            Utils.showAlert("Error", "No se pudo abrir la ventana principal");
            Logs.error(this.getClass(), "Failed to open main window: {}", e.getMessage());
        }
    }
}
