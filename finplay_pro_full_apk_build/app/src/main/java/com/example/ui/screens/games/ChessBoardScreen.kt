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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameMatchResult
import com.example.domain.games.ChessBotAi
import com.example.domain.games.ChessEngine
import com.example.domain.games.ChessGameState
import com.example.domain.games.ChessMove
import com.example.domain.games.ChessOutcome
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.PieceColor
import com.example.domain.games.PieceType
import com.example.i18n.LanguageManager
import com.example.i18n.LocalStrings
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ChessBoardScreen(
    onRecordMatch: (outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _ -> }
) {
    val appStrings = LocalStrings.current
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    var playVsBot by remember { mutableStateOf(true) }
    var botAccuracy by remember { mutableIntStateOf(70) }
    var showRules by remember { mutableStateOf(false) }

    var state by remember { mutableStateOf(ChessEngine.newGame()) }
    var selectedSquare by remember { mutableStateOf<Int?>(null) }
    var pendingPromotionMoves by remember { mutableStateOf<List<ChessMove>>(emptyList()) }
    var botThinking by remember { mutableStateOf(false) }
    var resultRecorded by remember { mutableStateOf(false) }
    var lastEarnedScore by remember { mutableIntStateOf(0) }

    fun resetChess() {
        selectedSquare = null
        pendingPromotionMoves = emptyList()
        resultRecorded = false
        lastEarnedScore = 0
        state = ChessEngine.newGame()
    }

    LaunchedEffect(state.outcome) {
        if (state.outcome != ChessOutcome.ONGOING && !resultRecorded) {
            resultRecorded = true
            val whiteMaterial = state.board.filterNotNull()
                .filter { it.color == PieceColor.WHITE }
                .sumOf {
                    when (it.type) {
                        PieceType.PAWN -> 1
                        PieceType.KNIGHT, PieceType.BISHOP -> 3
                        PieceType.ROOK -> 5
                        PieceType.QUEEN -> 9
                        PieceType.KING -> 0
                    }
                }
            val result = when (state.outcome) {
                ChessOutcome.WHITE_WINS -> GameMatchResult.WIN
                ChessOutcome.BLACK_WINS -> GameMatchResult.LOSS
                else -> GameMatchResult.DRAW
            }
            val speedBonus = (200 - state.moveNotations.size * 2).coerceAtLeast(0)
            val score = (whiteMaterial * 25) + speedBonus + when (result) {
                GameMatchResult.WIN -> 600 + if (playVsBot) botAccuracy * 8 else 200
                GameMatchResult.DRAW -> 300
                GameMatchResult.LOSS -> 80
            }
            lastEarnedScore = score
            onRecordMatch(result, score, playVsBot, botAccuracy)
        }
    }

    fun playMoveWithSfx(cur: ChessGameState, move: ChessMove) {
        val captured = cur.board[move.to] != null ||
            (cur.board[move.from]?.type == PieceType.PAWN && move.to == cur.enPassantTarget)
        val next = ChessEngine.applyMove(cur, move)
        state = next
        selectedSquare = null
        when {
            next.outcome != ChessOutcome.ONGOING -> GameSoundEngine.play(GameSoundEffect.WIN)
            next.inCheck -> GameSoundEngine.play(GameSoundEffect.CHECK)
            captured -> GameSoundEngine.play(GameSoundEffect.CAPTURE)
            else -> GameSoundEngine.play(GameSoundEffect.MOVE)
        }
    }

    if (showRules) {
        GameRulesModalSheet(gameId = ArenaGameId.CHESS, onDismiss = { showRules = false })
    }

    if (pendingPromotionMoves.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingPromotionMoves = emptyList() },
            title = { Text(gStrings.promotePawnTitle, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        PieceType.QUEEN to "♕",
                        PieceType.ROOK to "♖",
                        PieceType.BISHOP to "♗",
                        PieceType.KNIGHT to "♘"
                    ).forEach { (type, glyph) ->
                        val move = pendingPromotionMoves.firstOrNull { it.promotion == type }
                        if (move != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        playMoveWithSfx(state, move)
                                        pendingPromotionMoves = emptyList()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(glyph, fontSize = 30.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingPromotionMoves = emptyList() }) {
                    Text(appStrings.cancel)
                }
            }
        )
    }

    val isBotTurn = playVsBot && state.outcome == ChessOutcome.ONGOING && state.turn == PieceColor.BLACK
    LaunchedEffect(state.turn, state.outcome, playVsBot, botAccuracy) {
        if (!isBotTurn) return@LaunchedEffect
        botThinking = true
        delay(240L)
        val bestMove = withContext(Dispatchers.Default) {
            ChessBotAi.chooseMove(state, botAccuracy)
        }
        if (bestMove != null) {
            playMoveWithSfx(state, bestMove)
        }
        botThinking = false
    }

    val legalFromSelected = remember(state, selectedSquare) {
        selectedSquare?.let { ChessEngine.legalMovesForSquare(state, it) }.orEmpty()
    }
    val legalTargets = remember(legalFromSelected) { legalFromSelected.map { it.to }.toSet() }

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
                resetChess()
            },
            onAccuracyChange = { botAccuracy = it },
            onOpenRules = { showRules = true },
            onRestart = { resetChess() }
        )

        // Status Header + Undo
        val statusText = when (state.outcome) {
            ChessOutcome.WHITE_WINS -> gStrings.checkmateWhiteWins
            ChessOutcome.BLACK_WINS -> gStrings.checkmateBlackWins
            ChessOutcome.STALEMATE -> gStrings.drawStalemate
            ChessOutcome.DRAW_FIFTY_MOVE -> gStrings.drawFiftyMove
            ChessOutcome.DRAW_INSUFFICIENT_MATERIAL -> gStrings.drawInsufficient
            ChessOutcome.ONGOING -> {
                val side = if (state.turn == PieceColor.WHITE) gStrings.whiteTurn
                else if (playVsBot) "${gStrings.blackTurn} ${gStrings.botThinking}"
                else gStrings.blackTurn
                if (state.inCheck) "$side • ${gStrings.checkTag}" else side
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (state.inCheck) Color(0xFFDC2626).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, if (state.inCheck) Color(0xFFDC2626) else financeColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (state.inCheck) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                    )
                    val capturedRow = (state.capturedBlack.map { it.symbol() } + state.capturedWhite.map { it.symbol() }).joinToString("")
                    if (capturedRow.isNotEmpty()) {
                        Text("${gStrings.capturedLabel} $capturedRow", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(
                    enabled = state.history.isNotEmpty() && !botThinking,
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.TAP)
                        selectedSquare = null
                        var next = ChessEngine.undoLastMove(state)
                        if (playVsBot && next.turn == PieceColor.BLACK && next.history.isNotEmpty()) {
                            next = ChessEngine.undoLastMove(next)
                        }
                        state = next
                    },
                    modifier = Modifier.testTag("chess_undo_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = gStrings.undoMove)
                }
            }
        }

        // Luxury 8x8 Chess Board with Rank/File labels & contrasted piece tokens
        val lightSquare = Color(0xFFECFDF5)
        val darkSquare = Color(0xFF065F46)
        val selectedTint = Color(0xFFF59E0B).copy(alpha = 0.50f)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF022C22),
            border = BorderStroke(2.5.dp, financeColors.champagneGold.copy(alpha = 0.72f)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("chess_board")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                for (row in 0..7) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0..7) {
                            val sq = row * 8 + col
                            val isLight = (row + col) % 2 == 0
                            val piece = state.board[sq]
                            val isSelected = selectedSquare == sq
                            val isLegalDest = sq in legalTargets
                            val isLastMove = state.lastMove?.from == sq || state.lastMove?.to == sq
                            val isCheckedKing = state.inCheck && piece?.type == PieceType.KING && piece.color == state.turn

                            val bg = when {
                                isCheckedKing -> Color(0xFFDC2626).copy(alpha = 0.72f)
                                isSelected -> selectedTint
                                isLastMove -> if (isLight) Color(0xFFFEF08A) else Color(0xFF0D9488)
                                isLight -> lightSquare
                                else -> darkSquare
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(bg)
                                    .clickable(enabled = !isBotTurn && state.outcome == ChessOutcome.ONGOING) {
                                        if (isLegalDest) {
                                            val matching = legalFromSelected.filter { it.to == sq }
                                            if (matching.size > 1 && matching.any { it.promotion != null }) {
                                                pendingPromotionMoves = matching
                                            } else if (matching.isNotEmpty()) {
                                                playMoveWithSfx(state, matching.first())
                                            }
                                        } else if (piece != null && piece.color == state.turn) {
                                            GameSoundEngine.play(GameSoundEffect.TAP)
                                            selectedSquare = if (selectedSquare == sq) null else sq
                                        } else {
                                            selectedSquare = null
                                        }
                                    }
                                    .testTag("chess_sq_$sq"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Algebraic rank & file mini annotations
                                if (col == 0) {
                                    Text(
                                        text = "${8 - row}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLight) Color(0xFF065F46).copy(alpha = 0.6f) else Color(0xFFD1FAE5).copy(alpha = 0.65f),
                                        modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 1.dp)
                                    )
                                }
                                if (row == 7) {
                                    Text(
                                        text = ('a' + col).toString(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLight) Color(0xFF065F46).copy(alpha = 0.6f) else Color(0xFFD1FAE5).copy(alpha = 0.65f),
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp)
                                    )
                                }

                                if (piece != null) {
                                    val pieceScale by animateFloatAsState(
                                        targetValue = when {
                                            isSelected -> 1.15f
                                            state.lastMove?.to == sq -> 1.07f
                                            else -> 1f
                                        },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "chess_piece_spring_$sq"
                                    )
                                    // Crisp contrasted disc backing for White vs Black pieces so glyphs shine on emerald tiles
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.80f)
                                            .graphicsLayer {
                                                scaleX = pieceScale
                                                scaleY = pieceScale
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                if (piece.color == PieceColor.WHITE) Color(0xFFFFFFFF).copy(alpha = 0.85f)
                                                else Color(0xFF0F172A).copy(alpha = 0.84f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = piece.symbol(),
                                            fontSize = 23.sp,
                                            color = if (piece.color == PieceColor.WHITE) Color(0xFF0F172A) else Color(0xFFFDE68A)
                                        )
                                    }
                                }
                                if (isLegalDest) {
                                    if (piece != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.88f)
                                                .border(2.5.dp, Color(0xFFDC2626), CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD97706).copy(alpha = 0.88f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.moveNotations.isNotEmpty()) {
            val paired = state.moveNotations.chunked(2).mapIndexed { idx, pair ->
                "${idx + 1}. ${pair.joinToString(" ")}"
            }.takeLast(6).joinToString("   ")
            Text(
                text = "${gStrings.movesLabel} $paired",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GameMatchResultBanner(
            visible = state.outcome != ChessOutcome.ONGOING,
            headline = statusText,
            scoreEarned = lastEarnedScore,
            onPlayAgain = { resetChess() }
        )
        Spacer(Modifier.height(24.dp))
    }
}

