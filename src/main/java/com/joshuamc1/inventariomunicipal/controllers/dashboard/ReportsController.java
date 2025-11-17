package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.AsyncLoadable;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.models.Report;
import com.joshuamc1.inventariomunicipal.services.ReportService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ReportsController implements AsyncLoadable {
    @FXML private ListView<String> lstReports;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private Button btnRun;
    @FXML private Button btnRefresh;
    @FXML private Button btnExportCsv;
    @FXML private Button btnExportPdf;
    @FXML private Button btnExportAllCsv;
    @FXML private Button btnExportAllPdf;
    @FXML private TableView<List<String>> tblReport;

    private final ReportService reportService = new ReportService();
    private Report currentReport;
    private final ObservableList<String> reportNames = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        reportNames.addAll(
                "Usuarios con roles",
                "Inventario total",
                "Inventario - Stock bajo",
                "Inventario por rango de fechas",
                "Transacciones",
                "Transacciones por tipo",
                "Transacciones por rango",
                "Personas registradas",
                "Transacciones por persona"
        );
        lstReports.setItems(reportNames);
        lstReports.getSelectionModel().selectFirst();

        btnRun.setOnAction(e -> runReport());
        btnRefresh.setOnAction(e -> runReport());
        btnExportCsv.setOnAction(e -> exportCsv());
        btnExportPdf.setOnAction(e -> exportPdf());
        btnExportAllCsv.setOnAction(e -> exportAllCsv());
        btnExportAllPdf.setOnAction(e -> exportAllPdf());

        // cargar reporte al seleccionar una opción
        lstReports.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) runReport();
        });

        // refrescar cuando cambien las fechas
        dpFrom.valueProperty().addListener((o, a, b) -> runReport());
        dpTo.valueProperty().addListener((o, a, b) -> runReport());
    }

    private void runReport() {
        String selected = lstReports.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        new Thread(() -> {
            try {
                Report res = generateReportForSelection(selected);
                Platform.runLater(() -> showReport(res));
            } catch (Throwable t) {
                Logs.error(this.getClass(), "Error generando reporte: {}", t.getMessage());
                Platform.runLater(() -> showAlert("Error", "No se pudo generar el reporte: " + t.getMessage()));
            }
        }).start();
    }

    // Genera el reporte para la opción actual usando el rango de fechas de los DatePicker
    private Report generateReportForSelection(@NotNull String selected) {
        String[] range = currentRange();
        String fromStr = range[0];
        String toStr = range[1];

        return switch (selected) {
            case "Usuarios con roles" -> reportService.usersWithRoles();
            case "Inventario total" -> reportService.inventoryTotal();
            case "Inventario - Stock bajo" -> reportService.inventoryLowStock();
            case "Inventario por rango de fechas" -> reportService.inventoryByDateRange(fromStr, toStr);
            case "Transacciones" -> reportService.transactions(fromStr, toStr);
            case "Transacciones por tipo" -> reportService.transactionsByType("in", fromStr, toStr); // default type in
            case "Transacciones por rango" -> reportService.transactions(fromStr, toStr);
            case "Personas registradas" -> reportService.peopleRegistered(fromStr, toStr);
            case "Transacciones por persona" -> reportService.transactionsByPerson(0, fromStr, toStr);
            default -> new Report("Vacío");
        };
    }

    private String[] currentRange() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        // No normalizamos aquí para evitar parseos repetidos; ReportService ya normaliza.
        String fromStr = (from == null) ? null : from.format(DateTimeFormatter.ISO_DATE);
        String toStr = (to == null) ? null : to.format(DateTimeFormatter.ISO_DATE);
        return new String[]{fromStr, toStr};
    }

    private void showReport(@NotNull Report res) {
        this.currentReport = res;
        // clear existing columns
        tblReport.getColumns().clear();
        tblReport.getItems().clear();

        // create dynamic columns
        List<String> headers = res.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            final int colIndex = i;
            TableColumn<List<String>, String> col = new TableColumn<>(headers.get(i));
            col.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                String v = colIndex < row.size() ? row.get(colIndex) : "";
                return new javafx.beans.property.SimpleStringProperty(v);
            });
            tblReport.getColumns().add(col);
        }

        // fill rows
        for (List<String> row : res.getRows()) {
            tblReport.getItems().add(row);
        }
    }

    private void exportCsv() {
        // Re-generamos con el rango actual antes de exportar
        String selected = lstReports.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Info", "Seleccione un reporte"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = fc.showSaveDialog(tblReport.getScene().getWindow());
        if (file == null) return;

        CompletableFuture.supplyAsync(() -> generateReportForSelection(selected))
                .thenAccept(res -> {
                    Platform.runLater(() -> showReport(res));
                    try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                        // encabezado: título, usuario y fecha
                        String username = com.joshuamc1.inventariomunicipal.core.Session.getCurrentUsername();
                        String fechaReporte = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                        bw.write(res.getTitle()); bw.write('\n');
                        bw.write("Usuario: " + (username == null ? "-" : username) + ", Fecha: " + fechaReporte); bw.write('\n');

                        // columnas y filas
                        bw.write(String.join(",", res.getHeaders())); bw.write('\n');
                        for (List<String> row : res.getRows()) {
                            bw.write(row.stream().map(s -> s == null ? "" : s.replace("\"", "\"\"")).collect(Collectors.joining(","))); bw.write('\n');
                        }
                        Platform.runLater(() -> showInfo("Exportado", "CSV guardado en: " + file.getAbsolutePath()));
                    } catch (IOException e) {
                        Logs.error(this.getClass(), "Error exportando CSV: {}", e.getMessage());
                        Platform.runLater(() -> showAlert("Error", "No se pudo exportar CSV: " + e.getMessage()));
                    }
                })
                .exceptionally(ex -> { Platform.runLater(() -> showAlert("Error", ex.getMessage())); return null; });
    }

    private void exportPdf() {
        // Re-generamos con el rango actual antes de exportar
        String selected = lstReports.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Info", "Seleccione un reporte"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = fc.showSaveDialog(tblReport.getScene().getWindow());
        if (file == null) return;

        CompletableFuture.supplyAsync(() -> generateReportForSelection(selected))
                .thenAccept(res -> {
                    Platform.runLater(() -> showReport(res));
                    generatePdf(file, res);
                })
                .exceptionally(ex -> { Platform.runLater(() -> showAlert("Error", ex.getMessage())); return null; });
    }

    private void exportAllCsv() {
        // Por ahora, mismo comportamiento que exportCsv. Si se desea ignorar filtros, aquí sería el lugar.
        exportCsv();
    }

    private void exportAllPdf() {
        // Por ahora, mismo comportamiento que exportPdf. Si se desea ignorar filtros, aquí sería el lugar.
        exportPdf();
    }

    // genera un PDF estilizado con logo, título, usuario/fecha y tabla
    private void generatePdf(File file, Report report) {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);

            float margin = 40f;
            float pageWidth = page.getMediaBox().getWidth();
            float y = page.getMediaBox().getHeight() - margin;

            // logo centrado
            try (InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null) {
                    byte[] imgBytes = is.readAllBytes();
                    org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(doc, imgBytes, "logo");
                    float imgW = 80f; float imgH = 80f;
                    float imgX = (pageWidth - imgW) / 2f; float imgY = y - imgH;
                    cs.drawImage(pdImage, imgX, imgY, imgW, imgH);
                    y = imgY - 10f;
                }
            } catch (Exception ex) {
                Logs.warn(this.getClass(), "No se pudo cargar logo para PDF: {}", ex.getMessage());
            }

            // título
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
            String title = report.getTitle();
            float titleWidth = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 16;
            float titleX = (pageWidth - titleWidth) / 2f;
            cs.beginText(); cs.newLineAtOffset(titleX, y); cs.showText(title); cs.endText();
            y -= 24f;

            // usuario y fecha
            String username = com.joshuamc1.inventariomunicipal.core.Session.getCurrentUsername();
            String fechaReporte = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
            cs.beginText(); cs.newLineAtOffset(margin, y); cs.showText("Usuario: " + (username == null ? "-" : username)); cs.endText();
            String fechaText = "Fecha: " + fechaReporte;
            float fechaWidth = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA.getStringWidth(fechaText) / 1000 * 10;
            cs.beginText(); cs.newLineAtOffset(pageWidth - margin - fechaWidth, y); cs.showText(fechaText); cs.endText();
            y -= 24f;

            // encabezados tabla
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 9);
            float tableX = margin; float tableWidth = pageWidth - margin * 2;
            float colW = tableWidth / Math.max(1, report.getHeaders().size());
            float cx = tableX;
            float headerY = y;
            for (String h : report.getHeaders()) { cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText(h); cs.endText(); cx += colW; }
            y -= 16f;

            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9);
            float rowHeight = 14f;
            for (List<String> row : report.getRows()) {
                if (y < margin + rowHeight) {
                    cs.close();
                    page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                cx = tableX;
                for (int i = 0; i < report.getHeaders().size(); i++) {
                    String v = i < row.size() ? (row.get(i) == null ? "" : row.get(i)) : "";
                    cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(truncate(v, (int) colW / 6)); cs.endText();
                    cx += colW;
                }
                y -= rowHeight;
            }

            cs.close();
            doc.save(file);
            Platform.runLater(() -> showInfo("Exportado", "PDF guardado en: " + file.getAbsolutePath()));
        } catch (Exception e) {
            Logs.error(this.getClass(), "Error exportando PDF: {}", e.getMessage());
            Platform.runLater(() -> showAlert("Error", "No se pudo exportar PDF: " + e.getMessage()));
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        showAlert(title, msg);
    }

    private @NotNull String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    @Override
    public boolean isLoading() { return false; }

    @Override
    public void setOnLoaded(Runnable callback) {}
}
