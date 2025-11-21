package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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

    // Botones de acción
    @FXML private Button btnNew;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnResetPassword;

    private final UserService userService = new UserService();

    // Paginación
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private ChoiceBox<Integer> choicePageSize;

    private int currentPage = 1;
    private int pageSize = 50;
    private int totalItems = 0;
    private int totalPages = 1;

    private final ObservableList<User> currentPageItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getInteger("id"))));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("name")));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("username")));
        colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRolNombre()));
        colCreated.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().get("created_at"))));

        colId.setStyle("-fx-font-size: 17;");
        colName.setStyle("-fx-font-size: 17;");
        colUsername.setStyle("-fx-font-size: 17;");
        colRole.setStyle("-fx-font-size: 17;");
        colCreated.setStyle("-fx-font-size: 17;");

        // Inicializar paginación
        choicePageSize.setItems(FXCollections.observableArrayList(10,25,50,100));
        choicePageSize.setValue(pageSize);
        choicePageSize.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                pageSize = newV;
                currentPage = 1;
                loadPageFromRepo(currentPage);
            }
        });
        btnPrev.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadPageFromRepo(currentPage);
            }
        });
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadPageFromRepo(currentPage);
            }
        });

        loadPageFromRepo(1);
        configurePermissions();
    }

    private void configurePermissions() {
        Logs.info(getClass(), "Configurando permisos de usuarios para: {}",
            com.joshuamc1.inventariomunicipal.core.Session.getCurrentUser().getString("username"));

        // Configurar visibilidad de botones según permisos
        if (btnNew != null) {
            com.joshuamc1.inventariomunicipal.core.PermissionHelper.hideIfNoPermission(btnNew, "USUARIOS", "CREATE");
        }
        if (btnEdit != null) {
            com.joshuamc1.inventariomunicipal.core.PermissionHelper.hideIfNoPermission(btnEdit, "USUARIOS", "UPDATE");
        }
        if (btnDelete != null) {
            com.joshuamc1.inventariomunicipal.core.PermissionHelper.hideIfNoPermission(btnDelete, "USUARIOS", "DELETE");
        }
        if (btnResetPassword != null) {
            com.joshuamc1.inventariomunicipal.core.PermissionHelper.hideIfNoPermission(btnResetPassword, "USUARIOS", "UPDATE");
        }

        Logs.info(getClass(), "Permisos de usuarios configurados");
    }

    @FXML
    public void onSearch() {
        currentPage = 1;
        loadPageFromRepo(currentPage);
    }

    @FXML
    public void onPrev() {
        if (currentPage > 1) {
            currentPage--;
            loadPageFromRepo(currentPage);
        }
    }

    @FXML
    public void onNext() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPageFromRepo(currentPage);
        }
    }

    @FXML
    public void onNew() {
        // Validar permiso USUARIOS.CREATE
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("USUARIOS", "CREATE")) {
            return;
        }

        Dialog<User> dialog = userDialog(null);
        dialog.showAndWait().ifPresent(u -> {
            try {
                // Usar contraseña por defecto: "contraseña" + año actual
                String defaultPwd = "contraseña" + Year.now().getValue();

                // Crear usuario directamente con el modelo
                User newUser = new User();
                newUser.set("name", u.getString("name"));
                newUser.set("username", u.getString("username"));
                newUser.setPassword(defaultPwd);
                newUser.set("rol_id", u.get("rol_id"));
                newUser.set("must_change_password", 1);
                newUser.saveIt();

                loadPageFromRepo(currentPage);
                Utils.showAlert("Éxito", "Usuario creado. Contraseña por defecto: " + defaultPwd);
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando usuario: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo crear el usuario: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onEdit() {
        // Validar permiso USUARIOS.UPDATE
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("USUARIOS", "UPDATE")) {
            return;
        }

        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione un usuario");
            return;
        }
        Dialog<User> dialog = userDialog(selected);
        dialog.showAndWait().ifPresent(u -> {
            try {
                // Actualizar directamente
                selected.set("name", u.getString("name"));
                selected.set("username", u.getString("username"));
                selected.set("rol_id", u.get("rol_id"));
                selected.saveIt();
                loadPageFromRepo(currentPage);
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando usuario: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo actualizar el usuario: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onDelete() {
        // Validar permiso USUARIOS.DELETE
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("USUARIOS", "DELETE")) {
            return;
        }

        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione un usuario");
            return;
        }

        // VALIDACIÓN ESPECIAL 1: No eliminar usuario ROOT
        com.joshuamc1.inventariomunicipal.models.Rol rol = selected.getRol();
        if (rol != null && "ROOT".equals(rol.getString("nombre"))) {
            Utils.showAlert("Error", "No se puede eliminar el usuario ROOT del sistema");
            Logs.warn(getClass(), "Intento de eliminar usuario ROOT bloqueado");
            return;
        }

        // VALIDACIÓN ESPECIAL 2: No eliminar el usuario actual (a sí mismo)
        User currentUser = com.joshuamc1.inventariomunicipal.core.Session.getCurrentUser();
        if (selected.getId().equals(currentUser.getId())) {
            Utils.showAlert("Error", "No puedes eliminar tu propio usuario");
            Logs.warn(getClass(), "Intento de eliminar usuario actual bloqueado");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar usuario " + selected.getString("username") + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    userService.deleteUser(selected.getInteger("id"));
                    loadPageFromRepo(currentPage);
                } catch (Exception e) {
                    Logs.error(getClass(), "Error eliminando usuario: {}", e.getMessage());
                    Utils.showAlert("Error", "No se pudo eliminar el usuario: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void onResetPassword() {
        // Validar permiso USUARIOS.UPDATE (resetear contraseña es una actualización)
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("USUARIOS", "UPDATE")) {
            return;
        }

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
                        loadPageFromRepo(currentPage);
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

    private void loadPageFromRepo(int page) {
        final int p = page;
        new Thread(() -> {
            try {
                String q = txtSearch == null ? null : txtSearch.getText();
                long count = userService.countUsers();
                int total = (int) count;
                int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                int pageToLoad = Math.min(Math.max(1, p), pages);
                int offset = (pageToLoad - 1) * pageSize;
                List<User> pageItems = userService.listPaged(offset, pageSize, q);
                Platform.runLater(() -> {
                    totalItems = total;
                    totalPages = pages;
                    currentPage = pageToLoad;
                    currentPageItems.setAll(pageItems == null ? java.util.List.of() : pageItems);
                    tblUsers.setItems(currentPageItems);
                    updatePaginationControls();
                });
            } catch (Throwable t) {
                Logs.error(getClass(), "Error listando usuarios: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar usuarios: " + t.getMessage()));
            }
        }).start();
    }

    private void updatePaginationControls() {
        if (lblPage != null) lblPage.setText(String.format("Página %d/%d (%d)", currentPage, totalPages, totalItems));
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages);
    }

    private @NotNull Dialog<User> userDialog(User base) {
        Dialog<User> d = new Dialog<>();
        String lblStyle = "-fx-font-weight: bold; -fx-font-size: 17;";
        String inputStyle = "-fx-font-size: 17;";

        // Hacer el diálogo redimensionable y darle un tamaño inicial un poco mayor
        d.setResizable(true);
        d.getDialogPane().setPrefSize(680, 300);

        d.setTitle(base == null ? "Nuevo usuario" : "Editar usuario");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField txtName = new TextField();
        txtName.setPromptText("Nombre");
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Usuario");

        // ComboBox con los roles disponibles de la base de datos (excluyendo ROOT)
        ComboBox<com.joshuamc1.inventariomunicipal.models.Rol> cmbRole = new ComboBox<>();

        // Cargar roles desde la base de datos
        try {
            List<com.joshuamc1.inventariomunicipal.models.Rol> roles =
                com.joshuamc1.inventariomunicipal.models.Rol.where("activo = ? AND id != ?", 1, 1).orderBy("nombre");
            cmbRole.getItems().addAll(roles);
        } catch (Exception e) {
            Logs.error(getClass(), "Error cargando roles: {}", e.getMessage());
        }

        // Configurar cómo se muestra cada rol
        cmbRole.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.joshuamc1.inventariomunicipal.models.Rol role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                } else {
                    setText(role.getString("nombre") + " - " + role.getString("descripcion"));
                }
            }
        });
        cmbRole.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(com.joshuamc1.inventariomunicipal.models.Rol role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                } else {
                    setText(role.getString("nombre"));
                }
            }
        });

        txtName.setStyle(inputStyle);
        txtUsername.setStyle(inputStyle);
        cmbRole.setStyle(inputStyle);

        if (base != null) {
            txtName.setText(base.getString("name"));
            txtUsername.setText(base.getString("username"));
            // Seleccionar el rol actual
            com.joshuamc1.inventariomunicipal.models.Rol rolActual = base.getRol();
            if (rolActual != null) {
                for (com.joshuamc1.inventariomunicipal.models.Rol r : cmbRole.getItems()) {
                    if (r.getId().equals(rolActual.getId())) {
                        cmbRole.getSelectionModel().select(r);
                        break;
                    }
                }
            }
        } else {
            // Por defecto seleccionar el primer rol disponible (usualmente OPERADOR)
            if (!cmbRole.getItems().isEmpty()) {
                cmbRole.getSelectionModel().select(0);
            }
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
                com.joshuamc1.inventariomunicipal.models.Rol selectedRole = cmbRole.getValue();
                if (selectedRole != null) {
                    u.set("rol_id", selectedRole.getId());
                }
                return u;
            }
            return null;
        });

        return d;
    }
}
