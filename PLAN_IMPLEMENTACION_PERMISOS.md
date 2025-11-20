# 📋 PLAN DE IMPLEMENTACIÓN - CONTROL DE ACCESO BASADO EN PERMISOS

## 🎯 Objetivo
Implementar el control de acceso basado en permisos en todo el sistema, habilitando/deshabilitando y mostrando/ocultando elementos según los permisos del usuario.

---

## 📊 FASES DE IMPLEMENTACIÓN

### ✅ FASE 0: Preparación Base (COMPLETADO)
**Estado:** ✅ **COMPLETADO**

- [x] Sistema de roles y permisos creado
- [x] Tablas de base de datos (roles, permisos, rol_permisos)
- [x] 6 roles predefinidos (ROOT, ADMIN, AUDITOR, SUPERVISOR, OPERADOR, CONSULTOR)
- [x] 49 permisos distribuidos en 11 módulos
- [x] Modelo Rol.java con métodos de gestión
- [x] Modelo Permiso.java con métodos de gestión
- [x] User.java con métodos tienePermiso() y getRol()
- [x] RolesController con interfaz de gestión

---

### 🔵 FASE 1: Infraestructura y Módulos Base
**Módulos:** MainController + HomeController + InventoryController  
**Estado:** ⏳ **PENDIENTE**

#### 1.1 MainController (Menú Principal)
- [ ] Ocultar/mostrar botones del menú según permisos
- [ ] Deshabilitar navegación a módulos sin permiso
- [ ] Control de acceso a módulos principales
- [ ] **Permisos a verificar:**
  - `DASHBOARD.READ` - Acceso al dashboard
  - `INVENTARIO.READ` - Ver botón de inventario
  - `USUARIOS.READ` - Ver botón de usuarios
  - `ROLES.READ` - Ver botón de roles (solo ROOT)
  - `AUDITORIA.READ` - Ver botón de auditoría
  - `REPORTES.READ` - Ver botón de reportes
  - `PERSONAS.READ` - Ver botón de personas
  - `INGRESOS.READ` - Ver botón de ingresos
  - `EGRESOS.READ` - Ver botón de egresos
  - `PRESTAMOS.READ` - Ver botón de préstamos

#### 1.2 HomeController (Dashboard)
- [ ] Mostrar solo estadísticas permitidas
- [ ] Filtrar gráficos según permisos
- [ ] Ocultar widgets sin acceso
- [ ] **Permisos a verificar:**
  - `DASHBOARD.READ` - Acceso completo al dashboard
  - `INVENTARIO.READ` - Ver estadísticas de inventario
  - `REPORTES.READ` - Ver gráficos y reportes rápidos

#### 1.3 InventoryController (Inventario)
- [ ] Botón "Nuevo" según `INVENTARIO.CREATE`
- [ ] Botón "Editar" según `INVENTARIO.UPDATE`
- [ ] Botón "Eliminar" según `INVENTARIO.DELETE`
- [ ] Tabla en modo solo lectura si no tiene UPDATE
- [ ] **Permisos a verificar:**
  - `INVENTARIO.CREATE` - Crear productos
  - `INVENTARIO.READ` - Ver productos
  - `INVENTARIO.UPDATE` - Editar productos
  - `INVENTARIO.DELETE` - Eliminar productos

---

### 🟢 FASE 2: Módulos de Transacciones
**Módulos:** IncomesController + OutcomesController + LoansController  
**Estado:** ⏳ **PENDIENTE**

#### 2.1 IncomesController (Ingresos)
- [ ] Botón "Nuevo Ingreso" según `INGRESOS.CREATE`
- [ ] Botón "Editar" según `INGRESOS.UPDATE`
- [ ] Botón "Eliminar" según `INGRESOS.DELETE`
- [ ] Validar permisos antes de guardar
- [ ] **Permisos a verificar:**
  - `INGRESOS.CREATE` - Crear ingresos
  - `INGRESOS.READ` - Ver ingresos
  - `INGRESOS.UPDATE` - Editar ingresos
  - `INGRESOS.DELETE` - Eliminar ingresos

#### 2.2 OutcomesController (Egresos)
- [ ] Botón "Nuevo Egreso" según `EGRESOS.CREATE`
- [ ] Botón "Editar" según `EGRESOS.UPDATE`
- [ ] Botón "Eliminar" según `EGRESOS.DELETE`
- [ ] Validar permisos antes de guardar
- [ ] **Permisos a verificar:**
  - `EGRESOS.CREATE` - Crear egresos
  - `EGRESOS.READ` - Ver egresos
  - `EGRESOS.UPDATE` - Editar egresos
  - `EGRESOS.DELETE` - Eliminar egresos

#### 2.3 LoansController (Préstamos)
- [ ] Botón "Nuevo Préstamo" según `PRESTAMOS.CREATE`
- [ ] Botón "Editar" según `PRESTAMOS.UPDATE`
- [ ] Botón "Eliminar" según `PRESTAMOS.DELETE`
- [ ] Botón "Aprobar" según `PRESTAMOS.APPROVE`
- [ ] Validar permisos antes de guardar
- [ ] **Permisos a verificar:**
  - `PRESTAMOS.CREATE` - Crear préstamos
  - `PRESTAMOS.READ` - Ver préstamos
  - `PRESTAMOS.UPDATE` - Editar préstamos
  - `PRESTAMOS.DELETE` - Eliminar préstamos
  - `PRESTAMOS.APPROVE` - Aprobar préstamos

---

### 🟡 FASE 3: Módulos de Gestión
**Módulos:** PeopleController + UsersController + AuditController  
**Estado:** ⏳ **PENDIENTE**

#### 3.1 PeopleController (Personas)
- [ ] Botón "Nueva Persona" según `PERSONAS.CREATE`
- [ ] Botón "Editar" según `PERSONAS.UPDATE`
- [ ] Botón "Eliminar" según `PERSONAS.DELETE`
- [ ] Tabla en modo solo lectura si no tiene UPDATE
- [ ] **Permisos a verificar:**
  - `PERSONAS.CREATE` - Crear personas
  - `PERSONAS.READ` - Ver personas
  - `PERSONAS.UPDATE` - Editar personas
  - `PERSONAS.DELETE` - Eliminar personas

#### 3.2 UsersController (Usuarios)
- [ ] Botón "Nuevo Usuario" según `USUARIOS.CREATE`
- [ ] Botón "Editar" según `USUARIOS.UPDATE`
- [ ] Botón "Eliminar" según `USUARIOS.DELETE`
- [ ] Selector de roles según `USUARIOS.CHANGE_ROLE`
- [ ] Solo ADMIN y ROOT pueden gestionar usuarios
- [ ] **Permisos a verificar:**
  - `USUARIOS.CREATE` - Crear usuarios
  - `USUARIOS.READ` - Ver usuarios
  - `USUARIOS.UPDATE` - Editar usuarios
  - `USUARIOS.DELETE` - Eliminar usuarios
  - `USUARIOS.CHANGE_ROLE` - Cambiar rol de usuarios

#### 3.3 AuditController (Auditoría)
- [ ] Acceso completo solo con `AUDITORIA.READ`
- [ ] Botón "Exportar" según `AUDITORIA.EXPORT`
- [ ] Filtros avanzados solo para AUDITOR y ROOT
- [ ] **Permisos a verificar:**
  - `AUDITORIA.READ` - Ver auditorías
  - `AUDITORIA.EXPORT` - Exportar registros

---

### 🟣 FASE 4: Módulos de Reportes y Configuración
**Módulos:** ReportsController + ProfileController + RolesController  
**Estado:** ⏳ **PENDIENTE**

#### 4.1 ReportsController (Reportes)
- [ ] Lista de reportes según permisos
- [ ] Botón "Generar" según `REPORTES.GENERATE`
- [ ] Botón "Exportar" según `REPORTES.EXPORT`
- [ ] Filtrar tipos de reportes por permisos
- [ ] **Permisos a verificar:**
  - `REPORTES.READ` - Ver reportes
  - `REPORTES.GENERATE` - Generar reportes
  - `REPORTES.EXPORT` - Exportar reportes

#### 4.2 ProfileController (Perfil)
- [ ] Permitir cambio de nombre y contraseña
- [ ] Mostrar rol actual (sin poder cambiarlo)
- [ ] Información de permisos asignados
- [ ] **No requiere permisos especiales** (todos pueden ver su perfil)

#### 4.3 RolesController (Roles) - YA IMPLEMENTADO
- [ ] ✅ Ya está protegido (solo ROOT)
- [ ] ✅ No se puede editar/eliminar rol ROOT
- [ ] ✅ No se puede eliminar rol con usuarios asignados
- [ ] Verificar que funciona correctamente
- [ ] **Permisos a verificar:**
  - `ROLES.READ` - Ver roles
  - `ROLES.CREATE` - Crear roles
  - `ROLES.UPDATE` - Editar roles
  - `ROLES.DELETE` - Eliminar roles
  - `ROLES.ASSIGN_PERMISSIONS` - Gestionar permisos

---

## 🛠️ COMPONENTES AUXILIARES A CREAR

### PermissionHelper.java
Clase utilitaria para verificación de permisos:
```java
public class PermissionHelper {
    public static boolean canAccess(String modulo, String accion);
    public static void configureButton(Button btn, String modulo, String accion);
    public static void hideIfNoPermission(Node node, String modulo, String accion);
    public static void disableIfNoPermission(Node node, String modulo, String accion);
}
```

### AccessDenied.java
Componente para mostrar mensajes de acceso denegado:
```java
public class AccessDenied {
    public static void show(String modulo, String accion);
    public static void showAndLog(String modulo, String accion);
}
```

---

## 📝 CHECKLIST POR MÓDULO

Para cada controlador, seguir estos pasos:

### 1. Preparación
- [ ] Importar `User`, `Rol`, `Session`
- [ ] Obtener usuario actual en `initialize()`
- [ ] Crear métodos helper para verificar permisos

### 2. Botones de Acción
- [ ] Configurar visibilidad según permisos
- [ ] Agregar tooltips explicativos
- [ ] Deshabilitar en lugar de ocultar (opcional)

### 3. Acciones en Métodos
- [ ] Validar permiso antes de ejecutar
- [ ] Mostrar mensaje de acceso denegado
- [ ] Registrar intentos no autorizados en auditoría

### 4. Tabla/Vista
- [ ] Deshabilitar edición si no tiene UPDATE
- [ ] Ocultar columnas según permisos
- [ ] Mostrar indicador de solo lectura

### 5. Testing
- [ ] Probar con rol ROOT (todos los permisos)
- [ ] Probar con rol OPERADOR (permisos limitados)
- [ ] Probar con rol CONSULTOR (solo lectura)
- [ ] Verificar mensajes de error

---

## 🎯 PROGRESO GENERAL

| Fase | Módulos | Estado | Completado |
|------|---------|--------|------------|
| **FASE 0** | Preparación Base | ✅ COMPLETADO | 100% |
| **FASE 1** | MainController, HomeController, InventoryController | ⏳ PENDIENTE | 0% |
| **FASE 2** | IncomesController, OutcomesController, LoansController | ⏳ PENDIENTE | 0% |
| **FASE 3** | PeopleController, UsersController, AuditController | ⏳ PENDIENTE | 0% |
| **FASE 4** | ReportsController, ProfileController, RolesController | ⏳ PENDIENTE | 0% |

**Progreso Total:** 20% (1/5 fases completadas)

---

## 📚 NOTAS IMPORTANTES

1. **Prioridad de Implementación:**
   - Fase 1 es crítica (controla acceso a todos los módulos)
   - Fases 2-3 son de alta prioridad (operaciones diarias)
   - Fase 4 es de media prioridad (administrativo)

2. **Retrocompatibilidad:**
   - Los métodos `isRoot()`, `isAdmin()`, `isOperator()` del modelo User seguirán funcionando
   - Pero se priorizará el uso de `tienePermiso(modulo, accion)`

3. **Testing:**
   - Probar cada fase antes de continuar con la siguiente
   - Verificar con diferentes roles
   - Documentar casos edge

4. **Auditoría:**
   - Registrar intentos de acceso denegado
   - Logging de cambios importantes
   - Mantener trazabilidad

---

## 🚀 PRÓXIMO PASO

Comenzar con **FASE 1 - Módulo MainController**

Este es el más crítico porque controla el acceso a todos los demás módulos del sistema.

---

**Fecha de Creación:** 20 de Noviembre de 2025  
**Última Actualización:** 20 de Noviembre de 2025  
**Estado:** 📋 PLANIFICACIÓN COMPLETADA - LISTO PARA IMPLEMENTAR

