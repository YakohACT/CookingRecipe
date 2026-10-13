@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\..\"

REM ----------------------------------------------------------------------
REM   Recipe Manager - Whisper transcription auto-setup
REM
REM   Downloads everything needed to transcribe YouTube videos locally
REM   when the original captions are missing:
REM     - yt-dlp.exe        (audio downloader)
REM     - ffmpeg.exe         (audio re-encoding to 16kHz mono WAV)
REM     - whisper.cpp binary (speech-to-text)
REM     - ggml whisper model (base, ~140MB - balanced speed/accuracy)
REM
REM   All artefacts are placed under lib\tools\ and lib\models\.
REM   Safe to re-run: existing files are skipped.
REM ----------------------------------------------------------------------

REM curl is built into Windows 10+ (1803+)
where curl >nul 2>&1
if errorlevel 1 (
    echo [ERROR] curl not found. Windows 10 1803 or later is required.
    pause
    exit /b 1
)
where powershell >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PowerShell not found.
    pause
    exit /b 1
)

if not exist lib\tools  mkdir lib\tools
if not exist lib\models mkdir lib\models

REM === 1. yt-dlp =====================================================
echo [1/4] yt-dlp ...
if exist lib\tools\yt-dlp.exe (
    echo       skip ^(already exists^)
) else (
    curl -L --fail -o lib\tools\yt-dlp.exe ^
        https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe
    if errorlevel 1 goto fail
)

REM === 2. ffmpeg =====================================================
echo [2/4] ffmpeg ...
if exist lib\tools\ffmpeg.exe (
    echo       skip ^(already exists^)
) else (
    curl -L --fail -o ffmpeg.zip ^
        https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip
    if errorlevel 1 goto fail
    powershell -NoProfile -Command "Expand-Archive -Path 'ffmpeg.zip' -DestinationPath 'ffmpeg-tmp' -Force"
    powershell -NoProfile -Command "Get-ChildItem -Path 'ffmpeg-tmp' -Recurse -Filter ffmpeg.exe | Select-Object -First 1 | Copy-Item -Destination 'lib\tools\'"
    if not exist lib\tools\ffmpeg.exe (
        echo [ERROR] ffmpeg.exe not found in archive.
        rmdir /s /q ffmpeg-tmp
        del ffmpeg.zip
        goto fail
    )
    rmdir /s /q ffmpeg-tmp
    del ffmpeg.zip
)

REM === 3. whisper.cpp =================================================
echo [3/4] whisper.cpp ...
if exist lib\tools\whisper-cli.exe (
    echo       skip ^(already exists^)
) else (
    REM Pin to a known-good release (v1.7.6, Oct 2024).
    REM Update this URL when a newer release is desired.
    set WHISPER_VER=v1.7.6
    curl -L --fail -o whisper-bin.zip ^
        "https://github.com/ggml-org/whisper.cpp/releases/download/!WHISPER_VER!/whisper-bin-x64.zip"
    if errorlevel 1 goto fail
    powershell -NoProfile -Command "Expand-Archive -Path 'whisper-bin.zip' -DestinationPath 'whisper-tmp' -Force"
    REM Layout in the zip: Release\whisper-cli.exe + DLLs (or top-level depending on version)
    REM Copy only whisper-cli.exe and the DLLs it needs (whisper.dll / ggml*.dll).
    REM Other tools in the zip (server, stream, bench, SDL2.dll ...) are not used by the app.
    powershell -NoProfile -Command "Get-ChildItem -Path 'whisper-tmp' -Recurse | Where-Object { $_.Name -eq 'whisper-cli.exe' -or $_.Name -eq 'whisper.dll' -or $_.Name -like 'ggml*.dll' } | Copy-Item -Destination 'lib\tools\' -Force"
    if not exist lib\tools\whisper-cli.exe (
        echo [ERROR] whisper-cli.exe not found in archive.
        rmdir /s /q whisper-tmp
        del whisper-bin.zip
        goto fail
    )
    rmdir /s /q whisper-tmp
    del whisper-bin.zip
)

REM === 4. Whisper model ==============================================
echo [4/4] ggml-base.bin (~140MB) ...
if exist lib\models\ggml-base.bin (
    echo       skip ^(already exists^)
) else (
    curl -L --fail -o lib\models\ggml-base.bin ^
        https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin
    if errorlevel 1 goto fail
)

echo.
echo ============================================================
echo   [OK] Whisper setup complete.
echo   - The app will now auto-transcribe YouTube videos
echo     when subtitles are unavailable.
echo   - First run after install can be slow (model load).
echo ============================================================
echo.
pause
exit /b 0

:fail
echo.
echo [ERROR] setup failed. See messages above.
pause
exit /b 1
