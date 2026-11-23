package com.example.domain.games

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class ReversiDisc {
    EMPTY,
    EMERALD, // Player 1 (Dark Teal / Obsidian)
    PEARL    // Player 2 / Bot (Champagne Pearl)
}

fun ReversiDisc.opponent(): ReversiDisc = when (this) {
    ReversiDisc.EMERALD -> ReversiDisc.PEARL
    ReversiDisc.PEARL -> ReversiDisc.EMERALD
    ReversiDisc.EMPTY -> ReversiDisc.EMPTY
}

enum class ReversiOutcome {
    ONGOING,
    EMERALD_WINS,
    PEARL_WINS,
    DRAW
}

data class ReversiMove(
    val index: Int,
    val flippedIndices: List<Int>
)

data class ReversiGameState(
    val board: List<ReversiDisc>,
    val turn: ReversiDisc = ReversiDisc.EMERALD,
    val outcome: ReversiOutcome = ReversiOutcome.ONGOING,
    val lastMoveIndex: Int? = null,
    val lastFlippedIndices: Set<Int> = emptySet(),
    val passedLastTurn: Boolean = false,
    val emeraldScore: Int = 2,
    val pearlScore: Int = 2
)

object ReversiEngine {
    private val DIRS = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1,            0 to 1,
        1 to -1,  1 to 0,  1 to 1
    )

    fun newGame(): ReversiGameState {
        val board = MutableList(64) { ReversiDisc.EMPTY }
        board[3 * 8 + 3] = ReversiDisc.PEARL
        board[3 * 8 + 4] = ReversiDisc.EMERALD
        board[4 * 8 + 3] = ReversiDisc.EMERALD
        board[4 * 8 + 4] = ReversiDisc.PEARL
        return ReversiGameState(board = board)
    }

    fun legalMoves(state: ReversiGameState, player: ReversiDisc = state.turn): List<ReversiMove> {
        if (state.outcome != ReversiOutcome.ONGOING || player == ReversiDisc.EMPTY) return emptyList()
        val moves = ArrayList<ReversiMove>(20)
        val opp = player.opponent()
        for (sq in 0 until 64) {
            if (state.board[sq] != ReversiDisc.EMPTY) continue
            val r = sq / 8
            val c = sq % 8
            val allFlips = ArrayList<Int>(8)
            for ((dr, dc) in DIRS) {
                var cr = r + dr
                var cc = c + dc
                val ray = ArrayList<Int>(6)
                while (cr in 0..7 && cc in 0..7 && state.board[cr * 8 + cc] == opp) {
                    ray.add(cr * 8 + cc)
                    cr += dr
                    cc += dc
                }
                if (ray.isNotEmpty() && cr in 0..7 && cc in 0..7 && state.board[cr * 8 + cc] == player) {
                    allFlips.addAll(ray)
                }
            }
            if (allFlips.isNotEmpty()) {
                moves.add(ReversiMove(sq, allFlips))
            }
        }
        return moves
    }

    fun applyMove(state: ReversiGameState, index: Int): ReversiGameState {
        if (state.outcome != ReversiOutcome.ONGOING) return state
        val move = legalMoves(state, state.turn).firstOrNull { it.index == index } ?: return state
        val nextBoard = state.board.toMutableList()
        nextBoard[move.index] = state.turn
        move.flippedIndices.forEach { nextBoard[it] = state.turn }

        val em = nextBoard.count { it == ReversiDisc.EMERALD }
        val pr = nextBoard.count { it == ReversiDisc.PEARL }
        val nextTurn = state.turn.opponent()

        val candidateNext = state.copy(
            board = nextBoard,
            turn = nextTurn,
            lastMoveIndex = move.index,
            lastFlippedIndices = move.flippedIndices.toSet(),
            passedLastTurn = false,
            emeraldScore = em,
            pearlScore = pr
        )

        val nextLegal = legalMoves(candidateNext, nextTurn)
        if (nextLegal.isNotEmpty()) return candidateNext

        // Opponent has no moves -> check if current player still has moves
        val backToCurrent = candidateNext.copy(turn = state.turn, passedLastTurn = true)
        val backLegal = legalMoves(backToCurrent, state.turn)
        if (backLegal.isNotEmpty()) return backToCurrent

        // Neither side can move -> Game Over
        val finalOutcome = when {
            em > pr -> ReversiOutcome.EMERALD_WINS
            pr > em -> ReversiOutcome.PEARL_WINS
            else -> ReversiOutcome.DRAW
        }
        return candidateNext.copy(outcome = finalOutcome, passedLastTurn = false)
    }
}

object ReversiBotAi {
    private val POSITIONAL_WEIGHTS = intArrayOf(
        120, -25,  20,  10,  10,  20, -25, 120,
        -25, -45,  -5,  -5,  -5,  -5, -45, -25,
         20,  -5,  15,   4,   4,  15,  -5,  20,
         10,  -5,   4,   2,   2,   4,  -5,  10,
         10,  -5,   4,   2,   2,   4,  -5,  10,
         20,  -5,  15,   4,   4,  15,  -5,  20,
        -25, -45,  -5,  -5,  -5,  -5, -45, -25,
        120, -25,  20,  10,  10,  20, -25, 120
    )

    fun chooseMove(
        state: ReversiGameState,
        accuracyPercent: Int,
        rng: Random = Random.Default
    ): ReversiMove? {
        val legal = ReversiEngine.legalMoves(state)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        val acc = accuracyPercent.coerceIn(10, 100)
        // Random slip chance for lower accuracies
        val mistakeProb = ((100 - acc) / 100f) * 0.65f
        if (rng.nextFloat() < mistakeProb) {
            return legal[rng.nextInt(legal.size)]
        }

        val depth = when {
            acc >= 85 -> 3
            acc >= 55 -> 2
            else -> 1
        }
        val botColor = state.turn
        var bestScore = Int.MIN_VALUE
        var bestMove = legal.first()
        for (move in legal) {
            val next = ReversiEngine.applyMove(state, move.index)
            val score = minimax(next, depth - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, botColor)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(
        state: ReversiGameState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        botColor: ReversiDisc
    ): Int {
        if (state.outcome != ReversiOutcome.ONGOING) {
            val diff = (if (botColor == ReversiDisc.PEARL) state.pearlScore - state.emeraldScore else state.emeraldScore - state.pearlScore)
            return when {
                diff > 0 -> 10_000 + diff
                diff < 0 -> -10_000 + diff
                else -> 0
            }
        }
        if (depth <= 0) return evaluate(state, botColor)

        val moves = ReversiEngine.legalMoves(state)
        if (moves.isEmpty()) return evaluate(state, botColor)

        val maximizing = state.turn == botColor
        var alpha = alphaInit
        var beta = betaInit

        if (maximizing) {
            var value = Int.MIN_VALUE / 2
            for (m in moves) {
                val child = ReversiEngine.applyMove(state, m.index)
                value = max(value, minimax(child, depth - 1, alpha, beta, botColor))
                alpha = max(alpha, value)
                if (alpha >= beta) break
            }
            return value
        } else {
            var value = Int.MAX_VALUE / 2
            for (m in moves) {
                val child = ReversiEngine.applyMove(state, m.index)
                value = min(value, minimax(child, depth - 1, alpha, beta, botColor))
                beta = min(beta, value)
                if (alpha >= beta) break
            }
            return value
        }
    }

    private fun evaluate(state: ReversiGameState, botColor: ReversiDisc): Int {
        val opp = botColor.opponent()
        var score = 0
        var emptyCount = 0
        for (sq in 0 until 64) {
            when (state.board[sq]) {
                botColor -> score += POSITIONAL_WEIGHTS[sq]
                opp -> score -= POSITIONAL_WEIGHTS[sq]
                else -> emptyCount++
            }
        }
        val myMob = ReversiEngine.legalMoves(state, botColor).size
        val oppMob = ReversiEngine.legalMoves(state, opp).size
        score += (myMob - oppMob) * 14
        // Endgame disc-count parity weight
        if (emptyCount < 14) {
            val discDiff = if (botColor == ReversiDisc.PEARL) state.pearlScore - state.emeraldScore else state.emeraldScore - state.pearlScore
            score += discDiff * 24
        }
        return score
    }
}
