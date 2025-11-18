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
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class OutcomesController {
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
        colPerson.setCellValueFactory(data -> {
            Integer personId = data.getValue().getInteger("person_id");
            if (personId != null) {
                Person person = personService.findById(personId);
                return new SimpleStringProperty(person != null ? person.getFullName() : "N/A");
            }
            return new SimpleStringProperty("-");
        });
        colNotes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getString("notes")));

        loadTransactions();
    }

    @FXML
    public void onNew() {
        Dialog<Transaction> dialog = transactionDialog();
        dialog.showAndWait().ifPresent(t -> {
            try {
                transactionService.createTransaction(
                    t.getInteger("product_id"),
                    Transaction.TYPE_OUT,
                    t.getDouble("quantity"),
                    t.getString("date"),
                    t.getInteger("person_id"),
                    t.getString("notes")
                );
                loadTransactions();
                Utils.showAlert("Éxito", "Egreso registrado exitosamente");
            } catch (Exception e) {
                Logs.error(getClass(), "Error creando egreso: {}", e.getMessage());
                Utils.showAlert("Error", "No se pudo registrar el egreso: " + e.getMessage());
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
                List<Transaction> transactions = transactionService.findByType(Transaction.TYPE_OUT);
                Platform.runLater(() -> tblTransactions.getItems().setAll(transactions));
            } catch (Throwable t) {
                Logs.error(getClass(), "Error listando egresos: {}", t.getMessage());
                Platform.runLater(() -> Utils.showAlert("Error", "No se pudo cargar egresos: " + t.getMessage()));
            }
        }).start();
    }

    private @NotNull Dialog<Transaction> transactionDialog() {
        Dialog<Transaction> d = new Dialog<>();
        d.setResizable(true);
        d.getDialogPane().setPrefSize(520, 400);
        d.setTitle("Nuevo Egreso");

        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Product> cmbProduct = new ComboBox<>();
        List<Product> products = productService.findAll();
        cmbProduct.setItems(FXCollections.observableArrayList(products));
        cmbProduct.setPromptText("Seleccione producto");
        cmbProduct.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getString("name") + " (Stock: " + item.getDouble("current_stock") + ")");
                }
            }
        });
        cmbProduct.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getString("name") + " (Stock: " + item.getDouble("current_stock") + ")");
                }
            }
        });

        TextField txtQuantity = new TextField();
        txtQuantity.setPromptText("Cantidad");

        DatePicker dpDate = new DatePicker();
        dpDate.setValue(LocalDate.now());

        ComboBox<Person> cmbPerson = new ComboBox<>();
        List<Person> people = personService.findAll();
        cmbPerson.setItems(FXCollections.observableArrayList(people));
        cmbPerson.setPromptText("Seleccione persona (opcional)");
        cmbPerson.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName());
            }
        });
        cmbPerson.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName());
            }
        });

        TextArea txtNotes = new TextArea();
        txtNotes.setPromptText("Notas (opcional)");
        txtNotes.setPrefRowCount(3);

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new Label("Producto"), cmbProduct);
        gp.addRow(1, new Label("Cantidad"), txtQuantity);
        gp.addRow(2, new Label("Fecha"), dpDate);
        gp.addRow(3, new Label("Persona"), cmbPerson);
        gp.addRow(4, new Label("Notas"), txtNotes);

        gp.setPrefWidth(480);
        GridPane.setHgrow(cmbProduct, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtQuantity, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(dpDate, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(cmbPerson, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(txtNotes, javafx.scene.layout.Priority.ALWAYS);

        d.getDialogPane().setContent(gp);

        Node btnSave = d.getDialogPane().lookupButton(save);
        BooleanBinding invalid = cmbProduct.valueProperty().isNull()
                .or(txtQuantity.textProperty().isEmpty());
        btnSave.disableProperty().bind(invalid);

        d.setResultConverter(bt -> {
            if (bt == save) {
                try {
                    double quantity = Double.parseDouble(txtQuantity.getText());
                    Product selectedProduct = cmbProduct.getValue();

                    // Validar que hay suficiente stock
                    if (selectedProduct.getDouble("current_stock") < quantity) {
                        Utils.showAlert("Error", "Stock insuficiente. Disponible: " + selectedProduct.getDouble("current_stock"));
                        return null;
                    }

                    Transaction t = new Transaction();
                    t.set("product_id", selectedProduct.getInteger("id"));
                    t.set("quantity", quantity);
                    t.set("date", dpDate.getValue().toString());
                    if (cmbPerson.getValue() != null) {
                        t.set("person_id", cmbPerson.getValue().getInteger("id"));
                    }
                    t.set("notes", txtNotes.getText());
                    t.set("user_id", Session.getCurrentUserId());
                    return t;
                } catch (NumberFormatException e) {
                    Utils.showAlert("Error", "La cantidad debe ser un número válido");
                    return null;
                }
            }
            return null;
        });

        return d;
    }
}

