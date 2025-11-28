# Sistema de Impresión de Recibos de Préstamos

## Fecha de Implementación
27 de Noviembre de 2025

## Descripción General
Se ha implementado un sistema profesional para emitir recibos/tickets de préstamos que permite:
- Imprimir en impresoras térmicas (compatible con Epson M249A)
- Generar recibos en formato PDF
- Emitir original y copia automáticamente
- Re-imprimir recibos desde la tabla de préstamos

## Componentes Implementados

### 1. Servicio de Impresión (`LoanReceiptService.java`)
**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/services/LoanReceiptService.java`

**Características:**
- Detecta automáticamente impresoras disponibles en el sistema
- Imprime recibos en impresoras térmicas usando ESC/POS
- Genera PDFs con formato profesional
- Incluye logotipo de la organización
- Muestra información completa del préstamo:
  - Código del préstamo
  - Fecha y hora de emisión
  - Usuario que realizó el préstamo
  - Persona que recibe el préstamo (con DNI y teléfono)
  - Fecha esperada de devolución
  - Lista detallada de artículos prestados
  - Notas adicionales
  - Espacio para firma

**Métodos principales:**
```java
// Obtener impresoras disponibles
List<String> getAvailablePrinters()

// Imprimir en impresora térmica
void printReceipt(Transaction transaction, String printerName, int copies)

// Generar PDF
void generatePdfReceipt(Transaction transaction, String outputPath)
```

### 2. Diálogo de Impresión (`PrintReceiptDialog.java`)
**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/utils/PrintReceiptDialog.java`

**Características:**
- Interfaz gráfica intuitiva para seleccionar opciones de impresión
- Dos modos de salida:
  1. **Impresora Térmica:**
     - Selector de impresora disponible
     - Control de número de copias (1-5)
  2. **Archivo PDF:**
     - Selector de ubicación de archivo
     - Nombre de archivo sugerido con código de préstamo y fecha
     - Opción para abrir carpeta destino después de generar

**Características adicionales:**
- Validación de selecciones antes de proceder
- Mensajes de progreso durante impresión/generación
- Manejo de errores con mensajes informativos
- Ejecución en hilos separados para no bloquear la UI

### 3. Integración con LoansController
**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/controllers/dashboard/LoansController.java`

**Cambios realizados:**

#### a) Emisión automática al crear préstamo
Cuando se crea un nuevo préstamo, se muestra un mensaje de éxito con dos opciones:
- **Emitir Recibo:** Abre el diálogo de impresión
- **Cerrar:** Cierra sin imprimir

```java
Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
successAlert.setTitle("Éxito");
successAlert.setHeaderText("Préstamo registrado exitosamente");
// ... código ...
ButtonType emitReceiptButton = new ButtonType("Emitir Recibo");
ButtonType closeButton = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
```

#### b) Botón de recibo en la tabla
Se agregó un botón "Recibo" en la columna de acciones de cada préstamo:
- Estilo: Botón azul (#2196F3)
- Visible siempre (tanto para préstamos activos como inactivos)
- Tooltip: "Emitir recibo del préstamo"
- Acción: Abre el diálogo de impresión

```java
private final Button btnPrintReceipt = new Button("Recibo");
btnPrintReceipt.setStyle("-fx-font-size: 12; -fx-background-color: #2196F3; -fx-text-fill: white;");
```

## Dependencias Agregadas

### pom.xml
```xml
<!-- ESC/POS para impresoras térmicas -->
<dependency>
    <groupId>com.github.anastaciocintra</groupId>
    <artifactId>escpos-coffee</artifactId>
    <version>4.1.0</version>
</dependency>

<!-- iText para generación de PDF -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
```

### module-info.java
```java
requires escpos.coffee;
requires io;         // iText io
requires kernel;     // iText kernel
requires layout;     // iText layout
```

## Formato del Recibo

### Estructura del Recibo Impreso/PDF:
```
================================
[LOGO]
INVENTARIO MUNICIPAL
RECIBO DE PRÉSTAMO
ORIGINAL/COPIA
================================

Código: P-XX20251127-001
Fecha: 27/11/2025
Hora: 14:30:00

Realizado por:
  Juan Pérez
  Usuario: jperez

Prestado a:
  María García López
  DNI: 12345678
  Tel: 555-1234

Devolución esperada:
  04/12/2025

================================
ARTÍCULOS PRESTADOS:

1. Laptop HP ProBook
   Código: LAP001
   Cantidad: 1.0 unidad(es)

2. Mouse Inalámbrico
   Código: MOU001
   Cantidad: 2.0 unidad(es)

================================
NOTAS:
Manejar con cuidado

================================
Total de artículos: 2

GRACIAS POR SU CONFIANZA

Firma: _________________
```

## Flujo de Uso

### Caso 1: Nuevo Préstamo
1. Usuario crea un nuevo préstamo desde el módulo de Préstamos
2. Completa todos los datos y productos
3. Guarda el préstamo
4. Se muestra mensaje de éxito con opción "Emitir Recibo"
5. Usuario hace clic en "Emitir Recibo"
6. Se abre diálogo con opciones:
   - Selecciona impresora térmica o PDF
   - Configura opciones (copias, ubicación)
7. Click en "Emitir"
8. Se genera/imprime el recibo

### Caso 2: Re-imprimir Recibo Existente
1. Usuario navega a la tabla de préstamos
2. Localiza el préstamo deseado
3. Hace clic en el botón azul "Recibo"
4. Se abre diálogo con opciones de impresión
5. Procede con la emisión

## Manejo de Errores

El sistema maneja los siguientes casos de error:
- **No hay impresoras disponibles:** Deshabilita la opción de impresora térmica
- **Impresora no encontrada:** Mensaje de error específico
- **Error de impresión:** Muestra detalles del error
- **Error al generar PDF:** Mensaje con causa del error
- **Ubicación de PDF inválida:** Solicita seleccionar ubicación válida

## Ventajas del Sistema

1. **Profesionalismo:** Recibos formales con logo e información completa
2. **Flexibilidad:** Opción de impresora térmica o PDF según necesidad
3. **Respaldo:** Emisión automática de original y copia
4. **Re-impresión:** Posibilidad de reimprimir recibos antiguos
5. **Portabilidad:** PDFs pueden enviarse por correo o mensajería
6. **Comprobante:** Firma manual para registro físico

## Notas Técnicas

### Impresoras Térmicas
- Compatible con cualquier impresora que soporte ESC/POS
- Probado específicamente con Epson M249A
- Ancho de papel: 80mm (estándar)
- Requiere drivers de impresora instalados en el sistema

### Generación de PDF
- Formato A4
- Fuentes embebidas
- Compatible con todos los lectores PDF
- Tamaño aproximado: 50-100 KB por recibo

### Rendimiento
- Impresión térmica: ~2-5 segundos
- Generación PDF: ~1-2 segundos
- Operaciones en hilos separados (no bloquea UI)

## Configuración Requerida

### Logo de Organización
- Ubicación: `src/main/resources/images/logo.png`
- Formato recomendado: PNG con fondo transparente
- Dimensiones: 200x200 px (se escala automáticamente)

### Pruebas Recomendadas
1. Verificar que la impresora térmica esté correctamente instalada
2. Probar impresión en modo prueba antes de uso real
3. Verificar que el logo se muestre correctamente
4. Probar generación de PDF en diferentes ubicaciones

## Posibles Mejoras Futuras

1. **Plantillas personalizables:** Permitir diferentes estilos de recibo
2. **Envío por correo:** Opción de enviar PDF por email
3. **Código QR:** Agregar QR con información del préstamo
4. **Impresión múltiple:** Imprimir varios recibos a la vez
5. **Historial de impresiones:** Registrar cuándo se imprimieron recibos
6. **Vista previa:** Mostrar preview antes de imprimir

## Soporte y Mantenimiento

Para modificar el formato del recibo, editar:
- **Impresora térmica:** Método `printReceiptContent()` en `LoanReceiptService.java`
- **PDF:** Método `generatePdfReceipt()` en `LoanReceiptService.java`

Para cambiar estilos:
- **Colores ESC/POS:** Modificar objetos `Style` en `printReceiptContent()`
- **Colores PDF:** Ajustar `ColorConstants` en `generatePdfReceipt()`

## Conclusión

El sistema de impresión de recibos mejora significativamente la profesionalidad del sistema de préstamos, proporcionando comprobantes físicos formales que pueden utilizarse como respaldo legal y para auditorías. La implementación es flexible, permitiendo adaptarse a diferentes necesidades y equipos disponibles.

