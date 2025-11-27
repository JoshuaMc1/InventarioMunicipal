# 🗑️ Plan de Implementación: Sistema de Soft Delete y Restauración

## 📋 Resumen Ejecutivo

**Objetivo**: Implementar un sistema de eliminación lógica (soft delete) que proteja la integridad de los datos y permita su restauración controlada mediante permisos específicos.

**Principio**: La información NO se borra físicamente de la base de datos a menos que el usuario tenga el permiso especial `DESTRUCTIVE_OPERATIONS`. Los demás usuarios solo pueden cambiar el estado de los registros.

> **⚠️ IMPORTANTE**: Este plan incluye dos fases críticas que deben implementarse AL FINAL:
> - **FASE 8**: Control de acceso en Login - Bloquear usuarios inactivos/eliminados
> - **FASE 9**: Resolución de conflictos en reportes, búsquedas y relaciones entre entidades
> 
> Estas fases garantizan que el sistema funcione correctamente y eviten inconsistencias en datos.

---

## 🎯 Alcance del Sistema

### Entidades Afectadas

1. **Productos** (`products`)
2. **Transacciones** (`transactions`) 
3. **Items de Transacciones** (`transaction_items`)
4. **Usuarios** (`users`)
5. **Roles** (`roles`)
6. **Personas** (`people`)

### Conceptos Clave

- **Eliminación Lógica**: Cambiar el estado de un registro a `deleted` o `inactive`
- **Eliminación Física**: Borrar permanentemente el registro de la base de datos (solo ROOT)
- **Restauración**: Revertir el estado `deleted` a `active` (requiere permiso especial)
- **Visibilidad**: Los datos "eliminados" no se muestran en listas normales (requiere permiso para verlos)

---

## 🔐 Nuevos Permisos a Implementar

### 1. Permiso de Operaciones Destructivas
```
Módulo: SISTEMA
Acción: DESTRUCTIVE_OPERATIONS
Descripción: Permite ejecutar operaciones de eliminación física permanente
Rol asignado: ROOT (únicamente)
```

### 2. Permisos de Restauración por Módulo
```
Módulo: INVENTARIO
Acción: RESTORE
Descripción: Permite restaurar productos eliminados lógicamente

Módulo: INGRESOS
Acción: RESTORE
Descripción: Permite restaurar ingresos invalidados

Módulo: EGRESOS
Acción: RESTORE
Descripción: Permite restaurar egresos invalidados

Módulo: PRESTAMOS
Acción: RESTORE
Descripción: Permite restaurar préstamos invalidados

Módulo: USUARIOS
Acción: RESTORE
Descripción: Permite restaurar usuarios deshabilitados

Módulo: ROLES
Acción: RESTORE
Descripción: Permite restaurar roles deshabilitados

Módulo: PERSONAS
Acción: RESTORE
Descripción: Permite restaurar personas deshabilitadas
```

### 3. Permiso para Ver Datos Eliminados
```
Módulo: SISTEMA
Acción: VIEW_DELETED
Descripción: Permite visualizar registros eliminados lógicamente
Rol asignado: ROOT, ADMIN, AUDITOR
```

---

## 📊 Cambios en Base de Datos

### FASE 1: Modificar Migraciones Existentes

#### 1.1 Modificar `20251114112627_create_roles_table.sql`
```sql
-- ANTES:
activo TINYINT(1) DEFAULT 1

-- DESPUÉS: Agregar campo status
status ENUM('active', 'inactive', 'deleted') DEFAULT 'active',
activo TINYINT(1) DEFAULT 1  -- Mantener por compatibilidad temporal
```

#### 1.2 Modificar `20251114112628_create_user_table.sql`
```sql
-- AGREGAR:
status ENUM('active', 'inactive', 'deleted') DEFAULT 'active',
deleted_at TIMESTAMP NULL DEFAULT NULL,
deleted_by INT NULL DEFAULT NULL,

-- AGREGAR índice:
INDEX idx_users_status (status),
FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL
```

#### 1.3 Modificar `20251114112848_create_people_table.sql`
```sql
-- AGREGAR:
status ENUM('active', 'inactive', 'deleted') DEFAULT 'active',
deleted_at TIMESTAMP NULL DEFAULT NULL,
deleted_by INT NULL DEFAULT NULL,

-- AGREGAR índice:
INDEX idx_people_status (status),
FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL
```

#### 1.4 Modificar `20251114112911_create_product_table.sql`
```sql
-- AGREGAR:
status ENUM('active', 'inactive', 'deleted') DEFAULT 'active',
deleted_at TIMESTAMP NULL DEFAULT NULL,
deleted_by INT NULL DEFAULT NULL,

-- AGREGAR índice:
INDEX idx_products_status (status),
FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL
```

#### 1.5 Modificar `20251124120000_migrate_to_multiproduct_transactions.sql`
```sql
-- EN LA TABLA transactions AGREGAR:
status ENUM('active', 'cancelled', 'invalidated') DEFAULT 'active',
invalidated_at TIMESTAMP NULL DEFAULT NULL,
invalidated_by INT NULL DEFAULT NULL,
invalidation_reason TEXT NULL,

-- AGREGAR índice:
INDEX idx_transactions_status (status),
FOREIGN KEY (invalidated_by) REFERENCES users(id) ON DELETE SET NULL

-- EN LA TABLA transaction_items AGREGAR:
status ENUM('active', 'cancelled') DEFAULT 'active',
```

#### 1.6 Modificar `20251114114107_seed_roles_and_permissions.sql`
```sql
-- AGREGAR NUEVOS PERMISOS AL FINAL:

-- Módulo: Sistema (Operaciones Destructivas y Visibilidad)
('SISTEMA', 'DESTRUCTIVE_OPERATIONS', 'Permite ejecutar eliminaciones físicas permanentes'),
('SISTEMA', 'VIEW_DELETED', 'Permite visualizar registros eliminados lógicamente'),

-- Permisos de RESTORE por módulo
('INVENTARIO', 'RESTORE', 'Restaurar productos eliminados'),
('INGRESOS', 'RESTORE', 'Restaurar ingresos invalidados'),
('EGRESOS', 'RESTORE', 'Restaurar egresos invalidados'),
('PRESTAMOS', 'RESTORE', 'Restaurar préstamos invalidados'),
('USUARIOS', 'RESTORE', 'Restaurar usuarios deshabilitados'),
('ROLES', 'RESTORE', 'Restaurar roles deshabilitados'),
('PERSONAS', 'RESTORE', 'Restaurar personas deshabilitadas');

-- ASIGNAR PERMISOS SOLO A ROOT
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT 1, id FROM permisos
WHERE (modulo = 'SISTEMA' AND accion IN ('DESTRUCTIVE_OPERATIONS', 'VIEW_DELETED'))
   OR accion = 'RESTORE';
```

---

## 💻 Cambios en Código Java

### FASE 2: Actualizar Modelos

#### 2.1 Agregar Métodos en Modelos Base
Crear métodos comunes para soft delete en cada modelo:

**Product.java**
```java
public void softDelete(Integer deletedBy) {
    this.set("status", "deleted");
    this.set("deleted_at", new Timestamp(System.currentTimeMillis()));
    this.set("deleted_by", deletedBy);
    this.saveIt();
}

public void restore() {
    this.set("status", "active");
    this.set("deleted_at", null);
    this.set("deleted_by", null);
    this.saveIt();
}

public boolean isDeleted() {
    return "deleted".equals(this.getString("status"));
}
```

**Transaction.java**
```java
public void invalidate(Integer invalidatedBy, String reason) {
    this.set("status", "invalidated");
    this.set("invalidated_at", new Timestamp(System.currentTimeMillis()));
    this.set("invalidated_by", invalidatedBy);
    this.set("invalidation_reason", reason);
    this.saveIt();
}

public void restore() {
    this.set("status", "active");
    this.set("invalidated_at", null);
    this.set("invalidated_by", null);
    this.set("invalidation_reason", null);
    this.saveIt();
}

public boolean isInvalidated() {
    return "invalidated".equals(this.getString("status"));
}
```

**User.java**, **Person.java**, **Rol.java** - Similar a Product

---

### FASE 3: Actualizar Repositorios

#### 3.1 ProductRepository.java
```java
// MODIFICAR método findAll para excluir eliminados por defecto
public LazyList<Product> findAll() {
    return Product.where("status != 'deleted'");
}

// NUEVO método para ver todos (incluidos eliminados)
public LazyList<Product> findAllIncludingDeleted() {
    return Product.findAll();
}

// NUEVO método para encontrar solo eliminados
public LazyList<Product> findDeleted() {
    return Product.where("status = 'deleted'");
}

// MODIFICAR método delete para soft delete
public void softDelete(Product product, Integer deletedBy) {
    product.softDelete(deletedBy);
}

// NUEVO método para eliminación física (solo ROOT)
public void hardDelete(Product product) {
    product.delete(); // Eliminación física real
}

// NUEVO método para restaurar
public void restore(Product product) {
    product.restore();
}
```

Aplicar cambios similares a:
- TransactionRepository.java
- UserRepository.java
- PersonRepository.java
- RolRepository.java

---

### FASE 4: Actualizar Servicios

#### 4.1 ProductService.java
```java
// MODIFICAR método delete actual
public void delete(@NotNull Product product) {
    Integer id = product.getInteger("id");
    String name = product.getString("name");
    Integer currentUserId = Session.getCurrentUserId();
    
    DatabaseManager.runInConnection(() -> {
        // Verificar si tiene permiso para eliminación física
        boolean canHardDelete = PermissionHelper.canAccess("SISTEMA", "DESTRUCTIVE_OPERATIONS");
        
        if (canHardDelete && Session.getCurrentUser().isRootRole()) {
            // Eliminación física (solo ROOT con permiso explícito)
            repo.hardDelete(product);
            auditService.logAction(
                currentUserId,
                "hard_delete",
                "products",
                id,
                String.format("ELIMINACIÓN FÍSICA: id=%s,name=%s", id, name)
            );
        } else {
            // Eliminación lógica (usuarios normales)
            repo.softDelete(product, currentUserId);
            auditService.logAction(
                currentUserId,
                "soft_delete",
                "products",
                id,
                String.format("ELIMINACIÓN LÓGICA: id=%s,name=%s", id, name)
            );
        }
    });
}

// NUEVO método para restaurar
public void restore(@NotNull Product product) {
    // Validar permiso INVENTARIO.RESTORE
    if (!PermissionHelper.canAccess("INVENTARIO", "RESTORE")) {
        throw new SecurityException("No tiene permiso para restaurar productos");
    }
    
    Integer id = product.getInteger("id");
    String name = product.getString("name");
    
    DatabaseManager.runInConnection(() -> {
        repo.restore(product);
        auditService.logAction(
            Session.getCurrentUserId(),
            "restore",
            "products",
            id,
            String.format("RESTAURACIÓN: id=%s,name=%s", id, name)
        );
    });
}

// NUEVO método para listar eliminados
public List<Product> listDeleted() {
    // Validar permiso SISTEMA.VIEW_DELETED
    if (!PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
        throw new SecurityException("No tiene permiso para ver productos eliminados");
    }
    
    return DatabaseManager.callInConnection(() -> repo.findDeleted());
}
```

Aplicar cambios similares a:
- TransactionService.java (invalidate en vez de delete)
- UserService.java
- PersonService.java
- RolService.java

---

### FASE 5: Actualizar Controladores

#### 5.1 InventoryController.java
```java
// MODIFICAR método deleteSelected
private void deleteSelected() {
    // Ya no valida DELETE, sino que verifica qué tipo de operación puede hacer
    Product sel = tblProducts.getSelectionModel().getSelectedItem();
    
    if (sel == null) {
        Utils.showAlert("Info", "Seleccione un producto para eliminar");
        return;
    }
    
    boolean canHardDelete = PermissionHelper.canAccess("SISTEMA", "DESTRUCTIVE_OPERATIONS")
                         && Session.getCurrentUser().isRootRole();
    
    boolean canSoftDelete = PermissionHelper.canAccess("INVENTARIO", "DELETE");
    
    if (!canHardDelete && !canSoftDelete) {
        Utils.showAlert("Error", "No tiene permisos para eliminar productos");
        return;
    }
    
    String message = canHardDelete 
        ? "¿ELIMINAR PERMANENTEMENTE el producto: " + sel.getString("name") + "?\nEsta acción NO se puede deshacer."
        : "¿Deshabilitar el producto: " + sel.getString("name") + "?\nPodrá restaurarlo después.";
    
    boolean ok = Utils.confirmAction("Confirmar Eliminación", message);
    if (!ok) return;
    
    productService.delete(sel); // El servicio decidirá el tipo de eliminación
    loadProducts();
}

// NUEVO método para restaurar
@FXML
public void onRestore() {
    if (!PermissionHelper.validateAndShow("INVENTARIO", "RESTORE")) {
        return;
    }
    
    // Mostrar diálogo con productos eliminados
    List<Product> deletedProducts = productService.listDeleted();
    
    // ... crear diálogo de selección ...
    
    Product selected = showRestoreDialog(deletedProducts);
    if (selected != null) {
        productService.restore(selected);
        loadProducts();
        Utils.showAlert("Éxito", "Producto restaurado correctamente");
    }
}
```

#### 5.2 TransactionsController.java (Ingresos, Egresos, Préstamos)
```java
// MODIFICAR eliminación a invalidación
private void invalidateTransaction(Transaction transaction) {
    boolean canInvalidate = PermissionHelper.canAccess(getModuleName(), "DELETE");
    
    if (!canInvalidate) {
        Utils.showAlert("Error", "No tiene permisos para invalidar transacciones");
        return;
    }
    
    // Solicitar razón de invalidación
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Invalidar Transacción");
    dialog.setHeaderText("Proporcione una razón para invalidar esta transacción");
    dialog.setContentText("Razón:");
    
    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().trim().isEmpty()) {
        transactionService.invalidate(transaction, result.get());
        loadTransactions();
        Utils.showAlert("Éxito", "Transacción invalidada correctamente");
    }
}

// NUEVO método para restaurar transacciones
@FXML
public void onRestoreTransaction() {
    if (!PermissionHelper.validateAndShow(getModuleName(), "RESTORE")) {
        return;
    }
    
    // Mostrar diálogo con transacciones invalidadas
    List<Transaction> invalidated = transactionService.listInvalidated();
    
    Transaction selected = showRestoreDialog(invalidated);
    if (selected != null) {
        // ADVERTENCIA: Restaurar transacción afectará el stock
        boolean confirm = Utils.confirmAction(
            "Confirmar Restauración",
            "Al restaurar esta transacción se revertirán los cambios de stock.\n¿Continuar?"
        );
        
        if (confirm) {
            transactionService.restore(selected);
            loadTransactions();
            Utils.showAlert("Éxito", "Transacción restaurada correctamente");
        }
    }
}
```

Aplicar cambios similares a:
- UsersController.java
- PeopleController.java
- RolesController.java

---

### FASE 6: Actualizar Vistas FXML

#### 6.1 Agregar Botones de Restauración
En cada vista principal (dashboard/inventory.fxml, etc.):

```xml
<!-- Botón Restaurar (visible solo con permiso) -->
<Button fx:id="btnRestore" text="Restaurar" styleClass="btn-restore" 
        onAction="#onRestore" visible="false" managed="false">
    <graphic>
        <ImageView>
            <Image url="@/images/restore-icon.svg"/>
        </ImageView>
    </graphic>
</Button>
```

#### 6.2 Inicialización de Visibilidad en Controladores
```java
@FXML
public void initialize() {
    // ... código existente ...
    
    // Mostrar botón de restauración solo si tiene el permiso
    boolean canRestore = PermissionHelper.canAccess("INVENTARIO", "RESTORE");
    btnRestore.setVisible(canRestore);
    btnRestore.setManaged(canRestore);
    
    // Cambiar texto del botón eliminar según permisos
    if (PermissionHelper.canAccess("SISTEMA", "DESTRUCTIVE_OPERATIONS")) {
        btnDelete.setText("Eliminar Permanentemente");
        btnDelete.setStyle("-fx-background-color: #d32f2f;"); // Rojo más intenso
    } else {
        btnDelete.setText("Deshabilitar");
    }
}
```

---

### FASE 7: Actualizar Sistema de Auditoría

#### 7.1 Nuevas Acciones de Auditoría
```java
// En AuditService.java agregar constantes:
public static final String ACTION_SOFT_DELETE = "soft_delete";
public static final String ACTION_HARD_DELETE = "hard_delete";
public static final String ACTION_RESTORE = "restore";
public static final String ACTION_INVALIDATE = "invalidate";
```

#### 7.2 Registrar Detalles Adicionales
```java
// Ejemplo en auditoría de transacciones
auditService.logAction(
    userId,
    "invalidate",
    "transactions",
    transactionId,
    String.format(
        "transaction_id=%d, type=%s, reason=%s, invalidated_by=%d",
        transactionId, type, reason, userId
    )
);
```

---

### FASE 8: Actualizar Control de Acceso - Login

#### 8.1 Modificar LoginController.java o AuthService.java
```java
// En el método de autenticación, agregar validación de estado
public User authenticate(String username, String password) {
    return DatabaseManager.callInConnection(() -> {
        User user = User.findFirst("username = ?", username);
        
        if (user == null) {
            auditService.logAction(null, "login_failed", "users", null, 
                "username=" + username + ", reason=user_not_found");
            throw new AuthenticationException("Usuario o contraseña incorrectos");
        }
        
        // NUEVA VALIDACIÓN: Verificar estado del usuario
        String status = user.getString("status");
        if (!"active".equals(status)) {
            String reason = "deleted".equals(status) 
                ? "Usuario eliminado del sistema" 
                : "Usuario inactivo";
            
            auditService.logAction(user.getInteger("id"), "login_blocked", "users", 
                user.getInteger("id"), "reason=" + reason + ", status=" + status);
            
            throw new AuthenticationException(
                "Acceso denegado: " + reason + ". Contacte al administrador."
            );
        }
        
        // Verificar contraseña
        if (!PasswordHelper.verify(password, user.getString("password"))) {
            auditService.logAction(user.getInteger("id"), "login_failed", "users", 
                user.getInteger("id"), "reason=invalid_password");
            throw new AuthenticationException("Usuario o contraseña incorrectos");
        }
        
        // Login exitoso
        auditService.logAction(user.getInteger("id"), "login", "users", 
            user.getInteger("id"), "successful_login");
        
        return user;
    });
}
```

#### 8.2 Mensaje Amigable en UI
```java
// En LoginController, capturar la excepción y mostrar mensaje apropiado
@FXML
private void handleLogin() {
    String username = txtUsername.getText().trim();
    String password = txtPassword.getText();
    
    try {
        User user = authService.authenticate(username, password);
        Session.setCurrentUser(user);
        navigateToDashboard();
        
    } catch (AuthenticationException e) {
        // Mostrar mensaje específico al usuario
        if (e.getMessage().contains("inactivo") || e.getMessage().contains("eliminado")) {
            Utils.showAlert("Acceso Denegado", 
                e.getMessage() + "\n\nSu cuenta ha sido deshabilitada. " +
                "Por favor contacte al administrador del sistema.");
        } else {
            Utils.showAlert("Error de Autenticación", 
                "Usuario o contraseña incorrectos");
        }
        
        // Limpiar campos
        txtPassword.clear();
        txtUsername.requestFocus();
    }
}
```

#### 8.3 Validación en Sesión Activa
```java
// En Session.java o middleware, validar periódicamente el estado
public static void validateCurrentUserStatus() {
    User currentUser = getCurrentUser();
    if (currentUser == null) return;
    
    // Recargar usuario desde BD para obtener estado actual
    User freshUser = DatabaseManager.callInConnection(() -> 
        User.findById(currentUser.getId())
    );
    
    if (freshUser == null || !"active".equals(freshUser.getString("status"))) {
        // Usuario fue deshabilitado mientras estaba logueado
        Logs.warn(Session.class, "Usuario " + currentUser.getString("username") + 
            " deshabilitado durante sesión activa");
        
        Platform.runLater(() -> {
            Utils.showAlert("Sesión Terminada", 
                "Su cuenta ha sido deshabilitada. La sesión se cerrará.");
            logout();
            navigateToLogin();
        });
    }
}
```

---

### FASE 9: Áreas con Posibles Conflictos (Revisión Final)

Esta fase identifica áreas donde el soft delete puede generar conflictos o comportamientos inesperados que deben ser revisados y ajustados.

#### 9.1 Sistema de Reportes

**Problema**: Los reportes pueden mostrar datos inconsistentes si incluyen registros eliminados lógicamente.

**Solución**:
```java
// En ReportService.java
public List<Product> getProductsForReport(boolean includeDeleted) {
    return DatabaseManager.callInConnection(() -> {
        if (includeDeleted && PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
            return productRepo.findAllIncludingDeleted();
        } else {
            return productRepo.findAll(); // Solo activos
        }
    });
}

// Reporte de Transacciones
public List<Transaction> getTransactionsForReport(
        LocalDate startDate, 
        LocalDate endDate,
        boolean includeInvalidated) {
    
    return DatabaseManager.callInConnection(() -> {
        String query = "date BETWEEN ? AND ?";
        
        // Por defecto excluir invalidadas
        if (!includeInvalidated) {
            query += " AND status = 'active'";
        } else {
            // Verificar permiso para ver invalidadas
            if (!PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
                query += " AND status = 'active'";
            }
        }
        
        return Transaction.where(query, startDate, endDate)
            .orderBy("date DESC");
    });
}

// Reportes de Stock: CRÍTICO
public StockReport generateStockReport() {
    // El stock actual debe considerar SOLO transacciones activas
    // Las transacciones invalidadas ya revirtieron su efecto en stock
    return DatabaseManager.callInConnection(() -> {
        List<Product> products = productRepo.findAll(); // Solo productos activos
        
        for (Product product : products) {
            // El current_stock ya es correcto porque las transacciones 
            // invalidadas revirtieron su impacto
            // Solo mostrar productos activos
        }
        
        return new StockReport(products);
    });
}
```

**Consideraciones UI**:
```java
// En ReportController.java, agregar CheckBox opcional
@FXML
private CheckBox chkIncludeDeleted;

@FXML
public void initialize() {
    // Solo mostrar opción si tiene permiso
    boolean canViewDeleted = PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED");
    chkIncludeDeleted.setVisible(canViewDeleted);
    chkIncludeDeleted.setManaged(canViewDeleted);
    
    if (canViewDeleted) {
        chkIncludeDeleted.setText("Incluir registros eliminados/invalidados");
        chkIncludeDeleted.setSelected(false);
    }
}
```

#### 9.2 Búsquedas y Filtros Globales

**Problema**: Las búsquedas pueden devolver registros eliminados si no se filtran correctamente.

**Solución**:
```java
// En todos los métodos de búsqueda, agregar filtro de status
public List<Product> search(String query) {
    return DatabaseManager.callInConnection(() -> {
        // Excluir eliminados por defecto
        return Product.where(
            "(name LIKE ? OR code LIKE ?) AND status != 'deleted'", 
            "%" + query + "%", 
            "%" + query + "%"
        );
    });
}

// Búsqueda avanzada con opción
public List<Product> searchAdvanced(String query, boolean includeDeleted) {
    return DatabaseManager.callInConnection(() -> {
        String condition = "(name LIKE ? OR code LIKE ?)";
        
        if (!includeDeleted) {
            condition += " AND status != 'deleted'";
        } else if (!PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
            // Forzar exclusión si no tiene permiso
            condition += " AND status != 'deleted'";
        }
        
        return Product.where(condition, "%" + query + "%", "%" + query + "%");
    });
}
```

#### 9.3 Relaciones y Claves Foráneas

**Problema**: Al eliminar (soft delete) un registro padre, los registros hijos pueden quedar inconsistentes.

**Soluciones por Caso**:

```java
// CASO 1: Producto con Transacciones
// Al intentar eliminar un producto que tiene transacciones
public void delete(@NotNull Product product) {
    Integer id = product.getInteger("id");
    
    DatabaseManager.runInConnection(() -> {
        // Verificar si tiene transacciones activas
        long activeTransactions = TransactionItem.count(
            "product_id = ? AND status = 'active'", id
        );
        
        if (activeTransactions > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el producto porque tiene " + 
                activeTransactions + " transacciones activas. " +
                "Invalide las transacciones primero o use eliminación lógica."
            );
        }
        
        // Proceder con soft delete o hard delete según permiso
        // ...
    });
}

// CASO 2: Usuario que tiene Auditorías
// No se puede eliminar físicamente un usuario con auditorías
public boolean deleteUser(Integer userId) {
    return DatabaseManager.callInConnection(() -> {
        boolean canHardDelete = PermissionHelper.canAccess("SISTEMA", "DESTRUCTIVE_OPERATIONS")
                             && Session.getCurrentUser().isRootRole();
        
        if (canHardDelete) {
            // Verificar si tiene auditorías (nunca eliminar si tiene auditorías)
            long auditCount = Audit.count("user_id = ?", userId);
            if (auditCount > 0) {
                throw new IllegalStateException(
                    "No se puede eliminar físicamente un usuario con registros de auditoría. " +
                    "Use eliminación lógica."
                );
            }
        }
        
        // Soft delete
        User user = userRepo.findById(userId);
        userRepo.softDelete(user, Session.getCurrentUserId());
        return true;
    });
}

// CASO 3: Rol asignado a Usuarios
// No eliminar rol si tiene usuarios activos
public void deleteRol(Rol rol) {
    Integer rolId = rol.getInteger("id");
    
    DatabaseManager.runInConnection(() -> {
        // Contar usuarios activos con este rol
        long activeUsers = User.count("rol_id = ? AND status = 'active'", rolId);
        
        if (activeUsers > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el rol porque tiene " + activeUsers + 
                " usuarios activos asignados. Reasigne los usuarios primero."
            );
        }
        
        // Proceder con eliminación
        // ...
    });
}
```

#### 9.4 Estadísticas y Contadores en Dashboard

**Problema**: Las estadísticas pueden estar incorrectas si cuentan registros eliminados.

**Solución**:
```java
// En DashboardService.java o métodos de conteo
public long countActiveProducts() {
    return DatabaseManager.callInConnection(() -> 
        Product.count("status = 'active'")
    );
}

public long countActiveUsers() {
    return DatabaseManager.callInConnection(() -> 
        User.count("status = 'active'")
    );
}

public long countActiveTransactions() {
    return DatabaseManager.callInConnection(() -> 
        Transaction.count("status = 'active'")
    );
}

// Dashboard debe mostrar solo datos activos por defecto
public DashboardStats getStats() {
    return new DashboardStats(
        countActiveProducts(),
        countActiveUsers(),
        countActiveTransactions(),
        calculateTotalStockValue() // Solo productos activos
    );
}
```

#### 9.5 Exportación de Datos

**Problema**: Al exportar datos (CSV, Excel, PDF), debe quedar claro qué se incluye.

**Solución**:
```java
// En ExportService.java
public void exportProducts(String format, boolean includeDeleted) {
    // Validar permiso si quiere incluir eliminados
    if (includeDeleted && !PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
        throw new SecurityException("No tiene permiso para exportar datos eliminados");
    }
    
    List<Product> products = includeDeleted 
        ? productRepo.findAllIncludingDeleted()
        : productRepo.findAll();
    
    // Agregar encabezado en el archivo indicando el filtro
    String header = includeDeleted 
        ? "Reporte de Productos (INCLUYE ELIMINADOS) - " + LocalDate.now()
        : "Reporte de Productos (SOLO ACTIVOS) - " + LocalDate.now();
    
    // Exportar...
}
```

#### 9.6 ComboBox y Selectores

**Problema**: Los ComboBox pueden mostrar opciones eliminadas.

**Solución**:
```java
// Al cargar productos para un ComboBox
private void loadProductComboBox() {
    List<Product> products = productService.listActive(); // Solo activos
    
    ObservableList<Product> productList = FXCollections.observableArrayList(products);
    cmbProduct.setItems(productList);
    
    // Configurar el display
    cmbProduct.setConverter(new StringConverter<Product>() {
        @Override
        public String toString(Product product) {
            return product == null ? "" : product.getString("name");
        }
        
        @Override
        public Product fromString(String string) {
            return null;
        }
    });
}

// Similar para usuarios, personas, etc.
```

#### 9.7 Validaciones de Negocio

**Problema**: Validaciones pueden fallar si intentan acceder a registros eliminados.

**Solución**:
```java
// Al crear una transacción, validar que el producto esté activo
public Transaction createTransaction(...) {
    return DatabaseManager.callInConnection(() -> {
        Product product = productRepo.findById(productId);
        
        if (product == null) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        
        // NUEVA VALIDACIÓN
        if (!"active".equals(product.getString("status"))) {
            throw new IllegalStateException(
                "No se pueden crear transacciones con productos inactivos o eliminados. " +
                "Restaure el producto primero."
            );
        }
        
        // Continuar...
    });
}

// Similar para persona, usuario, etc.
```

---

### FASE 10: Sistema de Devolución de Préstamos

Esta fase implementa el ciclo completo de vida de los préstamos, permitiendo registrar devoluciones y gestionar diferentes tipos de finalización del préstamo.

#### 10.1 Contexto y Objetivos

**Problema Actual**: 
- Los préstamos se registran pero no hay forma de cerrar el ciclo
- No se sabe si un préstamo fue devuelto, pagado o está pendiente
- No hay relación entre el préstamo original y su devolución

**Solución Propuesta**:
- Agregar estado de préstamo en la tabla `transactions`
- Permitir asociar un ingreso con un préstamo original (devolución)
- Tipos de devolución: Devuelto (mismo producto), Pagado (compra), Reposición
- Cambiar estado del préstamo automáticamente al registrar devolución

#### 10.2 Cambios en Base de Datos

**Modificar migración `20251124120000_migrate_to_multiproduct_transactions.sql`**

```sql
-- EN LA TABLA transactions AGREGAR (después de los campos existentes):

-- Campos para gestión de préstamos
loan_status ENUM('pending', 'returned', 'paid', 'replaced', 'overdue') DEFAULT 'pending',
expected_return_date DATE NULL,
actual_return_date DATE NULL,
related_transaction_id INT NULL COMMENT 'ID de transacción relacionada (para devoluciones)',
return_type ENUM('same_product', 'purchased', 'replaced', 'other') NULL,
return_notes TEXT NULL,

-- Índices para optimizar consultas
INDEX idx_loan_status (loan_status),
INDEX idx_related_transaction (related_transaction_id),
FOREIGN KEY (related_transaction_id) REFERENCES transactions(id) 
    ON UPDATE CASCADE 
    ON DELETE SET NULL;

-- COMENTARIO: 
-- loan_status: Estado del préstamo
-- expected_return_date: Fecha esperada de devolución
-- actual_return_date: Fecha real de devolución
-- related_transaction_id: Si es un ingreso, referencia al préstamo que devuelve
-- return_type: Tipo de devolución (mismo producto, comprado, reemplazado)
-- return_notes: Notas sobre la devolución
```

#### 10.3 Actualizar Modelo Transaction.java

```java
// Agregar métodos para gestión de préstamos

/**
 * Verifica si esta transacción es un préstamo
 */
public boolean isLoan() {
    return "loan".equals(this.getString("type"));
}

/**
 * Verifica si el préstamo está pendiente
 */
public boolean isLoanPending() {
    return isLoan() && "pending".equals(this.getString("loan_status"));
}

/**
 * Verifica si el préstamo está vencido
 */
public boolean isLoanOverdue() {
    if (!isLoan() || !"pending".equals(this.getString("loan_status"))) {
        return false;
    }
    
    Date expectedReturn = this.getDate("expected_return_date");
    if (expectedReturn == null) return false;
    
    return expectedReturn.before(new Date());
}

/**
 * Marca el préstamo como devuelto
 */
public void markAsReturned(Integer returnTransactionId, String returnType, 
                          Date returnDate, String notes) {
    this.set("loan_status", getStatusForReturnType(returnType));
    this.set("actual_return_date", returnDate);
    this.set("related_transaction_id", returnTransactionId);
    this.set("return_type", returnType);
    this.set("return_notes", notes);
    this.saveIt();
}

/**
 * Obtiene el estado según el tipo de devolución
 */
private String getStatusForReturnType(String returnType) {
    return switch (returnType) {
        case "same_product" -> "returned";
        case "purchased" -> "paid";
        case "replaced" -> "replaced";
        default -> "returned";
    };
}

/**
 * Obtiene la transacción de préstamo relacionada (si existe)
 */
public Transaction getRelatedLoan() {
    Integer relatedId = this.getInteger("related_transaction_id");
    if (relatedId == null) return null;
    
    return Transaction.findById(relatedId);
}

/**
 * Verifica si este ingreso es una devolución de préstamo
 */
public boolean isLoanReturn() {
    return "in".equals(this.getString("type")) 
        && this.getInteger("related_transaction_id") != null;
}
```

#### 10.4 Actualizar TransactionService.java

```java
/**
 * Crea una transacción de préstamo con fecha de devolución esperada
 */
public Transaction createLoan(List<TransactionItemDTO> items, 
                              Integer personId, 
                              LocalDate expectedReturnDate,
                              String notes) {
    
    // Validar permiso
    if (!PermissionHelper.canAccess("PRESTAMOS", "CREATE")) {
        throw new SecurityException("No tiene permiso para crear préstamos");
    }
    
    return DatabaseManager.callInConnection(() -> {
        Base.openTransaction();
        try {
            // Crear transacción de préstamo
            Transaction loan = new Transaction();
            loan.set("type", "loan");
            loan.set("date", LocalDate.now());
            loan.set("user_id", Session.getCurrentUserId());
            loan.set("person_id", personId);
            loan.set("notes", notes);
            loan.set("loan_status", "pending");
            loan.set("expected_return_date", expectedReturnDate);
            loan.set("status", "active");
            loan.saveIt();
            
            // Procesar items (similar a createMultiProductTransaction)
            for (TransactionItemDTO itemDTO : items) {
                Product product = productRepo.findById(itemDTO.getProductId());
                if (product == null) {
                    throw new IllegalArgumentException("Producto no encontrado");
                }
                
                // Validar producto activo
                if (!"active".equals(product.getString("status"))) {
                    throw new IllegalStateException(
                        "No se pueden prestar productos inactivos o eliminados"
                    );
                }
                
                // Restar del stock
                product.updateStock(-itemDTO.getQuantity());
                productRepo.save(product);
                
                // Crear item
                TransactionItem item = new TransactionItem();
                item.set("transaction_id", loan.getId());
                item.set("product_id", itemDTO.getProductId());
                item.set("quantity", itemDTO.getQuantity());
                item.set("notes", itemDTO.getNotes());
                item.set("status", "active");
                itemRepo.create(item);
            }
            
            // Auditar
            auditService.logAction(
                Session.getCurrentUserId(),
                "create_loan",
                "transactions",
                loan.getId(),
                String.format("person_id=%d, items=%d, expected_return=%s",
                    personId, items.size(), expectedReturnDate)
            );
            
            Base.commitTransaction();
            return loan;
            
        } catch (Exception e) {
            Base.rollbackTransaction();
            throw e;
        }
    });
}

/**
 * Crea un ingreso asociado a la devolución de un préstamo
 */
public Transaction createLoanReturn(Integer loanId,
                                   String returnType,
                                   List<TransactionItemDTO> items,
                                   LocalDate returnDate,
                                   String returnNotes) {
    
    // Validar permiso
    if (!PermissionHelper.canAccess("INGRESOS", "CREATE")) {
        throw new SecurityException("No tiene permiso para crear ingresos");
    }
    
    return DatabaseManager.callInConnection(() -> {
        Base.openTransaction();
        try {
            // 1. Obtener el préstamo original
            Transaction loan = transactionRepo.findById(loanId);
            if (loan == null) {
                throw new IllegalArgumentException("Préstamo no encontrado");
            }
            
            if (!"loan".equals(loan.getString("type"))) {
                throw new IllegalArgumentException("La transacción no es un préstamo");
            }
            
            if (!"pending".equals(loan.getString("loan_status"))) {
                throw new IllegalStateException(
                    "El préstamo ya fue devuelto o finalizado. Estado: " + 
                    loan.getString("loan_status")
                );
            }
            
            // 2. Crear transacción de ingreso (devolución)
            Transaction returnTransaction = new Transaction();
            returnTransaction.set("type", "in");
            returnTransaction.set("date", returnDate);
            returnTransaction.set("user_id", Session.getCurrentUserId());
            returnTransaction.set("person_id", loan.getInteger("person_id"));
            returnTransaction.set("notes", "DEVOLUCIÓN DE PRÉSTAMO #" + loanId + 
                (returnNotes != null ? " - " + returnNotes : ""));
            returnTransaction.set("related_transaction_id", loanId);
            returnTransaction.set("status", "active");
            returnTransaction.saveIt();
            
            // 3. Procesar items de devolución
            for (TransactionItemDTO itemDTO : items) {
                Product product = productRepo.findById(itemDTO.getProductId());
                if (product == null) {
                    throw new IllegalArgumentException("Producto no encontrado");
                }
                
                // Validar producto activo
                if (!"active".equals(product.getString("status"))) {
                    throw new IllegalStateException(
                        "No se pueden devolver productos inactivos o eliminados"
                    );
                }
                
                // Sumar al stock (devolución)
                product.updateStock(itemDTO.getQuantity());
                productRepo.save(product);
                
                // Crear item de devolución
                TransactionItem item = new TransactionItem();
                item.set("transaction_id", returnTransaction.getId());
                item.set("product_id", itemDTO.getProductId());
                item.set("quantity", itemDTO.getQuantity());
                item.set("notes", itemDTO.getNotes());
                item.set("status", "active");
                itemRepo.create(item);
            }
            
            // 4. Actualizar estado del préstamo original
            loan.markAsReturned(
                returnTransaction.getId(),
                returnType,
                java.sql.Date.valueOf(returnDate),
                returnNotes
            );
            
            // 5. Auditar
            auditService.logAction(
                Session.getCurrentUserId(),
                "loan_return",
                "transactions",
                returnTransaction.getId(),
                String.format("loan_id=%d, return_type=%s, items=%d",
                    loanId, returnType, items.size())
            );
            
            Base.commitTransaction();
            return returnTransaction;
            
        } catch (Exception e) {
            Base.rollbackTransaction();
            throw e;
        }
    });
}

/**
 * Lista préstamos pendientes
 */
public List<Transaction> listPendingLoans() {
    return DatabaseManager.callInConnection(() -> 
        transactionRepo.findPendingLoans()
    );
}

/**
 * Lista préstamos vencidos
 */
public List<Transaction> listOverdueLoans() {
    return DatabaseManager.callInConnection(() -> {
        List<Transaction> pending = transactionRepo.findPendingLoans();
        return pending.stream()
            .filter(Transaction::isLoanOverdue)
            .collect(Collectors.toList());
    });
}
```

#### 10.5 Actualizar TransactionRepository.java

```java
/**
 * Encuentra préstamos pendientes
 */
public List<Transaction> findPendingLoans() {
    return Transaction.where(
        "type = 'loan' AND loan_status = 'pending' AND status = 'active'"
    ).orderBy("expected_return_date ASC");
}

/**
 * Encuentra préstamos vencidos
 */
public List<Transaction> findOverdueLoans() {
    return Transaction.where(
        "type = 'loan' AND loan_status = 'pending' " +
        "AND expected_return_date < CURDATE() " +
        "AND status = 'active'"
    ).orderBy("expected_return_date ASC");
}

/**
 * Obtiene el historial de un préstamo (préstamo + devolución)
 */
public List<Transaction> getLoanHistory(Integer loanId) {
    List<Transaction> history = new ArrayList<>();
    
    // Obtener préstamo original
    Transaction loan = findById(loanId);
    if (loan != null) {
        history.add(loan);
        
        // Buscar devolución asociada
        Transaction returnTx = Transaction.findFirst(
            "related_transaction_id = ? AND type = 'in'", loanId
        );
        if (returnTx != null) {
            history.add(returnTx);
        }
    }
    
    return history;
}
```

#### 10.6 Actualizar LoansController.java (o crear si no existe)

```java
// Agregar campos en el formulario de creación de préstamo
@FXML
private DatePicker dpExpectedReturnDate;

@FXML
private void onCreateLoan() {
    // ... validaciones existentes ...
    
    LocalDate expectedReturn = dpExpectedReturnDate.getValue();
    if (expectedReturn == null) {
        Utils.showAlert("Error", "Debe especificar la fecha esperada de devolución");
        return;
    }
    
    if (expectedReturn.isBefore(LocalDate.now())) {
        Utils.showAlert("Error", "La fecha de devolución no puede ser anterior a hoy");
        return;
    }
    
    // Crear préstamo con fecha de devolución
    Transaction loan = transactionService.createLoan(
        items,
        selectedPerson.getId(),
        expectedReturn,
        txtNotes.getText()
    );
    
    Utils.showAlert("Éxito", "Préstamo registrado correctamente");
    clearForm();
    loadLoans();
}

// NUEVO: Método para registrar devolución
@FXML
private void onRegisterReturn() {
    Transaction selectedLoan = tblLoans.getSelectionModel().getSelectedItem();
    
    if (selectedLoan == null) {
        Utils.showAlert("Info", "Seleccione un préstamo para registrar devolución");
        return;
    }
    
    if (!selectedLoan.isLoanPending()) {
        Utils.showAlert("Error", "Este préstamo ya fue devuelto o finalizado");
        return;
    }
    
    // Mostrar diálogo de devolución
    showReturnDialog(selectedLoan);
}

private void showReturnDialog(Transaction loan) {
    Dialog<ReturnData> dialog = new Dialog<>();
    dialog.setTitle("Registrar Devolución de Préstamo");
    dialog.setHeaderText("Préstamo #" + loan.getId() + " - " + 
        loan.getParent(Person.class).getString("first_name"));
    
    // Crear formulario
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    
    // Tipo de devolución
    ComboBox<String> cmbReturnType = new ComboBox<>();
    cmbReturnType.getItems().addAll(
        "Mismo Producto (Devuelto)",
        "Compró el Producto (Pagado)",
        "Reemplazó con Otro",
        "Otro"
    );
    cmbReturnType.setValue("Mismo Producto (Devuelto)");
    
    // Fecha de devolución
    DatePicker dpReturnDate = new DatePicker(LocalDate.now());
    
    // Notas
    TextArea txtReturnNotes = new TextArea();
    txtReturnNotes.setPromptText("Observaciones sobre la devolución...");
    txtReturnNotes.setPrefRowCount(3);
    
    // Items a devolver (tabla con los productos del préstamo)
    TableView<TransactionItemDTO> tblReturnItems = new TableView<>();
    // ... configurar tabla con productos del préstamo original ...
    
    grid.add(new Label("Tipo de Devolución:"), 0, 0);
    grid.add(cmbReturnType, 1, 0);
    grid.add(new Label("Fecha de Devolución:"), 0, 1);
    grid.add(dpReturnDate, 1, 1);
    grid.add(new Label("Items a Devolver:"), 0, 2);
    grid.add(tblReturnItems, 0, 3, 2, 1);
    grid.add(new Label("Notas:"), 0, 4);
    grid.add(txtReturnNotes, 0, 5, 2, 1);
    
    dialog.getDialogPane().setContent(grid);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
    dialog.setResultConverter(buttonType -> {
        if (buttonType == ButtonType.OK) {
            String returnTypeCode = switch (cmbReturnType.getValue()) {
                case "Mismo Producto (Devuelto)" -> "same_product";
                case "Compró el Producto (Pagado)" -> "purchased";
                case "Reemplazó con Otro" -> "replaced";
                default -> "other";
            };
            
            return new ReturnData(
                returnTypeCode,
                dpReturnDate.getValue(),
                tblReturnItems.getItems(),
                txtReturnNotes.getText()
            );
        }
        return null;
    });
    
    Optional<ReturnData> result = dialog.showAndWait();
    result.ifPresent(data -> {
        try {
            transactionService.createLoanReturn(
                loan.getId(),
                data.returnType,
                data.items,
                data.returnDate,
                data.notes
            );
            
            Utils.showAlert("Éxito", "Devolución registrada correctamente");
            loadLoans();
            
        } catch (Exception e) {
            Utils.showAlert("Error", "Error al registrar devolución: " + e.getMessage());
        }
    });
}

// Clase auxiliar para datos de devolución
private record ReturnData(
    String returnType,
    LocalDate returnDate,
    List<TransactionItemDTO> items,
    String notes
) {}
```

#### 10.7 Actualizar IncomesController.java

```java
// Agregar opción para marcar ingreso como devolución de préstamo

@FXML
private CheckBox chkIsLoanReturn;

@FXML
private ComboBox<Transaction> cmbRelatedLoan;

@FXML
public void initialize() {
    // ... código existente ...
    
    // Mostrar opción de préstamo solo si hay préstamos pendientes
    chkIsLoanReturn.selectedProperty().addListener((obs, oldVal, newVal) -> {
        cmbRelatedLoan.setVisible(newVal);
        cmbRelatedLoan.setManaged(newVal);
        
        if (newVal) {
            loadPendingLoans();
        }
    });
}

private void loadPendingLoans() {
    List<Transaction> pendingLoans = transactionService.listPendingLoans();
    
    ObservableList<Transaction> loansList = FXCollections.observableArrayList(pendingLoans);
    cmbRelatedLoan.setItems(loansList);
    
    // Configurar display
    cmbRelatedLoan.setConverter(new StringConverter<Transaction>() {
        @Override
        public String toString(Transaction loan) {
            if (loan == null) return "";
            
            Person person = loan.getParent(Person.class);
            String personName = person != null ? 
                person.getString("first_name") + " " + person.getString("last_name") : 
                "N/A";
            
            return String.format("Préstamo #%d - %s - %s", 
                loan.getId(),
                personName,
                loan.getDate("date")
            );
        }
        
        @Override
        public Transaction fromString(String string) {
            return null;
        }
    });
}

@FXML
private void onSaveIncome() {
    // ... validaciones existentes ...
    
    if (chkIsLoanReturn.isSelected()) {
        Transaction relatedLoan = cmbRelatedLoan.getValue();
        
        if (relatedLoan == null) {
            Utils.showAlert("Error", "Debe seleccionar el préstamo que se está devolviendo");
            return;
        }
        
        // Crear ingreso como devolución de préstamo
        transactionService.createLoanReturn(
            relatedLoan.getId(),
            "same_product", // O permitir seleccionar
            items,
            dpDate.getValue(),
            txtNotes.getText()
        );
        
    } else {
        // Crear ingreso normal (código existente)
        transactionService.createMultiProductTransaction(
            "in",
            dpDate.getValue().toString(),
            Session.getCurrentUserId(),
            selectedPerson != null ? selectedPerson.getId() : null,
            txtNotes.getText(),
            items
        );
    }
    
    Utils.showAlert("Éxito", "Ingreso registrado correctamente");
    clearForm();
    loadIncomes();
}
```

#### 10.8 Indicadores Visuales en Tabla de Préstamos

```java
// En LoansController, agregar columnas visuales de estado

TableColumn<Transaction, String> colLoanStatus = new TableColumn<>("Estado Préstamo");
colLoanStatus.setCellFactory(col -> new TableCell<>() {
    @Override
    protected void updateItem(String loanStatus, boolean empty) {
        super.updateItem(loanStatus, empty);
        if (empty || loanStatus == null) {
            setText(null);
            setGraphic(null);
        } else {
            Transaction loan = getTableView().getItems().get(getIndex());
            Label label = new Label();
            
            if (loan.isLoanOverdue()) {
                label.setText("VENCIDO");
                label.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; " +
                             "-fx-padding: 5px; -fx-border-radius: 3px;");
            } else {
                String displayText = switch (loanStatus) {
                    case "pending" -> "PENDIENTE";
                    case "returned" -> "DEVUELTO";
                    case "paid" -> "PAGADO";
                    case "replaced" -> "REEMPLAZADO";
                    default -> loanStatus.toUpperCase();
                };
                
                label.setText(displayText);
                
                String bgColor = switch (loanStatus) {
                    case "pending" -> "#ff9800";
                    case "returned" -> "#4caf50";
                    case "paid" -> "#2196f3";
                    case "replaced" -> "#9c27b0";
                    default -> "#757575";
                };
                
                label.setStyle("-fx-background-color: " + bgColor + 
                             "; -fx-text-fill: white; -fx-padding: 5px;");
            }
            
            setGraphic(label);
        }
    }
});
colLoanStatus.setCellValueFactory(data -> 
    new SimpleStringProperty(data.getValue().getString("loan_status"))
);
```

#### 10.9 Dashboard: Alertas de Préstamos Vencidos

```java
// En DashboardController, agregar indicador de préstamos vencidos

@FXML
private Label lblOverdueLoans;

@FXML
public void initialize() {
    // ... código existente ...
    
    loadOverdueLoansAlert();
}

private void loadOverdueLoansAlert() {
    List<Transaction> overdueLoans = transactionService.listOverdueLoans();
    
    if (overdueLoans.isEmpty()) {
        lblOverdueLoans.setVisible(false);
    } else {
        lblOverdueLoans.setVisible(true);
        lblOverdueLoans.setText(overdueLoans.size() + " préstamo(s) vencido(s)");
        lblOverdueLoans.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                                "-fx-padding: 10px; -fx-font-weight: bold;");
        
        // Hacer clickeable para navegar a préstamos
        lblOverdueLoans.setOnMouseClicked(e -> navigateToLoans());
    }
}
```

#### 10.10 Reportes de Préstamos

```java
// En ReportService, agregar reporte de préstamos

public LoanReport generateLoanReport(LocalDate startDate, LocalDate endDate) {
    return DatabaseManager.callInConnection(() -> {
        List<Transaction> loans = Transaction.where(
            "type = 'loan' AND date BETWEEN ? AND ? AND status = 'active'",
            startDate, endDate
        ).orderBy("date DESC");
        
        int totalLoans = loans.size();
        int pending = (int) loans.stream()
            .filter(Transaction::isLoanPending)
            .count();
        int returned = (int) loans.stream()
            .filter(t -> "returned".equals(t.getString("loan_status")))
            .count();
        int overdue = (int) loans.stream()
            .filter(Transaction::isLoanOverdue)
            .count();
        
        return new LoanReport(totalLoans, pending, returned, overdue, loans);
    });
}
```

---

## 🎨 Cambios en UI/UX

### Indicadores Visuales

#### 1. Tabla de Productos
```java
// En las TableView, agregar columna de estado con indicador visual
TableColumn<Product, String> colStatus = new TableColumn<>("Estado");
colStatus.setCellFactory(col -> new TableCell<>() {
    @Override
    protected void updateItem(String status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
            setText(null);
            setGraphic(null);
        } else {
            Label label = new Label(status.toUpperCase());
            switch (status) {
                case "active" -> label.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                case "inactive" -> label.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
                case "deleted" -> label.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            }
            setGraphic(label);
        }
    }
});
```

#### 2. Filtros en Tablas
```java
// Agregar ComboBox para filtrar por estado
ComboBox<String> cmbStatusFilter = new ComboBox<>();
cmbStatusFilter.getItems().addAll("Todos", "Activos", "Inactivos");

// Solo mostrar "Eliminados" si tiene permiso VIEW_DELETED
if (PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
    cmbStatusFilter.getItems().add("Eliminados");
}

cmbStatusFilter.setValue("Activos");
cmbStatusFilter.setOnAction(e -> loadProductsByStatus(cmbStatusFilter.getValue()));
```

#### 3. Diálogo de Restauración
Crear un diálogo modal que muestre:
- Lista de elementos eliminados/invalidados
- Fecha de eliminación
- Usuario que eliminó
- Razón (en caso de transacciones)
- Botón "Restaurar" por elemento

---

## 🔄 Impacto en Stock (Transacciones)

### Consideraciones Especiales

#### Invalidar Transacción
```java
public void invalidate(Transaction transaction, String reason) {
    DatabaseManager.runInConnection(() -> {
        Base.openTransaction();
        try {
            // 1. Marcar transacción como invalidada
            transaction.invalidate(Session.getCurrentUserId(), reason);
            
            // 2. REVERTIR el efecto en stock de cada item
            List<TransactionItem> items = transactionItemRepo.findByTransactionId(
                transaction.getInteger("id")
            );
            
            for (TransactionItem item : items) {
                Product product = item.getProduct();
                String type = transaction.getString("type");
                double quantity = item.getDouble("quantity");
                
                // Revertir: si fue ingreso, restar; si fue egreso, sumar
                if ("in".equals(type)) {
                    product.updateStock(-quantity); // Restar lo que se había sumado
                } else {
                    product.updateStock(quantity); // Sumar lo que se había restado
                }
                productRepo.save(product);
                
                // Marcar item como cancelado
                item.set("status", "cancelled");
                item.saveIt();
            }
            
            // 3. Auditar
            auditService.logAction(
                Session.getCurrentUserId(),
                "invalidate",
                "transactions",
                transaction.getInteger("id"),
                String.format("type=%s, reason=%s, items=%d", 
                    transaction.getString("type"), reason, items.size())
            );
            
            Base.commitTransaction();
        } catch (Exception e) {
            Base.rollbackTransaction();
            throw e;
        }
    });
}
```

#### Restaurar Transacción
```java
public void restore(Transaction transaction) {
    // Validar permiso
    if (!PermissionHelper.canAccess(getModuleForType(transaction.getString("type")), "RESTORE")) {
        throw new SecurityException("No tiene permiso para restaurar esta transacción");
    }
    
    DatabaseManager.runInConnection(() -> {
        Base.openTransaction();
        try {
            // 1. Restaurar transacción
            transaction.restore();
            
            // 2. REAPLICAR el efecto en stock
            List<TransactionItem> items = transactionItemRepo.findByTransactionId(
                transaction.getInteger("id")
            );
            
            for (TransactionItem item : items) {
                Product product = item.getProduct();
                String type = transaction.getString("type");
                double quantity = item.getDouble("quantity");
                
                // Reaplicar: si fue ingreso, sumar; si fue egreso, restar
                if ("in".equals(type)) {
                    product.updateStock(quantity);
                } else {
                    product.updateStock(-quantity);
                }
                productRepo.save(product);
                
                // Reactivar item
                item.set("status", "active");
                item.saveIt();
            }
            
            // 3. Auditar
            auditService.logAction(
                Session.getCurrentUserId(),
                "restore",
                "transactions",
                transaction.getInteger("id"),
                String.format("type=%s, items=%d", transaction.getString("type"), items.size())
            );
            
            Base.commitTransaction();
        } catch (Exception e) {
            Base.rollbackTransaction();
            throw e;
        }
    });
}
```

---

## 📝 Testing y Validación

### Casos de Prueba Críticos

1. **Usuario Normal (OPERADOR)**
   - ❌ NO puede eliminar físicamente
   - ✅ Puede deshabilitar productos (soft delete)
   - ❌ NO puede ver datos eliminados
   - ❌ NO puede restaurar datos

2. **Usuario Supervisor**
   - ❌ NO puede eliminar físicamente
   - ✅ Puede deshabilitar productos
   - ❌ NO puede ver datos eliminados (a menos que se le dé VIEW_DELETED)
   - ✅ Puede restaurar datos (si se le asigna RESTORE)

3. **Usuario ROOT**
   - ✅ Puede eliminar físicamente
   - ✅ Puede deshabilitar productos
   - ✅ Puede ver datos eliminados
   - ✅ Puede restaurar datos

4. **Transacciones**
   - Al invalidar una transacción, el stock se revierte correctamente
   - Al restaurar una transacción, el stock se reaplica correctamente
   - No se puede restaurar si causaría stock negativo

5. **Préstamos y Devoluciones (FASE 10)**
   - ✅ Crear préstamo reduce el stock correctamente
   - ✅ Préstamo se crea con status='pending'
   - ✅ Fecha de devolución esperada se guarda correctamente
   - ✅ No se puede crear préstamo con productos eliminados
   - ✅ Devolver préstamo incrementa el stock correctamente
   - ✅ Estado del préstamo cambia a 'returned', 'paid' o 'replaced' según tipo
   - ✅ No se puede devolver un préstamo ya devuelto
   - ✅ Ingreso de devolución se vincula correctamente con préstamo original
   - ✅ Préstamos vencidos se identifican correctamente
   - ✅ Dashboard muestra alertas de préstamos vencidos
   - ✅ Reporte de préstamos calcula estadísticas correctamente
   - ✅ Historial de préstamo muestra transacción original y devolución

---

## 📦 Orden de Implementación

### Sprint 1: Base de Datos y Permisos (Día 1-2)
1. ✅ Modificar todas las migraciones con campos de status
2. ✅ Agregar nuevos permisos en seed
3. ✅ Ejecutar migraciones y verificar estructura
4. ✅ Verificar que datos existentes quedan con status='active'

### Sprint 2: Modelos y Repositorios (Día 3-4)
1. ✅ Agregar métodos softDelete/restore en todos los modelos
2. ✅ Actualizar repositorios con métodos para filtrar por status
3. ✅ Agregar métodos findDeleted, findAllIncludingDeleted
4. ✅ Testing unitario de repositorios

### Sprint 3: Servicios (Día 5-6)
1. ✅ Actualizar ProductService con soft delete
2. ✅ Actualizar TransactionService con invalidate/restore (+ lógica de stock)
3. ✅ Actualizar UserService, PersonService, RolService
4. ✅ Validar auditorías se registren correctamente
5. ✅ Testing de servicios

### Sprint 4: Controladores y UI (Día 7-9)
1. ✅ Actualizar InventoryController con soft delete y restore
2. ✅ Actualizar controladores de transacciones (Ingresos/Egresos/Préstamos)
3. ✅ Actualizar UsersController, PeopleController, RolesController
4. ✅ Agregar botones de restauración en FXML
5. ✅ Agregar filtros de estado en tablas
6. ✅ Crear diálogos de restauración

### Sprint 5: Control de Acceso y Login (Día 10-11)
1. ✅ Modificar sistema de autenticación para validar status de usuario
2. ✅ Agregar validación de usuario activo en login
3. ✅ Implementar mensajes de error amigables en UI
4. ✅ Agregar validación periódica de sesión activa
5. ✅ Testing de login con usuarios inactivos/eliminados
6. ✅ Auditar intentos de login bloqueados

### Sprint 6: Resolución de Conflictos y Reportes (Día 12-14)
1. ✅ Actualizar sistema de reportes para filtrar por status
2. ✅ Agregar opción para incluir/excluir eliminados en reportes
3. ✅ Revisar y actualizar todas las búsquedas y filtros
4. ✅ Implementar validaciones en relaciones (producto-transacción, usuario-auditoría, etc.)
5. ✅ Actualizar estadísticas del Dashboard para excluir eliminados
6. ✅ Revisar y corregir ComboBox y selectores
7. ✅ Actualizar sistema de exportación de datos
8. ✅ Agregar validaciones de negocio para registros eliminados

### Sprint 7: Sistema de Devolución de Préstamos (Día 15-17)
1. ✅ Modificar migración para agregar campos de gestión de préstamos
2. ✅ Actualizar modelo Transaction con métodos de préstamo y devolución
3. ✅ Implementar createLoan con fecha de devolución esperada
4. ✅ Implementar createLoanReturn con tipos de devolución
5. ✅ Actualizar TransactionRepository con métodos de préstamos
6. ✅ Actualizar LoansController con diálogo de devolución
7. ✅ Actualizar IncomesController para vincular con préstamos
8. ✅ Agregar indicadores visuales de estado de préstamos
9. ✅ Implementar alertas de préstamos vencidos en Dashboard
10. ✅ Crear reportes de préstamos con estadísticas

### Sprint 8: Testing Integral y Documentación (Día 18)
1. ✅ Testing end-to-end con diferentes roles y escenarios
2. ✅ Verificar permisos funcionan correctamente en todas las áreas
3. ✅ Testing de reversión de stock en transacciones
4. ✅ Testing de reportes con datos eliminados
5. ✅ Verificar búsquedas y filtros excluyen eliminados
6. ✅ Testing de login con usuarios en diferentes estados
7. ✅ Testing de ciclo completo de préstamos (crear, devolver, vencidos)
8. ✅ Verificar cálculo correcto de stock con devoluciones
9. ✅ Documentar cambios en SECURITY.md y DATABASE.md
10. ✅ Actualizar README con nuevas funcionalidades

---

## ⚠️ Consideraciones Importantes

### Seguridad
- Solo ROOT puede eliminar físicamente (hard delete)
- Los permisos RESTORE son independientes por módulo
- Validar siempre permisos en backend (servicios), no solo en UI
- Auditar TODAS las operaciones de eliminación y restauración
- **CRÍTICO**: Usuarios inactivos/eliminados NO pueden hacer login (FASE 8)
- Validar estado de sesión periódicamente para cerrar sesiones de usuarios deshabilitados

### Performance
- Agregar índices en columnas `status` para mejorar consultas
- Los datos "eliminados" siguen en BD, considerar limpieza periódica de datos muy antiguos (solo ROOT)
- Optimizar consultas para excluir deleted por defecto
- Los filtros en reportes y búsquedas deben ser eficientes con índices

### Datos Relacionales
- Si un producto tiene transacciones, considerar no permitir eliminación física
- Al restaurar un producto, verificar que sus relaciones sean válidas
- Transacciones invalidadas mantienen los items pero con status='cancelled'
- **CRÍTICO**: Validar registros activos antes de crear relaciones (ej: transacción con producto eliminado)
- No permitir eliminar roles con usuarios activos asignados
- No permitir eliminación física de usuarios con auditorías

### Reportes y Visualización (FASE 9)
- **CRÍTICO**: Los reportes deben excluir registros eliminados por defecto
- Agregar opción para incluir eliminados solo si tiene permiso VIEW_DELETED
- Dashboard y estadísticas deben contar solo registros activos
- ComboBox y selectores deben mostrar solo registros activos
- Exportaciones deben indicar claramente si incluyen eliminados

### UX
- Indicar claramente cuando una operación es "Deshabilitar" vs "Eliminar Permanentemente"
- Usar colores distintivos: verde (activo), naranja (inactivo), rojo (eliminado)
- Confirmar siempre operaciones destructivas con diálogos claros
- Mostrar razón de invalidación en transacciones
- Mensajes de error amigables cuando un usuario intenta login con cuenta deshabilitada

### Búsquedas y Filtros
- **CRÍTICO**: Todas las búsquedas deben excluir registros eliminados por defecto
- Agregar filtro opcional para ver eliminados (solo con permiso)
- Validar que los filtros no causen conflictos con el estado

---

## 🎓 Beneficios del Sistema

1. **Integridad de Datos**: La información no se pierde accidentalmente
2. **Auditoría Completa**: Historial de qué se eliminó y quién lo hizo
3. **Recuperación Rápida**: Restaurar datos eliminados por error
4. **Cumplimiento**: Trazabilidad para auditorías legales/administrativas
5. **Control Granular**: Permisos específicos por módulo y acción
6. **Seguridad**: Solo ROOT puede hacer eliminaciones permanentes
7. **Gestión de Préstamos**: Ciclo completo desde préstamo hasta devolución
8. **Control de Vencimientos**: Alertas automáticas de préstamos vencidos
9. **Trazabilidad Completa**: Relación entre préstamos y devoluciones
10. **Flexibilidad**: Múltiples tipos de devolución (mismo producto, pagado, reemplazado)

---

## ✅ Checklist de Áreas Críticas a Revisar (FASE 9)

Antes de dar por completada la implementación, verificar que TODAS estas áreas funcionan correctamente:

### 🔐 Autenticación y Acceso
- [ ] Login bloquea usuarios con status != 'active'
- [ ] Mensajes de error claros para usuarios deshabilitados
- [ ] Auditoría registra intentos de login bloqueados
- [ ] Sesiones activas se invalidan si usuario es deshabilitado
- [ ] Rol eliminado no permite login a sus usuarios

### 📊 Reportes y Estadísticas
- [ ] Dashboard cuenta solo registros activos
- [ ] Reportes de stock excluyen productos eliminados
- [ ] Reportes de transacciones excluyen invalidadas por defecto
- [ ] Opción para incluir eliminados solo con permiso VIEW_DELETED
- [ ] Exportaciones indican claramente qué datos incluyen
- [ ] Gráficos y estadísticas usan datos activos

### 🔍 Búsquedas y Filtros
- [ ] Búsquedas globales excluyen registros eliminados
- [ ] Búsqueda de productos excluye status='deleted'
- [ ] Búsqueda de usuarios excluye status='deleted'
- [ ] Búsqueda de personas excluye status='deleted'
- [ ] Filtros avanzados respetan permisos VIEW_DELETED
- [ ] Autocomplete excluye registros eliminados

### 🎯 ComboBox y Selectores
- [ ] ComboBox de productos muestra solo activos
- [ ] ComboBox de usuarios muestra solo activos
- [ ] ComboBox de personas muestra solo activas
- [ ] ComboBox de roles muestra solo activos
- [ ] Selectores en formularios validan estado al guardar

### 🔗 Relaciones y Validaciones
- [ ] No se puede crear transacción con producto eliminado
- [ ] No se puede crear transacción con persona eliminada
- [ ] No se puede asignar rol eliminado a usuario
- [ ] Producto con transacciones activas no permite hard delete
- [ ] Usuario con auditorías no permite hard delete
- [ ] Rol con usuarios activos no permite eliminación

### 📝 Tablas y Listados
- [ ] Tabla de productos muestra solo activos por defecto
- [ ] Tabla de usuarios muestra solo activos por defecto
- [ ] Tabla de transacciones muestra solo activas por defecto
- [ ] Tabla de personas muestra solo activas por defecto
- [ ] Paginación respeta filtros de status
- [ ] Ordenamiento funciona con filtros de status

### 💾 Transacciones y Stock
- [ ] Invalidar transacción revierte stock correctamente
- [ ] Restaurar transacción reaplica stock correctamente
- [ ] Stock actual refleja solo transacciones activas
- [ ] Reportes de movimientos excluyen transacciones invalidadas
- [ ] Historial de producto muestra estado de transacciones

### 🔄 Préstamos y Devoluciones (FASE 10)
- [ ] Crear préstamo reduce stock correctamente
- [ ] Préstamo se guarda con status='pending'
- [ ] Fecha de devolución esperada se registra
- [ ] No se puede crear préstamo con producto eliminado
- [ ] Devolución incrementa stock correctamente
- [ ] Estado del préstamo cambia según tipo de devolución
- [ ] No se puede devolver préstamo ya devuelto
- [ ] Ingreso de devolución se vincula con préstamo original
- [ ] Préstamos vencidos se identifican automáticamente
- [ ] Dashboard muestra alertas de préstamos vencidos
- [ ] ComboBox de préstamos muestra solo pendientes
- [ ] Historial de préstamo muestra todas las transacciones
- [ ] Reporte de préstamos calcula estadísticas correctamente
- [ ] Tipos de devolución (devuelto, pagado, reemplazado) funcionan

### 🎨 UI/UX
- [ ] Botón "Eliminar" vs "Deshabilitar" según permisos
- [ ] Botón "Restaurar" visible solo con permiso
- [ ] Colores distintivos por estado (verde/naranja/rojo)
- [ ] Confirmaciones claras para operaciones destructivas
- [ ] Mensajes informativos al restaurar registros
- [ ] Indicadores visuales de estado en tablas

### 🔒 Permisos y Seguridad
- [ ] SISTEMA.DESTRUCTIVE_OPERATIONS solo ROOT
- [ ] SISTEMA.VIEW_DELETED funciona correctamente
- [ ] Permisos RESTORE independientes por módulo
- [ ] Validaciones en servicios (no solo UI)
- [ ] Auditoría completa de todas las operaciones

### 📈 Performance
- [ ] Índices en columnas status creados
- [ ] Consultas optimizadas con filtros de status
- [ ] Sin consultas N+1 al cargar relaciones
- [ ] Reportes grandes funcionan sin timeout

---

## 📚 Referencias

- Migraciones actuales: `src/main/resources/migrations/`
- Documentación de seguridad: `docs/SECURITY.md`
- Sistema de permisos: `docs/PLAN_COMPLETO_PERMISOS.md`
- Arquitectura: `docs/ARCHITECTURE.md`

---

**Autor**: Sistema de Inventario Municipal  
**Fecha**: 27 de Noviembre, 2025  
**Versión**: 1.0

