package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.dialog.PersonDialogHelper;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.core.PermissionHelper;
import com.joshuamc1.inventariomunicipal.models.Person;
import com.joshuamc1.inventariomunicipal.services.PersonService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PeopleController {
    @FXML
    private TableView<Person> tblPeople;
    @FXML @SuppressWarnings("unused") private Button btnNew;
    @FXML @SuppressWarnings("unused") private Button btnEdit;
    @FXML @SuppressWarnings("unused") private Button btnDelete;
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

    // Controles de paginación
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private ChoiceBox<Integer> choicePageSize;

    private final PersonService personService = new PersonService();

    private int currentPage = 1;
    private int pageSize = 50;
    private int totalItems = 0;
    private int totalPages = 1;

    private final ObservableList<Person> currentPageItems = FXCollections.observableArrayList();

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

        // Configurar visibilidad/estado de botones según permisos
        if (btnNew != null) PermissionHelper.hideIfNoPermission(btnNew, "PERSONAS", "CREATE");
        if (btnEdit != null) PermissionHelper.hideIfNoPermission(btnEdit, "PERSONAS", "UPDATE");
        if (btnDelete != null) PermissionHelper.hideIfNoPermission(btnDelete, "PERSONAS", "DELETE");

        Logs.info(getClass(), "Permisos de personas configurados");

        configurePermissions();

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
    }

    private void configurePermissions() {
        // Ya se configuró en initialize(), este método es para consistencia con otros controladores
        Logs.info(getClass(), "Configurando permisos de personas para: {}",
            com.joshuamc1.inventariomunicipal.core.Session.getCurrentUser().getString("username"));

        // Los botones ya fueron configurados con hideIfNoPermission en initialize()
        // Este método adicional permite logging y futuras extensiones
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
        // Validar permiso en UI
        if (!PermissionHelper.validateAndShow("PERSONAS", "CREATE")) return;
        Person created = PersonDialogHelper.showCreatePersonDialog();
        if (created != null) {
            loadPageFromRepo(currentPage);
        }
    }

    @FXML
    public void onEdit() {
        // Validar permiso en UI
        if (!PermissionHelper.validateAndShow("PERSONAS", "UPDATE")) return;
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
                loadPageFromRepo(currentPage);
                Utils.showAlert("Éxito", "Persona actualizada exitosamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error actualizando persona: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo actualizar la persona: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onDelete() {
        // Validar permiso en UI
        if (!PermissionHelper.validateAndShow("PERSONAS", "DELETE")) return;
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
                    loadPageFromRepo(currentPage);
                    Utils.showAlert("Éxito", "Persona eliminada exitosamente");
                } catch (Exception e) {
                    Logs.error(getClass(), "Error eliminando persona: {}", e.getMessage());
                    Utils.showAlert("Error", "No se pudo eliminar la persona: " + e.getMessage());
                }
            }
        });
    }

    private void loadPageFromRepo(int page) {
        final int p = page;
        new Thread(() -> {
            try {
                String q = txtSearch == null ? null : txtSearch.getText();
                long count = personService.count();
                if (q != null && !q.isBlank()) {
                    // Cuando hay búsqueda activa, usamos el count general como aproximación.
                    // Si se requiere mayor precisión, agregar un método countFiltered en PersonRepository.
                    Logs.info(getClass(), "Búsqueda activa en personas: usando count aproximado para paginación");
                }
                int total = (int) count;
                int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                int pageToLoad = Math.min(Math.max(1, p), pages);
                int offset = (pageToLoad - 1) * pageSize;
                List<Person> pageItems = personService.listPaged(offset, pageSize, q);
                Platform.runLater(() -> {
                    totalItems = total;
                    totalPages = pages;
                    currentPage = pageToLoad;
                    currentPageItems.setAll(pageItems == null ? java.util.List.of() : pageItems);
                    tblPeople.setItems(currentPageItems);
                    updatePaginationControls();
                });
            } catch (Throwable t) {
                Logs.error(getClass(), "Error listando personas: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar personas: " + t.getMessage()));
            }
        }).start();
    }

    private void updatePaginationControls() {
        if (lblPage != null) lblPage.setText(String.format("Página %d/%d (%d)", currentPage, totalPages, totalItems));
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages);
    }
}
