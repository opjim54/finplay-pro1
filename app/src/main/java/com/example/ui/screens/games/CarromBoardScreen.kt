package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.CarromBotAi
import com.example.domain.games.CarromEngine
import com.example.domain.games.CarromGameState
import com.example.domain.games.CarromPhase
import com.example.domain.games.CarromPiece
import com.example.domain.games.CarromPieceType
import com.example.domain.games.CarromTurn
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.QueenStatus
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CarromBoardScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    var playVsBot by remember { mutableStateOf(true) }
    var botAccuracy by remember { mutableIntStateOf(80) }
    var showRules by remember { mutableStateOf(false) }

    var gameState by remember {
        mutableStateOf(CarromEngine.newGame(isVsBot = true, botAccuracy = 80))
    }

    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var currentDragOffset by remember { mutableStateOf<Offset?>(null) }
    var lastRecordedMatchForShot by remember { mutableIntStateOf(-1) }
    var botThinking by remember { mutableStateOf(false) }

    // Floating Score Toast animation
    var scoreToastText by remember { mutableStateOf<String?>(null) }
    val toastAlpha = remember { Animatable(0f) }
    val toastOffsetY = remember { Animatable(0f) }

    // Reset game helper
    fun resetGame() {
        gameState = CarromEngine.newGame(isVsBot = playVsBot, botAccuracy = botAccuracy)
        dragStartOffset = null
        currentDragOffset = null
        lastRecordedMatchForShot = -1
        botThinking = false
        scoreToastText = null
    }

    // Trigger floating score toast
    LaunchedEffect(gameState.recentScoreToast) {
        val toast = gameState.recentScoreToast
        if (!toast.isNullOrBlank()) {
            scoreToastText = toast
            toastAlpha.snapTo(1f)
            toastOffsetY.snapTo(0f)
            toastOffsetY.animateTo(-40f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            toastAlpha.animateTo(0f, animationSpec = tween(400))
            scoreToastText = null
        }
    }

    // Continuous 60fps Physics Simulation Loop
    LaunchedEffect(gameState.phase) {
        if (gameState.phase == CarromPhase.MOVING) {
            var lastTime = System.nanoTime()
            while (isActive && gameState.phase == CarromPhase.MOVING) {
                withFrameNanos { now ->
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.008f, 0.033f)
                    lastTime = now
                    val oldPieces = gameState.pieces
                    val nextState = CarromEngine.updatePhysics(gameState, dt)

                    // Check for audio cues
                    val anyJustPocketed = nextState.pieces.any { np ->
                        val op = oldPieces.firstOrNull { it.id == np.id }
                        op != null && op.pocketId == -1 && np.pocketId != -1
                    }
                    if (anyJustPocketed) {
                        GameSoundEngine.play(GameSoundEffect.SPECIAL)
                    }

                    gameState = nextState
                }
            }
        }
    }

    // Bot AI Turn execution
    LaunchedEffect(gameState.turn, gameState.phase, playVsBot, botAccuracy) {
        if (playVsBot && gameState.turn == CarromTurn.PLAYER_2 && gameState.phase == CarromPhase.AIMING && gameState.winner == null) {
            botThinking = true
            delay(650L) // Bot thinking delay
            val (botX, impulse) = CarromBotAi.computeBotShot(gameState, botAccuracy)

            // Step 1: Slide striker to chosen baseline position
            gameState = CarromEngine.setStrikerBaselineX(gameState, botX)
            delay(350L) // Aiming pause

            // Step 2: Strike!
            GameSoundEngine.play(GameSoundEffect.TAP)
            gameState = CarromEngine.shootStriker(gameState, impulse.x, impulse.y)
            botThinking = false
        }
    }

    // Settle Match when Game Over
    LaunchedEffect(gameState.phase, gameState.winner) {
        if (gameState.phase == CarromPhase.ROUND_OVER && lastRecordedMatchForShot != gameState.shotNumber) {
            lastRecordedMatchForShot = gameState.shotNumber
            val outcome = when {
                gameState.winner == CarromTurn.PLAYER_1 -> GameMatchResult.WIN
                gameState.winner == CarromTurn.PLAYER_2 -> GameMatchResult.LOSS
                else -> GameMatchResult.DRAW
            }
            val score = gameState.player1Score * 10 + if (outcome == GameMatchResult.WIN) 500 else 100
            GameSoundEngine.play(if (outcome == GameMatchResult.WIN) GameSoundEffect.WIN else GameSoundEffect.SPECIAL)
            onRecordMatch(outcome, score, playVsBot, botAccuracy)
        }
    }

    if (showRules) {
        GameRulesModalSheet(gameId = ArenaGameId.CARROM, onDismiss = { showRules = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Universal Bot & Game Mode Bar
        UniversalBotControlBar(
            playVsBot = playVsBot,
            accuracyPercent = botAccuracy,
            onToggleBot = { enabled ->
                playVsBot = enabled
                resetGame()
            },
            onAccuracyChange = {
                botAccuracy = it
                gameState = gameState.copy(botAccuracy = it)
            },
            onOpenRules = { showRules = true },
            onRestart = { resetGame() }
        )

        // Status & Score Header Card
        CarromScoreHeader(
            state = gameState,
            playVsBot = playVsBot,
            botThinking = botThinking,
            financeColors = financeColors
        )

        // Interactive 3D Carrom Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(24.dp))
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1107)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gameState.phase, gameState.turn, playVsBot) {
                        if (gameState.phase != CarromPhase.AIMING) return@pointerInput
                        if (playVsBot && gameState.turn == CarromTurn.PLAYER_2) return@pointerInput

                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartOffset = offset
                                currentDragOffset = offset
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentDragOffset = change.position
                            },
                            onDragEnd = {
                                val start = dragStartOffset
                                val curr = currentDragOffset
                                if (start != null && curr != null) {
                                    val dx = (start.x - curr.x)
                                    val dy = (start.y - curr.y)
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dist > 25f) {
                                        // Convert drag offset into impulse normalized for carrom board space
                                        val impulseScale = 0.0075f
                                        val impulseX = (dx * impulseScale).coerceIn(-CarromEngine.MAX_IMPULSE, CarromEngine.MAX_IMPULSE)
                                        val impulseY = (dy * impulseScale).coerceIn(-CarromEngine.MAX_IMPULSE, CarromEngine.MAX_IMPULSE)

                                        GameSoundEngine.play(GameSoundEffect.TAP)
                                        gameState = CarromEngine.shootStriker(gameState, impulseX, impulseY)
                                    }
                                }
                                dragStartOffset = null
                                currentDragOffset = null
                            },
                            onDragCancel = {
                                dragStartOffset = null
                                currentDragOffset = null
                            }
                        )
                    }
            ) {
                val boardSize = size.minDimension
                val halfBoard = boardSize / 2f
                val centerOffset = Offset(halfBoard, halfBoard)

                // 1. Draw 3D Wooden Beveled Border Frame
                draw3DWoodenFrame(boardSize)

                // 2. Draw 4 Drop Corner Pockets
                drawCornerPockets(boardSize, halfBoard)

                // 3. Draw Lacquered Satin Board Markings (Center mandala, radiating arrows, baselines)
                drawBoardMarkings(boardSize, halfBoard, centerOffset)

                // 4. Draw Trajectory Aim Line & Power Meter when aiming
                if (gameState.phase == CarromPhase.AIMING && dragStartOffset != null && currentDragOffset != null) {
                    val striker = gameState.pieces.firstOrNull { it.type == CarromPieceType.STRIKER }
                    if (striker != null) {
                        val strikerCanvasX = halfBoard + (striker.x * halfBoard)
                        val strikerCanvasY = halfBoard + (striker.y * halfBoard)
                        val strikerCanvasPos = Offset(strikerCanvasX, strikerCanvasY)

                        val pullDx = dragStartOffset!!.x - currentDragOffset!!.x
                        val pullDy = dragStartOffset!!.y - currentDragOffset!!.y
                        val pullDist = sqrt(pullDx * pullDx + pullDy * pullDy)

                        if (pullDist > 15f) {
                            val aimAngle = atan2(pullDy, pullDx)
                            val aimLength = (pullDist * 2.5f).coerceIn(40f, boardSize * 0.75f)
                            val aimTarget = Offset(
                                strikerCanvasPos.x + aimLength * cos(aimAngle),
                                strikerCanvasPos.y + aimLength * sin(aimAngle)
                            )

                            // Draw Aim Guide Path
                            drawLine(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFFEF4444))
                                ),
                                start = strikerCanvasPos,
                                end = aimTarget,
                                strokeWidth = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                            )

                            // Target Reticle
                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = 7.dp.toPx(),
                                center = aimTarget,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                    }
                }

                // 5. Draw 3D Pieces (Carrom Men & Striker) with realistic depth and specular highlights
                gameState.pieces.forEach { piece ->
                    if (!piece.isPocketed) {
                        draw3DCarromPiece(piece, halfBoard)
                    }
                }
            }

            // Floating Score Toast
            if (scoreToastText != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.92f * toastAlpha.value),
                    border = BorderStroke(1.5.dp, Color(0xFFFBBF24).copy(alpha = toastAlpha.value)),
                    modifier = Modifier
                        .offset { IntOffset(0, toastOffsetY.value.roundToIntPx()) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = scoreToastText ?: "",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Striker Baseline Slider & Strike Controls (Active when player is aiming)
        if (gameState.phase == CarromPhase.AIMING && (!playVsBot || gameState.turn == CarromTurn.PLAYER_1)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, financeColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Position Striker",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Drag slider left/right",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = gameState.strikerBaselineX,
                        onValueChange = { newX ->
                            gameState = CarromEngine.setStrikerBaselineX(gameState, newX)
                        },
                        valueRange = -0.55f..0.55f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF0284C7),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("carrom_baseline_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "💡 Tip: Drag backward from the striker on the board to aim and shoot!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Game Over Dialog
        if (gameState.phase == CarromPhase.ROUND_OVER) {
            AlertDialog(
                onDismissRequest = { /* forced action */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        text = if (gameState.winner == CarromTurn.PLAYER_1) "🏆 Victory! Player 1 Wins!" else "Match Over! Winner: Player 2",
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Score: ${gameState.player1Score} vs ${gameState.player2Score}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Queen: ${if (gameState.queenStatus == QueenStatus.COVERED_BY_P1) "Covered by P1 (+25)" else if (gameState.queenStatus == QueenStatus.COVERED_BY_P2) "Covered by P2 (+25)" else "Uncovered"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Coins Pocketed: P1 (${gameState.player1PocketedCount}) • P2 (${gameState.player2PocketedCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { resetGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669), contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play Again", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onExit) {
                        Text("Exit to Arena")
                    }
                }
            )
        }
    }
}

@Composable
private fun CarromScoreHeader(
    state: CarromGameState,
    playVsBot: Boolean,
    botThinking: Boolean,
    financeColors: com.example.ui.theme.FinanceColors
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financeColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 Badge (White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))))
                            .border(1.5.dp, Color(0xFFFBBF24), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Player 1 (White)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Score: ${state.player1Score} (${state.player1PocketedCount} coins)", fontSize = 11.sp, color = Color(0xFF059669))
                    }
                }

                // VS Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("VS", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }

                // Player 2 / Bot Badge (Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (playVsBot) "Smart Bot" else "Player 2 (Black)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Score: ${state.player2Score} (${state.player2PocketedCount} coins)", fontSize = 11.sp, color = Color(0xFF0284C7))
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFF475569), Color(0xFF1E293B), Color.Black)))
                            .border(1.5.dp, Color(0xFFF59E0B), CircleShape)
                    )
                }
            }

            // Queen Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF831843).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Red Queen (25 pts):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = when (state.queenStatus) {
                        QueenStatus.ON_BOARD -> "On Board"
                        QueenStatus.POCKETED_PENDING_COVER -> "Pocketed • Pending Cover!"
                        QueenStatus.COVERED_BY_P1 -> "Covered by Player 1"
                        QueenStatus.COVERED_BY_P2 -> "Covered by Player 2"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48)
                )
            }

            // Active Turn Message
            Text(
                text = if (botThinking) "🤖 Smart Bot is calculating shot..." else state.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (state.turn == CarromTurn.PLAYER_1) Color(0xFF059669) else Color(0xFFD97706),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------- CANVAS DRAWING HELPERS ----------------

private fun DrawScope.draw3DWoodenFrame(boardSize: Float) {
    val frameWidth = boardSize * 0.065f

    // Rich Mahogany Wooden Border Frame
    drawRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFF2B1408), Color(0xFF5A2C11), Color(0xFF381B0B), Color(0xFF1F0D04))
        ),
        topLeft = Offset.Zero,
        size = Size(boardSize, boardSize)
    )

    // Inner Playing Surface (Rich Honey Teak & Warm Amber - No Overly White Center)
    val playAreaLeft = frameWidth
    val playAreaSize = boardSize - 2 * frameWidth
    drawRect(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFE3B879), // Warm honey birch center
                Color(0xFFD9A867), // Golden amber mid
                Color(0xFFBE8743), // Rich teak outer
                Color(0xFF78451D)  // Deep polished wood border
            ),
            center = Offset(boardSize / 2f, boardSize / 2f),
            radius = boardSize * 0.58f
        ),
        topLeft = Offset(playAreaLeft, playAreaLeft),
        size = Size(playAreaSize, playAreaSize)
    )

    // Subtle Natural Wood Grain Striations
    val grainSpacing = (playAreaSize / 28f).coerceAtLeast(6f)
    var grainY = playAreaLeft + grainSpacing
    while (grainY < playAreaLeft + playAreaSize) {
        drawLine(
            color = Color(0xFF5C300E).copy(alpha = 0.05f),
            start = Offset(playAreaLeft, grainY),
            end = Offset(playAreaLeft + playAreaSize, grainY),
            strokeWidth = 1f
        )
        grainY += grainSpacing
    }

    // Inner Cushion Border Line (Dark Mahogany Cushion)
    drawRect(
        color = Color(0xFF4A1E0D),
        topLeft = Offset(frameWidth, frameWidth),
        size = Size(boardSize - 2 * frameWidth, boardSize - 2 * frameWidth),
        style = Stroke(width = 3.dp.toPx())
    )

    // Metallic Corner Caps at 4 corners
    val cornerCapSize = frameWidth * 1.5f
    val cornerOffsets = listOf(
        Offset(0f, 0f),
        Offset(boardSize - cornerCapSize, 0f),
        Offset(0f, boardSize - cornerCapSize),
        Offset(boardSize - cornerCapSize, boardSize - cornerCapSize)
    )
    cornerOffsets.forEach { pos ->
        drawRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFF92400E))
            ),
            topLeft = pos,
            size = Size(cornerCapSize, cornerCapSize)
        )
        // Rivet dot
        drawCircle(
            color = Color(0xFF451A03),
            radius = 2.5.dp.toPx(),
            center = Offset(pos.x + cornerCapSize / 2f, pos.y + cornerCapSize / 2f)
        )
    }
}

private fun DrawScope.drawCornerPockets(boardSize: Float, halfBoard: Float) {
    CarromEngine.POCKETS.forEach { pocket ->
        val px = halfBoard + (pocket.x * halfBoard)
        val py = halfBoard + (pocket.y * halfBoard)
        val radius = pocket.radius * halfBoard

        // Outer brass/chrome rim
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFF451A03)),
                center = Offset(px, py),
                radius = radius * 1.25f
            ),
            radius = radius * 1.2f,
            center = Offset(px, py)
        )

        // Pocket hole depth gradient
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF000000), Color(0xFF1A1A1A), Color(0xFF2C1810)),
                center = Offset(px, py),
                radius = radius
            ),
            radius = radius,
            center = Offset(px, py)
        )

        // Pocket drop net lines
        val netColor = Color.White.copy(alpha = 0.15f)
        drawLine(netColor, Offset(px - radius * 0.7f, py), Offset(px + radius * 0.7f, py), strokeWidth = 1.5f)
        drawLine(netColor, Offset(px, py - radius * 0.7f), Offset(px, py + radius * 0.7f), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawBoardMarkings(boardSize: Float, halfBoard: Float, center: Offset) {
    val markColor = Color(0xFF8B4513)
    val redColor = Color(0xFFB91C1C)

    // Center Mandala Rings
    drawCircle(
        color = markColor,
        radius = 0.23f * halfBoard,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = redColor,
        radius = 0.075f * halfBoard,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = markColor,
        radius = 0.04f * halfBoard,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Center 6-petal Floral Mandala
    for (i in 0 until 6) {
        val angle = i * (PI.toFloat() / 3f)
        val petalCenter = Offset(
            center.x + 0.114f * halfBoard * cos(angle),
            center.y + 0.114f * halfBoard * sin(angle)
        )
        drawCircle(
            color = redColor.copy(alpha = 0.4f),
            radius = 0.038f * halfBoard,
            center = petalCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    // 4 Corner-to-Pocket Arrows
    CarromEngine.POCKETS.forEach { pocket ->
        val px = halfBoard + (pocket.x * halfBoard)
        val py = halfBoard + (pocket.y * halfBoard)
        val angle = atan2(py - center.y, px - center.x)

        val start = Offset(
            center.x + 0.25f * halfBoard * cos(angle),
            center.y + 0.25f * halfBoard * sin(angle)
        )
        val end = Offset(
            center.x + 0.75f * halfBoard * cos(angle),
            center.y + 0.75f * halfBoard * sin(angle)
        )
        drawLine(
            color = redColor,
            start = start,
            end = end,
            strokeWidth = 2.dp.toPx()
        )
        // Arrow tip circle
        drawCircle(
            color = redColor,
            radius = 0.025f * halfBoard,
            center = end
        )
    }

    // 4 Baselines (Two parallel lines with end circles)
    val baselineCoords = listOf(
        0.65f,  // Bottom (Player 1)
        -0.65f, // Top (Player 2)
    )
    baselineCoords.forEach { baseNormY ->
        val y = halfBoard + (baseNormY * halfBoard)
        val xLeft = halfBoard - (0.55f * halfBoard)
        val xRight = halfBoard + (0.55f * halfBoard)

        // Baseline parallel rail 1
        drawLine(markColor, Offset(xLeft, y - 5.dp.toPx()), Offset(xRight, y - 5.dp.toPx()), strokeWidth = 2.dp.toPx())
        // Baseline parallel rail 2
        drawLine(markColor, Offset(xLeft, y + 5.dp.toPx()), Offset(xRight, y + 5.dp.toPx()), strokeWidth = 2.dp.toPx())

        // End circles with red center
        listOf(Offset(xLeft, y), Offset(xRight, y)).forEach { c ->
            drawCircle(markColor, radius = 0.045f * halfBoard, center = c, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(redColor, radius = 0.032f * halfBoard, center = c)
        }
    }
}

private fun DrawScope.draw3DCarromPiece(piece: CarromPiece, halfBoard: Float) {
    val cx = halfBoard + (piece.x * halfBoard)
    val cy = halfBoard + (piece.y * halfBoard)
    val r = piece.radius * halfBoard * piece.animScale

    if (r <= 1f) return

    // 1. Soft Realistic Drop Shadow under piece
    drawCircle(
        color = Color.Black.copy(alpha = 0.38f),
        radius = r * 1.05f,
        center = Offset(cx + 3.dp.toPx(), cy + 4.dp.toPx())
    )

    when (piece.type) {
        CarromPieceType.WHITE -> {
            // Ivory White 3D Disc with radial gradient bevel
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8F4EE), Color(0xFFDDD2C1), Color(0xFF9E9281)),
                    center = Offset(cx - r * 0.3f, cy - r * 0.35f),
                    radius = r * 1.2f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            // Golden outer ring
            drawCircle(
                color = Color(0xFFD4AF37),
                radius = r * 0.88f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Center indent
            drawCircle(
                color = Color(0xFFC4B59F),
                radius = r * 0.35f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        CarromPieceType.BLACK -> {
            // Obsidian Black 3D Disc with warm bronze highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF555555), Color(0xFF2D2D2D), Color(0xFF141414), Color(0xFF000000)),
                    center = Offset(cx - r * 0.3f, cy - r * 0.35f),
                    radius = r * 1.2f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            // Bronze edge ring
            drawCircle(
                color = Color(0xFFB45309),
                radius = r * 0.88f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Center indent
            drawCircle(
                color = Color(0xFF451A03),
                radius = r * 0.35f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        CarromPieceType.QUEEN -> {
            // Radiant Ruby Crimson Queen with golden star mandala
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF4D6D), Color(0xFFE11D48), Color(0xFF9F1239), Color(0xFF4C0519)),
                    center = Offset(cx - r * 0.3f, cy - r * 0.35f),
                    radius = r * 1.2f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            // Gold edge ring
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = r * 0.88f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            // Golden star core
            drawCircle(
                color = Color(0xFFFDE68A),
                radius = r * 0.40f,
                center = Offset(cx, cy)
            )
        }

        CarromPieceType.STRIKER -> {
            // Premium Tournament Striker with metallic/neon radial finish
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF075985), Color(0xFF0C4A6E)),
                    center = Offset(cx - r * 0.3f, cy - r * 0.35f),
                    radius = r * 1.3f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            // Metallic Gold outer ring
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = r * 0.92f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5.dp.toPx())
            )
            // Inner groove
            drawCircle(
                color = Color(0xFFBAE6FD),
                radius = r * 0.65f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Crosshair aiming center
            val chLen = r * 0.35f
            drawLine(Color.White, Offset(cx - chLen, cy), Offset(cx + chLen, cy), strokeWidth = 2.dp.toPx())
            drawLine(Color.White, Offset(cx, cy - chLen), Offset(cx, cy + chLen), strokeWidth = 2.dp.toPx())
            drawCircle(Color(0xFFF59E0B), radius = r * 0.16f, center = Offset(cx, cy))
        }
    }

    // Top-Left 3D Specular Lighting Shine on all pieces
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = r * 0.22f,
        center = Offset(cx - r * 0.32f, cy - r * 0.32f)
    )
}

private fun Float.roundToIntPx(): Int = Math.round(this)
