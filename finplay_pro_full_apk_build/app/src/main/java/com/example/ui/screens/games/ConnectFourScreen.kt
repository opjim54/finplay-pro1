package com.example.ui.screens.games

import androidx.compose.animation.core.Animatable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.ConnectFourBotAi
import com.example.domain.games.ConnectFourDisc
import com.example.domain.games.ConnectFourEngine
import com.example.domain.games.ConnectFourOutcome
import com.example.domain.games.ConnectFourState
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ConnectFourScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> }
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    var playVsBot by remember { mutableStateOf(true) }
    var botAccuracy by remember { mutableIntStateOf(75) }
    var showRules by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ConnectFourEngine.newGame()) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastEarnedScore by remember { mutableIntStateOf(0) }

    // Gravity drop spring offset for the most recently dropped disc
    val dropOffsetY = remember { Animatable(0f) }
    LaunchedEffect(state.moveCount, state.lastDropIndex) {
        val dropIdx = state.lastDropIndex
        if (dropIdx != null) {
            val targetRow = dropIdx / ConnectFourState.COLS
            dropOffsetY.snapTo(-((targetRow + 1.4f) * 90f))
            dropOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    fun resetConnectFour() {
        resultRecorded = false
        lastEarnedScore = 0
        state = ConnectFourEngine.newGame()
    }

    LaunchedEffect(state.outcome) {
        if (state.outcome != ConnectFourOutcome.ONGOING && !resultRecorded) {
            resultRecorded = true
            val result = when (state.outcome) {
                ConnectFourOutcome.SOLAR_WINS -> GameMatchResult.WIN
                ConnectFourOutcome.CRIMSON_WINS -> GameMatchResult.LOSS
                else -> GameMatchResult.DRAW
            }
            val score = when (result) {
                GameMatchResult.WIN -> (700 - state.moveCount * 12).coerceAtLeast(250) +
                    if (playVsBot) botAccuracy * 6 else 150
                GameMatchResult.DRAW -> 260
                GameMatchResult.LOSS -> (state.moveCount * 8).coerceAtLeast(40)
            }
            lastEarnedScore = score
            onRecordMatch(result, score, playVsBot, botAccuracy)
        }
    }

    if (showRules) {
        GameRulesModalSheet(gameId = ArenaGameId.CONNECT_FOUR, onDismiss = { showRules = false })
    }

    val isBotTurn = playVsBot && state.outcome == ConnectFourOutcome.ONGOING && state.turn == ConnectFourDisc.CRIMSON
    LaunchedEffect(state.moveCount, state.outcome, playVsBot, botAccuracy) {
        if (!isBotTurn) return@LaunchedEffect
        delay(320L)
        val col = withContext(Dispatchers.Default) {
            ConnectFourBotAi.chooseColumn(state, botAccuracy)
        }
        if (col != null) {
            val next = ConnectFourEngine.dropDisc(state, col)
            state = next
            GameSoundEngine.play(if (next.outcome != ConnectFourOutcome.ONGOING) GameSoundEffect.WIN else GameSoundEffect.MOVE)
        }
    }

    val solarAccent = Color(0xFFF59E0B)
    val crimsonAccent = Color(0xFFE11D48)

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
                resetConnectFour()
            },
            onAccuracyChange = { botAccuracy = it },
            onOpenRules = { showRules = true },
            onRestart = { resetConnectFour() }
        )

        // Status Header
        val statusMsg = when (state.outcome) {
            ConnectFourOutcome.SOLAR_WINS -> gStrings.connectFourSolarWins
            ConnectFourOutcome.CRIMSON_WINS -> gStrings.connectFourCrimsonWins
            ConnectFourOutcome.DRAW -> gStrings.connectFourDraw
            ConnectFourOutcome.ONGOING -> {
                val who = if (state.turn == ConnectFourDisc.SOLAR) gStrings.solarGoldName
                else if (playVsBot) "${gStrings.crimsonRubyName} ${gStrings.botThinking}"
                else gStrings.crimsonRubyName
                "$who • ${gStrings.tapColumnToDrop}"
            }
        }
        val activeAccent = if (state.turn == ConnectFourDisc.SOLAR) solarAccent else crimsonAccent
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = activeAccent.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(activeAccent)
                        .border(1.5.dp, Color.White, CircleShape)
                )
                Text(
                    text = statusMsg,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Column Drop Header Pills (1..7)
        val legalCols = remember(state) { ConnectFourEngine.legalColumns(state).toSet() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (c in 0 until ConnectFourState.COLS) {
                val enabled = !isBotTurn && c in legalCols && state.outcome == ConnectFourOutcome.ONGOING
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (enabled) activeAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (enabled) activeAccent else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            val next = ConnectFourEngine.dropDisc(state, c)
                            state = next
                            GameSoundEngine.play(if (next.outcome != ConnectFourOutcome.ONGOING) GameSoundEffect.WIN else GameSoundEffect.MOVE)
                        }
                        .testTag("connect4_col_$c")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼${c + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        // 7x6 Luxury Emerald Cabinet Grid
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.5.dp, financeColors.champagneGold.copy(alpha = 0.75f)),
            color = Color(0xFF064E3B),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(7f / 6f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("connect4_board")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF022C22)))
                    )
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (r in 0 until ConnectFourState.ROWS) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in 0 until ConnectFourState.COLS) {
                                val idx = r * ConnectFourState.COLS + c
                                val disc = state.board[idx]
                                val isWinning = idx in state.winningCells
                                val isLastDrop = state.lastDropIndex == idx
                                val popScale by animateFloatAsState(
                                    targetValue = if (isWinning) 1.1f else if (isLastDrop) 1.04f else 1f,
                                    animationSpec = spring(dampingRatio = 0.55f),
                                    label = "c4_disc_$idx"
                                )
                                val canClick = !isBotTurn && c in legalCols && state.outcome == ConnectFourOutcome.ONGOING

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = popScale
                                            scaleY = popScale
                                            translationY = if (isLastDrop) dropOffsetY.value else 0f
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            when (disc) {
                                                ConnectFourDisc.EMPTY -> Brush.radialGradient(
                                                    listOf(Color(0xFF0F172A), Color(0xFF020617))
                                                )
                                                ConnectFourDisc.SOLAR -> Brush.radialGradient(
                                                    listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                                                )
                                                ConnectFourDisc.CRIMSON -> Brush.radialGradient(
                                                    listOf(Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF881337))
                                                )
                                            }
                                        )
                                        .border(
                                            width = if (isWinning) 3.dp else if (isLastDrop) 2.dp else 1.dp,
                                            color = when {
                                                isWinning -> Color(0xFFFDE047)
                                                isLastDrop -> Color.White
                                                else -> Color(0xFF1E293B)
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable(enabled = canClick) {
                                            val next = ConnectFourEngine.dropDisc(state, c)
                                            state = next
                                            GameSoundEngine.play(
                                                if (next.outcome != ConnectFourOutcome.ONGOING) GameSoundEffect.WIN else GameSoundEffect.MOVE
                                            )
                                        }
                                        .testTag("connect4_cell_$idx")
                                )
                            }
                        }
                    }
                }
            }
        }

        GameMatchResultBanner(
            visible = state.outcome != ConnectFourOutcome.ONGOING,
            headline = statusMsg,
            scoreEarned = lastEarnedScore,
            onPlayAgain = { resetConnectFour() }
        )

        Spacer(Modifier.height(24.dp))
    }
}
