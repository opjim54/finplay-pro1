package com.example.domain.games

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class CarromPieceType {
    WHITE,
    BLACK,
    QUEEN,
    STRIKER
}

enum class CarromTurn {
    PLAYER_1, // Bottom player (White)
    PLAYER_2  // Top player / Bot (Black)
}

enum class CarromPhase {
    AIMING,
    MOVING,
    ROUND_OVER
}

enum class QueenStatus {
    ON_BOARD,
    POCKETED_PENDING_COVER,
    COVERED_BY_P1,
    COVERED_BY_P2
}

data class CarromPiece(
    val id: Int,
    val type: CarromPieceType,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val radius: Float = if (type == CarromPieceType.STRIKER) 0.082f else 0.055f,
    val isPocketed: Boolean = false,
    val animScale: Float = 1.0f,
    val pocketId: Int = -1
)

data class CarromPocket(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 0.11f
)

data class CarromGameState(
    val pieces: List<CarromPiece>,
    val turn: CarromTurn = CarromTurn.PLAYER_1,
    val phase: CarromPhase = CarromPhase.AIMING,
    val strikerBaselineX: Float = 0.0f,
    val strikerBaselineY: Float = 0.65f,
    val queenStatus: QueenStatus = QueenStatus.ON_BOARD,
    val queenPendingCoverBy: CarromTurn? = null,
    val queenPocketedThisTurn: Boolean = false,
    val coinPocketedThisTurn: Boolean = false,
    val foulOccurredThisTurn: Boolean = false,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val player1PocketedCount: Int = 0,
    val player2PocketedCount: Int = 0,
    val isVsBot: Boolean = true,
    val botAccuracy: Int = 85,
    val winner: CarromTurn? = null,
    val statusMessage: String = "",
    val recentScoreToast: String? = null,
    val shotNumber: Int = 0,
    val lastPocketedPieceType: CarromPieceType? = null
)

object CarromEngine {
    const val BOARD_BOUND = 0.88f
    const val BASELINE_MAX_X = 0.60f
    const val FRICTION = 0.982f
    const val RESTITUTION = 0.88f
    const val MIN_VELOCITY = 0.005f
    const val MAX_IMPULSE = 1.05f

    val POCKETS = listOf(
        CarromPocket(1, -0.83f, -0.83f), // Top-Left
        CarromPocket(2, 0.83f, -0.83f),  // Top-Right
        CarromPocket(3, -0.83f, 0.83f),  // Bottom-Left
        CarromPocket(4, 0.83f, 0.83f)   // Bottom-Right
    )

    fun newGame(isVsBot: Boolean = true, botAccuracy: Int = 85): CarromGameState {
        val pieces = setupInitialPieces()
        return CarromGameState(
            pieces = pieces,
            turn = CarromTurn.PLAYER_1,
            phase = CarromPhase.AIMING,
            strikerBaselineX = 0f,
            strikerBaselineY = 0.65f,
            queenStatus = QueenStatus.ON_BOARD,
            isVsBot = isVsBot,
            botAccuracy = botAccuracy,
            statusMessage = "Player 1's Turn • Aim & Strike"
        )
    }

    private fun setupInitialPieces(): List<CarromPiece> {
        val list = mutableListOf<CarromPiece>()
        var nextId = 0

        // 1. Red Queen at exact center (0, 0)
        list.add(
            CarromPiece(
                id = nextId++,
                type = CarromPieceType.QUEEN,
                x = 0f,
                y = 0f
            )
        )

        // 2. Ring 1: 6 pieces (alternating White, Black, White, Black, White, Black)
        val r1 = 0.114f
        for (i in 0 until 6) {
            val angle = i * (PI.toFloat() / 3f)
            val type = if (i % 2 == 0) CarromPieceType.WHITE else CarromPieceType.BLACK
            list.add(
                CarromPiece(
                    id = nextId++,
                    type = type,
                    x = r1 * cos(angle),
                    y = r1 * sin(angle)
                )
            )
        }

        // 3. Ring 2: 12 pieces (6 White, 6 Black alternating)
        val r2 = 0.228f
        for (i in 0 until 12) {
            val angle = (i * (PI.toFloat() / 6f)) + (PI.toFloat() / 12f)
            val type = if (i % 2 == 0) CarromPieceType.WHITE else CarromPieceType.BLACK
            list.add(
                CarromPiece(
                    id = nextId++,
                    type = type,
                    x = r2 * cos(angle),
                    y = r2 * sin(angle)
                )
            )
        }

        // 4. Striker placed on Player 1 baseline
        list.add(
            CarromPiece(
                id = nextId,
                type = CarromPieceType.STRIKER,
                x = 0f,
                y = 0.65f
            )
        )

        return list
    }

    fun setStrikerBaselineX(state: CarromGameState, x: Float): CarromGameState {
        if (state.phase != CarromPhase.AIMING) return state
        val desiredX = x.coerceIn(-BASELINE_MAX_X, BASELINE_MAX_X)
        val safeX = findNonOverlappingBaselineX(
            pieces = state.pieces,
            desiredX = desiredX,
            baselineY = state.strikerBaselineY,
            preferRight = desiredX >= state.strikerBaselineX
        )
        val updatedPieces = state.pieces.map { piece ->
            if (piece.type == CarromPieceType.STRIKER) {
                piece.copy(x = safeX, y = state.strikerBaselineY, vx = 0f, vy = 0f)
            } else piece
        }
        return state.copy(strikerBaselineX = safeX, pieces = updatedPieces)
    }

    private fun findNonOverlappingBaselineX(
        pieces: List<CarromPiece>,
        desiredX: Float,
        baselineY: Float,
        preferRight: Boolean = true
    ): Float {
        val strikerRadius = 0.082f
        fun overlaps(testX: Float): Boolean = pieces.any { p ->
            p.type != CarromPieceType.STRIKER && !p.isPocketed && p.pocketId == -1 && run {
                val dx = p.x - testX
                val dy = p.y - baselineY
                val minDist = p.radius + strikerRadius + 0.004f
                (dx * dx + dy * dy) < (minDist * minDist)
            }
        }
        if (!overlaps(desiredX)) return desiredX
        // First scan full remaining baseline in the player's drag direction so striker glides past blocking coins
        for (step in 1..65) {
            val delta = step * 0.02f
            val cand = if (preferRight) desiredX + delta else desiredX - delta
            if (cand in -BASELINE_MAX_X..BASELINE_MAX_X && !overlaps(cand)) return cand
        }
        // Only search the opposite direction if no slot exists ahead
        for (step in 1..65) {
            val delta = step * 0.02f
            val fallback = if (preferRight) desiredX - delta else desiredX + delta
            if (fallback in -BASELINE_MAX_X..BASELINE_MAX_X && !overlaps(fallback)) return fallback
        }
        return desiredX
    }

    fun shootStriker(state: CarromGameState, impulseX: Float, impulseY: Float): CarromGameState {
        if (state.phase != CarromPhase.AIMING) return state

        val magnitude = sqrt(impulseX * impulseX + impulseY * impulseY)
        val scale = if (magnitude > MAX_IMPULSE) MAX_IMPULSE / magnitude else 1.0f
        val finalVx = impulseX * scale
        val finalVy = impulseY * scale

        val updatedPieces = state.pieces.map { piece ->
            if (piece.type == CarromPieceType.STRIKER) {
                piece.copy(
                    vx = finalVx,
                    vy = finalVy,
                    isPocketed = false,
                    animScale = 1.0f
                )
            } else piece
        }

        return state.copy(
            pieces = updatedPieces,
            phase = CarromPhase.MOVING,
            coinPocketedThisTurn = false,
            queenPocketedThisTurn = false,
            foulOccurredThisTurn = false,
            recentScoreToast = null,
            lastPocketedPieceType = null,
            shotNumber = state.shotNumber + 1
        )
    }

    fun updatePhysics(state: CarromGameState, dt: Float = 0.016f): CarromGameState {
        if (state.phase != CarromPhase.MOVING) return state

        val pieces = state.pieces.map { it.copy() }.toMutableList()
        var coinPocketed = state.coinPocketedThisTurn
        var queenPocketed = state.queenPocketedThisTurn
        var foulOccurred = state.foulOccurredThisTurn
        var p1ScoreDelta = 0
        var p2ScoreDelta = 0
        var p1CountDelta = 0
        var p2CountDelta = 0
        var scoreToast: String? = state.recentScoreToast
        var lastType: CarromPieceType? = state.lastPocketedPieceType
        var newQueenStatus = state.queenStatus

        val step = (dt * 60f).coerceIn(0.4f, 2.0f)
        val frictionFactor = FRICTION.toDouble().pow(step.toDouble()).toFloat()
        var newQueenPendingBy = state.queenPendingCoverBy

        // 1. Move & apply frame-rate normalized friction
        for (i in pieces.indices) {
            val p = pieces[i]
            if (p.isPocketed) continue

            // Pocket sink animation progress
            if (p.pocketId != -1) {
                val targetPocket = POCKETS.first { it.id == p.pocketId }
                val newScale = (p.animScale - 3.5f * dt).coerceAtLeast(0f)
                val lerp = (0.25f * step).coerceAtMost(0.6f)
                val newX = p.x + (targetPocket.x - p.x) * lerp
                val newY = p.y + (targetPocket.y - p.y) * lerp
                if (newScale <= 0.05f) {
                    pieces[i] = p.copy(
                        isPocketed = true,
                        animScale = 0f,
                        x = targetPocket.x,
                        y = targetPocket.y,
                        vx = 0f,
                        vy = 0f
                    )
                } else {
                    pieces[i] = p.copy(animScale = newScale, x = newX, y = newY, vx = 0f, vy = 0f)
                }
                continue
            }

            var vx = p.vx * frictionFactor
            var vy = p.vy * frictionFactor
            if (abs(vx) < MIN_VELOCITY) vx = 0f
            if (abs(vy) < MIN_VELOCITY) vy = 0f

            var x = p.x + vx * step
            var y = p.y + vy * step

            // Cushion wall bounce with restitution
            val bound = BOARD_BOUND - p.radius
            if (x < -bound) {
                x = -bound
                vx = -vx * RESTITUTION
            } else if (x > bound) {
                x = bound
                vx = -vx * RESTITUTION
            }
            if (y < -bound) {
                y = -bound
                vy = -vy * RESTITUTION
            } else if (y > bound) {
                y = bound
                vy = -vy * RESTITUTION
            }

            pieces[i] = p.copy(x = x, y = y, vx = vx, vy = vy)
        }

        // 2. Elastic Piece-on-Piece collisions (iterative relaxation)
        val pieceCount = pieces.size
        for (i in 0 until pieceCount) {
            val p1 = pieces[i]
            if (p1.isPocketed || p1.pocketId != -1) continue

            for (j in (i + 1) until pieceCount) {
                val p2 = pieces[j]
                if (p2.isPocketed || p2.pocketId != -1) continue

                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = p1.radius + p2.radius

                if (dist < minDist && dist > 0.0001f) {
                    val nx = dx / dist
                    val ny = dy / dist

                    // Positional overlap resolution
                    val overlap = 0.5f * (minDist - dist)
                    val p1NewX = p1.x - nx * overlap
                    val p1NewY = p1.y - ny * overlap
                    val p2NewX = p2.x + nx * overlap
                    val p2NewY = p2.y + ny * overlap

                    // Elastic momentum transfer
                    val dvx = p2.vx - p1.vx
                    val dvy = p2.vy - p1.vy
                    val velAlongNormal = dvx * nx + dvy * ny

                    if (velAlongNormal < 0f) {
                        val m1 = if (p1.type == CarromPieceType.STRIKER) 3.0f else 1.0f
                        val m2 = if (p2.type == CarromPieceType.STRIKER) 3.0f else 1.0f
                        val impulse = -(1f + 0.92f) * velAlongNormal / (1f / m1 + 1f / m2)

                        val p1NewVx = p1.vx - (impulse / m1) * nx
                        val p1NewVy = p1.vy - (impulse / m1) * ny
                        val p2NewVx = p2.vx + (impulse / m2) * nx
                        val p2NewVy = p2.vy + (impulse / m2) * ny

                        pieces[i] = pieces[i].copy(x = p1NewX, y = p1NewY, vx = p1NewVx, vy = p1NewVy)
                        pieces[j] = pieces[j].copy(x = p2NewX, y = p2NewY, vx = p2NewVx, vy = p2NewVy)
                    } else {
                        pieces[i] = pieces[i].copy(x = p1NewX, y = p1NewY)
                        pieces[j] = pieces[j].copy(x = p2NewX, y = p2NewY)
                    }
                }
            }
        }

        // 3. Pocket capture check
        for (i in pieces.indices) {
            val p = pieces[i]
            if (p.isPocketed || p.pocketId != -1) continue

            for (pocket in POCKETS) {
                val dx = p.x - pocket.x
                val dy = p.y - pocket.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < pocket.radius) {
                    pieces[i] = p.copy(pocketId = pocket.id, vx = 0f, vy = 0f)

                    when (p.type) {
                        CarromPieceType.STRIKER -> {
                            foulOccurred = true
                            scoreToast = "Foul! Striker Pocketed"
                            lastType = CarromPieceType.STRIKER
                        }
                        CarromPieceType.WHITE -> {
                            lastType = CarromPieceType.WHITE
                            p1ScoreDelta += 10
                            p1CountDelta += 1
                            if (state.turn == CarromTurn.PLAYER_1) {
                                coinPocketed = true
                                scoreToast = "+10 White Coin!"
                                if (newQueenStatus == QueenStatus.POCKETED_PENDING_COVER && newQueenPendingBy == CarromTurn.PLAYER_1) {
                                    newQueenStatus = QueenStatus.COVERED_BY_P1
                                    newQueenPendingBy = null
                                    p1ScoreDelta += 25
                                    scoreToast = "Queen Covered! +25"
                                }
                            } else {
                                scoreToast = "White Coin Pocketed (+10 P1)"
                            }
                        }
                        CarromPieceType.BLACK -> {
                            lastType = CarromPieceType.BLACK
                            p2ScoreDelta += 10
                            p2CountDelta += 1
                            if (state.turn == CarromTurn.PLAYER_2) {
                                coinPocketed = true
                                scoreToast = "+10 Black Coin!"
                                if (newQueenStatus == QueenStatus.POCKETED_PENDING_COVER && newQueenPendingBy == CarromTurn.PLAYER_2) {
                                    newQueenStatus = QueenStatus.COVERED_BY_P2
                                    newQueenPendingBy = null
                                    p2ScoreDelta += 25
                                    scoreToast = "Queen Covered! +25"
                                }
                            } else {
                                scoreToast = "Black Coin Pocketed (+10 P2)"
                            }
                        }
                        CarromPieceType.QUEEN -> {
                            queenPocketed = true
                            lastType = CarromPieceType.QUEEN
                            if (coinPocketed) {
                                // Already pocketed own coin on this same strike — Queen is covered immediately!
                                if (state.turn == CarromTurn.PLAYER_1) {
                                    newQueenStatus = QueenStatus.COVERED_BY_P1
                                    p1ScoreDelta += 25
                                } else {
                                    newQueenStatus = QueenStatus.COVERED_BY_P2
                                    p2ScoreDelta += 25
                                }
                                newQueenPendingBy = null
                                scoreToast = "Queen Covered! +25"
                            } else {
                                newQueenStatus = QueenStatus.POCKETED_PENDING_COVER
                                newQueenPendingBy = state.turn
                                scoreToast = "Queen Pocketed! Cover Next Shot!"
                            }
                        }
                    }
                    break
                }
            }
        }

        // 4. Check if all motion has stopped
        val isStillMoving = pieces.any {
            (!it.isPocketed && (it.vx != 0f || it.vy != 0f)) || (it.pocketId != -1 && !it.isPocketed)
        }

        if (isStillMoving) {
            return state.copy(
                pieces = pieces,
                coinPocketedThisTurn = coinPocketed,
                queenPocketedThisTurn = queenPocketed,
                foulOccurredThisTurn = foulOccurred,
                player1Score = state.player1Score + p1ScoreDelta,
                player2Score = state.player2Score + p2ScoreDelta,
                player1PocketedCount = state.player1PocketedCount + p1CountDelta,
                player2PocketedCount = state.player2PocketedCount + p2CountDelta,
                queenStatus = newQueenStatus,
                queenPendingCoverBy = newQueenPendingBy,
                recentScoreToast = scoreToast,
                lastPocketedPieceType = lastType
            )
        }

        // --- Motion has stopped: Settle Turn & Check Game Over ---
        var finalPieces = pieces.map { it.copy() }.toMutableList()

        // Settle Queen Cover rule:
        // - If Queen was pocketed THIS shot without a foul, player gets the immediate next shot to cover it.
        // - If Queen was ALREADY pending cover from the previous shot (or a foul occurred) and wasn't covered, return Queen to center!
        val queenFailedCover = newQueenStatus == QueenStatus.POCKETED_PENDING_COVER &&
            (foulOccurred || (!queenPocketed && !coinPocketed))
        if (queenFailedCover) {
            newQueenStatus = QueenStatus.ON_BOARD
            newQueenPendingBy = null
            finalPieces = finalPieces.map { p ->
                if (p.type == CarromPieceType.QUEEN) {
                    p.copy(x = 0f, y = 0f, vx = 0f, vy = 0f, isPocketed = false, animScale = 1.0f, pocketId = -1)
                } else p
            }.toMutableList()
            scoreToast = "Queen Uncovered • Returned to Center"
        }

        // Settle Striker Foul penalty: return one previously pocketed piece of the active player to center
        if (foulOccurred) {
            val returnTargetType = if (state.turn == CarromTurn.PLAYER_1) CarromPieceType.WHITE else CarromPieceType.BLACK
            val pocketedIndex = finalPieces.indexOfFirst { it.type == returnTargetType && it.isPocketed }
            if (pocketedIndex != -1) {
                finalPieces[pocketedIndex] = finalPieces[pocketedIndex].copy(
                    x = 0f,
                    y = if (state.turn == CarromTurn.PLAYER_1) 0.12f else -0.12f,
                    vx = 0f,
                    vy = 0f,
                    isPocketed = false,
                    animScale = 1.0f,
                    pocketId = -1
                )
                if (state.turn == CarromTurn.PLAYER_1) {
                    p1ScoreDelta -= 10
                    p1CountDelta -= 1
                } else {
                    p2ScoreDelta -= 10
                    p2CountDelta -= 1
                }
            }
        }

        // Check Round Over condition:
        val remainingWhite = finalPieces.count { it.type == CarromPieceType.WHITE && !it.isPocketed }
        val remainingBlack = finalPieces.count { it.type == CarromPieceType.BLACK && !it.isPocketed }
        val queenOnBoard = finalPieces.any { it.type == CarromPieceType.QUEEN && !it.isPocketed }

        val isGameOver = remainingWhite == 0 || remainingBlack == 0 || (remainingWhite + remainingBlack <= 1 && !queenOnBoard)
        if (isGameOver) {
            val totalP1 = state.player1Score + p1ScoreDelta
            val totalP2 = state.player2Score + p2ScoreDelta
            val winner = when {
                totalP1 > totalP2 -> CarromTurn.PLAYER_1
                totalP2 > totalP1 -> CarromTurn.PLAYER_2
                else -> if (remainingWhite == 0) CarromTurn.PLAYER_1 else CarromTurn.PLAYER_2
            }
            return state.copy(
                pieces = finalPieces,
                phase = CarromPhase.ROUND_OVER,
                winner = winner,
                player1Score = totalP1,
                player2Score = totalP2,
                player1PocketedCount = state.player1PocketedCount + p1CountDelta,
                player2PocketedCount = state.player2PocketedCount + p2CountDelta,
                statusMessage = if (winner == CarromTurn.PLAYER_1) "Player 1 Wins the Match!" else "Player 2 / Bot Wins!",
                recentScoreToast = "Game Over • Champion Decided"
            )
        }

        // Determine next turn:
        // In carrom, pocketing your assigned piece or queen grants another shot unless a foul occurred!
        val extraTurn = (coinPocketed || queenPocketed) && !foulOccurred
        val nextTurn = if (extraTurn) state.turn else if (state.turn == CarromTurn.PLAYER_1) CarromTurn.PLAYER_2 else CarromTurn.PLAYER_1
        val nextBaselineY = if (nextTurn == CarromTurn.PLAYER_1) 0.65f else -0.65f

        val safeStrikerX = findNonOverlappingBaselineX(finalPieces, 0f, nextBaselineY)
        // Reset striker on baseline for next turn
        finalPieces = finalPieces.map { p ->
            if (p.type == CarromPieceType.STRIKER) {
                p.copy(
                    x = safeStrikerX,
                    y = nextBaselineY,
                    vx = 0f,
                    vy = 0f,
                    isPocketed = false,
                    animScale = 1.0f,
                    pocketId = -1
                )
            } else p
        }.toMutableList()

        val nextStatus = when {
            newQueenStatus == QueenStatus.POCKETED_PENDING_COVER ->
                "${if (nextTurn == CarromTurn.PLAYER_1) "Player 1" else "Player 2"} • Pocket a coin to Cover Queen!"
            extraTurn -> "${if (nextTurn == CarromTurn.PLAYER_1) "Player 1" else "Player 2"} Continues • Extra Turn!"
            nextTurn == CarromTurn.PLAYER_1 -> "Player 1's Turn • Aim & Strike"
            state.isVsBot -> "Smart Bot Thinking..."
            else -> "Player 2's Turn • Aim & Strike"
        }

        return state.copy(
            pieces = finalPieces,
            turn = nextTurn,
            phase = CarromPhase.AIMING,
            strikerBaselineX = safeStrikerX,
            strikerBaselineY = nextBaselineY,
            coinPocketedThisTurn = false,
            queenPocketedThisTurn = false,
            foulOccurredThisTurn = false,
            player1Score = (state.player1Score + p1ScoreDelta).coerceAtLeast(0),
            player2Score = (state.player2Score + p2ScoreDelta).coerceAtLeast(0),
            player1PocketedCount = (state.player1PocketedCount + p1CountDelta).coerceAtLeast(0),
            player2PocketedCount = (state.player2PocketedCount + p2CountDelta).coerceAtLeast(0),
            queenStatus = newQueenStatus,
            queenPendingCoverBy = newQueenPendingBy,
            statusMessage = nextStatus,
            recentScoreToast = scoreToast
        )
    }
}

object CarromBotAi {
    data class BotShot(
        val baselineX: Float,
        val impulseX: Float,
        val impulseY: Float
    )

    fun computeBotShot(state: CarromGameState, accuracyPercent: Int): Pair<Float, CarromBotAiOffset> {
        val shot = evaluateBestShot(state, accuracyPercent)
        return shot.baselineX to CarromBotAiOffset(shot.impulseX, shot.impulseY)
    }

    private fun evaluateBestShot(state: CarromGameState, accuracyPercent: Int): BotShot {
        val legalPieces = state.pieces.filter {
            !it.isPocketed && it.pocketId == -1 && (it.type == CarromPieceType.BLACK || it.type == CarromPieceType.QUEEN)
        }

        val targetPiece = legalPieces.minByOrNull { p ->
            CarromEngine.POCKETS.minOf { pocket ->
                val dx = p.x - pocket.x
                val dy = p.y - pocket.y
                dx * dx + dy * dy
            }
        } ?: state.pieces.firstOrNull { !it.isPocketed && it.type != CarromPieceType.STRIKER }

        if (targetPiece == null) {
            return BotShot(0f, 0f, 0.68f)
        }

        // Find nearest pocket to target piece
        val nearestPocket = CarromEngine.POCKETS.minByOrNull { pocket ->
            val dx = targetPiece.x - pocket.x
            val dy = targetPiece.y - pocket.y
            dx * dx + dy * dy
        } ?: CarromEngine.POCKETS[0]

        // Calculate ghost ball position (behind target piece relative to pocket)
        val pocketAngle = atan2(targetPiece.y - nearestPocket.y, targetPiece.x - nearestPocket.x)
        val ghostX = targetPiece.x + 0.11f * cos(pocketAngle)
        val ghostY = targetPiece.y + 0.11f * sin(pocketAngle)

        // Choose baseline X position closest to ghostX
        val baselineX = ghostX.coerceIn(-0.50f, 0.50f)
        val baselineY = -0.65f // Bot baseline is at top

        // Calculate aim vector from striker to ghost position
        val aimDx = ghostX - baselineX
        val aimDy = ghostY - baselineY
        var aimAngle = atan2(aimDy, aimDx)

        // Add humanized error based on accuracy percentage (e.g. 100% -> 0 error, 70% -> slight error)
        val maxJitter = (100 - accuracyPercent.coerceIn(40, 100)) * 0.003f
        val jitter = ((Math.random() - 0.5) * 2.0 * maxJitter).toFloat()
        aimAngle += jitter

        val speed = 0.68f + (Math.random() * 0.22f).toFloat()
        val impulseX = speed * cos(aimAngle)
        val impulseY = speed * sin(aimAngle)

        return BotShot(baselineX, impulseX, impulseY)
    }
}

data class CarromBotAiOffset(val x: Float, val y: Float)
