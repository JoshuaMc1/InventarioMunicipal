# 🗄️ Base de Datos - Sistema de Inventario Municipal

## 📊 Estructura de la Base de Datos

### Motor de Base de Datos
- **MySQL 8.0+**
- **Charset:** utf8mb4
- **Collation:** utf8mb4_unicode_ci

---

## 📋 Tablas Principales

### 1. `roles`
Tabla de roles del sistema con permisos asociados.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único del rol |
| `nombre` | VARCHAR(50) | Nombre del rol (ROOT, ADMIN, etc.) |
| `descripcion` | TEXT | Descripción del rol |
| `activo` | TINYINT(1) | Estado del rol (1=activo, 0=inactivo) |
| `created_at` | TIMESTAMP | Fecha de creación |

**Relaciones:**
- Tiene muchos: `users` (a través de `rol_id`)
- Tiene muchos: `permisos` (a través de `rol_permisos`)

---

### 2. `permisos`
Tabla de permisos granulares del sistema.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único del permiso |
| `modulo` | VARCHAR(50) | Módulo (INVENTARIO, USUARIOS, etc.) |
| `accion` | VARCHAR(50) | Acción (CREATE, READ, UPDATE, DELETE) |
| `descripcion` | TEXT | Descripción del permiso |
| `created_at` | TIMESTAMP | Fecha de creación |

**Relaciones:**
- Pertenece a muchos: `roles` (a través de `rol_permisos`)

---

### 3. `rol_permisos`
Tabla pivote para la relación many-to-many entre roles y permisos.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único |
| `rol_id` | INT | FK a `roles.id` |
| `permiso_id` | INT | FK a `permisos.id` |
| `created_at` | TIMESTAMP | Fecha de asignación |

---

### 4. `users`
Tabla de usuarios del sistema.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único del usuario |
| `name` | VARCHAR(100) | Nombre completo |
| `username` | VARCHAR(50) | Nombre de usuario (único) |
| `password` | VARCHAR(255) | Contraseña hasheada (BCrypt) |
| `rol_id` | INT | FK a `roles.id` |
| `must_change_password` | TINYINT(1) | Requiere cambio de contraseña |
| `created_at` | TIMESTAMP | Fecha de creación |

**Índices:**
- UNIQUE: `username`
- INDEX: `rol_id`

**Relaciones:**
- Pertenece a: `roles`
- Tiene muchos: `transactions` (como usuario que registra)
- Tiene muchos: `audit` (como usuario que realiza acción)

---

### 5. `people`
Tabla de personas (para préstamos y registros).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único de la persona |
| `dni` | VARCHAR(20) | DNI o identificación (único) |
| `first_name` | VARCHAR(50) | Nombre(s) |
| `last_name` | VARCHAR(50) | Apellido(s) |
| `phone` | VARCHAR(20) | Teléfono |
| `internal_code` | VARCHAR(50) | Código interno (opcional) |
| `created_at` | TIMESTAMP | Fecha de creación |

**Índices:**
- UNIQUE: `dni`
- INDEX: `internal_code`

**Relaciones:**
- Tiene muchos: `transactions` (préstamos y transacciones)

---

### 6. `products`
Tabla de productos del inventario.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único del producto |
| `name` | VARCHAR(100) | Nombre del producto |
| `description` | TEXT | Descripción detallada |
| `unit` | VARCHAR(20) | Unidad de medida |
| `initial_stock` | DECIMAL(10,2) | Stock inicial |
| `current_stock` | DECIMAL(10,2) | Stock actual |
| `min_stock` | DECIMAL(10,2) | Stock mínimo (alerta) |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

**Índices:**
- INDEX: `name`
- INDEX: `current_stock`

**Relaciones:**
- Tiene muchos: `transactions`

---

### 7. `transactions`
Tabla de transacciones (ingresos, egresos, préstamos).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único de la transacción |
| `product_id` | INT | FK a `products.id` |
| `type` | ENUM | Tipo: 'IN', 'OUT', 'LOAN' |
| `quantity` | DECIMAL(10,2) | Cantidad |
| `date` | DATE | Fecha de la transacción |
| `person_id` | INT | FK a `people.id` (nullable) |
| `user_id` | INT | FK a `users.id` (quien registró) |
| `notes` | TEXT | Notas adicionales |
| `created_at` | TIMESTAMP | Fecha de registro |

**Índices:**
- INDEX: `product_id`
- INDEX: `type`
- INDEX: `date`
- INDEX: `person_id`
- INDEX: `user_id`

**Relaciones:**
- Pertenece a: `products`
- Pertenece a: `users`
- Pertenece a: `people` (opcional)

---

### 8. `audit`
Tabla de auditoría de todas las acciones.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único del registro |
| `usuario_id` | INT | FK a `users.id` |
| `accion` | VARCHAR(50) | Acción realizada |
| `tabla_afectada` | VARCHAR(50) | Tabla afectada |
| `registro_afectado_id` | INT | ID del registro afectado |
| `detalles` | TEXT | Detalles de la acción |
| `fecha` | TIMESTAMP | Fecha y hora |

**Índices:**
- INDEX: `usuario_id`
- INDEX: `accion`
- INDEX: `tabla_afectada`
- INDEX: `fecha`

**Relaciones:**
- Pertenece a: `users`

---

### 9. `backups`
Tabla de historial de backups de la base de datos.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | INT | ID único del registro |
| `filename` | VARCHAR(255) | Nombre del archivo de backup |
| `filepath` | VARCHAR(512) | Ruta completa del archivo |
| `size_bytes` | BIGINT | Tamaño del archivo en bytes |
| `usuario_id` | INT | FK a `users.id` (quien creó el backup) |
| `created_at` | TIMESTAMP | Fecha de creación |
| `status` | ENUM('SUCCESS', 'FAILED') | Estado del backup |
| `error_message` | TEXT | Mensaje de error (si falló) |

**Índices:**
- INDEX: `created_at`
- INDEX: `usuario_id`
- INDEX: `status`

**Relaciones:**
- Pertenece a: `users`

---

## 🔄 Migraciones

### Sistema de Migraciones
Las migraciones se ejecutan automáticamente en orden secuencial usando el patrón:
```
YYYYMMDDHHMMSS_descripcion.sql
```

### Tabla de Control
```sql
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(255) PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Migraciones Disponibles

| Versión | Archivo | Descripción |
|---------|---------|-------------|
| 20251114112627 | create_roles_table.sql | Crea tabla de roles |
| 20251114112628 | create_user_table.sql | Crea tabla de usuarios |
| 20251114112848 | create_people_table.sql | Crea tabla de personas |
| 20251114112911 | create_product_table.sql | Crea tabla de productos |
| 20251114113444 | create_transaction_table.sql | Crea tabla de transacciones |
| 20251114114102 | create_audit_table.sql | Crea tabla de auditoría |
| 20251114114104 | create_indexes_table.sql | Crea índices optimizados |
| 20251114114105 | create_permissions_table.sql | Crea tabla de permisos |
| 20251114114106 | create_role_permissions_table.sql | Crea tabla pivote |
| 20251114114107 | seed_roles_and_permissions.sql | Inserta datos iniciales |
| 20251120000001 | create_backups_table.sql | Crea tabla de backups |
| 20251120000002 | add_backup_permissions.sql | Agrega permisos de backup |

---

## 🔗 Diagrama de Relaciones

```
┌─────────────┐       ┌──────────────┐       ┌─────────────┐
│    roles    │◄──────│ rol_permisos │──────►│  permisos   │
└──────┬──────┘       └──────────────┘       └─────────────┘
       │
       │ 1:N
       ▼
┌─────────────┐
│    users    │───────┐
└──────┬──────┘       │
       │              │ 1:N
       │ 1:N          ▼
       ▼         ┌─────────────┐
┌─────────────┐  │   backups   │
│transactions │  └─────────────┘
└──────┬──────┘ 
       │      
       │ N:1  
       ▼      
┌─────────────┐
│  products   │
└──────┬──────┘
       │
       │ N:1
       ▼
┌─────────────┐
│   people    │
└─────────────┘

┌─────────────┐
│    audit    │──────► (Referencia a users)
└─────────────┘
```

---

## 📝 Consultas Útiles

### Ver Roles con Cantidad de Usuarios
```sql
SELECT r.nombre, r.descripcion, COUNT(u.id) as usuarios
FROM roles r
LEFT JOIN users u ON r.id = u.rol_id
GROUP BY r.id, r.nombre, r.descripcion;
```

### Ver Permisos de un Rol
```sql
SELECT p.modulo, p.accion, p.descripcion
FROM permisos p
JOIN rol_permisos rp ON p.id = rp.permiso_id
JOIN roles r ON r.id = rp.rol_id
WHERE r.nombre = 'ADMIN'
ORDER BY p.modulo, p.accion;
```

### Stock Bajo de Productos
```sql
SELECT id, name, current_stock, min_stock
FROM products
WHERE current_stock <= min_stock
ORDER BY current_stock ASC;
```

### Transacciones del Día
```sql
SELECT t.*, p.name as product_name, u.username
FROM transactions t
JOIN products p ON t.product_id = p.id
JOIN users u ON t.user_id = u.id
WHERE DATE(t.created_at) = CURDATE()
ORDER BY t.created_at DESC;
```

### Auditoría por Usuario
```sql
SELECT a.*, u.username
FROM audit a
JOIN users u ON a.usuario_id = u.id
WHERE u.username = 'admin'
ORDER BY a.fecha DESC
LIMIT 50;
```

---

## 🔒 Consideraciones de Seguridad

### Contraseñas
- Hasheadas con **BCrypt** (factor 10)
- Nunca almacenar en texto plano
- Validación de complejidad en aplicación

### Credenciales de Conexión
- Cifradas con **AES-256**
- Almacenadas en: `%USERPROFILE%\.inventario_municipal\mysql.config`
- No incluir en código fuente

### Permisos MySQL
```sql
-- Usuario con permisos mínimos necesarios
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
ON inventario_municipal.*
TO 'inventario_user'@'localhost'
IDENTIFIED BY 'contraseña_segura';
```

---

## 🔧 Backup y Restauración

### Backup Completo
```bash
mysqldump -u root -p inventario_municipal > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restaurar Backup
```bash
mysql -u root -p inventario_municipal < backup_20251120_020000.sql
```

### Backup Programado (Windows)
```batch
@echo off
set MYSQL_USER=root
set MYSQL_PASS=tu_contraseña
set BACKUP_DIR=C:\Backups\InventarioMunicipal
set TIMESTAMP=%DATE:~-4,4%%DATE:~-7,2%%DATE:~-10,2%_%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%

mysqldump -u %MYSQL_USER% -p%MYSQL_PASS% inventario_municipal > %BACKUP_DIR%\backup_%TIMESTAMP%.sql
```

---

## 📊 Optimización

### Índices Recomendados
Ya implementados en la migración `create_indexes_table.sql`:

- `products`: `name`, `current_stock`
- `transactions`: `product_id`, `type`, `date`, `person_id`, `user_id`
- `audit`: `usuario_id`, `accion`, `tabla_afectada`, `fecha`
- `people`: `dni`, `internal_code`
- `users`: `username`, `rol_id`

### Mantenimiento
```sql
-- Analizar tablas
ANALYZE TABLE products, transactions, audit;

-- Optimizar tablas
OPTIMIZE TABLE products, transactions, audit;

-- Ver uso de índices
SHOW INDEX FROM transactions;
```

---

**Documentación actualizada:** 20 de Noviembre de 2025  
**Versión de BD:** 1.0  
**Compatible con:** MySQL 8.0+

