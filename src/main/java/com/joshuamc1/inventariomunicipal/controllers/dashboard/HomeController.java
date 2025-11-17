package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.AsyncLoadable;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.Transaction;
import com.joshuamc1.inventariomunicipal.services.ProductService;
import com.joshuamc1.inventariomunicipal.services.TransactionService;
import com.joshuamc1.inventariomunicipal.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HomeController implements AsyncLoadable {
    @FXML private Label lblTotalProductos;
    @FXML private Label lblBajoStock;
    @FXML private Label lblMovimientosHoy;
    @FXML private Label lblUsuarios;

    @FXML private BarChart<String, Number> barStock;
    @FXML private PieChart pieStock;

    @FXML private TableView<Transaction> tblTransacciones;
    @FXML private TableColumn<Transaction, String> colFecha;
    @FXML private TableColumn<Transaction, String> colProducto;
    @FXML private TableColumn<Transaction, String> colTipo;
    @FXML private TableColumn<Transaction, Number> colCantidad;

    private final ProductService productService = new ProductService();
    private final TransactionService transactionService = new TransactionService();
    private final UserService userService = new UserService();

    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicReference<Runnable> onLoaded = new AtomicReference<>(null);

    @FXML
    public void initialize() {
        loadDashboardAsync();
    }

    @Override
    public boolean isLoading() {
        return loading.get();
    }

    @Override
    public void setOnLoaded(Runnable callback) {
        this.onLoaded.set(callback);
    }

    private void notifyLoaded() {
        Runnable cb = onLoaded.getAndSet(null);
        if (cb != null) {
            try {
                cb.run();
            } catch (Exception e) {
                Logs.error(HomeController.class, "onLoaded callback failed: {}", e.getMessage());
            }
        }
    }

    private void loadDashboardAsync() {
        if (!loading.compareAndSet(false, true)) {
            Logs.info(HomeController.class, "Dashboard load already in progress, skipping");
            return;
        }

        Task<Void> task = new Task<>() {
            private long totalProducts;
            private long lowStock;
            private long movementsToday;
            private long usersCount;
            private List<Product> products;
            private List<Transaction> latestTransactions;

            @Override
            protected @Nullable Void call() {
                try {
                    totalProducts = productService.countAll();
                    lowStock = productService.countLowStock();
                    movementsToday = transactionService.countToday();
                    usersCount = userService.countUsers();

                    products = productService.findAll();
                    latestTransactions = transactionService.findLatest(10);

                    return null;
                } catch (Exception e) {
                    Logs.error(HomeController.class, "Error loading dashboard data: {}", e.getMessage());
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    try {
                        lblTotalProductos.setText(String.valueOf(totalProducts));
                        lblBajoStock.setText(String.valueOf(lowStock));
                        lblMovimientosHoy.setText(String.valueOf(movementsToday));
                        lblUsuarios.setText(String.valueOf(usersCount));

                        XYChart.Series<String, Number> series = new XYChart.Series<>();
                        series.setName("Stock");
                        series.getData().clear();
                        for (Product p : products) {
                            series.getData().add(new XYChart.Data<>(p.getString("name"), p.getDouble("current_stock")));
                        }
                        barStock.getData().clear();
                        barStock.getData().add(series);

                        pieStock.setData(FXCollections.observableArrayList(
                                new PieChart.Data("Crítico", lowStock),
                                new PieChart.Data("Normal", Math.max(0, totalProducts - lowStock))
                        ));

                        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getString("created_at")));
                        colProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().parent(Product.class).getString("name")));
                        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getString("type")));
                        colCantidad.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getDouble("quantity")));

                        tblTransacciones.setItems(FXCollections.observableArrayList(latestTransactions));

                    } finally {
                        loading.set(false);
                        notifyLoaded();
                    }
                });
            }

            @Override
            protected void failed() {
                loading.set(false);
                Platform.runLater(() -> {
                    Throwable e = getException();
                    Logs.error(HomeController.class, "Dashboard load failed: {}", e == null ? "unknown" : e.getMessage());
                    notifyLoaded();
                });
            }
        };

        Thread th = new Thread(task, "dashboard-loader");
        th.setDaemon(true);
        th.start();
    }
}
