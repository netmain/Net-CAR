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

## Getting Started

### Web (Root / AI Studio / Bun / npm)
The root directory is configured as a monorepo workspace supporting **npm**, **bun**, and cloud IDEs like **Google AI Studio**:

```bash
# From project root:
npm install        # or: bun install
npm run dev        # or: bun run dev
```

Or from the `web/` subfolder:
```bash
cd web
npm install
npm run dev
```

### Android
Open `android/` in Android Studio and run on device/emulator.

### Firebase
```bash
cd firebase
firebase deploy
```

## License

Private — All rights reserved.
