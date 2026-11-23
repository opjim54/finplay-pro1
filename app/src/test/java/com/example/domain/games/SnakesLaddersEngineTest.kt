package com.example.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakesLaddersEngineTest {

    @Test
    fun newGame_initializesPlayersAtPositionZero() {
        val state = SnakesLaddersEngine.newGame(playerCount = 4, playVsBot = true)
        assertEquals(4, state.players.size)
        assertTrue(state.players.all { it.position == 0 })
        assertEquals(0, state.currentTurnIndex)
        assertEquals(SnakesPhase.WAIT_ROLL, state.phase)
    }

    @Test
    fun ladder_climbsFromBottomToTop() {
        var state = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false)
        // Force roll of 4 (ladder at 4 leads to 14)
        val res = SnakesLaddersEngine.rollAndMove(state, forcedDice = 4)
        assertTrue(res.wasLadder)
        assertEquals(14, res.finalPos)
        assertEquals(14, res.nextState.players[0].position)
    }

    @Test
    fun snake_slidesDownFromHeadToTail() {
        var state = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false)
        // Position player at 13, then roll 4 to land on 17 (snake at 17 leads to 7)
        val p1 = state.players[0].copy(position = 13)
        state = state.copy(players = listOf(p1, state.players[1]))

        val res = SnakesLaddersEngine.rollAndMove(state, forcedDice = 4)
        assertTrue(res.wasSnake)
        assertEquals(7, res.finalPos)
        assertEquals(7, res.nextState.players[0].position)
    }

    @Test
    fun winAt100_triggersGameOver() {
        var state = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false)
        val p1 = state.players[0].copy(position = 96)
        state = state.copy(players = listOf(p1, state.players[1]))

        val res = SnakesLaddersEngine.rollAndMove(state, forcedDice = 4)
        assertEquals(100, res.finalPos)
        assertEquals(SnakesPhase.GAME_OVER, res.nextState.phase)
        assertNotNull(res.nextState.winner)
    }

    @Test
    fun cellToRowCol_serpentineGridMath() {
        // Cell 1: Bottom-Left (row 0, col 0)
        assertEquals(0 to 0, SnakesLaddersEngine.cellToRowCol(1))
        // Cell 10: Bottom-Right (row 0, col 9)
        assertEquals(0 to 9, SnakesLaddersEngine.cellToRowCol(10))
        // Cell 11: Row 1, reversed (row 1, col 9)
        assertEquals(1 to 9, SnakesLaddersEngine.cellToRowCol(11))
        // Cell 20: Row 1, left side (row 1, col 0)
        assertEquals(1 to 0, SnakesLaddersEngine.cellToRowCol(20))
        // Cell 100: Top Row 9, left side (row 9, col 0)
        assertEquals(9 to 0, SnakesLaddersEngine.cellToRowCol(100))
    }
}
