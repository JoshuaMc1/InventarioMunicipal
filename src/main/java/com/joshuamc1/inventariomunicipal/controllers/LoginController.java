package com.joshuamc1.inventariomunicipal.controllers;

import com.joshuamc1.inventariomunicipal.controllers.dashboard.MainController;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Session;
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

            // Establecer sesión antes de cualquier auditoría (p.ej., cambio de contraseña forzado)
            Session.setCurrentUser(user);

            // Leer must_change_password de forma robusta (MySQL puede devolver boolean o tinyint)
            boolean mustChange = false;
            try {
                Object mcp = user.get("must_change_password");
                if (mcp instanceof Number) {
                    mustChange = ((Number) mcp).intValue() != 0;
                } else if (mcp instanceof Boolean) {
                    mustChange = (Boolean) mcp;
                } else if (mcp != null) {
                    String s = mcp.toString().trim().toLowerCase();
                    mustChange = s.equals("1") || s.equals("true") || s.equals("t") || s.equals("yes");
                }
            } catch (Exception ignore) {
                mustChange = false;
            }

            // Si el usuario debe cambiar la contraseña, forzar el cambio antes de abrir el dashboard
            if (mustChange) {
                boolean changed = forceChangePassword(user);
                if (!changed) {
                    Utils.showAlert("Aviso", "Debe cambiar la contraseña para continuar");
                    // Limpiar sesión si no completó el cambio
                    Session.setCurrentUser(null);
                    return;
                }
            }

            Utils.showAlert("Success", "Bienvenido " + user.getString("name") + "!");
            openMainWindow(user);
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error during login: {}", e.getMessage());
            Utils.showAlert("Error", "Ocurrió un error durante el inicio de sesión");
        }
    }

    private boolean forceChangePassword(User user) {
        // Mostrar un diálogo simple pidiendo nueva contraseña y confirmación
        javafx.scene.control.Dialog<String> d = new javafx.scene.control.Dialog<>();
        d.setTitle("Cambiar contraseña");
        d.setHeaderText("Debe cambiar su contraseña antes de continuar");
        d.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        PasswordField pf1 = new PasswordField();
        pf1.setPromptText("Nueva contraseña");
        PasswordField pf2 = new PasswordField();
        pf2.setPromptText("Confirmar contraseña");

        javafx.scene.layout.GridPane gp = new javafx.scene.layout.GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new javafx.scene.control.Label("Nueva contraseña"), pf1);
        gp.addRow(1, new javafx.scene.control.Label("Confirmar contraseña"), pf2);

        javafx.scene.Node okButton = d.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setDisable(true);

        javafx.beans.binding.BooleanBinding invalid = pf1.textProperty().isEmpty().or(pf2.textProperty().isEmpty()).or(new javafx.beans.binding.BooleanBinding() {
            {
                bind(pf1.textProperty(), pf2.textProperty());
            }

            @Override
            protected boolean computeValue() {
                return !pf1.getText().equals(pf2.getText());
            }
        });
        okButton.disableProperty().bind(invalid);

        d.getDialogPane().setContent(gp);

        java.util.concurrent.atomic.AtomicBoolean result = new java.util.concurrent.atomic.AtomicBoolean(false);
        d.setResultConverter(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                return pf1.getText();
            }
            return null;
        });

        java.util.Optional<String> res = d.showAndWait();
        if (res.isPresent()) {
            String newPwd = res.get();
            try {
                // Actualizar contraseña directamente
                user.setPassword(newPwd);
                user.set("must_change_password", 0);
                user.saveIt();
                result.set(true);
            } catch (Exception e) {
                Logs.error(this.getClass(), "Error cambiando contraseña forzada: {}", e.getMessage());
                Utils.showAlert("Error", "Ocurrió un error al cambiar la contraseña: " + e.getMessage());
            }
        }

        return result.get();
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
            stage.setFullScreen(true);

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
            e.printStackTrace();
        }
    }
}
