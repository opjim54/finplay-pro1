# FinPlay Pro - Standalone Android APK Build Package

Welcome to the standalone, self-contained **FinPlay Pro** Android application build package. This folder contains all source code, dependencies, asset pipelines, embedded 3D and 2D games, configuration files, and build scripts required to build the complete FinPlay Pro APK.

---

## 📱 App Highlights & Sections Included

FinPlay Pro is an all-in-one super application engineered with Jetpack Compose, Material 3, and WebGL/Three.js:

1. **Smart Finance & Wealth Hub**:
   - Live Wallet balance, Income/Expense tracking, and Cashflow projections.
   - Comprehensive Analytics & category-wise breakdown charts.
   - Export to CSV/PDF reports and Google Drive Cloud Vault synchronization.

2. **Games Arena (Full Portfolio)**:
   - **Warland 3D Battle Royale**: Rebuilt from the ground up with Three.js WebGL:
     - Enterable multi-story buildings (Villa, Townhouses, Barracks, Watchtowers).
     - Smooth ramp stair climbing system with zero collision clipping.
     - Rooftop observation decks and tactical sniper vantage perches.
     - Root-motion neutralized locomotion animations (eliminating running snapping/rubberbanding).
     - True forward crosshair bullet ballistics with zero angular offset.
     - 25-meter spatial grid collision acceleration delivering locked 60 FPS performance.
   - **Classic Carrom**: Realistic 2D physics, striker control, coin sliding dynamics, and bot AI.
   - **Ludo Master**: 4-player classic board, 3D dice rolling, animated pawn paths, and sound effects.
   - **Fruit Slash**: High-velocity blade slicing mechanics, combo counters, and fruit splatters.
   - **Chess Master & Board Games**: Chess, Connect Four, Reversi, Snakes & Ladders, and Aether Crown.
   - **Esports Tournaments & Armory**: Match history, custom weapon skins, and Play Wallet integration.

3. **Notes & Tasks Studio**:
   - Rich Markdown Note Editor with tags, categories, and pinning.
   - Tasks Pro checklist with due dates, priority labels, and reminders.
   - AI Assistant integration with Gemini-powered summary and auto-categorization.

4. **Smart Pro Tools**:
   - Financial & Daily Calculators (Loan EMI, Compound Interest, Unit Converters).
   - Smart Text & File Tools (PDF conversion, OCR scanner, QR Code generator/scanner).
   - Stylish Text Studio for typography design and custom fonts.

5. **Security & Authentication**:
   - Biometric fingerprint / Face unlock support.
   - 4-digit PIN security lock screen.
   - Firebase Authentication & Google Sign-In.

---

## 🚀 Quick Start: Building the APK

### Method 1: One-Click Build Script (Recommended for Windows)
1. Double-click **`build_apk.bat`** in this folder.
2. Select **`[1] Build DEBUG APK`**.
3. The script automatically verifies JDK and Android SDK, triggers Gradle, and opens the folder containing your generated `app-debug.apk`.

### Method 2: Android Studio (Standard GUI)
1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, Ladybug or newer).
2. Click **File -> Open...** and select this directory (`finplay_pro_full_apk_build`).
3. Allow Gradle to sync dependencies (configured with Gradle 9.3.1 and Android Gradle Plugin 8.9+).
4. Go to **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
5. Once complete, click **locate** in the popup notification to get your APK.

### Method 3: Command Line (Linux / macOS / Windows Terminal)
- **Windows (PowerShell or CMD)**:
  ```cmd
  gradlew.bat assembleDebug
  ```
- **macOS / Linux**:
  ```bash
  chmod +x ./gradlew
  ./gradlew assembleDebug
  ```
- The output APK will be generated at:
  ```
  app/build/outputs/apk/debug/app-debug.apk
  ```

---

## 🌐 Local Web Testing (Warland & Embedded Games)

All embedded games can also be tested instantly in your desktop web browser:
1. Run `node server.js` or choose option `[4]` in `build_apk.bat`.
2. Open your browser at:
   - **Warland 3D**: [http://localhost:3000/warland/index.html](http://localhost:3000/warland/index.html)
   - **Carrom**: [http://localhost:3000/carrom/index.html](http://localhost:3000/carrom/index.html)
   - **Ludo**: [http://localhost:3000/ludo/index.html](http://localhost:3000/ludo/index.html)
   - **Fruit Slash**: [http://localhost:3000/fruitslash/index.html](http://localhost:3000/fruitslash/index.html)

---

## 📁 Directory Structure

```
finplay_pro_full_apk_build/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml       # App permissions, activities, receivers
│   │   │   ├── java/com/example/         # Complete Jetpack Compose source code
│   │   │   │   ├── ui/screens/           # UI Screens (Dashboard, GamesArena, Notes, Tools)
│   │   │   │   ├── data/                 # Room Database, Repositories, Firebase
│   │   │   │   ├── domain/               # Business logic, Game engines, Calculators
│   │   │   │   └── security/             # Biometrics, App lock, KeyStore encryption
│   │   │   ├── res/                      # Icons, layouts, themes, XML resources
│   │   │   └── assets/                   # Embedded WebGL games (Warland, Carrom, Ludo, FruitSlash)
│   │   │       ├── warland/              # Rebuilt Warland 3D Engine & assets
│   │   │       ├── carrom/               # Carrom game assets
│   │   │       ├── ludo/                 # Ludo game assets
│   │   │       └── fruitslash/           # Fruit Slash game assets
│   │   └── test/                         # Unit tests
│   ├── build.gradle.kts                  # App module build configuration
│   ├── google-services.json              # Firebase project configuration
│   └── proguard-rules.pro                # R8 / ProGuard optimization rules
├── gradle/
│   ├── libs.versions.toml                # Dependency Version Catalog
│   └── wrapper/                          # Gradle wrapper properties (v9.3.1)
├── public/                               # Full standalone web mirror of all games
├── build.gradle.kts                      # Root Gradle configuration
├── settings.gradle.kts                   # Project module definitions
├── gradle.properties                     # JVM memory & AndroidX settings
├── build_apk.bat                         # Interactive Windows builder script
├── build_apk.sh                          # Interactive Linux/macOS builder script
├── gradlew.bat                           # Gradle wrapper for Windows
├── gradlew                               # Gradle wrapper for Unix
├── server.js                             # Zero-dependency local Node.js test server
└── README.md                             # Documentation & user guide
```
