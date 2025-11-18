; ------------------------------------------------------------
; Instalador para Inventario Municipal v2.0
; Incluye JDK, JavaFX, .bat de lanzamiento y MySQL opcional
; Versión mejorada con rutas relativas y validaciones
; ------------------------------------------------------------

; Definir constantes para rutas relativas
#define MyAppName "Inventario Municipal"
#define MyAppVersion "1.0"
#define MyAppPublisher "JoshuaMc1"
#define MyAppURL "https://github.com/JoshuaMc1/InventarioMunicipal"
#define MyAppExeName "InventarioMunicipal-1.0-SNAPSHOT.jar"
#define MyAppLauncher "run.bat"

[Setup]
; Información básica de la aplicación
AppId={{8B2F4A3D-9C1E-4F6A-B2D3-1A4E5F6C7D8E}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases

; Configuración de instalación
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
LicenseFile=LICENSE.txt
InfoBeforeFile=README.txt
OutputDir=dist
OutputBaseFilename={#MyAppName}Setup-{#MyAppVersion}
SetupIconFile=src\main\resources\images\favicon.ico
UninstallDisplayIcon={app}\favicon.ico

; Configuración de compresión y seguridad
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64

; Requisitos mínimos
MinVersion=10.0.17763

; ----------------------------------------------------------------
; Tipos de instalación
[Types]
Name: "full"; Description: "Instalación completa (incluye MySQL Workbench)"
Name: "compact"; Description: "Instalación compacta (solo aplicación)"
Name: "custom"; Description: "Instalación personalizada"; Flags: iscustom

; ----------------------------------------------------------------
; Componentes
[Components]
Name: "core"; Description: "Archivos principales de {#MyAppName}"; Types: full compact custom; Flags: fixed
Name: "jdk"; Description: "Java Development Kit 25"; Types: full compact custom; Flags: fixed
Name: "javafx"; Description: "JavaFX Runtime"; Types: full compact custom; Flags: fixed
Name: "shortcuts"; Description: "Accesos directos"; Types: full compact; Flags: checkablealone
Name: "mysql"; Description: "MySQL Workbench (herramienta de gestión de BD)"; Types: full custom

; ----------------------------------------------------------------
; Archivos a copiar
[Files]
; Aplicación principal
Source: "D:\Java\InventarioMunicipal\target\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion; Components: core

; JDK 25 - usando ruta absoluta
Source: "E:\sdks\Java\openjdk-25.0.1\*"; DestDir: "{app}\jdk"; Flags: recursesubdirs createallsubdirs ignoreversion; Components: jdk

; JavaFX SDK - usando ruta absoluta
Source: "E:\sdks\Java\javafx-sdk-25.0.1\*"; DestDir: "{app}\javafx-sdk"; Flags: recursesubdirs createallsubdirs ignoreversion; Components: javafx

; Archivos de la aplicación
Source: "D:\Java\InventarioMunicipal\src\main\resources\images\favicon.ico"; DestDir: "{app}"; Flags: ignoreversion; Components: core
Source: "D:\Java\InventarioMunicipal\{#MyAppLauncher}"; DestDir: "{app}"; DestName: "{#MyAppLauncher}"; Flags: ignoreversion; Components: core

; Archivos de documentación
Source: "D:\Java\InventarioMunicipal\README.txt"; DestDir: "{app}"; Flags: ignoreversion; Components: core
Source: "D:\Java\InventarioMunicipal\LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion; Components: core

; MySQL Workbench (opcional)
Source: "D:\Instaladores\mysql-workbench-community-8.0.44-winx64.msi"; DestDir: "{app}\mysql"; Flags: ignoreversion; Components: mysql

; Crear carpeta de logs se hará automáticamente

; ----------------------------------------------------------------
; Crear accesos directos
[Icons]
; Menú Inicio
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppLauncher}"; WorkingDir: "{app}"; IconFilename: "{app}\favicon.ico"; Components: shortcuts
Name: "{group}\Desinstalar {#MyAppName}"; Filename: "{uninstallexe}"; Components: shortcuts
Name: "{group}\Documentación"; Filename: "{app}\README.txt"; Components: shortcuts

; Escritorio (opcional)
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppLauncher}"; WorkingDir: "{app}"; IconFilename: "{app}\favicon.ico"; Tasks: desktopicon; Components: shortcuts

; ----------------------------------------------------------------
; Tareas adicionales
[Tasks]
Name: "desktopicon"; Description: "Crear icono en el &escritorio"; GroupDescription: "Iconos adicionales:"
Name: "quicklaunchicon"; Description: "Crear icono en la barra de &inicio rápido"; GroupDescription: "Iconos adicionales:"; Flags: unchecked; OnlyBelowVersion: 6.1

; ----------------------------------------------------------------
; Entradas del registro
[Registry]
Root: HKLM; Subkey: "Software\{#MyAppPublisher}\{#MyAppName}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "Software\{#MyAppPublisher}\{#MyAppName}"; ValueType: string; ValueName: "Version"; ValueData: "{#MyAppVersion}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\App Paths\{#MyAppLauncher}"; ValueType: string; ValueName: ""; ValueData: "{app}\{#MyAppLauncher}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\App Paths\{#MyAppLauncher}"; ValueType: string; ValueName: "Path"; ValueData: "{app}"; Flags: uninsdeletekey

; ----------------------------------------------------------------
; Ejecutar al finalizar instalación
[Run]
; Instalar MySQL Workbench si fue seleccionado
Filename: "msiexec.exe"; Parameters: "/i ""{app}\mysql\mysql-workbench-community-8.0.44-winx64.msi"" /quiet"; Components: mysql; Flags: nowait postinstall skipifsilent; Description: "Instalar MySQL Workbench silenciosamente"

; Ejecutar la aplicación
Filename: "{app}\{#MyAppLauncher}"; Flags: nowait postinstall skipifsilent shellexec; Description: "Ejecutar {#MyAppName} ahora"; WorkingDir: "{app}"

; Abrir documentación
Filename: "{app}\README.txt"; Flags: nowait postinstall skipifsilent shellexec unchecked; Description: "Ver documentación"

; ----------------------------------------------------------------
; Archivos a eliminar en desinstalación
[UninstallDelete]
Type: files; Name: "{app}\*.log"
Type: files; Name: "{app}\logs\*.log"
Type: dirifempty; Name: "{app}\logs"
Type: files; Name: "{app}\*.tmp"

; ----------------------------------------------------------------
; Funciones de código Pascal para validaciones
[Code]
function InitializeSetup(): Boolean;
begin
  Result := True;
  
  // Verificar espacio en disco - removido por problemas de compatibilidad
  // RequiredSpace := 500 * 1024 * 1024;
  
  // Verificar si hay una instalación previa
  if RegKeyExists(HKLM, 'Software\{#MyAppPublisher}\{#MyAppName}') then
  begin
    if MsgBox('Se detectó una instalación previa. ¿Desea continuar?', mbConfirmation, MB_YESNO) = IDNO then
    begin
      Result := False;
      Exit;
    end;
  end;
  
  // Mostrar información sobre los componentes
  Log('Iniciando instalación de {#MyAppName} v{#MyAppVersion}');
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  
  // Saltar página de licencia si no existe el archivo
  if (PageID = wpLicense) and not FileExists('LICENSE.txt') then
    Result := True;
end;

// Función para mostrar información post-instalación
procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpFinished then
  begin
    Log('Instalación finalizada exitosamente');
  end;
end;