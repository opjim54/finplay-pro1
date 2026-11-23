# Professional 3D Carrom Board Game Design Document

## 1. Intent & Executive Summary
The goal is to introduce a premium, realistic, colorful 3D Carrom Board game to FinPlay Pro's Game Arena section. The game must match the high production values of popular mobile carrom games (e.g. Carrom Pool), supporting:
1. **Free Practice Mode**: Zero coin requirement, allowing users to practice aim, rebounds, and trick shots.
2. **Offline 2-Player Pass & Play**: Local offline multiplayer on a single device between two players.
3. **Smart Bot AI**: Single player vs configurable AI bot with accuracy scaling.
4. **Tournament Mode**: Seamlessly integrated into FinPlay Pro's tournament tiers (Practice, Bronze, Silver, Gold, Platinum) with real wallet entry fees, win rewards, and ranking.
5. **Admin Panel Control**: 100% connected to FinPlay Pro's Admin Dashboard for tier pricing, bot accuracy defaults, commission rates, and live tournament launches.

## 2. Goals, Non-Goals, and Anti-Goals
### Goals
- Realistic 2D/3D physics simulation: Elastic disk-on-disk collisions, cushion bounce with restitution & friction, boric powder surface drag, and pocket sink dynamics.
- High-fidelity visual presentation: Beveled wooden board frame, mahogany/teak varnished board surface, concentric floral mandalas, 4 drop-corner pockets, and 3D depth-shaded pieces (9 White, 9 Black, 1 Red Queen, 1 Striker) with realistic specular highlights and drop shadows.
- Interactive Aim & Shot Controls: Smooth baseline striker positioning slider, drag-to-aim with cushion bounce trajectory line, and dynamic power bar with color gradients.
- Comprehensive Game Rules: Standard carrom rules including Queen cover, striker foul penalties, pocket scoring (White = 10 pts, Black = 5 pts, Queen = 25 pts), and turn rotation.
- Complete Integration: Admin panel tier configuration, bot accuracy adjustments, leaderboard records, sound engine SFX, and Firestore multiplayer/tournament models.

### Non-Goals
- Complex 3D camera pan/tilt during gameplay (top-down perspective with realistic 3D bevels, drop shadows, and depth lighting offers the optimal mobile touch control for carrom).
- Full 4-player doubles online matchmaking in phase 1 (phase 1 focuses on standard 1v1 singles for offline, bot, pass & play, and 1v1 tournament tiers).

### Anti-Goals
- Heavy external 3D engine libraries or heavy web assets that cause lag, loading screens, or frame drops on low-end devices.
- Sluggish touch response or floaty non-physical collisions that break player trust in tournament matches.

## 3. Constraints
- **Platform**: Android (Min SDK 24, Compile SDK 36, Kotlin 2.x, Jetpack Compose with Material 3).
- **Sound**: Must utilize `GameSoundEngine` (procedural AudioTrack synthesis) without requiring external audio files that could fail to load or bloat the APK.
- **Admin & Wallet Sync**: Must align with `PlayWalletRepository.ALL_INBUILT_GAME_IDS` and `TournamentGameType`.
- **Firebase & Security**: Must follow Firestore rules and blueprint constraints for `tournaments` and `rooms`.

## 4. Approaches Considered
- **Approach A: Embedded Three.js / WebGL Engine via WebView (like WarLand)**:
  - *Pros*: Ready-made 3D shaders and meshes.
  - *Cons*: WebView initialization overhead, touch latency between JS bridge and Android, higher memory footprint, potential white-screen flashes, and less cohesive UI integration with Compose theme.
- **Approach B: Native Jetpack Compose 3D Canvas & Physics Engine (Recommended & Selected)**:
  - *Pros*: Zero loading time, 60-120 FPS buttery smooth animation, instant touch responsiveness for striker drag & aim, direct state integration with Compose ViewModels, and zero external binary overhead.
  - *Cons*: Requires custom mathematical implementation of 2D/3D disk collision physics, trajectory raycasting, and 3D radial depth shading. (Fully accomplished in this design).

## 5. Game Architecture & Technical Specifications

### A. Carrom Board & Piece Layout
- **Coordinate Space**: Normalized square `[-1.0f, 1.0f]` with center at `(0, 0)`.
- **Pocket Positions**: Four corners at `(±0.88f, ±0.88f)` with radius `0.11f`.
- **Baseline Positions**:
  - Player 1 (Bottom): `y = 0.65f`, `x in [-0.55f, 0.55f]`.
  - Player 2 / Bot (Top): `y = -0.65f`, `x in [-0.55f, 0.55f]`.
- **Center Piece Arrangement**:
  - Queen (Red) at `(0, 0)`.
  - Ring 1 (Radius `0.09f`): 6 pieces alternating (White, Black, White, Black, White, Black).
  - Ring 2 (Radius `0.17f`): 12 pieces alternating.
  - Total: 9 White, 9 Black, 1 Red Queen.

### B. Physics & Collision Engine
- **Motion Update**: Velocity updated per frame with friction factor `friction = 0.985f` per tick.
- **Piece-to-Piece Collision**:
  - Distance check: `dist <= r1 + r2`.
  - Normal vector: `n = (p2 - p1) / dist`.
  - Elastic impulse: `j = -(1 + e) * (vRel . n) / (1/m1 + 1/m2)` where `e = 0.92f` (elasticity).
  - Mass: Striker mass = 3.0, Carrom Men mass = 1.0.
- **Cushion Bounce**:
  - Left/Right boundary: `abs(x) >= 0.88f - radius` -> reverse `vx`, apply restitution `0.85f`.
  - Top/Bottom boundary: `abs(y) >= 0.88f - radius` -> reverse `vy`, apply restitution `0.85f`.
- **Pocket Sinking**:
  - Distance from corner pocket `< pocketRadius`.
  - Piece decelerates, scales down to 0, and transitions to `isPocketed = true`.

### C. Game Engine & State Management (`CarromEngine.kt`)
- `CarromPieceType`: `WHITE`, `BLACK`, `QUEEN`, `STRIKER`.
- `CarromPiece`: `id, type, x, y, vx, vy, radius, isPocketed, animScale`.
- `CarromTurn`: `PLAYER_1`, `PLAYER_2_OR_BOT`.
- `CarromGameState`:
  - `pieces`: List of 19 carrom men + 1 striker.
  - `player1Score`, `player2Score`.
  - `player1Color`, `player2Color`.
  - `queenStatus`: `ON_BOARD`, `POCKETED_PENDING_COVER`, `COVERED_BY_P1`, `COVERED_BY_P2`.
  - `turn`: Active player.
  - `phase`: `AIMING`, `MOVING`, `ROUND_OVER`.
  - `isVsBot`, `botAccuracy`.
- `CarromBotAi`:
  - Evaluates legal target pieces (bot's color or Queen).
  - Finds the best pocket angle, calculates aim vector from striker baseline to target coin to pocket.
  - Adds controlled Gaussian noise proportional to `(100 - botAccuracy)`.

### D. Presentation & 3D Colorful UI (`CarromBoardScreen.kt`)
- **Wooden Frame**: Deep rosewood / mahogany beveled borders with metallic corner caps.
- **Board Face**: Polished satin wood finish with amber floral center mandala, outer concentric rings, arrows pointing to corner pockets, and baseline guide circles.
- **3D Shaded Discs**:
  - Radial gradient shading + specular light reflection at top-left gives tactile 3D convex disc appearance.
  - Dynamic soft drop shadow under each coin based on simulated light source.
  - Colorful striker with metallic rim and luminous neon center.
- **Interactive Controls**:
  - Horizontal Striker Slider along active baseline.
  - Drag-back Slingshot aiming with trajectory guide line and cushion bounce prediction.
  - Dynamic colorful power meter (Cyan -> Yellow -> Red) indicating shot impulse.
- **VFX & Animations**:
  - Sunk coin golden burst particles.
  - Sunk Queen ruby star sparkle.
  - Floating score toasts (+10, +5, QUEEN +25).
  - Match victory dialog with tournament coin awards.

### E. Admin Panel & Ecosystem Integration
- `ArenaGameId.CARROM`: Integrated in `GameLocalizedStrings`, `GamesArenaScreen`, `GameLeaderboardComponent`.
- `PlayWalletRepository.ALL_INBUILT_GAME_IDS`: Adding `CARROM` automatically provisions default tournament tiers:
  - Practice (Entry: ৳0, Win: ৳0)
  - Bronze (Entry: ৳20, Win: ৳36)
  - Silver (Entry: ৳50, Win: ৳90)
  - Gold (Entry: ৳100, Win: ৳185)
  - Platinum (Entry: ৳250, Win: ৳460)
- `AdminSuperComponents.kt`: Admin can configure Carrom tiers, set Bot Accuracy % slider, change seat limits, and launch official tournaments.
- `TournamentModel.kt` & `FirestoreTournamentRepository.kt`: `TournamentGameType.CARROM`.
- `firestore.rules` & `firebase-blueprint.json`: Add `CARROM` to allowed game enums.
