@echo off
setlocal
cd /d "%~dp0..\..\"

REM ----------------------------------------------------------------------
REM   Recipe Manager - debug launcher (with console)
REM   Use this when you need stdout/stderr for troubleshooting.
REM ----------------------------------------------------------------------

call "%~dp0check-build.bat"
if errorlevel 1 (
    echo RecipeManager.jar is missing or older than the sources. Running build first...
    call "%~dp0build.bat" nopause
    if errorlevel 1 exit /b 1
)

echo === Launching with console attached. Close the GUI to exit. ===
java -jar RecipeManager.jar

echo.
echo === Application exited. ===
pause
endlocal
