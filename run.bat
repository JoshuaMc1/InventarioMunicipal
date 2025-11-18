::  Código original
SET JAVA_HOME=%~dp0\jdk
SET PATH=%JAVA_HOME%\bin;%PATH%
start /B "" "%JAVA_HOME%\bin\javaw.exe" --module-path "%~dp0\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -jar "%~dp0\InventarioMunicipal-1.0-SNAPSHOT.jar"