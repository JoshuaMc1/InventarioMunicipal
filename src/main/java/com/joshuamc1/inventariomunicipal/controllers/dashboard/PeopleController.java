package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.models.Person;
import com.joshuamc1.inventariomunicipal.services.PersonService;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PeopleController {
    @FXML
    private TableView<Person> tblPeople;
    @FXML
    private TableColumn<Person, String> colId;
    @FXML
    private TableColumn<Person, String> colDni;
    @FXML
    private TableColumn<Person, String> colFirstName;
    @FXML
    private TableColumn<Person, String> colLastName;
    @FXML
    private TableColumn<Person, String> colPhone;
    @FXML
    private TableColumn<Person, String> colInternalCode;
    @FXML
    private TextField txtSearch;

    private final PersonService personService = new PersonService();

    private int offset = 0;
    private final int limit = 50;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getInteger("id"))));
        colDni.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("dni")));
        colFirstName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("first_name")));
        colLastName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("last_name")));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("phone")));
        colInternalCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("internal_code")));

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
        Dialog<Person> dialog = personDialog(null);
        dialog.showAndWait().ifPresent(p -> {
            try {
                personService.create(
                    p.getString("dni"),
                    p.getString("first_name"),
                    p.getString("last_name"),
                    p.getString("phone"),
                    p.getString("internal_code")
                );
                loadPage();
                Utils.showAlert("Éxito", "Persona registrada exitosamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando persona: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo registrar la persona: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onEdit() {
        Person selected = tblPeople.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione una persona");
            return;
        }
        Dialog<Person> dialog = personDialog(selected);
        dialog.showAndWait().ifPresent(p -> {
            try {
                personService.update(
                    selected.getInteger("id"),
                    p.getString("dni"),
                    p.getString("first_name"),
                    p.getString("last_name"),
                    p.getString("phone"),
                    p.getString("internal_code")
                );
                loadPage();
                Utils.showAlert("Éxito", "Persona actualizada exitosamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando persona: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo actualizar la persona: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onDelete() {
        Person selected = tblPeople.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione una persona");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar a " + selected.getFullName() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    personService.delete(selected.getInteger("id"));
                    loadPage();
                    Utils.showAlert("Éxito", "Persona eliminada exitosamente");
                } catch (Exception e) {
                    Logs.error(getClass(), "Error eliminando persona: {}", e.getMessage());
                    Utils.showAlert("Error", "No se pudo eliminar la persona: " + e.getMessage());
                }
            }
        });
    }

    private void loadPage() {
        new Thread(() -> {
            try {
                String q = txtSearch == null ? null : txtSearch.getText();
                List<Person> people = personService.listPaged(offset, limit, q);
                Platform.runLater(() -> tblPeople.getItems().setAll(people));
            } catch (Throwable t) {
                Logs.error(getClass(), "Error listando personas: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar personas: " + t.getMessage()));
            }
        }).start();
    }

    private @NotNull Dialog<Person> personDialog(Person base) {
        Dialog<Person> d = new Dialog<>();
        d.setResizable(true);
        d.getDialogPane().setPrefSize(500, 350);
        d.setTitle(base == null ? "Nueva Persona" : "Editar Persona");

        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField txtDni = new TextField();
        txtDni.setPromptText("0000-0000-00000");
        TextField txtFirstName = new TextField();
        txtFirstName.setPromptText("Nombre");
        TextField txtLastName = new TextField();
        txtLastName.setPromptText("Apellido");
        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Teléfono");
        TextField txtInternalCode = new TextField();
        txtInternalCode.setPromptText("Código Interno (opcional)");

        if (base != null) {
            txtDni.setText(base.getString("dni"));
            txtFirstName.setText(base.getString("first_name"));
            txtLastName.setText(base.getString("last_name"));
            txtPhone.setText(base.getString("phone"));
            txtInternalCode.setText(base.getString("internal_code"));
        }

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new Label("DNI"), txtDni);
        gp.addRow(1, new Label("Nombre"), txtFirstName);
        gp.addRow(2, new Label("Apellido"), txtLastName);
        gp.addRow(3, new Label("Teléfono"), txtPhone);
        gp.addRow(4, new Label("Código Interno"), txtInternalCode);

        gp.setPrefWidth(460);
        GridPane.setHgrow(txtDni, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtFirstName, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtLastName, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtPhone, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtInternalCode, javafx.scene.layout.Priority.ALWAYS);

        d.getDialogPane().setContent(gp);

        Node btnSave = d.getDialogPane().lookupButton(save);
        BooleanBinding invalid = txtDni.textProperty().isEmpty()
                .or(txtFirstName.textProperty().isEmpty())
                .or(txtLastName.textProperty().isEmpty());
        btnSave.disableProperty().bind(invalid);

        d.setResultConverter(bt -> {
            if (bt == save) {
                Person p = new Person();
                p.set("dni", txtDni.getText());
                p.set("first_name", txtFirstName.getText());
                p.set("last_name", txtLastName.getText());
                p.set("phone", txtPhone.getText());
                p.set("internal_code", txtInternalCode.getText());
                return p;
            }
            return null;
        });

        return d;
    }
}

