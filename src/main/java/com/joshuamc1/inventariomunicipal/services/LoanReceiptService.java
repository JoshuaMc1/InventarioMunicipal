package com.joshuamc1.inventariomunicipal.services;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.image.*;
import com.github.anastaciocintra.output.PrinterOutputStream;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.joshuamc1.inventariomunicipal.models.*;
import com.joshuamc1.inventariomunicipal.models.dto.LoanReceiptDTO;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Servicio para imprimir recibos de préstamos en impresoras térmicas o generar PDF
 */
public class LoanReceiptService {
    private static final Logger logger = LoggerFactory.getLogger(LoanReceiptService.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * Obtiene la lista de impresoras disponibles en el sistema
     * @return Lista de nombres de impresoras
     */
    public static @NotNull List<String> getAvailablePrinters() {
        List<String> printers = new ArrayList<>();
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService printService : printServices) {
            printers.add(printService.getName());
        }

        System.out.println("Impresoras disponibles: " + printers);

        return printers;
    }

    /**
     * Convierte una transacción a DTO con todos los datos necesarios
     * Este método debe llamarse en el hilo principal donde hay conexión a BD
     * @param transaction Transacción del préstamo
     * @return DTO con todos los datos del recibo
     */
    public static @NotNull LoanReceiptDTO createReceiptDTO(@NotNull Transaction transaction) {
        LoanReceiptDTO dto = new LoanReceiptDTO();

        // Datos básicos de la transacción
        dto.setLoanCode(transaction.getString("loan_code"));
        dto.setCreatedAt(transaction.getTimestamp("created_at"));
        dto.setExpectedReturnDate(transaction.getDate("expected_return_date"));
        dto.setNotes(transaction.getString("notes"));

        // Usuario que realizó el préstamo
        User user = transaction.parent(User.class);
        if (user != null) {
            dto.setUserName(user.getString("name"));
            dto.setUserUsername(user.getString("username"));
        }

        // Persona que recibe el préstamo
        Person person = transaction.parent(Person.class);
        if (person != null) {
            dto.setPersonFirstName(person.getString("first_name"));
            dto.setPersonLastName(person.getString("last_name"));
            dto.setPersonDni(person.getString("dni"));
            dto.setPersonPhone(person.getString("phone"));
        }

        // Items del préstamo
        List<TransactionItem> items = transaction.getAll(TransactionItem.class);
        List<LoanReceiptDTO.LoanReceiptItemDTO> itemDTOs = new ArrayList<>();

        for (TransactionItem item : items) {
            Product product = item.parent(Product.class);
            if (product != null) {
                LoanReceiptDTO.LoanReceiptItemDTO itemDTO = new LoanReceiptDTO.LoanReceiptItemDTO(
                    product.getString("name"),
                    product.getString("code"),
                    item.getDouble("quantity"),
                    product.getString("unit"),
                    item.getString("notes")
                );
                itemDTOs.add(itemDTO);
            }
        }

        dto.setItems(itemDTOs);

        return dto;
    }

    /**
     * Imprime el recibo de préstamo en una impresora térmica
     * @param transaction Transacción del préstamo
     * @param printerName Nombre de la impresora
     * @param copies Número de copias a imprimir
     * @throws Exception Si ocurre un error durante la impresión
     */
    public static void printReceipt(Transaction transaction, String printerName, int copies) throws Exception {
        if (transaction == null) {
            throw new IllegalArgumentException("La transacción no puede ser nula");
        }

        PrintService printService = findPrintService(printerName);
        if (printService == null) {
            throw new Exception("No se encontró la impresora: " + printerName);
        }

        for (int i = 0; i < copies; i++) {
            try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
                 EscPos escpos = new EscPos(printerOutputStream)) {

                printReceiptContent(escpos, transaction, i == 0 ? "ORIGINAL" : "COPIA");

                escpos.feed(5);
                escpos.cut(EscPos.CutMode.FULL);

            } catch (IOException e) {
                logger.error("Error al imprimir recibo", e);
                throw new Exception("Error al imprimir: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Imprime el contenido del recibo en el dispositivo ESC/POS
     */
    private static void printReceiptContent(EscPos escpos, Transaction transaction, String copyType) throws IOException {
        Style titleStyle = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center);

        Style boldStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setBold(true);

        Style normalStyle = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setBold(false);

        Style centerStyle = new Style()
                .setJustification(EscPosConst.Justification.Center);

        // Logo (si existe)
        try {
            InputStream logoStream = LoanReceiptService.class.getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                BufferedImage bufferedImage = ImageIO.read(logoStream);
                com.github.anastaciocintra.escpos.image.CoffeeImageImpl coffeeImage =
                    new com.github.anastaciocintra.escpos.image.CoffeeImageImpl(bufferedImage);
                com.github.anastaciocintra.escpos.image.BitonalThreshold threshold =
                    new com.github.anastaciocintra.escpos.image.BitonalThreshold();
                com.github.anastaciocintra.escpos.image.EscPosImage escposImage =
                    new com.github.anastaciocintra.escpos.image.EscPosImage(coffeeImage, threshold);
                RasterBitImageWrapper imageWrapper = new RasterBitImageWrapper();
                escpos.write(imageWrapper, escposImage);
                escpos.feed(1);
                logoStream.close();
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar el logo", e);
        }

        // Encabezado
        escpos.writeLF(titleStyle, "INVENTARIO MUNICIPAL");
        escpos.feed(1);
        escpos.writeLF(centerStyle, "RECIBO DE PRESTAMO");
        escpos.writeLF(centerStyle, copyType);
        escpos.writeLF(centerStyle, "================================");
        escpos.feed(1);

        // Información del préstamo
        String loanCode = transaction.getString("loan_code");
        escpos.writeLF(boldStyle, "Codigo: " + loanCode);
        escpos.feed(1);

        // Fecha y hora
        Date createdAt = transaction.getTimestamp("created_at");
        escpos.writeLF(normalStyle, "Fecha: " + dateFormat.format(createdAt));
        escpos.writeLF(normalStyle, "Hora: " + timeFormat.format(createdAt));
        escpos.feed(1);

        // Usuario que realizó el préstamo
        User user = transaction.parent(User.class);
        if (user != null) {
            escpos.writeLF(normalStyle, "Realizado por:");
            escpos.writeLF(boldStyle, "  " + user.getString("name"));
            escpos.writeLF(normalStyle, "  Usuario: " + user.getString("username").toUpperCase());
        }
        escpos.feed(1);

        // Persona que recibe el préstamo
        Person person = transaction.parent(Person.class);
        if (person != null) {
            escpos.writeLF(normalStyle, "Prestado a:");
            escpos.writeLF(boldStyle, "  " + person.getString("first_name") + " " + person.getString("last_name"));
            escpos.writeLF(normalStyle, "  DNI: " + person.getString("dni"));
            String phone = person.getString("phone");
            if (phone != null && !phone.isEmpty()) {
                escpos.writeLF(normalStyle, "  Tel: " + phone);
            }
        }
        escpos.feed(1);

        // Fecha de devolución esperada
        Date expectedReturnDate = transaction.getDate("expected_return_date");
        if (expectedReturnDate != null) {
            escpos.writeLF(boldStyle, "Devolucion esperada:");
            escpos.writeLF(normalStyle, "  " + dateFormat.format(expectedReturnDate));
            escpos.feed(1);
        }

        // Línea separadora
        escpos.writeLF(normalStyle, "================================");
        escpos.feed(1);

        // Artículos prestados
        escpos.writeLF(boldStyle, "ARTICULOS PRESTADOS:");
        escpos.feed(1);

        List<TransactionItem> items = transaction.getAll(TransactionItem.class);
        int itemNumber = 1;
        for (TransactionItem item : items) {
            Product product = item.parent(Product.class);
            if (product != null) {
                escpos.writeLF(normalStyle, itemNumber + ". " + product.getString("name"));
                escpos.writeLF(normalStyle, "   Codigo: " + product.getString("code"));
                escpos.writeLF(normalStyle, "   Cantidad: " + item.getDouble("quantity") + " " +
                              (product.getString("unit") != null ? product.getString("unit") : "unidad(es)"));

                String itemNotes = item.getString("notes");
                if (itemNotes != null && !itemNotes.isEmpty()) {
                    escpos.writeLF(normalStyle, "   Nota: " + itemNotes);
                }
                escpos.feed(1);
                itemNumber++;
            }
        }

        // Notas generales
        String notes = transaction.getString("notes");
        if (notes != null && !notes.isEmpty()) {
            escpos.writeLF(normalStyle, "================================");
            escpos.writeLF(boldStyle, "NOTAS:");
            escpos.writeLF(normalStyle, notes);
            escpos.feed(1);
        }

        // Pie de página
        escpos.writeLF(normalStyle, "================================");
        escpos.feed(1);
        escpos.writeLF(centerStyle, "Total de articulos: " + items.size());
        escpos.feed(1);
        escpos.writeLF(centerStyle, "GRACIAS POR SU CONFIANZA");
        escpos.feed(1);
        escpos.writeLF(centerStyle, "Firma: _________________");
    }

    /**
     * Genera un PDF del recibo de préstamo
     * @param transaction Transacción del préstamo
     * @param outputPath Ruta donde guardar el PDF
     * @throws Exception Si ocurre un error durante la generación
     */
    public static void generatePdfReceipt(Transaction transaction, String outputPath) throws Exception {
        if (transaction == null) {
            throw new IllegalArgumentException("La transacción no puede ser nula");
        }

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Configuración del documento
            document.setMargins(30, 30, 30, 30);

            // Logo (si existe)
            try {
                InputStream logoStream = LoanReceiptService.class.getResourceAsStream("/images/logo.png");
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    Image logo = new Image(ImageDataFactory.create(logoBytes));
                    logo.setWidth(100);
                    logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    document.add(logo);
                }
            } catch (Exception e) {
                logger.warn("No se pudo cargar el logo para PDF", e);
            }

            // Título
            Paragraph title = new Paragraph("INVENTARIO MUNICIPAL")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("RECIBO DE PRÉSTAMO")
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(subtitle);

            document.add(new Paragraph("\n"));

            // Información del préstamo
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            infoTable.setWidth(UnitValue.createPercentValue(100));

            String loanCode = transaction.getString("loan_code");
            addTableRow(infoTable, "Código:", loanCode, true);

            Date createdAt = transaction.getTimestamp("created_at");
            addTableRow(infoTable, "Fecha:", dateFormat.format(createdAt), false);
            addTableRow(infoTable, "Hora:", timeFormat.format(createdAt), false);

            // Usuario
            User user = transaction.parent(User.class);
            if (user != null) {
                addTableRow(infoTable, "Realizado por:", user.getString("name"), false);
                addTableRow(infoTable, "Usuario:", user.getString("username"), false);
            }

            // Persona
            Person person = transaction.parent(Person.class);
            if (person != null) {
                addTableRow(infoTable, "Prestado a:",
                           person.getString("first_name") + " " + person.getString("last_name"), true);
                addTableRow(infoTable, "DNI:", person.getString("dni"), false);
                String phone = person.getString("phone");
                if (phone != null && !phone.isEmpty()) {
                    addTableRow(infoTable, "Teléfono:", phone, false);
                }
            }

            // Fecha de devolución
            Date expectedReturnDate = transaction.getDate("expected_return_date");
            if (expectedReturnDate != null) {
                addTableRow(infoTable, "Devolución esperada:", dateFormat.format(expectedReturnDate), true);
            }

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Tabla de artículos
            Paragraph itemsTitle = new Paragraph("ARTÍCULOS PRESTADOS")
                    .setFontSize(14)
                    .setBold();
            document.add(itemsTitle);

            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 1, 2}));
            itemsTable.setWidth(UnitValue.createPercentValue(100));

            // Encabezados
            itemsTable.addHeaderCell(createHeaderCell("#"));
            itemsTable.addHeaderCell(createHeaderCell("Artículo"));
            itemsTable.addHeaderCell(createHeaderCell("Código"));
            itemsTable.addHeaderCell(createHeaderCell("Cantidad"));
            itemsTable.addHeaderCell(createHeaderCell("Unidad"));

            // Items
            List<TransactionItem> items = transaction.getAll(TransactionItem.class);
            int itemNumber = 1;
            for (TransactionItem item : items) {
                Product product = item.parent(Product.class);
                if (product != null) {
                    itemsTable.addCell(createCell(String.valueOf(itemNumber)));
                    itemsTable.addCell(createCell(product.getString("name")));
                    itemsTable.addCell(createCell(product.getString("code")));
                    itemsTable.addCell(createCell(String.valueOf(item.getDouble("quantity"))));
                    itemsTable.addCell(createCell(product.getString("unit") != null ?
                                                  product.getString("unit") : "unidad(es)"));
                    itemNumber++;
                }
            }

            document.add(itemsTable);
            document.add(new Paragraph("\n"));

            // Notas
            String notes = transaction.getString("notes");
            if (notes != null && !notes.isEmpty()) {
                Paragraph notesTitle = new Paragraph("NOTAS:")
                        .setFontSize(12)
                        .setBold();
                document.add(notesTitle);
                Paragraph notesContent = new Paragraph(notes);
                document.add(notesContent);
                document.add(new Paragraph("\n"));
            }

            // Pie de página
            document.add(new Paragraph("\n\n"));

            Paragraph totalItems = new Paragraph("Total de artículos: " + items.size())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold();
            document.add(totalItems);

            document.add(new Paragraph("\n\n"));

            Paragraph signature = new Paragraph("Firma: _____________________________")
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(signature);

            Paragraph footer = new Paragraph("GRACIAS POR SU CONFIANZA")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic();
            document.add(footer);

            logger.info("PDF generado exitosamente en: {}", outputPath);
        } catch (Exception e) {
            logger.error("Error al generar PDF", e);
            throw new Exception("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un PDF del recibo de préstamo usando un DTO
     * Este método es seguro para usar en hilos secundarios ya que no accede a la BD
     * @param receiptDTO DTO con los datos del recibo
     * @param outputPath Ruta donde guardar el PDF
     * @throws Exception Si ocurre un error durante la generación
     */
    public static void generatePdfReceiptFromDTO(LoanReceiptDTO receiptDTO, String outputPath) throws Exception {
        if (receiptDTO == null) {
            throw new IllegalArgumentException("El DTO no puede ser nulo");
        }

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Configuración del documento con márgenes más amplios
            document.setMargins(40, 40, 40, 40);

            // ============ ENCABEZADO MEJORADO Y PROFESIONAL ============

            // Tabla para el encabezado (3 columnas: Logo - Título - Info)
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBorder(Border.NO_BORDER);

            // Columna 1: Logo
            Cell logoCell = new Cell();
            logoCell.setBorder(Border.NO_BORDER);
            try {
                InputStream logoStream = LoanReceiptService.class.getResourceAsStream("/images/logo.png");
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    Image logo = new Image(ImageDataFactory.create(logoBytes));
                    logo.setWidth(80);
                    logo.setHeight(80);
                    logoCell.add(logo);
                }
            } catch (Exception e) {
                logger.warn("No se pudo cargar el logo para PDF", e);
            }
            headerTable.addCell(logoCell);

            // Columna 2: Título central
            Cell titleCell = new Cell();
            titleCell.setBorder(Border.NO_BORDER);
            titleCell.setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

            Paragraph mainTitle = new Paragraph("INVENTARIO MUNICIPAL")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.BLACK);

            Paragraph headerSubtitle = new Paragraph("Sistema de Gestión de Préstamos")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(100, 100, 100))
                    .setMarginTop(5);

            titleCell.add(mainTitle);
            titleCell.add(headerSubtitle);
            headerTable.addCell(titleCell);

            // Columna 3: Información de fecha/hora
            Cell infoCell = new Cell();
            infoCell.setBorder(Border.NO_BORDER);
            infoCell.setTextAlignment(TextAlignment.RIGHT);
            infoCell.setFontSize(8);

            Paragraph dateInfo = new Paragraph("Fecha de emisión:")
                    .setFontSize(8)
                    .setBold()
                    .setMarginBottom(2);
            Paragraph dateValue = new Paragraph(dateFormat.format(receiptDTO.getCreatedAt()))
                    .setFontSize(9)
                    .setMarginBottom(5);

            Paragraph timeInfo = new Paragraph("Hora:")
                    .setFontSize(8)
                    .setBold()
                    .setMarginBottom(2);
            Paragraph timeValue = new Paragraph(timeFormat.format(receiptDTO.getCreatedAt()))
                    .setFontSize(9);

            infoCell.add(dateInfo);
            infoCell.add(dateValue);
            infoCell.add(timeInfo);
            infoCell.add(timeValue);
            headerTable.addCell(infoCell);

            document.add(headerTable);

            // Línea decorativa superior
            com.itextpdf.layout.element.LineSeparator separator = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(2f));
            separator.setMarginTop(10);
            separator.setMarginBottom(5);
            document.add(separator);

            // Título del recibo destacado con fondo de color
            Paragraph receiptTitle = new Paragraph("RECIBO DE PRÉSTAMO")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.WHITE)
                    .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(41, 128, 185))
                    .setPadding(10)
                    .setMarginTop(5)
                    .setMarginBottom(5);
            document.add(receiptTitle);

            // Código del préstamo destacado
            Paragraph loanCodeParagraph = new Paragraph()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15);

            loanCodeParagraph.add(new com.itextpdf.layout.element.Text("Código de Préstamo: ")
                    .setFontSize(11)
                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(100, 100, 100)));

            loanCodeParagraph.add(new com.itextpdf.layout.element.Text(receiptDTO.getLoanCode())
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(41, 128, 185)));

            document.add(loanCodeParagraph);

            // Línea decorativa inferior
            com.itextpdf.layout.element.LineSeparator separator2 = new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f));
            separator2.setMarginBottom(15);
            document.add(separator2);

            // Información del préstamo
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            infoTable.setWidth(UnitValue.createPercentValue(100));


            // Usuario
            if (receiptDTO.getUserName() != null) {
                addTableRow(infoTable, "Realizado por:", receiptDTO.getUserName(), false);
                addTableRow(infoTable, "Usuario:", receiptDTO.getUserUsername(), false);
            }

            // Persona
            if (receiptDTO.getPersonFirstName() != null) {
                addTableRow(infoTable, "Prestado a:",
                           receiptDTO.getPersonFirstName() + " " + receiptDTO.getPersonLastName(), true);
                addTableRow(infoTable, "DNI:", receiptDTO.getPersonDni(), false);
                String phone = receiptDTO.getPersonPhone();
                if (phone != null && !phone.isEmpty()) {
                    addTableRow(infoTable, "Teléfono:", phone, false);
                }
            }

            // Fecha de devolución
            Date expectedReturnDate = receiptDTO.getExpectedReturnDate();
            if (expectedReturnDate != null) {
                addTableRow(infoTable, "Devolución esperada:", dateFormat.format(expectedReturnDate), true);
            }

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Tabla de artículos
            Paragraph itemsTitle = new Paragraph("ARTÍCULOS PRESTADOS")
                    .setFontSize(14)
                    .setBold();
            document.add(itemsTitle);

            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 1, 2}));
            itemsTable.setWidth(UnitValue.createPercentValue(100));

            // Encabezados
            itemsTable.addHeaderCell(createHeaderCell("#"));
            itemsTable.addHeaderCell(createHeaderCell("Artículo"));
            itemsTable.addHeaderCell(createHeaderCell("Código"));
            itemsTable.addHeaderCell(createHeaderCell("Cantidad"));
            itemsTable.addHeaderCell(createHeaderCell("Unidad"));

            // Items
            List<LoanReceiptDTO.LoanReceiptItemDTO> items = receiptDTO.getItems();
            int itemNumber = 1;
            for (LoanReceiptDTO.LoanReceiptItemDTO item : items) {
                itemsTable.addCell(createCell(String.valueOf(itemNumber)));
                itemsTable.addCell(createCell(item.getProductName()));
                itemsTable.addCell(createCell(item.getProductCode()));
                itemsTable.addCell(createCell(String.valueOf(item.getQuantity())));
                itemsTable.addCell(createCell(item.getUnit() != null ? item.getUnit() : "unidad(es)"));
                itemNumber++;
            }

            document.add(itemsTable);
            document.add(new Paragraph("\n"));

            // Notas
            String notes = receiptDTO.getNotes();
            if (notes != null && !notes.isEmpty()) {
                Paragraph notesTitle = new Paragraph("NOTAS:")
                        .setFontSize(12)
                        .setBold();
                document.add(notesTitle);
                Paragraph notesContent = new Paragraph(notes);
                document.add(notesContent);
                document.add(new Paragraph("\n"));
            }

            // Pie de página
            document.add(new Paragraph("\n\n"));

            Paragraph totalItems = new Paragraph("Total de artículos: " + items.size())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold();
            document.add(totalItems);

            document.add(new Paragraph("\n\n"));

            Paragraph signature = new Paragraph("Firma: _____________________________")
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(signature);

            Paragraph footer = new Paragraph("GRACIAS POR SU CONFIANZA")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic();
            document.add(footer);

            logger.info("PDF generado exitosamente en: {}", outputPath);
        } catch (Exception e) {
            logger.error("Error al generar PDF", e);
            throw new Exception("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un servicio de impresión por nombre
     */
    private static PrintService findPrintService(String printerName) {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService printService : printServices) {
            if (printService.getName().equalsIgnoreCase(printerName)) {
                return printService;
            }
        }
        return null;
    }

    /**
     * Agrega una fila a la tabla de información
     */
    private static void addTableRow(Table table, String label, String value, boolean bold) {
        Cell labelCell = new Cell().add(new Paragraph(label).setBold());
        labelCell.setBorder(Border.NO_BORDER);
        table.addCell(labelCell);

        Paragraph valueParagraph = new Paragraph(value != null ? value : "");
        if (bold) {
            valueParagraph.setBold();
        }
        Cell valueCell = new Cell().add(valueParagraph);
        valueCell.setBorder(Border.NO_BORDER);
        table.addCell(valueCell);
    }

    /**
     * Crea una celda de encabezado para tablas
     */
    private static Cell createHeaderCell(String content) {
        return new Cell()
                .add(new Paragraph(content).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }

    /**
     * Crea una celda normal para tablas
     */
    private static Cell createCell(String content) {
        return new Cell()
                .add(new Paragraph(content != null ? content : ""))
                .setTextAlignment(TextAlignment.LEFT);
    }
}

