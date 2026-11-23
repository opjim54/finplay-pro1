# 3D Colorful Carrom Board Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and integrate a professional, colorful 3D Carrom Board game with realistic physics, animations, Free Practice, Offline 2-Player, Smart Bot, Tournament Mode, and full Admin Panel connectivity into FinPlay Pro.

**Architecture:** Pure Kotlin & Jetpack Compose native 3D Canvas physics engine with realistic disk collisions, cushion restitution, friction decay, and pocket sinking animations. Fully integrated into FinPlay Pro's repository tier configs, tournament models, leaderboard, audio synthesizer, and Admin Dashboard.

**Tech Stack:** Kotlin 2.x, Android SDK 36, Jetpack Compose Material 3, Coroutines, StateFlow, Room DB, AudioTrack procedural sound synthesis, Firebase Firestore models.

**Spec:** `docs/superpowers/specs/2026-11-22-carrom-board-design.md`

## Global Constraints
- Android Min SDK: 24, Target SDK: 36.
- Pure native Kotlin/Compose implementation with zero external 3D libraries or heavy web assets.
- Sound effects must use `GameSoundEngine` procedural synthesis.
- Must preserve 100% backward compatibility with existing games (`LUDO`, `CHESS`, `AETHER_CROWN`, `REVERSI`, `CONNECT_FOUR`, `WAR_LAND`).
- Must support both English and Bengali locales.

## Review Focus
1. Striker positioning boundary: Striker cannot be positioned outside baseline limits or overlapping other pieces.
2. Queen cover rule: If Queen is pocketed, player must pocket a piece of their color on that shot or the immediate next shot, otherwise Queen returns to center.
3. Striker pocket foul: Sinking striker must return one of the player's previously pocketed pieces to the center circle.
4. Bot AI accuracy scaling: Bot shots should scale from casual amateur (low accuracy) to grandmaster (high accuracy) based on Admin Bot config.
5. Tournament wallet settlement: Real money / coins must be correctly deducted upon match start and win prizes credited on victory.

---

### Task 1: Core Carrom Physics Engine & AI Bot (`CarromEngine.kt` & Tests)

**Files:**
- Create: `app/src/main/java/com/example/domain/games/CarromEngine.kt`
- Create: `app/src/test/java/com/example/domain/games/CarromEngineTest.kt`

**Interfaces:**
- Produces:
  - `enum class CarromPieceType { WHITE, BLACK, QUEEN, STRIKER }`
  - `enum class CarromTurn { PLAYER_1, PLAYER_2 }`
  - `enum class CarromPhase { AIMING, MOVING, ROUND_OVER }`
  - `data class CarromPiece(...)`
  - `data class CarromGameState(...)`
  - `object CarromEngine { fun newGame(...): CarromGameState; fun updatePhysics(...): CarromGameState; fun shootStriker(...): CarromGameState }`
  - `object CarromBotAi { fun computeBotShot(state: CarromGameState, accuracyPercent: Int): Pair<Float, Offset> }`

- [ ] **Step 1: Write the failing unit tests for CarromEngine**
```kotlin
package com.example.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarromEngineTest {
    @Test
    fun newGame_hasCorrectPieceCountAndQueenAtCenter() {
        val state = CarromEngine.newGame(isVsBot = true, botAccuracy = 85)
        assertEquals(20, state.pieces.size) // 9 white, 9 black, 1 queen, 1 striker
        val queen = state.pieces.firstOrNull { it.type == CarromPieceType.QUEEN }
        assertNotNull(queen)
        assertEquals(0f, queen!!.x, 0.01f)
        assertEquals(0f, queen.y, 0.01f)
    }

    @Test
    fun shootStriker_impartsVelocityAndTransitionsToMoving() {
        val initial = CarromEngine.newGame(isVsBot = false)
        val shot = CarromEngine.shootStriker(initial, impulseX = 0f, impulseY = -1.2f)
        assertEquals(CarromPhase.MOVING, shot.phase)
        val striker = shot.pieces.first { it.type == CarromPieceType.STRIKER }
        assertTrue(striker.vy < -0.5f)
    }

    @Test
    fun physicsStep_deceleratesMovingPieces() {
        var state = CarromEngine.newGame(isVsBot = false)
        state = CarromEngine.shootStriker(state, impulseX = 0.5f, impulseY = -0.8f)
        val strikerInitialVy = state.pieces.first { it.type == CarromPieceType.STRIKER }.vy
        state = CarromEngine.updatePhysics(state, dt = 0.016f)
        val strikerNewVy = state.pieces.first { it.type == CarromPieceType.STRIKER }.vy
        assertTrue(Math.abs(strikerNewVy) < Math.abs(strikerInitialVy))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.example.domain.games.CarromEngineTest"`
Expected: FAIL with compilation error (unresolved CarromEngine).

- [ ] **Step 3: Implement `CarromEngine.kt`**
Implement full carrom physics:
- Setup 19 carrom men in concentric tournament layout (Queen in center, ring 1 with 6 pieces, ring 2 with 12 pieces) + 1 striker on baseline.
- Vectorized elastic disk-disk collision impulse resolution with mass weighting.
- Cushion wall collision with restitution and friction.
- Deceleration damping factor representing board powder.
- Corner pocket radius detection with smooth sink animation scaling.
- Carrom turn handling, Queen cover rule tracking, and Striker foul penalty.
- `CarromBotAi` raycasting best pocket trajectories for bot shots.

- [ ] **Step 4: Run test to verify it passes**
Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.example.domain.games.CarromEngineTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/domain/games/CarromEngine.kt app/src/test/java/com/example/domain/games/CarromEngineTest.kt
git commit -m "feat(game): add core 3D carrom physics engine, rules, and AI bot"
```

---

### Task 2: Data Models, Rules, and Admin Panel Ecosystem Integration

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/games/GameLocalizedStrings.kt`
- Modify: `app/src/main/java/com/example/data/games/TournamentModel.kt`
- Modify: `app/src/main/java/com/example/data/games/FirestoreTournamentRepository.kt`
- Modify: `app/src/main/java/com/example/data/games/FirestoreMultiplayerRepository.kt`
- Modify: `app/src/main/java/com/example/domain/games/PlayArmoryCatalog.kt`
- Modify: `app/src/main/java/com/example/data/games/PlayWalletRepository.kt`
- Modify: `firestore.rules`
- Modify: `firebase-blueprint.json`

**Interfaces:**
- Consumes: `ArenaGameId.CARROM`, `TournamentGameType.CARROM`
- Produces: Default tiers for CARROM in `PlayWalletRepository.ALL_INBUILT_GAME_IDS`, Admin dashboard UI support, localized strings.

- [ ] **Step 1: Write unit tests for tournament and repository game type registration**
Create `app/src/test/java/com/example/data/games/CarromEcosystemTest.kt` verifying:
- `TournamentGameType.fromWire("CARROM") == TournamentGameType.CARROM`
- `PlayWalletRepository.ALL_INBUILT_GAME_IDS.contains("CARROM")`
- `ArenaGameId.valueOf("CARROM")` exists and localized strings provide valid title, subtitle, and rules.

- [ ] **Step 2: Run test to verify failure**
Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.example.data.games.CarromEcosystemTest"`
Expected: FAIL.

- [ ] **Step 3: Implement ecosystem updates**
- Add `CARROM` to `ArenaGameId` enum in `GameLocalizedStrings.kt`.
- Add Bengali & English titles ("ক্যারম বোর্ড 3D", "Carrom Board 3D"), badges ("3D POPULAR"), subtitles, and full rule sections to `GameLocalizedStrings.kt`.
- Add `CARROM("CARROM", "Carrom 3D")` to `TournamentGameType` in `TournamentModel.kt`.
- Add `"CARROM"` to `ALL_INBUILT_GAME_IDS` in `PlayWalletRepository.kt`.
- Add `"CARROM"` to `ALLOWED_WIRE_VALUES` and normalize methods in `FirestoreTournamentRepository.kt` and `FirestoreMultiplayerRepository.kt`.
- Add `"CARROM"` to `firestore.rules` and `firebase-blueprint.json` gameType enums.
- Add `CARROM` skin catalog definitions in `PlayArmoryCatalog.kt`.

- [ ] **Step 4: Run test to verify it passes**
Run: `cmd /c gradlew.bat testDebugUnitTest --tests "com.example.data.games.CarromEcosystemTest"`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/games/GameLocalizedStrings.kt app/src/main/java/com/example/data/games/TournamentModel.kt app/src/main/java/com/example/data/games/FirestoreTournamentRepository.kt app/src/main/java/com/example/data/games/FirestoreMultiplayerRepository.kt app/src/main/java/com/example/domain/games/PlayArmoryCatalog.kt app/src/main/java/com/example/data/games/PlayWalletRepository.kt firestore.rules firebase-blueprint.json app/src/test/java/com/example/data/games/CarromEcosystemTest.kt
git commit -m "feat(ecosystem): wire CARROM to wallet tiers, tournament repository, and admin panel"
```

---

### Task 3: Professional 3D Colorful UI & Animations Screen (`CarromBoardScreen.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/screens/games/CarromBoardScreen.kt`

**Interfaces:**
- Consumes: `CarromEngine`, `CarromGameState`, `GameSoundEngine`, `UniversalBotControlBar`
- Produces: `@Composable fun CarromBoardScreen(onRecordMatch: (GameMatchResult, Int, Boolean, Int) -> Unit = { _, _, _, _ -> }, onExit: () -> Unit = {})`

- [ ] **Step 1: Build the UI component structure**
- **3D Wooden Frame & Cushions**: Rich teak/mahogany border with beveled metallic corner brackets and deep circular drop pockets with shadow gradients.
- **Tournament Board Face**: Smooth satin wood texture with central floral mandala, concentric rings, diagonal arrows pointing directly to corner pockets, and baseline guide circles.
- **Realistic 3D Discs Drawing**:
  - Radial gradient shading with top-left highlight and bottom-right shadow, rendering pieces as tangible 3D convex wooden/acrylic disks.
  - Drop shadow under each coin that shifts slightly with position for simulated depth.
  - White Men: Ivory pearlescent glow with golden edge ring.
  - Black Men: Obsidian ebony finish with warm bronze edge ring.
  - Red Queen: Radiant crimson ruby with golden mandala star.
  - Striker: Metallic silver/gold with concentric grooves and glowing neon reticle.
- **Interactive Controls**:
  - Baseline slider to position striker horizontally within legal boundaries.
  - Pull-back slingshot gesture with dynamic aiming line, cushion bounce reflection vector, and color-coded power meter (Cyan -> Amber -> Neon Red).
  - Realistic release animation triggering physics motion.
- **VFX & Animations**:
  - Sunk coin golden sparkle bursts.
  - Sunk Queen star burst.
  - Floating score indicators (+10 White, +5 Black, +25 Queen, Foul Penalty).
  - Victory celebration dialog with trophy and score breakdown.
- **Audio Synthesizer Integration**:
  - Hook striker impact to `GameSoundEffect.TAP`.
  - Hook coin-to-coin clack to `GameSoundEffect.MOVE`.
  - Hook cushion bounce to `GameSoundEffect.CAPTURE`.
  - Hook pocket sink to `GameSoundEffect.SPECIAL`.
  - Hook win celebration to `GameSoundEffect.WIN`.

- [ ] **Step 2: Implement modes**
- Free Practice Mode (infinite play, quick reset button).
- Offline 2-Player Pass & Play (alternating turns on same device).
- Vs Smart Bot (with bot accuracy % control bar and thinking delay).
- Tournament mode integration (score submission and match recording).

- [ ] **Step 3: Run project compilation check**
Run: `cmd /c gradlew.bat compileDebugKotlin`
Expected: Compilation successful with no errors.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/games/CarromBoardScreen.kt
git commit -m "feat(ui): implement professional 3D colorful Carrom board with realistic physics and controls"
```

---

### Task 4: Games Arena Hub & Admin Dashboard Connection

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/games/GamesArenaScreen.kt`

**Interfaces:**
- Consumes: `CarromBoardScreen`, `ArenaGameId.CARROM`
- Produces: Carrom card in Arena Games list, tier selection, match fee deduction and prize settlement, rules modal dialog.

- [ ] **Step 1: Update `visualsFor(ArenaGameId.CARROM)` in `GamesArenaScreen.kt`**
- Set icon to `Icons.Default.SportsEsports` or Carrom-themed icon.
- Set vibrant amber/crimson/emerald gradient background colors.
- Set recommended armory skill item.

- [ ] **Step 2: Add `ArenaGameId.CARROM -> CarromBoardScreen(...)` to game renderers**
- Wire `settleAndRecord` callback so tournament entry fee and win prize are settled automatically to player's wallet upon match completion.
- Support exit back to Arena hub.

- [ ] **Step 3: Verify Admin Dashboard displays Carrom**
- Ensure `selectedGameId` dropdown in Admin tab includes Carrom.
- Ensure tier creation and bot accuracy sliders work smoothly for Carrom.

- [ ] **Step 4: Run compilation and test verification**
Run: `cmd /c gradlew.bat compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/ui/screens/games/GamesArenaScreen.kt
git commit -m "feat(hub): connect Carrom Board 3D to Arena Hub, wallet settlement, and Admin dashboard"
```

---

### Task 5: End-to-End Verification & Validation

**Files:**
- All modified and new files.

- [ ] **Step 1: Run full unit test suite**
Run: `cmd /c gradlew.bat testDebugUnitTest`
Expected: ALL PASS.

- [ ] **Step 2: Verify lint and build health**
Run: `cmd /c gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit and summarize**
```bash
git commit -m "chore(release): finalize 3D Carrom Board integration across app and admin panel"
```
