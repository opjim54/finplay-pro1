package com.example.domain.games

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class LudoColor(val displayName: String, val startTrack: Int, val colorHex: Long) {
    RED("Emerald", 0, 0xFF059669),
    GREEN("Sapphire", 13, 0xFF0284C7),
    YELLOW("Gold", 26, 0xFFD97706),
    BLUE("Crimson", 39, 0xFFDC2626),
    PURPLE("Amethyst", 52, 0xFF9333EA)
}

enum class LudoPhase {
    WAIT_ROLL,
    WAIT_MOVE,
    GAME_OVER
}

data class LudoToken(
    val color: LudoColor,
    val index: Int,
    /**
     * -1 = Yard,
     * Classic 2P-4P: 0..50 = Main Outer Track (52 squares), 51..55 = Home Stretch, 56 = Finished Home Crown.
     * 5-Player Star: 0..63 = Main Outer Track (65 squares), 64..68 = Home Stretch, 69 = Finished Home Crown.
     */
    val progress: Int = -1
)

data class LudoGameState(
    val activeColors: List<LudoColor> = listOf(LudoColor.RED, LudoColor.YELLOW),
    val botColors: Set<LudoColor> = setOf(LudoColor.YELLOW),
    val tokens: Map<LudoColor, List<LudoToken>> = LudoEngine.initialTokens(listOf(LudoColor.RED, LudoColor.YELLOW)),
    val turnIndex: Int = 0,
    val phase: LudoPhase = LudoPhase.WAIT_ROLL,
    val diceValue: Int = 1,
    val consecutiveSixes: Int = 0,
    val winner: LudoColor? = null,
    val statusLog: String = "Roll the dice to begin"
) {
    val currentTurn: LudoColor get() = activeColors[turnIndex]
    val isFivePlayerBoard: Boolean get() = activeColors.size >= 5 || LudoColor.PURPLE in activeColors
    val outerTrackLength: Int get() = if (isFivePlayerBoard) LudoEngine.TRACK_LEN_5P else LudoEngine.TRACK_LEN_4P
    val maxOuterProgress: Int get() = outerTrackLength - 2
    val homeStretchStart: Int get() = outerTrackLength - 1
    val finishProgress: Int get() = outerTrackLength + 4
    val safeOuterIndices: Set<Int> get() = if (isFivePlayerBoard) LudoEngine.SAFE_OUTER_TRACK_INDICES_5P else LudoEngine.SAFE_OUTER_TRACK_INDICES
}

data class LudoStarCell(
    val armIndex: Int,
    val lane: Int, // -1 = left (outward), 0 = center (home/tip), +1 = right (inward)
    val row: Int,  // 0 (innermost near pentagon) .. 5 (outermost tip)
    val centerX: Float,
    val centerY: Float,
    val angleRad: Double
)

object LudoEngine {
    const val TRACK_LEN_4P = 52
    const val TRACK_LEN_5P = 65
    const val FINISH_PROGRESS_4P = 56
    const val FINISH_PROGRESS_5P = 69

    val SAFE_OUTER_TRACK_INDICES = setOf(0, 8, 13, 21, 26, 34, 39, 47)
    val SAFE_OUTER_TRACK_INDICES_5P = setOf(0, 8, 13, 21, 26, 34, 39, 47, 52, 60)

    // 5-Arm Pentagonal Star Geometry Constants (Normalized 0f..1f board space)
    const val STAR_CENTER = 0.5f
    const val STAR_INNER_RADIUS = 0.116f
    const val STAR_CELL_W = 0.043f
    const val STAR_CELL_H = 0.053f
    const val STAR_YARD_RADIUS_FROM_CENTER = 0.362f
    const val STAR_YARD_CIRCLE_RADIUS = 0.094f

    val FIVE_PLAYER_COLORS: List<LudoColor> = listOf(
        LudoColor.RED,
        LudoColor.GREEN,
        LudoColor.YELLOW,
        LudoColor.BLUE,
        LudoColor.PURPLE
    )

    /** Exact 52-square clockwise outer ring coordinates (col, row) on a 15x15 Ludo board. */
    val OUTER_TRACK_COORDS: List<Pair<Int, Int>> = listOf(
        1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6, // 0..4 (Red arm top)
        6 to 5, 6 to 4, 6 to 3, 6 to 2, 6 to 1, 6 to 0, // 5..10 (Top arm left)
        7 to 0, // 11 (Top middle)
        8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5, // 12..17 (Top arm right, 13 = Green start)
        9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 6, 14 to 6, // 18..23 (Right arm top)
        14 to 7, // 24 (Right middle)
        14 to 8, 13 to 8, 12 to 8, 11 to 8, 10 to 8, 9 to 8, // 25..30 (Right arm bottom, 26 = Yellow start)
        8 to 9, 8 to 10, 8 to 11, 8 to 12, 8 to 13, 8 to 14, // 31..36 (Bottom arm right)
        7 to 14, // 37 (Bottom middle)
        6 to 14, 6 to 13, 6 to 12, 6 to 11, 6 to 10, 6 to 9, // 38..43 (Bottom arm left, 39 = Blue start)
        5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8, // 44..49 (Left arm bottom)
        0 to 7, // 50 (Left middle)
        0 to 6  // 51 (Left top corner before Red start 0)
    )

    fun armAngleRad(armIndex: Int): Double = -PI / 2.0 + (armIndex % 5) * (2.0 * PI / 5.0)

    fun starArmCell(armIndex: Int, lane: Int, row: Int): LudoStarCell {
        val k = ((armIndex % 5) + 5) % 5
        val angle = armAngleRad(k)
        val ux = cos(angle)
        val uy = sin(angle)
        // Clockwise perpendicular vector in screen coordinates (x right, y down)
        val tx = -sin(angle)
        val ty = cos(angle)
        val r = STAR_INNER_RADIUS + (row + 0.5f) * STAR_CELL_H
        val lateral = lane * STAR_CELL_W
        val cx = (STAR_CENTER + r * ux + lateral * tx).toFloat()
        val cy = (STAR_CENTER + r * uy + lateral * ty).toFloat()
        return LudoStarCell(
            armIndex = k,
            lane = lane,
            row = row,
            centerX = cx,
            centerY = cy,
            angleRad = angle
        )
    }

    /**
     * Exact 65-square clockwise outer perimeter cells for the 5-Arm Pentagonal Star board.
     * Index `k * 13` (0, 13, 26, 39, 52) is Arm `k`'s Start Square (`lane = +1, row = 4`).
     */
    val FIVE_PLAYER_OUTER_CELLS: List<LudoStarCell> = buildList(TRACK_LEN_5P) {
        for (k in 0 until 5) {
            val nextArm = (k + 1) % 5
            // local 0..4: Arm k right lane going inward (row 4 -> 0); local 0 = Start Square
            for (r in 4 downTo 0) add(starArmCell(k, lane = +1, row = r))
            // local 5..10: Arm k+1 left lane going outward (row 0 -> 5); local 8 = Safe Star
            for (r in 0..5) add(starArmCell(nextArm, lane = -1, row = r))
            // local 11: Arm k+1 tip middle (turn-in square before home stretch)
            add(starArmCell(nextArm, lane = 0, row = 5))
            // local 12: Arm k+1 outer right corner before next start square
            add(starArmCell(nextArm, lane = +1, row = 5))
        }
    }

    fun fivePlayerHomeCell(color: LudoColor, homeStep: Int): LudoStarCell {
        // homeStep in 1..5 maps from outer row 4 inward to row 0
        val armIdx = FIVE_PLAYER_COLORS.indexOf(color).coerceAtLeast(0)
        val row = (5 - homeStep.coerceIn(1, 5)).coerceIn(0, 4)
        return starArmCell(armIdx, lane = 0, row = row)
    }

    fun fivePlayerYardCenter(color: LudoColor): Pair<Float, Float> {
        val armIdx = FIVE_PLAYER_COLORS.indexOf(color).coerceAtLeast(0)
        val yardAngle = armAngleRad(armIdx) + PI / 5.0 // 36 deg clockwise next to Start Square
        val cx = (STAR_CENTER + STAR_YARD_RADIUS_FROM_CENTER * cos(yardAngle)).toFloat()
        val cy = (STAR_CENTER + STAR_YARD_RADIUS_FROM_CENTER * sin(yardAngle)).toFloat()
        return cx to cy
    }

    fun fivePlayerYardCoords(color: LudoColor): List<Pair<Float, Float>> {
        val (cx, cy) = fivePlayerYardCenter(color)
        val d = 0.036f
        return listOf(
            (cx - d) to (cy - d),
            (cx + d) to (cy - d),
            (cx - d) to (cy + d),
            (cx + d) to (cy + d)
        )
    }

    fun fivePlayerGoalCoord(color: LudoColor): Pair<Float, Float> {
        val armIdx = FIVE_PLAYER_COLORS.indexOf(color).coerceAtLeast(0)
        val angle = armAngleRad(armIdx)
        val r = 0.056f
        return (STAR_CENTER + r * cos(angle)).toFloat() to (STAR_CENTER + r * sin(angle)).toFloat()
    }

    fun homeColumnCoord(color: LudoColor, homeStep: Int): Pair<Int, Int> {
        // homeStep in 1..5, 6 = home crown edge (classic 4P board)
        return when (color) {
            LudoColor.RED -> (homeStep.coerceIn(1, 6)) to 7
            LudoColor.GREEN -> 7 to (homeStep.coerceIn(1, 6))
            LudoColor.YELLOW -> (14 - homeStep.coerceIn(1, 6)) to 7
            LudoColor.BLUE, LudoColor.PURPLE -> 7 to (14 - homeStep.coerceIn(1, 6))
        }
    }

    fun yardCoords(color: LudoColor): List<Pair<Float, Float>> = when (color) {
        LudoColor.RED -> listOf(1.7f to 1.7f, 3.8f to 1.7f, 1.7f to 3.8f, 3.8f to 3.8f)
        LudoColor.GREEN -> listOf(10.7f to 1.7f, 12.8f to 1.7f, 10.7f to 3.8f, 12.8f to 3.8f)
        LudoColor.YELLOW -> listOf(10.7f to 10.7f, 12.8f to 10.7f, 10.7f to 12.8f, 12.8f to 12.8f)
        LudoColor.BLUE, LudoColor.PURPLE -> listOf(1.7f to 10.7f, 3.8f to 10.7f, 1.7f to 12.8f, 3.8f to 12.8f)
    }

    /** Classic 15x15 grid coordinates for 2P/3P/4P board (kept for backward compatibility). */
    fun tokenBoardCoord(token: LudoToken): Pair<Float, Float> {
        return when {
            token.progress < 0 -> yardCoords(token.color)[token.index]
            token.progress <= 50 -> {
                val (cx, cy) = OUTER_TRACK_COORDS[outerTrackIndex(token.color, token.progress, isFivePlayer = false)!!]
                (cx + 0.5f) to (cy + 0.5f)
            }
            token.progress in 51..55 -> {
                val (cx, cy) = homeColumnCoord(token.color, token.progress - 50)
                (cx + 0.5f) to (cy + 0.5f)
            }
            else -> when (token.color) {
                LudoColor.RED -> 6.6f to 7.5f
                LudoColor.GREEN -> 7.5f to 6.6f
                LudoColor.YELLOW -> 8.4f to 7.5f
                LudoColor.BLUE, LudoColor.PURPLE -> 7.5f to 8.4f
            }
        }
    }

    /**
     * Unified normalized (0f..1f, 0f..1f) board coordinate supporting BOTH
     * classic 15x15 (2P/3P/4P) and 5-Arm Pentagonal Star (5P) layouts.
     */
    fun tokenNormalizedCoord(state: LudoGameState, token: LudoToken): Pair<Float, Float> {
        if (!state.isFivePlayerBoard) {
            val (gx, gy) = tokenBoardCoord(token)
            return (gx / 15f) to (gy / 15f)
        }
        return when {
            token.progress < 0 -> fivePlayerYardCoords(token.color)[token.index]
            token.progress <= state.maxOuterProgress -> {
                val idx = outerTrackIndex(token.color, token.progress, isFivePlayer = true)!!
                val cell = FIVE_PLAYER_OUTER_CELLS[idx]
                cell.centerX to cell.centerY
            }
            token.progress in state.homeStretchStart until state.finishProgress -> {
                val step = token.progress - state.maxOuterProgress
                val cell = fivePlayerHomeCell(token.color, step)
                cell.centerX to cell.centerY
            }
            else -> fivePlayerGoalCoord(token.color)
        }
    }

    fun initialTokens(colors: List<LudoColor>): Map<LudoColor, List<LudoToken>> {
        return colors.associateWith { c -> (0..3).map { idx -> LudoToken(c, idx, -1) } }
    }

    fun newGame(playerCount: Int, playVsBot: Boolean): LudoGameState {
        val colors = when (playerCount.coerceIn(2, 5)) {
            2 -> listOf(LudoColor.RED, LudoColor.YELLOW)
            3 -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW)
            4 -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
            else -> FIVE_PLAYER_COLORS
        }
        val bots = if (playVsBot) colors.drop(1).toSet() else emptySet()
        return LudoGameState(
            activeColors = colors,
            botColors = bots,
            tokens = initialTokens(colors),
            statusLog = "${colors.first().displayName}'s turn • Roll the dice!"
        )
    }

    fun outerTrackIndex(color: LudoColor, progress: Int, isFivePlayer: Boolean = false): Int? {
        val trackLen = if (isFivePlayer) TRACK_LEN_5P else TRACK_LEN_4P
        val maxOuter = trackLen - 2
        if (progress !in 0..maxOuter) return null
        return (color.startTrack + progress) % trackLen
    }

    fun canTokenMove(token: LudoToken, dice: Int, isFivePlayer: Boolean = false): Boolean {
        val finish = if (isFivePlayer) FINISH_PROGRESS_5P else FINISH_PROGRESS_4P
        if (token.progress == finish) return false
        if (token.progress < 0) return dice == 6
        return token.progress + dice <= finish
    }

    fun legalTokenIndices(state: LudoGameState): List<Int> {
        val list = state.tokens[state.currentTurn].orEmpty()
        return list.filter { canTokenMove(it, state.diceValue, state.isFivePlayerBoard) }.map { it.index }
    }

    fun applyRoll(state: LudoGameState, rolledValue: Int): LudoGameState {
        if (state.phase != LudoPhase.WAIT_ROLL) return state
        val dice = rolledValue.coerceIn(1, 6)
        val newSixes = if (dice == 6) state.consecutiveSixes + 1 else 0
        if (newSixes >= 3) {
            val nextTurn = (state.turnIndex + 1) % state.activeColors.size
            val nextColor = state.activeColors[nextTurn]
            return state.copy(
                diceValue = dice,
                consecutiveSixes = 0,
                turnIndex = nextTurn,
                phase = LudoPhase.WAIT_ROLL,
                statusLog = "${state.currentTurn.displayName} rolled three 6s! Turn passes to ${nextColor.displayName}."
            )
        }
        val candidates = state.tokens[state.currentTurn].orEmpty()
            .filter { canTokenMove(it, dice, state.isFivePlayerBoard) }
        if (candidates.isEmpty()) {
            val nextTurn = (state.turnIndex + 1) % state.activeColors.size
            val nextColor = state.activeColors[nextTurn]
            return state.copy(
                diceValue = dice,
                consecutiveSixes = 0,
                turnIndex = nextTurn,
                phase = LudoPhase.WAIT_ROLL,
                statusLog = "${state.currentTurn.displayName} rolled $dice — no moves. ${nextColor.displayName}'s turn."
            )
        }
        return state.copy(
            diceValue = dice,
            consecutiveSixes = newSixes,
            phase = LudoPhase.WAIT_MOVE,
            statusLog = "${state.currentTurn.displayName} rolled $dice — select a highlighted token."
        )
    }

    fun applyMove(state: LudoGameState, tokenIndex: Int): LudoGameState {
        if (state.phase != LudoPhase.WAIT_MOVE) return state
        val is5P = state.isFivePlayerBoard
        val finish = state.finishProgress
        val safeSet = state.safeOuterIndices
        val color = state.currentTurn
        val myTokens = state.tokens[color].orEmpty().toMutableList()
        val moving = myTokens.getOrNull(tokenIndex) ?: return state
        if (!canTokenMove(moving, state.diceValue, is5P)) return state

        val newProgress = if (moving.progress < 0) 0 else moving.progress + state.diceValue
        myTokens[tokenIndex] = moving.copy(progress = newProgress)

        val updatedMap = state.tokens.toMutableMap()
        updatedMap[color] = myTokens

        // Check capture on non-safe outer square
        var capturedAny = false
        val landedOuter = outerTrackIndex(color, newProgress, is5P)
        if (landedOuter != null && landedOuter !in safeSet) {
            for (otherColor in state.activeColors) {
                if (otherColor == color) continue
                val otherList = updatedMap[otherColor].orEmpty().toMutableList()
                val victims = otherList.filter { outerTrackIndex(otherColor, it.progress, is5P) == landedOuter }
                if (victims.size == 1) {
                    val v = victims.first()
                    otherList[v.index] = v.copy(progress = -1)
                    updatedMap[otherColor] = otherList
                    capturedAny = true
                }
            }
        }

        val reachedGoal = newProgress == finish
        val allFinished = myTokens.all { it.progress == finish }
        if (allFinished) {
            return state.copy(
                tokens = updatedMap,
                phase = LudoPhase.GAME_OVER,
                winner = color,
                statusLog = "${color.displayName} wins the Ludo match!"
            )
        }

        val grantsBonusTurn = state.diceValue == 6 || capturedAny || reachedGoal
        val nextTurn = if (grantsBonusTurn) state.turnIndex else (state.turnIndex + 1) % state.activeColors.size
        val nextColor = state.activeColors[nextTurn]
        val logReason = when {
            capturedAny -> "${color.displayName} captured an opponent & gets a bonus roll!"
            reachedGoal -> "${color.displayName} crowned a token & gets a bonus roll!"
            state.diceValue == 6 -> "${color.displayName} rolled 6 — bonus roll!"
            else -> "${nextColor.displayName}'s turn to roll."
        }
        return state.copy(
            tokens = updatedMap,
            turnIndex = nextTurn,
            phase = LudoPhase.WAIT_ROLL,
            consecutiveSixes = if (grantsBonusTurn && state.diceValue == 6) state.consecutiveSixes else 0,
            statusLog = logReason
        )
    }
}

object LudoBotAi {
    fun chooseToken(state: LudoGameState, accuracyPercent: Int, rng: Random = Random.Default): Int {
        val legal = LudoEngine.legalTokenIndices(state)
        if (legal.isEmpty()) return 0
        if (legal.size == 1) return legal.first()

        val is5P = state.isFivePlayerBoard
        val finish = state.finishProgress
        val maxOuter = state.maxOuterProgress
        val homeStart = state.homeStretchStart
        val safeSet = state.safeOuterIndices
        val color = state.currentTurn
        val tokens = state.tokens[color].orEmpty()
        val dice = state.diceValue
        val acc = accuracyPercent.coerceIn(10, 100) / 100.0

        val scored = legal.map { idx ->
            val token = tokens[idx]
            var score = 0.0
            val nextProg = if (token.progress < 0) 0 else token.progress + dice
            val nextOuter = LudoEngine.outerTrackIndex(color, nextProg, is5P)
            val curOuter = LudoEngine.outerTrackIndex(color, token.progress, is5P)

            if (nextProg == finish) score += 140.0
            if (token.progress < 0) score += 68.0
            if (nextProg in homeStart until finish && token.progress <= maxOuter) score += 75.0
            if (nextOuter != null && nextOuter in safeSet) score += 48.0
            if (curOuter != null && curOuter in safeSet && nextOuter !in safeSet) {
                score -= 22.0
            }

            // Capture reward
            if (nextOuter != null && nextOuter !in safeSet) {
                val hits = state.activeColors.filter { it != color }.sumOf { other ->
                    val list = state.tokens[other].orEmpty()
                    val onSq = list.count { LudoEngine.outerTrackIndex(other, it.progress, is5P) == nextOuter }
                    if (onSq == 1) 1 else 0
                }
                if (hits > 0) score += 115.0
            }

            // Threat evasion & danger awareness
            if (curOuter != null && curOuter !in safeSet) {
                if (isSquareThreatened(state, color, curOuter)) score += 38.0
            }
            if (nextOuter != null && nextOuter !in safeSet) {
                if (isSquareThreatened(state, color, nextOuter)) score -= 32.0
            }

            score += (nextProg.coerceAtLeast(0)) * 0.65
            idx to score
        }.sortedByDescending { it.second }

        return when {
            acc >= 0.88 -> if (rng.nextDouble() < acc) scored.first().first else scored[1].first
            acc >= 0.45 -> {
                val pickTopProb = 0.35 + (acc - 0.45) * 1.1
                if (rng.nextDouble() < pickTopProb) scored.first().first else scored.random(rng).first
            }
            else -> {
                if (rng.nextDouble() < acc) scored.first().first else scored.random(rng).first
            }
        }
    }

    private fun isSquareThreatened(state: LudoGameState, myColor: LudoColor, targetSq: Int): Boolean {
        val is5P = state.isFivePlayerBoard
        val trackLen = state.outerTrackLength
        return state.activeColors.filter { it != myColor }.any { enemy ->
            state.tokens[enemy].orEmpty().any { et ->
                val eSq = LudoEngine.outerTrackIndex(enemy, et.progress, is5P) ?: return@any false
                val dist = (targetSq - eSq + trackLen) % trackLen
                dist in 1..6
            }
        }
    }
}
