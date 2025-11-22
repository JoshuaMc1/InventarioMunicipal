# Plan de Mejora: Transacciones Multi-Producto

## Estado: 📋 PROPUESTA - No Implementado

**Fecha de Propuesta:** 21 de Noviembre de 2025  
**Prioridad:** Media  
**Complejidad:** Alta  
**Tiempo Estimado:** 2-3 semanas de desarrollo + 1 semana de testing

---

## 📌 Contexto Actual

Actualmente, el sistema **Inventario Municipal** maneja transacciones **mono-producto**, es decir:
- **1 Transacción = 1 Producto con su cantidad**
- Para registrar múltiples productos, se deben crear múltiples transacciones independientes
- Cada transacción tiene su propio registro individual

**Ejemplo actual:**
```
Transacción #1: 10 sillas
Transacción #2: 5 escritorios  
Transacción #3: 20 resmas de papel
```

### ¿Por qué fue diseñado así?

1. **Simplicidad:** Más fácil de implementar y mantener
2. **Contexto Municipal:** Los movimientos en municipalidades suelen ser uno a uno
3. **Trazabilidad:** Cada producto tiene su propio registro independiente
4. **Auditoría:** Más simple rastrear movimientos individuales

---

## 🎯 Objetivo de la Mejora

Permitir que una **única transacción pueda contener múltiples productos**, similar a:
- Una orden de compra con varios artículos
- Una entrega con múltiples productos
- Un préstamo de varios equipos simultáneos

**Ejemplo mejorado:**
```
Transacción #1:
  - 10 sillas
  - 5 escritorios
  - 20 resmas de papel
  Fecha: 21/11/2025
  Proveedor: Proveedor ABC
  Notas: Compra de mobiliario para oficina nueva
```

---

## 📊 Cambios en Base de Datos

### 1. Modificar tabla `transactions` (convertir a Header)

**Actual:**
```sql
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,              -- ❌ Eliminar
    type VARCHAR(20) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,      -- ❌ Mover a transaction_items
    date DATE DEFAULT (CURRENT_DATE),
    user_id INT NOT NULL,
    person_id INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    CHECK (type IN ('in', 'out', 'loan'))
);
```

**Nueva estructura (Header):**
```sql
CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    date DATE DEFAULT (CURRENT_DATE),
    user_id INT NOT NULL,
    person_id INT,
    notes TEXT,
    total_items INT DEFAULT 0,           -- ✅ Nuevo: contador de productos
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    FOREIGN KEY (person_id) REFERENCES people(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CHECK (type IN ('in', 'out', 'loan'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2. Crear nueva tabla `transaction_items` (Detalle)

```sql
CREATE TABLE transaction_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    notes TEXT,                          -- Notas específicas del producto
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,               -- Si se borra la transacción, borra los items
    FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,              -- No permitir borrar producto si está en items
    INDEX idx_transaction (transaction_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3. Script de Migración de Datos Existentes

**Archivo:** `migrations/2025MMDD_HHMMSS_migrate_to_multiproduct_transactions.sql`

```sql
-- Migración de transacciones mono-producto a multi-producto
-- ADVERTENCIA: Hacer backup antes de ejecutar

START TRANSACTION;

-- 1. Crear tabla temporal con datos actuales
CREATE TABLE transactions_backup AS SELECT * FROM transactions;

-- 2. Crear nueva tabla de items
CREATE TABLE transaction_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_transaction (transaction_id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Migrar datos: cada transacción actual se convierte en un item
INSERT INTO transaction_items (transaction_id, product_id, quantity, notes, created_at)
SELECT id, product_id, quantity, notes, created_at
FROM transactions_backup;

-- 4. Modificar tabla transactions
ALTER TABLE transactions 
    DROP FOREIGN KEY transactions_ibfk_1;  -- FK de product_id
    
ALTER TABLE transactions
    DROP COLUMN product_id,
    DROP COLUMN quantity,
    ADD COLUMN total_items INT DEFAULT 1 AFTER notes;

-- 5. Actualizar contador de items
UPDATE transactions t
SET total_items = (
    SELECT COUNT(*) 
    FROM transaction_items ti 
    WHERE ti.transaction_id = t.id
);

COMMIT;

-- Verificación
SELECT 
    COUNT(*) as total_transactions,
    (SELECT COUNT(*) FROM transaction_items) as total_items,
    (SELECT COUNT(*) FROM transactions_backup) as backup_count
FROM transactions;
```

---

## 💻 Cambios en Modelos (Java)

### 1. Modelo `Transaction.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/models/Transaction.java`

```java
package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.HasMany;

@Table("transactions")
@HasMany(child = TransactionItem.class, foreignKeyName = "transaction_id")
public class Transaction extends Model {

    public static final String TYPE_IN = "in";
    public static final String TYPE_OUT = "out";
    public static final String TYPE_LOAN = "loan";

    /**
     * Obtiene todos los items de esta transacción
     */
    public List<TransactionItem> getItems() {
        return this.getAll(TransactionItem.class);
    }

    /**
     * Agrega un producto a la transacción
     */
    public TransactionItem addItem(Integer productId, double quantity, String notes) {
        TransactionItem item = new TransactionItem();
        item.set("transaction_id", this.getId());
        item.set("product_id", productId);
        item.set("quantity", quantity);
        item.set("notes", notes);
        item.saveIt();
        
        // Actualizar contador
        this.set("total_items", this.getItems().size());
        this.saveIt();
        
        return item;
    }

    /**
     * Calcula la cantidad total de productos en la transacción
     */
    public double getTotalQuantity() {
        return this.getItems().stream()
            .mapToDouble(item -> item.getDouble("quantity"))
            .sum();
    }
}
```

### 2. Nuevo Modelo `TransactionItem.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/models/TransactionItem.java`

```java
package com.joshuamc1.inventariomunicipal.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

@Table("transaction_items")
@BelongsTo(parent = Transaction.class, foreignKeyName = "transaction_id")
@BelongsTo(parent = Product.class, foreignKeyName = "product_id")
public class TransactionItem extends Model {

    /**
     * Obtiene el producto asociado a este item
     */
    public Product getProduct() {
        return this.parent(Product.class);
    }

    /**
     * Obtiene la transacción asociada a este item
     */
    public Transaction getTransaction() {
        return this.parent(Transaction.class);
    }
}
```

---

## 🔧 Cambios en Repositorios

### 1. Nuevo `TransactionItemRepository.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/repositories/TransactionItemRepository.java`

```java
package com.joshuamc1.inventariomunicipal.repositories;

import com.joshuamc1.inventariomunicipal.models.TransactionItem;
import java.util.List;

public class TransactionItemRepository {

    public List<TransactionItem> findByTransactionId(Integer transactionId) {
        return TransactionItem.where("transaction_id = ?", transactionId);
    }

    public TransactionItem findById(Integer id) {
        return TransactionItem.findById(id);
    }

    public void create(TransactionItem item) {
        item.saveIt();
    }

    public void update(TransactionItem item) {
        item.saveIt();
    }

    public void delete(Integer id) {
        TransactionItem item = findById(id);
        if (item != null) {
            item.delete();
        }
    }

    public long countByTransactionId(Integer transactionId) {
        return TransactionItem.count("transaction_id = ?", transactionId);
    }
}
```

### 2. Modificar `TransactionRepository.java`

```java
// Agregar métodos para trabajar con items

public List<Transaction> findWithItems(int page, int pageSize, String searchTerm) {
    String sql = """
        SELECT DISTINCT t.* FROM transactions t
        LEFT JOIN transaction_items ti ON t.id = ti.transaction_id
        LEFT JOIN products p ON ti.product_id = p.id
        WHERE p.name LIKE ?
        ORDER BY t.created_at DESC
        LIMIT ? OFFSET ?
        """;
    
    int offset = (page - 1) * pageSize;
    return Transaction.findBySQL(sql, "%" + searchTerm + "%", pageSize, offset);
}
```

---

## 🎨 Cambios en Servicios

### 1. Modificar `TransactionService.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/services/TransactionService.java`

```java
public class TransactionService {
    
    private final TransactionRepository repo = new TransactionRepository();
    private final TransactionItemRepository itemRepo = new TransactionItemRepository();
    private final ProductRepository productRepo = new ProductRepository();
    private final AuditService auditService = new AuditService();

    /**
     * Crear transacción con múltiples productos
     * @param type Tipo de transacción (in, out, loan)
     * @param date Fecha de la transacción
     * @param userId Usuario que realiza la transacción
     * @param personId Persona relacionada (proveedor/beneficiario)
     * @param notes Notas generales de la transacción
     * @param items Lista de items: [{productId: 1, quantity: 10, notes: "..."}, ...]
     * @return Transaction creada con sus items
     */
    public Transaction createMultiProductTransaction(
            String type, 
            String date,
            Integer userId, 
            Integer personId, 
            String notes,
            List<TransactionItemDTO> items) {
        
        // Validar permisos
        boolean isRoot = Session.getCurrentUser() != null && Session.getCurrentUser().isRootRole();
        boolean allowed = switch (type) {
            case "in" -> PermissionHelper.canAccess("INGRESOS", "CREATE");
            case "out" -> PermissionHelper.canAccess("EGRESOS", "CREATE");
            case "loan" -> PermissionHelper.canAccess("PRESTAMOS", "CREATE");
            default -> false;
        };
        
        if (!isRoot && !allowed) {
            throw new IllegalStateException("Acceso denegado: no tiene permiso para crear " + type);
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La transacción debe contener al menos un producto");
        }

        return DatabaseManager.callInConnection(() -> {
            Base.openTransaction();

            // 1. Crear header de transacción
            Transaction transaction = new Transaction();
            transaction.set("type", type);
            transaction.set("date", date);
            transaction.set("user_id", userId);
            transaction.set("person_id", personId);
            transaction.set("notes", notes);
            transaction.set("total_items", items.size());
            repo.create(transaction);

            Integer transactionId = transaction.getInteger("id");

            // 2. Crear items y actualizar stock
            for (TransactionItemDTO itemDTO : items) {
                Product product = productRepo.findById(itemDTO.getProductId());
                
                if (product == null) {
                    throw new IllegalArgumentException("Producto no encontrado: " + itemDTO.getProductId());
                }

                // Actualizar stock según tipo
                if ("in".equals(type)) {
                    product.updateStock(itemDTO.getQuantity());
                } else {
                    product.updateStock(-itemDTO.getQuantity());
                }
                productRepo.save(product);

                // Crear item de transacción
                TransactionItem item = new TransactionItem();
                item.set("transaction_id", transactionId);
                item.set("product_id", itemDTO.getProductId());
                item.set("quantity", itemDTO.getQuantity());
                item.set("notes", itemDTO.getNotes());
                itemRepo.create(item);
            }

            // 3. Auditoría
            auditService.logAction(
                userId,
                "create_multi_transaction",
                "transactions",
                transactionId,
                String.format("type=%s, total_items=%d", type, items.size())
            );

            Base.commitTransaction();
            return transaction;
        });
    }

    // Mantener método original para compatibilidad
    public Transaction createTransaction(Integer productId, String type,
                                         double quantity, Integer userId,
                                         Integer personId, String notes) {
        // Internamente usar el nuevo método con un solo item
        List<TransactionItemDTO> items = List.of(
            new TransactionItemDTO(productId, quantity, notes)
        );
        
        return createMultiProductTransaction(
            type, 
            LocalDate.now().toString(),
            userId, 
            personId, 
            notes, 
            items
        );
    }
}
```

### 2. Nuevo DTO `TransactionItemDTO.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/dto/TransactionItemDTO.java`

```java
package com.joshuamc1.inventariomunicipal.dto;

public class TransactionItemDTO {
    private Integer productId;
    private double quantity;
    private String notes;

    public TransactionItemDTO(Integer productId, double quantity, String notes) {
        this.productId = productId;
        this.quantity = quantity;
        this.notes = notes;
    }

    // Getters y setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
```

---

## 🖥️ Cambios en Controladores

### 1. Modificar `IncomesController.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/controllers/dashboard/IncomesController.java`

**Cambios principales:**
- Cambiar diálogo de creación para agregar múltiples productos
- Mostrar tabla expandible con items de cada transacción
- Permitir agregar/quitar productos antes de guardar

```java
// Nuevo método para agregar múltiples productos
private void showMultiProductIncomeDialog() {
    Dialog<List<TransactionItemDTO>> dialog = new Dialog<>();
    dialog.setTitle("Nuevo Ingreso Multi-Producto");
    
    // Layout principal
    VBox mainLayout = new VBox(10);
    mainLayout.setPadding(new Insets(15));
    
    // Campos comunes
    DatePicker datePicker = new DatePicker(LocalDate.now());
    ComboBox<Person> personCombo = new ComboBox<>();
    TextArea notesArea = new TextArea();
    notesArea.setPromptText("Notas generales del ingreso");
    
    // Tabla de productos a agregar
    TableView<TransactionItemDTO> itemsTable = new TableView<>();
    ObservableList<TransactionItemDTO> items = FXCollections.observableArrayList();
    itemsTable.setItems(items);
    
    // Columnas de la tabla
    TableColumn<TransactionItemDTO, String> colProduct = new TableColumn<>("Producto");
    TableColumn<TransactionItemDTO, Double> colQuantity = new TableColumn<>("Cantidad");
    TableColumn<TransactionItemDTO, String> colNotes = new TableColumn<>("Notas");
    
    // Botones para agregar/quitar productos
    Button btnAddProduct = new Button("+ Agregar Producto");
    btnAddProduct.setOnAction(e -> {
        // Mostrar diálogo para seleccionar producto y cantidad
        Optional<TransactionItemDTO> result = showAddProductDialog();
        result.ifPresent(items::add);
    });
    
    Button btnRemoveProduct = new Button("- Quitar Seleccionado");
    btnRemoveProduct.setOnAction(e -> {
        TransactionItemDTO selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            items.remove(selected);
        }
    });
    
    // Ensamblar layout...
    // ... código completo de UI
    
    // Botón de guardar
    ButtonType btnSave = new ButtonType("Guardar Ingreso", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);
    
    dialog.setResultConverter(dialogButton -> {
        if (dialogButton == btnSave && !items.isEmpty()) {
            return items;
        }
        return null;
    });
    
    Optional<List<TransactionItemDTO>> result = dialog.showAndWait();
    result.ifPresent(itemList -> {
        try {
            Person selectedPerson = personCombo.getValue();
            transactionService.createMultiProductTransaction(
                "in",
                datePicker.getValue().toString(),
                Session.getCurrentUserId(),
                selectedPerson != null ? selectedPerson.getInteger("id") : null,
                notesArea.getText(),
                itemList
            );
            
            Utils.showSuccess("Éxito", "Ingreso registrado con " + itemList.size() + " productos");
            loadPageFromRepo(currentPage);
        } catch (Exception ex) {
            Utils.showError("Error", "No se pudo crear el ingreso: " + ex.getMessage());
        }
    });
}
```

### 2. Modificar tabla para mostrar items expandibles

```java
// Agregar columna expandible
TableColumn<Transaction, Void> colExpand = new TableColumn<>("Productos");
colExpand.setCellFactory(col -> new TableCell<>() {
    private final Button btnExpand = new Button("Ver Items");
    
    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            Transaction transaction = getTableView().getItems().get(getIndex());
            btnExpand.setOnAction(e -> showTransactionItems(transaction));
            setGraphic(btnExpand);
        }
    }
});

private void showTransactionItems(Transaction transaction) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Productos de la Transacción #" + transaction.getId());
    alert.setHeaderText(transaction.getItems().size() + " productos");
    
    TableView<TransactionItem> itemsTable = new TableView<>();
    // ... configurar columnas y cargar items
    
    alert.getDialogPane().setContent(itemsTable);
    alert.showAndWait();
}
```

**Aplicar cambios similares a:**
- `OutcomesController.java`
- `LoansController.java`

---

## 🎨 Cambios en Vistas (FXML)

### 1. Modificar `incomes.fxml`

**Ubicación:** `src/main/resources/fxml/dashboard/incomes.fxml`

**Cambios necesarios:**
- Agregar columna "Items" o "Productos" que muestre la cantidad de productos
- Botón "Ver Detalle" para expandir y ver todos los productos de la transacción
- Modificar diálogo de creación para soportar múltiples productos

```xml
<!-- Agregar columna de items -->
<TableColumn fx:id="colItems" text="# Items" prefWidth="80">
    <cellValueFactory>
        <PropertyValueFactory property="total_items" />
    </cellValueFactory>
</TableColumn>

<!-- Botón para ver detalle de items -->
<Button text="Ver Productos" onAction="#showItemsDialog" />
```

**Aplicar cambios similares a:**
- `outcomes.fxml`
- `loans.fxml`

---

## 📄 Cambios en Reportes

### 1. Modificar `ReportService.java`

**Ubicación:** `src/main/java/com/joshuamc1/inventariomunicipal/services/ReportService.java`

**Cambios principales:**
- Reportes de transacciones deben incluir desglose de productos
- Agregar opción para reportes consolidados vs detallados

```java
public void generateTransactionDetailReport(Integer transactionId, File outputFile) {
    Transaction transaction = transactionRepo.findById(transactionId);
    List<TransactionItem> items = transaction.getItems();
    
    // Generar PDF con tabla de items
    PDDocument document = new PDDocument();
    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);
    
    // Header de transacción
    // ... información general
    
    // Tabla de productos
    // Columnas: Producto | Cantidad | Unidad | Notas
    for (TransactionItem item : items) {
        Product product = item.getProduct();
        // ... renderizar fila
    }
    
    document.save(outputFile);
}

// Reporte consolidado de múltiples transacciones con items
public void generateConsolidatedReport(LocalDate from, LocalDate to, String type) {
    // ...
}
```

### 2. Actualizar exportación a Excel

```java
// Agregar hoja adicional con detalle de items
Workbook workbook = new XSSFWorkbook();
Sheet transactionsSheet = workbook.createSheet("Transacciones");
Sheet itemsSheet = workbook.createSheet("Detalle de Productos");

// ... llenar hojas
```

---

## 🔐 Cambios en Permisos

**No se requieren cambios en permisos**, ya que se mantienen los mismos módulos:
- `INGRESOS.CREATE` - Permite crear ingresos (mono o multi producto)
- `EGRESOS.CREATE` - Permite crear egresos (mono o multi producto)
- `PRESTAMOS.CREATE` - Permite crear préstamos (mono o multi producto)

---

## ✅ Checklist de Implementación

### Fase 1: Base de Datos (2-3 días)
- [ ] Crear tabla `transaction_items`
- [ ] Hacer backup completo de la base de datos actual
- [ ] Ejecutar script de migración de datos
- [ ] Verificar integridad de datos migrados
- [ ] Crear índices necesarios
- [ ] Probar rollback en ambiente de prueba

### Fase 2: Modelos y Repositorios (1-2 días)
- [ ] Crear modelo `TransactionItem.java`
- [ ] Modificar modelo `Transaction.java`
- [ ] Crear `TransactionItemRepository.java`
- [ ] Modificar `TransactionRepository.java`
- [ ] Crear `TransactionItemDTO.java`
- [ ] Ejecutar pruebas unitarias de modelos

### Fase 3: Servicios (2-3 días)
- [ ] Modificar `TransactionService.java`
- [ ] Implementar método `createMultiProductTransaction()`
- [ ] Mantener compatibilidad con método mono-producto
- [ ] Actualizar lógica de actualización de stock
- [ ] Implementar validaciones de negocio
- [ ] Agregar logging y auditoría

### Fase 4: Controladores (3-4 días)
- [ ] Modificar `IncomesController.java`
- [ ] Modificar `OutcomesController.java`
- [ ] Modificar `LoansController.java`
- [ ] Implementar diálogos multi-producto
- [ ] Implementar vista de detalle de items
- [ ] Agregar validaciones en UI

### Fase 5: Vistas FXML (2-3 días)
- [ ] Modificar `incomes.fxml`
- [ ] Modificar `outcomes.fxml`
- [ ] Modificar `loans.fxml`
- [ ] Crear nuevos componentes de UI necesarios
- [ ] Ajustar estilos CSS

### Fase 6: Reportes (2-3 días)
- [ ] Modificar `ReportService.java`
- [ ] Actualizar generación de PDFs
- [ ] Actualizar exportación a Excel
- [ ] Agregar reportes de detalle de items
- [ ] Modificar `ReportsController.java`

### Fase 7: Testing (5-7 días)
- [ ] Pruebas unitarias de servicios
- [ ] Pruebas de integración de base de datos
- [ ] Pruebas de UI - crear transacciones multi-producto
- [ ] Pruebas de reportes
- [ ] Pruebas de permisos
- [ ] Pruebas de auditoría
- [ ] Pruebas de rollback y manejo de errores
- [ ] Testing manual exhaustivo por módulo

### Fase 8: Documentación (1-2 días)
- [ ] Actualizar `DATABASE.md`
- [ ] Actualizar `ARCHITECTURE.md`
- [ ] Crear guía de usuario para nuevas funcionalidades
- [ ] Documentar cambios en API interna
- [ ] Actualizar diagramas de base de datos

---

## ⚠️ Riesgos y Consideraciones

### Riesgos Técnicos
1. **Migración de datos:** Alto riesgo si no se hace backup completo
2. **Integridad referencial:** Posible pérdida de relaciones si no se maneja correctamente
3. **Performance:** Consultas más complejas pueden afectar rendimiento
4. **Compatibilidad:** Código existente que asume mono-producto puede fallar

### Mitigación
- ✅ Hacer backup completo antes de cualquier cambio
- ✅ Probar migración en ambiente de desarrollo primero
- ✅ Mantener compatibilidad con método mono-producto
- ✅ Agregar índices para optimizar consultas
- ✅ Testing exhaustivo antes de producción

### Consideraciones de Negocio
- **Capacitación:** Personal debe aprender nuevo flujo multi-producto
- **Cambio de proceso:** Puede requerir ajustes en procedimientos operativos
- **Impacto en reportes:** Reportes históricos pueden verse diferentes

---

## 📈 Beneficios Esperados

### Operativos
- ✅ Reducción de tiempo para registrar múltiples productos
- ✅ Mejor organización de transacciones relacionadas
- ✅ Menos duplicación de datos (fecha, proveedor, notas)

### Técnicos
- ✅ Mejor normalización de base de datos
- ✅ Más flexible para futuros requerimientos
- ✅ Reportes más completos y detallados

### Para el Usuario
- ✅ Interfaz más intuitiva para compras/entregas grandes
- ✅ Mejor trazabilidad de movimientos relacionados
- ✅ Menos clicks para registrar múltiples productos

---

## 🔄 Estrategia de Rollback

En caso de problemas durante la implementación:

```sql
-- Rollback de base de datos
START TRANSACTION;

-- 1. Restaurar tabla transactions original
DROP TABLE IF EXISTS transactions;
RENAME TABLE transactions_backup TO transactions;

-- 2. Eliminar tabla de items
DROP TABLE IF EXISTS transaction_items;

-- 3. Restaurar foreign keys
ALTER TABLE transactions 
ADD CONSTRAINT transactions_ibfk_1 
FOREIGN KEY (product_id) REFERENCES products(id)
ON UPDATE CASCADE
ON DELETE RESTRICT;

COMMIT;
```

---

## 📞 Recursos y Referencias

### Archivos Principales a Modificar

**Base de Datos:**
- `migrations/2025MMDD_HHMMSS_migrate_to_multiproduct_transactions.sql`

**Modelos:**
- `src/main/java/com/joshuamc1/inventariomunicipal/models/Transaction.java`
- `src/main/java/com/joshuamc1/inventariomunicipal/models/TransactionItem.java` (NUEVO)

**Repositorios:**
- `src/main/java/com/joshuamc1/inventariomunicipal/repositories/TransactionRepository.java`
- `src/main/java/com/joshuamc1/inventariomunicipal/repositories/TransactionItemRepository.java` (NUEVO)

**Servicios:**
- `src/main/java/com/joshuamc1/inventariomunicipal/services/TransactionService.java`
- `src/main/java/com/joshuamc1/inventariomunicipal/services/ReportService.java`

**Controladores:**
- `src/main/java/com/joshuamc1/inventariomunicipal/controllers/dashboard/IncomesController.java`
- `src/main/java/com/joshuamc1/inventariomunicipal/controllers/dashboard/OutcomesController.java`
- `src/main/java/com/joshuamc1/inventariomunicipal/controllers/dashboard/LoansController.java`
- `src/main/java/com/joshuamc1/inventariomunicipal/controllers/dashboard/ReportsController.java`

**Vistas:**
- `src/main/resources/fxml/dashboard/incomes.fxml`
- `src/main/resources/fxml/dashboard/outcomes.fxml`
- `src/main/resources/fxml/dashboard/loans.fxml`

---

## 🎯 Conclusión

Esta mejora es **altamente recomendable** para sistemas de inventario con alto volumen de transacciones, pero **no es crítica** para el contexto municipal actual donde los movimientos son más esporádicos y controlados.

**Recomendación:** Implementar solo si:
- Se detecta necesidad real en operación diaria
- Se cuenta con tiempo suficiente para desarrollo y testing completo (3-4 semanas)
- Se puede hacer en una ventana de mantenimiento programado
- Se tiene backup completo y plan de rollback probado

**Para el delivery actual:** El sistema mono-producto es **suficiente y apropiado** para las necesidades de la Municipalidad de Olanchito.

---

**Autor:** Joshua David Mclean Escaleras  
**Fecha:** 21 de Noviembre de 2025  
**Versión del Sistema:** 1.0-SNAPSHOT  
**Estado del Plan:** Propuesta - No Implementado

