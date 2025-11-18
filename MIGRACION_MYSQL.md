# Guía de Migración a MySQL - Inventario Municipal

## 📋 Cambios Importantes Implementados

### ✅ Base de Datos MySQL
- **Cambiado de SQLite a MySQL**
- Configuración segura con cifrado de credenciales
- Diálogo de configuración inicial en primer arranque

### ✅ Usuario ROOT
- Nuevo rol `root` con acceso total al sistema
- El rol `admin` ahora es para administradores con acceso limitado
- Usuario root se crea en la configuración inicial

### ✅ Archivos Creados/Modificados

#### Nuevos Archivos:
1. **MySQLConfigDialog.java** - Diálogo de configuración de MySQL
2. **MySQLConfigManager.java** - Gestor de configuración con cifrado AES

#### Archivos Modificados:
1. **pom.xml** - Agregada dependencia MySQL Connector/J 8.3.0
2. **DatabaseConfig.java** - Adaptado para MySQL
3. **AppState.java** - Agregadas banderas: `database_configured`, `root_created`
4. **InitialSetupDialog.java** - Cambiado de admin a ROOT
5. **App.java** - Flujo de inicio actualizado
6. **User.java** - Agregado método `isRoot()`
7. **Roles.java** - Agregado rol "root"
8. **UsersController.java** - Incluido rol root en combo
9. **Todas las migraciones SQL** - Convertidas a sintaxis MySQL

---

## 🔧 Requisitos Previos para Instalación

### 1. Servidor MySQL
- MySQL Server 8.0 o superior
- Puerto por defecto: **3306** (configurable)
- Usuario con permisos de creación de tablas

### 2. Crear Base de Datos
Antes de ejecutar la aplicación, cree la base de datos en MySQL:

```sql
CREATE DATABASE inventario_municipal
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 3. Crear Usuario MySQL (Opcional pero Recomendado)
```sql
CREATE USER 'inventario_user'@'localhost' IDENTIFIED BY 'contraseña_segura';
GRANT ALL PRIVILEGES ON inventario_municipal.* TO 'inventario_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 🚀 Flujo de Primera Instalación

### Paso 1: Configuración de MySQL
Al iniciar la aplicación por primera vez, se mostrará el diálogo de **Configuración de Base de Datos MySQL**:

**Datos Necesarios:**
- **Host**: Dirección del servidor MySQL (ej: `localhost`, `192.168.1.100`)
- **Puerto**: Puerto MySQL (por defecto `3306`)
- **Base de datos**: Nombre de la BD creada (ej: `inventario_municipal`)
- **Usuario MySQL**: Usuario con permisos (ej: `root` o `inventario_user`)
- **Contraseña MySQL**: Contraseña del usuario MySQL

**Proceso:**
1. Ingresar los datos de conexión
2. Hacer clic en **"Probar Conexión"**
   - ✅ Si es exitosa: se habilita el botón de migraciones
   - ❌ Si falla: verificar datos y reintentar
3. Hacer clic en **"Ejecutar Migraciones"**
   - Esto creará todas las tablas necesarias
   - ✅ Si es exitosa: se habilita el botón de guardar
4. Hacer clic en **"Guardar y Continuar"**
   - Guardará la configuración de forma segura (cifrada con AES)

### Paso 2: Creación de Usuario ROOT
Después de configurar MySQL, se mostrará el diálogo para crear el **Usuario ROOT**:

**Datos Requeridos:**
- **Nombre completo**: Nombre del administrador principal
- **Nombre de usuario**: Username para login
- **Contraseña**: Contraseña segura (mínimo 6 caracteres)
- **Confirmar contraseña**: Repetir contraseña

**Características del Usuario ROOT:**
- ✅ Acceso total al sistema
- ✅ No requiere cambio forzado de contraseña en primer acceso
- ✅ Puede crear otros usuarios (admin, operator, auditor, guest)

### Paso 3: Inicio de Sesión
Una vez creado el usuario ROOT, ya puede iniciar sesión en el sistema.

---

## 📁 Ubicación de Archivos de Configuración

Todos los archivos de configuración se guardan en una carpeta segura:

**Windows:**
```
C:\Users\[USUARIO]\.inventario_municipal\
```

**Archivos generados:**
- `mysql.config` - Configuración de MySQL (cifrada)
- `appstate.properties` - Estado de la aplicación

---

## 🔐 Seguridad

### Cifrado de Credenciales
Las credenciales de MySQL se guardan cifradas usando:
- **Algoritmo**: AES-256
- **Derivación de clave**: PBKDF2WithHmacSHA256
- **Iteraciones**: 65,536

### Roles del Sistema

| Rol | Descripción | Acceso |
|-----|-------------|--------|
| **root** | Acceso total al sistema | ✅ Todos los módulos y configuraciones |
| **admin** | Administrador | ✅ Gestión general (sin acceso a ciertos ajustes críticos) |
| **operator** | Operador | ✅ Operaciones diarias |
| **auditor** | Auditor | 👁️ Solo lectura y reportes |
| **guest** | Invitado | 👁️ Acceso limitado de solo lectura |

---

## 📊 Estructura de Base de Datos MySQL

### Tablas Creadas:

1. **users** - Usuarios del sistema
2. **people** - Personas (para préstamos, etc.)
3. **products** - Productos del inventario
4. **transactions** - Transacciones (in/out/loan)
5. **audit** - Registro de auditoría
6. **schema_migrations** - Control de migraciones

### Características:
- Motor: **InnoDB**
- Charset: **utf8mb4**
- Collation: **utf8mb4_unicode_ci**
- Relaciones con Foreign Keys
- Índices optimizados

---

## 🛠️ Preparación para Inno Setup

### Archivos a Incluir en el Instalador:

1. **JAR de la aplicación** (generado con Maven)
2. **JRE embebido** (Java Runtime Environment)
3. **Manual de usuario** (PDF)
4. **Script de Inno Setup**

### Estructura Sugerida para Instalador:

```
[Instalador]
├── InventarioMunicipal.exe (launcher)
├── jre/ (Java Runtime embebido)
├── lib/
│   └── InventarioMunicipal.jar
├── docs/
│   └── Manual_Usuario.pdf
└── config/
    └── (vacío - se generará en primer arranque)
```

### Script Inno Setup - Sugerencias:

```iss
[Setup]
AppName=Inventario Municipal
AppVersion=1.0
DefaultDirName={pf}\InventarioMunicipal
DefaultGroupName=Inventario Municipal
OutputDir=output
OutputBaseFilename=InventarioMunicipal_Setup

[Files]
Source: "InventarioMunicipal.jar"; DestDir: "{app}\lib"
Source: "jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs
Source: "Manual_Usuario.pdf"; DestDir: "{app}\docs"

[Icons]
Name: "{group}\Inventario Municipal"; Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-jar ""{app}\lib\InventarioMunicipal.jar"""
Name: "{commondesktop}\Inventario Municipal"; Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-jar ""{app}\lib\InventarioMunicipal.jar"""

[Run]
Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-jar ""{app}\lib\InventarioMunicipal.jar"""; Description: "Iniciar Inventario Municipal"; Flags: postinstall nowait
```

---

## ✅ Checklist Pre-Instalación

Para el usuario final:

- [ ] MySQL Server instalado y funcionando
- [ ] Base de datos `inventario_municipal` creada
- [ ] Usuario MySQL con permisos configurado
- [ ] Puerto 3306 accesible (o el puerto configurado)
- [ ] Firewall permite conexión a MySQL

---

## 🔄 Proceso de Actualización de Versiones Futuras

Si necesita actualizar el sistema en el futuro:

1. La configuración de MySQL se mantiene (no se pierde)
2. Las nuevas migraciones se ejecutan automáticamente
3. Los datos existentes se preservan

---

## 📞 Solución de Problemas Comunes

### Error: "No se puede conectar a MySQL"
✅ **Solución:**
- Verificar que MySQL esté corriendo
- Verificar host/puerto/credenciales
- Verificar firewall

### Error: "Error ejecutando migraciones"
✅ **Solución:**
- Verificar permisos del usuario MySQL
- Verificar que la BD esté vacía
- Revisar logs en: `%USERPROFILE%\.inventario_municipal\logs\`

### Error: "Base de datos no configurada"
✅ **Solución:**
- Eliminar archivo: `%USERPROFILE%\.inventario_municipal\mysql.config`
- Reiniciar aplicación para reconfigurar

---

## 📝 Notas Importantes

1. **Backup**: Recomendado hacer backup regular de la base de datos MySQL
2. **Contraseñas**: El sistema usa bcrypt para hashear contraseñas de usuarios
3. **Logs**: Los logs se guardan en la carpeta de la aplicación
4. **Primer acceso**: Solo el usuario ROOT puede crear otros usuarios

---

## 🎯 Próximos Pasos

- [ ] Generar JAR con `mvn clean package`
- [ ] Empaquetar con JRE para Windows
- [ ] Crear script de Inno Setup
- [ ] Probar instalación en entorno limpio
- [ ] Crear manual de usuario
- [ ] Preparar documentación de soporte

---

**¡Sistema listo para empaquetado con Inno Setup!** 🚀

