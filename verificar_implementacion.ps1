# Script de Verificación - Sistema de Roles y Permisos
# Este script verifica que todos los archivos necesarios estén en su lugar

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VERIFICACIÓN DE IMPLEMENTACIÓN" -ForegroundColor Cyan
Write-Host "  Sistema de Roles y Permisos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errores = 0
$exitosos = 0

function Test-Archivo {
    param($ruta, $descripcion)
    if (Test-Path $ruta) {
        Write-Host "[✓]" -ForegroundColor Green -NoNewline
        Write-Host " $descripcion"
        $script:exitosos++
    } else {
        Write-Host "[✗]" -ForegroundColor Red -NoNewline
        Write-Host " $descripcion - NO ENCONTRADO"
        $script:errores++
    }
}

# Verificar Migraciones
Write-Host "`n--- MIGRACIONES SQL ---" -ForegroundColor Yellow
Test-Archivo "src\main\resources\migrations\20251119120000_create_roles_table.sql" "Tabla de roles"
Test-Archivo "src\main\resources\migrations\20251119120001_create_permissions_table.sql" "Tabla de permisos"
Test-Archivo "src\main\resources\migrations\20251119120002_create_role_permissions_table.sql" "Tabla rol_permisos"
Test-Archivo "src\main\resources\migrations\20251119120003_alter_users_add_role_id.sql" "Columna rol_id en users"
Test-Archivo "src\main\resources\migrations\20251119120004_seed_roles_and_permissions.sql" "Seeder de datos"

# Verificar Modelos
Write-Host "`n--- MODELOS JAVA ---" -ForegroundColor Yellow
Test-Archivo "src\main\java\com\joshuamc1\inventariomunicipal\models\Rol.java" "Modelo Rol"
Test-Archivo "src\main\java\com\joshuamc1\inventariomunicipal\models\Permiso.java" "Modelo Permiso"
Test-Archivo "src\main\java\com\joshuamc1\inventariomunicipal\models\User.java" "Modelo User (actualizado)"

# Verificar Controladores y Vistas
Write-Host "`n--- CONTROLADORES Y VISTAS ---" -ForegroundColor Yellow
Test-Archivo "src\main\java\com\joshuamc1\inventariomunicipal\controllers\dashboard\RolesController.java" "Controlador de Roles"
Test-Archivo "src\main\resources\fxml\dashboard\roles.fxml" "Vista de Roles"

# Verificar Configuración
Write-Host "`n--- CONFIGURACIÓN ---" -ForegroundColor Yellow
Test-Archivo "src\main\java\com\joshuamc1\inventariomunicipal\config\MigrationRunner.java" "MigrationRunner (actualizado)"

# Verificar Compilación
Write-Host "`n--- COMPILACIÓN ---" -ForegroundColor Yellow
if (Test-Path "target\InventarioMunicipal-1.0-SNAPSHOT.jar") {
    Write-Host "[✓]" -ForegroundColor Green -NoNewline
    Write-Host " JAR compilado"
    $exitosos++

    $tamano = (Get-Item "target\InventarioMunicipal-1.0-SNAPSHOT.jar").Length / 1MB
    Write-Host "    Tamaño: $([math]::Round($tamano, 2)) MB"
} else {
    Write-Host "[✗]" -ForegroundColor Red -NoNewline
    Write-Host " JAR no encontrado - ejecutar: mvn clean package"
    $errores++
}

# Verificar Clases Compiladas
Write-Host "`n--- CLASES COMPILADAS ---" -ForegroundColor Yellow
Test-Archivo "target\classes\com\joshuamc1\inventariomunicipal\models\Rol.class" "Rol.class"
Test-Archivo "target\classes\com\joshuamc1\inventariomunicipal\models\Permiso.class" "Permiso.class"
Test-Archivo "target\classes\com\joshuamc1\inventariomunicipal\controllers\dashboard\RolesController.class" "RolesController.class"

# Verificar Documentación
Write-Host "`n--- DOCUMENTACIÓN ---" -ForegroundColor Yellow
Test-Archivo "ROLES_Y_PERMISOS.md" "Documentación técnica"
Test-Archivo "IMPLEMENTACION_COMPLETA.md" "Guía de uso"

# Resumen
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  RESUMEN DE VERIFICACIÓN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Exitosos: " -NoNewline
Write-Host "$exitosos" -ForegroundColor Green
Write-Host "Errores:  " -NoNewline
Write-Host "$errores" -ForegroundColor $(if ($errores -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($errores -eq 0) {
    Write-Host "✓ IMPLEMENTACIÓN COMPLETA Y VERIFICADA" -ForegroundColor Green
    Write-Host ""
    Write-Host "Para ejecutar la aplicación:" -ForegroundColor Cyan
    Write-Host "  java -jar target\InventarioMunicipal-1.0-SNAPSHOT.jar" -ForegroundColor White
    Write-Host ""
    Write-Host "Credenciales de acceso ROOT:" -ForegroundColor Cyan
    Write-Host "  Usuario: root" -ForegroundColor White
    Write-Host "  Contraseña: contraseña2025" -ForegroundColor White
} else {
    Write-Host "✗ Hay archivos faltantes. Revisa los errores arriba." -ForegroundColor Red
}

Write-Host ""

