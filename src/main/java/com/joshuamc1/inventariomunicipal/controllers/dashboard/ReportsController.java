package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.DateRangeDialog;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.core.Roles;
import com.joshuamc1.inventariomunicipal.core.Session;
import com.joshuamc1.inventariomunicipal.core.TransactionTypeDialog;
import com.joshuamc1.inventariomunicipal.core.UserSelectionDialog;
import com.joshuamc1.inventariomunicipal.core.PersonSelectionDialog;
import com.joshuamc1.inventariomunicipal.core.MonthYearDialog;
import com.joshuamc1.inventariomunicipal.models.Report;
import com.joshuamc1.inventariomunicipal.services.ReportService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportsController {
    // Botones de Usuarios
    @FXML private Button btnUsersNoRootCsv;
    @FXML private Button btnUsersNoRootPdf;
    @FXML private Button btnUsersGroupedCsv;
    @FXML private Button btnUsersGroupedPdf;

    // Botones de Inventario
    @FXML private Button btnInventoryCompleteCsv;
    @FXML private Button btnInventoryCompletePdf;
    @FXML private Button btnInventoryLowStockCsv;
    @FXML private Button btnInventoryLowStockPdf;
    @FXML private Button btnInventoryOutOfStockCsv;
    @FXML private Button btnInventoryOutOfStockPdf;
    @FXML private Button btnInventoryMovementsCsv;
    @FXML private Button btnInventoryMovementsPdf;
    @FXML private Button btnMovementsByDateCsv;
    @FXML private Button btnMovementsByDatePdf;
    @FXML private Button btnProductsByDateCsv;
    @FXML private Button btnProductsByDatePdf;

    // Botones de Transacciones
    @FXML private Button btnTransactionsAllCsv;
    @FXML private Button btnTransactionsAllPdf;
    @FXML private Button btnTransactionsByTypeCsv;
    @FXML private Button btnTransactionsByTypePdf;
    @FXML private Button btnTransactionsByTypeAndDateCsv;
    @FXML private Button btnTransactionsByTypeAndDatePdf;
    @FXML private Button btnTransactionsByUserCsv;
    @FXML private Button btnTransactionsByUserPdf;
    @FXML private Button btnTransactionsByPersonCsv;
    @FXML private Button btnTransactionsByPersonPdf;

    // Botones de Reportes Analíticos
    @FXML private Button btnProductsMostMovedCsv;
    @FXML private Button btnProductsMostMovedPdf;
    @FXML private Button btnPeopleWithLoansCsv;
    @FXML private Button btnPeopleWithLoansPdf;
    @FXML private Button btnMonthlyConsumptionCsv;
    @FXML private Button btnMonthlyConsumptionPdf;
    @FXML private Button btnProductsNoMovementCsv;
    @FXML private Button btnProductsNoMovementPdf;
    @FXML private Button btnUserActivityCsv;
    @FXML private Button btnUserActivityPdf;
    @FXML private Button btnMonthlyDashboardCsv;
    @FXML private Button btnMonthlyDashboardPdf;

    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        // Reportes de Usuarios sin Root
        btnUsersNoRootCsv.setOnAction(e -> exportUsersExcludingRootCsv());
        btnUsersNoRootPdf.setOnAction(e -> exportUsersExcludingRootPdf());

        // Reportes de Usuarios Agrupados por Rol
        btnUsersGroupedCsv.setOnAction(e -> exportUsersGroupedCsv());
        btnUsersGroupedPdf.setOnAction(e -> exportUsersGroupedPdf());

        // Reportes de Inventario Completo
        btnInventoryCompleteCsv.setOnAction(e -> exportInventoryCompleteCsv());
        btnInventoryCompletePdf.setOnAction(e -> exportInventoryCompletePdf());

        // Reportes de Inventario con Stock Bajo
        btnInventoryLowStockCsv.setOnAction(e -> exportInventoryLowStockCsv());
        btnInventoryLowStockPdf.setOnAction(e -> exportInventoryLowStockPdf());

        // Reportes de Inventario Sin Stock
        btnInventoryOutOfStockCsv.setOnAction(e -> exportInventoryOutOfStockCsv());
        btnInventoryOutOfStockPdf.setOnAction(e -> exportInventoryOutOfStockPdf());

        // Reportes de Movimientos de Inventario
        btnInventoryMovementsCsv.setOnAction(e -> exportInventoryMovementsCsv());
        btnInventoryMovementsPdf.setOnAction(e -> exportInventoryMovementsPdf());

        // Reportes con Rango de Fechas Personalizado
        btnMovementsByDateCsv.setOnAction(e -> exportMovementsByDateRangeCsv());
        btnMovementsByDatePdf.setOnAction(e -> exportMovementsByDateRangePdf());
        btnProductsByDateCsv.setOnAction(e -> exportProductsByDateRangeCsv());
        btnProductsByDatePdf.setOnAction(e -> exportProductsByDateRangePdf());

        // Sección: Transacciones
        btnTransactionsAllCsv.setOnAction(e -> exportTransactionsAllCsv());
        btnTransactionsAllPdf.setOnAction(e -> exportTransactionsAllPdf());
        btnTransactionsByTypeCsv.setOnAction(e -> exportTransactionsByTypeCsv());
        btnTransactionsByTypePdf.setOnAction(e -> exportTransactionsByTypePdf());
        btnTransactionsByTypeAndDateCsv.setOnAction(e -> exportTransactionsByTypeAndDateCsv());
        btnTransactionsByTypeAndDatePdf.setOnAction(e -> exportTransactionsByTypeAndDatePdf());
        btnTransactionsByUserCsv.setOnAction(e -> exportTransactionsByUserCsv());
        btnTransactionsByUserPdf.setOnAction(e -> exportTransactionsByUserPdf());
        btnTransactionsByPersonCsv.setOnAction(e -> exportTransactionsByPersonCsv());
        btnTransactionsByPersonPdf.setOnAction(e -> exportTransactionsByPersonPdf());

        // Reportes Analíticos
        btnProductsMostMovedCsv.setOnAction(e -> exportProductsMostMovedCsv());
        btnProductsMostMovedPdf.setOnAction(e -> exportProductsMostMovedPdf());
        btnPeopleWithLoansCsv.setOnAction(e -> exportPeopleWithLoansCsv());
        btnPeopleWithLoansPdf.setOnAction(e -> exportPeopleWithLoansPdf());
        btnMonthlyConsumptionCsv.setOnAction(e -> exportMonthlyConsumptionCsv());
        btnMonthlyConsumptionPdf.setOnAction(e -> exportMonthlyConsumptionPdf());
        btnProductsNoMovementCsv.setOnAction(e -> exportProductsNoMovementCsv());
        btnProductsNoMovementPdf.setOnAction(e -> exportProductsNoMovementPdf());
        btnUserActivityCsv.setOnAction(e -> exportUserActivityCsv());
        btnUserActivityPdf.setOnAction(e -> exportUserActivityPdf());
        btnMonthlyDashboardCsv.setOnAction(e -> exportMonthlyDashboardCsv());
        btnMonthlyDashboardPdf.setOnAction(e -> exportMonthlyDashboardPdf());
    }

    // ==================== USUARIOS SIN ROOT ====================

    private void exportUsersExcludingRootCsv() {
        try {
            Report report = reportService.usersExcludingRoot();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Usuarios (sin Root) - CSV");
            fc.setInitialFileName("usuarios_sin_root_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnUsersNoRootCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando usuarios sin root a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportUsersExcludingRootPdf() {
        try {
            Report report = reportService.usersExcludingRoot();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Usuarios (sin Root) - PDF");
            fc.setInitialFileName("usuarios_sin_root_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnUsersNoRootPdf.getScene().getWindow());
            if (f == null) return;

            String description = "Lista completa de todos los usuarios del sistema, excluyendo el usuario Root por seguridad.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando usuarios sin root a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== USUARIOS AGRUPADOS POR ROL ====================

    private void exportUsersGroupedCsv() {
        try {
            Report report = reportService.usersGroupedByRole();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Usuarios Agrupados por Rol - CSV");
            fc.setInitialFileName("usuarios_por_rol_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnUsersGroupedCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando usuarios agrupados a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportUsersGroupedPdf() {
        try {
            Report report = reportService.usersGroupedByRole();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Usuarios Agrupados por Rol - PDF");
            fc.setInitialFileName("usuarios_por_rol_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnUsersGroupedPdf.getScene().getWindow());
            if (f == null) return;

            String description = "Reporte de usuarios organizados y agrupados por su rol asignado en el sistema.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando usuarios agrupados a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== INVENTARIO COMPLETO ====================

    private void exportInventoryCompleteCsv() {
        try {
            Report report = reportService.inventoryComplete();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Inventario Completo - CSV");
            fc.setInitialFileName("inventario_completo_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnInventoryCompleteCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando inventario completo a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportInventoryCompletePdf() {
        try {
            Report report = reportService.inventoryComplete();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Inventario Completo - PDF");
            fc.setInitialFileName("inventario_completo_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnInventoryCompletePdf.getScene().getWindow());
            if (f == null) return;

            String description = "Listado completo de todos los productos registrados en el sistema con su stock actual y mínimo.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando inventario completo a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== PRODUCTOS CON STOCK BAJO ====================

    private void exportInventoryLowStockCsv() {
        try {
            Report report = reportService.inventoryLowStockReport();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos con Stock Bajo - CSV");
            fc.setInitialFileName("productos_stock_bajo_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnInventoryLowStockCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos con stock bajo a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportInventoryLowStockPdf() {
        try {
            Report report = reportService.inventoryLowStockReport();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos con Stock Bajo - PDF");
            fc.setInitialFileName("productos_stock_bajo_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnInventoryLowStockPdf.getScene().getWindow());
            if (f == null) return;

            String description = "Productos cuyo stock actual es igual o menor al stock mínimo establecido. Ideal para planificar compras.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos con stock bajo a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== PRODUCTOS SIN STOCK ====================

    private void exportInventoryOutOfStockCsv() {
        try {
            Report report = reportService.inventoryOutOfStock();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos Sin Stock - CSV");
            fc.setInitialFileName("productos_sin_stock_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnInventoryOutOfStockCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos sin stock a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportInventoryOutOfStockPdf() {
        try {
            Report report = reportService.inventoryOutOfStock();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos Sin Stock - PDF");
            fc.setInitialFileName("productos_sin_stock_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnInventoryOutOfStockPdf.getScene().getWindow());
            if (f == null) return;

            String description = "Productos que actualmente tienen stock en cero. Requieren reabastecimiento urgente.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos sin stock a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== MOVIMIENTOS DE INVENTARIO ====================

    private void exportInventoryMovementsCsv() {
        try {
            Report report = reportService.inventoryMovements();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Movimientos de Inventario - CSV");
            fc.setInitialFileName("movimientos_inventario_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnInventoryMovementsCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando movimientos de inventario a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportInventoryMovementsPdf() {
        try {
            Report report = reportService.inventoryMovements();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Movimientos de Inventario - PDF");
            fc.setInitialFileName("movimientos_inventario_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnInventoryMovementsPdf.getScene().getWindow());
            if (f == null) return;

            String description = "Historial de todas las transacciones: ingresos, egresos y préstamos realizados en el último mes.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando movimientos de inventario a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== MOVIMIENTOS POR RANGO DE FECHAS ====================

    private void exportMovementsByDateRangeCsv() {
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Movimientos por Rango de Fechas");
        if (dateRange == null) return;

        try {
            Report report = reportService.inventoryMovementsByDateRange(
                dateRange.getFromString(),
                dateRange.getToString()
            );
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Movimientos por Fecha - CSV");
            fc.setInitialFileName("movimientos_" + dateRange.getFromString() + "_" + dateRange.getToString() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnMovementsByDateCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando movimientos por fecha a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportMovementsByDateRangePdf() {
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Movimientos por Rango de Fechas");
        if (dateRange == null) return;

        try {
            Report report = reportService.inventoryMovementsByDateRange(
                dateRange.getFromString(),
                dateRange.getToString()
            );
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Movimientos por Fecha - PDF");
            fc.setInitialFileName("movimientos_" + dateRange.getFromString() + "_" + dateRange.getToString() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnMovementsByDatePdf.getScene().getWindow());
            if (f == null) return;

            String description = String.format("Historial de transacciones desde %s hasta %s.",
                dateRange.getFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dateRange.getTo().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando movimientos por fecha a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== PRODUCTOS REGISTRADOS POR RANGO DE FECHAS ====================

    private void exportProductsByDateRangeCsv() {
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Productos Registrados por Fecha");
        if (dateRange == null) return;

        try {
            Report report = reportService.inventoryProductsByDateRange(
                dateRange.getFromString(),
                dateRange.getToString()
            );
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos por Fecha - CSV");
            fc.setInitialFileName("productos_registrados_" + dateRange.getFromString() + "_" + dateRange.getToString() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnProductsByDateCsv.getScene().getWindow());
            if (f == null) return;

            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos por fecha a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportProductsByDateRangePdf() {
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Productos Registrados por Fecha");
        if (dateRange == null) return;

        try {
            Report report = reportService.inventoryProductsByDateRange(
                dateRange.getFromString(),
                dateRange.getToString()
            );
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos por Fecha - PDF");
            fc.setInitialFileName("productos_registrados_" + dateRange.getFromString() + "_" + dateRange.getToString() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnProductsByDatePdf.getScene().getWindow());
            if (f == null) return;

            String description = String.format("Productos registrados en el sistema desde %s hasta %s.",
                dateRange.getFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dateRange.getTo().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos por fecha a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== TRANSACCIONES - HISTORIAL COMPLETO ====================
    private void exportTransactionsAllCsv() {
        try {
            Report report = reportService.transactionsAll();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Historial de Transacciones - CSV");
            fc.setInitialFileName("transacciones_historial_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnTransactionsAllCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando historial de transacciones a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportTransactionsAllPdf() {
        try {
            Report report = reportService.transactionsAll();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Historial de Transacciones - PDF");
            fc.setInitialFileName("transacciones_historial_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnTransactionsAllPdf.getScene().getWindow());
            if (f == null) return;
            String description = "Todas las transacciones registradas en el sistema";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando historial de transacciones a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== TRANSACCIONES - POR TIPO ====================
    private void exportTransactionsByTypeCsv() {
        TransactionTypeDialog.TransactionType type = TransactionTypeDialog.showDialog("Transacciones por Tipo");
        if (type == null) return;
        try {
            Report report = reportService.transactionsByTypeReport(type.getCode());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Tipo - CSV");
            fc.setInitialFileName("transacciones_" + type.getCode() + "_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnTransactionsByTypeCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por tipo a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportTransactionsByTypePdf() {
        TransactionTypeDialog.TransactionType type = TransactionTypeDialog.showDialog("Transacciones por Tipo");
        if (type == null) return;
        try {
            Report report = reportService.transactionsByTypeReport(type.getCode());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Tipo - PDF");
            fc.setInitialFileName("transacciones_" + type.getCode() + "_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnTransactionsByTypePdf.getScene().getWindow());
            if (f == null) return;
            String description = "Transacciones filtradas por tipo: " + type.getDisplayName();
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por tipo a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== TRANSACCIONES - POR TIPO Y FECHA ====================
    private void exportTransactionsByTypeAndDateCsv() {
        TransactionTypeDialog.TransactionType type = TransactionTypeDialog.showDialog("Transacciones por Tipo y Fecha");
        if (type == null) return;
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Seleccione el rango de fechas");
        if (dateRange == null) return;
        try {
            Report report = reportService.transactionsByTypeAndDateRange(type.getCode(), dateRange.getFromString(), dateRange.getToString());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Tipo y Fecha - CSV");
            fc.setInitialFileName("transacciones_" + type.getCode() + "_" + dateRange.getFromString() + "_" + dateRange.getToString() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnTransactionsByTypeAndDateCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por tipo y fecha a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportTransactionsByTypeAndDatePdf() {
        TransactionTypeDialog.TransactionType type = TransactionTypeDialog.showDialog("Transacciones por Tipo y Fecha");
        if (type == null) return;
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Seleccione el rango de fechas");
        if (dateRange == null) return;
        try {
            Report report = reportService.transactionsByTypeAndDateRange(type.getCode(), dateRange.getFromString(), dateRange.getToString());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Tipo y Fecha - PDF");
            fc.setInitialFileName("transacciones_" + type.getCode() + "_" + dateRange.getFromString() + "_" + dateRange.getToString() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnTransactionsByTypeAndDatePdf.getScene().getWindow());
            if (f == null) return;
            String description = String.format("Transacciones de tipo %s desde %s hasta %s.", type.getDisplayName(),
                dateRange.getFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dateRange.getTo().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por tipo y fecha a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== TRANSACCIONES - POR USUARIO ====================
    private void exportTransactionsByUserCsv() {
        UserSelectionDialog.UserSelection userSel = UserSelectionDialog.showDialog("Transacciones por Usuario");
        if (userSel == null) return;
        try {
            Report report = reportService.transactionsByUserReport(userSel.getUserId());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Usuario - CSV");
            fc.setInitialFileName("transacciones_usuario_" + userSel.getUserId() + "_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnTransactionsByUserCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por usuario a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportTransactionsByUserPdf() {
        UserSelectionDialog.UserSelection userSel = UserSelectionDialog.showDialog("Transacciones por Usuario");
        if (userSel == null) return;
        try {
            Report report = reportService.transactionsByUserReport(userSel.getUserId());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Usuario - PDF");
            fc.setInitialFileName("transacciones_usuario_" + userSel.getUserId() + "_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnTransactionsByUserPdf.getScene().getWindow());
            if (f == null) return;
            String description = "Historial de transacciones realizadas por el usuario: " + userSel.getUserName();
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por usuario a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== TRANSACCIONES - POR PERSONA ====================
    private void exportTransactionsByPersonCsv() {
        PersonSelectionDialog.PersonSelection personSel = PersonSelectionDialog.showDialog("Transacciones por Persona");
        if (personSel == null) return;
        try {
            Report report = reportService.transactionsByPersonReport(personSel.getPersonId());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Persona - CSV");
            fc.setInitialFileName("transacciones_persona_" + personSel.getPersonId() + "_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnTransactionsByPersonCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por persona a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportTransactionsByPersonPdf() {
        PersonSelectionDialog.PersonSelection personSel = PersonSelectionDialog.showDialog("Transacciones por Persona");
        if (personSel == null) return;
        try {
            Report report = reportService.transactionsByPersonReport(personSel.getPersonId());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Transacciones por Persona - PDF");
            fc.setInitialFileName("transacciones_persona_" + personSel.getPersonId() + "_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnTransactionsByPersonPdf.getScene().getWindow());
            if (f == null) return;
            String description = "Transacciones asociadas a la persona: " + personSel.getPersonName();
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando transacciones por persona a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== REPORTES ANALÍTICOS - PRODUCTOS MÁS MOVIDOS ====================
    private void exportProductsMostMovedCsv() {
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Productos Más Movidos");
        if (dateRange == null) return;
        try {
            Report report = reportService.productsMostMoved(dateRange.getFromString(), dateRange.getToString());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos Más Movidos - CSV");
            fc.setInitialFileName("productos_mas_movidos_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnProductsMostMovedCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos más movidos a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportProductsMostMovedPdf() {
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Productos Más Movidos");
        if (dateRange == null) return;
        try {
            Report report = reportService.productsMostMoved(dateRange.getFromString(), dateRange.getToString());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos Más Movidos - PDF");
            fc.setInitialFileName("productos_mas_movidos_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnProductsMostMovedPdf.getScene().getWindow());
            if (f == null) return;
            String description = String.format("Top 20 productos con más transacciones desde %s hasta %s.",
                dateRange.getFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dateRange.getTo().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos más movidos a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== REPORTES ANALÍTICOS - PRÉSTAMOS ACTIVOS ====================
    private void exportPeopleWithLoansCsv() {
        try {
            Report report = reportService.peopleWithActiveLoans();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Personas con Préstamos Activos - CSV");
            fc.setInitialFileName("prestamos_activos_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnPeopleWithLoansCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando préstamos activos a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportPeopleWithLoansPdf() {
        try {
            Report report = reportService.peopleWithActiveLoans();
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Personas con Préstamos Activos - PDF");
            fc.setInitialFileName("prestamos_activos_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnPeopleWithLoansPdf.getScene().getWindow());
            if (f == null) return;
            String description = "Control de préstamos activos con información detallada de días transcurridos.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando préstamos activos a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== REPORTES ANALÍTICOS - CONSUMO MENSUAL ====================
    private void exportMonthlyConsumptionCsv() {
        try {
            Report report = reportService.monthlyConsumption(12); // Últimos 12 meses
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Consumo Mensual - CSV");
            fc.setInitialFileName("consumo_mensual_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnMonthlyConsumptionCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando consumo mensual a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportMonthlyConsumptionPdf() {
        try {
            Report report = reportService.monthlyConsumption(12); // Últimos 12 meses
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Consumo Mensual - PDF");
            fc.setInitialFileName("consumo_mensual_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnMonthlyConsumptionPdf.getScene().getWindow());
            if (f == null) return;
            String description = "Análisis de consumo de productos de los últimos 12 meses con desglose mensual.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando consumo mensual a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== REPORTES ANALÍTICOS - PRODUCTOS SIN MOVIMIENTO ====================
    private void exportProductsNoMovementCsv() {
        try {
            Report report = reportService.productsWithoutMovement(90); // Últimos 90 días
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos Sin Movimiento - CSV");
            fc.setInitialFileName("productos_sin_movimiento_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnProductsNoMovementCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos sin movimiento a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportProductsNoMovementPdf() {
        try {
            Report report = reportService.productsWithoutMovement(90); // Últimos 90 días
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Productos Sin Movimiento - PDF");
            fc.setInitialFileName("productos_sin_movimiento_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnProductsNoMovementPdf.getScene().getWindow());
            if (f == null) return;
            String description = "Productos que no han tenido movimientos en los últimos 90 días. Detecta productos obsoletos.";
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando productos sin movimiento a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== REPORTES ANALÍTICOS - ACTIVIDAD POR USUARIO ====================
    private void exportUserActivityCsv() {
        UserSelectionDialog.UserSelection userSel = UserSelectionDialog.showDialog("Actividad por Usuario");
        if (userSel == null) return;
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Seleccione el rango de fechas");
        if (dateRange == null) return;
        try {
            Report report = reportService.userActivity(userSel.getUserId(), dateRange.getFromString(), dateRange.getToString());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Actividad por Usuario - CSV");
            fc.setInitialFileName("actividad_usuario_" + userSel.getUserId() + "_" + getTimestamp() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnUserActivityCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando actividad de usuario a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportUserActivityPdf() {
        UserSelectionDialog.UserSelection userSel = UserSelectionDialog.showDialog("Actividad por Usuario");
        if (userSel == null) return;
        DateRangeDialog.DateRange dateRange = DateRangeDialog.showDialog("Seleccione el rango de fechas");
        if (dateRange == null) return;
        try {
            Report report = reportService.userActivity(userSel.getUserId(), dateRange.getFromString(), dateRange.getToString());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Actividad por Usuario - PDF");
            fc.setInitialFileName("actividad_usuario_" + userSel.getUserId() + "_" + getTimestamp() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnUserActivityPdf.getScene().getWindow());
            if (f == null) return;
            String description = String.format("Actividad detallada del usuario %s desde %s hasta %s con desglose diario.",
                userSel.getUserName(),
                dateRange.getFrom().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dateRange.getTo().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando actividad de usuario a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== REPORTES ANALÍTICOS - DASHBOARD MENSUAL ====================
    private void exportMonthlyDashboardCsv() {
        MonthYearDialog.MonthYearSelection monthYear = MonthYearDialog.showDialog("Dashboard Mensual");
        if (monthYear == null) return;
        try {
            Report report = reportService.monthlyDashboard(monthYear.getYearMonth());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Dashboard Mensual - CSV");
            fc.setInitialFileName("dashboard_" + monthYear.getYearMonth() + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File f = fc.showSaveDialog(btnMonthlyDashboardCsv.getScene().getWindow());
            if (f == null) return;
            exportToCsv(report, f);
            showSuccess("CSV exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando dashboard mensual a CSV", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private void exportMonthlyDashboardPdf() {
        MonthYearDialog.MonthYearSelection monthYear = MonthYearDialog.showDialog("Dashboard Mensual");
        if (monthYear == null) return;
        try {
            Report report = reportService.monthlyDashboard(monthYear.getYearMonth());
            FileChooser fc = new FileChooser();
            fc.setTitle("Exportar Dashboard Mensual - PDF");
            fc.setInitialFileName("dashboard_" + monthYear.getYearMonth() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File f = fc.showSaveDialog(btnMonthlyDashboardPdf.getScene().getWindow());
            if (f == null) return;
            String description = String.format("Dashboard mensual con KPIs principales del sistema para %s.", monthYear.getFormatted());
            exportToPdf(report, f, description);
            showSuccess("PDF exportado exitosamente: " + f.getName());
        } catch (Exception ex) {
            Logs.error(getClass(), "Error exportando dashboard mensual a PDF", ex);
            showError("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    // ==================== MÉTODOS DE EXPORTACIÓN ====================

    private void exportToCsv(Report report, File file) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            // Escribir encabezados
            bw.write(String.join(",", report.getHeaders()));
            bw.write("\n");

            // Escribir filas
            for (List<String> row : report.getRows()) {
                List<String> quoted = new ArrayList<>(row.size());
                for (String s : row) {
                    quoted.add(quoteCsv(s));
                }
                bw.write(String.join(",", quoted));
                bw.write("\n");
            }
            bw.flush();
        }
    }

    private void exportToPdf(Report report, File file, String description) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float margin = 50f;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float y = pageHeight - margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // ========== ENCABEZADO ==========

            // 1. Logo
            try (java.io.InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null) {
                    byte[] imgBytes = is.readAllBytes();
                    org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage =
                            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(doc, imgBytes, "logo");
                    float imgW = 60f;
                    float imgH = 60f;
                    float imgX = margin;
                    float imgY = y - imgH;
                    cs.drawImage(pdImage, imgX, imgY, imgW, imgH);
                    y = imgY;
                } else {
                    Logs.warn(getClass(), "No se encontró el logo en /images/logo.png");
                }
            } catch (Exception imgEx) {
                Logs.warn(getClass(), "Error cargando logo: {}", imgEx.getMessage());
            }

            // 2. Título del reporte (a la derecha del logo)
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.beginText();
            cs.newLineAtOffset(margin + 75, y + 35);
            cs.showText(report.getTitle());
            cs.endText();

            y -= 20f;

            // 3. Línea separadora
            cs.setLineWidth(1f);
            cs.moveTo(margin, y);
            cs.lineTo(pageWidth - margin, y);
            cs.stroke();

            y -= 20f;

            // 4. Descripción del reporte
            cs.setFont(PDType1Font.HELVETICA, 11);
            List<String> descLines = wrapText(description, 90);
            for (String line : descLines) {
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(line);
                cs.endText();
                y -= 14f;
            }

            y -= 5f;

            // 5. Usuario que generó el reporte
            String username = Session.getCurrentUsername();
            if (username == null || username.isEmpty()) {
                username = "Sistema";
            }
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Generado por: ");
            cs.endText();

            cs.setFont(PDType1Font.HELVETICA, 10);
            float userX = margin + PDType1Font.HELVETICA_BOLD.getStringWidth("Generado por: ") / 1000 * 10;
            cs.beginText();
            cs.newLineAtOffset(userX, y);
            cs.showText(username);
            cs.endText();

            y -= 14f;

            // 6. Fecha de generación
            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Fecha: ");
            cs.endText();

            cs.setFont(PDType1Font.HELVETICA, 10);
            float fechaX = margin + PDType1Font.HELVETICA_BOLD.getStringWidth("Fecha: ") / 1000 * 10;
            cs.beginText();
            cs.newLineAtOffset(fechaX, y);
            cs.showText(fecha);
            cs.endText();

            y -= 25f;

            // ========== TABLA DE DATOS ==========

            List<String> headers = report.getHeaders();
            int numCols = headers.size();
            float tableWidth = pageWidth - 2 * margin;
            float colWidth = tableWidth / numCols;
            float rowHeight = 18f;

            // Encabezados de tabla
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            for (int i = 0; i < numCols; i++) {
                float x = margin + i * colWidth;
                cs.beginText();
                cs.newLineAtOffset(x + 3, y);
                cs.showText(truncate(headers.get(i), getMaxChars(colWidth)));
                cs.endText();
            }

            y -= rowHeight;

            // Línea bajo encabezados
//            cs.setLineWidth(0.5f);
//            cs.moveTo(margin, y + 3);
//            cs.lineTo(pageWidth - margin, y + 5);
//            cs.stroke();

            // Filas de datos
            cs.setFont(PDType1Font.HELVETICA, 8);
            for (List<String> row : report.getRows()) {
                // Verificar si necesitamos nueva página
                if (y < margin + rowHeight * 2) {
                    cs.close();
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - margin;

                    // Reescribir encabezados
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
                    for (int i = 0; i < numCols; i++) {
                        float x = margin + i * colWidth;
                        cs.beginText();
                        cs.newLineAtOffset(x + 3, y);
                        cs.showText(truncate(headers.get(i), getMaxChars(colWidth)));
                        cs.endText();
                    }
                    y -= rowHeight;
                    cs.setFont(PDType1Font.HELVETICA, 8);
                }

                // Escribir fila
                for (int i = 0; i < numCols && i < row.size(); i++) {
                    float x = margin + i * colWidth;
                    String cellValue = row.get(i);
                    if (cellValue != null) {
                        // Formatear rol para mostrar nombre completo
                        if (i == 3 && headers.get(i).equalsIgnoreCase("Rol")) {
                            cellValue = Roles.getRoleFullName(cellValue);
                        }
                        cs.beginText();
                        cs.newLineAtOffset(x + 3, y);
                        cs.showText(truncate(cellValue, getMaxChars(colWidth)));
                        cs.endText();
                    }
                }

                y -= rowHeight;
            }

            cs.close();
            doc.save(file);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String quoteCsv(String s) {
        if (s == null) return "";
        String esc = s.replace("\"", "\"\"");
        if (esc.contains(",") || esc.contains("\n") || esc.contains("\r")) {
            return "\"" + esc + "\"";
        }
        return esc;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private int getMaxChars(float colWidth) {
        // Aproximación de caracteres que caben en el ancho de columna
        return Math.max(8, (int)(colWidth / 5.5));
    }

    private List<String> wrapText(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
