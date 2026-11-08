@echo off
setlocal enabledelayedexpansion
title Net-CAR - Vehicle Management & Telemetry OS

echo ========================================================
echo   Net-CAR - Performance Vehicle Management ^& Telemetry
echo ========================================================
echo.

:: Change to the directory where this script is located
cd /d "%~dp0"

:: 1. Verify Node.js is installed
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not installed or not in your PATH.
    echo [ERREUR] Node.js n'est pas installe sur ce PC.
    echo.
    echo Please download and install Node.js LTS from:
    echo Veuillez telecharger et installer Node.js LTS depuis :
    echo   https://nodejs.org/
    echo.
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('node -v') do set NODE_VER=%%i
echo [OK] Node.js detected (%NODE_VER%)

:: 2. Install dependencies if node_modules is missing
if not exist "node_modules\" (
    echo.
    echo [1/3] Installing dependencies (first-time setup)...
    echo       Installation des dependances ^(premiere execution^)...
    call npm install
    if %errorlevel% neq 0 (
        echo.
        echo [ERROR] Failed to install dependencies.
        pause
        exit /b 1
    )
) else (
    echo [OK] Dependencies already installed.
)

:: 3. Build optimized production bundle if not yet built
if not exist "web\.next\BUILD_ID" (
    echo.
    echo [2/3] Building optimized application for fast PC performance...
    echo       Compilation optimisee de l'application...
    call npm run build
    if %errorlevel% neq 0 (
        echo.
        echo [WARN] Production build failed, falling back to development server...
        goto :run_dev
    )
)

:: 4. Launch browser automatically once server starts and run production server
echo.
echo [3/3] Starting Net-CAR on http://localhost:3000 ...
echo       Ouverture automatique du navigateur sur http://localhost:3000 ...
echo       ^(Press Ctrl+C to stop / Appuyez sur Ctrl+C pour arreter^)
echo.
start "" cmd /c "timeout /t 3 /nobreak >nul & start http://localhost:3000"
call npm run start
pause
exit /b 0

:run_dev
echo.
echo Starting Net-CAR (Development Mode) on http://localhost:3000 ...
start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:3000"
call npm run dev
pause
