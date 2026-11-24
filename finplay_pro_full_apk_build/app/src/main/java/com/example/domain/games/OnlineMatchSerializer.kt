package com.example.domain.games

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compact, deterministic state serialization for real-time Firestore online multiplayer
 * across all 5 FinPlay Pro board games.
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

    // ---- 3. AETHER CROWN ----
    @Serializable
    private data class AetherTokenWire(val id: Int, val zone: String, val nodeIndex: Int)

    @Serializable
    private data class AetherWire(
        val solarTokens: List<AetherTokenWire>,
        val lunarTokens: List<AetherTokenWire>,
        val turn: String,
        val phase: String,
        val d1: Int,
        val d2: Int,
        val remainingDice: List<Int>,
        val bridgesPhaseEven: Boolean,
        val roundCount: Int,
        val solarCharges: Int,
        val lunarCharges: Int,
        val winner: String? = null,
        val statusLog: String = ""
    )

    fun encodeAether(state: AetherCrownState): String {
        val wire = AetherWire(
            solarTokens = (state.tokens[AetherPlayer.SOLAR] ?: emptyList()).map {
                AetherTokenWire(it.id, it.zone.name, it.nodeIndex)
            },
            lunarTokens = (state.tokens[AetherPlayer.LUNAR] ?: emptyList()).map {
                AetherTokenWire(it.id, it.zone.name, it.nodeIndex)
            },
            turn = state.turn.name,
            phase = state.phase.name,
            d1 = state.dice.first,
            d2 = state.dice.second,
            remainingDice = state.remainingDice,
            bridgesPhaseEven = state.bridgesPhaseEven,
            roundCount = state.roundCount,
            solarCharges = state.charges[AetherPlayer.SOLAR] ?: 0,
            lunarCharges = state.charges[AetherPlayer.LUNAR] ?: 0,
            winner = state.winner?.name,
            statusLog = state.statusLog.take(180)
        )
        return json.encodeToString(wire)
    }

    fun decodeAether(payload: String): AetherCrownState? = runCatching {
        val wire = json.decodeFromString<AetherWire>(payload)
        val solar = wire.solarTokens.map {
            AetherToken(AetherPlayer.SOLAR, it.id, runCatching { AetherZone.valueOf(it.zone) }.getOrDefault(AetherZone.DOCKED), it.nodeIndex)
        }
        val lunar = wire.lunarTokens.map {
            AetherToken(AetherPlayer.LUNAR, it.id, runCatching { AetherZone.valueOf(it.zone) }.getOrDefault(AetherZone.DOCKED), it.nodeIndex)
        }
        AetherCrownState(
            tokens = mapOf(AetherPlayer.SOLAR to solar, AetherPlayer.LUNAR to lunar),
            turn = runCatching { AetherPlayer.valueOf(wire.turn) }.getOrDefault(AetherPlayer.SOLAR),
            playVsBot = false,
            phase = runCatching { AetherTurnPhase.valueOf(wire.phase) }.getOrDefault(AetherTurnPhase.ROLL_DICE),
            dice = wire.d1.coerceIn(1, 4) to wire.d2.coerceIn(1, 4),
            remainingDice = wire.remainingDice,
            bridgesPhaseEven = wire.bridgesPhaseEven,
            roundCount = wire.roundCount,
            charges = mapOf(AetherPlayer.SOLAR to wire.solarCharges, AetherPlayer.LUNAR to wire.lunarCharges),
            winner = wire.winner?.let { runCatching { AetherPlayer.valueOf(it) }.getOrNull() },
            statusLog = wire.statusLog
        )
    }.getOrNull()

    // ---- 4. REVERSI ----
    @Serializable
    private data class ReversiWire(
        val board: String, // 64 chars: '.', 'E', 'P'
        val turn: String,
        val outcome: String,
        val lastMove: Int? = null,
        val lastFlipped: List<Int> = emptyList(),
        val passedLastTurn: Boolean = false
    )

    fun encodeReversi(state: ReversiGameState): String {
        val sb = StringBuilder(64)
        state.board.forEach { d ->
            sb.append(
                when (d) {
                    ReversiDisc.EMERALD -> 'E'
                    ReversiDisc.PEARL -> 'P'
                    ReversiDisc.EMPTY -> '.'
                }
            )
        }
        val wire = ReversiWire(
            board = sb.toString(),
            turn = state.turn.name,
            outcome = state.outcome.name,
            lastMove = state.lastMoveIndex,
            lastFlipped = state.lastFlippedIndices.toList(),
            passedLastTurn = state.passedLastTurn
        )
        return json.encodeToString(wire)
    }

    fun decodeReversi(payload: String): ReversiGameState? = runCatching {
        val wire = json.decodeFromString<ReversiWire>(payload)
        if (wire.board.length != 64) return null
        val board = wire.board.map { ch ->
            when (ch) {
                'E' -> ReversiDisc.EMERALD
                'P' -> ReversiDisc.PEARL
                else -> ReversiDisc.EMPTY
            }
        }
        ReversiGameState(
            board = board,
            turn = runCatching { ReversiDisc.valueOf(wire.turn) }.getOrDefault(ReversiDisc.EMERALD),
            outcome = runCatching { ReversiOutcome.valueOf(wire.outcome) }.getOrDefault(ReversiOutcome.ONGOING),
            lastMoveIndex = wire.lastMove,
            lastFlippedIndices = wire.lastFlipped.toSet(),
            passedLastTurn = wire.passedLastTurn,
            emeraldScore = board.count { it == ReversiDisc.EMERALD },
            pearlScore = board.count { it == ReversiDisc.PEARL }
        )
    }.getOrNull()

    // ---- 5. CONNECT FOUR ----
    @Serializable
    private data class ConnectFourWire(
        val board: String, // 42 chars: '.', 'S', 'C'
        val turn: String,
        val outcome: String,
        val lastDrop: Int? = null,
        val winningCells: List<Int> = emptyList(),
        val moveCount: Int = 0
    )

    fun encodeConnectFour(state: ConnectFourState): String {
        val sb = StringBuilder(42)
        state.board.forEach { d ->
            sb.append(
                when (d) {
                    ConnectFourDisc.SOLAR -> 'S'
                    ConnectFourDisc.CRIMSON -> 'C'
                    ConnectFourDisc.EMPTY -> '.'
                }
            )
        }
        val wire = ConnectFourWire(
            board = sb.toString(),
            turn = state.turn.name,
            outcome = state.outcome.name,
            lastDrop = state.lastDropIndex,
            winningCells = state.winningCells.toList(),
            moveCount = state.moveCount
        )
        return json.encodeToString(wire)
    }

    fun decodeConnectFour(payload: String): ConnectFourState? = runCatching {
        val wire = json.decodeFromString<ConnectFourWire>(payload)
        if (wire.board.length != ConnectFourState.ROWS * ConnectFourState.COLS) return null
        val board = wire.board.map { ch ->
            when (ch) {
                'S' -> ConnectFourDisc.SOLAR
                'C' -> ConnectFourDisc.CRIMSON
                else -> ConnectFourDisc.EMPTY
            }
        }
        ConnectFourState(
            board = board,
            turn = runCatching { ConnectFourDisc.valueOf(wire.turn) }.getOrDefault(ConnectFourDisc.SOLAR),
            outcome = runCatching { ConnectFourOutcome.valueOf(wire.outcome) }.getOrDefault(ConnectFourOutcome.ONGOING),
            lastDropIndex = wire.lastDrop,
            winningCells = wire.winningCells.toSet(),
            moveCount = wire.moveCount
        )
    }.getOrNull()

    fun initialPayloadForGame(gameType: String): String = when (gameType) {
        "LUDO" -> encodeLudo(LudoEngine.newGame(playerCount = 2, playVsBot = false))
        "CHESS" -> encodeChess(ChessGameState())
        "AETHER_CROWN" -> encodeAether(AetherCrownEngine.newGame(playVsBot = false))
        "REVERSI" -> encodeReversi(ReversiEngine.newGame())
        "CONNECT_FOUR" -> encodeConnectFour(ConnectFourEngine.newGame())
        "CARROM" -> "{}"
        "SNAKES_LADDERS" -> "{}"
        "FRUIT_SLASH" -> "{}"
        else -> "{}"
    }
}
