#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

echo "========================================================"
echo "  Net-CAR - Performance Vehicle Management & Telemetry"
echo "========================================================"
echo ""

if ! command -v node >/dev/null 2>&1; then
  echo "[ERROR] Node.js is not installed. Please install Node.js LTS from https://nodejs.org/"
  exit 1
fi

echo "[OK] Node.js detected ($(node -v))"

if [ ! -d "node_modules" ]; then
  echo "[1/3] Installing dependencies..."
  npm install
fi

if [ "$1" = "--dev" ]; then
  echo "[2/2] Starting Net-CAR in Development Mode on http://localhost:3000 ..."
  (sleep 4 && (xdg-open "http://localhost:3000" 2>/dev/null || open "http://localhost:3000" 2>/dev/null || true)) &
  npm run dev
  exit 0
fi

if [ ! -f "web/.next/BUILD_ID" ]; then
  echo "[2/3] Building optimized production bundle..."
  npm run build
fi

echo "[3/3] Starting Net-CAR on http://localhost:3000 ..."
(sleep 2 && (xdg-open "http://localhost:3000" 2>/dev/null || open "http://localhost:3000" 2>/dev/null || true)) &
npm run start
