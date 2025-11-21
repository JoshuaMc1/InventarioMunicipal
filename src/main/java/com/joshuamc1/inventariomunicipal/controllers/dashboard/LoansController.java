package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.dialog.PersonDialogHelper;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.models.Person;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.Transaction;
import com.joshuamc1.inventariomunicipal.services.PersonService;
import com.joshuamc1.inventariomunicipal.services.ProductService;
import com.joshuamc1.inventariomunicipal.services.TransactionService;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class LoansController {
    @FXML
    private TableView<Transaction> tblTransactions;
    @FXML
    private TableColumn<Transaction, String> colId;
    @FXML
    private TableColumn<Transaction, String> colProduct;
    @FXML
    private TableColumn<Transaction, String> colQuantity;
    @FXML
    private TableColumn<Transaction, String> colDate;
    @FXML
    private TableColumn<Transaction, String> colDni;
    @FXML
    private TableColumn<Transaction, String> colPerson;
    @FXML
    private TableColumn<Transaction, String> colNotes;

    // Paginación
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private ChoiceBox<Integer> choicePageSize;
    @FXML private Button btnNewLoan;
    @FXML private TextField txtSearch;
    @FXML private Button btnRefresh;

    private int currentPage = 1;
    private int pageSize = 25;
    private int totalItems = 0;
    private int totalPages = 1;

    private final ObservableList<Transaction> currentPageItems = FXCollections.observableArrayList();

    private final TransactionService transactionService = new TransactionService();
    private final ProductService productService = new ProductService();
    private final PersonService personService = new PersonService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getInteger("id"))));
        colProduct.setCellValueFactory(data -> {
            Integer productId = data.getValue().getInteger("product_id");
            Product product = productService.findById(productId);
            return new SimpleStringProperty(product != null ? product.getString("name") : "N/A");
        });
        colQuantity.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDouble("quantity"))));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("date")));

        colDni.setCellValueFactory(data -> {
            Integer personId = data.getValue().getInteger("person_id");
            if (personId != null) {
                Person person = personService.findById(personId);
                return new SimpleStringProperty(person != null ? person.getString("dni") : "N/A");
            }
            return new SimpleStringProperty("-");
        });
        colPerson.setCellValueFactory(data -> {
            Integer personId = data.getValue().getInteger("person_id");
            if (personId != null) {
                Person person = personService.findById(personId);
                return new SimpleStringProperty(person != null ? person.getFullName() : "N/A");
            }
            return new SimpleStringProperty("-");
        });
        colNotes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("notes")));

        colId.setStyle("-fx-font-size: 17;");
        colProduct.setStyle("-fx-font-size: 17;");
        colQuantity.setStyle("-fx-font-size: 17;");
        colDate.setStyle("-fx-font-size: 17;");
        colDni.setStyle("-fx-font-size: 17;");
        colPerson.setStyle("-fx-font-size: 17;");
        colNotes.setStyle("-fx-font-size: 17;");

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

        // Botón refrescar
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> loadPageFromRepo(currentPage));
        }

        // Búsqueda automática con debounce de 500ms
        if (txtSearch != null) {
            PauseTransition searchPause = new PauseTransition(Duration.millis(500));
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                searchPause.setOnFinished(event -> {
                    currentPage = 1;
                    loadPageFromRepo(currentPage);
                });
                searchPause.playFromStart();
            });
        }

        loadPageFromRepo(1);

        configurePermissions();
    }

    private void configurePermissions() {
        if (btnNewLoan != null) {
            com.joshuamc1.inventariomunicipal.core.PermissionHelper.hideIfNoPermission(btnNewLoan, "PRESTAMOS", "CREATE");
        }
    }

    @FXML
    public void onNew() {
        // Validar permiso específico del módulo PRESTAMOS
        if (!com.joshuamc1.inventariomunicipal.core.PermissionHelper.validateAndShow("PRESTAMOS", "CREATE")) {
            return;
        }
        Dialog<Transaction> dialog = transactionDialog();
        dialog.showAndWait().ifPresent(t -> {
            try {
                transactionService.createTransaction(
                    t.getInteger("product_id"),
                    Transaction.TYPE_LOAN,
                    t.getDouble("quantity"),
                    t.getString("date"),
                    t.getInteger("person_id"),
                    t.getString("notes")
                );
                loadPageFromRepo(currentPage);
                Utils.showAlert("Éxito", "Préstamo registrado exitosamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando préstamo: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo registrar el préstamo: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onRefresh() {
        loadPageFromRepo(currentPage);
    }

    private void loadPageFromRepo(int page) {
        final int p = page;
        final String searchText = txtSearch != null ? txtSearch.getText() : "";
        new Thread(() -> {
            try {
                long count;
                List<Transaction> pageItems;
                int offset = (Math.max(1, p) - 1) * pageSize;

                if (searchText == null || searchText.trim().isEmpty()) {
                    count = transactionService.countByType(Transaction.TYPE_LOAN);
                    pageItems = transactionService.findByTypePage(Transaction.TYPE_LOAN, pageSize, offset);
                } else {
                    // Búsqueda filtrada
                    String search = searchText.trim().toLowerCase();
                    List<Transaction> allTransactions = transactionService.findByType(Transaction.TYPE_LOAN);
                    List<Transaction> filtered = allTransactions.stream()
                        .filter(t -> {
                            if (String.valueOf(t.getInteger("id")).contains(search)) return true;
                            Product product = productService.findById(t.getInteger("product_id"));
                            if (product != null && product.getString("name").toLowerCase().contains(search)) return true;
                            Integer personId = t.getInteger("person_id");
                            if (personId != null) {
                                Person person = personService.findById(personId);
                                if (person != null) {
                                    if (person.getString("dni").toLowerCase().contains(search)) return true;
                                    if (person.getFullName().toLowerCase().contains(search)) return true;
                                }
                            }
                            return false;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    count = filtered.size();
                    int fromIndex = Math.min(offset, filtered.size());
                    int toIndex = Math.min(offset + pageSize, filtered.size());
                    pageItems = fromIndex < toIndex ? filtered.subList(fromIndex, toIndex) : List.of();
                }

                int total = (int) count;
                int pages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                int pageToLoad = Math.min(Math.max(1, p), pages);

                Platform.runLater(() -> {
                    totalItems = total;
                    totalPages = pages;
                    currentPageItems.setAll(pageItems == null ? java.util.List.of() : pageItems);
                    tblTransactions.setItems(currentPageItems);
                    updatePaginationControls();
                });
            } catch (Throwable t) {
                Logs.error(getClass(), "Error cargando página de préstamos: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar préstamos: " + t.getMessage()));
            }
        }, "loans-pager").start();
    }

    private void updatePaginationControls() {
        if (lblPage != null) lblPage.setText(String.format("Página %d/%d (%d)", currentPage, totalPages, totalItems));
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages);
    }

    private @NotNull Dialog<Transaction> transactionDialog() {
        Dialog<Transaction> d = new Dialog<>();
        d.setResizable(true);
        d.getDialogPane().setPrefSize(1050, 580);
        d.setTitle("Nuevo Préstamo");
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        // Listas observables inicialmente vacías (se llenan asíncronamente)
        var masterProducts = FXCollections.<Product>observableArrayList();
        var filteredProducts = new FilteredList<>(masterProducts, p -> true);
        var masterPeople = FXCollections.<Person>observableArrayList();
        var filteredPeople = new FilteredList<>(masterPeople, p -> true);

        // Combos
        ComboBox<Product> cmbProduct = new ComboBox<>(filteredProducts);
        cmbProduct.setPromptText("Seleccione producto");
        cmbProduct.setCellFactory(lv -> new ListCell<>() { @Override protected void updateItem(Product it, boolean empty){ super.updateItem(it,empty); setText(empty||it==null?null: it.getInteger("id")+" - "+it.getString("name")+" (Stock: "+it.getDouble("current_stock")+")"); }});
        cmbProduct.setButtonCell(new ListCell<>() { @Override protected void updateItem(Product it, boolean empty){ super.updateItem(it,empty); setText(empty||it==null?null: it.getInteger("id")+" - "+it.getString("name")+" (Stock: "+it.getDouble("current_stock")+")"); }});

        ComboBox<Person> cmbPerson = new ComboBox<>(filteredPeople);
        cmbPerson.setPromptText("Seleccione persona");
        cmbPerson.setCellFactory(lv -> new ListCell<>() { @Override protected void updateItem(Person it, boolean empty){ super.updateItem(it,empty); setText(empty||it==null?null: it.getString("dni")+" - "+it.getFullName()); }});
        cmbPerson.setButtonCell(new ListCell<>() { @Override protected void updateItem(Person it, boolean empty){ super.updateItem(it,empty); setText(empty||it==null?null: it.getString("dni")+" - "+it.getFullName()); }});

        // Campos de búsqueda con debounce - OPTIMIZADO con búsqueda dinámica desde BD
        TextField txtSearchProduct = new TextField(); txtSearchProduct.setPromptText("Buscar producto por ID o nombre...");
        TextField txtSearchPerson = new TextField(); txtSearchPerson.setPromptText("Buscar persona por DNI o nombre...");
        PauseTransition productDebounce = new PauseTransition(Duration.millis(300));
        PauseTransition personDebounce = new PauseTransition(Duration.millis(300));

        txtSearchProduct.textProperty().addListener((obs, old, val) -> {
            productDebounce.stop();
            productDebounce.setOnFinished(e -> {
                String query = val == null ? "" : val.trim();
                if (query.length() >= 2 || query.isEmpty()) {
                    // Buscar dinámicamente desde BD con límite de 100
                    new Thread(() -> {
                        List<Product> results = productService.searchProducts(query, 100);
                        Platform.runLater(() -> {
                            Product selected = cmbProduct.getValue();
                            masterProducts.setAll(results);
                            if (selected != null && results.contains(selected)) {
                                cmbProduct.setValue(selected);
                            }
                        });
                    }).start();
                }
            });
            productDebounce.playFromStart();
        });

        txtSearchPerson.textProperty().addListener((obs, old, val) -> {
            personDebounce.stop();
            personDebounce.setOnFinished(e -> {
                String query = val == null ? "" : val.trim();
                if (query.length() >= 2 || query.isEmpty()) {
                    // Buscar dinámicamente desde BD con límite de 100
                    new Thread(() -> {
                        List<Person> results = personService.searchPeople(query, 100);
                        Platform.runLater(() -> {
                            Person selected = cmbPerson.getValue();
                            masterPeople.setAll(results);
                            if (selected != null && results.contains(selected)) {
                                cmbPerson.setValue(selected);
                            }
                        });
                    }).start();
                }
            });
            personDebounce.playFromStart();
        });

        TextField txtQuantity = new TextField(); txtQuantity.setPromptText("Cantidad");
        // TextFormatter para permitir solo números decimales o enteros
        TextFormatter<String> quantityFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change; // permitir vacío (se valida luego)
            if (newText.matches("\\d+(\\.\\d{0,4})?")) return change; // hasta 4 decimales
            return null; // descartar cambios inválidos
        });
        txtQuantity.setTextFormatter(quantityFormatter);
        txtQuantity.textProperty().addListener((obs, old, val) -> {
            if(val == null || val.isBlank()) {
                txtQuantity.setStyle("-fx-font-size: 17; -fx-border-color: #d60000; -fx-border-width: 1; -fx-border-radius: 4;");
            } else if(!val.matches("\\d+(\\.\\d{0,4})?")) {
                txtQuantity.setStyle("-fx-font-size: 17; -fx-border-color: #d60000; -fx-border-width: 1; -fx-border-radius: 4;");
            } else {
                txtQuantity.setStyle("-fx-font-size: 17;");
            }
        });

        DatePicker dpDate = new DatePicker(LocalDate.now());
        dpDate.valueProperty().addListener((obs, old, val) -> {
            if(val == null) {
                if(dpDate.getEditor()!=null) dpDate.getEditor().setStyle("-fx-font-size: 17; -fx-border-color: #d60000; -fx-border-width: 1; -fx-border-radius: 4;");
            } else {
                if(dpDate.getEditor()!=null) dpDate.getEditor().setStyle("-fx-font-size: 17;");
            }
        });

        TextArea txtNotes = new TextArea(); txtNotes.setPromptText("Notas (opcional)"); txtNotes.setPrefRowCount(3);

        // Stock actual
        Label lblStockTitle = new Label("Stock Actual:");
        Label lblStockValue = new Label("-");

        // Estilos
        String lblStyle = "-fx-font-weight: bold; -fx-font-size: 17;";
        String inputStyle = "-fx-font-size: 17;";
        cmbProduct.setStyle(inputStyle); cmbPerson.setStyle(inputStyle); txtSearchProduct.setStyle(inputStyle); txtSearchPerson.setStyle(inputStyle);
        txtQuantity.setStyle(inputStyle); dpDate.setStyle(inputStyle); if(dpDate.getEditor()!=null) dpDate.getEditor().setStyle(inputStyle); txtNotes.setStyle(inputStyle);
        lblStockTitle.setStyle(lblStyle); lblStockValue.setStyle("-fx-font-size: 17;");

        Label lblProduct = new Label("Producto:"); lblProduct.setStyle(lblStyle);
        Label lblSearchProduct = new Label("Filtrar Producto:"); lblSearchProduct.setStyle(lblStyle);
        Label lblQuantity = new Label("Cantidad:"); lblQuantity.setStyle(lblStyle);
        Label lblDate = new Label("Fecha:"); lblDate.setStyle(lblStyle);
        Label lblPerson = new Label("Persona:"); lblPerson.setStyle(lblStyle);
        Label lblSearchPerson = new Label("Filtrar Persona:"); lblSearchPerson.setStyle(lblStyle);
        Label lblNotes = new Label("Notas:"); lblNotes.setStyle(lblStyle);

        // Helper texts obligatorios
        Label helperProduct = new Label("Obligatorio");
        helperProduct.setStyle("-fx-text-fill: #d60000; -fx-font-size: 12px;");
        Label helperQuantity = new Label("Obligatorio");
        helperQuantity.setStyle("-fx-text-fill: #d60000; -fx-font-size: 12px;");
        Label helperDate = new Label("Obligatorio");
        helperDate.setStyle("-fx-text-fill: #d60000; -fx-font-size: 12px;");
        Label helperPerson = new Label("Obligatorio");
        helperPerson.setStyle("-fx-text-fill: #d60000; -fx-font-size: 12px;");
        // Botón para crear nueva persona
        Button btnNewPerson = new Button("+");
        btnNewPerson.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
        btnNewPerson.setTooltip(new Tooltip("Crear nueva persona"));
        btnNewPerson.setOnAction(e -> {
            Person newPerson = PersonDialogHelper.showCreatePersonDialog();
            if (newPerson != null) {
                // Agregar a la lista maestra
                masterPeople.add(newPerson);
                // Seleccionar automáticamente
                cmbPerson.getSelectionModel().select(newPerson);
            }
        });

        // HBox para ComboBox de persona + botón
        HBox personBox = new HBox(5, cmbPerson, btnNewPerson);
        HBox.setHgrow(cmbPerson, Priority.ALWAYS);

        GridPane gp = new GridPane();
        gp.setHgap(12); gp.setVgap(8); gp.setPadding(new Insets(14));
        // Fila 0-1 producto
        gp.add(lblProduct,0,0); gp.add(cmbProduct,1,0); gp.add(lblSearchProduct,2,0); gp.add(txtSearchProduct,3,0);
        gp.add(helperProduct,1,1,3,1);
        // Fila 2 stock
        gp.add(lblStockTitle,0,2); gp.add(lblStockValue,1,2);
        // Fila 3-4 cantidad
        gp.add(lblQuantity,0,3); gp.add(txtQuantity,1,3);
        gp.add(helperQuantity,1,4);
        // Fila 5-6 fecha
        gp.add(lblDate,0,5); gp.add(dpDate,1,5);
        gp.add(helperDate,1,6);
        // Fila 7-8 persona con botón
        gp.add(lblPerson,0,7); gp.add(personBox,1,7); gp.add(lblSearchPerson,2,7); gp.add(txtSearchPerson,3,7);
        gp.add(helperPerson,1,8,3,1);
        // Fila 9 notas
        gp.add(lblNotes,0,9); gp.add(txtNotes,1,9,3,1);

        GridPane.setHgrow(cmbProduct, Priority.ALWAYS);
        GridPane.setHgrow(txtSearchProduct, Priority.ALWAYS);
        GridPane.setHgrow(personBox, Priority.ALWAYS);
        GridPane.setHgrow(txtSearchPerson, Priority.ALWAYS);
        GridPane.setHgrow(txtQuantity, Priority.ALWAYS);
        GridPane.setHgrow(dpDate, Priority.ALWAYS);
        GridPane.setHgrow(txtNotes, Priority.ALWAYS);

        // Indicador de carga
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40,40);
        Label lblLoading = new Label("Cargando listas (productos y personas)...");
        lblLoading.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        HBox loadingBox = new HBox(10, pi, lblLoading);
        loadingBox.setPadding(new Insets(10));
        loadingBox.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-border-color: #ccc; -fx-border-radius: 4; -fx-background-radius: 4;");
        StackPane stack = new StackPane(gp, loadingBox);
        StackPane.setMargin(loadingBox, new Insets(0));
        d.getDialogPane().setContent(stack);

        // Deshabilitar controles mientras carga
        cmbProduct.setDisable(true); cmbPerson.setDisable(true); txtSearchProduct.setDisable(true); txtSearchPerson.setDisable(true);
        txtQuantity.setDisable(true); dpDate.setDisable(true); txtNotes.setDisable(true);

        // Carga asíncrona de listas
        // Carga asíncrona de listas OPTIMIZADA - Solo cargar 100 registros iniciales
        var productsTask = new Task<List<Product>>() {
            @Override
            protected List<Product> call() {
                return productService.searchProducts("", 100); // Límite de 100 productos iniciales
            }
        };
        var peopleTask = new Task<List<Person>>() {
            @Override
            protected List<Person> call() {
                return personService.searchPeople("", 100); // Límite de 100 personas iniciales
            }
        };

        Runnable tryFinish = () -> {
            if(productsTask.isDone() && peopleTask.isDone()) {
                Platform.runLater(() -> {
                    loadingBox.setVisible(false);
                    cmbProduct.setDisable(false); cmbPerson.setDisable(false); txtSearchProduct.setDisable(false); txtSearchPerson.setDisable(false);
                    txtQuantity.setDisable(false); dpDate.setDisable(false); txtNotes.setDisable(false);
                });
            }
        };

        productsTask.setOnSucceeded(ev -> {
            masterProducts.setAll(productsTask.getValue());
            tryFinish.run();
        });
        peopleTask.setOnSucceeded(ev -> {
            masterPeople.setAll(peopleTask.getValue());
            tryFinish.run();
        });
        productsTask.setOnFailed(ev -> {
            Logs.error(getClass(), "Error cargando productos: {}", productsTask.getException().getMessage());
            Platform.runLater(() -> Utils.showAlert("Error", "No se pudieron cargar productos: " + productsTask.getException().getMessage()));
            tryFinish.run();
        });
        peopleTask.setOnFailed(ev -> {
            Logs.error(getClass(), "Error cargando personas: {}", peopleTask.getException().getMessage());
            Platform.runLater(() -> Utils.showAlert("Error", "No se pudieron cargar personas: " + peopleTask.getException().getMessage()));
            tryFinish.run();
        });

        new Thread(productsTask, "products-loader").start();
        new Thread(peopleTask, "people-loader").start();

        // Actualizar stock cuando cambia producto
        cmbProduct.valueProperty().addListener((obs, old, val) -> {
            if(val == null) { lblStockValue.setText("-"); lblStockValue.setStyle("-fx-font-size: 17;"); return; }
            try {
                Double stock = val.getDouble("current_stock");
                lblStockValue.setText(stock == null ? "-" : String.format("%.2f", stock));
                // Resaltar si está en bajo stock
                if(val.isLowStock()) {
                    lblStockValue.setStyle("-fx-font-size: 17; -fx-text-fill: #d60000; -fx-font-weight: bold;");
                } else {
                    lblStockValue.setStyle("-fx-font-size: 17; -fx-text-fill: #1a1a1a;");
                }
            } catch (Exception ex){
                lblStockValue.setText("-");
                lblStockValue.setStyle("-fx-font-size: 17;");
            }
        });

        // Actualizar binding de botón Guardar para incluir persona y fecha
        Node btnSave = d.getDialogPane().lookupButton(save);
        BooleanBinding invalid = cmbProduct.valueProperty().isNull()
                .or(cmbPerson.valueProperty().isNull())
                .or(txtQuantity.textProperty().isEmpty())
                .or(dpDate.valueProperty().isNull());
        btnSave.disableProperty().bind(invalid.or(loadingBox.visibleProperty()));

        d.setResultConverter(bt -> {
            if(bt==save){
                try {
                    if(cmbProduct.getValue()==null){ Utils.showAlert("Validación","Debe seleccionar un producto"); return null; }
                    if(cmbPerson.getValue()==null){ Utils.showAlert("Validación","Debe seleccionar una persona"); return null; }
                    if(dpDate.getValue()==null){ Utils.showAlert("Validación","Debe seleccionar una fecha"); return null; }

                    String qText = txtQuantity.getText();

                    if(qText == null || qText.isBlank()) { Utils.showAlert("Validación","Debe ingresar la cantidad"); return null; }
                    if(!qText.matches("\\d+(\\.\\d{0,4})?")) { Utils.showAlert("Validación","Formato de cantidad inválido"); return null; }

                    double qty = Double.parseDouble(qText);

                    if(qty <= 0){ Utils.showAlert("Validación", "La cantidad debe ser mayor que cero"); return null; }

                    Product p = cmbProduct.getValue();

                    // Verificar stock para egreso
                    Double currentStock = p.getDouble("current_stock");
                    if(currentStock != null && currentStock < qty) { Utils.showAlert("Validación","Stock insuficiente. Disponible: "+currentStock); return null; }

                    Transaction t = new Transaction();

                    t.set("product_id", p.getInteger("id"));
                    t.set("quantity", qty);
                    t.set("date", dpDate.getValue().toString());
                    t.set("person_id", cmbPerson.getValue().getInteger("id"));
                    t.set("notes", txtNotes.getText());
                    t.set("user_id", Session.getCurrentUserId());

                    return t;
                } catch(NumberFormatException nf){
                    Utils.showAlert("Error", "La cantidad debe ser un número válido");
                } catch(Exception ex){
                    Logs.error(getClass(), "Error inesperado diálogo egreso (validaciones): {}", ex.getMessage());
                    Utils.showAlert("Error inesperado", "Se produjo un error: "+ex.getMessage());
                }
            }
            return null;
        });
        return d;
    }
}

