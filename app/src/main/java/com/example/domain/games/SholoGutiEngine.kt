package com.example.domain.games

import kotlin.math.abs
import kotlin.random.Random

enum class SholoGutiPlayer {
    RUBY, // Bottom player (Human / Host)
    GOLD; // Top player (Bot / Guest)

    fun opposite(): SholoGutiPlayer = if (this == RUBY) GOLD else RUBY
}

enum class SholoGutiOutcome {
    ONGOING,
    RUBY_WINS,
    GOLD_WINS,
    DRAW
}

data class SholoGutiNode(
    val id: Int,
    val gridX: Int, // 0..4
    val gridY: Int, // 0..8
    val normX: Float, // 0f..1f for rendering
    val normY: Float  // 0f..1f for rendering
)

data class SholoGutiMove(
    val from: Int,
    val to: Int,
    val capturedNode: Int? = null
) {
    val isCapture: Boolean get() = capturedNode != null
}

data class SholoGutiState(
    /** 37 intersections: null = empty, RUBY = P1, GOLD = P2/Bot */
    val board: List<SholoGutiPlayer?> = SholoGutiEngine.initialBoard(),
    val turn: SholoGutiPlayer = SholoGutiPlayer.RUBY,
    /** Non-null while a bead is mid multi-jump capture chain and may jump again. */
    val chainedNodeId: Int? = null,
    val lastMove: SholoGutiMove? = null,
    val rubyCaptured: Int = 0, // How many GOLD beads RUBY has captured
    val goldCaptured: Int = 0, // How many RUBY beads GOLD has captured
    val movesWithoutCapture: Int = 0,
    val turnSequence: Int = 0,
    val outcome: SholoGutiOutcome = SholoGutiOutcome.ONGOING,
    val statusLog: String = "Ruby's Turn • Select a bead to move"
) {
    val rubyCount: Int get() = board.count { it == SholoGutiPlayer.RUBY }
    val goldCount: Int get() = board.count { it == SholoGutiPlayer.GOLD }
}

/**
 * Authentic 37-point South Asian Sholo Guti (Sixteen Soldiers / ১৬ গুটি) engine:
 * - Top triangle wing (6 nodes: rows 0..1)
 * - 5×5 central square with Alquerque/Sholo Guti diagonals (25 nodes: rows 2..6)
 * - Bottom triangle wing (6 nodes: rows 7..8)
 * - 16 Ruby beads (bottom) vs 16 Gold beads (top); entire center row (5 nodes) starts empty.
 * - Supports adjacent slides, straight-line jump captures, multi-jump capture chains,
 *   and smart Bot AI with configurable accuracy.
 */
object SholoGutiEngine {
    const val NODE_COUNT = 37

    private val rawCoords: List<Pair<Int, Int>> = listOf(
        // Top triangle wing (6 nodes: 0..5)
        0 to 0, 2 to 0, 4 to 0,
        1 to 1, 2 to 1, 3 to 1,
        // Central 5x5 grid (25 nodes: 6..30)
        0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2,
        0 to 3, 1 to 3, 2 to 3, 3 to 3, 4 to 3,
        0 to 4, 1 to 4, 2 to 4, 3 to 4, 4 to 4,
        0 to 5, 1 to 5, 2 to 5, 3 to 5, 4 to 5,
        0 to 6, 1 to 6, 2 to 6, 3 to 6, 4 to 6,
        // Bottom triangle wing (6 nodes: 31..36)
        1 to 7, 2 to 7, 3 to 7,
        0 to 8, 2 to 8, 4 to 8
    )

    val NODES: List<SholoGutiNode> = rawCoords.mapIndexed { id, (gx, gy) ->
        SholoGutiNode(
            id = id,
            gridX = gx,
            gridY = gy,
            normX = 0.11f + (gx / 4f) * 0.78f,
            normY = 0.065f + (gy / 8f) * 0.87f
        )
    }

    private val coordToId: Map<Pair<Int, Int>, Int> =
        NODES.associate { (it.gridX to it.gridY) to it.id }

    /** Undirected board edges (fromId < toId) for rendering & graph traversal. */
    val EDGES: List<Pair<Int, Int>> = buildList {
        val seen = HashSet<Pair<Int, Int>>()
        fun link(a: Pair<Int, Int>, b: Pair<Int, Int>) {
            val idA = coordToId[a] ?: return
            val idB = coordToId[b] ?: return
            val pair = if (idA < idB) idA to idB else idB to idA
            if (seen.add(pair)) add(pair)
        }

        // Top triangle wing
        link(0 to 0, 2 to 0)
        link(2 to 0, 4 to 0)
        link(1 to 1, 2 to 1)
        link(2 to 1, 3 to 1)
        link(2 to 0, 2 to 1)
        link(2 to 1, 2 to 2)
        link(0 to 0, 1 to 1)
        link(1 to 1, 2 to 2)
        link(4 to 0, 3 to 1)
        link(3 to 1, 2 to 2)

        // Central 5x5 grid orthogonal edges
        for (y in 2..6) {
            for (x in 0..4) {
                if (x + 1 <= 4) link(x to y, (x + 1) to y)
                if (y + 1 <= 6) link(x to y, x to (y + 1))
            }
        }
        // Central 5x5 grid authentic Sholo Guti diagonals (on nodes where (x + y) is even)
        for (y in 2..6) {
            for (x in 0..4) {
                if ((x + y) % 2 == 0) {
                    if (x + 1 <= 4 && y + 1 <= 6) link(x to y, (x + 1) to (y + 1))
                    if (x - 1 >= 0 && y + 1 <= 6) link(x to y, (x - 1) to (y + 1))
                }
            }
        }

        // Bottom triangle wing
        link(2 to 6, 2 to 7)
        link(2 to 7, 2 to 8)
        link(2 to 6, 1 to 7)
        link(1 to 7, 0 to 8)
        link(2 to 6, 3 to 7)
        link(3 to 7, 4 to 8)
        link(1 to 7, 2 to 7)
        link(2 to 7, 3 to 7)
        link(0 to 8, 2 to 8)
        link(2 to 8, 4 to 8)
    }

    val ADJACENCY: Map<Int, List<Int>> = buildMap {
        val m = mutableMapOf<Int, MutableList<Int>>()
        for (i in 0 until NODE_COUNT) m[i] = mutableListOf()
        for ((a, b) in EDGES) {
            m[a]!!.add(b)
            m[b]!!.add(a)
        }
        m.forEach { (k, v) -> put(k, v.sorted()) }
    }

    data class JumpTriple(val from: Int, val over: Int, val to: Int)

    /**
     * All geometrically collinear jump triples `(from -> over -> to)` along board lines.
     */
    val JUMPS_FROM: Map<Int, List<JumpTriple>> = buildMap {
        for (from in 0 until NODE_COUNT) {
            val fNode = NODES[from]
            val list = mutableListOf<JumpTriple>()
            for (over in ADJACENCY[from].orEmpty()) {
                val oNode = NODES[over]
                val dx1 = oNode.gridX - fNode.gridX
                val dy1 = oNode.gridY - fNode.gridY
                for (to in ADJACENCY[over].orEmpty()) {
                    if (to == from) continue
                    val tNode = NODES[to]
                    val dx2 = tNode.gridX - oNode.gridX
                    val dy2 = tNode.gridY - oNode.gridY
                    // Positively collinear straight line jump
                    val cross = dx1 * dy2 - dy1 * dx2
                    val dot = dx1 * dx2 + dy1 * dy2
                    if (cross == 0 && dot > 0) {
                        list.add(JumpTriple(from, over, to))
                    }
                }
            }
            put(from, list)
        }
    }

    fun initialBoard(): List<SholoGutiPlayer?> {
        val arr = arrayOfNulls<SholoGutiPlayer>(NODE_COUNT)
        for (i in 0..15) arr[i] = SholoGutiPlayer.GOLD
        // 16..20 = middle row (5 empty intersections)
        for (i in 21..36) arr[i] = SholoGutiPlayer.RUBY
        return arr.toList()
    }

    fun newGame(): SholoGutiState = SholoGutiState(
        board = initialBoard(),
        turn = SholoGutiPlayer.RUBY,
        statusLog = "Ruby's Turn • Tap a bead to move or jump"
    )

    fun captureMovesForNode(state: SholoGutiState, fromNode: Int): List<SholoGutiMove> {
        if (state.outcome != SholoGutiOutcome.ONGOING) return emptyList()
        val owner = state.board.getOrNull(fromNode) ?: return emptyList()
        if (owner != state.turn) return emptyList()
        val enemy = owner.opposite()
        return JUMPS_FROM[fromNode].orEmpty().mapNotNull { j ->
            if (state.board[j.over] == enemy && state.board[j.to] == null) {
                SholoGutiMove(from = j.from, to = j.to, capturedNode = j.over)
            } else null
        }
    }

    fun legalMovesForNode(state: SholoGutiState, fromNode: Int): List<SholoGutiMove> {
        if (state.outcome != SholoGutiOutcome.ONGOING) return emptyList()
        val owner = state.board.getOrNull(fromNode) ?: return emptyList()
        if (owner != state.turn) return emptyList()

        // If mid multi-jump capture chain, ONLY continuation captures from chainedNodeId are allowed
        if (state.chainedNodeId != null) {
            if (fromNode != state.chainedNodeId) return emptyList()
            return captureMovesForNode(state, fromNode)
        }

        val captures = captureMovesForNode(state, fromNode)
        val slides = ADJACENCY[fromNode].orEmpty()
            .filter { state.board[it] == null }
            .map { SholoGutiMove(from = fromNode, to = it, capturedNode = null) }
        return captures + slides
    }

    fun allLegalMoves(state: SholoGutiState): List<SholoGutiMove> {
        if (state.outcome != SholoGutiOutcome.ONGOING) return emptyList()
        if (state.chainedNodeId != null) {
            return captureMovesForNode(state, state.chainedNodeId)
        }
        val out = mutableListOf<SholoGutiMove>()
        for (i in 0 until NODE_COUNT) {
            if (state.board[i] == state.turn) {
                out.addAll(legalMovesForNode(state, i))
            }
        }
        return out
    }

    /** Ends an active multi-jump chain early (if the player chooses not to jump further). */
    fun endCaptureChain(state: SholoGutiState): SholoGutiState {
        if (state.chainedNodeId == null || state.outcome != SholoGutiOutcome.ONGOING) return state
        val nextTurn = state.turn.opposite()
        val base = state.copy(
            turn = nextTurn,
            chainedNodeId = null,
            turnSequence = state.turnSequence + 1
        )
        return finalizeOutcome(base)
    }

    fun applyMove(state: SholoGutiState, move: SholoGutiMove): SholoGutiState {
        if (state.outcome != SholoGutiOutcome.ONGOING) return state
        val legal = legalMovesForNode(state, move.from)
        val valid = legal.firstOrNull { it.to == move.to && it.capturedNode == move.capturedNode }
            ?: return state

        val nextBoard = state.board.toMutableList()
        val mover = state.turn
        nextBoard[valid.from] = null
        nextBoard[valid.to] = mover
        if (valid.capturedNode != null) {
            nextBoard[valid.capturedNode] = null
        }

        val nextRubyCap = if (valid.isCapture && mover == SholoGutiPlayer.RUBY) state.rubyCaptured + 1 else state.rubyCaptured
        val nextGoldCap = if (valid.isCapture && mover == SholoGutiPlayer.GOLD) state.goldCaptured + 1 else state.goldCaptured
        val nextQuietMoves = if (valid.isCapture) 0 else state.movesWithoutCapture + 1

        // Check if opponent is already out of beads
        val enemy = mover.opposite()
        val enemyRemaining = nextBoard.count { it == enemy }
        if (enemyRemaining == 0) {
            val winnerOutcome = if (mover == SholoGutiPlayer.RUBY) SholoGutiOutcome.RUBY_WINS else SholoGutiOutcome.GOLD_WINS
            val winnerName = if (mover == SholoGutiPlayer.RUBY) "Ruby" else "Gold"
            return state.copy(
                board = nextBoard,
                chainedNodeId = null,
                lastMove = valid,
                rubyCaptured = nextRubyCap,
                goldCaptured = nextGoldCap,
                movesWithoutCapture = 0,
                turnSequence = state.turnSequence + 1,
                outcome = winnerOutcome,
                statusLog = "🏆 $winnerName captured all 16 beads & WON!"
            )
        }

        // Check multi-jump continuation if this was a capture
        if (valid.isCapture) {
            val probeState = state.copy(
                board = nextBoard,
                turn = mover,
                chainedNodeId = valid.to,
                lastMove = valid,
                rubyCaptured = nextRubyCap,
                goldCaptured = nextGoldCap,
                movesWithoutCapture = 0,
                turnSequence = state.turnSequence + 1
            )
            val followUps = captureMovesForNode(probeState, valid.to)
            if (followUps.isNotEmpty()) {
                val moverName = if (mover == SholoGutiPlayer.RUBY) "Ruby" else "Gold"
                return probeState.copy(
                    statusLog = "⚡ $moverName Double-Jump Ready! Tap highlighted spot to chain capture"
                )
            }
        }

        // Draw check on 60 quiet moves without capture
        if (nextQuietMoves >= 60) {
            return state.copy(
                board = nextBoard,
                chainedNodeId = null,
                lastMove = valid,
                rubyCaptured = nextRubyCap,
                goldCaptured = nextGoldCap,
                movesWithoutCapture = nextQuietMoves,
                turnSequence = state.turnSequence + 1,
                outcome = SholoGutiOutcome.DRAW,
                statusLog = "Match Draw (60 moves without capture)"
            )
        }

        val nextTurn = enemy
        val nextState = state.copy(
            board = nextBoard,
            turn = nextTurn,
            chainedNodeId = null,
            lastMove = valid,
            rubyCaptured = nextRubyCap,
            goldCaptured = nextGoldCap,
            movesWithoutCapture = nextQuietMoves,
            turnSequence = state.turnSequence + 1
        )
        return finalizeOutcome(nextState)
    }

    private fun finalizeOutcome(state: SholoGutiState): SholoGutiState {
        val nextMoves = allLegalMoves(state)
        if (nextMoves.isEmpty()) {
            val winner = state.turn.opposite()
            val outcome = if (winner == SholoGutiPlayer.RUBY) SholoGutiOutcome.RUBY_WINS else SholoGutiOutcome.GOLD_WINS
            val winnerName = if (winner == SholoGutiPlayer.RUBY) "Ruby" else "Gold"
            return state.copy(
                outcome = outcome,
                statusLog = "🏆 $winnerName wins (opponent blocked with no moves)!"
            )
        }
        val turnName = if (state.turn == SholoGutiPlayer.RUBY) "Ruby" else "Gold"
        val hasCap = nextMoves.any { it.isCapture }
        val log = if (hasCap) "$turnName's Turn • Capture opportunity available!" else "$turnName's Turn • Select a bead"
        return state.copy(statusLog = log)
    }
}

object SholoGutiBotAi {
    fun chooseMove(
        state: SholoGutiState,
        accuracyPercent: Int,
        rng: Random = Random.Default
    ): SholoGutiMove? {
        val legal = SholoGutiEngine.allLegalMoves(state)
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal.first()

        val acc = accuracyPercent.coerceIn(10, 100)
        if (acc <= 25 && rng.nextInt(100) > acc + 15) {
            return legal.random(rng)
        }

        val bot = state.turn
        val scored = legal.map { mv ->
            val after = SholoGutiEngine.applyMove(state, mv)
            var score = 0.0

            // 1. Immediate win
            if ((bot == SholoGutiPlayer.GOLD && after.outcome == SholoGutiOutcome.GOLD_WINS) ||
                (bot == SholoGutiPlayer.RUBY && after.outcome == SholoGutiOutcome.RUBY_WINS)
            ) {
                score += 10_000.0
            }

            // 2. Captures & multi-jump chains
            if (mv.isCapture) {
                score += 140.0
                if (after.chainedNodeId == mv.to) {
                    val chainDepth = estimateMaxChainDepth(after, depthLimit = 4)
                    score += 110.0 * chainDepth
                }
            }

            // 3. Avoid walking into an immediate enemy recapture (2-ply tactical safety)
            val resolvedTurnState = if (after.chainedNodeId != null) {
                simulateGreedyChain(after)
            } else after

            if (resolvedTurnState.outcome == SholoGutiOutcome.ONGOING && resolvedTurnState.turn != bot) {
                val enemyCaptures = SholoGutiEngine.allLegalMoves(resolvedTurnState).filter { it.isCapture }
                if (enemyCaptures.isNotEmpty()) {
                    val maxEnemyLoss = enemyCaptures.maxOf { eMv ->
                        val afterEnemy = SholoGutiEngine.applyMove(resolvedTurnState, eMv)
                        val chain = if (afterEnemy.chainedNodeId != null) estimateMaxChainDepth(afterEnemy, 3) else 0
                        1 + chain
                    }
                    score -= 125.0 * maxEnemyLoss
                }
            }

            // 4. Center control & forward progress
            val destNode = SholoGutiEngine.NODES[mv.to]
            val distToCenter = abs(destNode.gridX - 2) + abs(destNode.gridY - 4)
            score += (6 - distToCenter) * 4.5
            // Slight mobility bonus (nodes with more edges are stronger pivots)
            score += (SholoGutiEngine.ADJACENCY[mv.to]?.size ?: 0) * 2.2
            // Small jitter for variety at equal evaluations
            score += rng.nextDouble(-1.5, 1.5)

            mv to score
        }.sortedByDescending { it.second }

        return when {
            acc >= 85 -> scored.first().first
            acc >= 55 -> {
                val top2 = scored.take(2)
                if (rng.nextInt(100) < acc) top2.first().first else top2.last().first
            }
            else -> scored.take(4.coerceAtMost(scored.size)).random(rng).first
        }
    }

    private fun estimateMaxChainDepth(state: SholoGutiState, depthLimit: Int): Int {
        if (depthLimit <= 0 || state.chainedNodeId == null) return 0
        val nextCaps = SholoGutiEngine.captureMovesForNode(state, state.chainedNodeId)
        if (nextCaps.isEmpty()) return 0
        return 1 + nextCaps.maxOf { mv ->
            val next = SholoGutiEngine.applyMove(state, mv)
            estimateMaxChainDepth(next, depthLimit - 1)
        }
    }

    private fun simulateGreedyChain(state: SholoGutiState): SholoGutiState {
        var cur = state
        var guard = 0
        while (cur.chainedNodeId != null && cur.outcome == SholoGutiOutcome.ONGOING && guard < 8) {
            val caps = SholoGutiEngine.captureMovesForNode(cur, cur.chainedNodeId!!)
            if (caps.isEmpty()) break
            cur = SholoGutiEngine.applyMove(cur, caps.first())
            guard++
        }
        return cur
    }
}
