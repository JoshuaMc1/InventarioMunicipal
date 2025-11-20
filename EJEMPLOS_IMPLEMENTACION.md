# 📘 Guía de Implementación - Control de Acceso y Seguridad

## Ejemplos de Código para los Próximos Pasos

---

## 1️⃣ Control de Acceso en MainController

### Ocultar Botones Según Permisos

```java
@FXML
public void initialize() {
    // ...código existente...
    
    // Aplicar control de acceso después de cargar la vista
    Platform.runLater(() -> {
        aplicarControlDeAcceso();
    });
}

private void aplicarControlDeAcceso() {
    User currentUser = Session.getCurrentUser();
    if (currentUser == null) return;
    
    // Solo ROOT puede ver el módulo de Roles
    if (!currentUser.isRootRole()) {
        btnRoles.setVisible(false);
        btnRoles.setManaged(false);
    }
    
    // Control de acceso a Auditoría
    if (!currentUser.tienePermiso("AUDITORIA", "READ")) {
        btnAudit.setVisible(false);
        btnAudit.setManaged(false);
    }
    
    // Control de acceso a Usuarios
    if (!currentUser.tienePermiso("USUARIOS", "READ")) {
        btnUsers.setVisible(false);
        btnUsers.setManaged(false);
    }
    
    // Control de acceso a Reportes
    if (!currentUser.tienePermiso("REPORTES", "READ")) {
        btnReports.setVisible(false);
        btnReports.setManaged(false);
    }
}
```

---

## 2️⃣ Validación de Permisos en Controladores

### Ejemplo en InventoryController

```java
@FXML
public void onNew() {
    // Verificar permiso antes de crear
    if (!tienePermiso("INVENTARIO", "CREATE")) {
        mostrarAccesoDenegado("crear productos");
        return;
    }
    
    // Continuar con la lógica...
    Dialog<Product> dialog = productDialog(null);
    // ...resto del código...
}

@FXML
public void onEdit() {
    // Verificar permiso antes de editar
    if (!tienePermiso("INVENTARIO", "UPDATE")) {
        mostrarAccesoDenegado("editar productos");
        return;
    }
    
    Product selected = tblProducts.getSelectionModel().getSelectedItem();
    if (selected == null) {
        Utils.showAlert("Aviso", "Seleccione un producto");
        return;
    }
    
    // Continuar con la lógica...
}

@FXML
public void onDelete() {
    // Verificar permiso antes de eliminar
    if (!tienePermiso("INVENTARIO", "DELETE")) {
        mostrarAccesoDenegado("eliminar productos");
        return;
    }
    
    Product selected = tblProducts.getSelectionModel().getSelectedItem();
    if (selected == null) {
        Utils.showAlert("Aviso", "Seleccione un producto");
        return;
    }
    
    // Continuar con la lógica...
}

// Método auxiliar para verificar permisos
private boolean tienePermiso(String modulo, String accion) {
    User currentUser = Session.getCurrentUser();
    if (currentUser == null) return false;
    
    boolean tiene = currentUser.tienePermiso(modulo, accion);
    
    if (!tiene) {
        Logs.warn(getClass(), "Acceso denegado: usuario={}, modulo={}, accion={}", 
            currentUser.getString("username"), modulo, accion);
    }
    
    return tiene;
}

// Método auxiliar para mostrar mensaje de acceso denegado
private void mostrarAccesoDenegado(String accion) {
    Utils.showAlert("Acceso Denegado", 
        "No tienes permisos para " + accion + ".\n" +
        "Contacta al administrador si necesitas acceso.");
}
```

---

## 3️⃣ Deshabilitar Botones en lugar de Ocultarlos

### Ejemplo con Tooltips Informativos

```java
@FXML
public void initialize() {
    // ...código existente...
    
    configurarBotonesConPermisos();
}

private void configurarBotonesConPermisos() {
    User currentUser = Session.getCurrentUser();
    if (currentUser == null) return;
    
    // Botón de eliminar
    if (!currentUser.tienePermiso("INVENTARIO", "DELETE")) {
        btnDelete.setDisable(true);
        Tooltip tooltip = new Tooltip("No tienes permisos para eliminar");
        btnDelete.setTooltip(tooltip);
        btnDelete.setStyle("-fx-opacity: 0.5;");
    }
    
    // Botón de editar
    if (!currentUser.tienePermiso("INVENTARIO", "UPDATE")) {
        btnEdit.setDisable(true);
        Tooltip tooltip = new Tooltip("No tienes permisos para editar");
        btnEdit.setTooltip(tooltip);
        btnEdit.setStyle("-fx-opacity: 0.5;");
    }
    
    // Botón de nuevo
    if (!currentUser.tienePermiso("INVENTARIO", "CREATE")) {
        btnNew.setDisable(true);
        Tooltip tooltip = new Tooltip("No tienes permisos para crear");
        btnNew.setTooltip(tooltip);
        btnNew.setStyle("-fx-opacity: 0.5;");
    }
}
```

---

## 4️⃣ Actualizar UsersController para Asignar Roles

### Diálogo con Selector de Roles

```java
private Dialog<User> userDialog(User base) {
    Dialog<User> d = new Dialog<>();
    d.setTitle(base == null ? "Nuevo usuario" : "Editar usuario");
    d.setResizable(true);
    d.getDialogPane().setPrefSize(480, 350);
    
    ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
    d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
    
    TextField txtName = new TextField();
    txtName.setPromptText("Nombre");
    
    TextField txtUsername = new TextField();
    txtUsername.setPromptText("Usuario");
    
    // ComboBox con los roles disponibles
    ComboBox<Rol> cmbRol = new ComboBox<>();
    
    // Cargar roles desde la base de datos
    List<Rol> roles = Rol.where("activo = ?", 1).orderBy("nombre");
    cmbRol.getItems().addAll(roles);
    
    // Configurar cómo se muestra cada rol
    cmbRol.setCellFactory(lv -> new ListCell<Rol>() {
        @Override
        protected void updateItem(Rol rol, boolean empty) {
            super.updateItem(rol, empty);
            if (empty || rol == null) {
                setText(null);
            } else {
                setText(rol.getString("nombre") + " - " + rol.getString("descripcion"));
            }
        }
    });
    
    cmbRol.setButtonCell(new ListCell<Rol>() {
        @Override
        protected void updateItem(Rol rol, boolean empty) {
            super.updateItem(rol, empty);
            if (empty || rol == null) {
                setText(null);
            } else {
                setText(rol.getString("nombre"));
            }
        }
    });
    
    if (base != null) {
        txtName.setText(base.getString("name"));
        txtUsername.setText(base.getString("username"));
        
        // Seleccionar el rol actual
        Rol rolActual = base.getRol();
        if (rolActual != null) {
            for (Rol r : roles) {
                if (r.getId().equals(rolActual.getId())) {
                    cmbRol.getSelectionModel().select(r);
                    break;
                }
            }
        }
    } else {
        // Por defecto seleccionar OPERADOR
        for (Rol r : roles) {
            if ("OPERADOR".equals(r.getString("nombre"))) {
                cmbRol.getSelectionModel().select(r);
                break;
            }
        }
    }
    
    GridPane gp = new GridPane();
    gp.setHgap(10);
    gp.setVgap(10);
    gp.setPadding(new Insets(10));
    
    gp.add(new Label("Nombre:"), 0, 0);
    gp.add(txtName, 1, 0);
    gp.add(new Label("Usuario:"), 0, 1);
    gp.add(txtUsername, 1, 1);
    gp.add(new Label("Rol:"), 0, 2);
    gp.add(cmbRol, 1, 2);
    
    d.getDialogPane().setContent(gp);
    
    Node btnSave = d.getDialogPane().lookupButton(save);
    BooleanBinding invalid = txtName.textProperty().isEmpty()
            .or(txtUsername.textProperty().isEmpty())
            .or(cmbRol.getSelectionModel().selectedItemProperty().isNull());
    btnSave.disableProperty().bind(invalid);
    
    d.setResultConverter(bt -> {
        if (bt == save) {
            User u = base == null ? new User() : base;
            u.set("name", txtName.getText());
            u.set("username", txtUsername.getText());
            
            Rol rolSeleccionado = cmbRol.getValue();
            if (rolSeleccionado != null) {
                u.set("rol_id", rolSeleccionado.getId());
                // Mantener compatibilidad con campo antiguo
                u.set("role", rolSeleccionado.getString("nombre").toLowerCase());
            }
            
            return u;
        }
        return null;
    });
    
    return d;
}
```

---

## 5️⃣ Registrar Acciones en Auditoría

### Ejemplo de Registro de Cambios de Roles

```java
// En RolesController después de editar un rol
private void onEditarRol(Rol rol) {
    if (rol.isRoot()) {
        Utils.showAlert("Aviso", "No se puede editar el rol ROOT");
        return;
    }
    
    // Guardar valores anteriores para auditoría
    String nombreAnterior = rol.getString("nombre");
    String descripcionAnterior = rol.getString("descripcion");
    boolean activoAnterior = rol.isActivo();
    
    Dialog<Rol> dialog = crearDialogoRol(rol);
    dialog.showAndWait().ifPresent(rolEditado -> {
        try {
            rol.set("nombre", rolEditado.getString("nombre"));
            rol.set("descripcion", rolEditado.getString("descripcion"));
            rol.set("activo", rolEditado.get("activo"));
            rol.saveIt();
            
            // Registrar en auditoría
            auditService.logAction(
                Session.getCurrentUserId(),
                "update_role",
                "roles",
                rol.getInteger("id"),
                String.format("Rol actualizado: %s -> %s, activo: %s -> %s",
                    nombreAnterior, rol.getString("nombre"),
                    activoAnterior, rol.isActivo())
            );
            
            cargarRoles();
            Utils.showAlert("Éxito", "Rol actualizado correctamente");
        } catch (Exception e) {
            Logs.error(getClass(), "Error actualizando rol: {}", e.getMessage());
            Utils.showAlert("Error", "No se pudo actualizar el rol: " + e.getMessage());
        }
    });
}
```

### Registro de Intentos de Acceso Denegado

```java
private boolean tienePermiso(String modulo, String accion) {
    User currentUser = Session.getCurrentUser();
    if (currentUser == null) return false;
    
    boolean tiene = currentUser.tienePermiso(modulo, accion);
    
    if (!tiene) {
        // Registrar intento de acceso denegado
        try {
            auditService.logAction(
                currentUser.getInteger("id"),
                "access_denied",
                modulo.toLowerCase(),
                null,
                String.format("Intento de acceso: modulo=%s, accion=%s, rol=%s",
                    modulo, accion, currentUser.getRolNombre())
            );
        } catch (Exception e) {
            Logs.error(getClass(), "Error registrando acceso denegado: {}", e.getMessage());
        }
        
        Logs.warn(getClass(), "Acceso denegado: usuario={}, modulo={}, accion={}", 
            currentUser.getString("username"), modulo, accion);
    }
    
    return tiene;
}
```

---

## 6️⃣ Caché de Permisos para Optimización

### Implementación de Caché en Session

```java
// En la clase Session
private static Map<String, Boolean> permisosCache = new HashMap<>();

public static boolean tienePermisoCache(String modulo, String accion) {
    String key = getCurrentUserId() + ":" + modulo + ":" + accion;
    
    if (permisosCache.containsKey(key)) {
        return permisosCache.get(key);
    }
    
    User currentUser = getCurrentUser();
    boolean tiene = currentUser != null && currentUser.tienePermiso(modulo, accion);
    
    permisosCache.put(key, tiene);
    return tiene;
}

public static void limpiarCachePermisos() {
    permisosCache.clear();
}

// Llamar al cambiar de usuario o actualizar permisos
public static void setCurrentUser(User user) {
    currentUser = user;
    limpiarCachePermisos(); // Limpiar caché al cambiar de usuario
}
```

---

## 7️⃣ Validación en Servicios

### Ejemplo en ProductService

```java
public class ProductService {
    
    private boolean validarPermiso(String accion) {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Usuario no autenticado");
        }
        
        if (!currentUser.tienePermiso("INVENTARIO", accion)) {
            throw new SecurityException(
                "No tienes permisos para " + accion + " en inventario"
            );
        }
        
        return true;
    }
    
    public void createProduct(String name, String description, int quantity) {
        validarPermiso("CREATE");
        
        // Lógica de creación...
    }
    
    public void updateProduct(int id, String name, String description, int quantity) {
        validarPermiso("UPDATE");
        
        // Lógica de actualización...
    }
    
    public void deleteProduct(int id) {
        validarPermiso("DELETE");
        
        // Lógica de eliminación...
    }
}
```

---

## 8️⃣ Reporte de Permisos de Usuario

### Método para Generar Reporte de Permisos

```java
public class PermisosReport {
    
    public static String generarReportePermisos(User user) {
        StringBuilder sb = new StringBuilder();
        
        Rol rol = user.getRol();
        if (rol == null) {
            return "Usuario sin rol asignado";
        }
        
        sb.append("REPORTE DE PERMISOS\n");
        sb.append("===================\n\n");
        sb.append("Usuario: ").append(user.getString("name")).append("\n");
        sb.append("Username: ").append(user.getString("username")).append("\n");
        sb.append("Rol: ").append(rol.getString("nombre")).append("\n");
        sb.append("Descripción: ").append(rol.getString("descripcion")).append("\n\n");
        
        List<Permiso> permisos = rol.getPermisos();
        Map<String, List<Permiso>> porModulo = agruparPorModulo(permisos);
        
        sb.append("PERMISOS ASIGNADOS:\n");
        sb.append("-------------------\n\n");
        
        for (Map.Entry<String, List<Permiso>> entry : porModulo.entrySet()) {
            sb.append("📌 ").append(entry.getKey()).append("\n");
            for (Permiso p : entry.getValue()) {
                sb.append("   ✓ ").append(p.getString("accion"))
                  .append(" - ").append(p.getString("descripcion")).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("Total de permisos: ").append(permisos.size()).append("\n");
        
        return sb.toString();
    }
    
    private static Map<String, List<Permiso>> agruparPorModulo(List<Permiso> permisos) {
        Map<String, List<Permiso>> mapa = new TreeMap<>();
        for (Permiso p : permisos) {
            String modulo = p.getString("modulo");
            mapa.computeIfAbsent(modulo, k -> new ArrayList<>()).add(p);
        }
        return mapa;
    }
}
```

---

## 🎯 Orden de Implementación Recomendado

1. **Día 1**: Implementar control de acceso en MainController
2. **Día 2**: Validar permisos en InventoryController
3. **Día 3**: Validar permisos en controladores de transacciones
4. **Día 4**: Actualizar UsersController con selector de roles
5. **Día 5**: Implementar auditoría de cambios
6. **Día 6**: Agregar caché de permisos
7. **Día 7**: Testing y refinamiento

---

## ✅ Testing Manual

### Checklist de Pruebas

- [ ] Usuario ROOT puede acceder a todos los módulos
- [ ] Usuario ADMIN no ve el módulo de Roles
- [ ] Usuario OPERADOR no ve Auditoría ni Usuarios
- [ ] Usuario CONSULTOR solo puede ver información (botones deshabilitados)
- [ ] Usuario AUDITOR solo ve Dashboard, Auditoría y Reportes
- [ ] Intentos de acceso denegado se registran en auditoría
- [ ] Los permisos se actualizan correctamente al cambiar rol
- [ ] No se puede eliminar rol ROOT
- [ ] No se puede eliminar rol con usuarios asignados

---

**¡Con estos ejemplos puedes completar la implementación de seguridad del sistema!** 🔒

