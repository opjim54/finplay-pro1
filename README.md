# FinPlay Pro - All-in-One Super App & 3D Games Arena

FinPlay Pro is an enterprise-grade Android application combining **Smart Finance & Wealth Hub**, **Notes & Tasks Studio**, **Smart Pro Tools**, and a full **WebGL & Three.js 3D/2D Games Arena** featuring **Warland 3D Battle Royale**, **Carrom**, **Ludo Master**, and **Fruit Slash**.

---

## 🚀 What's New: Warland 3D Complete Rebuild (A-to-Z Upgrades)

The embedded 3D Battle Royale game **Warland** has undergone an extensive engineering rebuild:

1. **Enterable Multi-Story Buildings & Walkable Ramps:**
   - **Continuous Walkable Ramps (`walkableRamps`):** Smooth ramp height interpolation ($y = y_{start} + t \times (y_{end} - y_{start})$) with `isStairs: true` flag, allowing smooth running up and down staircases without getting caught on step lips.
   - **Enterable Villa (2 Floors + Rooftop):** Wide 3.2m entrance, Ground-to-2nd-Floor staircase, 2nd-Floor-to-Rooftop staircase, stairwell ceiling cutouts, 1.0m parapet walls with sniper slits, and elevated vantage crates.
   - **Enterable 3-Story Townhouses:** Ground $\to$ Floor 2 (Y = 3.8m) $\to$ Floor 3 (Y = 7.6m) $\to$ Rooftop Observation Deck (Y = 11.4m).
   - **Military Barracks & Watchtowers:** Dual-ended entrances, exterior steel roof access ramp (Y = 4.2m), and watchtower ramps leading to 12.0m sniper perches.

2. **Lag-Free 60 FPS Spatial Grid Optimization:**
   - **25-Meter Spatial Hash Grid:** Reduced obstacle collision queries from ~7,800 to ~150 checks per frame (>98% overhead reduction).
   - **Foliage Shadow Optimization:** Disabled shadow casting on vegetation and rocks (`castShadow = false`, `frustumCulled = true`), saving dozens of draw calls per frame on mobile GPUs.
   - **Device Pixel Ratio Capping:** Clamped `devicePixelRatio` to max 1.5x to eliminate mobile fillrate bottlenecks.

3. **Locomotion Animation Overhaul (Root-Motion Drift Fix):**
   - **v3.0 Animation System:** Automatically neutralizes translation drift on `mixamorig:Hips.position` across all locomotion clips (`WALK`, `RUN`, `SPRINT`, `CROUCH_WALK`, `PRONE_MOVE`), eliminating character snapping/rubberbanding.
   - **Unified Animation Authority:** Centralized animation mixer management through `AnimationSystem`.

4. **Precision Weapon Ballistics:**
   - Corrected raycast forward vector to align with the camera frustum:
     $$\vec{V} = (-\sin(\text{rotY}), \tan(-\text{pitch}), -\cos(\text{rotY}))$$
   - Bullets now fire straight through the center crosshair with pinpoint accuracy.

---

## 📱 Application Modules & Architecture

- **Smart Finance & Wealth Hub:**
  - Net worth tracker, Income & Expense ledgers, Cashflow analysis.
  - Interactive charts, monthly budget planning, and spending breakdown.
  - CSV/PDF export and Google Drive Cloud Vault synchronization.
- **Games Arena:**
  - **Warland 3D Battle Royale** (Three.js WebGL, 30-player battle royale simulation, bots, airdrops, safe zones).
  - **Classic Carrom** (2D physics engine, pocketing, foul detection, AI bot opponent).
  - **Ludo Master** (4-player local and vs-bot board, 3D dice rolling, animated pawns).
  - **Fruit Slash** (High-velocity touch slicing physics, combos, fruit splatters).
  - **Chess Master, Connect Four, Reversi, Snakes & Ladders, Aether Crown**.
  - **Esports Tournaments, Play Armory, and Play Wallet**.
- **Notes & Tasks Studio:**
  - Markdown notes with categories, tags, and pinned items.
  - Tasks Pro checklist with due dates and priority filters.
  - Gemini AI assistant for automatic summarization and task planning.
- **Smart Tools:**
  - Loan EMI, Compound Interest, Unit Converters, and QR Code scanner/generator.
  - Stylish Text Studio for typography design.
- **Security:**
  - Biometric authentication (Fingerprint / Face Unlock).
  - 4-digit PIN lock screen and encrypted SQLite Room database.

---

## 🛠️ How to Build the Android APK

### Option 1: One-Click Windows Builder
Run the automated builder script:
```cmd
build_apk.bat
```
Select `[1] Build DEBUG APK`. The script auto-detects JDK and Android SDK, triggers Gradle, and displays the output APK.

### Option 2: Android Studio
1. Open **Android Studio** (Hedgehog or newer).
2. Click **File -> Open...** and select this directory.
3. Select **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.

### Option 3: Command Line Gradle
- **Windows:**
  ```cmd
  gradlew.bat assembleDebug
  ```
- **Linux / macOS:**
  ```bash
  chmod +x ./gradlew
  ./gradlew assembleDebug
  ```
- Output APK location:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 🌐 Running Web Games Locally

All web games can also be played and tested in your web browser:
```bash
node server.js
```
Open in browser:
- **Warland 3D:** [http://localhost:3000/warland/index.html](http://localhost:3000/warland/index.html)
- **Carrom:** [http://localhost:3000/carrom/index.html](http://localhost:3000/carrom/index.html)
- **Ludo:** [http://localhost:3000/ludo/index.html](http://localhost:3000/ludo/index.html)
- **Fruit Slash:** [http://localhost:3000/fruitslash/index.html](http://localhost:3000/fruitslash/index.html)

---

## 📄 Documentation Links
- [Detailed Build Instructions](BUILD_INSTRUCTIONS.md)
- [Warland 3D Technical Changelog](WARLAND_UPGRADE_CHANGELOG.md)
