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
import org.jetbrains.annotations.NotNull;

import java.time.Year;
import java.util.List;

public class UsersController {
    @FXML
    private TableView<User> tblUsers;
    @FXML
    private TableColumn<User, String> colId;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colCreated;
    @FXML
    private TextField txtSearch;

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

        colId.setStyle("-fx-font-size: 17;");
        colName.setStyle("-fx-font-size: 17;");
        colUsername.setStyle("-fx-font-size: 17;");
        colRole.setStyle("-fx-font-size: 17;");
        colCreated.setStyle("-fx-font-size: 17;");

        loadPage();
    }

    @FXML
    public void onSearch() {
        offset = 0;
        loadPage();
    }

    @FXML
    public void onPrev() {
        if (offset - limit >= 0) {
            offset -= limit;
            loadPage();
        }
    }

    @FXML
    public void onNext() {
        offset += limit;
        loadPage();
    }

    @FXML
    public void onNew() {
        Dialog<User> dialog = userDialog(null);
        dialog.showAndWait().ifPresent(u -> {
            try {
                // Usar contraseña por defecto: "contraseña" + año actual
                String defaultPwd = "contraseña" + Year.now().getValue();
                userService.createUserFull(u.getString("name"), u.getString("username"), defaultPwd, u.getString("role"));
                loadPage();
                Utils.showAlert("Éxito", "Usuario creado. Contraseña por defecto: " + defaultPwd);
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando usuario: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo crear el usuario: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onEdit() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione un usuario");
            return;
        }
        Dialog<User> dialog = userDialog(selected);
        dialog.showAndWait().ifPresent(u -> {
            try {
                // No permitir cambiar la contraseña aquí: pasar null para newPassword
                userService.updateUser(selected.getInteger("id"), u.getString("name"), u.getString("username"), u.getString("role"), null);
                loadPage();
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando usuario: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo actualizar el usuario: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onDelete() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione un usuario");
            return;
        }
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

    @FXML
    public void onResetPassword() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione un usuario");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Restablecer la contraseña del usuario " + selected.getString("username") + " a la contraseña por defecto?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    String defaultPwd = "contraseña" + Year.now().getValue();
                    boolean ok = userService.resetPassword(selected.getInteger("id"), defaultPwd, true);
                    if (ok) {
                        Utils.showAlert("Éxito", "Contraseña restablecida a: " + defaultPwd);
                        loadPage();
                    } else {
                        Utils.showAlert("Error", "No se pudo restablecer la contraseña");
                    }
                } catch (Exception e) {
                    Logs.error(getClass(), "Error restableciendo contraseña: {}", e.getMessage());
                    Utils.showAlert("Error", "No se pudo restablecer la contraseña: " + e.getMessage());
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

    private @NotNull Dialog<User> userDialog(User base) {
        Dialog<User> d = new Dialog<>();
        String lblStyle = "-fx-font-weight: bold; -fx-font-size: 17;";
        String inputStyle = "-fx-font-size: 17;";

        // Hacer el diálogo redimensionable y darle un tamaño inicial un poco mayor
        d.setResizable(true);
        d.getDialogPane().setPrefSize(480, 300);

        d.setTitle(base == null ? "Nuevo usuario" : "Editar usuario");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField txtName = new TextField();
        txtName.setPromptText("Nombre");
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Usuario");

        // ComboBox con los roles asignables (excluyendo ROOT)
        ComboBox<Roles.Role> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll(Roles.getAssignableRoles());

        // Configurar cómo se muestra cada rol
        cmbRole.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Roles.Role role, boolean empty) {
                super.updateItem(role, empty);
                setText(empty || role == null ? null : role.getDisplayName());
            }
        });
        cmbRole.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Roles.Role role, boolean empty) {
                super.updateItem(role, empty);
                setText(empty || role == null ? null : role.getDisplayName());
            }
        });

        txtName.setStyle(inputStyle);
        txtUsername.setStyle(inputStyle);
        cmbRole.setStyle(inputStyle);

        if (base != null) {
            txtName.setText(base.getString("name"));
            txtUsername.setText(base.getString("username"));
            // Seleccionar el rol actual
            Roles.Role currentRole = Roles.Role.fromCode(base.getString("role"));
            cmbRole.getSelectionModel().select(currentRole);
        } else {
            // Por defecto seleccionar OPERATOR
            cmbRole.getSelectionModel().select(Roles.Role.OPERATOR);
        }

        Label lblName = new Label("Nombre");
        lblName.setStyle(lblStyle);
        Label lblUsername = new Label("Usuario");
        lblUsername.setStyle(lblStyle);
        Label lblRole = new Label("Rol");
        lblRole.setStyle(lblStyle);

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, lblName, txtName);
        gp.addRow(1, lblUsername, txtUsername);
        gp.addRow(2, lblRole, cmbRole);

        // Permitir que los controles ocupen el espacio horizontal disponible
        gp.setPrefWidth(440);
        GridPane.setHgrow(txtName, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtUsername, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(cmbRole, javafx.scene.layout.Priority.ALWAYS);

        d.getDialogPane().setContent(gp);

        Node btnSave = d.getDialogPane().lookupButton(save);
        BooleanBinding invalid = txtName.textProperty().isEmpty()
                .or(txtUsername.textProperty().isEmpty())
                .or(cmbRole.getSelectionModel().selectedItemProperty().isNull());
        btnSave.disableProperty().bind(invalid);

        d.setResultConverter(bt -> {
            if (bt == save) {
                User u = new User();
                u.set("name", txtName.getText());
                u.set("username", txtUsername.getText());
                u.set("role", cmbRole.getValue().getCode());
                return u;
            }
            return null;
        });

        return d;
    }
}
