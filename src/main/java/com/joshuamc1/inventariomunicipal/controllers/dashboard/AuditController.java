package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.AsyncLoadable;
import com.joshuamc1.inventariomunicipal.core.Logs;
import com.joshuamc1.inventariomunicipal.models.dto.AuditDTO;
import com.joshuamc1.inventariomunicipal.services.AuditService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;

public class AuditController implements AsyncLoadable {
    @FXML private TableView<AuditDTO> tblAudit;
    @FXML private TableColumn<AuditDTO, Integer> colId;
    @FXML private TableColumn<AuditDTO, String> colUser;
    @FXML private TableColumn<AuditDTO, String> colAction;
    @FXML private TableColumn<AuditDTO, String> colTable;
    @FXML private TableColumn<AuditDTO, Integer> colRecordId;
    @FXML private TableColumn<AuditDTO, String> colDetails;
    @FXML private TableColumn<AuditDTO, String> colCreatedAt;

    @FXML private Button btnRefresh;
    @FXML private Button btnExportCsv;
    @FXML private Button btnExportPdf;
    @FXML private TextField txtSearch;

    // pagination controls
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;
    @FXML private ChoiceBox<Integer> choicePageSize;

    private final AuditService auditService = new AuditService();
    private ObservableList<AuditDTO> data = FXCollections.observableArrayList();
    private volatile boolean loading = true;
    private Runnable onLoaded;

    private final DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter outFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // pagination state
    private int currentPage = 1; // 1-based
    private int pageSize = 25;
    private int totalItems = 0;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        // column mappings
        colId.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> c.getValue().getId()));
        colUser.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> {
            String n = c.getValue().getUsuarioName();
            return n == null || n.isEmpty() ? String.valueOf(c.getValue().getUsuarioId()) : n;
        }));
        colAction.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> c.getValue().getAccion()));
        colTable.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> c.getValue().getTablaAfectada()));
        colRecordId.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> c.getValue().getRegistroAfectadoId()));
        colDetails.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> c.getValue().getDetalles()));
        colCreatedAt.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() -> {
            String s = c.getValue().getFecha();
            if (s == null) return "";
            try {
                LocalDateTime dt = LocalDateTime.parse(s, dbFormatter);
                return dt.format(outFormatter);
            } catch (Exception ex) {
                return s;
            }
        }));

        // detalles cell factory (truncado)
        colDetails.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String display = item.length() > 120 ? item.substring(0, 117) + "..." : item;
                    setText(display);
                    setTooltip(item.length() > 120 ? new Tooltip(item) : null);
                }
            }
        });

        colId.setStyle("-fx-font-size: 17;");
        colUser.setStyle("-fx-font-size: 17;");
        colAction.setStyle("-fx-font-size: 17;");
        colTable.setStyle("-fx-font-size: 17;");
        colRecordId.setStyle("-fx-font-size: 17;");
        colDetails.setStyle("-fx-font-size: 17;");
        colCreatedAt.setStyle("-fx-font-size: 17;");

        tblAudit.setItems(data);

        // pagination UI
        choicePageSize.setItems(FXCollections.observableArrayList(10,25,50,100));
        choicePageSize.setValue(pageSize);
        choicePageSize.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                pageSize = newV;
                currentPage = 1;
                loadPageAsync(currentPage);
            }
        });

        btnPrev.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadPageAsync(currentPage);
            }
        });

        btnNext.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadPageAsync(currentPage);
            }
        });

        btnRefresh.setOnAction(e -> {
            currentPage = 1;
            loadPageAsync(currentPage);
        });

        // search triggers reload
        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            currentPage = 1;
            loadPageAsync(currentPage);
        });

        btnExportCsv.setOnAction(e -> exportCsv());
        btnExportPdf.setOnAction(e -> exportPdf());

        // initial load
        loadPageAsync(currentPage);
    }

    private void loadPageAsync(int page) {
        loading = true;
        int pageToLoad = Math.max(1, page);
        new Thread(() -> {
            try {
                String search = txtSearch.getText();
                int offset = (pageToLoad - 1) * pageSize;
                List<AuditDTO> items = auditService.getPageWithUser(offset, pageSize, search);
                int total = auditService.countWithFilter(search);

                Platform.runLater(() -> {
                    data.setAll(items);
                    totalItems = total;
                    totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
                    currentPage = Math.min(pageToLoad, totalPages);
                    updatePaginationControls();
                    loading = false;
                    if (onLoaded != null) onLoaded.run();
                });
            } catch (Throwable t) {
                Logs.error(this.getClass(), "Error cargando auditoría", t);
                Platform.runLater(() -> {
                    loading = false;
                    if (onLoaded != null) onLoaded.run();
                    showAlert("Error", "No se pudieron cargar registros de auditoría: " + t.getMessage());
                });
            }
        }).start();
    }

    private void updatePaginationControls() {
        lblPage.setText(String.format("Página %d/%d (%d)", currentPage, totalPages, totalItems));
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
    }

    private void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar auditoría a CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File f = fc.showSaveDialog(tblAudit.getScene().getWindow());
        if (f == null) return;

        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
            // header
            bw.write("id,usuario_id,usuario_name,accion,tabla_afectada,registro_afectado_id,detalles,fecha\n");
            for (AuditDTO r : data) {
                String line = String.format("%d,%s,%s,%s,%s,%s,%s,%s\n",
                        r.getId() != null ? r.getId() : 0,
                        r.getUsuarioId() != null ? r.getUsuarioId() : "",
                        quoteCsv(r.getUsuarioName()),
                        quoteCsv(r.getAccion()),
                        quoteCsv(r.getTablaAfectada()),
                        r.getRegistroAfectadoId() != null ? r.getRegistroAfectadoId() : "",
                        quoteCsv(r.getDetalles()),
                        quoteCsv(r.getFecha())
                );
                bw.write(line);
            }
            bw.flush();
            showInfo("Exportado", "CSV exportado: " + f.getAbsolutePath());
        } catch (IOException ex) {
            Logs.error(this.getClass(), "Error exportando CSV", ex);
            showAlert("Error", "No se pudo exportar CSV: " + ex.getMessage());
        }
    }

    private @NotNull String quoteCsv(String s) {
        if (s == null) return "";
        String esc = s.replace("\"", "\"\"");
        if (esc.contains(",") || esc.contains("\n") || esc.contains("\r")) {
            return "\"" + esc + "\"";
        }
        return esc;
    }

    private void exportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar auditoría a PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File f = fc.showSaveDialog(tblAudit.getScene().getWindow());
        if (f == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            float margin = 40f;
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float pageWidth = page.getMediaBox().getWidth();
            float y = page.getMediaBox().getHeight() - margin;

            // logo centrado
            try (java.io.InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null) {
                    byte[] imgBytes = is.readAllBytes();
                    org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage =
                            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(doc, imgBytes, "logo");
                    float imgW = 80f;
                    float imgH = 80f;
                    float imgX = (pageWidth - imgW) / 2f;
                    float imgY = y - imgH;
                    cs.drawImage(pdImage, imgX, imgY, imgW, imgH);
                    y = imgY - 10f;
                }
            } catch (Exception imgEx) {
                Logs.warn(this.getClass(), "No se pudo cargar logo para PDF: {}", imgEx.getMessage());
            }

            // título centrado
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            String title = "Reporte de auditoría";
            float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 16;
            float titleX = (pageWidth - titleWidth) / 2f;
            cs.beginText();
            cs.newLineAtOffset(titleX, y);
            cs.showText(title);
            cs.endText();
            y -= 24f;

            // linea con usuario y fecha (usuario a la izquierda, fecha a la derecha)
            String username = com.joshuamc1.inventariomunicipal.core.Session.getCurrentUsername();
            String fechaReporte = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Usuario: " + (username == null ? "-" : username));
            cs.endText();

            String fechaText = "Fecha: " + fechaReporte;
            float fechaWidth = PDType1Font.HELVETICA.getStringWidth(fechaText) / 1000 * 10;
            cs.beginText();
            cs.newLineAtOffset(pageWidth - margin - fechaWidth, y);
            cs.showText(fechaText);
            cs.endText();

            y -= 24f;

            // encabezados de la tabla
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            float tableX = margin;
            float tableWidth = pageWidth - margin * 2;

            // definir anchos de columnas (ajustables)
            float colIdW = 40f;
            float colUserW = 120f;
            float colActionW = 80f;
            float colTableW = 80f;
            float colRegW = 60f;
            float colFechaW = 110f;
            float colDetailsW = tableWidth - (colIdW + colUserW + colActionW + colTableW + colRegW + colFechaW);

            // dibujar encabezado
            float cx = tableX;
            float headerY = y;
            cs.beginText();
            cs.newLineAtOffset(cx + 2, headerY);
            cs.showText("ID");
            cs.endText();
            cx += colIdW;

            cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText("Usuario"); cs.endText(); cx += colUserW;
            cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText("Acción"); cs.endText(); cx += colActionW;
            cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText("Tabla"); cs.endText(); cx += colTableW;
            cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText("Reg ID"); cs.endText(); cx += colRegW;
            cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText("Fecha"); cs.endText(); cx += colFechaW;
            cs.beginText(); cs.newLineAtOffset(cx + 2, headerY); cs.showText("Detalles"); cs.endText();

            y -= 16f;

            cs.setFont(PDType1Font.HELVETICA, 9);
            float rowHeight = 14f;

            for (AuditDTO r : data) {
                if (y < margin + rowHeight) {
                    cs.close();
                    page = new PDPage(PDRectangle.LETTER);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                cx = tableX;
                // ID
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(String.valueOf(r.getId() == null ? "" : r.getId())); cs.endText(); cx += colIdW;
                // Usuario
                String uname = r.getUsuarioName() != null ? r.getUsuarioName() : (r.getUsuarioId() == null ? "" : String.valueOf(r.getUsuarioId()));
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(truncate(uname, (int)colUserW/6)); cs.endText(); cx += colUserW;
                // Acción
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(truncate(r.getAccion(), (int)colActionW/6)); cs.endText(); cx += colActionW;
                // Tabla
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(truncate(r.getTablaAfectada(), (int)colTableW/6)); cs.endText(); cx += colTableW;
                // Reg ID
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(r.getRegistroAfectadoId() == null ? "" : String.valueOf(r.getRegistroAfectadoId())); cs.endText(); cx += colRegW;
                // Fecha
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(truncate(r.getFecha(), (int)colFechaW/6)); cs.endText(); cx += colFechaW;
                // Detalles
                cs.beginText(); cs.newLineAtOffset(cx + 2, y); cs.showText(truncate(r.getDetalles(), Math.max(10, (int)colDetailsW/6))); cs.endText();

                y -= rowHeight;
            }

            cs.close();
            doc.save(f);
            showInfo("Exportado", "PDF exportado: " + f.getAbsolutePath());
        } catch (IOException ex) {
            Logs.error(this.getClass(), "Error exportando PDF", ex);
            showAlert("Error", "No se pudo exportar PDF: " + ex.getMessage());
        }
    }

    private @NotNull String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private void applyFilter(String q) {
        // kept for backward compatibility but now triggers DB reload via txtSearch listener
        if (q == null || q.trim().isEmpty()) {
            tblAudit.setItems(data);
            return;
        }

        String qq = q.toLowerCase();
        Predicate<AuditDTO> p = r -> {
            try {
                return (r.getAccion() != null && r.getAccion().toLowerCase().contains(qq))
                        || (r.getTablaAfectada() != null && r.getTablaAfectada().toLowerCase().contains(qq))
                        || (r.getDetalles() != null && r.getDetalles().toLowerCase().contains(qq))
                        || (r.getUsuarioName() != null && r.getUsuarioName().toLowerCase().contains(qq))
                        || String.valueOf(r.getUsuarioId()).contains(qq);
            } catch (Throwable t) {
                return false;
            }
        };

        tblAudit.setItems(data.filtered(p));
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setOnLoaded(Runnable callback) {
        this.onLoaded = callback;
    }
}
