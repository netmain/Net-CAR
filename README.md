# 🚗 Net-CAR

A bilingual (English/French) car management app for Android and Web.

Track fuel consumption, expenses, maintenance services, and set reminders — all synced across devices with Firebase.

## Features

- 🚘 Multi-vehicle management
- ⛽ Fuel tracking with economy calculations
- 💰 Expense tracking with categories and receipt photos
- 🔧 Maintenance service log
- ⏰ Smart reminders (by date, mileage, or both)
- 📊 Reports and charts
- 🌍 English & French
- 🔄 Real-time sync via Firebase
- 💾 Google Drive backup

## Tech Stack

| Platform | Technology |
|----------|-----------|
| Android  | Kotlin + Jetpack Compose + Material 3 |
| Web      | Next.js 14 + TypeScript + Tailwind CSS |
| Backend  | Firebase (Auth, Firestore, Functions, Storage) |
| Backup   | Google Drive API |

## Project Structure

```
Net-CAR/
├── android/     # Android app
├── web/         # Next.js web app
├── firebase/    # Firebase configuration
└── docs/        # Documentation
```

## Getting Started / Lancer sur PC

### 🖥️ Quick Launch on PC (One-Click)

1. **Prerequisite**: Install **[Node.js LTS](https://nodejs.org/)** (v18 or v20+) on your PC if not already installed.
2. **Windows (Double-Click)**:
   - Double-click **`start-netcar.bat`** in the project root folder.
   - On first run, it automatically installs dependencies (`npm install`), builds the optimized production app (`npm run build`), starts the server on `http://localhost:3000`, and opens your browser automatically.
   - For live-reload development mode, double-click **`start-netcar-dev.bat`**.
3. **macOS / Linux**:
   ```bash
   chmod +x start-netcar.sh
   ./start-netcar.sh
   ```

### 🛠️ Manual Terminal Commands (Web)
The root directory is configured as a monorepo workspace supporting **npm**, **bun**, and cloud IDEs:

```bash
# Install dependencies (first time only):
npm install

# Option A — Fast Production Mode (recommended for daily use):
npm run build
npm run start

# Option B — Development Mode (with hot-reload):
npm run dev
```
Then open **[http://localhost:3000](http://localhost:3000)** in your browser.

### Android
Open `android/` in Android Studio and run on device/emulator.

### Firebase
```bash
cd firebase
firebase deploy
```

## License

Private — All rights reserved.
