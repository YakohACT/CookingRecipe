@echo off
setlocal
cd /d "%~dp0..\..\"

REM ----------------------------------------------------------------------
REM   Recipe Manager - build check (used by run.bat / run-debug.bat)
REM   exit code 1: RecipeManager.jar is missing, or a .java file under src\
REM                is newer than the JAR (build required)
REM   exit code 0: JAR is up to date
REM ----------------------------------------------------------------------

if not exist "RecipeManager.jar" exit /b 1

powershell -NoProfile -Command "$jar = (Get-Item 'RecipeManager.jar').LastWriteTime; $newer = Get-ChildItem -Path 'src' -Recurse -Filter '*.java' | Where-Object { $_.LastWriteTime -gt $jar } | Select-Object -First 1; if ($newer) { exit 1 } else { exit 0 }"
exit /b %errorlevel%
