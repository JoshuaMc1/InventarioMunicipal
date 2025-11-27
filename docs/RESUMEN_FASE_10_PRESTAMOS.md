# 🔄 FASE 10: Sistema de Devolución de Préstamos

## 📋 Resumen Ejecutivo

**Objetivo**: Implementar un ciclo completo de gestión de préstamos que permita registrar devoluciones, controlar vencimientos y gestionar diferentes tipos de retorno del producto.

**Problema Actual**:
- ❌ Los préstamos se registran pero no hay forma de cerrar el ciclo
- ❌ No se sabe si un préstamo fue devuelto, pagado o está pendiente
- ❌ No hay relación entre el préstamo original y su devolución
- ❌ No hay alertas de préstamos vencidos

**Solución Propuesta**:
- ✅ Agregar estado de préstamo en la tabla `transactions`
- ✅ Permitir asociar un ingreso con un préstamo original (devolución)
- ✅ Tipos de devolución: Devuelto, Pagado, Reemplazado
- ✅ Cambiar estado del préstamo automáticamente al registrar devolución
- ✅ Alertas de préstamos vencidos en Dashboard

---

## 🗄️ Cambios en Base de Datos

### Nuevos Campos en Tabla `transactions`

```sql
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
```

### Significado de los Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `loan_code` | VARCHAR(20) | Código único amigable del préstamo (ej: P-JA20251127-001) |
| `loan_status` | ENUM | Estado del préstamo: pending, returned, paid, replaced, overdue |
| `expected_return_date` | DATE | Fecha esperada de devolución del préstamo |
| `actual_return_date` | DATE | Fecha real en que se devolvió |
| `related_transaction_id` | INT | Si es un ingreso, referencia al préstamo que devuelve |
| `return_type` | ENUM | Tipo de devolución: mismo producto, comprado, reemplazado |
| `return_notes` | TEXT | Notas sobre la devolución |

### Formato del Código de Préstamo

**Estructura**: `P-{INICIALES}{YYYYMMDD}-{SECUENCIA}`

**Ejemplos**:
- `P-JA20251127-001` - Primer préstamo de Juan Almendares el 27/11/2025
- `P-ML20251127-002` - Segundo préstamo de María López el 27/11/2025  
- `P-PG20251128-001` - Primer préstamo de Pedro Gómez el 28/11/2025

**Ventajas**:
- ✅ Fácil de recordar y comunicar
- ✅ Identifica a la persona (iniciales)
- ✅ Identifica la fecha del préstamo
- ✅ Secuencia para múltiples préstamos del mismo día
- ✅ Único en todo el sistema

---

## 🔄 Flujo de Funcionamiento

### Escenario 1: Préstamo Normal con Devolución

```
1. CREAR PRÉSTAMO
   └─> Usuario registra préstamo a Juan Almendares
   └─> Especifica fecha esperada de devolución
   └─> Sistema genera código automáticamente: P-JA20251127-001
   └─> Stock se REDUCE (productos prestados)
   └─> Préstamo queda en status='pending'

2. REGISTRO EN SISTEMA
   └─> Transaction type='loan'
   └─> loan_code='P-JA20251127-001'
   └─> loan_status='pending'
   └─> expected_return_date=2025-12-15
   └─> Stock: 100 → 95 (se prestaron 5 unidades)
   └─> Usuario recibe comprobante con código P-JA20251127-001

3. DEVOLUCIÓN DEL PRÉSTAMO
   └─> Usuario ingresa código: P-JA20251127-001
   └─> O busca por nombre: Juan Almendares
   └─> Selecciona el préstamo que se está devolviendo
   └─> Especifica tipo: "Mismo Producto (Devuelto)"
   └─> Stock se INCREMENTA (productos devueltos)

4. ACTUALIZACIÓN AUTOMÁTICA
   └─> Se crea Transaction type='in' (ingreso)
   └─> related_transaction_id apunta al préstamo original
   └─> Préstamo P-JA20251127-001 cambia a loan_status='returned'
   └─> actual_return_date=fecha_actual
   └─> Stock: 95 → 100 (productos devueltos)
```

### Escenario 2: Préstamo Pagado (Compra)

```
1. PRÉSTAMO INICIAL
   └─> Persona recibe producto en préstamo
   └─> Préstamo pendiente

2. PERSONA COMPRA EL PRODUCTO
   └─> En vez de devolver, compra el producto
   └─> Usuario registra "Devolución tipo Pagado"
   └─> NO se incrementa el stock (producto vendido)
   └─> Préstamo cambia a loan_status='paid'
   └─> Se registra como ingreso pero sin afectar inventario
```

### Escenario 3: Préstamo Reemplazado

```
1. PRÉSTAMO INICIAL
   └─> Persona recibe producto A en préstamo
   └─> Préstamo pendiente

2. PERSONA DEVUELVE PRODUCTO DIFERENTE
   └─> Devuelve producto B en vez de producto A
   └─> Usuario registra "Devolución tipo Reemplazado"
   └─> Stock de B se incrementa
   └─> Stock de A queda pendiente de recuperar
   └─> Préstamo cambia a loan_status='replaced'
```

### Escenario 4: Préstamo Vencido

```
1. PRÉSTAMO CON FECHA ESPERADA
   └─> expected_return_date=2025-11-20
   └─> Fecha actual: 2025-11-27
   └─> loan_status='pending'

2. SISTEMA IDENTIFICA VENCIMIENTO
   └─> Método isLoanOverdue() retorna true
   └─> Dashboard muestra alerta roja
   └─> Reporte incluye en "Préstamos Vencidos"
   └─> Supervisor puede tomar acción
```

---

## 🔢 Generación de Código de Préstamo

### Lógica de Generación Automática

El código de préstamo se genera automáticamente al crear un préstamo y sigue este formato:

**Formato**: `P-{INICIALES}{YYYYMMDD}-{SECUENCIA}`

#### Componentes del Código

| Componente | Descripción | Ejemplo |
|------------|-------------|---------|
| `P` | Prefijo fijo para préstamos | P |
| `INICIALES` | Primera letra del nombre + primera letra del apellido | JA (Juan Almendares) |
| `YYYYMMDD` | Año, mes y día del préstamo | 20251127 |
| `SECUENCIA` | Número secuencial (3 dígitos, empezando en 001) | 001 |

#### Ejemplos Completos

```
Persona: Juan Almendares
Fecha: 27/11/2025
Primer préstamo del día
→ Código: P-JA20251127-001

Persona: Juan Almendares  
Fecha: 27/11/2025
Segundo préstamo del mismo día
→ Código: P-JA20251127-002

Persona: María López
Fecha: 27/11/2025
Primer préstamo del día
→ Código: P-ML20251127-001

Persona: Juan Almendares
Fecha: 28/11/2025
Primer préstamo del nuevo día
→ Código: P-JA20251128-001
```

### Implementación en Java

```java
// En TransactionService.java o nueva clase LoanCodeGenerator.java

public class LoanCodeGenerator {
    
    /**
     * Genera un código único de préstamo para una persona en una fecha específica
     * Formato: P-{INICIALES}{YYYYMMDD}-{SECUENCIA}
     * 
     * @param person Persona que recibe el préstamo
     * @param loanDate Fecha del préstamo
     * @return Código único de préstamo
     */
    public static String generateLoanCode(Person person, LocalDate loanDate) {
        // 1. Obtener iniciales (primera letra de nombre y apellido)
        String firstName = person.getString("first_name");
        String lastName = person.getString("last_name");
        
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) {
            initials += firstName.substring(0, 1).toUpperCase();
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials += lastName.substring(0, 1).toUpperCase();
        }
        
        // Si no hay iniciales válidas, usar XX
        if (initials.isEmpty()) {
            initials = "XX";
        }
        
        // 2. Formatear fecha como YYYYMMDD
        String dateStr = loanDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 3. Obtener secuencia
        int sequence = getNextSequence(initials, dateStr);
        
        // 4. Construir código completo
        return String.format("P-%s%s-%03d", initials, dateStr, sequence);
    }
    
    /**
     * Obtiene el siguiente número de secuencia para un conjunto de iniciales y fecha
     * 
     * @param initials Iniciales de la persona
     * @param dateStr Fecha formateada como YYYYMMDD
     * @return Siguiente número de secuencia disponible
     */
    private static int getNextSequence(String initials, String dateStr) {
        return DatabaseManager.callInConnection(() -> {
            // Buscar el último préstamo con estas iniciales y fecha
            String pattern = "P-" + initials + dateStr + "-%";
            
            List<Transaction> existingLoans = Transaction.where(
                "type = 'loan' AND loan_code LIKE ? ORDER BY loan_code DESC LIMIT 1",
                pattern
            );
            
            if (existingLoans.isEmpty()) {
                return 1; // Primera secuencia del día
            }
            
            // Extraer la secuencia del último código
            Transaction lastLoan = existingLoans.get(0);
            String lastCode = lastLoan.getString("loan_code");
            
            // El código tiene formato P-JA20251127-001, extraer los últimos 3 dígitos
            String sequenceStr = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            int lastSequence = Integer.parseInt(sequenceStr);
            
            return lastSequence + 1; // Siguiente secuencia
        });
    }
    
    /**
     * Valida que un código de préstamo sea único
     * 
     * @param loanCode Código a validar
     * @return true si el código es único, false si ya existe
     */
    public static boolean isLoanCodeUnique(String loanCode) {
        return DatabaseManager.callInConnection(() -> {
            long count = Transaction.count("loan_code = ?", loanCode);
            return count == 0;
        });
    }
    
    /**
     * Busca un préstamo por su código
     * 
     * @param loanCode Código del préstamo
     * @return Transaction del préstamo o null si no existe
     */
    public static Transaction findByLoanCode(String loanCode) {
        return DatabaseManager.callInConnection(() -> 
            Transaction.findFirst("loan_code = ? AND type = 'loan'", loanCode)
        );
    }
}
```

### Uso en TransactionService

```java
// En TransactionService.java

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
            // Obtener persona
            Person person = Person.findById(personId);
            if (person == null) {
                throw new IllegalArgumentException("Persona no encontrada");
            }
            
            // Generar código de préstamo único
            String loanCode = LoanCodeGenerator.generateLoanCode(person, LocalDate.now());
            
            // Crear transacción de préstamo
            Transaction loan = new Transaction();
            loan.set("type", "loan");
            loan.set("loan_code", loanCode);  // ← NUEVO
            loan.set("date", LocalDate.now());
            loan.set("user_id", Session.getCurrentUserId());
            loan.set("person_id", personId);
            loan.set("notes", notes);
            loan.set("loan_status", "pending");
            loan.set("expected_return_date", expectedReturnDate);
            loan.set("status", "active");
            loan.saveIt();
            
            // Procesar items (código existente...)
            for (TransactionItemDTO itemDTO : items) {
                // ... código existente ...
            }
            
            // Auditar
            auditService.logAction(
                Session.getCurrentUserId(),
                "create_loan",
                "transactions",
                loan.getId(),
                String.format("loan_code=%s, person_id=%d, items=%d, expected_return=%s",
                    loanCode, personId, items.size(), expectedReturnDate)
            );
            
            Base.commitTransaction();
            
            // Mostrar mensaje de confirmación con código
            Logs.info(getClass(), String.format(
                "Préstamo creado exitosamente. Código: %s", loanCode
            ));
            
            return loan;
            
        } catch (Exception e) {
            Base.rollbackTransaction();
            throw e;
        }
    });
}
```

### Búsqueda por Código

```java
// En TransactionService.java

/**
 * Busca un préstamo por su código
 */
public Transaction findLoanByCode(String loanCode) {
    if (loanCode == null || loanCode.trim().isEmpty()) {
        throw new IllegalArgumentException("Código de préstamo inválido");
    }
    
    Transaction loan = LoanCodeGenerator.findByLoanCode(loanCode.trim().toUpperCase());
    
    if (loan == null) {
        throw new IllegalArgumentException(
            "No se encontró ningún préstamo con el código: " + loanCode
        );
    }
    
    return loan;
}
```

### Comprobante de Préstamo

Después de crear un préstamo, mostrar comprobante con el código:

```
┌─────────────────────────────────────────────┐
│          COMPROBANTE DE PRÉSTAMO            │
├─────────────────────────────────────────────┤
│                                             │
│  Código: P-JA20251127-001                   │
│                                             │
│  Persona: Juan Almendares                   │
│  DNI: 001-120385-0001N                      │
│                                             │
│  Fecha Préstamo: 27/11/2025                 │
│  Fecha Esperada Devolución: 15/12/2025      │
│                                             │
│  Productos Prestados:                       │
│  • Electrodos (50 unidades)                 │
│  • Canaletas (5 unidades)                   │
│                                             │
│  Notas: Préstamo para proyecto X            │
│                                             │
│  ─────────────────────────────────────────  │
│  Favor presentar este código al momento     │
│  de devolver los productos prestados.       │
│  ─────────────────────────────────────────  │
│                                             │
│  Usuario: operador1                         │
│  Fecha Impresión: 27/11/2025 10:30 AM      │
└─────────────────────────────────────────────┘
```

---

## 💻 Implementación Técnica

### Métodos Clave en Transaction.java

```java
// Verificaciones
boolean isLoan()              // ¿Es un préstamo?
boolean isLoanPending()       // ¿Está pendiente?
boolean isLoanOverdue()       // ¿Está vencido?
boolean isLoanReturn()        // ¿Es una devolución?

// Acciones
void markAsReturned(...)      // Marcar como devuelto
Transaction getRelatedLoan()  // Obtener préstamo original
```

### Métodos Clave en TransactionService.java

```java
// Crear préstamo con fecha de devolución
Transaction createLoan(items, personId, expectedReturnDate, notes)

// Registrar devolución de préstamo
Transaction createLoanReturn(loanId, returnType, items, returnDate, notes)

// Consultas
List<Transaction> listPendingLoans()    // Préstamos pendientes
List<Transaction> listOverdueLoans()    // Préstamos vencidos
```

---

## 🎨 Cambios en UI

### 1. Formulario de Creación de Préstamo

**Agregar Campo**:
```
┌─────────────────────────────────────┐
│ Crear Préstamo                      │
├─────────────────────────────────────┤
│ Persona: [ComboBox ▼]              │
│ Productos: [Tabla Multi-producto]   │
│ Fecha Esperada Devolución: [📅]    │  ← NUEVO
│ Notas: [TextArea]                   │
│ [Cancelar]  [Guardar Préstamo]     │
└─────────────────────────────────────┘
```

### 2. Diálogo de Devolución

**Nuevo Diálogo**:
```
┌─────────────────────────────────────┐
│ Registrar Devolución                │
│ Préstamo #123 - Juan Pérez          │
├─────────────────────────────────────┤
│ Tipo Devolución:                    │
│ ⦿ Mismo Producto (Devuelto)         │
│ ○ Compró el Producto (Pagado)       │
│ ○ Reemplazó con Otro                │
│ ○ Otro                               │
│                                     │
│ Fecha Devolución: [📅 27/11/2025]  │
│                                     │
│ Items a Devolver:                   │
│ ┌─────────────────────────────────┐ │
│ │ Producto  │ Cant │ Notas       │ │
│ │ Laptop HP │  1   │             │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Notas: [TextArea]                   │
│                                     │
│ [Cancelar]  [Registrar Devolución] │
└─────────────────────────────────────┘
```

### 3. Tabla de Préstamos con Indicadores

**Columna de Estado Visual**:
```
┌────────────────────────────────────────────────────────┐
│ ID │ Fecha  │ Persona      │ Estado      │ Devolución │
├────┼────────┼──────────────┼─────────────┼────────────┤
│ 10 │ Nov 20 │ Juan Pérez   │ [VENCIDO]   │ Nov 25    │  ← Rojo
│ 11 │ Nov 25 │ María López  │ [PENDIENTE] │ Dic 05    │  ← Naranja
│ 12 │ Nov 10 │ Pedro Gómez  │ [DEVUELTO]  │ Nov 15    │  ← Verde
│ 13 │ Oct 28 │ Ana Martínez │ [PAGADO]    │ Nov 10    │  ← Azul
└────────────────────────────────────────────────────────┘
```

### 4. Opción en Ingresos

**CheckBox en Formulario de Ingresos**:
```
┌─────────────────────────────────────┐
│ Registrar Ingreso                   │
├─────────────────────────────────────┤
│ Fecha: [📅]                         │
│ Persona: [ComboBox ▼]              │
│                                     │
│ ☑ Es devolución de préstamo        │  ← NUEVO
│                                     │
│ Buscar préstamo:                    │  ← NUEVO
│ • Por código: [P-JA20251127-001] 🔍│
│ • Por lista: [ComboBox ▼]          │
│                                     │
│ Productos: [Tabla]                  │
│ Notas: [TextArea]                   │
│ [Cancelar]  [Guardar]              │
└─────────────────────────────────────┘

NOTA: Al ingresar el código, el sistema:
- Valida que exista
- Valida que esté pendiente
- Carga automáticamente los productos del préstamo
- Carga automáticamente la persona
```

### 5. Dashboard con Alertas

**Indicador de Préstamos Vencidos**:
```
┌──────────────────────────────────────┐
│ 🏠 Dashboard                         │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ ⚠️ ALERTAS                       │ │
│ │                                  │ │
│ │ 🔴 3 préstamo(s) vencido(s)     │ │  ← Clickeable
│ │    Ver detalles →               │ │
│ └──────────────────────────────────┘ │
│                                      │
│ Estadísticas:                        │
│ Total Productos: 150                 │
│ Préstamos Activos: 12                │  ← NUEVO
│ Préstamos Vencidos: 3                │  ← NUEVO
└──────────────────────────────────────┘
```

---

## 📊 Reportes y Estadísticas

### Reporte de Préstamos

```java
public class LoanReport {
    int totalLoans;        // Total de préstamos en el período
    int pending;           // Préstamos pendientes
    int returned;          // Préstamos devueltos
    int paid;              // Préstamos pagados
    int replaced;          // Préstamos reemplazados
    int overdue;           // Préstamos vencidos
    List<Transaction> loans;  // Detalle de préstamos
}
```

**Salida del Reporte**:
```
═══════════════════════════════════════════════
   REPORTE DE PRÉSTAMOS
   Período: 01/11/2025 - 30/11/2025
═══════════════════════════════════════════════

Total de Préstamos:        25
├─ Pendientes:            10  (40%)
├─ Devueltos:             12  (48%)
├─ Pagados:                2  (8%)
└─ Vencidos:               3  (12%)  ⚠️

───────────────────────────────────────────────
PRÉSTAMOS VENCIDOS (ATENCIÓN REQUERIDA)
───────────────────────────────────────────────
P-JP20251120-001 - Juan Pérez    - Esperado: 25/11 - 2 días
P-ML20251115-001 - María López   - Esperado: 20/11 - 7 días
P-PG20251110-002 - Pedro Gómez   - Esperado: 15/11 - 12 días

───────────────────────────────────────────────
PRÓXIMOS A VENCER (Siguientes 7 días)
───────────────────────────────────────────────
P-AM20251125-001 - Ana Martínez  - Esperado: 30/11 - 3 días
P-LG20251127-001 - Luis García   - Esperado: 02/12 - 5 días
```

---

## ✅ Validaciones Implementadas

### Al Crear Préstamo

- [x] Verificar que la persona existe y está activa
- [x] Verificar que los productos existen y están activos
- [x] Verificar que hay stock disponible
- [x] Fecha de devolución no puede ser anterior a hoy
- [x] Usuario tiene permiso PRESTAMOS.CREATE

### Al Registrar Devolución

- [x] Verificar que el préstamo existe
- [x] Verificar que el préstamo es del tipo 'loan'
- [x] Verificar que el préstamo está en status='pending'
- [x] Verificar que los productos a devolver coinciden (o permitir variación según tipo)
- [x] No se puede devolver un préstamo ya devuelto
- [x] Usuario tiene permiso INGRESOS.CREATE

### Reglas de Negocio

- [x] Un préstamo solo puede tener UNA devolución asociada
- [x] La devolución debe crear una transacción tipo 'in'
- [x] El stock se ajusta automáticamente según el tipo de devolución
- [x] Las auditorías registran todas las acciones
- [x] Los préstamos vencidos se identifican automáticamente

---

## 🔍 Casos de Uso Detallados

### Caso 1: Préstamo Simple y Devolución

**Usuario**: Operador  
**Escenario**: Préstamo de 50 electrodos + 5 canaletas a Juan Almendares

```
PASO 1: Crear Préstamo
├─ Persona: Juan Almendares (ID: 5)
├─ Código Generado: P-JA20251127-001
├─ Productos: 
│  • Electrodos (50 unidades)
│  • Canaletas (5 unidades)
├─ Fecha Esperada: 15/12/2025
├─ Notas: "Préstamo para proyecto de instalación eléctrica"
└─ Resultado:
   ├─ Transaction #100 creada
   ├─ type='loan'
   ├─ loan_code='P-JA20251127-001'
   ├─ loan_status='pending'
   ├─ Stock Electrodos: 200 → 150
   ├─ Stock Canaletas: 50 → 45
   ├─ Comprobante impreso con código
   └─ Auditoría: "create_loan"

PASO 2: Juan recibe comprobante
├─ Código visible: P-JA20251127-001
├─ Instrucción: "Presente este código al devolver"
└─ Fecha límite: 15/12/2025

PASO 3: Devolver Préstamo (10 días después)
├─ Juan presenta código: P-JA20251127-001
├─ Sistema busca préstamo por código
├─ Muestra préstamo pendiente
├─ Tipo: "Mismo Producto (Devuelto)"
├─ Fecha: 07/12/2025
├─ Productos devueltos:
│  • Electrodos (50 unidades)
│  • Canaletas (5 unidades)
├─ Notas: "Devuelto en buen estado, proyecto finalizado"
└─ Resultado:
   ├─ Transaction #101 creada (type='in')
   ├─ related_transaction_id=100
   ├─ Préstamo P-JA20251127-001: loan_status='returned'
   ├─ actual_return_date='07/12/2025'
   ├─ Stock Electrodos: 150 → 200
   ├─ Stock Canaletas: 45 → 50
   └─ Auditoría: "loan_return"
```

### Caso 2: Préstamo con Compra

**Usuario**: Supervisor  
**Escenario**: Préstamo que termina en venta

```
PASO 1: Crear Préstamo
├─ Persona: María López (ID: 8)
├─ Producto: Mouse Logitech (ID: 25) - Cantidad: 1
├─ Fecha Esperada: 05/12/2025
└─ Stock Mouse: 20 → 19

PASO 2: Registrar Compra (persona decide quedarse el producto)
├─ Seleccionar préstamo #102
├─ Tipo: "Compró el Producto (Pagado)"
├─ Fecha: 03/12/2025
├─ Notas: "Cliente pagó $25 por el mouse"
└─ Resultado:
   ├─ Transaction #103 creada (type='in')
   ├─ Préstamo #102: loan_status='paid'
   ├─ Stock Mouse: 19 (NO SE INCREMENTA - producto vendido)
   └─ Se puede registrar venta en sistema de facturación
```

### Caso 3: Préstamo Vencido

**Usuario**: Auditor  
**Escenario**: Seguimiento de préstamo no devuelto

```
SITUACIÓN:
├─ Préstamo #98 creado: 10/11/2025
├─ Fecha esperada: 20/11/2025
├─ Fecha actual: 27/11/2025
├─ loan_status='pending'
└─ Días de retraso: 7

ALERTAS:
├─ Dashboard muestra: "3 préstamo(s) vencido(s)"
├─ Tabla de préstamos: Estado [VENCIDO] en rojo
├─ Reporte incluye en sección "Préstamos Vencidos"
└─ Supervisor puede:
   ├─ Contactar a la persona
   ├─ Registrar devolución cuando ocurra
   └─ Aplicar políticas de la organización
```

---

## 📈 Impacto en el Sistema

### Antes de FASE 10

```
Préstamos:
├─ ✅ Se pueden crear
├─ ❌ No se sabe si fueron devueltos
├─ ❌ Stock queda desfasado
├─ ❌ No hay control de vencimientos
└─ ❌ No hay trazabilidad
```

### Después de FASE 10

```
Préstamos:
├─ ✅ Se pueden crear con fecha esperada
├─ ✅ Se pueden devolver con tipos específicos
├─ ✅ Stock se mantiene sincronizado
├─ ✅ Alertas automáticas de vencimientos
├─ ✅ Trazabilidad completa
├─ ✅ Reportes estadísticos
└─ ✅ Historial completo del ciclo
```

---

## 🚀 Beneficios Específicos

1. **Control Total**: Saber en todo momento qué está prestado y a quién
2. **Responsabilidad**: Fechas esperadas crean compromiso
3. **Alertas**: No se pierden productos por olvido
4. **Flexibilidad**: Múltiples formas de cerrar un préstamo
5. **Reportes**: Estadísticas para toma de decisiones
6. **Auditoría**: Trazabilidad completa de préstamos
7. **Stock Preciso**: El inventario refleja la realidad
8. **Profesionalismo**: Sistema completo de gestión

---

## ⚙️ Configuración Recomendada

### Políticas de Préstamos

```java
// Configurar en application.properties o base de datos

# Días máximos de préstamo por defecto
loan.default.max_days=30

# Enviar alertas X días antes del vencimiento
loan.alert.days_before=3

# Permitir préstamos vencidos sin bloqueo
loan.allow.overdue=true

# Marcar automáticamente como 'overdue' después de X días
loan.auto_mark.overdue_after_days=7
```

### Permisos Sugeridos

```
ROOT:
├─ PRESTAMOS.CREATE
├─ PRESTAMOS.READ
├─ PRESTAMOS.UPDATE
├─ PRESTAMOS.DELETE
├─ PRESTAMOS.RETURN
└─ PRESTAMOS.VIEW_ALL

SUPERVISOR:
├─ PRESTAMOS.CREATE
├─ PRESTAMOS.READ
├─ PRESTAMOS.RETURN
└─ PRESTAMOS.VIEW_ALL

OPERADOR:
├─ PRESTAMOS.CREATE
├─ PRESTAMOS.READ
└─ PRESTAMOS.RETURN (solo sus propios préstamos)
```

---

## 📚 Documentación Adicional

Después de implementar la FASE 10, actualizar:

1. **DATABASE.md**: Documentar nuevos campos y relaciones
2. **SECURITY.md**: Agregar nuevas acciones de auditoría
3. **FEATURES.md**: Describir funcionalidad de préstamos
4. **README.md**: Incluir capturas del nuevo módulo

---

## ✨ Conclusión

La FASE 10 completa el ciclo de vida de los préstamos, convirtiendo el sistema en una herramienta profesional y completa para gestión de inventario municipal. Permite rastrear cada producto desde que sale del inventario hasta que regresa, con total trazabilidad y control.

**Duración Estimada**: 3 días (Sprint 7)  
**Complejidad**: Media-Alta  
**Prioridad**: Alta (funcionalidad core del sistema)  
**Dependencias**: FASES 1-6 completadas

🚀 **¡Listo para implementar!**

