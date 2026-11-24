package com.example.domain.games

import kotlin.random.Random

data class Snake(val head: Int, val tail: Int)
data class Ladder(val bottom: Int, val top: Int)

data class SnakesPlayer(
    val id: Int,
    val name: String,
    val color: Long, // 0xFFE11D48 (Red), 0xFF10B981 (Green), 0xFFF59E0B (Yellow), 0xFF0284C7 (Blue)
    val position: Int = 0, // 0 = start yard, 1..100 on board
    val isBot: Boolean = false,
    val avatarUrl: String = ""
)

enum class SnakesPhase {
    WAIT_ROLL,
    MOVING,
    GAME_OVER
}

data class SnakesLaddersState(
    val players: List<SnakesPlayer>,
    val currentTurnIndex: Int = 0,
    val phase: SnakesPhase = SnakesPhase.WAIT_ROLL,
    val lastDiceValue: Int = 1,
    val winner: SnakesPlayer? = null,
    val statusLog: String = "Player 1's Turn • Roll Dice",
    val recentActionBanner: String? = null,
    val isVsBot: Boolean = true,
    val botAccuracy: Int = 75,
    val turnSequence: Int = 0,
    val consecutiveSixes: Int = 0
) {
    val currentPlayer: SnakesPlayer get() = players[currentTurnIndex.coerceIn(0, players.lastIndex)]
}

object SnakesLaddersEngine {
    val LADDERS = listOf(
        Ladder(4, 14),
        Ladder(9, 31),
        Ladder(20, 38),
        Ladder(28, 84),
        Ladder(40, 59),
        Ladder(51, 67),
        Ladder(63, 81),
        Ladder(71, 91)
    )

    val SNAKES = listOf(
        Snake(17, 7),
        Snake(54, 34),
        Snake(62, 19),
        Snake(64, 60),
        Snake(87, 24),
        Snake(93, 73),
        Snake(95, 75),
        Snake(99, 78)
    )

    val DEFAULT_COLORS = listOf(
        0xFFE11D48, // Red (Player 1)
        0xFF10B981, // Green (Player 2)
        0xFFF59E0B, // Yellow (Player 3)
        0xFF0284C7  // Blue (Player 4)
    )

    fun newGame(playerCount: Int = 2, playVsBot: Boolean = true, botAccuracy: Int = 75): SnakesLaddersState {
        val count = playerCount.coerceIn(2, 4)
        val players = (0 until count).map { idx ->
            SnakesPlayer(
                id = idx,
                name = if (idx == 0) "Player 1" else if (playVsBot) "Smart Bot ${idx}" else "Player ${idx + 1}",
                color = DEFAULT_COLORS[idx % DEFAULT_COLORS.size],
                position = 0,
                isBot = playVsBot && idx > 0
            )
        }
        return SnakesLaddersState(
            players = players,
            currentTurnIndex = 0,
            phase = SnakesPhase.WAIT_ROLL,
            lastDiceValue = 1,
            winner = null,
            statusLog = "${players[0].name}'s Turn • Roll Dice",
            isVsBot = playVsBot,
            botAccuracy = botAccuracy,
            turnSequence = 0,
            consecutiveSixes = 0
        )
    }

    data class MoveResult(
        val nextState: SnakesLaddersState,
        val wasLadder: Boolean,
        val wasSnake: Boolean,
        val intermediatePos: Int,
        val finalPos: Int
    )

    fun rollAndMove(state: SnakesLaddersState, forcedDice: Int? = null): MoveResult {
        if (state.phase == SnakesPhase.GAME_OVER) {
            return MoveResult(state, wasLadder = false, wasSnake = false, intermediatePos = state.currentPlayer.position, finalPos = state.currentPlayer.position)
        }

        val dice = (forcedDice ?: Random.nextInt(1, 7)).coerceIn(1, 6)
        val curPlayer = state.currentPlayer
        val oldPos = curPlayer.position
        val nextSixes = if (dice == 6) state.consecutiveSixes + 1 else 0

        // Standard rule: 3 consecutive 6s forfeits the 3rd roll and passes turn
        if (nextSixes >= 3) {
            val nextIndex = (state.currentTurnIndex + 1) % state.players.size
            val nextPlayer = state.players[nextIndex]
            val passState = state.copy(
                currentTurnIndex = nextIndex,
                phase = SnakesPhase.WAIT_ROLL,
                lastDiceValue = dice,
                statusLog = "${curPlayer.name} rolled three 6s! Turn passes to ${nextPlayer.name}",
                recentActionBanner = "⚠️ Triple 6! Turn Passed",
                turnSequence = state.turnSequence + 1,
                consecutiveSixes = 0
            )
            return MoveResult(passState, wasLadder = false, wasSnake = false, intermediatePos = oldPos, finalPos = oldPos)
        }

        // Calculate intermediate landing position
        val rawTarget = if (oldPos == 0) dice else oldPos + dice
        val overshot = rawTarget > 100
        val intermediate = if (overshot) oldPos else rawTarget

        // Check for Ladder or Snake (only when token actually moved)
        val ladder = if (!overshot) LADDERS.firstOrNull { it.bottom == intermediate } else null
        val snake = if (!overshot) SNAKES.firstOrNull { it.head == intermediate } else null

        var finalPos = intermediate
        var wasLadder = false
        var wasSnake = false
        var actionBanner: String? = null

        if (ladder != null) {
            finalPos = ladder.top
            wasLadder = true
            actionBanner = "🪜 Climbed Ladder! ${ladder.bottom} ➔ ${ladder.top}"
        } else if (snake != null) {
            finalPos = snake.tail
            wasSnake = true
            actionBanner = "🐍 Bitten by Snake! ${snake.head} ➔ ${snake.tail}"
        } else if (overshot) {
            actionBanner = "🎯 Need exact ${100 - oldPos} to win!"
        }

        val updatedPlayer = curPlayer.copy(position = finalPos)
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentTurnIndex] = updatedPlayer

        // Check win condition
        if (finalPos == 100) {
            val winState = state.copy(
                players = updatedPlayers,
                lastDiceValue = dice,
                phase = SnakesPhase.GAME_OVER,
                winner = updatedPlayer,
                statusLog = "🏆 ${updatedPlayer.name} Reached 100 & Won the Game!",
                recentActionBanner = actionBanner,
                turnSequence = state.turnSequence + 1,
                consecutiveSixes = 0
            )
            return MoveResult(winState, wasLadder, wasSnake, intermediate, finalPos)
        }

        // Rolling a 6 grants bonus roll (even after a ladder or normal move, unless overshot 100)
        val getsBonusRoll = dice == 6 && !overshot
        val nextIndex = if (getsBonusRoll) state.currentTurnIndex else (state.currentTurnIndex + 1) % state.players.size
        val nextPlayer = updatedPlayers[nextIndex]

        val status = when {
            overshot -> "${curPlayer.name} rolled $dice (needs exact ${100 - oldPos}) • ${nextPlayer.name}'s Turn"
            getsBonusRoll -> "${curPlayer.name} rolled a 6! • Bonus Roll!"
            wasLadder -> "${curPlayer.name} climbed ladder to $finalPos! • ${nextPlayer.name}'s Turn"
            wasSnake -> "${curPlayer.name} slid down snake to $finalPos! • ${nextPlayer.name}'s Turn"
            else -> "${curPlayer.name} moved to $finalPos • ${nextPlayer.name}'s Turn"
        }

        val nextState = state.copy(
            players = updatedPlayers,
            currentTurnIndex = nextIndex,
            phase = SnakesPhase.WAIT_ROLL,
            lastDiceValue = dice,
            statusLog = status,
            recentActionBanner = actionBanner,
            turnSequence = state.turnSequence + 1,
            consecutiveSixes = if (getsBonusRoll) nextSixes else 0
        )

        return MoveResult(nextState, wasLadder, wasSnake, intermediate, finalPos)
    }

    /**
     * Converts board cell number (1 to 100) into 2D row and column on a 10x10 serpentine grid.
     * Row 0 = Bottom (1 to 10 from left to right)
     * Row 1 = (11 to 20 from right to left)
     * ...
     * Row 9 = Top (91 to 100)
     */
    fun cellToRowCol(cell: Int): Pair<Int, Int> {
        if (cell <= 0) return 0 to 0
        val clamped = cell.coerceIn(1, 100)
        val index = clamped - 1
        val row = index / 10 // 0 = bottom, 9 = top
        val colInRow = index % 10
        val col = if (row % 2 == 0) colInRow else (9 - colInRow) // serpentine alternation
        return row to col
    }
}
