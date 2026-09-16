@echo off
setlocal enabledelayedexpansion

rem Arranca Gestion de Reuniones en una ventana minimizada identificable por su titulo,
rem con el log redirigido a fichero. Espera a que responda y, si falla, muestra el log
rem aqui mismo (la ventana lanzada se cierra sola si el proceso muere, y si no
rem guardaramos el log no habria forma de ver el motivo).
rem Usa parar.bat para detenerla.

cd /d "%~dp0"

set "JAR=target\gestion-reuniones.jar"
set "TITLE=GestionReuniones"
set "PORT=8081"
set "CONTEXT=/gestion-reuniones"
set "LOG=gestion-reuniones.log"

tasklist /V /FI "IMAGENAME eq cmd.exe" 2>nul | findstr /I "%TITLE%" >nul
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
del "%LOG%" >nul 2>&1
start "%TITLE%" /min cmd /c ""%JAVA_EXE%" -jar "%JAR%" > "%LOG%" 2>&1"

echo Esperando a que responda en el puerto %PORT% (puede tardar unos segundos)...
set /a INTENTOS=0

:esperar
set /a INTENTOS+=1
curl -s -o nul "http://localhost:%PORT%%CONTEXT%"
if not errorlevel 1 goto listo

tasklist /V /FI "IMAGENAME eq cmd.exe" 2>nul | findstr /I "%TITLE%" >nul
if errorlevel 1 goto fallo

if !INTENTOS! GEQ 25 goto timeout
ping -n 2 127.0.0.1 >nul
goto esperar

:listo
echo.
echo Gestion de Reuniones disponible en http://localhost:%PORT%%CONTEXT%
exit /b 0

:fallo
echo.
echo ERROR: la aplicacion se ha cerrado nada mas arrancar. Esto es lo que ha escrito en "%LOG%":
echo ----------------------------------------------------------------
type "%LOG%"
echo ----------------------------------------------------------------
echo.
echo Causas habituales: no hay un Java 21+ en el PATH ni en JAVA_HOME ^(revisa con
echo "java -version" o definiendo JAVA_HOME^), o el puerto %PORT% esta ocupado por
echo otra aplicacion.
exit /b 1

:timeout
echo.
echo Aviso: no ha respondido a tiempo, pero el proceso sigue vivo. Revisa "%LOG%".
exit /b 1
