@echo off
setlocal enabledelayedexpansion
title Net-CAR - Development Server (Hot Reload)

echo ========================================================
echo   Net-CAR - Development Mode (Live Reload)
echo ========================================================
echo.

cd /d "%~dp0"

where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not installed. Download it from https://nodejs.org/
    pause
    exit /b 1
)

if not exist "node_modules\" (
    echo Installing dependencies...
    call npm install
)

echo.
echo Starting Next.js development server on http://localhost:3000 ...
echo Opening browser in 5 seconds...
echo.
start "" cmd /c "timeout /t 5 /nobreak >nul & start http://localhost:3000"
call npm run dev
pause
