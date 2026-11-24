package com.example.ui.screens.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.domain.games.SholoGutiBotAi
import com.example.domain.games.SholoGutiEngine
import com.example.domain.games.SholoGutiMove
import com.example.domain.games.SholoGutiOutcome
import com.example.domain.games.SholoGutiPlayer
import com.example.domain.games.SholoGutiState
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.hypot

@Composable
fun SholoGutiBoardScreen(
    initialVsBot: Boolean = true,
    initialBotAccuracy: Int = 75,
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> },
    onExit: () -> Unit = {}
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    var playVsBot by remember { mutableStateOf(initialVsBot) }
    var botAccuracy by remember { mutableIntStateOf(initialBotAccuracy) }
    var showRules by remember { mutableStateOf(false) }
    var matchSessionId by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(SholoGutiEngine.newGame()) }
    var selectedNode by remember { mutableStateOf<Int?>(null) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastEarnedScore by remember { mutableIntStateOf(0) }

    fun resetMatch() {
        matchSessionId++
        selectedNode = null
        resultRecorded = false
        lastEarnedScore = 0
        state = SholoGutiEngine.newGame()
    }

    if (showRules) {
        GameRulesModalSheet(gameId = ArenaGameId.SHOLO_GUTI, onDismiss = { showRules = false })
    }

    // Record finished match once
    LaunchedEffect(state.outcome) {
        if (state.outcome != SholoGutiOutcome.ONGOING && !resultRecorded) {
            resultRecorded = true
            GameSoundEngine.play(if (state.outcome == SholoGutiOutcome.RUBY_WINS) GameSoundEffect.WIN else GameSoundEffect.MOVE)
            val result = when (state.outcome) {
                SholoGutiOutcome.RUBY_WINS -> GameMatchResult.WIN
                SholoGutiOutcome.GOLD_WINS -> GameMatchResult.LOSS
                else -> GameMatchResult.DRAW
            }
            val score = (state.rubyCaptured * 45) + (state.rubyCount * 20) + when (result) {
                GameMatchResult.WIN -> 500 + if (playVsBot) botAccuracy * 5 else 150
                GameMatchResult.DRAW -> 200
                GameMatchResult.LOSS -> 60
            }
            lastEarnedScore = score
            onRecordMatch(result, score, playVsBot, botAccuracy)
        }
    }

    // Keep selectedNode locked to chained bead during multi-jump chain
    LaunchedEffect(state.chainedNodeId, state.turn) {
        if (state.chainedNodeId != null) {
            selectedNode = state.chainedNodeId
        }
    }

    // Bot turn driver (handles single moves and full multi-jump capture chains!)
    val isBotTurn = playVsBot && state.outcome == SholoGutiOutcome.ONGOING && state.turn == SholoGutiPlayer.GOLD
    LaunchedEffect(state.turnSequence, playVsBot, botAccuracy, matchSessionId) {
        if (!isBotTurn) return@LaunchedEffect
        val session = matchSessionId
        delay(if (state.chainedNodeId != null) 380L else 480L)
        if (session != matchSessionId) return@LaunchedEffect
        val snapshot = state
        val move = withContext(Dispatchers.Default) {
            SholoGutiBotAi.chooseMove(snapshot, botAccuracy)
        }
        if (session != matchSessionId) return@LaunchedEffect
        if (move != null) {
            val next = SholoGutiEngine.applyMove(state, move)
            state = next
            selectedNode = next.chainedNodeId
            GameSoundEngine.play(
                when {
                    next.outcome != SholoGutiOutcome.ONGOING -> GameSoundEffect.WIN
                    move.isCapture -> GameSoundEffect.CAPTURE
                    else -> GameSoundEffect.MOVE
                }
            )
        } else if (state.chainedNodeId != null) {
            state = SholoGutiEngine.endCaptureChain(state)
        }
    }

    val legalMovesForSelected = remember(state, selectedNode, isBotTurn) {
        if (isBotTurn || selectedNode == null) emptyList()
        else SholoGutiEngine.legalMovesForNode(state, selectedNode!!)
    }
    val legalTargetMap = remember(legalMovesForSelected) {
        legalMovesForSelected.associateBy { it.to }
    }

    val pulseTransition = rememberInfiniteTransition(label = "sholo_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "sholo_pulse_val"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A101D), Color(0xFF0F2622), Color(0xFF0A101D))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UniversalBotControlBar(
            playVsBot = playVsBot,
            accuracyPercent = botAccuracy,
            onToggleBot = { enabled ->
                playVsBot = enabled
                resetMatch()
            },
            onAccuracyChange = { botAccuracy = it },
            onOpenRules = { showRules = true },
            onRestart = { resetMatch() }
        )

        // Score & Bead Counter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SholoPlayerScorePill(
                title = if (gStrings.isBn) "রুবি (আপনি)" else "Ruby (You)",
                beadsLeft = state.rubyCount,
                captured = state.rubyCaptured,
                accent = Color(0xFFE11D48),
                isActive = state.turn == SholoGutiPlayer.RUBY && state.outcome == SholoGutiOutcome.ONGOING,
                modifier = Modifier.weight(1f)
            )
            SholoPlayerScorePill(
                title = if (playVsBot) {
                    if (gStrings.isBn) "গোল্ড (বট)" else "Gold (Bot)"
                } else {
                    if (gStrings.isBn) "গোল্ড (P2)" else "Gold (P2)"
                },
                beadsLeft = state.goldCount,
                captured = state.goldCaptured,
                accent = Color(0xFFF5C451),
                isActive = state.turn == SholoGutiPlayer.GOLD && state.outcome == SholoGutiOutcome.ONGOING,
                modifier = Modifier.weight(1f)
            )
        }

        // Turn Status & Multi-Jump Chain Control
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141F35),
            border = BorderStroke(1.dp, financeColors.champagneGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.statusLog,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.weight(1f)
                )
                if (state.chainedNodeId != null && !isBotTurn) {
                    OutlinedButton(
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.TAP)
                            state = SholoGutiEngine.endCaptureChain(state)
                            selectedNode = null
                        },
                        border = BorderStroke(1.dp, Color(0xFFF5C451)),
                        modifier = Modifier.testTag("sholo_end_chain_btn")
                    ) {
                        Text(
                            text = if (gStrings.isBn) "চেইন শেষ" else "End Chain",
                            color = Color(0xFFF5C451),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Premium Carved Walnut & Gold-Inlaid 37-Point Sholo Guti Board
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF2B1B12),
            border = BorderStroke(
                width = 2.5.dp,
                brush = Brush.linearGradient(listOf(Color(0xFFF5C451), Color(0xFFD97706), Color(0xFFFDE68A)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(16.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .testTag("sholo_guti_board_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state, selectedNode, isBotTurn) {
                        detectTapGestures { tap ->
                            if (isBotTurn || state.outcome != SholoGutiOutcome.ONGOING) return@detectTapGestures
                            val nx = tap.x / size.width.coerceAtLeast(1)
                            val ny = tap.y / size.height.coerceAtLeast(1)
                            val nearest = SholoGutiEngine.NODES.minByOrNull { node ->
                                hypot((nx - node.normX).toDouble(), (ny - node.normY).toDouble())
                            } ?: return@detectTapGestures
                            val dist = hypot((nx - nearest.normX).toDouble(), (ny - nearest.normY).toDouble())
                            if (dist > 0.105) return@detectTapGestures

                            val targetMove = legalTargetMap[nearest.id]
                            if (targetMove != null) {
                                val next = SholoGutiEngine.applyMove(state, targetMove)
                                state = next
                                selectedNode = next.chainedNodeId
                                GameSoundEngine.play(
                                    when {
                                        next.outcome != SholoGutiOutcome.ONGOING -> GameSoundEffect.WIN
                                        targetMove.isCapture -> GameSoundEffect.CAPTURE
                                        else -> GameSoundEffect.MOVE
                                    }
                                )
                            } else if (state.chainedNodeId == null && state.board[nearest.id] == state.turn) {
                                GameSoundEngine.play(GameSoundEffect.TAP)
                                selectedNode = if (selectedNode == nearest.id) null else nearest.id
                            } else if (state.chainedNodeId == null) {
                                selectedNode = null
                            }
                        }
                    }
            ) {
                drawSholoGutiBoard(
                    state = state,
                    selectedNode = selectedNode,
                    legalMoves = legalMovesForSelected,
                    pulse = pulse
                )
            }
        }

        // Result Banner
        if (state.outcome != SholoGutiOutcome.ONGOING) {
            val headline = when (state.outcome) {
                SholoGutiOutcome.RUBY_WINS -> if (gStrings.isBn) "🏆 রুবি জয়ী! ১৬ গুটি চ্যাম্পিয়ন!" else "🏆 Ruby Wins! Sholo Guti Champion!"
                SholoGutiOutcome.GOLD_WINS -> if (gStrings.isBn) "🏆 গোল্ড জয়ী হয়েছে!" else "🏆 Gold Wins the Match!"
                else -> if (gStrings.isBn) "ম্যাচ ড্র হয়েছে!" else "Match Drawn!"
            }
            GameMatchResultBanner(
                visible = true,
                headline = headline,
                scoreEarned = lastEarnedScore,
                onPlayAgain = { resetMatch() }
            )
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (gStrings.isBn) "গেম এরিনায় ফিরে যান" else "Back to Game Arena")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SholoPlayerScorePill(
    title: String,
    beadsLeft: Int,
    captured: Int,
    accent: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) accent.copy(alpha = 0.18f) else Color(0xFF141F35),
        border = BorderStroke(if (isActive) 1.8.dp else 1.dp, if (isActive) accent else Color(0xFF334155)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), accent)))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isActive) Color.White else Color(0xFFCBD5E1),
                    maxLines = 1
                )
                Text(
                    text = "Beads: $beadsLeft/16 • Cap: $captured",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) accent else Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

internal fun DrawScope.drawSholoGutiBoard(
    state: SholoGutiState,
    selectedNode: Int?,
    legalMoves: List<SholoGutiMove>,
    pulse: Float
) {
    val w = size.width
    val h = size.height

    // 1. Rich Walnut Wood & Emerald Inlay Background
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF3B2416), Color(0xFF24150D), Color(0xFF3B2416))
        ),
        size = size
    )
    // Subtle carved central emerald felt diamond mat
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color(0xFF064E3B).copy(alpha = 0.45f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.62f
        ),
        size = size
    )

    // 2. Gold Inlaid Board Lines (Shadow + Brass Gold Stroke)
    for ((a, b) in SholoGutiEngine.EDGES) {
        val na = SholoGutiEngine.NODES[a]
        val nb = SholoGutiEngine.NODES[b]
        val p1 = Offset(na.normX * w, na.normY * h)
        val p2 = Offset(nb.normX * w, nb.normY * h)
        drawLine(
            color = Color.Black.copy(alpha = 0.45f),
            start = p1 + Offset(1.5f, 2f),
            end = p2 + Offset(1.5f, 2f),
            strokeWidth = 5.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFEAB308).copy(alpha = 0.78f),
            start = p1,
            end = p2,
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )
    }

    // 3. Highlight last move line
    state.lastMove?.let { mv ->
        val nf = SholoGutiEngine.NODES[mv.from]
        val nt = SholoGutiEngine.NODES[mv.to]
        drawLine(
            color = Color(0xFF34D399).copy(alpha = 0.55f),
            start = Offset(nf.normX * w, nf.normY * h),
            end = Offset(nt.normX * w, nt.normY * h),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }

    val legalByDest = legalMoves.associateBy { it.to }
    val beadRadius = (w * 0.052f).coerceIn(14f, 34f)

    // 4. Intersection sockets, legal target rings, and 3D glossy marble beads
    for (node in SholoGutiEngine.NODES) {
        val ctr = Offset(node.normX * w, node.normY * h)
        val isCenter = node.id == 18

        // Carved intersection socket
        drawCircle(
            color = if (isCenter) Color(0xFFF5C451).copy(alpha = 0.35f) else Color(0xFF1A0F0A),
            radius = beadRadius * 0.42f,
            center = ctr
        )
        drawCircle(
            color = Color(0xFFD97706).copy(alpha = 0.65f),
            radius = beadRadius * 0.42f,
            center = ctr,
            style = Stroke(1.6f)
        )

        // Legal destination indicator
        val legalMv = legalByDest[node.id]
        if (legalMv != null) {
            val ringColor = if (legalMv.isCapture) Color(0xFF10B981) else Color(0xFFFACC15)
            drawCircle(
                color = ringColor.copy(alpha = 0.32f),
                radius = beadRadius * 1.12f * pulse,
                center = ctr
            )
            drawCircle(
                color = ringColor,
                radius = beadRadius * 0.72f,
                center = ctr,
                style = Stroke(3.2f)
            )
        }

        // Bead rendering
        val owner = state.board[node.id]
        if (owner != null) {
            val isSelected = selectedNode == node.id
            if (isSelected) {
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.48f),
                    radius = beadRadius * 1.32f * pulse,
                    center = ctr
                )
            }
            // Drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.48f),
                radius = beadRadius,
                center = ctr + Offset(2.5f, 4f)
            )
            val coreColor = if (owner == SholoGutiPlayer.RUBY) Color(0xFFE11D48) else Color(0xFFF5C451)
            val deepColor = if (owner == SholoGutiPlayer.RUBY) Color(0xFF881337) else Color(0xFF92400E)
            val rimColor = if (owner == SholoGutiPlayer.RUBY) Color(0xFFFDA4AF) else Color(0xFFFEF08A)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, coreColor, deepColor),
                    center = Offset(ctr.x - beadRadius * 0.32f, ctr.y - beadRadius * 0.32f),
                    radius = beadRadius * 1.25f
                ),
                radius = beadRadius,
                center = ctr
            )
            drawCircle(
                color = if (isSelected) Color.White else rimColor.copy(alpha = 0.75f),
                radius = beadRadius,
                center = ctr,
                style = Stroke(if (isSelected) 3.2f else 1.8f)
            )
            // Specular marble shine
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = beadRadius * 0.28f,
                center = Offset(ctr.x - beadRadius * 0.28f, ctr.y - beadRadius * 0.28f)
            )
        }
    }
}
