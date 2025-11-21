# 🏛️ Sistema de Inventario Municipal

[![GitHub](https://img.shields.io/badge/GitHub-JoshuaMc1-blue?logo=github)](https://github.com/JoshuaMc1/InventarioMunicipal)
[![Java](https://img.shields.io/badge/Java-14+-orange?logo=java)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25.0.1-brightgreen)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue?logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)

Sistema de gestión de inventario desarrollado en JavaFX con base de datos MySQL para municipalidades. Incluye control de usuarios con sistema de roles y permisos granulares, auditoría completa y generación de reportes.

**🔗 Repositorio**: [https://github.com/JoshuaMc1/InventarioMunicipal](https://github.com/JoshuaMc1/InventarioMunicipal)

---

## 🚀 Características Principales

### Gestión Completa
- ✅ **Inventario** - Control total de productos con alertas de stock bajo
- ✅ **Transacciones** - Registro de ingresos, egresos y préstamos
- ✅ **Personas** - Gestión de personas para préstamos y registros
- ✅ **Usuarios** - Administración de usuarios del sistema
- ✅ **Roles y Permisos** - Sistema RBAC con permisos granulares

### Seguridad y Control
- 🔐 **Sistema de Roles** - 6 roles predefinidos (ROOT, ADMIN, SUPERVISOR, OPERADOR, CONSULTOR, AUDITOR)
- 🔒 **Permisos Granulares** - Control de acceso por módulo y acción (CREATE, READ, UPDATE, DELETE)
- 🔑 **Cifrado** - Contraseñas con BCrypt, credenciales MySQL con AES-256
- 📝 **Auditoría Completa** - Registro de todas las acciones del sistema

### Reportes y Análisis
- 📊 **Dashboard Interactivo** - Estadísticas en tiempo real
- 📄 **Generación de Reportes** - Exportación a PDF y CSV
- 📈 **Análisis de Datos** - Reportes de movimientos, consumo, actividad
- 🔍 **Auditoría Visible** - Vista de logs para ROOT y AUDITOR

### Tecnología
- 🎨 **Interfaz Moderna** - JavaFX 25.0.1 con diseño intuitivo
- 🗄️ **Base de Datos Robusta** - MySQL 8.0+ con migraciones automáticas
- 🔄 **ORM Eficiente** - ActiveJDBC para acceso a datos
- 📦 **Fácil Distribución** - Empaquetado en JAR ejecutable

---

## 📚 Documentación

### Documentación Técnica

| Documento | Descripción |
|-----------|-------------|
| **[DATABASE.md](docs/DATABASE.md)** | Estructura completa de la base de datos, tablas, relaciones, migraciones y consultas útiles |
| **[SECURITY.md](docs/SECURITY.md)** | Arquitectura de seguridad, autenticación, autorización, validaciones y mejores prácticas |
| **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** | Arquitectura del proyecto, estructura de paquetes, clases y responsabilidades |

### Documentación de Desarrollo

| Documento | Descripción |
|-----------|-------------|
| **[PROGRESO_FASE1.md](docs/PROGRESO_FASE1.md)** | Implementación de permisos - Fase 1 (Menú y Dashboard) |
| **[PROGRESO_FASE2.md](docs/PROGRESO_FASE2.md)** | Implementación de permisos - Fase 2 (Transacciones) |
| **[PROGRESO_FASE3.md](docs/PROGRESO_FASE3.md)** | Implementación de permisos - Fase 3 (Gestión) |
| **[PROGRESO_FASE4.md](docs/PROGRESO_FASE4.md)** | Implementación de permisos - Fase 4 (Auditoría y Reportes) |
| **[PLAN_COMPLETO_PERMISOS.md](docs/PLAN_COMPLETO_PERMISOS.md)** | Plan completo del sistema de permisos |

---

## 📋 Requisitos del Sistema

### Para Desarrollo
- **Java JDK** 14 o superior
- **Maven** 3.6+
- **MySQL Server** 8.0+
  - **mysqldump** (incluido con MySQL Server)
- **gzip** (para compresión de backups)
  - Windows: Instalar desde [GnuWin32](http://gnuwin32.sourceforge.net/packages/gzip.htm) o usar WSL
  - Linux/macOS: Incluido por defecto
- **IDE** compatible (IntelliJ IDEA, Eclipse, NetBeans)
- **Git** (para clonar el repositorio)

### Para Producción/Uso
- **MySQL Server** 8.0+
  - **mysqldump** (incluido con MySQL Server, verificar en PATH)
- **gzip** (para compresión de backups)
  - Windows: Descargar e instalar [GnuWin32 gzip](http://gnuwin32.sourceforge.net/packages/gzip.htm)
  - Linux: `sudo apt-get install gzip` (Debian/Ubuntu) o `sudo yum install gzip` (RedHat/CentOS)
  - macOS: Incluido por defecto
- **Sistema Operativo**: Windows 10/11, Linux, macOS
- **RAM**: Mínimo 4GB recomendado
- **Espacio en disco**: 500MB libres (más espacio para backups)
- **Java Runtime** 14+ (incluido en el instalador)

#### Configuración Adicional para Windows

Para que `mysqldump` y `gzip` funcionen correctamente en el sistema de backups:

1. **mysqldump**: Agregar la carpeta `bin` de MySQL al PATH del sistema
   ```
   C:\Program Files\MySQL\MySQL Server 8.0\bin
   ```

2. **gzip**: Después de instalar GnuWin32, agregar al PATH del sistema
   ```
   C:\Program Files (x86)\GnuWin32\bin
   ```

**Verificar instalación:**
```bash
# Verificar mysqldump
mysqldump --version

# Verificar gzip
gzip --version
```

---

## 🛠️ Instalación y Configuración

### Instalación para Desarrollo

#### 1. Clonar el Repositorio
```bash
git clone https://github.com/JoshuaMc1/InventarioMunicipal.git
cd InventarioMunicipal
```

#### 2. Configurar MySQL
```sql
-- Crear base de datos
CREATE DATABASE inventario_municipal
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Crear usuario (opcional, recomendado para producción)
CREATE USER 'inventario_user'@'localhost' IDENTIFIED BY 'tu_contraseña_segura';
GRANT ALL PRIVILEGES ON inventario_municipal.* TO 'inventario_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. Compilar el Proyecto
```bash
mvn clean compile
```

#### 4. Ejecutar en Modo Desarrollo
```bash
mvn javafx:run
```

### Compilar para Distribución

#### Generar JAR Ejecutable
```bash
mvn clean package
```

El JAR se generará en: `target/InventarioMunicipal-1.0-SNAPSHOT.jar`

#### Ejecutar JAR
```bash
java -jar target/InventarioMunicipal-1.0-SNAPSHOT.jar
```

---

## 🔧 Configuración Inicial (Primera Vez)

### Paso 1: Configuración de MySQL

Al iniciar por primera vez, el sistema mostrará un diálogo solicitando:

1. **Host** del servidor MySQL (por defecto: `localhost`)
2. **Puerto** (por defecto: `3306`)
3. **Nombre de la base de datos** (por defecto: `inventario_municipal`)
4. **Usuario** MySQL
5. **Contraseña** MySQL

El sistema:
- Probará la conexión
- Creará la base de datos si no existe
- Ejecutará todas las migraciones automáticamente
- Cifrará y guardará las credenciales

### Paso 2: Crear Usuario ROOT

Después de configurar MySQL exitosamente, se solicitará crear el primer usuario ROOT:

1. **Nombre completo** del administrador
2. **Nombre de usuario** (único)
3. **Contraseña** segura

Este será el superadministrador con acceso total al sistema.

---

## 👥 Roles y Permisos

El sistema implementa un modelo **RBAC** (Role-Based Access Control) con 6 roles predefinidos:

| Rol | Permisos | Módulos Accesibles | Descripción |
|-----|----------|-------------------|-------------|
| **ROOT** | 52 (100%) | 13/13 | Superadministrador con acceso total |
| **ADMIN** | 40 (77%) | 12/13 | Administrador general (sin Auditoría ni Roles) |
| **SUPERVISOR** | 29 (56%) | 9/13 | Supervisor operativo (sin Usuarios, Roles, Auditoría, Backup) |
| **OPERADOR** | 16 (sin DELETE) | 8/13 | Operador básico (sin eliminaciones) |
| **CONSULTOR** | 12 (solo READ) | 7/13 | Consulta y reportes (solo lectura) |
| **AUDITOR** | 5 (especializado) | 3/13 | Auditoría y supervisión especializada |

### Matriz Detallada de Permisos

Ver [SECURITY.md](docs/SECURITY.md) para la matriz completa de permisos por módulo y rol.

---

## 📊 Módulos del Sistema

### Dashboard
- Estadísticas en tiempo real
- Gráficos de movimientos
- Alertas de stock bajo
- Resumen de transacciones

### Inventario
- CRUD de productos
- Control de stock
- Alertas automáticas
- Historial de movimientos

### Transacciones
- **Ingresos**: Registro de entradas al inventario
- **Egresos**: Registro de salidas del inventario
- **Préstamos**: Gestión de préstamos a personas

### Gestión
- **Personas**: Registro de personas para préstamos
- **Usuarios**: Administración de usuarios del sistema
- **Roles**: Gestión de roles y permisos (solo ROOT)

### Auditoría y Reportes
- **Auditoría**: Vista de logs del sistema (ROOT y AUDITOR)
- **Reportes**: Generación de reportes en PDF/CSV
- **Backup**: Copias de seguridad de la base de datos (ROOT y ADMIN)
- **Perfil**: Gestión del perfil propio del usuario

---

## 🔐 Seguridad

### Capas de Seguridad

1. **Interfaz de Usuario**: Ocultación de elementos sin permiso
2. **Lógica de Negocio**: Validación de permisos antes de ejecutar
3. **Acceso a Datos**: Consultas parametrizadas (prevención SQL Injection)
4. **Base de Datos**: Contraseñas hasheadas, credenciales cifradas

### Características de Seguridad

- ✅ Contraseñas hasheadas con **BCrypt** (factor 10)
- ✅ Credenciales MySQL cifradas con **AES-256**
- ✅ Validaciones especiales (no eliminar ROOT, no auto-eliminación, etc.)
- ✅ Auditoría completa de todas las acciones
- ✅ Control de sesiones de usuario
- ✅ Prevención de SQL Injection

Ver [SECURITY.md](docs/SECURITY.md) para información detallada.

---

## 🗄️ Base de Datos

### Tablas Principales

| Tabla | Descripción |
|-------|-------------|
| `roles` | Roles del sistema |
| `permisos` | Permisos granulares |
| `rol_permisos` | Relación roles-permisos |
| `users` | Usuarios del sistema |
| `people` | Personas (para préstamos) |
| `products` | Productos del inventario |
| `transactions` | Transacciones (IN/OUT/LOAN) |
| `audit` | Registro de auditoría |

### Migraciones

Las migraciones se ejecutan automáticamente en orden secuencial. Ver [DATABASE.md](docs/DATABASE.md) para más detalles.

---

## 🏗️ Arquitectura

### Patrón de Diseño

**MVC + Service Layer + Repository**

```
Vista (FXML) → Controlador → Servicio → Repositorio → Modelo → Base de Datos
```

### Estructura de Paquetes

```
com.joshuamc1.inventariomunicipal/
├── config/           # Configuraciones del sistema
├── controllers/      # Controladores JavaFX (MVC)
├── core/             # Utilidades centrales
├── export/           # Exportación de documentos
├── models/           # Modelos de datos (ORM)
├── repositories/     # Repositorios (acceso a datos)
└── services/         # Servicios (lógica de negocio)
```

Ver [ARCHITECTURE.md](docs/ARCHITECTURE.md) para documentación completa de la arquitectura.

---

## 🐛 Solución de Problemas

### Error: "Cannot connect to MySQL"
**Solución:**
1. Verificar que MySQL esté ejecutándose
2. Verificar credenciales (usuario/contraseña)
3. Revisar que el puerto 3306 esté abierto
4. Desactivar firewall temporalmente para probar

### Error: "Failed to execute migrations"
**Solución:**
1. Verificar permisos del usuario MySQL
2. Asegurar que la base de datos esté vacía (o eliminarla y recrearla)
3. Revisar logs en `logs/app.log`

### Error: "Database not configured"
**Solución:**
1. Eliminar configuración: `%USERPROFILE%\.inventario_municipal\mysql.config`
2. Reiniciar aplicación
3. Completar configuración inicial nuevamente

### Error: "mysqldump: command not found" o fallo en backup
**Solución:**
1. Verificar instalación: `mysqldump --version`
2. Windows: Agregar MySQL bin al PATH:
   - Panel de Control → Sistema → Configuración avanzada del sistema
   - Variables de entorno → PATH → Agregar: `C:\Program Files\MySQL\MySQL Server 8.0\bin`
3. Linux/macOS: Instalar mysql-client: `sudo apt-get install mysql-client`
4. Reiniciar terminal o aplicación después de cambiar PATH

### Error: "gzip: command not found" o fallo en compresión de backup
**Solución:**
1. Verificar instalación: `gzip --version`
2. Windows: 
   - Descargar GnuWin32 gzip desde http://gnuwin32.sourceforge.net/packages/gzip.htm
   - Instalar y agregar al PATH: `C:\Program Files (x86)\GnuWin32\bin`
   - O usar WSL (Windows Subsystem for Linux)
3. Linux: `sudo apt-get install gzip`
4. macOS: gzip viene preinstalado, verificar permisos
5. Reiniciar aplicación después de instalar

### La aplicación no inicia
**Solución:**
1. Verificar que Java 14+ esté instalado: `java -version`
2. Verificar que MySQL esté ejecutándose
3. Revisar logs en `logs/app.log`

---

## 📚 Dependencias Principales

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| **JavaFX** | 25.0.1 | Framework de interfaz gráfica |
| **ActiveJDBC** | 3.5-j11 | ORM (Object-Relational Mapping) |
| **MySQL Connector/J** | 8.3.0 | Driver de MySQL |
| **BCrypt** | 0.10.2 | Hashing de contraseñas |
| **Logback** | 1.4.11 | Sistema de logging |
| **Apache PDFBox** | 2.0.29 | Generación de PDFs |

---

## 🔄 Actualización del Sistema

Para actualizar a una nueva versión:

1. **Realizar backup** de la base de datos:
   ```bash
   mysqldump -u root -p inventario_municipal > backup.sql
   ```

2. **Instalar nueva versión** (reemplazar JAR)

3. **Ejecutar aplicación** - Las migraciones nuevas se aplicarán automáticamente

4. **Verificar funcionamiento**

---

## 📞 Soporte y Contacto

### Soporte Técnico
- **Email**: joshua15mclean@gmail.com
- **GitHub Issues**: [Reportar problema](https://github.com/JoshuaMc1/InventarioMunicipal/issues)

### Enlaces Útiles
- **Repositorio**: https://github.com/JoshuaMc1/InventarioMunicipal
- **Releases**: https://github.com/JoshuaMc1/InventarioMunicipal/releases
- **Wiki**: https://github.com/JoshuaMc1/InventarioMunicipal/wiki

---

## 📄 Licencia

Este proyecto está bajo la **Licencia MIT**. Ver el archivo [LICENSE.txt](LICENSE.txt) para más detalles.

```
MIT License

Copyright (c) 2025 Joshua MC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 👨‍💻 Desarrollado por

**Joshua MC** - 2025

- GitHub: [@JoshuaMc1](https://github.com/JoshuaMc1)
- Email: joshua15mclean@gmail.com

---

## 🙏 Agradecimientos

- A la comunidad de JavaFX por el excelente framework
- A los desarrolladores de ActiveJDBC por el ORM simple y potente
- A todos los contribuidores y usuarios del sistema

---

**Versión:** 1.0-SNAPSHOT  
**Última actualización:** 21 de Noviembre de 2025  
**Estado:** Producción Ready

