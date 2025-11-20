package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.dialog.PersonDialogHelper;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.models.Person;
import com.joshuamc1.inventariomunicipal.services.PersonService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

        colId.setStyle("-fx-font-size: 17;");
        colDni.setStyle("-fx-font-size: 17;");
        colFirstName.setStyle("-fx-font-size: 17;");
        colLastName.setStyle("-fx-font-size: 17;");
        colPhone.setStyle("-fx-font-size: 17;");
        colInternalCode.setStyle("-fx-font-size: 17;");

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
        Person created = PersonDialogHelper.showCreatePersonDialog();
        if (created != null) {
            loadPage();
        }
    }

    @FXML
    public void onEdit() {
        Person selected = tblPeople.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Utils.showAlert("Aviso", "Seleccione una persona");
            return;
        }
        Dialog<Person> dialog = PersonDialogHelper.createPersonDialog(selected);
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
}

