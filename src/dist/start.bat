@echo off
rem Mars Rover kiosk start. Runs from the folder this file is in, so levels\ and data\ are found.
cd /d "%~dp0"

where javaw >nul 2>nul
if errorlevel 1 (
    echo Java wurde nicht gefunden. Bitte JDK 25 installieren und neu starten.
    pause
    exit /b 1
)

rem javaw = no console window. Crashes are logged to data\errors.log.
start "" javaw -jar mars-rover.jar
