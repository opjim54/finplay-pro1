package com.example.ui.screens.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.ReversiBotAi
import com.example.domain.games.ReversiDisc
import com.example.domain.games.ReversiEngine
import com.example.domain.games.ReversiOutcome
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ReversiBoardScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> }
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    var playVsBot by remember { mutableStateOf(true) }
    var botAccuracy by remember { mutableIntStateOf(75) }
    var showRules by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ReversiEngine.newGame()) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastEarnedScore by remember { mutableIntStateOf(0) }

    fun resetReversi() {
        resultRecorded = false
        lastEarnedScore = 0
        state = ReversiEngine.newGame()
    }

    LaunchedEffect(state.outcome) {
        if (state.outcome != ReversiOutcome.ONGOING && !resultRecorded) {
            resultRecorded = true
            val result = when (state.outcome) {
                ReversiOutcome.EMERALD_WINS -> GameMatchResult.WIN
                ReversiOutcome.PEARL_WINS -> GameMatchResult.LOSS
                else -> GameMatchResult.DRAW
            }
            val margin = (state.emeraldScore - state.pearlScore).coerceAtLeast(0)
            val score = (state.emeraldScore * 20) + (margin * 15) + when (result) {
                GameMatchResult.WIN -> 400 + if (playVsBot) botAccuracy * 6 else 120
                GameMatchResult.DRAW -> 220
                GameMatchResult.LOSS -> 50
            }
            lastEarnedScore = score
            onRecordMatch(result, score, playVsBot, botAccuracy)
        }
    }

    if (showRules) {
        GameRulesModalSheet(gameId = ArenaGameId.REVERSI, onDismiss = { showRules = false })
    }

    val isBotTurn = playVsBot && state.outcome == ReversiOutcome.ONGOING && state.turn == ReversiDisc.PEARL
    LaunchedEffect(state.turn, state.emeraldScore, state.pearlScore, state.outcome, playVsBot, botAccuracy) {
        if (!isBotTurn) return@LaunchedEffect
        delay(320L)
        val move = withContext(Dispatchers.Default) {
            ReversiBotAi.chooseMove(state, botAccuracy)
        }
        if (move != null) {
            val next = ReversiEngine.applyMove(state, move.index)
            state = next
            when {
                next.outcome != ReversiOutcome.ONGOING -> GameSoundEngine.play(GameSoundEffect.WIN)
                move.flippedIndices.size >= 3 -> GameSoundEngine.play(GameSoundEffect.CAPTURE)
                else -> GameSoundEngine.play(GameSoundEffect.MOVE)
            }
        }
    }

    val legalMoves = remember(state) {
        if (!isBotTurn && state.outcome == ReversiOutcome.ONGOING) ReversiEngine.legalMoves(state) else emptyList()
    }
    val legalFlipMap = remember(legalMoves) { legalMoves.associate { it.index to it.flippedIndices.size } }

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
                resetReversi()
            },
            onAccuracyChange = { botAccuracy = it },
            onOpenRules = { showRules = true },
            onRestart = { resetReversi() }
        )

        // Score HUD Row
        val emeraldAccent = Color(0xFF10B981)
        val pearlAccent = Color(0xFFD97706)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReversiScoreCard(
                label = gStrings.emeraldName,
                count = state.emeraldScore,
                discBrush = Brush.radialGradient(listOf(Color(0xFF34D399), Color(0xFF064E3B))),
                isTurn = state.outcome == ReversiOutcome.ONGOING && state.turn == ReversiDisc.EMERALD,
                accent = emeraldAccent,
                modifier = Modifier.weight(1f)
            )
            ReversiScoreCard(
                label = if (playVsBot) "${gStrings.pearlName} ${gStrings.botSuffix}" else gStrings.pearlName,
                count = state.pearlScore,
                discBrush = Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD1D5DB))),
                isTurn = state.outcome == ReversiOutcome.ONGOING && state.turn == ReversiDisc.PEARL,
                accent = pearlAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Balance Territory Bar
        val totalDiscs = (state.emeraldScore + state.pearlScore).coerceAtLeast(1)
        val emeraldShare by animateFloatAsState(
            targetValue = state.emeraldScore.toFloat() / totalDiscs,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "reversi_share"
        )
        LinearProgressIndicator(
            progress = { emeraldShare },
            color = emeraldAccent,
            trackColor = Color(0xFFCBD5E1),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        // Status Banner
        val statusMsg = when (state.outcome) {
            ReversiOutcome.EMERALD_WINS -> gStrings.reversiEmeraldWins
            ReversiOutcome.PEARL_WINS -> gStrings.reversiPearlWins
            ReversiOutcome.DRAW -> gStrings.reversiDraw
            ReversiOutcome.ONGOING -> {
                val turnWho = if (state.turn == ReversiDisc.EMERALD) gStrings.emeraldName
                else if (playVsBot) "${gStrings.pearlName} ${gStrings.botThinking}"
                else gStrings.pearlName
                if (state.passedLastTurn) "$turnWho • ${gStrings.reversiTurnPassed}"
                else "$turnWho • ${gStrings.legalMovesHint} ${legalMoves.size}"
            }
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, financeColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusMsg,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        // 8x8 Luxury Emerald Felt Board
        Surface(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.5.dp, financeColors.champagneGold.copy(alpha = 0.7f)),
            color = Color(0xFF042F2E),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .testTag("reversi_board")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (r in 0..7) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (c in 0..7) {
                            val idx = r * 8 + c
                            val disc = state.board[idx]
                            val flipCount = legalFlipMap[idx]
                            val isLastPlaced = state.lastMoveIndex == idx
                            val isJustFlipped = idx in state.lastFlippedIndices
                            val scale by animateFloatAsState(
                                targetValue = if (isLastPlaced || isJustFlipped) 1.09f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "disc_pop_$idx"
                            )
                            val flipRotationY by animateFloatAsState(
                                targetValue = when (disc) {
                                    ReversiDisc.EMERALD -> 0f
                                    ReversiDisc.PEARL -> 180f
                                    ReversiDisc.EMPTY -> 0f
                                },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "disc_flip_$idx"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        when {
                                            isLastPlaced -> Color(0xFF0F766E)
                                            (r + c) % 2 == 0 -> Color(0xFF064E3B)
                                            else -> Color(0xFF065F46)
                                        }
                                    )
                                    .border(
                                        width = if (flipCount != null) 1.dp else 0.dp,
                                        color = if (flipCount != null) financeColors.champagneGold.copy(alpha = 0.55f) else Color.Transparent,
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .clickable(enabled = flipCount != null && !isBotTurn) {
                                        val next = ReversiEngine.applyMove(state, idx)
                                        state = next
                                        when {
                                            next.outcome != ReversiOutcome.ONGOING -> GameSoundEngine.play(GameSoundEffect.WIN)
                                            (flipCount ?: 0) >= 3 -> GameSoundEngine.play(GameSoundEffect.CAPTURE)
                                            else -> GameSoundEngine.play(GameSoundEffect.MOVE)
                                        }
                                    }
                                    .testTag("reversi_cell_$idx"),
                                contentAlignment = Alignment.Center
                            ) {
                                when (disc) {
                                    ReversiDisc.EMERALD -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.78f)
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                    rotationY = flipRotationY
                                                    cameraDistance = 12f * density
                                                }
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(Color(0xFF34D399), Color(0xFF047857), Color(0xFF022C22))
                                                    )
                                                )
                                                .border(1.5.dp, Color(0xFF6EE7B7), CircleShape)
                                        )
                                    }

                                    ReversiDisc.PEARL -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.78f)
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                    rotationY = flipRotationY
                                                    cameraDistance = 12f * density
                                                }
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFF94A3B8))
                                                    )
                                                )
                                                .border(1.5.dp, financeColors.champagneGold, CircleShape)
                                        )
                                    }

                                    ReversiDisc.EMPTY -> {
                                        if (flipCount != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(financeColors.champagneGold.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+$flipCount",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFFDE68A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        GameMatchResultBanner(
            visible = state.outcome != ReversiOutcome.ONGOING,
            headline = statusMsg,
            scoreEarned = lastEarnedScore,
            onPlayAgain = { resetReversi() }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReversiScoreCard(
    label: String,
    count: Int,
    discBrush: Brush,
    isTurn: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isTurn) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isTurn) 1.6.dp else 1.dp, if (isTurn) accent else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(discBrush)
                    .border(1.dp, accent, CircleShape)
            )
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accent)
            }
        }
    }
}
