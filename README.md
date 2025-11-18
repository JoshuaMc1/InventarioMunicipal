# 🏛️ Sistema de Inventario Municipal

Sistema de gestión de inventario desarrollado en JavaFX con base de datos MySQL para municipalidades.

## 🚀 Características Principales

- ✅ **Gestión de Inventario** - Control completo de productos
- ✅ **Transacciones** - Registro de ingresos, egresos y préstamos
- ✅ **Gestión de Usuarios** - Sistema de roles y permisos
- ✅ **Auditoría** - Registro completo de todas las acciones
- ✅ **Reportes** - Generación de reportes en PDF
- ✅ **Base de Datos MySQL** - Almacenamiento seguro y confiable
- ✅ **Interfaz Intuitiva** - Diseño moderno con JavaFX

## 📋 Requisitos del Sistema

### Para Desarrollo
- Java JDK 14 o superior
- Maven 3.6+
- MySQL Server 8.0+
- IDE compatible (IntelliJ IDEA, Eclipse, etc.)

### Para Producción
- MySQL Server 8.0+
- Sistema Operativo: Windows 10/11
- RAM: Mínimo 4GB
- Espacio en disco: 500MB

## 🛠️ Instalación para Desarrollo

### 1. Clonar el Repositorio
```bash
git clone [URL_DEL_REPOSITORIO]
cd InventarioMunicipal
```

### 2. Configurar MySQL
```sql
CREATE DATABASE inventario_municipal
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER 'inventario_user'@'localhost' IDENTIFIED BY 'tu_contraseña';
GRANT ALL PRIVILEGES ON inventario_municipal.* TO 'inventario_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Compilar el Proyecto
```bash
mvn clean compile
```

### 4. Ejecutar la Aplicación
```bash
mvn javafx:run
```

## 📦 Compilar para Distribución

### Generar JAR
```bash
mvn clean package
```

El JAR se generará en: `target/InventarioMunicipal-1.0-SNAPSHOT.jar`

### Ejecutar JAR
```bash
java -jar target/InventarioMunicipal-1.0-SNAPSHOT.jar
```

## 🔧 Configuración Inicial (Primera Vez)

### Paso 1: Configuración de MySQL
Al iniciar por primera vez, el sistema solicitará:
- Host del servidor MySQL
- Puerto (por defecto: 3306)
- Nombre de la base de datos
- Usuario y contraseña MySQL

### Paso 2: Crear Usuario ROOT
Después de configurar MySQL, crear el usuario principal:
- Nombre completo
- Nombre de usuario
- Contraseña segura

## 👥 Roles y Permisos

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **ROOT** | Superadministrador | Acceso total al sistema |
| **Admin** | Administrador | Gestión completa excepto configuraciones críticas |
| **Operator** | Operador | Operaciones diarias del inventario |
| **Auditor** | Auditor | Solo lectura y generación de reportes |
| **Guest** | Invitado | Acceso limitado de consulta |

## 📊 Estructura del Proyecto

```
InventarioMunicipal/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/joshuamc1/inventariomunicipal/
│   │   │       ├── config/          # Configuraciones
│   │   │       ├── controllers/     # Controladores JavaFX
│   │   │       ├── core/            # Utilidades centrales
│   │   │       ├── models/          # Modelos de datos
│   │   │       ├── repositories/    # Acceso a datos
│   │   │       ├── services/        # Lógica de negocio
│   │   │       └── App.java         # Clase principal
│   │   └── resources/
│   │       ├── css/                 # Estilos
│   │       ├── fxml/                # Vistas JavaFX
│   │       ├── images/              # Recursos gráficos
│   │       └── migrations/          # Migraciones SQL
│   └── test/                        # Tests unitarios
├── pom.xml                          # Configuración Maven
├── build.bat                        # Script de construcción
└── README.md                        # Este archivo
```

## 🔐 Seguridad

- **Contraseñas**: Hasheadas con BCrypt
- **Credenciales MySQL**: Cifradas con AES-256
- **Auditoría**: Registro completo de acciones
- **Sesiones**: Control de sesiones de usuario

## 📝 Base de Datos

### Tablas Principales
- `users` - Usuarios del sistema
- `people` - Personas (para préstamos)
- `products` - Productos del inventario
- `transactions` - Transacciones (entrada/salida/préstamo)
- `audit` - Registro de auditoría

### Migraciones
Las migraciones se ejecutan automáticamente en la configuración inicial.
Ubicación: `src/main/resources/migrations/`

## 🐛 Solución de Problemas

### Error: "Cannot connect to MySQL"
- Verificar que MySQL esté ejecutándose
- Verificar credenciales
- Revisar firewall

### Error: "Failed to execute migrations"
- Verificar permisos del usuario MySQL
- Asegurar que la base de datos esté vacía

### Error: "Database not configured"
- Eliminar configuración: `%USERPROFILE%\.inventario_municipal\mysql.config`
- Reiniciar aplicación

## 📚 Dependencias Principales

- **JavaFX** 25.0.1 - Framework UI
- **ActiveJDBC** 3.5 - ORM
- **MySQL Connector/J** 8.3.0 - Driver MySQL
- **BCrypt** 0.4 - Hashing de contraseñas
- **Logback** 1.4.11 - Logging
- **Apache PDFBox** 2.0.29 - Generación de PDFs

## 🔄 Actualización del Sistema

Para actualizar a una nueva versión:
1. Realizar backup de la base de datos
2. Instalar nueva versión
3. Las migraciones nuevas se aplicarán automáticamente

## 📞 Soporte

Para soporte técnico o reportar problemas:
- Email: [TU_EMAIL]
- Issues: [URL_GITHUB_ISSUES]

## 📄 Licencia

[Especificar licencia]

## 👨‍💻 Desarrollado por

Joshua MC - 2025

---

**Versión:** 1.0-SNAPSHOT  
**Última actualización:** Noviembre 2025

