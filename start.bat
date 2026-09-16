@echo off
setlocal

rem Arranca Gestion de Reuniones en una ventana minimizada identificable por su titulo.
rem Usa stop.bat para detenerla.

cd /d "%~dp0"

set "JAR=target\gestion-reuniones.jar"
set "TITLE=GestionReuniones"
set "PORT=8081"

tasklist /V /FI "IMAGENAME eq java.exe" 2>nul | findstr /I "%TITLE%" >nul
if not errorlevel 1 (
    echo La aplicacion ya parece estar en marcha ^(ventana "%TITLE%"^).
    echo Si crees que es un error, cierra esa ventana manualmente y reintenta.
    exit /b 1
)

if not exist "%JAR%" (
    echo No se encuentra "%JAR%". Compilando el proyecto primero...
    if exist "mvnw.cmd" (
        call mvnw.cmd -q -DskipTests package
    ) else (
        call mvn -q -DskipTests package
    )
    if errorlevel 1 (
        echo ERROR: fallo al compilar el proyecto.
        exit /b 1
    )
)

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

echo Arrancando Gestion de Reuniones con "%JAVA_EXE%"...
start "%TITLE%" /min "%JAVA_EXE%" -jar "%JAR%"

echo.
echo Aplicacion lanzada en segundo plano (ventana minimizada "%TITLE%").
echo Puede tardar unos segundos en estar lista. Abre http://localhost:%PORT% en el navegador.
echo Usa stop.bat para detenerla.
exit /b 0
