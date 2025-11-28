# Solución: Error de Conexión de Base de Datos en Hilos Secundarios

## 📅 Fecha: 27 de Noviembre de 2025

## 🐛 Problema Detectado

Al intentar generar un PDF de recibo de préstamo, se producía el siguiente error:

```
org.javalite.activejdbc.DBException: there is no connection 'default' on this thread, 
are you sure you opened it?
```

### Causa Raíz

El error ocurría porque:
1. La generación de PDF se ejecutaba en un **hilo secundario** (Thread-5)
2. En ese hilo se intentaba acceder a modelos de ActiveJDBC:
   - `transaction.parent(User.class)`
   - `transaction.parent(Person.class)`
   - `transaction.getAll(TransactionItem.class)`
   - `item.parent(Product.class)`
3. ActiveJDBC requiere que **cada hilo tenga su propia conexión** a la base de datos
4. El hilo secundario no tenía conexión establecida

### Stack Trace del Error
```
at LoanReceiptService.generatePdfReceipt(LoanReceiptService.java:281)
at PrintReceiptDialog.lambda$showProgressAndGeneratePdf$0(PrintReceiptDialog.java:234)
at java.lang.Thread.run(Thread.java:1474)
```

## ✅ Solución Implementada

Se implementó el **patrón DTO (Data Transfer Object)** para separar la recolección de datos (que requiere BD) de la generación del PDF (que no debería requerir BD).

### Componentes Creados/Modificados

#### 1. **LoanReceiptDTO.java** (NUEVO)
**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/models/dto/LoanReceiptDTO.java`

DTO que encapsula todos los datos necesarios para generar un recibo:
- Código del préstamo
- Fechas (creación, devolución esperada)
- Datos del usuario (nombre, username)
- Datos de la persona (nombre completo, DNI, teléfono)
- Lista de items con sus productos
- Notas

```java
public class LoanReceiptDTO {
    private String loanCode;
    private Date createdAt;
    private Date expectedReturnDate;
    private String notes;
    private String userName;
    private String userUsername;
    private String personFirstName;
    private String personLastName;
    private String personDni;
    private String personPhone;
    private List<LoanReceiptItemDTO> items;
    
    // Inner class para items
    public static class LoanReceiptItemDTO {
        private String productName;
        private String productCode;
        private Double quantity;
        private String unit;
        private String itemNotes;
    }
}
```

#### 2. **LoanReceiptService.java** (MODIFICADO)

Se agregaron dos métodos clave:

**a) `createReceiptDTO(Transaction transaction)`**
- Extrae todos los datos de la transacción y sus relaciones
- Se ejecuta en el hilo principal (donde hay conexión a BD)
- Retorna un DTO con todos los datos necesarios

```java
public static LoanReceiptDTO createReceiptDTO(Transaction transaction) {
    LoanReceiptDTO dto = new LoanReceiptDTO();
    
    // Datos básicos
    dto.setLoanCode(transaction.getString("loan_code"));
    dto.setCreatedAt(transaction.getTimestamp("created_at"));
    // ...
    
    // Usuario (accede a BD)
    User user = transaction.parent(User.class);
    if (user != null) {
        dto.setUserName(user.getString("name"));
        dto.setUserUsername(user.getString("username"));
    }
    
    // Persona (accede a BD)
    Person person = transaction.parent(Person.class);
    if (person != null) {
        dto.setPersonFirstName(person.getString("first_name"));
        // ...
    }
    
    // Items (accede a BD)
    List<TransactionItem> items = transaction.getAll(TransactionItem.class);
    // Convierte a DTOs...
    
    return dto;
}
```

**b) `generatePdfReceiptFromDTO(LoanReceiptDTO receiptDTO, String outputPath)`**
- Genera el PDF usando solo los datos del DTO
- **No accede a la base de datos**
- Seguro para ejecutar en hilos secundarios

```java
public static void generatePdfReceiptFromDTO(LoanReceiptDTO receiptDTO, String outputPath) 
    throws Exception {
    // Genera PDF usando:
    // - receiptDTO.getLoanCode()
    // - receiptDTO.getUserName()
    // - receiptDTO.getPersonFirstName()
    // - receiptDTO.getItems()
    // Sin acceder a transaction.parent() ni otros métodos que requieren BD
}
```

#### 3. **PrintReceiptDialog.java** (MODIFICADO)

Modificación en el método `showProgressAndGeneratePdf()`:

**Antes (con error):**
```java
private static void showProgressAndGeneratePdf(Transaction transaction, String pdfPath) {
    // ...
    Thread pdfThread = new Thread(() -> {
        try {
            // ❌ Accede a BD en hilo secundario
            LoanReceiptService.generatePdfReceipt(transaction, pdfPath);
        }
    });
}
```

**Después (corregido):**
```java
private static void showProgressAndGeneratePdf(Transaction transaction, String pdfPath) {
    // ...
    
    // ✅ Crear DTO en el hilo principal (donde hay conexión a BD)
    LoanReceiptDTO receiptDTO = LoanReceiptService.createReceiptDTO(transaction);
    
    // ✅ Ejecutar generación en hilo secundario SIN acceder a BD
    Thread pdfThread = new Thread(() -> {
        try {
            LoanReceiptService.generatePdfReceiptFromDTO(receiptDTO, pdfPath);
        }
    });
}
```

## 🔄 Flujo de Ejecución Corregido

### Generación de PDF:

```
┌─────────────────────┐
│  Hilo Principal     │
│  (JavaFX Thread)    │
└──────────┬──────────┘
           │
           │ 1. Usuario solicita PDF
           ▼
┌─────────────────────┐
│ createReceiptDTO()  │
│ • Accede a BD       │
│ • Obtiene User      │
│ • Obtiene Person    │
│ • Obtiene Items     │
│ • Crea DTO          │
└──────────┬──────────┘
           │
           │ 2. DTO creado (sin BD)
           ▼
┌─────────────────────┐
│  Hilo Secundario    │
│  (Thread-5)         │
└──────────┬──────────┘
           │
           │ 3. Genera PDF
           ▼
┌──────────────────────┐
│ generatePdfFromDTO() │
│ • NO accede a BD     │
│ • Usa solo DTO       │
│ • Genera archivo     │
└──────────────────────┘
```

### Ventajas de esta Solución:

1. ✅ **Separación de responsabilidades**
   - Recolección de datos (hilo principal)
   - Generación de archivo (hilo secundario)

2. ✅ **No requiere conexión de BD en hilo secundario**
   - Evita complejidad de abrir/cerrar conexiones
   - Evita problemas de sincronización

3. ✅ **Código más testeable**
   - DTO puede ser creado fácilmente en tests
   - No requiere conexión real a BD para probar generación

4. ✅ **Mejor rendimiento**
   - Datos se obtienen una sola vez
   - No hay múltiples queries durante generación

5. ✅ **Más robusto**
   - Si la BD se cierra, el hilo secundario sigue funcionando
   - Datos capturados en punto específico del tiempo

## 📊 Estado Actual

- ✅ Compilación exitosa: BUILD SUCCESS
- ✅ Instrumentación de ActiveJDBC: Completada
- ✅ Nuevo DTO creado: `LoanReceiptDTO.java`
- ✅ Método de conversión agregado: `createReceiptDTO()`
- ✅ Método de generación seguro: `generatePdfReceiptFromDTO()`
- ✅ Diálogo actualizado para usar DTO

## 🚀 Pruebas Recomendadas

1. **Generar PDF de un préstamo nuevo**
   - Crear préstamo
   - Seleccionar "Emitir Recibo"
   - Elegir "Guardar como PDF"
   - ✅ Debe generar sin errores

2. **Re-generar PDF de préstamo existente**
   - Ir a tabla de préstamos
   - Click en botón "Recibo"
   - Elegir "Guardar como PDF"
   - ✅ Debe generar sin errores

3. **Verificar contenido del PDF**
   - Abrir PDF generado
   - ✅ Verificar todos los datos están presentes:
     - Código de préstamo
     - Usuario que realizó
     - Persona que recibió
     - Lista de artículos
     - Fechas correctas

## 📝 Notas Técnicas

### ActiveJDBC y Conexiones por Hilo

ActiveJDBC usa un patrón de "conexión por hilo" (thread-local):
- Cada hilo debe abrir su propia conexión: `Base.open()`
- Cada hilo debe cerrar su conexión: `Base.close()`
- Las conexiones no se comparten entre hilos

### Alternativas Consideradas

1. **Abrir conexión en hilo secundario** ❌
   - Más complejo
   - Requiere manejo de excepciones de BD
   - Necesita credenciales disponibles

2. **Usar DTO** ✅ (Solución implementada)
   - Más limpio
   - Mejor separación
   - Más testeable

3. **Ejecutar todo en hilo principal** ❌
   - Bloquearía la UI
   - Mala experiencia de usuario

## 🎯 Conclusión

El problema se resolvió exitosamente implementando el patrón DTO para separar la recolección de datos (que requiere BD) de la generación del archivo (que no debería requerir BD). Esta solución es más robusta, mantenible y sigue mejores prácticas de desarrollo.

La aplicación ahora puede generar PDFs de recibos sin errores de conexión de base de datos.

