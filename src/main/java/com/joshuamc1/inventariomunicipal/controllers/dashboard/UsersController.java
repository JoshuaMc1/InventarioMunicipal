package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Roles;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.services.UserService;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;

public class UsersController {
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreated;
    @FXML private TextField txtSearch;

    private final UserService userService = new UserService();

    private int offset = 0;
    private final int limit = 50;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getInteger("id"))));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("name")));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("username")));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(Roles.getRoleFullName(data.getValue().getString("role"))));
        colCreated.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("created_at"))));

        loadPage();
    }

    @FXML public void onSearch() { offset = 0; loadPage(); }
    @FXML public void onPrev() { if (offset - limit >= 0) { offset -= limit; loadPage(); } }
    @FXML public void onNext() { offset += limit; loadPage(); }

    @FXML public void onNew() {
        Dialog<User> dialog = userDialog(null);
        dialog.showAndWait().ifPresent(u -> {
            try {
                String pwd = u.getString("password");
                if (pwd == null || pwd.isBlank()) { Utils.showAlert("Validación", "La contraseña es obligatoria"); return; }
                userService.createUserFull(u.getString("name"), u.getString("username"), pwd, u.getString("role"));
                loadPage();
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando usuario: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo crear el usuario: " + e.getMessage());
            }
        });
    }

    @FXML public void onEdit() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) { Utils.showAlert("Aviso", "Seleccione un usuario"); return; }
        Dialog<User> dialog = userDialog(selected);
        dialog.showAndWait().ifPresent(u -> {
            try {
                userService.updateUser(selected.getInteger("id"), u.getString("name"), u.getString("username"), u.getString("role"), u.getString("password"));
                loadPage();
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando usuario: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo actualizar el usuario: " + e.getMessage());
            }
        });
    }

    @FXML public void onDelete() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) { Utils.showAlert("Aviso", "Seleccione un usuario"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar usuario " + selected.getString("username") + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    userService.deleteUser(selected.getInteger("id"));
                    loadPage();
                } catch (Exception e) {
                    Logs.error(getClass(), "Error eliminando usuario: {}", e.getMessage());
                    Utils.showAlert("Error", "No se pudo eliminar el usuario: " + e.getMessage());
                }
            }
        });
    }

    private void loadPage() {
        new Thread(() -> {
            try {
                String q = txtSearch == null ? null : txtSearch.getText();
                List<User> users = userService.listPaged(offset, limit, q);
                Platform.runLater(() -> tblUsers.getItems().setAll(users));
            } catch (Throwable t) {
                Logs.error(getClass(), "Error listando usuarios: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar usuarios: " + t.getMessage()));
            }
        }).start();
    }

    private Dialog<User> userDialog(User base) {
        Dialog<User> d = new Dialog<>();
        d.setTitle(base == null ? "Nuevo usuario" : "Editar usuario");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField txtName = new TextField(); txtName.setPromptText("Nombre");
        TextField txtUsername = new TextField(); txtUsername.setPromptText("Usuario");
        PasswordField txtPassword = new PasswordField(); txtPassword.setPromptText(base == null ? "Contraseña" : "Contraseña (dejar vacío para no cambiar)");
        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("admin","operator","auditor","guest");

        if (base != null) {
            txtName.setText(base.getString("name"));
            txtUsername.setText(base.getString("username"));
            cmbRole.getSelectionModel().select(base.getString("role"));
        } else {
            cmbRole.getSelectionModel().select("operator");
        }

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new Label("Nombre"), txtName);
        gp.addRow(1, new Label("Usuario"), txtUsername);
        gp.addRow(2, new Label("Contraseña"), txtPassword);
        gp.addRow(3, new Label("Rol"), cmbRole);
        d.getDialogPane().setContent(gp);

        // Validación: nombre, usuario, rol requeridos; contraseña requerida sólo en nuevo
        Node btnSave = d.getDialogPane().lookupButton(save);
        BooleanBinding invalid = txtName.textProperty().isEmpty()
                .or(txtUsername.textProperty().isEmpty())
                .or(cmbRole.getSelectionModel().selectedItemProperty().isNull())
                .or((base == null) ? txtPassword.textProperty().isEmpty() : new javafx.beans.property.SimpleBooleanProperty(false));
        btnSave.disableProperty().bind(invalid);

        d.setResultConverter(bt -> {
            if (bt == save) {
                User u = new User();
                u.set("name", txtName.getText());
                u.set("username", txtUsername.getText());
                String pwd = txtPassword.getText(); if (pwd != null && !pwd.isBlank()) u.set("password", pwd);
                u.set("role", cmbRole.getValue());
                return u;
            }
            return null;
        });

        return d;
    }
}
