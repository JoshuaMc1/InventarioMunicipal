@echo off
echo ============================================
echo  Generando JAR - Inventario Municipal
echo ============================================
echo.

REM Limpiar compilaciones anteriores
echo [1/3] Limpiando proyecto...
call mvn clean
if %errorlevel% neq 0 (
    echo Error en limpieza del proyecto
    pause
    exit /b %errorlevel%
)

echo.
echo [2/3] Compilando y empaquetando...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo Error en la compilacion
    pause
    exit /b %errorlevel%
)

echo.
echo [3/3] Verificando JAR generado...
if exist "target\InventarioMunicipal-1.0-SNAPSHOT.jar" (
    echo.
    echo ============================================
    echo  JAR generado exitosamente!
    echo ============================================
    echo.
    echo Ubicacion: target\InventarioMunicipal-1.0-SNAPSHOT.jar
    echo.
    echo Para ejecutar:
    echo java -jar target\InventarioMunicipal-1.0-SNAPSHOT.jar
    echo.
) else (
    echo Error: No se genero el JAR
)

pause

