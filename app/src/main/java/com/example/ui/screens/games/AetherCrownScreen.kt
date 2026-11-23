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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.AetherAction
import com.example.domain.games.AetherBotAi
import com.example.domain.games.AetherCrownEngine
import com.example.domain.games.AetherCrownState
import com.example.domain.games.AetherMoveType
import com.example.domain.games.AetherPlayer
import com.example.domain.games.AetherTurnPhase
import com.example.domain.games.AetherZone
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.i18n.LanguageManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AetherCrownScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> }
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val scope = rememberCoroutineScope()

    var playVsBot by remember { mutableStateOf(true) }
    var botAccuracy by remember { mutableIntStateOf(75) }
    var showRules by remember { mutableStateOf(false) }
    var rollingDiceAnim by remember { mutableStateOf(false) }
    var dicePreview by remember { mutableStateOf(1 to 1) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastEarnedScore by remember { mutableIntStateOf(0) }

    var state by remember { mutableStateOf(AetherCrownEngine.newGame(playVsBot)) }

    fun resetAether(vsBot: Boolean = playVsBot) {
        resultRecorded = false
        lastEarnedScore = 0
        state = AetherCrownEngine.newGame(vsBot)
    }

    LaunchedEffect(state.phase, state.winner) {
        val winner = state.winner
        if (state.phase == AetherTurnPhase.GAME_OVER && winner != null && !resultRecorded) {
            resultRecorded = true
            val humanWon = winner == AetherPlayer.SOLAR
            val solarCharges = state.charges[AetherPlayer.SOLAR] ?: 0
            val solarCrowned = state.tokens[AetherPlayer.SOLAR].orEmpty().count { it.zone == AetherZone.CROWNED }
            val score = (solarCharges * 100) + (solarCrowned * 280) +
                if (humanWon) 500 + if (playVsBot) botAccuracy * 6 else 150 else 60
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
        GameRulesModalSheet(gameId = ArenaGameId.AETHER_CROWN, onDismiss = { showRules = false })
    }

    suspend fun rollTwinDice(cur: AetherCrownState) {
        if (rollingDiceAnim || cur.phase != AetherTurnPhase.ROLL_DICE) return
        rollingDiceAnim = true
        for (i in 0..5) {
            dicePreview = Random.nextInt(1, 5) to Random.nextInt(1, 5)
            if (i % 2 == 0) GameSoundEngine.play(GameSoundEffect.DICE_TICK)
            delay(48L + i * 10L)
        }
        val d1 = Random.nextInt(1, 5)
        val d2 = Random.nextInt(1, 5)
        dicePreview = d1 to d2
        state = AetherCrownEngine.rollDice(cur, d1, d2)
        if (d1 == d2) GameSoundEngine.play(GameSoundEffect.SPECIAL)
        rollingDiceAnim = false
    }

    fun applyWithSfx(cur: AetherCrownState, action: AetherAction) {
        val next = AetherCrownEngine.applyAction(cur, action)
        state = next
        when {
            next.winner != null -> GameSoundEngine.play(GameSoundEffect.WIN)
            action.type == AetherMoveType.QUANTUM_SWAP ||
                action.type == AetherMoveType.CROSS_FLUX_BRIDGE ||
                action.type == AetherMoveType.CROWN_FROM_VAULT -> GameSoundEngine.play(GameSoundEffect.SPECIAL)
            else -> GameSoundEngine.play(GameSoundEffect.MOVE)
        }
    }

    val isBotTurn = playVsBot && state.phase != AetherTurnPhase.GAME_OVER && state.turn == AetherPlayer.LUNAR
    LaunchedEffect(state.turn, state.phase, state.remainingDice, playVsBot, botAccuracy) {
        if (!isBotTurn) return@LaunchedEffect
        delay(340L)
        if (state.phase == AetherTurnPhase.ROLL_DICE) {
            rollTwinDice(state)
        } else if (state.phase == AetherTurnPhase.ALLOCATE_DICE) {
            val action = withContext(Dispatchers.Default) {
                AetherBotAi.chooseAction(state, botAccuracy)
            }
            if (action != null) {
                delay(240L)
                applyWithSfx(state, action)
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "aetherOrb")
    val glowPulse by pulse.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "aetherGlow"
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
                resetAether(enabled)
            },
            onAccuracyChange = { botAccuracy = it },
            onOpenRules = { showRules = true },
            onRestart = { resetAether(playVsBot) }
        )

        // Score & Charge Bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerChargeCard(
                label = gStrings.aetherPlayerName(AetherPlayer.SOLAR),
                chargesLabel = gStrings.chargesLabel,
                crownedLabel = gStrings.crownedLabel,
                player = AetherPlayer.SOLAR,
                isTurn = state.turn == AetherPlayer.SOLAR && state.phase != AetherTurnPhase.GAME_OVER,
                charges = state.charges[AetherPlayer.SOLAR] ?: 0,
                crowned = state.tokens[AetherPlayer.SOLAR].orEmpty().count { it.zone == AetherZone.CROWNED },
                modifier = Modifier.weight(1f)
            )
            PlayerChargeCard(
                label = gStrings.aetherPlayerName(AetherPlayer.LUNAR) + if (playVsBot) " ${gStrings.botSuffix}" else "",
                chargesLabel = gStrings.chargesLabel,
                crownedLabel = gStrings.crownedLabel,
                player = AetherPlayer.LUNAR,
                isTurn = state.turn == AetherPlayer.LUNAR && state.phase != AetherTurnPhase.GAME_OVER,
                charges = state.charges[AetherPlayer.LUNAR] ?: 0,
                crowned = state.tokens[AetherPlayer.LUNAR].orEmpty().count { it.zone == AetherZone.CROWNED },
                modifier = Modifier.weight(1f)
            )
        }

        // Orbital Board Canvas
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF07131A),
            border = BorderStroke(1.8.dp, financeColors.champagneGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.06f)
                .clip(RoundedCornerShape(22.dp))
                .testTag("aether_board_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAetherOrbitalBoard(this, state, glowPulse)
            }
        }

        // Status, Bridge Phase & Twin Dice Readout
        val playerName = gStrings.aetherPlayerName(state.turn)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(state.turn.colorHex).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(state.turn.colorHex).copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.winner != null) {
                            "🏆 ${gStrings.aetherPlayerName(state.winner!!)} ${gStrings.winsSuffix}"
                        } else {
                            "${gStrings.roundLabel} ${state.roundCount} • $playerName"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(state.turn.colorHex)
                    )
                    Text(
                        text = if (state.bridgesPhaseEven) "⚡ ${gStrings.bridgesEvenLabel}" else "⚡ ${gStrings.bridgesOddLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                    Text(state.statusLog, style = MaterialTheme.typography.bodySmall)
                }
                val twinScale by animateFloatAsState(
                    targetValue = if (rollingDiceAnim) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "aether_twin_dice_scale"
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, financeColors.champagneGold.copy(alpha = 0.6f)),
                    modifier = Modifier.graphicsLayer {
                        scaleX = twinScale
                        scaleY = twinScale
                    }
                ) {
                    val shown = if (rollingDiceAnim) dicePreview else state.dice
                    Text(
                        text = "[${shown.first}] [${shown.second}]",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(state.turn.colorHex),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Roll or Choose Move Actions
        when {
            state.phase == AetherTurnPhase.GAME_OVER -> {
                GameMatchResultBanner(
                    visible = true,
                    headline = state.winner?.let { "🏆 ${gStrings.aetherPlayerName(it)} ${gStrings.winsSuffix}" }.orEmpty(),
                    scoreEarned = lastEarnedScore,
                    onPlayAgain = { resetAether(playVsBot) }
                )
            }

            !isBotTurn && state.phase == AetherTurnPhase.ROLL_DICE -> {
                Button(
                    enabled = !rollingDiceAnim,
                    onClick = { scope.launch { rollTwinDice(state) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(state.turn.colorHex)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aether_roll_btn")
                ) {
                    Text(
                        if (rollingDiceAnim) gStrings.chargingTwinDice else gStrings.rollTwinDice,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            !isBotTurn && state.phase == AetherTurnPhase.ALLOCATE_DICE -> {
                val actions = remember(state) { AetherCrownEngine.legalActions(state) }
                Text(
                    "${gStrings.chooseActionPrompt} (${gStrings.diceLeftLabel} ${state.remainingDice.joinToString(", ") { "[$it]" }})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                actions.forEachIndexed { idx, act ->
                    val highlight = act.type == AetherMoveType.CROWN_FROM_VAULT ||
                        act.type == AetherMoveType.CROSS_FLUX_BRIDGE ||
                        act.type == AetherMoveType.QUANTUM_SWAP ||
                        act.type == AetherMoveType.SURGE_MOVE
                    OutlinedButton(
                        onClick = { applyWithSfx(state, act) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.2.dp,
                            if (highlight) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("aether_action_$idx")
                    ) {
                        Text(
                            text = act.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlayerChargeCard(
    label: String,
    chargesLabel: String,
    crownedLabel: String,
    player: AetherPlayer,
    isTurn: Boolean,
    charges: Int,
    crowned: Int,
    modifier: Modifier = Modifier
) {
    val c = Color(player.colorHex)
    val animatedCharge by animateFloatAsState(
        targetValue = (charges / 7f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aether_charge_${player.name}"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isTurn) c.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isTurn) 1.6.dp else 1.dp, if (isTurn) c else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = c)
            Text("$chargesLabel: $charges/7 • $crownedLabel: $crowned/3", style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(
                progress = { animatedCharge },
                color = c,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

private fun drawAetherOrbitalBoard(scope: DrawScope, state: AetherCrownState, glowPulse: Float) = with(scope) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val minSide = minOf(size.width, size.height)
    val outerR = minSide * 0.40f
    val vaultR = minSide * 0.22f

    // Deep cosmic backdrop gradient
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF0F292E), Color(0xFF07131A)),
            center = Offset(cx, cy),
            radius = minSide * 0.6f
        ),
        radius = minSide * 0.6f,
        center = Offset(cx, cy)
    )

    // Outer & Vault orbital rings
    drawCircle(Color(0xFF1E3A40), radius = outerR, center = Offset(cx, cy), style = Stroke(3.2f))
    drawCircle(Color(0xFF334155), radius = vaultR, center = Offset(cx, cy), style = Stroke(2.5f))

    fun outerPos(node: Int): Offset {
        val angle = -PI / 2.0 + (node * (2.0 * PI / 16.0))
        return Offset((cx + cos(angle) * outerR).toFloat(), (cy + sin(angle) * outerR).toFloat())
    }

    fun vaultPos(vIdx: Int): Offset {
        val outerAligned = AetherCrownEngine.VAULT_TO_BRIDGE_OUTER[vIdx] ?: 2
        val angle = -PI / 2.0 + (outerAligned * (2.0 * PI / 16.0))
        return Offset((cx + cos(angle) * vaultR).toFloat(), (cy + sin(angle) * vaultR).toFloat())
    }

    // 4 Flux Bridges
    AetherCrownEngine.BRIDGE_OUTER_TO_VAULT.forEach { (outerNode, vIdx) ->
        val open = AetherCrownEngine.isBridgeOpen(outerNode, state.bridgesPhaseEven)
        val p1 = outerPos(outerNode)
        val p2 = vaultPos(vIdx)
        if (open) {
            drawLine(Color(0xFF10B981).copy(alpha = 0.28f), p1, p2, strokeWidth = 12f * glowPulse)
        }
        drawLine(
            color = if (open) Color(0xFF34D399) else Color(0xFF475569).copy(alpha = 0.5f),
            start = p1,
            end = p2,
            strokeWidth = if (open) 5f * glowPulse else 2f,
            pathEffect = if (open) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }

    // Center Crown Core
    drawCircle(
        color = Color(0xFFF59E0B).copy(alpha = 0.22f * glowPulse),
        radius = minSide * 0.11f,
        center = Offset(cx, cy)
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706)), center = Offset(cx, cy), radius = minSide * 0.08f),
        radius = minSide * 0.075f,
        center = Offset(cx, cy)
    )

    // 16 Outer Orbit Nodes
    for (n in 0..15) {
        val pos = outerPos(n)
        val isSwitch = n in AetherCrownEngine.SWITCH_NODES
        val isBridge = n in AetherCrownEngine.BRIDGE_OUTER_TO_VAULT
        val nodeColor = when {
            n == AetherPlayer.SOLAR.homeOuterNode -> Color(AetherPlayer.SOLAR.colorHex)
            n == AetherPlayer.LUNAR.homeOuterNode -> Color(AetherPlayer.LUNAR.colorHex)
            isSwitch -> Color(0xFF38BDF8)
            isBridge -> Color(0xFF34D399)
            else -> Color(0xFF1E293B)
        }
        if (isSwitch) {
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.25f * glowPulse), radius = minSide * 0.044f, center = pos)
        }
        drawCircle(nodeColor, radius = minSide * 0.030f, center = pos)
        drawCircle(Color.White.copy(alpha = 0.65f), radius = minSide * 0.030f, center = pos, style = Stroke(1.6f))
    }

    // 4 Shielded Sanctuary Vaults
    for (v in 0..3) {
        val pos = vaultPos(v)
        drawCircle(Color(0xFF0F766E).copy(alpha = 0.65f), radius = minSide * 0.044f, center = pos)
        drawCircle(Color(0xFFFDE68A), radius = minSide * 0.044f, center = pos, style = Stroke(2.5f))
    }

    // Draw Tokens with specular bevels
    listOf(AetherPlayer.SOLAR, AetherPlayer.LUNAR).forEach { player ->
        val pColor = Color(player.colorHex)
        val tokens = state.tokens[player].orEmpty()
        tokens.forEach { tk ->
            val pt = when (tk.zone) {
                AetherZone.DOCKED -> {
                    val baseX = if (player == AetherPlayer.SOLAR) minSide * 0.10f else size.width - minSide * 0.10f
                    val baseY = minSide * (0.14f + tk.id * 0.07f)
                    Offset(baseX, baseY)
                }
                AetherZone.OUTER -> outerPos(tk.nodeIndex)
                AetherZone.VAULT -> vaultPos(tk.nodeIndex)
                AetherZone.CROWNED -> {
                    val shift = if (player == AetherPlayer.SOLAR) -minSide * 0.03f else minSide * 0.03f
                    Offset(cx + shift, cy + (tk.id - 1.5f) * (minSide * 0.025f))
                }
            }
            drawCircle(Color.Black.copy(alpha = 0.45f), radius = minSide * 0.026f, center = pt.copy(y = pt.y + 2f))
            drawCircle(Color.White, radius = minSide * 0.025f, center = pt)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.55f), pColor),
                    center = Offset(pt.x - 3f, pt.y - 3f),
                    radius = minSide * 0.024f
                ),
                radius = minSide * 0.020f,
                center = pt
            )
        }
    }
}

