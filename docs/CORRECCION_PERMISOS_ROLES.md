# 🔐 Corrección de Permisos por Rol

**Fecha**: 27 de Noviembre, 2025  
**Archivo**: `20251114114107_seed_roles_and_permissions.sql`

---

## 🎯 Problema Detectado

La asignación de permisos anterior tenía varios problemas:
- ❌ ADMIN no tenía acceso a AUDITORÍA ni ROLES
- ❌ ADMIN no tenía permisos de RESTORE
- ❌ ADMIN no tenía permiso VIEW_DELETED
- ❌ SUPERVISOR no tenía permisos de DELETE
- ❌ SUPERVISOR no tenía permisos de RESTORE
- ❌ AUDITOR no tenía permiso VIEW_DELETED
- ❌ Había inserciones duplicadas

---

## ✅ Solución Implementada

### 1. ROL ROOT (ID: 1)
**Nivel de Acceso**: ⭐⭐⭐⭐⭐ Total

```sql
-- Acceso TOTAL a todos los permisos sin excepción
SELECT 1, id FROM permisos;
```

**Permisos**:
- ✅ Todos los módulos (CRUD completo)
- ✅ DESTRUCTIVE_OPERATIONS (eliminación física)
- ✅ VIEW_DELETED (ver eliminados)
- ✅ RESTORE (restaurar en todos los módulos)
- ✅ Gestión de roles y usuarios
- ✅ Auditoría completa
- ✅ Configuración del sistema

---

### 2. ROL ADMIN (ID: 2)
**Nivel de Acceso**: ⭐⭐⭐⭐ Casi Total

```sql
-- Acceso completo EXCEPTO operaciones destructivas
WHERE modulo != 'SISTEMA' OR accion != 'DESTRUCTIVE_OPERATIONS';
```

**Permisos Incluidos**:
- ✅ Dashboard (READ)
- ✅ Inventario (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Ingresos (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Egresos (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Préstamos (CREATE, READ, UPDATE, DELETE, APPROVE, RESTORE)
- ✅ Personas (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Usuarios (CREATE, READ, UPDATE, DELETE, CHANGE_ROLE, RESTORE)
- ✅ Roles (CREATE, READ, UPDATE, DELETE, ASSIGN_PERMISSIONS, RESTORE)
- ✅ Auditoría (READ, EXPORT)
- ✅ Reportes (READ, GENERATE, EXPORT)
- ✅ Configuración (READ, UPDATE)
- ✅ Sistema (VIEW_DELETED)

**Permisos NO Incluidos**:
- ❌ SISTEMA.DESTRUCTIVE_OPERATIONS (solo ROOT)

**Diferencia con ROOT**:
- Solo ROOT puede hacer eliminaciones físicas permanentes
- ADMIN puede hacer soft delete pero no hard delete

---

### 3. ROL AUDITOR (ID: 3)
**Nivel de Acceso**: ⭐⭐ Solo Lectura + Auditoría

```sql
-- Solo lectura de auditoría, reportes y datos
WHERE (modulo = 'AUDITORIA' AND accion IN ('READ', 'EXPORT'))
   OR (modulo = 'REPORTES' AND accion IN ('READ', 'GENERATE', 'EXPORT'))
   OR (modulo = 'DASHBOARD' AND accion = 'READ')
   OR (modulo = 'SISTEMA' AND accion = 'VIEW_DELETED')
   OR (modulo IN ('INVENTARIO', 'INGRESOS', 'EGRESOS', 'PRESTAMOS', 'PERSONAS', 'USUARIOS') 
       AND accion = 'READ')
```

**Permisos Incluidos**:
- ✅ Dashboard (READ)
- ✅ Auditoría (READ, EXPORT)
- ✅ Reportes (READ, GENERATE, EXPORT)
- ✅ Inventario (READ)
- ✅ Ingresos (READ)
- ✅ Egresos (READ)
- ✅ Préstamos (READ)
- ✅ Personas (READ)
- ✅ Usuarios (READ)
- ✅ Sistema (VIEW_DELETED)

**Permisos NO Incluidos**:
- ❌ CREATE, UPDATE, DELETE en cualquier módulo
- ❌ RESTORE
- ❌ Gestión de roles
- ❌ Configuración

**Propósito**: Auditar el sistema sin poder modificar nada

---

### 4. ROL SUPERVISOR (ID: 4)
**Nivel de Acceso**: ⭐⭐⭐ Gestión Operativa Completa

```sql
-- Gestión completa de operaciones + restauración
WHERE (modulo IN ('DASHBOARD', 'INVENTARIO', 'INGRESOS', 'EGRESOS', 'PRESTAMOS', 'PERSONAS', 'REPORTES')
       AND accion IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'APPROVE', 'GENERATE', 'EXPORT'))
   OR (modulo IN ('INVENTARIO', 'INGRESOS', 'EGRESOS', 'PRESTAMOS', 'PERSONAS') 
       AND accion = 'RESTORE')
   OR (modulo = 'SISTEMA' AND accion = 'VIEW_DELETED')
```

**Permisos Incluidos**:
- ✅ Dashboard (READ)
- ✅ Inventario (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Ingresos (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Egresos (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Préstamos (CREATE, READ, UPDATE, DELETE, APPROVE, RESTORE)
- ✅ Personas (CREATE, READ, UPDATE, DELETE, RESTORE)
- ✅ Reportes (READ, GENERATE, EXPORT)
- ✅ Sistema (VIEW_DELETED)

**Permisos NO Incluidos**:
- ❌ Usuarios (no puede gestionar usuarios)
- ❌ Roles (no puede gestionar roles)
- ❌ Auditoría (no tiene acceso)
- ❌ Configuración (no puede cambiar config)
- ❌ DESTRUCTIVE_OPERATIONS

**Propósito**: Supervisor de operaciones diarias

---

### 5. ROL OPERADOR (ID: 5)
**Nivel de Acceso**: ⭐⭐ Operaciones Básicas

```sql
-- Solo CREATE, READ, UPDATE (sin DELETE ni RESTORE)
WHERE modulo IN ('DASHBOARD', 'INVENTARIO', 'INGRESOS', 'EGRESOS', 'PRESTAMOS', 'PERSONAS')
  AND accion IN ('CREATE', 'READ', 'UPDATE')
```

**Permisos Incluidos**:
- ✅ Dashboard (READ)
- ✅ Inventario (CREATE, READ, UPDATE)
- ✅ Ingresos (CREATE, READ, UPDATE)
- ✅ Egresos (CREATE, READ, UPDATE)
- ✅ Préstamos (CREATE, READ, UPDATE)
- ✅ Personas (CREATE, READ, UPDATE)

**Permisos NO Incluidos**:
- ❌ DELETE (no puede eliminar nada)
- ❌ RESTORE (no puede restaurar)
- ❌ APPROVE (no puede aprobar préstamos)
- ❌ Reportes
- ❌ Usuarios, Roles, Auditoría, Configuración
- ❌ VIEW_DELETED

**Propósito**: Operador de tareas diarias sin permisos de eliminación

---

### 6. ROL CONSULTOR (ID: 6)
**Nivel de Acceso**: ⭐ Solo Lectura

```sql
-- Solo lectura y generación de reportes
WHERE accion IN ('READ', 'GENERATE', 'EXPORT')
  AND modulo NOT IN ('AUDITORIA', 'ROLES', 'USUARIOS', 'CONFIGURACION')
```

**Permisos Incluidos**:
- ✅ Dashboard (READ)
- ✅ Inventario (READ)
- ✅ Ingresos (READ)
- ✅ Egresos (READ)
- ✅ Préstamos (READ)
- ✅ Personas (READ)
- ✅ Reportes (READ, GENERATE, EXPORT)

**Permisos NO Incluidos**:
- ❌ CREATE, UPDATE, DELETE
- ❌ RESTORE
- ❌ Usuarios, Roles
- ❌ Auditoría
- ❌ Configuración
- ❌ VIEW_DELETED

**Propósito**: Consulta y reportes sin modificar nada

---

## 📊 Matriz de Permisos por Rol

| Módulo | ROOT | ADMIN | AUDITOR | SUPERVISOR | OPERADOR | CONSULTOR |
|--------|------|-------|---------|------------|----------|-----------|
| **Dashboard** | ✅ | ✅ | ✅ READ | ✅ READ | ✅ READ | ✅ READ |
| **Inventario** | ✅ | ✅ + RESTORE | ✅ READ | ✅ + RESTORE | ✅ CRU | ✅ READ |
| **Ingresos** | ✅ | ✅ + RESTORE | ✅ READ | ✅ + RESTORE | ✅ CRU | ✅ READ |
| **Egresos** | ✅ | ✅ + RESTORE | ✅ READ | ✅ + RESTORE | ✅ CRU | ✅ READ |
| **Préstamos** | ✅ | ✅ + RESTORE + APPROVE | ✅ READ | ✅ + RESTORE + APPROVE | ✅ CRU | ✅ READ |
| **Personas** | ✅ | ✅ + RESTORE | ✅ READ | ✅ + RESTORE | ✅ CRU | ✅ READ |
| **Usuarios** | ✅ | ✅ + RESTORE | ✅ READ | ❌ | ❌ | ❌ |
| **Roles** | ✅ | ✅ + RESTORE | ❌ | ❌ | ❌ | ❌ |
| **Auditoría** | ✅ | ✅ | ✅ READ + EXPORT | ❌ | ❌ | ❌ |
| **Reportes** | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ READ |
| **Configuración** | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **VIEW_DELETED** | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| **DESTRUCTIVE_OPS** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

**Leyenda**:
- ✅ = Acceso completo (CRUD)
- CRU = Create, Read, Update (sin Delete)
- READ = Solo lectura
- ❌ = Sin acceso

---

## 🔑 Cambios Principales

### ADMIN
**Antes**:
- ❌ No tenía acceso a AUDITORÍA
- ❌ No tenía acceso a ROLES
- ❌ No tenía RESTORE
- ❌ No tenía VIEW_DELETED

**Después**:
- ✅ Acceso completo a AUDITORÍA
- ✅ Acceso completo a ROLES
- ✅ Puede RESTORE en todos los módulos
- ✅ Puede VIEW_DELETED
- ✅ Solo le falta DESTRUCTIVE_OPERATIONS (exclusivo de ROOT)

### SUPERVISOR
**Antes**:
- ❌ No tenía DELETE
- ❌ No tenía RESTORE
- ❌ No tenía VIEW_DELETED

**Después**:
- ✅ Puede DELETE en módulos operativos
- ✅ Puede RESTORE en módulos operativos
- ✅ Puede VIEW_DELETED

### AUDITOR
**Antes**:
- ❌ No tenía VIEW_DELETED
- ❌ No tenía READ en módulos operativos

**Después**:
- ✅ Puede VIEW_DELETED
- ✅ Tiene READ en todos los módulos operativos

---

## ✅ Verificación

Puedes verificar los permisos de cada rol con:

```sql
-- Ver permisos de ADMIN
SELECT p.modulo, p.accion, p.descripcion
FROM rol_permisos rp
JOIN permisos p ON rp.permiso_id = p.id
WHERE rp.rol_id = 2
ORDER BY p.modulo, p.accion;

-- Ver permisos de SUPERVISOR
SELECT p.modulo, p.accion, p.descripcion
FROM rol_permisos rp
JOIN permisos p ON rp.permiso_id = p.id
WHERE rp.rol_id = 4
ORDER BY p.modulo, p.accion;
```

---

## 🎯 Recomendaciones de Uso

### ROOT (ID: 1)
**Asignar a**: Administrador del sistema principal  
**Uso**: Mantenimiento, configuración crítica, eliminaciones físicas

### ADMIN (ID: 2)
**Asignar a**: Administradores de la institución  
**Uso**: Gestión completa del sistema excepto eliminaciones físicas

### AUDITOR (ID: 3)
**Asignar a**: Personal de auditoría  
**Uso**: Revisar auditorías, generar reportes, consultar datos

### SUPERVISOR (ID: 4)
**Asignar a**: Jefes de departamento  
**Uso**: Supervisar operaciones diarias, aprobar préstamos, gestionar inventario

### OPERADOR (ID: 5)
**Asignar a**: Personal operativo  
**Uso**: Registro de transacciones diarias, actualización de inventario

### CONSULTOR (ID: 6)
**Asignar a**: Personal externo o visitantes  
**Uso**: Solo consulta de información y generación de reportes

---

## 📝 Notas Importantes

1. **ROOT es único**: Solo debe haber un usuario ROOT
2. **ADMIN es poderoso**: Puede gestionar usuarios y roles, úsalo con cuidado
3. **DESTRUCTIVE_OPERATIONS**: Solo ROOT puede eliminar físicamente
4. **VIEW_DELETED**: ROOT, ADMIN, AUDITOR y SUPERVISOR pueden ver eliminados
5. **RESTORE**: ROOT, ADMIN y SUPERVISOR pueden restaurar
6. **Usuario SYSTEM**: Sin permisos, solo para operaciones automáticas

---

**Estado**: ✅ Permisos corregidos y optimizados  
**Fecha**: 27 de Noviembre, 2025  
**Migración**: `20251114114107_seed_roles_and_permissions.sql`

