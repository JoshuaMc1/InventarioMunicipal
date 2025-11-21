package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.PermissionHelper;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.core.Utils;
import com.joshuamc1.inventariomunicipal.core.enums.Units;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.services.ProductService;
import com.joshuamc1.inventariomunicipal.config.DatabaseConfig;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    @FXML private TableColumn<Product, String> colDescription;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, Double> colStock;
    @FXML private TableColumn<Product, Double> colMinStock;

    @FXML private Button btnRefresh;
    @FXML private Button btnNew;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private TextField txtSearch;
    @FXML private Button btnClearSearch;
    // Controles de paginación
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private ChoiceBox<Integer> choicePageSize;

    // Estado de paginación
    private int currentPage = 1; // 1-based
    private int pageSize = 25;
    private int totalItems = 0;
    private int totalPages = 1;

    private List<Product> allProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> currentPageItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        configurePermissions(); // Agregar configuración de permisos
        loadProducts();

        btnRefresh.setOnAction(e -> loadProducts());
        btnNew.setOnAction(e -> openCreateDialog());
        btnEdit.setOnAction(e -> openEditDialog());
        btnDelete.setOnAction(e -> deleteSelected());

        // Búsqueda en tiempo real
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 1;
            applyFiltersAndPagination();
        });
        btnClearSearch.setOnAction(e -> {
            txtSearch.clear();
            currentPage = 1;
            applyFiltersAndPagination();
        });

        // Inicializar paginación
        choicePageSize.setItems(FXCollections.observableArrayList(10,25,50,100));
        choicePageSize.setValue(pageSize);
        choicePageSize.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                pageSize = newV;
                currentPage = 1;
                applyFiltersAndPagination();
            }
        });
        btnPrev.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                applyFiltersAndPagination();
            }
        });
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                applyFiltersAndPagination();
            }
        });
    }

    /**
     * Configura la visibilidad de los botones según los permisos del usuario
     */
    private void configurePermissions() {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            Logs.warn(getClass(), "No hay usuario en sesión para configurar permisos");
            return;
        }

        Logs.info(getClass(), "Configurando permisos de inventario para: {}",
            currentUser.getString("username"));

        // Botón Nuevo - Requiere CREATE
        PermissionHelper.hideIfNoPermission(btnNew, "INVENTARIO", "CREATE");

        // Botón Editar - Requiere UPDATE
        PermissionHelper.hideIfNoPermission(btnEdit, "INVENTARIO", "UPDATE");

        // Botón Eliminar - Requiere DELETE
        PermissionHelper.hideIfNoPermission(btnDelete, "INVENTARIO", "DELETE");

        // Si solo tiene READ, la tabla es de solo lectura
        boolean hasOnlyRead = PermissionHelper.canAccess("INVENTARIO", "READ") &&
                             !PermissionHelper.canAccess("INVENTARIO", "UPDATE") &&
                             !PermissionHelper.canAccess("INVENTARIO", "DELETE");

        if (hasOnlyRead) {
            Logs.info(getClass(), "Usuario con acceso de solo lectura al inventario");
        }

        Logs.info(getClass(), "Permisos de inventario configurados");
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> {
            Integer id = c.getValue().getInteger("id");
            return new SimpleIntegerProperty(id == null ? 0 : id).asObject();
        });

        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getString("name")));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getString("description")));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(Units.fromDbValue(c.getValue().getString("unit")).getDisplayName()));
        colStock.setCellValueFactory(c -> {
            Double v = c.getValue().getDouble("current_stock");
            return new SimpleDoubleProperty(v == null ? 0.0 : v).asObject();
        });
        colMinStock.setCellValueFactory(c -> {
            Double v = c.getValue().getDouble("minimum_stock");
            return new SimpleDoubleProperty(v == null ? 0.0 : v).asObject();
        });

        colId.setStyle("-fx-font-size: 17;");
        colName.setStyle("-fx-font-size: 17;");
        colDescription.setStyle("-fx-font-size: 17;");
        colUnit.setStyle("-fx-font-size: 17;");
        colStock.setStyle("-fx-font-size: 17;");
        colMinStock.setStyle("-fx-font-size: 17;");

        tblProducts.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadProducts() {
        Task<List<Product>> task = new Task<>() {
            @Override
            protected java.util.List<Product> call() {
                // Asegurar que el hilo que ejecuta la tarea tenga una conexión abierta
                // para operaciones ActiveJDBC que puedan no usar DatabaseManager internamente.
                DatabaseConfig.openConnection();
                try {
                    return productService.findAll();
                } finally {
                    DatabaseConfig.closeConnection();
                }
            }
        };

        task.setOnSucceeded(evt -> {
            allProducts = task.getValue();
            currentPage = 1;
            applyFiltersAndPagination();
        });
        task.setOnFailed(evt -> {
            Throwable t = task.getException();
            Utils.showAlert("Error", "No se pudieron cargar los productos: " + (t == null ? "unknown" : t.getMessage()));
        });

        Thread th = new Thread(task, "inventory-loader");
        th.setDaemon(true);
        th.start();
    }

    private void applyFiltersAndPagination() {
        String searchText = txtSearch.getText();
        List<Product> filtered;
        if (searchText == null || searchText.trim().isEmpty()) {
            filtered = allProducts;
        } else {
            String search = searchText.trim().toLowerCase();
            filtered = allProducts.stream()
                .filter(p -> {
                    String name = p.getString("name");
                    String id = String.valueOf(p.getInteger("id"));
                    return (name != null && name.toLowerCase().contains(search)) || (id.contains(search));
                })
                .collect(java.util.stream.Collectors.toList());
        }
        totalItems = filtered.size();
        totalPages = Math.max(1, (int)Math.ceil(totalItems / (double)pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<Product> pageSlice = fromIndex >= toIndex ? java.util.Collections.emptyList() : filtered.subList(fromIndex, toIndex);
        currentPageItems.setAll(pageSlice);
        tblProducts.setItems(currentPageItems);
        updatePaginationControls();
    }

    private void updatePaginationControls() {
        if (lblPage != null) lblPage.setText(String.format("Página %d/%d (%d)", currentPage, totalPages, totalItems));
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages);
    }

    private void openCreateDialog() {
        // Validar permiso antes de abrir el diálogo
        if (!PermissionHelper.validateAndShow("INVENTARIO", "CREATE")) {
            return;
        }

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

        // Agregar estilos a los labels ["-fx-font-weight: bold; -fx-font-size: 17;"] y campos de entrada ["-fx-font-weight: semibold; -fx-font-size: 17;"]
        txtName.setStyle("-fx-font-size: 17;");
        txtDesc.setStyle("-fx-font-size: 17;");
        cbUnit.setStyle("-fx-font-size: 17;");
        txtCurrentStock.setStyle("-fx-font-size: 17;");
        txtMinStock.setStyle("-fx-font-size: 17;");

        Label lblName = new Label("Nombre:");
        Label lblDescription = new Label("Descripción:");
        Label lblUnit = new Label("Unidad:");
        Label lblCurrentStock = new Label("Stock actual:");
        Label lblMinStock = new Label("Stock mínimo:");

        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblDescription.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblUnit.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblCurrentStock.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblMinStock.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(lblName, 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(lblDescription, 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(lblUnit, 0, 2);
        grid.add(cbUnit, 1, 2);
        grid.add(lblCurrentStock, 0, 3);
        grid.add(txtCurrentStock, 1, 3);
        grid.add(lblMinStock, 0, 4);
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
        result.ifPresent(p -> {
            Utils.showAlert("Éxito", "Producto creado correctamente");

            loadProducts();
        });
    }

    private void openEditDialog() {
        // Validar permiso antes de abrir el diálogo
        if (!PermissionHelper.validateAndShow("INVENTARIO", "UPDATE")) {
            return;
        }

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

        txtName.setStyle("-fx-font-size: 17;");
        txtDesc.setStyle("-fx-font-size: 17;");
        cbUnit.setStyle("-fx-font-size: 17;");
        txtCurrentStock.setStyle("-fx-font-size: 17;");
        txtMinStock.setStyle("-fx-font-size: 17;");

        Label lblName = new Label("Nombre:");
        Label lblDescription = new Label("Descripción:");
        Label lblUnit = new Label("Unidad:");
        Label lblCurrentStock = new Label("Stock actual:");
        Label lblMinStock = new Label("Stock mínimo:");

        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblDescription.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblUnit.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblCurrentStock.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
        lblMinStock.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(lblName, 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(lblDescription, 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(lblUnit, 0, 2);
        grid.add(cbUnit, 1, 2);
        grid.add(lblCurrentStock, 0, 3);
        grid.add(txtCurrentStock, 1, 3);
        grid.add(lblMinStock, 0, 4);
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
        // Validar permiso antes de eliminar
        if (!PermissionHelper.validateAndShow("INVENTARIO", "DELETE")) {
            return;
        }

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
