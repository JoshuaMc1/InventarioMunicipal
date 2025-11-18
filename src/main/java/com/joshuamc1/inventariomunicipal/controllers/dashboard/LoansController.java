package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
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

        loadTransactions();
    }

    @FXML
    public void onNew() {
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
                loadTransactions();
                Utils.showAlert("Éxito", "Préstamo registrado exitosamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando préstamo: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo registrar el préstamo: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onRefresh() {
        loadTransactions();
    }

    private void loadTransactions() {
        new Thread(() -> {
            try {
                List<Transaction> transactions = transactionService.findByType(Transaction.TYPE_LOAN);
                Platform.runLater(() -> tblTransactions.getItems().setAll(transactions));
            } catch (Throwable t) {
                Logs.error(getClass(), "Error listando préstamos: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar préstamos: " + t.getMessage()));
            }
        }).start();
    }

    private @NotNull Dialog<Transaction> transactionDialog() {
        Dialog<Transaction> d = new Dialog<>();
        d.setResizable(true);
        d.getDialogPane().setPrefSize(960, 460);
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

        // Campos de búsqueda con debounce
        TextField txtSearchProduct = new TextField(); txtSearchProduct.setPromptText("Buscar producto por ID o nombre...");
        TextField txtSearchPerson = new TextField(); txtSearchPerson.setPromptText("Buscar persona por DNI o nombre...");
        PauseTransition productDebounce = new PauseTransition(Duration.millis(150));
        PauseTransition personDebounce = new PauseTransition(Duration.millis(150));

        txtSearchProduct.textProperty().addListener((obs, old, val) -> {
            productDebounce.stop();
            productDebounce.setOnFinished(e -> {
                String q = val == null ? "" : val.trim().toLowerCase();
                filteredProducts.setPredicate(p -> {
                    if(q.isEmpty()) return true;
                    String name = p.getString("name"); Integer id = p.getInteger("id");
                    return (name!=null && name.toLowerCase().contains(q)) || (id!=null && String.valueOf(id).contains(q));
                });
                if(!filteredProducts.contains(cmbProduct.getValue())) cmbProduct.getSelectionModel().clearSelection();
            });
            productDebounce.playFromStart();
        });

        txtSearchPerson.textProperty().addListener((obs, old, val) -> {
            personDebounce.stop();
            personDebounce.setOnFinished(e -> {
                String q = val == null ? "" : val.trim().toLowerCase();
                filteredPeople.setPredicate(p -> {
                    if(q.isEmpty()) return true;
                    String dni=p.getString("dni"); String f=p.getString("first_name"); String l=p.getString("last_name"); String full=p.getFullName();
                    return (dni!=null && dni.toLowerCase().contains(q)) || (f!=null && f.toLowerCase().contains(q)) || (l!=null && l.toLowerCase().contains(q)) || (full!=null && full.toLowerCase().contains(q));
                });
                if(!filteredPeople.contains(cmbPerson.getValue())) cmbPerson.getSelectionModel().clearSelection();
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

        Label lblProduct = new Label("Producto*"); lblProduct.setStyle(lblStyle);
        Label lblSearchProduct = new Label("Filtrar Producto:"); lblSearchProduct.setStyle(lblStyle);
        Label lblQuantity = new Label("Cantidad*"); lblQuantity.setStyle(lblStyle);
        Label lblDate = new Label("Fecha*"); lblDate.setStyle(lblStyle);
        Label lblPerson = new Label("Persona*"); lblPerson.setStyle(lblStyle);
        Label lblSearchPerson = new Label("Filtrar Persona:"); lblSearchPerson.setStyle(lblStyle);
        Label lblNotes = new Label("Notas:"); lblNotes.setStyle(lblStyle);

        GridPane gp = new GridPane();
        gp.setHgap(12); gp.setVgap(12); gp.setPadding(new Insets(14));
        // Fila 0 producto
        gp.add(lblProduct,0,0); gp.add(cmbProduct,1,0); gp.add(lblSearchProduct,2,0); gp.add(txtSearchProduct,3,0);
        // Fila 1 stock
        gp.add(lblStockTitle,0,1); gp.add(lblStockValue,1,1);
        // Fila 2 cantidad
        gp.add(lblQuantity,0,2); gp.add(txtQuantity,1,2);
        // Fila 3 fecha
        gp.add(lblDate,0,3); gp.add(dpDate,1,3);
        // Fila 4 persona
        gp.add(lblPerson,0,4); gp.add(cmbPerson,1,4); gp.add(lblSearchPerson,2,4); gp.add(txtSearchPerson,3,4);
        // Fila 5 notas
        gp.add(lblNotes,0,5); gp.add(txtNotes,1,5,3,1);

        GridPane.setHgrow(cmbProduct, Priority.ALWAYS);
        GridPane.setHgrow(txtSearchProduct, Priority.ALWAYS);
        GridPane.setHgrow(cmbPerson, Priority.ALWAYS);
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
        var productsTask = new Task<List<Product>>() { @Override protected List<Product> call(){ return productService.findAll(); } };
        var peopleTask = new Task<List<Person>>() { @Override protected List<Person> call(){ return personService.findAll(); } };

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

