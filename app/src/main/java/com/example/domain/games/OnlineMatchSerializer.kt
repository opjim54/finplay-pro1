package com.example.domain.games

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compact, deterministic state serialization for real-time Firestore online multiplayer
 * across FinPlay Pro arena games.
 */
object OnlineMatchSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---- 1. LUDO ----
    @Serializable
    private data class LudoWire(
        val activeColors: List<String>,
        val tokens: Map<String, List<Int>>, // color -> 4 progress ints
        val turnIndex: Int,
        val phase: String,
        val diceValue: Int,
        val consecutiveSixes: Int,
        val winner: String? = null,
        val statusLog: String = ""
    )

    fun encodeLudo(state: LudoGameState): String {
        val wire = LudoWire(
            activeColors = state.activeColors.map { it.name },
            tokens = state.tokens.mapKeys { it.key.name }.mapValues { (_, list) ->
                list.sortedBy { it.index }.map { it.progress }
            },
            turnIndex = state.turnIndex,
            phase = state.phase.name,
            diceValue = state.diceValue,
            consecutiveSixes = state.consecutiveSixes,
            winner = state.winner?.name,
            statusLog = state.statusLog.take(180)
        )
        return json.encodeToString(wire)
    }

    fun decodeLudo(payload: String): LudoGameState? = runCatching {
        val wire = json.decodeFromString<LudoWire>(payload)
        val colors = wire.activeColors.mapNotNull { runCatching { LudoColor.valueOf(it) }.getOrNull() }
            .ifEmpty { listOf(LudoColor.RED, LudoColor.YELLOW) }
        val tokenMap = colors.associateWith { color ->
            val progresses = wire.tokens[color.name] ?: listOf(-1, -1, -1, -1)
            (0..3).map { idx -> LudoToken(color = color, index = idx, progress = progresses.getOrElse(idx) { -1 }) }
        }
        LudoGameState(
            activeColors = colors,
            botColors = emptySet(),
            tokens = tokenMap,
            turnIndex = wire.turnIndex.coerceIn(0, colors.lastIndex),
            phase = runCatching { LudoPhase.valueOf(wire.phase) }.getOrDefault(LudoPhase.WAIT_ROLL),
            diceValue = wire.diceValue.coerceIn(1, 6),
            consecutiveSixes = wire.consecutiveSixes.coerceIn(0, 3),
            winner = wire.winner?.let { runCatching { LudoColor.valueOf(it) }.getOrNull() },
            statusLog = wire.statusLog
        )
    }.getOrNull()

    // ---- 2. CHESS ----
    @Serializable
    private data class ChessWire(
        val boardCodes: List<String>, // 64 strings: "" or "WP", "BK", etc.
        val turn: String,
        val wK: Boolean,
        val wQ: Boolean,
        val bK: Boolean,
        val bQ: Boolean,
        val ep: Int? = null,
        val halfMove: Int = 0,
        val fullMove: Int = 1,
        val lastFrom: Int? = null,
        val lastTo: Int? = null,
        val outcome: String = ChessOutcome.ONGOING.name,
        val inCheck: Boolean = false,
        val capturedW: List<String> = emptyList(),
        val capturedB: List<String> = emptyList()
    )

    private fun encodePiece(piece: ChessPiece?): String {
        if (piece == null) return ""
        val c = if (piece.color == PieceColor.WHITE) "W" else "B"
        val t = when (piece.type) {
            PieceType.PAWN -> "P"
            PieceType.KNIGHT -> "N"
            PieceType.BISHOP -> "B"
            PieceType.ROOK -> "R"
            PieceType.QUEEN -> "Q"
            PieceType.KING -> "K"
        }
        return "$c$t"
    }

    private fun decodePiece(code: String): ChessPiece? {
        if (code.length != 2) return null
        val color = when (code[0]) {
            'W' -> PieceColor.WHITE
            'B' -> PieceColor.BLACK
            else -> return null
        }
        val type = when (code[1]) {
            'P' -> PieceType.PAWN
            'N' -> PieceType.KNIGHT
            'B' -> PieceType.BISHOP
            'R' -> PieceType.ROOK
            'Q' -> PieceType.QUEEN
            'K' -> PieceType.KING
            else -> return null
        }
        return ChessPiece(color, type)
    }

    fun encodeChess(state: ChessGameState): String {
        val wire = ChessWire(
            boardCodes = state.board.map(::encodePiece),
            turn = state.turn.name,
            wK = state.castling.whiteKingSide,
            wQ = state.castling.whiteQueenSide,
            bK = state.castling.blackKingSide,
            bQ = state.castling.blackQueenSide,
            ep = state.enPassantTarget,
            halfMove = state.halfMoveClock,
            fullMove = state.fullMoveNumber,
            lastFrom = state.lastMove?.from,
            lastTo = state.lastMove?.to,
            outcome = state.outcome.name,
            inCheck = state.inCheck,
            capturedW = state.capturedWhite.map(::encodePiece),
            capturedB = state.capturedBlack.map(::encodePiece)
        )
        return json.encodeToString(wire)
    }

    fun decodeChess(payload: String): ChessGameState? = runCatching {
        val wire = json.decodeFromString<ChessWire>(payload)
        if (wire.boardCodes.size != 64) return null
        ChessGameState(
            board = wire.boardCodes.map(::decodePiece),
            turn = runCatching { PieceColor.valueOf(wire.turn) }.getOrDefault(PieceColor.WHITE),
            castling = CastlingRights(wire.wK, wire.wQ, wire.bK, wire.bQ),
            enPassantTarget = wire.ep,
            halfMoveClock = wire.halfMove,
            fullMoveNumber = wire.fullMove,
            capturedWhite = wire.capturedW.mapNotNull(::decodePiece),
            capturedBlack = wire.capturedB.mapNotNull(::decodePiece),
            lastMove = if (wire.lastFrom != null && wire.lastTo != null) ChessMove(wire.lastFrom, wire.lastTo) else null,
            outcome = runCatching { ChessOutcome.valueOf(wire.outcome) }.getOrDefault(ChessOutcome.ONGOING),
            inCheck = wire.inCheck
        )
    }.getOrNull()

    // ---- 3. SHOLO GUTI (16 BEADS) ----
    @Serializable
    private data class SholoGutiWire(
        val board: String, // 37 chars: '.', 'R', 'G'
        val turn: String,
        val chainedNodeId: Int? = null,
        val lastFrom: Int? = null,
        val lastTo: Int? = null,
        val lastCap: Int? = null,
        val rubyCaptured: Int = 0,
        val goldCaptured: Int = 0,
        val movesWithoutCapture: Int = 0,
        val turnSequence: Int = 0,
        val outcome: String = SholoGutiOutcome.ONGOING.name,
        val statusLog: String = ""
    )

    fun encodeSholoGuti(state: SholoGutiState): String {
        val sb = StringBuilder(SholoGutiEngine.NODE_COUNT)
        state.board.forEach { p ->
            sb.append(
                when (p) {
                    SholoGutiPlayer.RUBY -> 'R'
                    SholoGutiPlayer.GOLD -> 'G'
                    null -> '.'
                }
            )
        }
        val wire = SholoGutiWire(
            board = sb.toString(),
            turn = state.turn.name,
            chainedNodeId = state.chainedNodeId,
            lastFrom = state.lastMove?.from,
            lastTo = state.lastMove?.to,
            lastCap = state.lastMove?.capturedNode,
            rubyCaptured = state.rubyCaptured,
            goldCaptured = state.goldCaptured,
            movesWithoutCapture = state.movesWithoutCapture,
            turnSequence = state.turnSequence,
            outcome = state.outcome.name,
            statusLog = state.statusLog.take(180)
        )
        return json.encodeToString(wire)
    }

    fun decodeSholoGuti(payload: String): SholoGutiState? = runCatching {
        val wire = json.decodeFromString<SholoGutiWire>(payload)
        if (wire.board.length != SholoGutiEngine.NODE_COUNT) return null
        val board = wire.board.map { ch ->
            when (ch) {
                'R' -> SholoGutiPlayer.RUBY
                'G' -> SholoGutiPlayer.GOLD
                else -> null
            }
        }
        SholoGutiState(
            board = board,
            turn = runCatching { SholoGutiPlayer.valueOf(wire.turn) }.getOrDefault(SholoGutiPlayer.RUBY),
            chainedNodeId = wire.chainedNodeId,
            lastMove = if (wire.lastFrom != null && wire.lastTo != null) {
                SholoGutiMove(wire.lastFrom, wire.lastTo, wire.lastCap)
            } else null,
            rubyCaptured = wire.rubyCaptured,
            goldCaptured = wire.goldCaptured,
            movesWithoutCapture = wire.movesWithoutCapture,
            turnSequence = wire.turnSequence,
            outcome = runCatching { SholoGutiOutcome.valueOf(wire.outcome) }.getOrDefault(SholoGutiOutcome.ONGOING),
            statusLog = wire.statusLog
        )
    }.getOrNull()

    // ---- 7. SNAKES & LADDERS ----
    @Serializable
    private data class SnakesWire(
        val p1Pos: Int = 0,
        val p2Pos: Int = 0,
        val turnIndex: Int = 0,
        val lastDice: Int = 1,
        val winnerIdx: Int? = null,
        val statusLog: String = "",
        val turnSequence: Int = 0,
        val consecutiveSixes: Int = 0
    )

    fun encodeSnakes(state: SnakesLaddersState): String {
        val p1 = state.players.getOrNull(0)?.position ?: 0
        val p2 = state.players.getOrNull(1)?.position ?: 0
        val wire = SnakesWire(
            p1Pos = p1,
            p2Pos = p2,
            turnIndex = state.currentTurnIndex,
            lastDice = state.lastDiceValue,
            winnerIdx = state.winner?.id,
            statusLog = state.statusLog.take(180),
            turnSequence = state.turnSequence,
            consecutiveSixes = state.consecutiveSixes
        )
        return json.encodeToString(wire)
    }

    fun decodeSnakes(payload: String, hostName: String = "Player 1", guestName: String = "Player 2"): SnakesLaddersState? = runCatching {
        val wire = json.decodeFromString<SnakesWire>(payload)
        val p1 = SnakesPlayer(0, hostName.ifBlank { "Player 1" }, SnakesLaddersEngine.DEFAULT_COLORS[0], wire.p1Pos, isBot = false)
        val p2 = SnakesPlayer(1, guestName.ifBlank { "Player 2" }, SnakesLaddersEngine.DEFAULT_COLORS[1], wire.p2Pos, isBot = false)
        val list = listOf(p1, p2)
        val winner = wire.winnerIdx?.let { list.getOrNull(it) }
        SnakesLaddersState(
            players = list,
            currentTurnIndex = wire.turnIndex.coerceIn(0, 1),
            phase = if (winner != null) SnakesPhase.GAME_OVER else SnakesPhase.WAIT_ROLL,
            lastDiceValue = wire.lastDice.coerceIn(1, 6),
            winner = winner,
            statusLog = wire.statusLog,
            isVsBot = false,
            turnSequence = wire.turnSequence,
            consecutiveSixes = wire.consecutiveSixes
        )
    }.getOrNull()

    fun initialPayloadForGame(gameType: String): String = when (gameType) {
        "LUDO" -> encodeLudo(LudoEngine.newGame(playerCount = 2, playVsBot = false))
        "CHESS" -> encodeChess(ChessGameState())
        "SHOLO_GUTI" -> encodeSholoGuti(SholoGutiEngine.newGame())
        "SNAKES_LADDERS" -> encodeSnakes(SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false))
        "CARROM" -> "{}"
        "FRUIT_SLASH" -> "{}"
        "WAR_LAND" -> "{}"
        else -> "{}"
    }
}
