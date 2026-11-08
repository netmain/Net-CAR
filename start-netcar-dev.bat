@echo off
title Net-CAR - Development Server
cd /d "%~dp0"

echo ========================================================
echo   Net-CAR - Development Mode
echo ========================================================
echo.

where node >nul 2>nul
if errorlevel 1 goto :no_node

if exist "node_modules" goto :run_dev
echo Installing dependencies...
call npm install

:run_dev
echo.
echo Starting Next.js server on http://localhost:3000 ...
echo Opening browser in 6 seconds (first load compiles the page, please wait ~10s)...
echo.
start "" cmd /c "timeout /t 6 /nobreak >nul & explorer http://localhost:3000"
call npm run dev
pause
exit /b 0

:no_node
echo [ERROR] Node.js is not installed. Download it from https://nodejs.org/
pause
exit /b 1
