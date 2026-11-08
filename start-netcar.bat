@echo off
title Net-CAR - Vehicle Management and Telemetry OS
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
if exist "web\.next\BUILD_ID" goto :start_server
echo.
echo [Step 2] Building optimized app for fast PC performance...
call npm run build
if errorlevel 1 goto :fallback_dev

:start_server
echo.
echo [Step 3] Starting Net-CAR server on http://localhost:3000 ...
echo Your browser will open automatically in 3 seconds.
echo Keep this window open while using the app.
echo.
start "" cmd /c "timeout /t 3 /nobreak >nul & explorer http://localhost:3000"
call npm run start
if errorlevel 1 goto :fallback_dev
pause
exit /b 0

:fallback_dev
echo.
echo Starting Net-CAR in Development Mode on http://localhost:3000 ...
echo Please wait 10 seconds after the browser opens for the first page to compile.
echo.
start "" cmd /c "timeout /t 5 /nobreak >nul & explorer http://localhost:3000"
call npm run dev
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
