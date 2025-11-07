package com.joshuamc1.inventariomunicipal.controllers;

import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

//JavaFX Login Controller
public class LoginController {
    private final UserService userService = new UserService();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    // Método que se llama al hacer click en "Login"
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Username and password are required");
            return;
        }

        // Aquí se podría encriptar el password antes de pasar al servicio
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = userService.login(username, passwordHash);

        if (user != null) {
            // Login exitoso
            showAlert("Success", "Welcome " + user.getString("nombre"));
            openMainWindow(user);
            // Cerrar ventana de login
            ((Stage) txtUsername.getScene().getWindow()).close();
        } else {
            showAlert("Error", "Invalid username or password");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openMainWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Municipal Inventory System - Main");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open main window");
        }
    }
}
