# 📋 PLAN DE IMPLEMENTACIÓN - FASE 5: Controladores

**Fecha**: 27 de Noviembre, 2025  
**Estado**: EN PROGRESO

---

## 🎯 Objetivo

Actualizar los controladores para implementar la UI de:
- Soft Delete / Hard Delete (solo ROOT)
- Restauración (requiere permiso RESTORE)
- Vista de registros eliminados (requiere VIEW_DELETED)
- Gestión de préstamos (crear, devolver, alertas)
- Invalidación de transacciones

---

## 📝 Controladores a Modificar

### 1. InventoryController.java ⏳
**Prioridad**: ALTA

**Funcionalidad a agregar**:
- ✅ Botón "Ver Eliminados" (solo si tiene VIEW_DELETED)
- ✅ Botón "Restaurar" en vista de eliminados
- ✅ Botón "Eliminar Permanentemente" (solo ROOT)
- ✅ Cambiar botón "Eliminar" a soft delete
- ✅ Dialog de confirmación con opciones
- ✅ Columna de estado visual (Activo/Inactivo/Eliminado)

**Métodos a implementar**:
```java
- showDeletedProducts()      // Vista de eliminados
- restoreProduct()            // Restaurar
- hardDeleteProduct()         // Eliminar permanentemente (ROOT)
- toggleProductStatus()       // Activar/Desactivar
- confirmDelete()             // Dialog mejorado
```

---

### 2. UsersController.java ⏳
**Prioridad**: ALTA

**Funcionalidad a agregar**:
- ✅ Botón "Ver Eliminados"
- ✅ Botón "Restaurar"
- ✅ Botón "Eliminar Permanentemente" (solo ROOT)
- ✅ Botón "Activar/Desactivar" (cambia status)
- ✅ Columna de estado visual
- ✅ Indicador si usuario está inactivo (no puede login)

**Métodos a implementar**:
```java
- showDeletedUsers()
- restoreUser()
- hardDeleteUser()           // Solo ROOT
- toggleUserStatus()          // Activar/Desactivar
- showStatusIndicator()       // Badge visual
```

---

### 3. PeopleController.java ⏳
**Prioridad**: ALTA

**Funcionalidad a agregar**:
- ✅ Botón "Ver Eliminados"
- ✅ Botón "Restaurar"
- ✅ Botón "Eliminar Permanentemente" (solo ROOT)
- ✅ Botón "Activar/Desactivar"
- ✅ Columna de estado visual

**Métodos a implementar**:
```java
- showDeletedPeople()
- restorePerson()
- hardDeletePerson()          // Solo ROOT
- togglePersonStatus()
```

---

### 4. RolesController.java ⏳
**Prioridad**: MEDIA

**Funcionalidad a agregar**:
- ✅ Botón "Ver Eliminados"
- ✅ Botón "Restaurar"
- ✅ Botón "Eliminar Permanentemente" (solo ROOT)
- ✅ Validación: no eliminar si tiene usuarios activos
- ✅ Columna de estado visual
- ✅ Indicador de cantidad de usuarios asignados

**Métodos a implementar**:
```java
- showDeletedRoles()
- restoreRole()
- hardDeleteRole()            // Solo ROOT
- validateRoleHasUsers()      // Antes de eliminar
- showUserCount()             // En tabla
```

---

### 5. IncomesController.java ⏳
**Prioridad**: ALTA

**Funcionalidad a agregar**:
- ✅ Botón "Invalidar" (marca como errónea + revierte stock)
- ✅ Botón "Ver Invalidadas"
- ✅ Botón "Restaurar" transacción invalidada
- ✅ Dialog con razón obligatoria para invalidar
- ✅ Columna de estado visual (Activa/Invalidada)
- ✅ Badge indicando si es devolución de préstamo

**Métodos a implementar**:
```java
- invalidateIncome()          // Con razón
- showInvalidatedIncomes()
- restoreIncome()
- showInvalidationReason()    // En tooltip o dialog
```

---

### 6. OutcomesController.java ⏳
**Prioridad**: ALTA

**Funcionalidad a agregar**:
- ✅ Botón "Invalidar"
- ✅ Botón "Ver Invalidadas"
- ✅ Botón "Restaurar"
- ✅ Dialog con razón obligatoria
- ✅ Columna de estado visual

**Métodos a implementar**:
```java
- invalidateOutcome()
- showInvalidatedOutcomes()
- restoreOutcome()
```

---

### 7. LoansController.java ⏳⏳⏳
**Prioridad**: MUY ALTA (FASE 10)

**Funcionalidad a agregar**:
- ✅ Crear préstamo con código automático
- ✅ Dialog multi-producto para préstamo
- ✅ Campo de fecha esperada de devolución
- ✅ Botón "Registrar Devolución"
- ✅ Dialog de devolución con tipo (mismo producto, pagado, reemplazado)
- ✅ Vista de préstamos pendientes
- ✅ Vista de préstamos vencidos (CON ALERTA VISUAL)
- ✅ Badge de estado: Pendiente/Vencido/Devuelto/Pagado
- ✅ Columna de código de préstamo
- ✅ Columna de días restantes/vencidos
- ✅ Filtro por estado
- ✅ Botón "Invalidar" préstamo
- ✅ Vista de préstamos invalidados

**Métodos a implementar**:
```java
- createLoan()                // Con código automático
- showLoanDialog()            // Multi-producto + fecha
- registerReturn()            // Vincula con préstamo
- showReturnDialog()          // Tipo de devolución
- filterByStatus()            // Pendientes/Vencidos/Devueltos
- showOverdueAlert()          // Badge rojo
- showLoanHistory()           // Préstamo + devolución
- invalidateLoan()
- restoreLoan()
- showDaysRemaining()         // En columna
```

---

## 🎨 Componentes UI Nuevos a Crear

### 1. DeleteConfirmationDialog
```java
Opciones:
- [ ] Soft Delete (reversible)
- [ ] Hard Delete (permanente) - Solo ROOT

Inputs:
- Razón (opcional para soft, obligatorio para hard)
- Checkbox de confirmación

Botones:
- [Cancelar] [Eliminar]
```

### 2. InvalidateTransactionDialog
```java
Inputs:
- Razón (obligatorio)
- Checkbox "Estoy seguro de invalidar esta transacción"

Botones:
- [Cancelar] [Invalidar]

Warning:
"⚠️ Esto revertirá el stock automáticamente"
```

### 3. LoanDialog (Multi-Producto)
```java
Inputs:
- Persona (ComboBox con búsqueda)
- Fecha esperada devolución (DatePicker)
- Tabla de productos:
  - Producto (con búsqueda)
  - Cantidad
  - Notas
  - [+] Agregar otro producto
- Notas generales

Botones:
- [Cancelar] [Crear Préstamo]

Info:
"✓ Se generará código automático: P-XX20251127-001"
```

### 4. LoanReturnDialog
```java
Inputs:
- Tipo de devolución:
  [ ] Mismo producto
  [ ] Pagado/Comprado
  [ ] Reemplazado
  [ ] Otro
- Tabla de productos devueltos:
  - Producto (prellenado del préstamo)
  - Cantidad devuelta
  - Notas
- Notas generales

Botones:
- [Cancelar] [Registrar Devolución]

Info:
"Préstamo: P-XX20251127-001"
"Días de préstamo: 15 días"
```

### 5. StatusBadge Component
```java
Estados:
- ✅ ACTIVO (verde)
- ⏸️ INACTIVO (amarillo)
- ❌ ELIMINADO (rojo)
- 🚫 INVALIDADO (naranja)
- ⏰ PENDIENTE (azul)
- ⏰ VENCIDO (rojo parpadeante)
- ✓ DEVUELTO (verde)
- 💰 PAGADO (verde oscuro)
```

---

## 🔐 Permisos a Verificar en UI

### Botones que requieren permisos especiales:

| Botón/Acción | Permiso Requerido |
|--------------|-------------------|
| Ver Eliminados | SISTEMA.VIEW_DELETED |
| Restaurar | [MODULO].RESTORE |
| Eliminar Permanentemente | SISTEMA.DESTRUCTIVE_OPERATIONS |
| Invalidar Transacción | [MODULO].DELETE |
| Restaurar Transacción | [MODULO].RESTORE |

### Lógica de visibilidad:
```java
// Solo mostrar botón si tiene permiso
if (PermissionHelper.canAccess("SISTEMA", "VIEW_DELETED")) {
    btnViewDeleted.setVisible(true);
}

// Deshabilitar botón si no tiene permiso
if (!PermissionHelper.canAccess("INVENTARIO", "RESTORE")) {
    btnRestore.setDisable(true);
}
```

---

## 📊 Orden de Implementación Sugerido

### Día 1-2: Controladores Básicos
1. ✅ InventoryController - Soft delete + restauración
2. ✅ UsersController - Soft delete + estados
3. ✅ PeopleController - Soft delete + restauración

### Día 2-3: Transacciones
4. ✅ IncomesController - Invalidación
5. ✅ OutcomesController - Invalidación

### Día 3-4: Préstamos (FASE 10)
6. ✅ LoansController - Gestión completa

### Día 4-5: Roles y Pulido
7. ✅ RolesController - Soft delete + validaciones
8. ✅ Testing y ajustes finales

---

## 🧪 Testing Checklist

### Para cada controlador:
- [ ] Soft delete funciona
- [ ] Hard delete solo visible para ROOT
- [ ] Restauración funciona
- [ ] Vista de eliminados requiere permiso
- [ ] Estados visuales correctos
- [ ] Confirmaciones funcionan
- [ ] Auditoría se registra
- [ ] No hay errores de UI
- [ ] Permisos se respetan

### Específico para LoansController:
- [ ] Código se genera automáticamente
- [ ] Multi-producto funciona
- [ ] Fecha obligatoria valida
- [ ] Devolución actualiza estado
- [ ] Préstamos vencidos se marcan en rojo
- [ ] Historial muestra préstamo + devolución
- [ ] Filtros funcionan
- [ ] Alertas visuales aparecen

---

## 📝 Notas Importantes

### Diseño Consistente
- Todos los botones de eliminar: rojo
- Todos los botones de restaurar: verde
- Todos los botones de invalidar: naranja
- Estados: badges con colores consistentes

### UX
- Siempre pedir confirmación para acciones destructivas
- Mostrar razón al invalidar (obligatorio)
- Tooltips en botones para claridad
- Feedback visual inmediato
- Loading indicators para operaciones largas

### Seguridad
- Validar permisos en UI Y en backend
- No mostrar opciones sin permiso
- Mensajes claros cuando no tiene permiso
- Logging de todas las acciones

---

## 🚀 Estado Actual

```
InventoryController      ⏳ Por iniciar
UsersController          ⏳ Por iniciar
PeopleController         ⏳ Por iniciar
RolesController          ⏳ Por iniciar
IncomesController        ⏳ Por iniciar
OutcomesController       ⏳ Por iniciar
LoansController          ⏳ Por iniciar
```

**Progreso FASE 5**: 0%

---

**Inicio de Implementación**: 27 de Noviembre, 2025  
**Duración Estimada**: 2-3 días  
**Prioridad**: ALTA

