package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.AsyncLoadable;
import com.joshuamc1.inventariomunicipal.core.DatabaseManager;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.PermissionHelper;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.models.Product;
import com.joshuamc1.inventariomunicipal.models.Transaction;
import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.services.ProductService;
import com.joshuamc1.inventariomunicipal.services.TransactionService;
import com.joshuamc1.inventariomunicipal.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HomeController implements AsyncLoadable {
    // KPIs
    @FXML private Label lblTotalProductos;
    @FXML private Label lblBajoStock;
    @FXML private Label lblSinStock;
    @FXML private Label lblPrestamosActivos;
    @FXML private Label lblTransaccionesMes;
    @FXML private Label lblTrendMes;

    // Información de contexto
    @FXML private Label lblUltimaActualizacion;
    @FXML private Label lblEstadoSistema;
    @FXML private Button btnRefresh;

    // Gráficos
    @FXML private BarChart<String, Number> barStock;
    @FXML private PieChart pieStock;
    @FXML private LineChart<String, Number> lineMovimientos;
    @FXML private BarChart<String, Number> barMasMovidos;
    @FXML private PieChart pieTipoTransaccion;
    @FXML private BarChart<String, Number> barActividadSemanal;
    @FXML private BarChart<String, Number> barTendenciaTrimestral;

    // Comparativas
    @FXML private Label lblCompTransacciones;
    @FXML private Label lblCompTransaccionesTrend;
    @FXML private Label lblCompProductos;
    @FXML private Label lblCompProductosTrend;
    @FXML private Label lblCompIngresos;
    @FXML private Label lblCompIngresosTrend;

    // Tabla
    @FXML private TableView<Transaction> tblTransacciones;
    @FXML private TableColumn<Transaction, String> colFecha;
    @FXML private TableColumn<Transaction, String> colProducto;
    @FXML private TableColumn<Transaction, String> colTipo;
    @FXML private TableColumn<Transaction, Number> colCantidad;
    @FXML private TableColumn<Transaction, String> colUsuario;

    private final ProductService productService = new ProductService();
    private final TransactionService transactionService = new TransactionService();
    private final UserService userService = new UserService();

    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AtomicReference<Runnable> onLoaded = new AtomicReference<>(null);

    @FXML
    public void initialize() {
        btnRefresh.setOnAction(e -> loadDashboardAsync());
        setupTableColumns();
        configurePermissions();
        loadDashboardAsync();
    }

    /**
     * Configura la visibilidad de elementos según los permisos del usuario
     */
    private void configurePermissions() {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            Logs.warn(getClass(), "No hay usuario en sesión para configurar permisos");
            return;
        }

        Logs.info(getClass(), "Configurando permisos del dashboard para: {}", currentUser.getString("username"));

        // Verificar acceso general al dashboard
        // Si tiene DASHBOARD.READ, puede ver todas las estadísticas (solo observación)
        boolean canViewDashboard = PermissionHelper.canAccess("DASHBOARD", "READ");

        if (!canViewDashboard) {
            Logs.warn(getClass(), "Usuario sin permiso DASHBOARD.READ - acceso denegado");
            // Ocultar todo el dashboard
            hideAllWidgets();
            return;
        }

        // Con DASHBOARD.READ puede ver todas las estadísticas
        Logs.info(getClass(), "Usuario con DASHBOARD.READ - mostrando todas las estadísticas");
    }

    /**
     * Oculta todos los widgets del dashboard
     */
    private void hideAllWidgets() {
        hideInventoryWidgets();
        hideTransactionWidgets();
        Logs.info(getClass(), "Todos los widgets del dashboard ocultos");
    }

    /**
     * Oculta widgets relacionados con inventario
     */
    private void hideInventoryWidgets() {
        if (lblTotalProductos != null) {
            lblTotalProductos.getParent().setVisible(false);
            lblTotalProductos.getParent().setManaged(false);
        }
        if (lblBajoStock != null) {
            lblBajoStock.getParent().setVisible(false);
            lblBajoStock.getParent().setManaged(false);
        }
        if (lblSinStock != null) {
            lblSinStock.getParent().setVisible(false);
            lblSinStock.getParent().setManaged(false);
        }
        if (barStock != null) {
            barStock.setVisible(false);
            barStock.setManaged(false);
        }
        if (pieStock != null) {
            pieStock.setVisible(false);
            pieStock.setManaged(false);
        }
        if (barMasMovidos != null) {
            barMasMovidos.setVisible(false);
            barMasMovidos.setManaged(false);
        }
        Logs.info(getClass(), "Widgets de inventario ocultos");
    }

    /**
     * Oculta widgets relacionados con transacciones
     */
    private void hideTransactionWidgets() {
        if (lblTransaccionesMes != null) {
            lblTransaccionesMes.getParent().setVisible(false);
            lblTransaccionesMes.getParent().setManaged(false);
        }
        if (lblPrestamosActivos != null) {
            lblPrestamosActivos.getParent().setVisible(false);
            lblPrestamosActivos.getParent().setManaged(false);
        }
        if (lineMovimientos != null) {
            lineMovimientos.setVisible(false);
            lineMovimientos.setManaged(false);
        }
        if (pieTipoTransaccion != null) {
            pieTipoTransaccion.setVisible(false);
            pieTipoTransaccion.setManaged(false);
        }
        if (barActividadSemanal != null) {
            barActividadSemanal.setVisible(false);
            barActividadSemanal.setManaged(false);
        }
        if (tblTransacciones != null) {
            tblTransacciones.setVisible(false);
            tblTransacciones.setManaged(false);
        }
        Logs.info(getClass(), "Widgets de transacciones ocultos");
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

    private void setupTableColumns() {
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getString("created_at")));
        colProducto.setCellValueFactory(c -> {
            Product p = c.getValue().parent(Product.class);
            return new SimpleStringProperty(p != null ? p.getString("name") : "N/A");
        });
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(
            com.joshuamc1.inventariomunicipal.core.enums.Transaction.fromDbValue(c.getValue().getString("type")).toString()
        ));
        colCantidad.setCellValueFactory(c -> {
            Double qty = c.getValue().getDouble("quantity");
            return new ReadOnlyObjectWrapper<>(qty != null ? qty : 0.0);
        });
        colUsuario.setCellValueFactory(c -> {
            User u = c.getValue().parent(User.class);
            return new SimpleStringProperty(u != null ? u.getString("name") : "N/A");
        });

        colFecha.setStyle("-fx-font-size: 14;");
        colProducto.setStyle("-fx-font-size: 14;");
        colTipo.setStyle("-fx-font-size: 14;");
        colCantidad.setStyle("-fx-font-size: 14;");
        colUsuario.setStyle("-fx-font-size: 14;");
    }

    private void loadDashboardAsync() {
        if (!loading.compareAndSet(false, true)) {
            Logs.info(HomeController.class, "Dashboard load already in progress, skipping");
            return;
        }

        Task<DashboardData> task = new Task<>() {
            @Override
            protected @Nullable DashboardData call() {
                try {
                    // Envolver TODA la lógica (incluida la verificación de permisos) dentro de la conexión
                    return DatabaseManager.callInConnection(() -> {
                        // Si tiene DASHBOARD.READ, puede ver todas las estadísticas
                        boolean canViewDashboard = PermissionHelper.canAccess("DASHBOARD", "READ");

                        if (!canViewDashboard) {
                            Logs.warn(HomeController.class, "Usuario sin DASHBOARD.READ - no se cargan datos");
                            return new DashboardData(); // Retorna datos vacíos
                        }

                        DashboardData data = new DashboardData();

                        Logs.info(HomeController.class, "Cargando datos completos del dashboard");

                        // KPIs básicos de inventario
                        data.totalProducts = productService.countAll();
                        data.lowStock = productService.countLowStock();
                        data.outOfStock = productService.countOutOfStock();
                        data.products = productService.findAll();
                        data.newProductsThisMonth = productService.countCreatedThisMonth();
                        data.newProductsLastMonth = productService.countCreatedLastMonth();

                        // KPIs de transacciones
                        data.activeLoans = transactionService.countActiveLoans();
                        data.transactionsThisMonth = transactionService.countThisMonth();
                        data.transactionsLastMonth = transactionService.countLastMonth();
                        data.latestTransactions = transactionService.findLatest(15);
                        data.last30DaysTransactions = transactionService.findLast30Days();
                        data.mostMovedProducts = transactionService.getMostMovedProducts(10);
                        data.transactionsByType = transactionService.getTransactionsByType();
                        data.weeklyActivity = transactionService.getWeeklyActivity();
                        data.incomesThisMonth = transactionService.countByTypeThisMonth("in");
                        data.incomesLastMonth = transactionService.countByTypeLastMonth("in");
                        data.quarterlyTrend = transactionService.getQuarterlyTrend();

                        Logs.info(HomeController.class, "Dashboard data loaded successfully");

                        return data;
                    });
                } catch (Exception e) {
                    Logs.error(HomeController.class, "Error loading dashboard data: {}", e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    try {
                        DashboardData data = getValue();
                        updateKPIs(data);
                        updateCharts(data);
                        updateComparatives(data);
                        updateTable(data.latestTransactions);
                        updateContextInfo();
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
                    lblEstadoSistema.setText("● Error de carga");
                    lblEstadoSistema.setStyle("-fx-font-size: 12; -fx-text-fill: #e74c3c;");
                    notifyLoaded();
                });
            }
        };

        Thread th = new Thread(task, "dashboard-loader");
        th.setDaemon(true);
        th.start();
    }

    private void updateKPIs(DashboardData data) {
        lblTotalProductos.setText(String.valueOf(data.totalProducts));
        lblBajoStock.setText(String.valueOf(data.lowStock));
        lblSinStock.setText(String.valueOf(data.outOfStock));
        lblPrestamosActivos.setText(String.valueOf(data.activeLoans));
        lblTransaccionesMes.setText(String.valueOf(data.transactionsThisMonth));

        // Tendencia del mes
        if (data.transactionsLastMonth > 0) {
            double change = ((data.transactionsThisMonth - data.transactionsLastMonth) / (double) data.transactionsLastMonth) * 100;
            if (change > 0) {
                lblTrendMes.setText(String.format("↑ %.1f%%", change));
                lblTrendMes.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            } else if (change < 0) {
                lblTrendMes.setText(String.format("↓ %.1f%%", Math.abs(change)));
                lblTrendMes.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            } else {
                lblTrendMes.setText("→ 0%");
                lblTrendMes.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");
            }
        }
    }

    private void updateCharts(DashboardData data) {
        // 1. BarChart: Top 10 Stock
        XYChart.Series<String, Number> stockSeries = new XYChart.Series<>();
        stockSeries.setName("Stock");
        data.products.stream()
            .sorted((p1, p2) -> {
                Double s1 = p1.getDouble("current_stock");
                Double s2 = p2.getDouble("current_stock");
                if (s1 == null) s1 = 0.0;
                if (s2 == null) s2 = 0.0;
                return Double.compare(s2, s1);
            })
            .limit(10)
            .forEach(p -> {
                String name = p.getString("name");
                Double stock = p.getDouble("current_stock");
                if (name != null && stock != null) {
                    String displayName = name.length() > 15 ? name.substring(0, 12) + "..." : name;
                    stockSeries.getData().add(new XYChart.Data<>(displayName, stock));
                }
            });
        barStock.getData().clear();
        barStock.getData().add(stockSeries);

        // 2. PieChart: Stock Crítico vs Normal
        pieStock.setData(FXCollections.observableArrayList(
            new PieChart.Data("Crítico", data.lowStock),
            new PieChart.Data("Normal", Math.max(0, data.totalProducts - data.lowStock)),
            new PieChart.Data("Sin Stock", data.outOfStock)
        ));

        // 3. LineChart: Movimientos últimos 30 días
        lineMovimientos.getData().clear();
        XYChart.Series<String, Number> ingresosSeries = new XYChart.Series<>();
        ingresosSeries.setName("Ingresos");
        XYChart.Series<String, Number> egresosSeries = new XYChart.Series<>();
        egresosSeries.setName("Egresos");
        XYChart.Series<String, Number> prestamosSeries = new XYChart.Series<>();
        prestamosSeries.setName("Préstamos");

        Map<String, Map<String, Long>> dailyData = groupTransactionsByDate(data.last30DaysTransactions);
        dailyData.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String date = entry.getKey();
                Map<String, Long> types = entry.getValue();
                ingresosSeries.getData().add(new XYChart.Data<>(date, types.getOrDefault("in", 0L)));
                egresosSeries.getData().add(new XYChart.Data<>(date, types.getOrDefault("out", 0L)));
                prestamosSeries.getData().add(new XYChart.Data<>(date, types.getOrDefault("loan", 0L)));
            });

        lineMovimientos.getData().addAll(ingresosSeries, egresosSeries, prestamosSeries);

        // 4. BarChart: Productos Más Movidos
        XYChart.Series<String, Number> movidosSeries = new XYChart.Series<>();
        movidosSeries.setName("Movimientos");
        data.mostMovedProducts.forEach((productName, count) -> {
            String displayName = productName.length() > 15 ? productName.substring(0, 12) + "..." : productName;
            movidosSeries.getData().add(new XYChart.Data<>(displayName, count));
        });
        barMasMovidos.getData().clear();
        barMasMovidos.getData().add(movidosSeries);

        // 5. PieChart: Distribución por Tipo
        pieTipoTransaccion.setData(FXCollections.observableArrayList(
            data.transactionsByType.entrySet().stream()
                .map(e -> new PieChart.Data(
                    com.joshuamc1.inventariomunicipal.core.enums.Transaction.fromDbValue(e.getKey()).getDisplayName(),
                    e.getValue()
                ))
                .collect(java.util.stream.Collectors.toList())
        ));

        // 6. BarChart: Actividad Semanal
        XYChart.Series<String, Number> weeklySeries = new XYChart.Series<>();
        weeklySeries.setName("Transacciones");
        String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        for (int i = 0; i < 7; i++) {
            weeklySeries.getData().add(new XYChart.Data<>(dias[i], data.weeklyActivity.getOrDefault(i + 1, 0L)));
        }
        barActividadSemanal.getData().clear();
        barActividadSemanal.getData().add(weeklySeries);

        // 7. BarChart: Tendencia Trimestral
        barTendenciaTrimestral.getData().clear();
        XYChart.Series<String, Number> trimIngresos = new XYChart.Series<>();
        trimIngresos.setName("Ingresos");
        XYChart.Series<String, Number> trimEgresos = new XYChart.Series<>();
        trimEgresos.setName("Egresos");
        XYChart.Series<String, Number> trimPrestamos = new XYChart.Series<>();
        trimPrestamos.setName("Préstamos");

        data.quarterlyTrend.forEach((month, types) -> {
            trimIngresos.getData().add(new XYChart.Data<>(month, types.getOrDefault("in", 0L)));
            trimEgresos.getData().add(new XYChart.Data<>(month, types.getOrDefault("out", 0L)));
            trimPrestamos.getData().add(new XYChart.Data<>(month, types.getOrDefault("loan", 0L)));
        });

        barTendenciaTrimestral.getData().addAll(trimIngresos, trimEgresos, trimPrestamos);
    }

    private void updateComparatives(DashboardData data) {
        // Transacciones
        lblCompTransacciones.setText(data.transactionsThisMonth + " vs " + data.transactionsLastMonth);
        setTrendLabel(lblCompTransaccionesTrend, data.transactionsThisMonth, data.transactionsLastMonth);

        // Productos
        lblCompProductos.setText(data.newProductsThisMonth + " vs " + data.newProductsLastMonth);
        setTrendLabel(lblCompProductosTrend, data.newProductsThisMonth, data.newProductsLastMonth);

        // Ingresos
        lblCompIngresos.setText(data.incomesThisMonth + " vs " + data.incomesLastMonth);
        setTrendLabel(lblCompIngresosTrend, data.incomesThisMonth, data.incomesLastMonth);
    }

    private void setTrendLabel(Label label, long current, long previous) {
        if (previous == 0) {
            label.setText(current > 0 ? "↑ Nuevo" : "→");
            label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3498db;");
            return;
        }

        double change = ((current - previous) / (double) previous) * 100;
        if (change > 0) {
            label.setText(String.format("↑ %.1f%%", change));
            label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else if (change < 0) {
            label.setText(String.format("↓ %.1f%%", Math.abs(change)));
            label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        } else {
            label.setText("→ 0%");
            label.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");
        }
    }

    private void updateTable(List<Transaction> transactions) {
        tblTransacciones.setItems(FXCollections.observableArrayList(transactions));
    }

    private void updateContextInfo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        lblUltimaActualizacion.setText("Última actualización: " + timestamp);
        lblEstadoSistema.setText("● Sistema OK");
        lblEstadoSistema.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60;");
    }

    private Map<String, Map<String, Long>> groupTransactionsByDate(List<Transaction> transactions) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateKey = date.format(DateTimeFormatter.ofPattern("dd/MM"));
            result.put(dateKey, new HashMap<>());
        }

        transactions.forEach(t -> {
            String createdAt = t.getString("created_at");
            if (createdAt != null && createdAt.length() >= 10) {
                try {
                    LocalDate date = LocalDate.parse(createdAt.substring(0, 10));
                    String dateKey = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                    String type = t.getString("type");

                    result.computeIfAbsent(dateKey, k -> new HashMap<>());
                    result.get(dateKey).merge(type, 1L, Long::sum);
                } catch (Exception e) {
                    Logs.warn(HomeController.class, "Error parsing date: {}", createdAt);
                }
            }
        });

        return result;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Clase interna para datos del dashboard
    private static class DashboardData {
        long totalProducts;
        long lowStock;
        long outOfStock;
        long activeLoans;
        long transactionsThisMonth;
        long transactionsLastMonth;
        long newProductsThisMonth;
        long newProductsLastMonth;
        long incomesThisMonth;
        long incomesLastMonth;

        List<Product> products;
        List<Transaction> latestTransactions;
        List<Transaction> last30DaysTransactions;
        Map<String, Long> mostMovedProducts;
        Map<String, Long> transactionsByType;
        Map<Integer, Long> weeklyActivity;
        Map<String, Map<String, Long>> quarterlyTrend;
    }
}
