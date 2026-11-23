package com.example.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarromEngineTest {

    @Test
    fun newGame_hasCorrectPieceCountAndQueenAtCenter() {
        val state = CarromEngine.newGame(isVsBot = true, botAccuracy = 85)
        assertEquals(20, state.pieces.size) // 9 white, 9 black, 1 queen, 1 striker
        val queen = state.pieces.firstOrNull { it.type == CarromPieceType.QUEEN }
        assertNotNull(queen)
        assertEquals(0f, queen!!.x, 0.01f)
        assertEquals(0f, queen.y, 0.01f)

        val whites = state.pieces.filter { it.type == CarromPieceType.WHITE }
        val blacks = state.pieces.filter { it.type == CarromPieceType.BLACK }
        assertEquals(9, whites.size)
        assertEquals(9, blacks.size)

        val striker = state.pieces.firstOrNull { it.type == CarromPieceType.STRIKER }
        assertNotNull(striker)
        assertEquals(0.65f, striker!!.y, 0.05f)
    }

    @Test
    fun shootStriker_impartsVelocityAndTransitionsToMoving() {
        val initial = CarromEngine.newGame(isVsBot = false)
        val shot = CarromEngine.shootStriker(initial, impulseX = 0f, impulseY = -1.2f)
        assertEquals(CarromPhase.MOVING, shot.phase)
        val striker = shot.pieces.first { it.type == CarromPieceType.STRIKER }
        assertTrue(striker.vy < -0.5f)
    }

    @Test
    fun physicsStep_deceleratesMovingPieces() {
        var state = CarromEngine.newGame(isVsBot = false)
        state = CarromEngine.shootStriker(state, impulseX = 0.5f, impulseY = -0.8f)
        val strikerInitialVy = state.pieces.first { it.type == CarromPieceType.STRIKER }.vy
        state = CarromEngine.updatePhysics(state, dt = 0.016f)
        val strikerNewVy = state.pieces.first { it.type == CarromPieceType.STRIKER }.vy
        assertTrue(Math.abs(strikerNewVy) < Math.abs(strikerInitialVy))
    }

    @Test
    fun cornerPocket_detectsAndPocketsPiece() {
        var state = CarromEngine.newGame(isVsBot = false)
        // Position a white coin right on the brink of top-left pocket (-0.88f, -0.88f)
        val modifiedPieces = state.pieces.map {
            if (it.type == CarromPieceType.WHITE && it.id == 1) {
                it.copy(x = -0.87f, y = -0.87f, vx = -0.05f, vy = -0.05f)
            } else it
        }
        state = state.copy(pieces = modifiedPieces, phase = CarromPhase.MOVING)
        // Run physics steps until pocket sink registers
        repeat(10) {
            state = CarromEngine.updatePhysics(state, dt = 0.016f)
        }
        val target = state.pieces.first { it.id == 1 }
        assertTrue("Piece near pocket should be sinking or pocketed", target.isPocketed || target.animScale < 1.0f)
    }

    @Test
    fun botAi_calculatesLegalShot() {
        val state = CarromEngine.newGame(isVsBot = true, botAccuracy = 90)
        val (baselineX, impulse) = CarromBotAi.computeBotShot(state, accuracyPercent = 90)
        assertTrue(baselineX in -0.55f..0.55f)
        assertTrue(Math.hypot(impulse.x.toDouble(), impulse.y.toDouble()) > 0.1)
    }
}
