package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.core.Units;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.services.ProductService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

public class InventoryController {
    private final ProductService productService = new ProductService();

    @FXML private TableView<Product> tblProducts;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, Double> colStock;
    @FXML private TableColumn<Product, Double> colMinStock;

    @FXML private Button btnRefresh;
    @FXML private Button btnNew;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    @FXML
    public void initialize() {
        setupTable();
        loadProducts();

        btnRefresh.setOnAction(e -> loadProducts());
        btnNew.setOnAction(e -> openCreateDialog());
        btnEdit.setOnAction(e -> openEditDialog());
        btnDelete.setOnAction(e -> deleteSelected());
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> {
            Integer id = c.getValue().getInteger("id");
            return new SimpleIntegerProperty(id == null ? 0 : id).asObject();
        });

        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getString("name")));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(Units.fromDbValue(c.getValue().getString("unit")).getDisplayName()));
        colStock.setCellValueFactory(c -> {
            Double v = c.getValue().getDouble("current_stock");
            return new SimpleDoubleProperty(v == null ? 0.0 : v).asObject();
        });
        colMinStock.setCellValueFactory(c -> {
            Double v = c.getValue().getDouble("minimum_stock");
            return new SimpleDoubleProperty(v == null ? 0.0 : v).asObject();
        });

        tblProducts.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadProducts() {
        Task<List<Product>> task = new Task<>() {
            @Override
            protected java.util.List<Product> call() {
                return productService.findAll();
            }
        };

        task.setOnSucceeded(evt -> tblProducts.setItems(FXCollections.observableArrayList(task.getValue())));
        task.setOnFailed(evt -> {
            Throwable t = task.getException();
            Utils.showAlert("Error", "No se pudieron cargar los productos: " + (t == null ? "unknown" : t.getMessage()));
        });

        Thread th = new Thread(task, "inventory-loader");
        th.setDaemon(true);
        th.start();
    }

    private void openCreateDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Nuevo producto");

        ButtonType createBtn = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtn, ButtonType.CANCEL);

        TextField txtName = new TextField();
        TextArea txtDesc = new TextArea();
        ComboBox<Units> cbUnit = new ComboBox<>(FXCollections.observableArrayList(Units.values()));
        cbUnit.setValue(Units.UNIDAD);
        TextField txtCurrentStock = new TextField("0");
        TextField txtMinStock = new TextField();

        txtDesc.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(new Label("Unidad:"), 0, 2);
        grid.add(cbUnit, 1, 2);
        grid.add(new Label("Stock actual:"), 0, 3);
        grid.add(txtCurrentStock, 1, 3);
        grid.add(new Label("Stock mínimo:"), 0, 4);
        grid.add(txtMinStock, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == createBtn) {
                String name = txtName.getText().trim();
                String desc = txtDesc.getText().trim();
                Units unit = cbUnit.getValue();
                double min = 0.0;
                double current = 0.0;

                try {
                    current = Double.parseDouble(txtCurrentStock.getText().trim());
                } catch (Exception ignored) {}

                try {
                    min = Double.parseDouble(txtMinStock.getText().trim());
                } catch (Exception ignored) {}

                if (name.isEmpty()) {
                    Utils.showAlert("Error", "El nombre es requerido");
                    return null;
                }

                return productService.createProduct(name, desc, unit == null ? Units.OTROS.getDbValue() : unit.getDbValue(), current, min);
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(p -> loadProducts());
    }

    private void openEditDialog() {
        Product sel = tblProducts.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Utils.showAlert("Info", "Seleccione un producto para editar");
            return;
        }

        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Editar producto");

        ButtonType saveBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField txtName = new TextField(sel.getString("name"));
        TextArea txtDesc = new TextArea(sel.getString("description"));
        ComboBox<Units> cbUnit = new ComboBox<>(FXCollections.observableArrayList(Units.values()));
        cbUnit.setValue(Units.fromDbValue(sel.getString("unit")));
        TextField txtCurrentStock = new TextField(String.valueOf(sel.getDouble("current_stock") == null ? 0.0 : sel.getDouble("current_stock")));
        TextField txtMinStock = new TextField(String.valueOf(sel.getDouble("minimum_stock")));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(new Label("Unidad:"), 0, 2);
        grid.add(cbUnit, 1, 2);
        grid.add(new Label("Stock actual:"), 0, 3);
        grid.add(txtCurrentStock, 1, 3);
        grid.add(new Label("Stock mínimo:"), 0, 4);
        grid.add(txtMinStock, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                String name = txtName.getText().trim();
                String desc = txtDesc.getText().trim();
                Units unit = cbUnit.getValue();

                double min = sel.getDouble("minimum_stock");
                double current = sel.getDouble("current_stock") == null ? 0.0 : sel.getDouble("current_stock");

                try {
                    current = Double.parseDouble(txtCurrentStock.getText().trim());
                } catch (Exception ignored) {}

                try {
                    min = Double.parseDouble(txtMinStock.getText().trim());
                } catch (Exception ignored) {}

                if (name.isEmpty()) {
                    Utils.showAlert("Error", "El nombre es requerido");
                    return null;
                }

                sel.set("name", name);
                sel.set("description", desc);
                sel.set("unit", unit == null ? Units.OTROS.getDbValue() : unit.getDbValue());
                sel.set("minimum_stock", min);
                sel.set("current_stock", current);

                productService.save(sel);

                return sel;
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(p -> loadProducts());
    }

    private void deleteSelected() {
        Product sel = tblProducts.getSelectionModel().getSelectedItem();

        if (sel == null) {
            Utils.showAlert("Info", "Seleccione un producto para eliminar");
            return;
        }

        boolean ok = Utils.confirmAction("Confirmar", "¿Desea eliminar el producto: " + sel.getString("name") + "?");
        if (!ok) return;

        productService.delete(sel);

        loadProducts();
    }
}
