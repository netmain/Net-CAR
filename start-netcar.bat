@echo off
title Net-CAR - Fast Production Mode (Low RAM)
cd /d "%~dp0"

echo ========================================================
echo   Net-CAR - Vehicle Management and Telemetry OS
echo ========================================================
echo.

where node >nul 2>nul
if errorlevel 1 goto :no_node

echo [OK] Node.js found:
node -v
echo.

if exist "node_modules" goto :check_build
echo [Step 1] Installing dependencies...
call npm install
if errorlevel 1 goto :error_install

:check_build
if exist "web\.next\win32_ready.txt" goto :start_prod
echo.
echo [Step 2] Building optimized production bundle (one-time setup for low RAM and instant speed)...
if exist "web\.next" rmdir /s /q "web\.next"
call npm run build --workspace=web
if errorlevel 1 goto :fallback_dev
echo ok > "web\.next\win32_ready.txt"

:start_prod
echo.
echo [Step 3] Starting Net-CAR Fast Server on http://localhost:3000 ...
echo Memory usage in this mode is ~80 MB RAM (vs 1.8 GB in dev mode).
echo Opening browser in 2 seconds...
echo.
start "" cmd /c "timeout /t 2 /nobreak >nul & explorer http://localhost:3000"
call npm run start --workspace=web
if errorlevel 1 goto :fallback_dev
pause
exit /b 0

:fallback_dev
echo.
echo Starting Net-CAR in Development Mode on http://localhost:3000 ...
start "" cmd /c "timeout /t 5 /nobreak >nul & explorer http://localhost:3000"
call npm run dev --workspace=web
pause
exit /b 0

:no_node
echo [ERROR] Node.js is not installed on this PC.
echo Please download and install Node.js LTS from: https://nodejs.org/
echo.
pause
exit /b 1

:error_install
echo.
echo [ERROR] npm install failed. Check your internet connection and try again.
echo.
pause
exit /b 1
