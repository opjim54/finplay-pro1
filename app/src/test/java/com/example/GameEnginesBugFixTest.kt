package com.example

import com.example.data.games.PlayWalletRepository
import com.example.domain.games.CarromEngine
import com.example.domain.games.CarromGameState
import com.example.domain.games.CarromPhase
import com.example.domain.games.CarromPiece
import com.example.domain.games.CarromPieceType
import com.example.domain.games.CarromTurn
import com.example.domain.games.QueenStatus
import com.example.domain.games.SnakesLaddersEngine
import com.example.domain.games.SnakesPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class GameEnginesBugFixTest {

    @Test
    fun snakesLadders_bonusSixIncrementsTurnSequenceForSamePlayer() {
        val start = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = true)
        val res = SnakesLaddersEngine.rollAndMove(start, forcedDice = 6)

        assertEquals(0, res.nextState.currentTurnIndex)
        assertEquals(1, res.nextState.turnSequence)
        assertEquals(1, res.nextState.consecutiveSixes)
        assertEquals(6, res.nextState.players[0].position)
    }

    @Test
    fun snakesLadders_threeConsecutiveSixesForfeitsTurn() {
        var state = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false)
        state = SnakesLaddersEngine.rollAndMove(state, forcedDice = 6).nextState
        assertEquals(6, state.players[0].position)
        state = SnakesLaddersEngine.rollAndMove(state, forcedDice = 6).nextState
        assertEquals(12, state.players[0].position)
        val third = SnakesLaddersEngine.rollAndMove(state, forcedDice = 6)

        assertEquals("Third 6 must not move the token", 12, third.nextState.players[0].position)
        assertEquals("Third 6 must pass turn to Player 2", 1, third.nextState.currentTurnIndex)
        assertEquals(0, third.nextState.consecutiveSixes)
        assertEquals(3, third.nextState.turnSequence)
    }

    @Test
    fun snakesLadders_overshootDoesNotGrantBonusSixOrTriggerSnake() {
        val base = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false)
        val nearEnd = base.copy(
            players = base.players.mapIndexed { i, p -> if (i == 0) p.copy(position = 98) else p }
        )
        val res = SnakesLaddersEngine.rollAndMove(nearEnd, forcedDice = 6)

        assertEquals(98, res.nextState.players[0].position)
        assertFalse(res.wasSnake)
        assertFalse(res.wasLadder)
        assertEquals("Overshooting with 6 must pass turn", 1, res.nextState.currentTurnIndex)
        assertNull(res.nextState.winner)
    }

    @Test
    fun snakesLadders_exactLandingOn100WinsGame() {
        val base = SnakesLaddersEngine.newGame(playerCount = 2, playVsBot = false)
        val nearEnd = base.copy(
            players = base.players.mapIndexed { i, p -> if (i == 0) p.copy(position = 97) else p }
        )
        val res = SnakesLaddersEngine.rollAndMove(nearEnd, forcedDice = 3)

        assertEquals(100, res.finalPos)
        assertEquals(SnakesPhase.GAME_OVER, res.nextState.phase)
        assertEquals(0, res.nextState.winner?.id)
    }

    private fun settleMotion(initial: CarromGameState): CarromGameState {
        var cur = initial
        var ticks = 0
        while (cur.phase == CarromPhase.MOVING && ticks < 240) {
            cur = CarromEngine.updatePhysics(cur, 0.016f)
            ticks++
        }
        return cur
    }

    @Test
    fun carrom_queenTwoShotCoverRuleGrantsExtraTurnAndCoversOnNextShot() {
        val pieces = listOf(
            // Queen sits right inside top-left pocket radius so it sinks on shot 1
            CarromPiece(id = 0, type = CarromPieceType.QUEEN, x = -0.82f, y = -0.82f),
            CarromPiece(id = 1, type = CarromPieceType.WHITE, x = 0.0f, y = 0.0f),
            CarromPiece(id = 2, type = CarromPieceType.WHITE, x = 0.25f, y = 0.0f),
            CarromPiece(id = 3, type = CarromPieceType.BLACK, x = -0.25f, y = 0.0f),
            CarromPiece(id = 4, type = CarromPieceType.STRIKER, x = 0f, y = 0.65f)
        )
        val start = CarromGameState(pieces = pieces, turn = CarromTurn.PLAYER_1, phase = CarromPhase.AIMING)

        // Shot 1: Gentle non-foul shot while Queen sinks into pocket
        val shot1 = settleMotion(CarromEngine.shootStriker(start, 0.05f, 0f))
        assertEquals(CarromPhase.AIMING, shot1.phase)
        assertEquals("Player 1 must get immediate extra turn to cover Queen", CarromTurn.PLAYER_1, shot1.turn)
        assertEquals(QueenStatus.POCKETED_PENDING_COVER, shot1.queenStatus)
        assertEquals(CarromTurn.PLAYER_1, shot1.queenPendingCoverBy)

        // Shot 2 (success): Sink a White coin to cover Queen
        val setupCover = shot1.copy(
            pieces = shot1.pieces.map { p ->
                if (p.id == 1) p.copy(x = 0.82f, y = -0.82f) else p
            }
        )
        val shot2 = settleMotion(CarromEngine.shootStriker(setupCover, 0.05f, 0f))
        assertEquals(QueenStatus.COVERED_BY_P1, shot2.queenStatus)
        assertEquals("Covering Queen should award +10 coin + 25 Queen = 35", 35, shot2.player1Score)
        assertEquals(CarromTurn.PLAYER_1, shot2.turn)
    }

    @Test
    fun carrom_queenReturnsToCenterWhenUncoveredOnNextShot() {
        val pieces = listOf(
            CarromPiece(id = 0, type = CarromPieceType.QUEEN, x = -0.82f, y = -0.82f),
            CarromPiece(id = 1, type = CarromPieceType.WHITE, x = 0.0f, y = 0.0f),
            CarromPiece(id = 2, type = CarromPieceType.BLACK, x = -0.25f, y = 0.0f),
            CarromPiece(id = 3, type = CarromPieceType.STRIKER, x = 0f, y = 0.65f)
        )
        val afterQueen = settleMotion(
            CarromEngine.shootStriker(
                CarromGameState(pieces = pieces, turn = CarromTurn.PLAYER_1, phase = CarromPhase.AIMING),
                0.05f,
                0f
            )
        )
        assertEquals(QueenStatus.POCKETED_PENDING_COVER, afterQueen.queenStatus)

        // Shot 2: Miss without pocketing own coin
        val afterMiss = settleMotion(CarromEngine.shootStriker(afterQueen, 0.05f, 0f))
        assertEquals(QueenStatus.ON_BOARD, afterMiss.queenStatus)
        val queen = afterMiss.pieces.first { it.type == CarromPieceType.QUEEN }
        assertFalse(queen.isPocketed)
        assertEquals(0f, queen.x, 0.01f)
        assertEquals(0f, queen.y, 0.01f)
        assertEquals(CarromTurn.PLAYER_2, afterMiss.turn)
    }

    @Test
    fun carrom_pocketingOpponentCoinCreditsOpponentAndPassesTurn() {
        val pieces = listOf(
            CarromPiece(id = 0, type = CarromPieceType.WHITE, x = 0.0f, y = 0.0f),
            CarromPiece(id = 1, type = CarromPieceType.BLACK, x = 0.82f, y = 0.82f),
            CarromPiece(id = 2, type = CarromPieceType.BLACK, x = -0.3f, y = 0.0f),
            CarromPiece(id = 3, type = CarromPieceType.STRIKER, x = 0f, y = 0.65f)
        )
        val start = CarromGameState(pieces = pieces, turn = CarromTurn.PLAYER_1, phase = CarromPhase.AIMING)
        val settled = settleMotion(CarromEngine.shootStriker(start, 0.05f, 0f))

        assertEquals(0, settled.player1Score)
        assertEquals(10, settled.player2Score)
        assertEquals(1, settled.player2PocketedCount)
        assertEquals("Pocketing opponent coin must not grant extra turn", CarromTurn.PLAYER_2, settled.turn)
    }

    @Test
    fun carrom_strikerPlacementAvoidsOverlappingBaselineCoin() {
        val pieces = listOf(
            CarromPiece(id = 0, type = CarromPieceType.WHITE, x = 0f, y = 0.65f),
            CarromPiece(id = 1, type = CarromPieceType.STRIKER, x = -0.4f, y = 0.65f)
        )
        val start = CarromGameState(pieces = pieces, turn = CarromTurn.PLAYER_1, phase = CarromPhase.AIMING)
        val updated = CarromEngine.setStrikerBaselineX(start, 0f)
        val striker = updated.pieces.first { it.type == CarromPieceType.STRIKER }
        val coin = updated.pieces.first { it.type == CarromPieceType.WHITE }

        assertNotEquals(0f, striker.x, 0.01f)
        assertTrue(hypot(striker.x - coin.x, striker.y - coin.y) >= striker.radius + coin.radius)
    }

    @Test
    fun carrom_strikerSlidesFullBaselineAndShootsFromChosenPosition() {
        val start = CarromEngine.newGame(isVsBot = true)

        val farLeft = CarromEngine.setStrikerBaselineX(start, -0.60f)
        assertEquals(-0.60f, farLeft.strikerBaselineX, 0.001f)
        val strikerLeft = farLeft.pieces.first { it.type == CarromPieceType.STRIKER }
        assertEquals(-0.60f, strikerLeft.x, 0.001f)

        val shotLeft = CarromEngine.shootStriker(farLeft, 0.2f, -0.9f)
        assertEquals(CarromPhase.MOVING, shotLeft.phase)
        val launchedStriker = shotLeft.pieces.first { it.type == CarromPieceType.STRIKER }
        assertEquals("Striker must launch from far-left chosen X, not center", -0.60f, launchedStriker.x, 0.001f)
        assertTrue(launchedStriker.vy < -0.5f)

        val farRight = CarromEngine.setStrikerBaselineX(start, 0.60f)
        assertEquals(0.60f, farRight.strikerBaselineX, 0.001f)
        val strikerRight = farRight.pieces.first { it.type == CarromPieceType.STRIKER }
        assertEquals(0.60f, strikerRight.x, 0.001f)
    }

    @Test
    fun playWalletRepository_seedsAllNineInbuiltGamesIncludingWarLand() {
        assertEquals(9, PlayWalletRepository.ALL_INBUILT_GAME_IDS.size)
        assertTrue("WAR_LAND" in PlayWalletRepository.ALL_INBUILT_GAME_IDS)
        assertTrue("LUDO" in PlayWalletRepository.ALL_INBUILT_GAME_IDS)
        assertTrue("SNAKES_LADDERS" in PlayWalletRepository.ALL_INBUILT_GAME_IDS)
    }
}
