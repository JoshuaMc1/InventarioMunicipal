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
import org.javalite.activejdbc.Base;

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

        btnNuevoRol.setOnAction(e -> onNuevoRol());
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
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
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
                btnPermisos.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 5 10;");
                btnEditar.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 5 10;");
                btnEliminar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 5 10;");

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

    private void onEditarRol(Rol rol) {
        if (rol.isRoot()) {
            Utils.showAlert("Aviso", "No se puede editar el rol ROOT");
            return;
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

    private void onEliminarRol(Rol rol) {
        if (rol.isRoot()) {
            Utils.showAlert("Aviso", "No se puede eliminar el rol ROOT");
            return;
        }

        int cantidadUsuarios = rol.getUsuarios().size();
        if (cantidadUsuarios > 0) {
            Utils.showAlert("Aviso",
                "No se puede eliminar el rol porque tiene " + cantidadUsuarios + " usuario(s) asignado(s)");
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

    private Dialog<Rol> crearDialogoRol(Rol base) {
        Dialog<Rol> dialog = new Dialog<>();
        dialog.setTitle(base == null ? "Nuevo Rol" : "Editar Rol");
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(500, 300);

        ButtonType saveButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del rol (ej: OPERADOR)");

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Descripción del rol");
        txtDescripcion.setPrefRowCount(3);

        CheckBox chkActivo = new CheckBox("Activo");
        chkActivo.setSelected(true);

        if (base != null) {
            txtNombre.setText(base.getString("nombre"));
            txtDescripcion.setText(base.getString("descripcion"));
            chkActivo.setSelected(base.isActivo());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
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

    private Dialog<List<Integer>> crearDialogoPermisos(Rol rol) {
        Dialog<List<Integer>> dialog = new Dialog<>();
        dialog.setTitle("Gestionar Permisos - " + rol.getString("nombre"));
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(700, 600);

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

