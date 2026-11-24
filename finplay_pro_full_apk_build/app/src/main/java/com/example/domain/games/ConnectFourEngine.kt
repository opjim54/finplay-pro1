package com.example.domain.games

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class ConnectFourDisc {
    EMPTY,
    SOLAR, // Player 1 (Champagne Gold)
    CRIMSON // Player 2 / Bot (Royal Crimson)
}

fun ConnectFourDisc.opponent(): ConnectFourDisc = when (this) {
    ConnectFourDisc.SOLAR -> ConnectFourDisc.CRIMSON
    ConnectFourDisc.CRIMSON -> ConnectFourDisc.SOLAR
    ConnectFourDisc.EMPTY -> ConnectFourDisc.EMPTY
}

enum class ConnectFourOutcome {
    ONGOING,
    SOLAR_WINS,
    CRIMSON_WINS,
    DRAW
}

data class ConnectFourState(
    val board: List<ConnectFourDisc> = List(ROWS * COLS) { ConnectFourDisc.EMPTY },
    val turn: ConnectFourDisc = ConnectFourDisc.SOLAR,
    val outcome: ConnectFourOutcome = ConnectFourOutcome.ONGOING,
    val lastDropIndex: Int? = null,
    val winningCells: Set<Int> = emptySet(),
    val moveCount: Int = 0
) {
    companion object {
        const val ROWS = 6
        const val COLS = 7
    }
}

object ConnectFourEngine {
    fun newGame(): ConnectFourState = ConnectFourState()

    fun legalColumns(state: ConnectFourState): List<Int> {
        if (state.outcome != ConnectFourOutcome.ONGOING) return emptyList()
        return (0 until ConnectFourState.COLS).filter { col ->
            state.board[col] == ConnectFourDisc.EMPTY
        }
    }

    fun dropRowForColumn(board: List<ConnectFourDisc>, col: Int): Int? {
        if (col !in 0 until ConnectFourState.COLS) return null
        for (r in (ConnectFourState.ROWS - 1) downTo 0) {
            if (board[r * ConnectFourState.COLS + col] == ConnectFourDisc.EMPTY) return r
        }
        return null
    }

    fun dropDisc(state: ConnectFourState, col: Int): ConnectFourState {
        if (state.outcome != ConnectFourOutcome.ONGOING) return state
        val row = dropRowForColumn(state.board, col) ?: return state
        val dropIdx = row * ConnectFourState.COLS + col
        val nextBoard = state.board.toMutableList()
        nextBoard[dropIdx] = state.turn

        val winCells = findWinningLine(nextBoard, state.turn)
        val outcome = when {
            winCells.isNotEmpty() -> if (state.turn == ConnectFourDisc.SOLAR) ConnectFourOutcome.SOLAR_WINS else ConnectFourOutcome.CRIMSON_WINS
            nextBoard.none { it == ConnectFourDisc.EMPTY } -> ConnectFourOutcome.DRAW
            else -> ConnectFourOutcome.ONGOING
        }

        return state.copy(
            board = nextBoard,
            turn = if (outcome == ConnectFourOutcome.ONGOING) state.turn.opponent() else state.turn,
            outcome = outcome,
            lastDropIndex = dropIdx,
            winningCells = winCells,
            moveCount = state.moveCount + 1
        )
    }

    fun findWinningLine(board: List<ConnectFourDisc>, player: ConnectFourDisc): Set<Int> {
        val cols = ConnectFourState.COLS
        val rows = ConnectFourState.ROWS
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r * cols + c] != player) continue
                for ((dr, dc) in dirs) {
                    val cells = ArrayList<Int>(4)
                    for (k in 0..3) {
                        val nr = r + dr * k
                        val nc = c + dc * k
                        if (nr !in 0 until rows || nc !in 0 until cols) break
                        val idx = nr * cols + nc
                        if (board[idx] != player) break
                        cells.add(idx)
                    }
                    if (cells.size == 4) return cells.toSet()
                }
            }
        }
        return emptySet()
    }
}

object ConnectFourBotAi {
    private val CENTER_ORDER = listOf(3, 2, 4, 1, 5, 0, 6)

    fun chooseColumn(
        state: ConnectFourState,
        accuracyPercent: Int,
        rng: Random = Random.Default
    ): Int? {
        val legal = ConnectFourEngine.legalColumns(state)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        val acc = accuracyPercent.coerceIn(10, 100)
        val mistakeProb = ((100 - acc) / 100f) * 0.6f
        if (rng.nextFloat() < mistakeProb) {
            return legal[rng.nextInt(legal.size)]
        }

        val bot = state.turn
        val ordered = CENTER_ORDER.filter { it in legal }

        // Immediate winning drop check
        for (c in ordered) {
            val next = ConnectFourEngine.dropDisc(state, c)
            if (next.outcome == ConnectFourOutcome.CRIMSON_WINS || next.outcome == ConnectFourOutcome.SOLAR_WINS) {
                return c
            }
        }

        val depth = when {
            acc >= 85 -> 4
            acc >= 55 -> 3
            else -> 2
        }

        var bestCol = ordered.first()
        var bestScore = Int.MIN_VALUE
        for (col in ordered) {
            val child = ConnectFourEngine.dropDisc(state, col)
            val score = alphabeta(child, depth - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, bot)
            if (score > bestScore) {
                bestScore = score
                bestCol = col
            }
        }
        return bestCol
    }

    private fun alphabeta(
        state: ConnectFourState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        bot: ConnectFourDisc
    ): Int {
        if (state.outcome != ConnectFourOutcome.ONGOING) {
            return when (state.outcome) {
                ConnectFourOutcome.DRAW -> 0
                ConnectFourOutcome.SOLAR_WINS -> if (bot == ConnectFourDisc.SOLAR) 100_000 + depth * 100 else -100_000 - depth * 100
                ConnectFourOutcome.CRIMSON_WINS -> if (bot == ConnectFourDisc.CRIMSON) 100_000 + depth * 100 else -100_000 - depth * 100
                ConnectFourOutcome.ONGOING -> 0
            }
        }
        if (depth <= 0) return evaluateBoard(state.board, bot)

        val legal = CENTER_ORDER.filter { state.board[it] == ConnectFourDisc.EMPTY }
        val maximizing = state.turn == bot
        var alpha = alphaInit
        var beta = betaInit

        if (maximizing) {
            var value = Int.MIN_VALUE / 2
            for (c in legal) {
                value = max(value, alphabeta(ConnectFourEngine.dropDisc(state, c), depth - 1, alpha, beta, bot))
                alpha = max(alpha, value)
                if (alpha >= beta) break
            }
            return value
        } else {
            var value = Int.MAX_VALUE / 2
            for (c in legal) {
                value = min(value, alphabeta(ConnectFourEngine.dropDisc(state, c), depth - 1, alpha, beta, bot))
                beta = min(beta, value)
                if (alpha >= beta) break
            }
            return value
        }
    }

    private fun evaluateBoard(board: List<ConnectFourDisc>, bot: ConnectFourDisc): Int {
        val opp = bot.opponent()
        val rows = ConnectFourState.ROWS
        val cols = ConnectFourState.COLS
        var score = 0

        // Center column control bonus
        for (r in 0 until rows) {
            when (board[r * cols + 3]) {
                bot -> score += 6
                opp -> score -= 6
                else -> Unit
            }
        }

        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                for ((dr, dc) in dirs) {
                    val endR = r + dr * 3
                    val endC = c + dc * 3
                    if (endR !in 0 until rows || endC !in 0 until cols) continue
                    var botCount = 0
                    var oppCount = 0
                    for (k in 0..3) {
                        when (board[(r + dr * k) * cols + (c + dc * k)]) {
                            bot -> botCount++
                            opp -> oppCount++
                            else -> Unit
                        }
                    }
                    if (botCount > 0 && oppCount == 0) {
                        score += when (botCount) {
                            3 -> 55
                            2 -> 12
                            else -> 2
                        }
                    } else if (oppCount > 0 && botCount == 0) {
                        score -= when (oppCount) {
                            3 -> 68
                            2 -> 14
                            else -> 2
                        }
                    }
                }
            }
        }
        return score
    }
}
