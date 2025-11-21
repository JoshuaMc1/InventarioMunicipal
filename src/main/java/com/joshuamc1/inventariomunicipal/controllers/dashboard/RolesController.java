package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.models.Permiso;
import com.joshuamc1.inventariomunicipal.models.Rol;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RolesController {
    @FXML
    private TableView<Rol> tableRoles;
    @FXML
    private TableColumn<Rol, Number> colId;
    @FXML
    private TableColumn<Rol, String> colNombre;
    @FXML
    private TableColumn<Rol, String> colDescripcion;
    @FXML
    private TableColumn<Rol, Boolean> colActivo;
    @FXML
    private TableColumn<Rol, Number> colUsuarios;
    @FXML
    private TableColumn<Rol, Void> colAcciones;
    @FXML
    private Button btnNuevoRol;

    @FXML
    public void initialize() {
        configurarTabla();
        cargarRoles();
        configurePermissions();

        btnNuevoRol.setOnAction(e -> onNuevoRol());
    }

    private void configurePermissions() {
        Logs.info(getClass(), "Configurando permisos de roles para: {}",
            com.joshuamc1.inventariomunicipal.core.Session.getCurrentUser().getString("username"));

        // Solo ROOT puede gestionar roles
        if (btnNuevoRol != null) {
            com.joshuamc1.inventariomunicipal.core.PermissionHelper.hideIfNoPermission(btnNuevoRol, "ROLES", "CREATE");
        }

        Logs.info(getClass(), "Permisos de roles configurados - Solo ROOT tiene acceso");
    }

    private void configurarTabla() {
        colId.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getInteger("id")));

        colNombre.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getString("nombre")));

        colDescripcion.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getString("descripcion")));

        colActivo.setCellValueFactory(data ->
            new SimpleBooleanProperty(data.getValue().isActivo()));

        colActivo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Sí" : "No");
                    setStyle(item ? "-fx-text-fill: green; -fx-font-size: 17;" : "-fx-text-fill: red; -fx-font-size: 17;");
                }
            }
        });

        colUsuarios.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getUsuarios().size()));

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnPermisos = new Button("Permisos");
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox hbox = new HBox(5, btnPermisos, btnEditar, btnEliminar);

            {
                btnPermisos.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 14;");
                btnEditar.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 14;");
                btnEliminar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 14;");

                btnPermisos.setOnAction(e -> {
                    Rol rol = getTableView().getItems().get(getIndex());
                    onGestionarPermisos(rol);
                });

                btnEditar.setOnAction(e -> {
                    Rol rol = getTableView().getItems().get(getIndex());
                    onEditarRol(rol);
                });

                btnEliminar.setOnAction(e -> {
                    Rol rol = getTableView().getItems().get(getIndex());
                    onEliminarRol(rol);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Rol rol = getTableView().getItems().get(getIndex());
                    // No permitir eliminar o editar el rol ROOT
                    btnEliminar.setDisable(rol.isRoot());
                    btnEditar.setDisable(rol.isRoot());
                    setGraphic(hbox);
                }
            }
        });

        colId.setStyle("-fx-font-size: 17;");
        colNombre.setStyle("-fx-font-size: 17;");
        colDescripcion.setStyle("-fx-font-size: 17;");
        colActivo.setStyle("-fx-font-size: 17;");
        colUsuarios.setStyle("-fx-font-size: 17;");
        colAcciones.setStyle("-fx-font-size: 17;");
    }

    private void cargarRoles() {
        new Thread(() -> {
            try {
                List<Rol> roles = Rol.findAll().orderBy("id");
                Platform.runLater(() -> tableRoles.getItems().setAll(roles));
            } catch (Exception e) {
                Logs.error(getClass(), "Error cargando roles: {}", e.getMessage());
                Platform.runLater(() ->
                    Utils.showAlert("Error", "No se pudieron cargar los roles: " + e.getMessage()));
            }
        }).start();
    }

    private void onNuevoRol() {
        // Validar permiso ROLES.CREATE (Solo ROOT)
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("ROLES", "CREATE")) {
            return;
        }

        Dialog<Rol> dialog = crearDialogoRol(null);
        dialog.showAndWait().ifPresent(rol -> {
            try {
                rol.saveIt();
                cargarRoles();
                Utils.showAlert("Éxito", "Rol creado correctamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando rol: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo crear el rol: " + e.getMessage());
            }
        });
    }

    private void onEditarRol(@NotNull Rol rol) {
        // Validar permiso ROLES.UPDATE (Solo ROOT)
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("ROLES", "UPDATE")) {
            return;
        }

        // VALIDACIÓN ESPECIAL: No editar roles del sistema
        if (rol.isRoot()) {
            Utils.showAlert("Error", "No se puede editar el rol ROOT del sistema");
            Logs.warn(getClass(), "Intento de editar rol ROOT bloqueado");
            return;
        }

        // Validación adicional para otros roles del sistema
        String nombreRol = rol.getString("nombre");
        if (nombreRol != null && (nombreRol.equals("ADMIN") || nombreRol.equals("AUDITOR") ||
            nombreRol.equals("SUPERVISOR") || nombreRol.equals("OPERADOR") || nombreRol.equals("CONSULTOR"))) {
            Alert confirm = new Alert(Alert.AlertType.WARNING,
                "Estás editando un rol del sistema. Los cambios pueden afectar el funcionamiento. ¿Continuar?",
                ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                return;
            }
        }

        Dialog<Rol> dialog = crearDialogoRol(rol);
        dialog.showAndWait().ifPresent(rolEditado -> {
            try {
                rol.set("nombre", rolEditado.getString("nombre"));
                rol.set("descripcion", rolEditado.getString("descripcion"));
                rol.set("activo", rolEditado.get("activo"));
                rol.saveIt();
                cargarRoles();
                Utils.showAlert("Éxito", "Rol actualizado correctamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando rol: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo actualizar el rol: " + e.getMessage());
            }
        });
    }

    private void onEliminarRol(@NotNull Rol rol) {
        // Validar permiso ROLES.DELETE (Solo ROOT)
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("ROLES", "DELETE")) {
            return;
        }

        // VALIDACIÓN ESPECIAL 1: No eliminar roles del sistema
        String nombreRol = rol.getString("nombre");
        java.util.List<String> rolesDelSistema = java.util.Arrays.asList("ROOT", "ADMIN", "AUDITOR", "SUPERVISOR", "OPERADOR", "CONSULTOR");

        if (nombreRol != null && rolesDelSistema.contains(nombreRol)) {
            Utils.showAlert("Error", "No se puede eliminar un rol del sistema: " + nombreRol);
            Logs.warn(getClass(), "Intento de eliminar rol del sistema bloqueado: {}", nombreRol);
            return;
        }

        // VALIDACIÓN ESPECIAL 2: No eliminar roles con usuarios asignados
        int cantidadUsuarios = rol.getUsuarios().size();
        if (cantidadUsuarios > 0) {
            Utils.showAlert("Error",
                "No se puede eliminar el rol porque tiene " + cantidadUsuarios + " usuario(s) asignado(s)");
            Logs.warn(getClass(), "Intento de eliminar rol con usuarios asignados bloqueado");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Está seguro de eliminar el rol '" + rol.getString("nombre") + "'?",
            ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    rol.delete();
                    cargarRoles();
                    Utils.showAlert("Éxito", "Rol eliminado correctamente");
                } catch (Exception e) {
                    Logs.error(getClass(), "Error eliminando rol: {}", e.getMessage());
                    Utils.showAlert("Error", "No se pudo eliminar el rol: " + e.getMessage());
                }
            }
        });
    }

    private void onGestionarPermisos(Rol rol) {
        // Validar permiso ROLES.UPDATE (gestionar permisos es una actualización)
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("ROLES", "UPDATE")) {
            return;
        }

        Dialog<List<Integer>> dialog = crearDialogoPermisos(rol);
        dialog.showAndWait().ifPresent(permisosIds -> {
            try {
                rol.sincronizarPermisos(permisosIds);
                Utils.showAlert("Éxito", "Permisos actualizados correctamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando permisos: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudieron actualizar los permisos: " + e.getMessage());
            }
        });
    }

    private @NotNull Dialog<Rol> crearDialogoRol(Rol base) {
        Dialog<Rol> dialog = new Dialog<>();
        dialog.setTitle(base == null ? "Nuevo Rol" : "Editar Rol");
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(830, 300);
        String lblStyle = "-fx-font-weight: bold; -fx-font-size: 17;";
        String inputStyle = "-fx-font-size: 17;";

        ButtonType saveButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del rol (ej: OPERADOR)");

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Descripción del rol");
        txtDescripcion.setPrefRowCount(3);

        CheckBox chkActivo = new CheckBox("Activo");
        chkActivo.setSelected(true);

        txtNombre.setStyle(inputStyle);
        txtDescripcion.setStyle(inputStyle);

        if (base != null) {
            txtNombre.setText(base.getString("nombre"));
            txtDescripcion.setText(base.getString("descripcion"));
            chkActivo.setSelected(base.isActivo());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label lblNombre = new Label("Nombre:");
        lblNombre.setStyle(lblStyle);
        Label lblDescripcion = new Label("Descripción:");
        lblDescripcion.setStyle(lblStyle);

        grid.add(lblNombre, 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(lblDescripcion, 0, 1);
        grid.add(txtDescripcion, 1, 1);
        grid.add(chkActivo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                Rol rol = base == null ? new Rol() : base;
                rol.set("nombre", txtNombre.getText().toUpperCase());
                rol.set("descripcion", txtDescripcion.getText());
                rol.set("activo", chkActivo.isSelected() ? 1 : 0);
                return rol;
            }
            return null;
        });

        return dialog;
    }

    private @NotNull Dialog<List<Integer>> crearDialogoPermisos(@NotNull Rol rol) {
        Dialog<List<Integer>> dialog = new Dialog<>();
        dialog.setTitle("Gestionar Permisos - " + rol.getString("nombre"));
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(780, 600);

        ButtonType saveButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        // Obtener permisos actuales del rol
        List<Permiso> permisosActuales = rol.getPermisos();
        List<Integer> idsActuales = new ArrayList<>();
        for (Permiso p : permisosActuales) {
            idsActuales.add(p.getInteger("id"));
        }

        // Crear checkboxes agrupados por módulo
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        Map<String, List<Permiso>> permisosPorModulo = Permiso.agruparPorModulo();
        List<CheckBox> checkBoxes = new ArrayList<>();

        for (Map.Entry<String, List<Permiso>> entry : permisosPorModulo.entrySet()) {
            String modulo = entry.getKey();
            List<Permiso> permisos = entry.getValue();

            Label lblModulo = new Label(modulo);
            lblModulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
            content.getChildren().add(lblModulo);

            VBox permisoBox = new VBox(5);
            permisoBox.setPadding(new Insets(0, 0, 0, 20));

            for (Permiso permiso : permisos) {
                CheckBox cb = new CheckBox(permiso.getString("accion") + " - " +
                    permiso.getString("descripcion"));
                cb.setUserData(permiso.getInteger("id"));
                cb.setSelected(idsActuales.contains(permiso.getInteger("id")));
                checkBoxes.add(cb);
                permisoBox.getChildren().add(cb);
            }

            content.getChildren().add(permisoBox);
            content.getChildren().add(new Separator());
        }

        dialog.getDialogPane().setContent(scrollPane);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                List<Integer> permisosSeleccionados = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) {
                        permisosSeleccionados.add((Integer) cb.getUserData());
                    }
                }
                return permisosSeleccionados;
            }
            return null;
        });

        return dialog;
    }
}

