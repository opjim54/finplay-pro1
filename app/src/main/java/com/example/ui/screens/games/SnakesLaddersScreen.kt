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
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.Ladder
import com.example.domain.games.Snake
import com.example.domain.games.SnakesLaddersEngine
import com.example.domain.games.SnakesLaddersState
import com.example.domain.games.SnakesPhase
import com.example.domain.games.SnakesPlayer
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SnakesLaddersScreen(
    initialPlayerCount: Int = 2,
    initialVsBot: Boolean = true,
    initialBotAccuracy: Int = 75,
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    var playerCount by remember { mutableIntStateOf(initialPlayerCount) }
    var playVsBot by remember { mutableStateOf(initialVsBot) }
    var botAccuracy by remember { mutableIntStateOf(initialBotAccuracy) }
    var isRollingAnim by remember { mutableStateOf(false) }
    var displayDice by remember { mutableIntStateOf(1) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastBannerMsg by remember { mutableStateOf<String?>(null) }

    var state by remember {
        mutableStateOf(
            SnakesLaddersEngine.newGame(playerCount, playVsBot, botAccuracy)
        )
    }

    fun resetGame(count: Int = playerCount, vsBot: Boolean = playVsBot, acc: Int = botAccuracy) {
        resultRecorded = false
        lastBannerMsg = null
        playerCount = count
        playVsBot = vsBot
        botAccuracy = acc
        state = SnakesLaddersEngine.newGame(count, vsBot, acc)
    }

    // Bot Auto-turn handler
    LaunchedEffect(state.currentTurnIndex, state.phase, state.winner) {
        if (state.phase == SnakesPhase.WAIT_ROLL && state.winner == null) {
            val cur = state.currentPlayer
            if (cur.isBot) {
                delay(650L + Random.nextLong(200L, 500L))
                // Execute bot roll
                isRollingAnim = true
                for (i in 0 until 6) {
                    displayDice = Random.nextInt(1, 7)
                    GameSoundEngine.play(GameSoundEffect.DICE_TICK)
                    delay(55L)
                }
                isRollingAnim = false
                val moveRes = SnakesLaddersEngine.rollAndMove(state)
                displayDice = moveRes.nextState.lastDiceValue
                state = moveRes.nextState

                if (moveRes.wasLadder) {
                    GameSoundEngine.play(GameSoundEffect.SPECIAL)
                    lastBannerMsg = "🪜 Bot climbed ladder to ${moveRes.finalPos}!"
                } else if (moveRes.wasSnake) {
                    GameSoundEngine.play(GameSoundEffect.CAPTURE)
                    lastBannerMsg = "🐍 Bot caught by snake to ${moveRes.finalPos}!"
                } else {
                    GameSoundEngine.play(GameSoundEffect.MOVE)
                }
            }
        }
    }

    // Record winner
    LaunchedEffect(state.winner) {
        val winner = state.winner
        if (winner != null && !resultRecorded) {
            resultRecorded = true
            GameSoundEngine.play(GameSoundEffect.WIN)
            val humanWon = !winner.isBot
            val score = 350 + (winner.position * 2)
            onRecordMatch(
                if (humanWon) GameMatchResult.WIN else GameMatchResult.LOSS,
                score,
                state.isVsBot,
                state.botAccuracy
            )
        }
    }

    fun performRoll() {
        if (isRollingAnim || state.phase != SnakesPhase.WAIT_ROLL || state.winner != null) return
        val cur = state.currentPlayer
        if (cur.isBot) return

        scope.launch {
            isRollingAnim = true
            for (i in 0 until 7) {
                displayDice = Random.nextInt(1, 7)
                GameSoundEngine.play(GameSoundEffect.DICE_TICK)
                delay(50L)
            }
            isRollingAnim = false

            val moveRes = SnakesLaddersEngine.rollAndMove(state)
            displayDice = moveRes.nextState.lastDiceValue
            state = moveRes.nextState

            if (moveRes.wasLadder) {
                GameSoundEngine.play(GameSoundEffect.SPECIAL)
                lastBannerMsg = "🪜 Climbed ladder to ${moveRes.finalPos}!"
            } else if (moveRes.wasSnake) {
                GameSoundEngine.play(GameSoundEffect.CAPTURE)
                lastBannerMsg = "🐍 Bitten by snake down to ${moveRes.finalPos}!"
            } else {
                GameSoundEngine.play(GameSoundEffect.MOVE)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF022C22),
                        Color(0xFF064E3B)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.testTag("snakes_exit_btn")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🐍 SNAKES & LADDERS 🪜",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = financeColors.champagneGold
                )
                Text(
                    text = state.statusLog,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA7F3D0),
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { resetGame() },
                modifier = Modifier.testTag("snakes_reset_btn")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White
                )
            }
        }

        // Active Event Banner
        AnimatedVisibility(
            visible = lastBannerMsg != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            lastBannerMsg?.let { banner ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (banner.contains("ladder", ignoreCase = true)) Color(0xFF059669) else Color(0xFFDC2626),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3D Snakes & Ladders 10x10 Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .border(2.5.dp, Brush.linearGradient(listOf(financeColors.champagneGold, Color(0xFF059669))), RoundedCornerShape(20.dp))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boardSize = minOf(maxWidth, maxHeight)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cellSize = w / 10f

                    // 1. Draw 10x10 Cells
                    for (row in 0 until 10) {
                        for (col in 0 until 10) {
                            val left = col * cellSize
                            val top = (9 - row) * cellSize // row 0 at bottom
                            val isEven = (row + col) % 2 == 0

                            // Alternating cell themes
                            val cellColor = if (row == 9 && col == 0) {
                                // Cell 100 (Grand Champion Castle)
                                Color(0xFFF59E0B)
                            } else if (isEven) {
                                Color(0xFFF8FAFC)
                            } else {
                                Color(0xFFE2E8F0)
                            }

                            drawRect(
                                color = cellColor,
                                topLeft = Offset(left, top),
                                size = Size(cellSize, cellSize)
                            )
                            drawRect(
                                color = Color(0xFFCBD5E1),
                                topLeft = Offset(left, top),
                                size = Size(cellSize, cellSize),
                                style = Stroke(width = 1f)
                            )

                            // Cell Number
                            val cellNumber = getCellNumber(row, col)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = cellNumber.toString(),
                                topLeft = Offset(left + 3f, top + 2f),
                                style = TextStyle(
                                    fontSize = (cellSize * 0.22f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cellNumber == 100) Color(0xFF78350F) else Color(0xFF475569)
                                )
                            )
                        }
                    }

                    // 2. Draw Ladders
                    for (ladder in SnakesLaddersEngine.LADDERS) {
                        draw3DLadder(
                            bottomPos = ladder.bottom,
                            topPos = ladder.top,
                            cellSize = cellSize
                        )
                    }

                    // 3. Draw Snakes
                    for (snake in SnakesLaddersEngine.SNAKES) {
                        draw3DSnake(
                            headPos = snake.head,
                            tailPos = snake.tail,
                            cellSize = cellSize
                        )
                    }

                    // 4. Draw Player Tokens
                    // Group tokens on the same cell to calculate offset
                    val cellGroups = state.players.groupBy { it.position }
                    for ((cell, playersOnCell) in cellGroups) {
                        if (cell == 0) continue // In start yard
                        val (row, col) = SnakesLaddersEngine.cellToRowCol(cell)
                        val centerX = col * cellSize + cellSize / 2f
                        val centerY = (9 - row) * cellSize + cellSize / 2f

                        playersOnCell.forEachIndexed { idx, p ->
                            val offset = when (playersOnCell.size) {
                                1 -> Offset.Zero
                                2 -> if (idx == 0) Offset(-cellSize * 0.18f, 0f) else Offset(cellSize * 0.18f, 0f)
                                3 -> when (idx) {
                                    0 -> Offset(-cellSize * 0.18f, -cellSize * 0.15f)
                                    1 -> Offset(cellSize * 0.18f, -cellSize * 0.15f)
                                    else -> Offset(0f, cellSize * 0.18f)
                                }
                                else -> when (idx) {
                                    0 -> Offset(-cellSize * 0.18f, -cellSize * 0.18f)
                                    1 -> Offset(cellSize * 0.18f, -cellSize * 0.18f)
                                    2 -> Offset(-cellSize * 0.18f, cellSize * 0.18f)
                                    else -> Offset(cellSize * 0.18f, cellSize * 0.18f)
                                }
                            }
                            draw3DToken(
                                center = Offset(centerX + offset.x, centerY + offset.y),
                                radius = cellSize * 0.32f,
                                color = Color(p.color),
                                isCurrent = state.currentPlayer.id == p.id
                            )
                        }
                    }
                }
            }
        }

        // Bottom Dashboard: Players & Interactive Dice Roller
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player List / Status Cards
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.players.forEach { p ->
                    val isCurrent = state.currentPlayer.id == p.id
                    val isWin = state.winner?.id == p.id

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) Color(p.color).copy(alpha = 0.22f) else Color(0xFF1E293B).copy(alpha = 0.7f),
                        border = BorderStroke(
                            if (isCurrent) 2.dp else 1.dp,
                            if (isCurrent) Color(p.color) else Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(p.color))
                                )
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) Color.White else Color(0xFFCBD5E1)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = if (p.position == 0) "Yard" else "Tile ${p.position}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWin) financeColors.champagneGold else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // 3D Interactive Dice Roll Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isHumanTurn = !state.currentPlayer.isBot && state.phase == SnakesPhase.WAIT_ROLL && state.winner == null
                val pulseTransition = rememberInfiniteTransition(label = "dice_pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isHumanTurn) 1.08f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .graphicsLayer {
                            scaleX = if (isRollingAnim) 0.9f else pulseScale
                            scaleY = if (isRollingAnim) 0.9f else pulseScale
                            rotationZ = if (isRollingAnim) Random.nextFloat() * 20f - 10f else 0f
                        }
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B))
                            )
                        )
                        .border(
                            2.dp,
                            if (isHumanTurn) Color(state.currentPlayer.color) else Color(0xFFD97706),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = isHumanTurn) { performRoll() }
                        .testTag("snakes_dice_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        drawDiceDots(displayDice)
                    }
                }

                Text(
                    text = when {
                        state.winner != null -> "Finished"
                        isRollingAnim -> "Rolling..."
                        state.currentPlayer.isBot -> "Bot Thinking..."
                        else -> "TAP DICE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isHumanTurn) Color(0xFF34D399) else Color(0xFF94A3B8)
                )
            }
        }

        // Winner Dialog / Bottom Overlay
        if (state.winner != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(2.dp, financeColors.champagneGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🏆 CHAMPION REVEALED 🏆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = financeColors.champagneGold
                    )
                    Text(
                        text = "${state.winner?.name} has reached 100!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play Again")
                        }
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Exit Arena")
                        }
                    }
                }
            }
        }
    }
}

// Helper: Calculate cell number for row (0=bottom) and col (0=left)
private fun getCellNumber(row: Int, col: Int): Int {
    val colInRow = if (row % 2 == 0) col else (9 - col)
    return row * 10 + colInRow + 1
}

// Draw 3D Golden Ladder
private fun DrawScope.draw3DLadder(bottomPos: Int, topPos: Int, cellSize: Float) {
    val (bRow, bCol) = SnakesLaddersEngine.cellToRowCol(bottomPos)
    val (tRow, tCol) = SnakesLaddersEngine.cellToRowCol(topPos)

    val bx = bCol * cellSize + cellSize / 2f
    val by = (9 - bRow) * cellSize + cellSize / 2f
    val tx = tCol * cellSize + cellSize / 2f
    val ty = (9 - tRow) * cellSize + cellSize / 2f

    val dx = tx - bx
    val dy = ty - by
    val angle = atan2(dy, dx)
    val perpX = -sin(angle) * (cellSize * 0.16f)
    val perpY = cos(angle) * (cellSize * 0.16f)

    // Left Rail
    drawLine(
        brush = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFB45309))),
        start = Offset(bx - perpX, by - perpY),
        end = Offset(tx - perpX, ty - perpY),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    // Right Rail
    drawLine(
        brush = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFB45309))),
        start = Offset(bx + perpX, by + perpY),
        end = Offset(tx + perpX, ty + perpY),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Rungs (steps)
    val rungsCount = 6
    for (i in 1..rungsCount) {
        val fraction = i.toFloat() / (rungsCount + 1)
        val rx = bx + dx * fraction
        val ry = by + dy * fraction
        drawLine(
            color = Color(0xFFF59E0B),
            start = Offset(rx - perpX, ry - perpY),
            end = Offset(rx + perpX, ry + perpY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }
}

// Draw 3D Smooth Snake
private fun DrawScope.draw3DSnake(headPos: Int, tailPos: Int, cellSize: Float) {
    val (hRow, hCol) = SnakesLaddersEngine.cellToRowCol(headPos)
    val (tRow, tCol) = SnakesLaddersEngine.cellToRowCol(tailPos)

    val hx = hCol * cellSize + cellSize / 2f
    val hy = (9 - hRow) * cellSize + cellSize / 2f
    val tx = tCol * cellSize + cellSize / 2f
    val ty = (9 - tRow) * cellSize + cellSize / 2f

    val midX = (hx + tx) / 2f + (cellSize * 0.35f)
    val midY = (hy + ty) / 2f - (cellSize * 0.2f)

    val path = Path().apply {
        moveTo(hx, hy)
        quadraticTo(midX, midY, tx, ty)
    }

    // Shadow
    drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.35f),
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )

    // Snake Body with Gradient
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            listOf(Color(0xFFE11D48), Color(0xFF059669), Color(0xFF10B981))
        ),
        style = Stroke(width = 6.5f, cap = StrokeCap.Round)
    )

    // Snake Head (Oval)
    drawCircle(
        color = Color(0xFF9F1239),
        radius = cellSize * 0.22f,
        center = Offset(hx, hy)
    )
    // Eyes
    drawCircle(
        color = Color.Yellow,
        radius = cellSize * 0.06f,
        center = Offset(hx - cellSize * 0.08f, hy - cellSize * 0.05f)
    )
    drawCircle(
        color = Color.Yellow,
        radius = cellSize * 0.06f,
        center = Offset(hx + cellSize * 0.08f, hy - cellSize * 0.05f)
    )
}

// Draw 3D Player Token with Rim and Center
private fun DrawScope.draw3DToken(center: Offset, radius: Float, color: Color, isCurrent: Boolean) {
    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = radius,
        center = Offset(center.x + 2f, center.y + 3f)
    )
    // Outer metallic ring
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White, color, Color.Black),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.2f
        ),
        radius = radius,
        center = center
    )
    // Inner vibrant core
    drawCircle(
        color = color,
        radius = radius * 0.72f,
        center = center
    )
    // Highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = radius * 0.3f,
        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f)
    )

    if (isCurrent) {
        drawCircle(
            color = Color.White,
            radius = radius * 1.15f,
            center = center,
            style = Stroke(width = 2.5f)
        )
    }
}

// Draw Dots on Dice Face (1 to 6)
private fun DrawScope.drawDiceDots(value: Int) {
    val dotRadius = size.width * 0.1f
    val dotColor = Color(0xFF1E293B)

    val cX = size.width / 2f
    val cY = size.height / 2f
    val left = size.width * 0.25f
    val right = size.width * 0.75f
    val top = size.height * 0.25f
    val bottom = size.height * 0.75f

    when (value) {
        1 -> drawCircle(dotColor, dotRadius, Offset(cX, cY))
        2 -> {
            drawCircle(dotColor, dotRadius, Offset(left, top))
            drawCircle(dotColor, dotRadius, Offset(right, bottom))
        }
        3 -> {
            drawCircle(dotColor, dotRadius, Offset(left, top))
            drawCircle(dotColor, dotRadius, Offset(cX, cY))
            drawCircle(dotColor, dotRadius, Offset(right, bottom))
        }
        4 -> {
            drawCircle(dotColor, dotRadius, Offset(left, top))
            drawCircle(dotColor, dotRadius, Offset(right, top))
            drawCircle(dotColor, dotRadius, Offset(left, bottom))
            drawCircle(dotColor, dotRadius, Offset(right, bottom))
        }
        5 -> {
            drawCircle(dotColor, dotRadius, Offset(left, top))
            drawCircle(dotColor, dotRadius, Offset(right, top))
            drawCircle(dotColor, dotRadius, Offset(cX, cY))
            drawCircle(dotColor, dotRadius, Offset(left, bottom))
            drawCircle(dotColor, dotRadius, Offset(right, bottom))
        }
        6 -> {
            drawCircle(dotColor, dotRadius, Offset(left, top))
            drawCircle(dotColor, dotRadius, Offset(right, top))
            drawCircle(dotColor, dotRadius, Offset(left, cY))
            drawCircle(dotColor, dotRadius, Offset(right, cY))
            drawCircle(dotColor, dotRadius, Offset(left, bottom))
            drawCircle(dotColor, dotRadius, Offset(right, bottom))
        }
    }
}
