@echo off
setlocal
cd /d "%~dp0"

REM ----------------------------------------------------------------------
REM   main.java.Recipe.Recipe Manager Pro - launcher (no console window)
REM   - Auto-builds the JAR on first run
REM   - Launches the GUI via javaw (detached, no console)
REM ----------------------------------------------------------------------

if not exist "RecipeManager.jar" (
    echo First-run setup: building...
    call "%~dp0build.bat"
    if errorlevel 1 exit /b 1
)

where javaw >nul 2>&1
if errorlevel 1 (
    echo [ERROR] javaw not found. Please install JRE/JDK.
    pause
    exit /b 1
)

start "" javaw -jar RecipeManager.jar

endlocal
