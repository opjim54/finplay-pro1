package com.example.ui.screens.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.LudoBotAi
import com.example.domain.games.LudoColor
import com.example.domain.games.LudoEngine
import com.example.domain.games.LudoGameState
import com.example.domain.games.LudoPhase
import com.example.domain.games.LudoStarCell
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LudoBoardScreen(
    initialVsBot: Boolean = true,
    initialBotAccuracy: Int = 75,
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> }
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val scope = rememberCoroutineScope()

    var playerCount by remember { mutableIntStateOf(2) }
    var playVsBot by remember { mutableStateOf(initialVsBot) }
    var botAccuracy by remember { mutableIntStateOf(initialBotAccuracy) }
    var showRules by remember { mutableStateOf(false) }
    var isRollingAnim by remember { mutableStateOf(false) }
    var displayDice by remember { mutableIntStateOf(1) }
    var rollTilt by remember { mutableFloatStateOf(0f) }
    var matchSessionId by remember { mutableIntStateOf(0) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastEarnedScore by remember { mutableIntStateOf(0) }
    val perPlayerDice = remember { mutableStateMapOf<LudoColor, Int>() }

    var state by remember { mutableStateOf(LudoEngine.newGame(playerCount, playVsBot)) }

    fun resetLudo(cnt: Int = playerCount, vsBot: Boolean = playVsBot) {
        matchSessionId++
        isRollingAnim = false
        rollTilt = 0f
        displayDice = 1
        resultRecorded = false
        lastEarnedScore = 0
        perPlayerDice.clear()
        state = LudoEngine.newGame(cnt, vsBot)
    }

    LaunchedEffect(state.phase, state.winner) {
        val winner = state.winner
        if (state.phase == LudoPhase.GAME_OVER && winner != null && !resultRecorded) {
            resultRecorded = true
            val humanWon = winner == LudoColor.RED
            val redTokens = state.tokens[LudoColor.RED].orEmpty()
            val crowned = redTokens.count { it.progress == state.finishProgress }
            val progressSum = redTokens.sumOf { it.progress.coerceAtLeast(0) }
            val score = (crowned * 250) + (progressSum * 2) +
                if (humanWon) 500 + if (playVsBot) botAccuracy * 5 else 150 else 50
            lastEarnedScore = score
            onRecordMatch(
                if (humanWon) GameMatchResult.WIN else GameMatchResult.LOSS,
                score,
                playVsBot,
                botAccuracy
            )
        }
    }

    if (showRules) {
        GameRulesModalSheet(gameId = ArenaGameId.LUDO, onDismiss = { showRules = false })
    }

    suspend fun performAnimatedRoll(currentState: LudoGameState) {
        if (isRollingAnim || currentState.phase != LudoPhase.WAIT_ROLL) return
        val session = matchSessionId
        isRollingAnim = true
        val rollingColor = currentState.currentTurn
        for (i in 0..6) {
            if (session != matchSessionId) return
            val rand = Random.nextInt(1, 7)
            displayDice = rand
            perPlayerDice[rollingColor] = rand
            rollTilt = if (i % 2 == 0) 14f else -14f
            if (i % 2 == 0) GameSoundEngine.play(GameSoundEffect.DICE_TICK)
            delay(48L + i * 10L)
        }
        if (session != matchSessionId) return
        val finalDice = Random.nextInt(1, 7)
        displayDice = finalDice
        perPlayerDice[rollingColor] = finalDice
        rollTilt = 0f
        val next = LudoEngine.applyRoll(currentState, finalDice)
        state = next
        if (finalDice == 6) GameSoundEngine.play(GameSoundEffect.SPECIAL)
        isRollingAnim = false
    }

    fun executeLudoMove(currentState: LudoGameState, tokenIdx: Int) {
        val finish = currentState.finishProgress
        val beforeHome = currentState.tokens.values.flatten().count { it.progress == finish }
        val beforeYard = currentState.tokens.values.flatten().count { it.progress == -1 }
        val next = LudoEngine.applyMove(currentState, tokenIdx)
        state = next
        val afterHome = next.tokens.values.flatten().count { it.progress == finish }
        val afterYard = next.tokens.values.flatten().count { it.progress == -1 }
        when {
            next.winner != null -> GameSoundEngine.play(GameSoundEffect.WIN)
            afterYard > beforeYard -> GameSoundEngine.play(GameSoundEffect.CAPTURE)
            afterHome > beforeHome -> GameSoundEngine.play(GameSoundEffect.SPECIAL)
            else -> GameSoundEngine.play(GameSoundEffect.MOVE)
        }
    }

    // Automatic Bot Turn driver
    val isBotTurn = state.phase != LudoPhase.GAME_OVER && state.currentTurn in state.botColors
    LaunchedEffect(state.turnIndex, state.phase, state.diceValue, state.botColors, botAccuracy, matchSessionId) {
        if (!isBotTurn) return@LaunchedEffect
        val session = matchSessionId
        delay(360L)
        if (session != matchSessionId) return@LaunchedEffect
        if (state.phase == LudoPhase.WAIT_ROLL) {
            performAnimatedRoll(state)
        } else if (state.phase == LudoPhase.WAIT_MOVE) {
            val snapshot = state
            val chosenIdx = withContext(Dispatchers.Default) {
                LudoBotAi.chooseToken(snapshot, botAccuracy)
            }
            delay(240L)
            if (session != matchSessionId) return@LaunchedEffect
            executeLudoMove(state, chosenIdx)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "ludoPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "ludoTokenPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        UniversalBotControlBar(
            playVsBot = playVsBot,
            accuracyPercent = botAccuracy,
            onToggleBot = { enabled ->
                playVsBot = enabled
                resetLudo(playerCount, enabled)
            },
            onAccuracyChange = { botAccuracy = it },
            onOpenRules = { showRules = true },
            onRestart = { resetLudo(playerCount, playVsBot) }
        )

        // Player Count Selector (2P / 3P / 4P / 5P Star Board)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(gStrings.playersLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                listOf(2, 3, 4, 5).forEach { cnt ->
                    FilterChip(
                        selected = playerCount == cnt,
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.TAP)
                            playerCount = cnt
                            resetLudo(cnt, playVsBot)
                        },
                        label = {
                            Text(
                                text = if (cnt == 5) "⭐ 5P" else "${cnt}P",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (cnt == 5) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.testTag("ludo_player_count_${cnt}p")
                    )
                }
            }

            // Per-Color Home Progress Ribbon (fits cleanly for 2, 3, 4, or 5 players)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.activeColors.forEach { col ->
                    val doneCount = state.tokens[col].orEmpty().count { it.progress == state.finishProgress }
                    val chipC = Color(col.colorHex)
                    val chipScale by animateFloatAsState(
                        targetValue = if (col == state.currentTurn) 1.07f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "ludo_chip_${col.name}"
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = chipC.copy(alpha = if (col == state.currentTurn) 0.25f else 0.12f),
                        border = BorderStroke(1.dp, chipC.copy(alpha = if (col == state.currentTurn) 0.9f else 0.35f)),
                        modifier = Modifier.graphicsLayer { scaleX = chipScale; scaleY = chipScale }
                    ) {
                        Text(
                            text = "${gStrings.ludoColorName(col).take(3)} 👑$doneCount/4",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = chipC,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Turn & Status Banner
        val activeColor = Color(state.currentTurn.colorHex)
        val turnDisplay = gStrings.ludoColorName(state.currentTurn)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = activeColor.copy(alpha = 0.14f),
            border = BorderStroke(1.2.dp, activeColor.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.winner != null) {
                            "🏆 ${gStrings.ludoColorName(state.winner!!)} ${gStrings.winsSuffix}"
                        } else {
                            "$turnDisplay${if (state.currentTurn in state.botColors) " ${gStrings.botSuffix}" else ""}"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = activeColor
                    )
                    Text(
                        text = state.statusLog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Top Corners aligned with Board Yards (RED = Top-Left, GREEN = Top-Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (LudoColor.RED in state.activeColors) {
                LudoPlayerCornerControl(
                    playerColor = LudoColor.RED,
                    isCurrentTurn = state.currentTurn == LudoColor.RED,
                    isBot = LudoColor.RED in state.botColors,
                    crownedCount = state.tokens[LudoColor.RED].orEmpty().count { it.progress == state.finishProgress },
                    diceValue = if (state.currentTurn == LudoColor.RED) displayDice else (perPlayerDice[LudoColor.RED] ?: 1),
                    isRolling = isRollingAnim && state.currentTurn == LudoColor.RED,
                    rollTilt = rollTilt,
                    canClickToRoll = state.currentTurn == LudoColor.RED && !isBotTurn && state.phase == LudoPhase.WAIT_ROLL && !isRollingAnim,
                    onRollClick = { scope.launch { performAnimatedRoll(state) } },
                    gStrings = gStrings,
                    alignRight = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Spacer(Modifier.width(8.dp))

            if (LudoColor.GREEN in state.activeColors) {
                LudoPlayerCornerControl(
                    playerColor = LudoColor.GREEN,
                    isCurrentTurn = state.currentTurn == LudoColor.GREEN,
                    isBot = LudoColor.GREEN in state.botColors,
                    crownedCount = state.tokens[LudoColor.GREEN].orEmpty().count { it.progress == state.finishProgress },
                    diceValue = if (state.currentTurn == LudoColor.GREEN) displayDice else (perPlayerDice[LudoColor.GREEN] ?: 1),
                    isRolling = isRollingAnim && state.currentTurn == LudoColor.GREEN,
                    rollTilt = rollTilt,
                    canClickToRoll = state.currentTurn == LudoColor.GREEN && !isBotTurn && state.phase == LudoPhase.WAIT_ROLL && !isRollingAnim,
                    onRollClick = { scope.launch { performAnimatedRoll(state) } },
                    gStrings = gStrings,
                    alignRight = true,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }

        val legalMoves = remember(state) {
            if (state.phase == LudoPhase.WAIT_MOVE && !isBotTurn) LudoEngine.legalTokenIndices(state) else emptyList()
        }

        // Luxury Ludo Board Canvas (Classic 15x15 for 2P-4P; Symmetrical 5-Arm Pentagonal Star for 5P)
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.5.dp, financeColors.champagneGold.copy(alpha = 0.75f)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("ludo_board_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, isBotTurn) {
                        detectTapGestures { tapOffset ->
                            if (isBotTurn || state.phase != LudoPhase.WAIT_MOVE) return@detectTapGestures
                            val tapNx = tapOffset.x / size.width.coerceAtLeast(1)
                            val tapNy = tapOffset.y / size.height.coerceAtLeast(1)
                            val myTokens = state.tokens[state.currentTurn].orEmpty()
                            val legal = LudoEngine.legalTokenIndices(state)
                            val hit = myTokens
                                .filter { it.index in legal }
                                .minByOrNull { tk ->
                                    val (tx, ty) = LudoEngine.tokenNormalizedCoord(state, tk)
                                    hypot(tapNx - tx, tapNy - ty)
                                }
                            if (hit != null) {
                                val (tx, ty) = LudoEngine.tokenNormalizedCoord(state, hit)
                                val hitRadius = if (state.isFivePlayerBoard) 0.085f else (1.35f / 15f)
                                if (hypot(tapNx - tx, tapNy - ty) <= hitRadius) {
                                    executeLudoMove(state, hit.index)
                                }
                            }
                        }
                    }
            ) {
                if (state.isFivePlayerBoard) {
                    drawFivePlayerStarLudoBoard(this, state, legalMoves, pulseScale)
                } else {
                    drawLudoBoardGrid(this, state, legalMoves, pulseScale)
                }
            }
        }

        // Bottom Corners aligned with Board Yards (BLUE = Bottom-Left, YELLOW = Bottom-Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (LudoColor.BLUE in state.activeColors) {
                LudoPlayerCornerControl(
                    playerColor = LudoColor.BLUE,
                    isCurrentTurn = state.currentTurn == LudoColor.BLUE,
                    isBot = LudoColor.BLUE in state.botColors,
                    crownedCount = state.tokens[LudoColor.BLUE].orEmpty().count { it.progress == state.finishProgress },
                    diceValue = if (state.currentTurn == LudoColor.BLUE) displayDice else (perPlayerDice[LudoColor.BLUE] ?: 1),
                    isRolling = isRollingAnim && state.currentTurn == LudoColor.BLUE,
                    rollTilt = rollTilt,
                    canClickToRoll = state.currentTurn == LudoColor.BLUE && !isBotTurn && state.phase == LudoPhase.WAIT_ROLL && !isRollingAnim,
                    onRollClick = { scope.launch { performAnimatedRoll(state) } },
                    gStrings = gStrings,
                    alignRight = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Spacer(Modifier.width(8.dp))

            if (LudoColor.YELLOW in state.activeColors) {
                LudoPlayerCornerControl(
                    playerColor = LudoColor.YELLOW,
                    isCurrentTurn = state.currentTurn == LudoColor.YELLOW,
                    isBot = LudoColor.YELLOW in state.botColors,
                    crownedCount = state.tokens[LudoColor.YELLOW].orEmpty().count { it.progress == state.finishProgress },
                    diceValue = if (state.currentTurn == LudoColor.YELLOW) displayDice else (perPlayerDice[LudoColor.YELLOW] ?: 1),
                    isRolling = isRollingAnim && state.currentTurn == LudoColor.YELLOW,
                    rollTilt = rollTilt,
                    canClickToRoll = state.currentTurn == LudoColor.YELLOW && !isBotTurn && state.phase == LudoPhase.WAIT_ROLL && !isRollingAnim,
                    onRollClick = { scope.launch { performAnimatedRoll(state) } },
                    gStrings = gStrings,
                    alignRight = true,
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }

        // 5th Player Control (PURPLE) for 5-Player Star Board
        if (LudoColor.PURPLE in state.activeColors) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LudoPlayerCornerControl(
                    playerColor = LudoColor.PURPLE,
                    isCurrentTurn = state.currentTurn == LudoColor.PURPLE,
                    isBot = LudoColor.PURPLE in state.botColors,
                    crownedCount = state.tokens[LudoColor.PURPLE].orEmpty().count { it.progress == state.finishProgress },
                    diceValue = if (state.currentTurn == LudoColor.PURPLE) displayDice else (perPlayerDice[LudoColor.PURPLE] ?: 1),
                    isRolling = isRollingAnim && state.currentTurn == LudoColor.PURPLE,
                    rollTilt = rollTilt,
                    canClickToRoll = state.currentTurn == LudoColor.PURPLE && !isBotTurn && state.phase == LudoPhase.WAIT_ROLL && !isRollingAnim,
                    onRollClick = { scope.launch { performAnimatedRoll(state) } },
                    gStrings = gStrings,
                    alignRight = false
                )
            }
        }

        Text(
            text = if (state.isFivePlayerBoard) {
                if (gStrings.isBn) "★ ৫-প্লেয়ার স্টার বোর্ড (৬৫ ঘর • ১০টি সেফ স্টার) • সঠিক সংখ্যায় সেন্টার ক্রাউনে প্রবেশ"
                else "★ 5-Player Star Board (65 Squares • 10 Safe Stars) • Exact roll enters Center Crown"
            } else {
                gStrings.ludoSafeStarTip
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Roll / Finish Action Row
        GameMatchResultBanner(
            visible = state.phase == LudoPhase.GAME_OVER && state.winner != null,
            headline = state.winner?.let { "🏆 ${gStrings.ludoColorName(it)} ${gStrings.winsSuffix}" }.orEmpty(),
            scoreEarned = lastEarnedScore,
            onPlayAgain = { resetLudo(playerCount, playVsBot) }
        )
        if (state.phase != LudoPhase.GAME_OVER && !isBotTurn && state.phase == LudoPhase.WAIT_ROLL) {
            Button(
                enabled = !isRollingAnim,
                onClick = { scope.launch { performAnimatedRoll(state) } },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ludo_roll_btn")
            ) {
                Text(
                    if (isRollingAnim) gStrings.rollingDice else "${gStrings.rollDice} ($turnDisplay)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun diceGlyph(v: Int): String = when (v) {
    1 -> "⚀"
    2 -> "⚁"
    3 -> "⚂"
    4 -> "⚃"
    5 -> "⚄"
    else -> "⚅"
}

private fun DrawScope.drawStarMarker(center: Offset, outerR: Float, color: Color) {
    val innerR = outerR * 0.44f
    val path = Path()
    for (i in 0 until 10) {
        val angle = -PI / 2.0 + i * (PI / 5.0)
        val r = if (i % 2 == 0) outerR else innerR
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

private fun DrawScope.drawStarCellBox(
    cell: LudoStarCell,
    boardSize: Float,
    fillColor: Color,
    borderColor: Color
) {
    val cx = cell.centerX * boardSize
    val cy = cell.centerY * boardSize
    val w = LudoEngine.STAR_CELL_H * boardSize
    val h = LudoEngine.STAR_CELL_W * boardSize
    val deg = Math.toDegrees(cell.angleRad).toFloat()
    rotate(degrees = deg, pivot = Offset(cx, cy)) {
        val topLeft = Offset(cx - w / 2f, cy - h / 2f)
        drawRect(color = fillColor, topLeft = topLeft, size = Size(w, h))
        drawRect(color = borderColor, topLeft = topLeft, size = Size(w, h), style = Stroke(1.3f))
    }
}

/**
 * Symmetrical 5-Arm Pentagonal Star Ludo Board renderer for 5-Player mode:
 * - 65 outer perimeter cells (5 arms x 13)
 * - 5 colored Home Stretches (5 cells each) leading into a 5-wedge central Crown Pentagon
 * - 10 golden Safe Star cells
 * - 5 circular Home Yards tucked cleanly between adjacent arms with 4 token pedestals each
 */
private fun drawFivePlayerStarLudoBoard(
    scope: DrawScope,
    state: LudoGameState,
    legalTokenIndices: List<Int>,
    pulseScale: Float
) = with(scope) {
    val s = size.width
    val center = Offset(LudoEngine.STAR_CENTER * s, LudoEngine.STAR_CENTER * s)

    // Royal radial pearl parchment background
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFFDF7), Color(0xFFF1F5F9)),
            center = center,
            radius = s * 0.65f
        ),
        size = size
    )

    // Subtle decorative outer golden ring
    drawCircle(
        color = Color(0xFFD4AF37).copy(alpha = 0.25f),
        radius = s * 0.468f,
        center = center,
        style = Stroke(2.2f)
    )

    val borderStroke = Color(0xFF64748B).copy(alpha = 0.55f)
    val startIndices = LudoEngine.FIVE_PLAYER_COLORS.associate { it.startTrack to Color(it.colorHex) }

    // 1. Draw all 65 outer perimeter cells
    LudoEngine.FIVE_PLAYER_OUTER_CELLS.forEachIndexed { idx, cell ->
        val startTint = startIndices[idx]
        val fill = startTint?.copy(alpha = 0.88f) ?: Color(0xFFFFFFFF)
        drawStarCellBox(cell, s, fill, borderStroke)
    }

    // 2. Draw all 5 colored Home Stretches (5 steps each)
    LudoEngine.FIVE_PLAYER_COLORS.forEach { col ->
        val c = Color(col.colorHex)
        for (step in 1..5) {
            val cell = LudoEngine.fivePlayerHomeCell(col, step)
            drawStarCellBox(cell, s, c.copy(alpha = 0.80f), borderStroke)
        }
    }

    // 3. Draw 10 Safe Star markers
    LudoEngine.SAFE_OUTER_TRACK_INDICES_5P.forEach { sq ->
        val cell = LudoEngine.FIVE_PLAYER_OUTER_CELLS[sq]
        val ctr = Offset(cell.centerX * s, cell.centerY * s)
        val r = LudoEngine.STAR_CELL_W * s
        drawCircle(color = Color(0xFFD4AF37).copy(alpha = 0.26f), radius = r * 0.40f, center = ctr)
        drawStarMarker(center = ctr, outerR = r * 0.30f, color = Color(0xFFD97706))
    }

    // 4. Draw Central 5-Wedge Crown Pentagon
    val pentR = (LudoEngine.STAR_INNER_RADIUS * 1.03f) * s
    val vertices = (0 until 5).map { i ->
        val a = LudoEngine.armAngleRad(i) - PI / 5.0
        Offset(
            center.x + (cos(a) * pentR).toFloat(),
            center.y + (sin(a) * pentR).toFloat()
        )
    }
    LudoEngine.FIVE_PLAYER_COLORS.forEachIndexed { i, col ->
        val v1 = vertices[i]
        val v2 = vertices[(i + 1) % 5]
        val wedge = Path().apply {
            moveTo(center.x, center.y)
            lineTo(v1.x, v1.y)
            lineTo(v2.x, v2.y)
            close()
        }
        drawPath(wedge, color = Color(col.colorHex).copy(alpha = 0.90f))
        drawPath(wedge, color = Color.White.copy(alpha = 0.65f), style = Stroke(1.5f))
    }
    drawCircle(Color(0xFFFEF08A), radius = s * 0.030f, center = center)
    drawCircle(Color(0xFFB45309), radius = s * 0.030f, center = center, style = Stroke(2.0f))

    // 5. Draw 5 Circular Corner Yards & Pedestals
    LudoEngine.FIVE_PLAYER_COLORS.forEach { col ->
        val c = Color(col.colorHex)
        val (ycx, ycy) = LudoEngine.fivePlayerYardCenter(col)
        val yardCtr = Offset(ycx * s, ycy * s)
        val yardR = LudoEngine.STAR_YARD_CIRCLE_RADIUS * s

        drawCircle(
            brush = Brush.radialGradient(
                listOf(c.copy(alpha = 0.92f), c.copy(alpha = 0.72f)),
                center = yardCtr,
                radius = yardR
            ),
            radius = yardR,
            center = yardCtr
        )
        drawCircle(Color(0xFFFFFDF9), radius = yardR * 0.78f, center = yardCtr)
        drawCircle(c.copy(alpha = 0.6f), radius = yardR, center = yardCtr, style = Stroke(2.2f))

        LudoEngine.fivePlayerYardCoords(col).forEach { (px, py) ->
            val pedCtr = Offset(px * s, py * s)
            drawCircle(c.copy(alpha = 0.22f), radius = s * 0.022f, center = pedCtr)
            drawCircle(c.copy(alpha = 0.55f), radius = s * 0.022f, center = pedCtr, style = Stroke(1.4f))
        }
    }

    // 6. Group tokens sharing coordinates + draw 3D shaded bevel pawns
    val pawnR = s * 0.0205f
    val allActiveTokens = state.activeColors.flatMap { c -> state.tokens[c].orEmpty() }
    val grouped = allActiveTokens.groupBy { LudoEngine.tokenNormalizedCoord(state, it) }
    grouped.forEach { (coord, list) ->
        list.forEachIndexed { idx, tk ->
            val offsetAngle = if (list.size > 1) idx * (2 * PI / list.size) else 0.0
            val shift = if (list.size > 1) pawnR * 0.62f else 0f
            val cx = coord.first * s + (cos(offsetAngle) * shift).toFloat()
            val cy = coord.second * s + (sin(offsetAngle) * shift).toFloat()
            val centerPt = Offset(cx, cy)
            val isSelectable = tk.color == state.currentTurn && tk.index in legalTokenIndices
            val tokenColor = Color(tk.color.colorHex)

            if (isSelectable) {
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = 0.52f),
                    radius = pawnR * 1.48f * pulseScale,
                    center = centerPt
                )
            }
            drawCircle(Color.Black.copy(alpha = 0.28f), radius = pawnR, center = centerPt.copy(y = centerPt.y + 2f))
            drawCircle(color = Color.White, radius = pawnR, center = centerPt)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.55f), tokenColor),
                    center = Offset(centerPt.x - pawnR * 0.26f, centerPt.y - pawnR * 0.26f),
                    radius = pawnR
                ),
                radius = pawnR * 0.82f,
                center = centerPt
            )
            drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = pawnR, center = centerPt, style = Stroke(1.6f))
        }
    }
}

private fun drawLudoBoardGrid(
    scope: DrawScope,
    state: LudoGameState,
    legalTokenIndices: List<Int>,
    pulseScale: Float
) = with(scope) {
    val cell = size.width / 15f
    drawRect(color = Color(0xFFFAFDFB), size = size)

    val redC = Color(LudoColor.RED.colorHex)
    val greenC = Color(LudoColor.GREEN.colorHex)
    val yellowC = Color(LudoColor.YELLOW.colorHex)
    val blueC = Color(LudoColor.BLUE.colorHex)

    // 4 Corner Yards with luxury gradient inner pads
    fun drawYard(x0: Int, y0: Int, color: Color) {
        drawRect(
            brush = Brush.linearGradient(
                listOf(color, color.copy(alpha = 0.82f)),
                start = Offset(x0 * cell, y0 * cell),
                end = Offset((x0 + 6) * cell, (y0 + 6) * cell)
            ),
            topLeft = Offset(x0 * cell, y0 * cell),
            size = Size(6 * cell, 6 * cell)
        )
        drawRect(
            color = Color(0xFFFFFDF9),
            topLeft = Offset((x0 + 0.78f) * cell, (y0 + 0.78f) * cell),
            size = Size(4.44f * cell, 4.44f * cell)
        )
        drawRect(
            color = color.copy(alpha = 0.35f),
            topLeft = Offset((x0 + 0.78f) * cell, (y0 + 0.78f) * cell),
            size = Size(4.44f * cell, 4.44f * cell),
            style = Stroke(2f)
        )
    }
    drawYard(0, 0, redC)
    drawYard(9, 0, greenC)
    drawYard(9, 9, yellowC)
    drawYard(0, 9, blueC)

    // Home Columns (1..5)
    for (i in 1..5) {
        drawRect(redC.copy(alpha = 0.82f), Offset(i * cell, 7 * cell), Size(cell, cell))
        drawRect(greenC.copy(alpha = 0.82f), Offset(7 * cell, i * cell), Size(cell, cell))
        drawRect(yellowC.copy(alpha = 0.82f), Offset((14 - i) * cell, 7 * cell), Size(cell, cell))
        drawRect(blueC.copy(alpha = 0.82f), Offset(7 * cell, (14 - i) * cell), Size(cell, cell))
    }

    // Start squares highlight
    drawRect(redC.copy(alpha = 0.90f), Offset(1 * cell, 6 * cell), Size(cell, cell))
    drawRect(greenC.copy(alpha = 0.90f), Offset(8 * cell, 1 * cell), Size(cell, cell))
    drawRect(yellowC.copy(alpha = 0.90f), Offset(13 * cell, 8 * cell), Size(cell, cell))
    drawRect(blueC.copy(alpha = 0.90f), Offset(6 * cell, 13 * cell), Size(cell, cell))

    // Safe Stars (5-point drawn gold stars)
    LudoEngine.SAFE_OUTER_TRACK_INDICES.forEach { sq ->
        val (cx, cy) = LudoEngine.OUTER_TRACK_COORDS[sq]
        val ctr = Offset((cx + 0.5f) * cell, (cy + 0.5f) * cell)
        drawCircle(color = Color(0xFFD4AF37).copy(alpha = 0.24f), radius = cell * 0.38f, center = ctr)
        drawStarMarker(center = ctr, outerR = cell * 0.29f, color = Color(0xFFD97706))
    }

    // Center Goal Triangles (6..9, 6..9) + Crown medallion
    val cTopLeft = Offset(6 * cell, 6 * cell)
    val cTopRight = Offset(9 * cell, 6 * cell)
    val cBotRight = Offset(9 * cell, 9 * cell)
    val cBotLeft = Offset(6 * cell, 9 * cell)
    val center = Offset(7.5f * cell, 7.5f * cell)

    fun tri(p1: Offset, p2: Offset, col: Color) {
        val p = Path().apply { moveTo(center.x, center.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }
        drawPath(p, color = col.copy(alpha = 0.92f))
    }
    tri(cTopLeft, cBotLeft, redC)
    tri(cTopLeft, cTopRight, greenC)
    tri(cTopRight, cBotRight, yellowC)
    tri(cBotLeft, cBotRight, blueC)
    drawCircle(Color(0xFFFEF08A), radius = cell * 0.42f, center = center)
    drawCircle(Color(0xFFB45309), radius = cell * 0.42f, center = center, style = Stroke(2.2f))

    // Subtle crisp grid lines
    val gridPaint = Color(0xFF64748B).copy(alpha = 0.38f)
    for (r in 0..15) {
        drawLine(gridPaint, Offset(0f, r * cell), Offset(size.width, r * cell), strokeWidth = 1f)
        drawLine(gridPaint, Offset(r * cell, 0f), Offset(r * cell, size.height), strokeWidth = 1f)
    }

    // Yard token pedestals
    state.activeColors.forEach { color ->
        val c = Color(color.colorHex)
        LudoEngine.yardCoords(color).forEach { (yx, yy) ->
            drawCircle(c.copy(alpha = 0.22f), radius = cell * 0.44f, center = Offset(yx * cell, yy * cell))
            drawCircle(c.copy(alpha = 0.45f), radius = cell * 0.44f, center = Offset(yx * cell, yy * cell), style = Stroke(1.5f))
        }
    }

    // Group tokens sharing coordinates + draw 3D shaded bevel pawns
    val allActiveTokens = state.activeColors.flatMap { c -> state.tokens[c].orEmpty() }
    val grouped = allActiveTokens.groupBy { LudoEngine.tokenBoardCoord(it) }
    grouped.forEach { (coord, list) ->
        list.forEachIndexed { idx, tk ->
            val offsetAngle = if (list.size > 1) idx * (2 * PI / list.size) else 0.0
            val shift = if (list.size > 1) cell * 0.16f else 0f
            val cx = coord.first * cell + (cos(offsetAngle) * shift).toFloat()
            val cy = coord.second * cell + (sin(offsetAngle) * shift).toFloat()
            val centerPt = Offset(cx, cy)
            val isSelectable = tk.color == state.currentTurn && tk.index in legalTokenIndices
            val tokenColor = Color(tk.color.colorHex)

            if (isSelectable) {
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = 0.46f),
                    radius = cell * 0.46f * pulseScale,
                    center = centerPt
                )
            }
            // Drop shadow
            drawCircle(Color.Black.copy(alpha = 0.28f), radius = cell * 0.35f, center = centerPt.copy(y = centerPt.y + 2.5f))
            // Outer pearl rim
            drawCircle(color = Color.White, radius = cell * 0.34f, center = centerPt)
            // Inner colored dome
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.55f), tokenColor),
                    center = Offset(centerPt.x - cell * 0.09f, centerPt.y - cell * 0.09f),
                    radius = cell * 0.34f
                ),
                radius = cell * 0.28f,
                center = centerPt
            )
            drawCircle(color = Color.Black.copy(alpha = 0.40f), radius = cell * 0.34f, center = centerPt, style = Stroke(1.8f))
        }
    }
}

@Composable
private fun LudoPlayerCornerControl(
    playerColor: LudoColor,
    isCurrentTurn: Boolean,
    isBot: Boolean,
    crownedCount: Int,
    diceValue: Int,
    isRolling: Boolean,
    rollTilt: Float,
    canClickToRoll: Boolean,
    onRollClick: () -> Unit,
    gStrings: GameUiStrings,
    alignRight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = Color(playerColor.colorHex)
    val cornerTransition = rememberInfiniteTransition(label = "corner_pulse_${playerColor.name}")
    val cornerPulseScale by cornerTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrentTurn && canClickToRoll) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "corner_dice_pulse"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentTurn) color.copy(alpha = 0.16f) else Color(0xFF1E293B).copy(alpha = 0.65f),
        border = BorderStroke(
            if (isCurrentTurn) 2.dp else 1.dp,
            if (isCurrentTurn) color else Color(0xFF334155)
        ),
        shadowElevation = if (isCurrentTurn) 4.dp else 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (alignRight) {
                CornerDiceBox(
                    color = color,
                    diceValue = diceValue,
                    isRolling = isRolling,
                    rollTilt = rollTilt,
                    canClickToRoll = canClickToRoll,
                    pulseScale = cornerPulseScale,
                    onRollClick = onRollClick,
                    tag = "ludo_dice_${playerColor.name.lowercase()}"
                )
                CornerPlayerInfo(
                    playerColor = playerColor,
                    isCurrentTurn = isCurrentTurn,
                    isBot = isBot,
                    crownedCount = crownedCount,
                    canClickToRoll = canClickToRoll,
                    color = color,
                    gStrings = gStrings,
                    alignEnd = true
                )
            } else {
                CornerPlayerInfo(
                    playerColor = playerColor,
                    isCurrentTurn = isCurrentTurn,
                    isBot = isBot,
                    crownedCount = crownedCount,
                    canClickToRoll = canClickToRoll,
                    color = color,
                    gStrings = gStrings,
                    alignEnd = false
                )
                CornerDiceBox(
                    color = color,
                    diceValue = diceValue,
                    isRolling = isRolling,
                    rollTilt = rollTilt,
                    canClickToRoll = canClickToRoll,
                    pulseScale = cornerPulseScale,
                    onRollClick = onRollClick,
                    tag = "ludo_dice_${playerColor.name.lowercase()}"
                )
            }
        }
    }
}

@Composable
private fun CornerDiceBox(
    color: Color,
    diceValue: Int,
    isRolling: Boolean,
    rollTilt: Float,
    canClickToRoll: Boolean,
    pulseScale: Float,
    onRollClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(2.dp, if (canClickToRoll) color else color.copy(alpha = 0.5f)),
        shadowElevation = if (canClickToRoll) 4.dp else 1.dp,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = if (canClickToRoll) pulseScale else 1f
                scaleY = if (canClickToRoll) pulseScale else 1f
                rotationZ = if (isRolling) rollTilt else 0f
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = canClickToRoll, onClick = onRollClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = diceGlyph(diceValue.coerceIn(1, 6)),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun CornerPlayerInfo(
    playerColor: LudoColor,
    isCurrentTurn: Boolean,
    isBot: Boolean,
    crownedCount: Int,
    canClickToRoll: Boolean,
    color: Color,
    gStrings: GameUiStrings,
    alignEnd: Boolean
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "${gStrings.ludoColorName(playerColor)}${if (isBot) " 🤖" else ""}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentTurn) color else Color.White,
                maxLines = 1
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "👑 $crownedCount/4",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.Bold
            )
            if (isCurrentTurn) {
                Text(
                    text = if (canClickToRoll) "• ROLL!" else if (isBot) "• THINKING" else "• MOVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canClickToRoll) Color(0xFF10B981) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
