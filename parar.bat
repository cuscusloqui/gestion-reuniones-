@echo off
setlocal enabledelayedexpansion

rem Para la aplicacion arrancada con arrancar.bat, localizandola UNICAMENTE por el titulo
rem de su ventana ("GestionReuniones"). Deliberadamente NO se mata "lo que haya" en el
rem puerto: podria ser un proceso de otro proyecto y no es seguro asumir que es el nuestro.

set "TITLE=GestionReuniones"

set "PID="
for /f "tokens=2 delims=," %%A in ('tasklist /V /FI "IMAGENAME eq cmd.exe" /FO CSV 2^>nul ^| findstr /I "%TITLE%"') do (
    set "PID=%%~A"
)

if not defined PID (
    echo No se encuentra ninguna ventana "%TITLE%" en marcha.
    echo Si la aplicacion esta colgada sin esa ventana, cierrala manualmente
    echo identificando el proceso correcto ^(no se hace de forma automatica para
    echo evitar matar por error un proceso de otro proyecto^).
    exit /b 1
)

echo Deteniendo Gestion de Reuniones ^(PID !PID!^)...
taskkill /PID !PID! /T /F >nul 2>&1

echo Aplicacion detenida.
exit /b 0
