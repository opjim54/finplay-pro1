package com.example.domain.games

import kotlin.math.abs
import kotlin.random.Random

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val value: Int) {
    PAWN(100),
    KNIGHT(320),
    BISHOP(330),
    ROOK(500),
    QUEEN(900),
    KING(20000)
}

data class ChessPiece(
    val color: PieceColor,
    val type: PieceType
) {
    fun symbol(): String = when (color) {
        PieceColor.WHITE -> when (type) {
            PieceType.KING -> "♔"
            PieceType.QUEEN -> "♕"
            PieceType.ROOK -> "♖"
            PieceType.BISHOP -> "♗"
            PieceType.KNIGHT -> "♘"
            PieceType.PAWN -> "♙"
        }
        PieceColor.BLACK -> when (type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
    }
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
)

data class ChessMove(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isCastleKingSide: Boolean = false,
    val isCastleQueenSide: Boolean = false,
    val isEnPassant: Boolean = false
)

enum class ChessOutcome {
    ONGOING,
    WHITE_WINS,
    BLACK_WINS,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL
}

data class ChessGameState(
    /** 64 squares: index = row * 8 + col, row 0 = rank 8 (Black back rank), row 7 = rank 1 (White back rank). */
    val board: List<ChessPiece?> = ChessEngine.initialBoard(),
    val turn: PieceColor = PieceColor.WHITE,
    val castling: CastlingRights = CastlingRights(),
    val enPassantTarget: Int? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val capturedWhite: List<ChessPiece> = emptyList(),
    val capturedBlack: List<ChessPiece> = emptyList(),
    val lastMove: ChessMove? = null,
    val moveNotations: List<String> = emptyList(),
    val outcome: ChessOutcome = ChessOutcome.ONGOING,
    val inCheck: Boolean = false,
    val history: List<ChessSnapshot> = emptyList()
)

data class ChessSnapshot(
    val board: List<ChessPiece?>,
    val turn: PieceColor,
    val castling: CastlingRights,
    val enPassantTarget: Int?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
    val capturedWhite: List<ChessPiece>,
    val capturedBlack: List<ChessPiece>,
    val lastMove: ChessMove?
)

object ChessEngine {
    fun initialBoard(): List<ChessPiece?> {
        val arr = arrayOfNulls<ChessPiece>(64)
        val backRank = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        for (c in 0..7) {
            arr[c] = ChessPiece(PieceColor.BLACK, backRank[c])
            arr[8 + c] = ChessPiece(PieceColor.BLACK, PieceType.PAWN)
            arr[48 + c] = ChessPiece(PieceColor.WHITE, PieceType.PAWN)
            arr[56 + c] = ChessPiece(PieceColor.WHITE, backRank[c])
        }
        return arr.toList()
    }

    fun newGame(): ChessGameState = ChessGameState()

    fun squareName(idx: Int): String {
        val col = idx % 8
        val row = idx / 8
        return "${('a' + col)}${8 - row}"
    }

    fun legalMovesForSquare(state: ChessGameState, fromSquare: Int): List<ChessMove> {
        val piece = state.board.getOrNull(fromSquare) ?: return emptyList()
        if (piece.color != state.turn || state.outcome != ChessOutcome.ONGOING) return emptyList()
        return legalMoves(state).filter { it.from == fromSquare }
    }

    fun legalMoves(state: ChessGameState): List<ChessMove> {
        if (state.outcome != ChessOutcome.ONGOING) return emptyList()
        val pseudo = generatePseudoLegalMoves(state, state.turn)
        return pseudo.filter { mv ->
            val nextBoard = applyBoardTransform(state.board, mv)
            !isKingInCheck(nextBoard, state.turn)
        }
    }

    fun applyMove(state: ChessGameState, move: ChessMove): ChessGameState {
        val movingPiece = state.board[move.from] ?: return state
        val capturedTarget = when {
            move.isEnPassant -> {
                val capSq = if (movingPiece.color == PieceColor.WHITE) move.to + 8 else move.to - 8
                state.board[capSq]
            }
            else -> state.board[move.to]
        }

        val snap = ChessSnapshot(
            board = state.board,
            turn = state.turn,
            castling = state.castling,
            enPassantTarget = state.enPassantTarget,
            halfMoveClock = state.halfMoveClock,
            fullMoveNumber = state.fullMoveNumber,
            capturedWhite = state.capturedWhite,
            capturedBlack = state.capturedBlack,
            lastMove = state.lastMove
        )

        val nextBoard = applyBoardTransform(state.board, move)
        val nextCastling = updateCastlingRights(state.castling, move.from, move.to, movingPiece)
        val nextEp = if (movingPiece.type == PieceType.PAWN && abs(move.from / 8 - move.to / 8) == 2) {
            (move.from + move.to) / 2
        } else null

        val nextHalfMove = if (movingPiece.type == PieceType.PAWN || capturedTarget != null) 0 else state.halfMoveClock + 1
        val nextFullMove = if (state.turn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber
        val nextTurn = state.turn.opposite()

        val nextCapWhite = if (capturedTarget?.color == PieceColor.WHITE) state.capturedWhite + capturedTarget else state.capturedWhite
        val nextCapBlack = if (capturedTarget?.color == PieceColor.BLACK) state.capturedBlack + capturedTarget else state.capturedBlack

        val notation = formatNotation(movingPiece, move, capturedTarget != null)
        val baseNext = state.copy(
            board = nextBoard,
            turn = nextTurn,
            castling = nextCastling,
            enPassantTarget = nextEp,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            capturedWhite = nextCapWhite,
            capturedBlack = nextCapBlack,
            lastMove = move,
            moveNotations = state.moveNotations + notation,
            history = state.history + snap
        )

        val oppInCheck = isKingInCheck(nextBoard, nextTurn)
        val oppMoves = generatePseudoLegalMoves(baseNext, nextTurn).filter { mv ->
            !isKingInCheck(applyBoardTransform(nextBoard, mv), nextTurn)
        }

        val outcome = when {
            oppMoves.isEmpty() && oppInCheck -> if (state.turn == PieceColor.WHITE) ChessOutcome.WHITE_WINS else ChessOutcome.BLACK_WINS
            oppMoves.isEmpty() -> ChessOutcome.STALEMATE
            nextHalfMove >= 100 -> ChessOutcome.DRAW_FIFTY_MOVE
            isInsufficientMaterial(nextBoard) -> ChessOutcome.DRAW_INSUFFICIENT_MATERIAL
            else -> ChessOutcome.ONGOING
        }

        return baseNext.copy(inCheck = oppInCheck, outcome = outcome)
    }

    fun undoLastMove(state: ChessGameState): ChessGameState {
        val snap = state.history.lastOrNull() ?: return state
        return state.copy(
            board = snap.board,
            turn = snap.turn,
            castling = snap.castling,
            enPassantTarget = snap.enPassantTarget,
            halfMoveClock = snap.halfMoveClock,
            fullMoveNumber = snap.fullMoveNumber,
            capturedWhite = snap.capturedWhite,
            capturedBlack = snap.capturedBlack,
            lastMove = snap.lastMove,
            moveNotations = state.moveNotations.dropLast(1),
            outcome = ChessOutcome.ONGOING,
            inCheck = isKingInCheck(snap.board, snap.turn),
            history = state.history.dropLast(1)
        )
    }

    fun isKingInCheck(board: List<ChessPiece?>, color: PieceColor): Boolean {
        val kingSq = board.indexOfFirst { it?.color == color && it.type == PieceType.KING }
        if (kingSq < 0) return false
        return isSquareAttacked(board, kingSq, byColor = color.opposite())
    }

    private fun isSquareAttacked(board: List<ChessPiece?>, targetSq: Int, byColor: PieceColor): Boolean {
        val tr = targetSq / 8
        val tc = targetSq % 8

        // Pawns
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        for (dc in intArrayOf(-1, 1)) {
            val pr = tr + pawnDir
            val pc = tc + dc
            if (pr in 0..7 && pc in 0..7) {
                val p = board[pr * 8 + pc]
                if (p?.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // Knights
        val knightSteps = arrayOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((dr, dc) in knightSteps) {
            val nr = tr + dr
            val nc = tc + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr * 8 + nc]
                if (p?.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val kr = tr + dr
                val kc = tc + dc
                if (kr in 0..7 && kc in 0..7) {
                    val p = board[kr * 8 + kc]
                    if (p?.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        // Orthogonal sliders (Rook / Queen)
        val ortho = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in ortho) {
            var r = tr + dr
            var c = tc + dc
            while (r in 0..7 && c in 0..7) {
                val p = board[r * 8 + c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                r += dr
                c += dc
            }
        }

        // Diagonal sliders (Bishop / Queen)
        val diag = arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diag) {
            var r = tr + dr
            var c = tc + dc
            while (r in 0..7 && c in 0..7) {
                val p = board[r * 8 + c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                r += dr
                c += dc
            }
        }
        return false
    }

    private fun generatePseudoLegalMoves(state: ChessGameState, color: PieceColor): List<ChessMove> {
        val board = state.board
        val out = ArrayList<ChessMove>(48)
        for (sq in 0..63) {
            val piece = board[sq] ?: continue
            if (piece.color != color) continue
            val r = sq / 8
            val c = sq % 8
            when (piece.type) {
                PieceType.PAWN -> {
                    val dir = if (color == PieceColor.WHITE) -1 else 1
                    val startRow = if (color == PieceColor.WHITE) 6 else 1
                    val promoRow = if (color == PieceColor.WHITE) 0 else 7
                    val oneStep = (r + dir) * 8 + c
                    if (r + dir in 0..7 && board[oneStep] == null) {
                        if (r + dir == promoRow) {
                            listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                                out.add(ChessMove(sq, oneStep, promotion = it))
                            }
                        } else {
                            out.add(ChessMove(sq, oneStep))
                            val twoStep = (r + 2 * dir) * 8 + c
                            if (r == startRow && board[twoStep] == null) {
                                out.add(ChessMove(sq, twoStep))
                            }
                        }
                    }
                    for (dc in intArrayOf(-1, 1)) {
                        val nc = c + dc
                        val nr = r + dir
                        if (nr in 0..7 && nc in 0..7) {
                            val target = nr * 8 + nc
                            val occ = board[target]
                            if (occ != null && occ.color != color) {
                                if (nr == promoRow) {
                                    listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                                        out.add(ChessMove(sq, target, promotion = it))
                                    }
                                } else {
                                    out.add(ChessMove(sq, target))
                                }
                            } else if (state.enPassantTarget == target) {
                                out.add(ChessMove(sq, target, isEnPassant = true))
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    val steps = arrayOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
                    for ((dr, dc) in steps) {
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0..7 && nc in 0..7) {
                            val t = nr * 8 + nc
                            if (board[t]?.color != color) out.add(ChessMove(sq, t))
                        }
                    }
                }

                PieceType.BISHOP -> addSlidingMoves(board, sq, color, arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), out)
                PieceType.ROOK -> addSlidingMoves(board, sq, color, arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), out)
                PieceType.QUEEN -> addSlidingMoves(
                    board, sq, color,
                    arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1),
                    out
                )

                PieceType.KING -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0..7 && nc in 0..7) {
                                val t = nr * 8 + nc
                                if (board[t]?.color != color) out.add(ChessMove(sq, t))
                            }
                        }
                    }
                    // Castling checks
                    if (color == PieceColor.WHITE && sq == 60 && !isSquareAttacked(board, 60, PieceColor.BLACK)) {
                        if (state.castling.whiteKingSide && board[61] == null && board[62] == null &&
                            board[63] == ChessPiece(PieceColor.WHITE, PieceType.ROOK) &&
                            !isSquareAttacked(board, 61, PieceColor.BLACK) &&
                            !isSquareAttacked(board, 62, PieceColor.BLACK)
                        ) {
                            out.add(ChessMove(60, 62, isCastleKingSide = true))
                        }
                        if (state.castling.whiteQueenSide && board[59] == null && board[58] == null && board[57] == null &&
                            board[56] == ChessPiece(PieceColor.WHITE, PieceType.ROOK) &&
                            !isSquareAttacked(board, 59, PieceColor.BLACK) &&
                            !isSquareAttacked(board, 58, PieceColor.BLACK)
                        ) {
                            out.add(ChessMove(60, 58, isCastleQueenSide = true))
                        }
                    } else if (color == PieceColor.BLACK && sq == 4 && !isSquareAttacked(board, 4, PieceColor.WHITE)) {
                        if (state.castling.blackKingSide && board[5] == null && board[6] == null &&
                            board[7] == ChessPiece(PieceColor.BLACK, PieceType.ROOK) &&
                            !isSquareAttacked(board, 5, PieceColor.WHITE) &&
                            !isSquareAttacked(board, 6, PieceColor.WHITE)
                        ) {
                            out.add(ChessMove(4, 6, isCastleKingSide = true))
                        }
                        if (state.castling.blackQueenSide && board[3] == null && board[2] == null && board[1] == null &&
                            board[0] == ChessPiece(PieceColor.BLACK, PieceType.ROOK) &&
                            !isSquareAttacked(board, 3, PieceColor.WHITE) &&
                            !isSquareAttacked(board, 2, PieceColor.WHITE)
                        ) {
                            out.add(ChessMove(4, 2, isCastleQueenSide = true))
                        }
                    }
                }
            }
        }
        return out
    }

    private fun addSlidingMoves(
        board: List<ChessPiece?>,
        from: Int,
        color: PieceColor,
        dirs: Array<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        val sr = from / 8
        val sc = from % 8
        for ((dr, dc) in dirs) {
            var r = sr + dr
            var c = sc + dc
            while (r in 0..7 && c in 0..7) {
                val t = r * 8 + c
                val occ = board[t]
                if (occ == null) {
                    out.add(ChessMove(from, t))
                } else {
                    if (occ.color != color) out.add(ChessMove(from, t))
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    private fun applyBoardTransform(board: List<ChessPiece?>, mv: ChessMove): List<ChessPiece?> {
        val next = board.toMutableList()
        val piece = next[mv.from] ?: return next
        next[mv.from] = null
        next[mv.to] = if (mv.promotion != null) ChessPiece(piece.color, mv.promotion) else piece

        if (mv.isEnPassant) {
            val capSq = if (piece.color == PieceColor.WHITE) mv.to + 8 else mv.to - 8
            next[capSq] = null
        } else if (mv.isCastleKingSide) {
            if (piece.color == PieceColor.WHITE) {
                next[61] = next[63]
                next[63] = null
            } else {
                next[5] = next[7]
                next[7] = null
            }
        } else if (mv.isCastleQueenSide) {
            if (piece.color == PieceColor.WHITE) {
                next[59] = next[56]
                next[56] = null
            } else {
                next[3] = next[0]
                next[0] = null
            }
        }
        return next
    }

    private fun updateCastlingRights(c: CastlingRights, from: Int, to: Int, moving: ChessPiece): CastlingRights {
        var wk = c.whiteKingSide
        var wq = c.whiteQueenSide
        var bk = c.blackKingSide
        var bq = c.blackQueenSide
        if (moving.type == PieceType.KING) {
            if (moving.color == PieceColor.WHITE) { wk = false; wq = false } else { bk = false; bq = false }
        }
        if (from == 63 || to == 63) wk = false
        if (from == 56 || to == 56) wq = false
        if (from == 7 || to == 7) bk = false
        if (from == 0 || to == 0) bq = false
        return CastlingRights(wk, wq, bk, bq)
    }

    private fun isInsufficientMaterial(board: List<ChessPiece?>): Boolean {
        val nonKingEntries = board.mapIndexedNotNull { idx, piece ->
            if (piece != null && piece.type != PieceType.KING) idx to piece else null
        }
        if (nonKingEntries.isEmpty()) return true
        if (nonKingEntries.size == 1 && (nonKingEntries[0].second.type == PieceType.KNIGHT || nonKingEntries[0].second.type == PieceType.BISHOP)) {
            return true
        }
        if (nonKingEntries.size == 2 && nonKingEntries.all { it.second.type == PieceType.BISHOP }) {
            val colors = nonKingEntries.map { (idx, _) -> ((idx / 8) + (idx % 8)) % 2 }.distinct()
            if (colors.size == 1) return true
        }
        return false
    }

    private fun formatNotation(piece: ChessPiece, move: ChessMove, capture: Boolean): String {
        if (move.isCastleKingSide) return "O-O"
        if (move.isCastleQueenSide) return "O-O-O"
        val prefix = when (piece.type) {
            PieceType.PAWN -> if (capture) "${squareName(move.from)[0]}x" else ""
            PieceType.KNIGHT -> "N${if (capture) "x" else ""}"
            PieceType.BISHOP -> "B${if (capture) "x" else ""}"
            PieceType.ROOK -> "R${if (capture) "x" else ""}"
            PieceType.QUEEN -> "Q${if (capture) "x" else ""}"
            PieceType.KING -> "K${if (capture) "x" else ""}"
        }
        val promo = move.promotion?.let { "=${it.name.first()}" } ?: ""
        return "$prefix${squareName(move.to)}$promo"
    }
}

object ChessBotAi {
    private val centerBonus = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10,   0,   0,   0,   0,   0,   0, -10,
        -10,   0,  10,  15,  15,  10,   0, -10,
        -10,   5,  15,  25,  25,  15,   5, -10,
        -10,   5,  15,  25,  25,  15,   5, -10,
        -10,   0,  10,  15,  15,  10,   0, -10,
        -10,   0,   5,   0,   0,   5,   0, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )

    fun chooseMove(state: ChessGameState, accuracyPercent: Int, rng: Random = Random.Default): ChessMove? {
        val legal = ChessEngine.legalMoves(state)
        if (legal.isEmpty()) return null
        val acc = accuracyPercent.coerceIn(10, 100)

        // Low accuracy: frequent random or greedy blunder moves
        if (acc <= 25 && rng.nextInt(100) > acc + 10) {
            return legal.random(rng)
        }

        val depth = when {
            acc >= 80 -> 3
            acc >= 45 -> 2
            else -> 1
        }
        val maximizing = state.turn == PieceColor.WHITE
        val scored = legal.map { mv ->
            val next = ChessEngine.applyMove(state, mv)
            val eval = minimax(next, depth - 1, -1_000_000, 1_000_000, maximizingPlayer = !maximizing)
            mv to (if (maximizing) eval else -eval)
        }.sortedByDescending { it.second }

        return when {
            acc >= 90 -> scored.first().first
            acc >= 60 -> {
                val keepTop = scored.take(2)
                if (rng.nextInt(100) < acc) keepTop.first().first else keepTop.last().first
            }
            else -> {
                val keepPool = scored.take(4.coerceAtMost(scored.size))
                keepPool.random(rng).first
            }
        }
    }

    private fun minimax(
        state: ChessGameState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        maximizingPlayer: Boolean
    ): Int {
        if (state.outcome != ChessOutcome.ONGOING) {
            return when (state.outcome) {
                ChessOutcome.WHITE_WINS -> 100_000 + depth * 10
                ChessOutcome.BLACK_WINS -> -100_000 - depth * 10
                else -> 0
            }
        }
        if (depth <= 0) return evaluateBoard(state)

        // Order captures/promotions first for efficient alpha-beta pruning
        val moves = ChessEngine.legalMoves(state).sortedByDescending { mv ->
            (if (state.board[mv.to] != null) state.board[mv.to]!!.type.value else 0) + (mv.promotion?.value ?: 0)
        }
        if (moves.isEmpty()) return 0

        var alpha = alphaInit
        var beta = betaInit
        if (maximizingPlayer) {
            var maxEval = -1_000_000
            for (mv in moves) {
                val eval = minimax(ChessEngine.applyMove(state, mv), depth - 1, alpha, beta, false)
                if (eval > maxEval) maxEval = eval
                if (eval > alpha) alpha = eval
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = 1_000_000
            for (mv in moves) {
                val eval = minimax(ChessEngine.applyMove(state, mv), depth - 1, alpha, beta, true)
                if (eval < minEval) minEval = eval
                if (eval < beta) beta = eval
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(state: ChessGameState): Int {
        var score = 0
        for (sq in 0..63) {
            val piece = state.board[sq] ?: continue
            val pos = centerBonus[sq]
            val pawnPush = if (piece.type == PieceType.PAWN) {
                val row = sq / 8
                if (piece.color == PieceColor.WHITE) (6 - row) * 8 else (row - 1) * 8
            } else 0
            val signed = piece.type.value + (pos / 2) + pawnPush
            score += if (piece.color == PieceColor.WHITE) signed else -signed
        }
        if (state.inCheck) {
            score += if (state.turn == PieceColor.WHITE) -45 else 45
        }
        return score
    }
}
