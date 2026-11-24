package com.example.domain.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Aether Crown: Flux Arena — An original dual-ring orbital strategy board game.
 *
 * Board Architecture:
 * - Outer Ring: 16 nodes (0..15 clockwise).
 *   - Nodes 0 & 8 are Anchor Home Bases (Player 1 = Emerald at 0, Player 2 = Amber at 8).
 *   - Nodes 0, 4, 8, 12 are Aether Switch Nodes (+1 Charge on landing + Phase-Shifts all 4 Flux Bridges!).
 * - Inner Sanctum Ring: 4 Vault Nodes (0..3, aligned with outer nodes 2, 6, 10, 14 via Flux Bridges).
 *   - Vaults grant Shield (cannot be captured) and +1 Aether Charge whenever you start your turn inside.
 * - Flux Bridges connect Outer <-> Inner at pairs: (2<->V0), (6<->V1), (10<->V2), (14<->V3).
 *   - 2 bridges are OPEN and 2 are SEALED at any time (polarity `bridgesPhaseEven = true` -> bridges at 2 & 10 open; `false` -> bridges at 6 & 14 open).
 *   - Bridges phase-shift automatically every 3 completed rounds OR immediately when any token lands on an Aether Switch Node (0, 4, 8, 12).
 *
 * Dual-Dice & Quantum Mechanics:
 * - Roll two 1..4 Aether Dice `[d1, d2]`.
 * - Mode 1 (Split Move): Move Token A by `d1`, then Token B by `d2`.
 * - Mode 2 (Surge Move): Combine `d1 + d2` onto one Token AND push 1 adjacent enemy away by 1 outer step.
 * - Mode 3 (Quantum Swap — available when Doubles `d1 == d2` are rolled): Teleport-swap any 2 active tokens on the board!
 *
 * Dual Win Conditions (first achieved wins):
 * 1. Ascend 3 Tokens through an Open Flux Bridge to claim Crowned Orbit (vault capture & lock), OR
 * 2. Reach 7 Aether Charges while holding at least 1 Shielded Inner Vault!
 */
enum class AetherPlayer(val displayName: String, val homeOuterNode: Int, val colorHex: Long) {
    SOLAR("Solaris Emerald", 0, 0xFF10B981),
    LUNAR("Aether Amber", 8, 0xFFF59E0B);

    fun opponent(): AetherPlayer = if (this == SOLAR) LUNAR else SOLAR
}

enum class AetherZone { DOCKED, OUTER, VAULT, CROWNED }

data class AetherToken(
    val owner: AetherPlayer,
    val id: Int, // 0..3
    val zone: AetherZone = AetherZone.DOCKED,
    val nodeIndex: Int = 0 // 0..15 if OUTER, 0..3 if VAULT
)

enum class AetherTurnPhase {
    ROLL_DICE,
    ALLOCATE_DICE,
    GAME_OVER
}

enum class AetherMoveType {
    DEPLOY_OR_MOVE_OUTER,
    CROSS_FLUX_BRIDGE,
    CROWN_FROM_VAULT,
    SURGE_MOVE,
    QUANTUM_SWAP
}

data class AetherAction(
    val type: AetherMoveType,
    val tokenId: Int,
    val diceCost: Int,
    val secondTokenId: Int? = null,
    val description: String
)

data class AetherCrownState(
    val tokens: Map<AetherPlayer, List<AetherToken>> = AetherCrownEngine.initialTokens(),
    val turn: AetherPlayer = AetherPlayer.SOLAR,
    val playVsBot: Boolean = true,
    val phase: AetherTurnPhase = AetherTurnPhase.ROLL_DICE,
    val dice: Pair<Int, Int> = 1 to 1,
    val remainingDice: List<Int> = emptyList(),
    val bridgesPhaseEven: Boolean = true,
    val roundCount: Int = 1,
    val charges: Map<AetherPlayer, Int> = mapOf(AetherPlayer.SOLAR to 0, AetherPlayer.LUNAR to 0),
    val winner: AetherPlayer? = null,
    val statusLog: String = "Solaris Emerald's turn — Roll Twin Aether Dice!"
)

object AetherCrownEngine {
    val SWITCH_NODES = setOf(0, 4, 8, 12)
    val BRIDGE_OUTER_TO_VAULT = mapOf(2 to 0, 6 to 1, 10 to 2, 14 to 3)
    val VAULT_TO_BRIDGE_OUTER = mapOf(0 to 2, 1 to 6, 2 to 10, 3 to 14)

    fun initialTokens(): Map<AetherPlayer, List<AetherToken>> = mapOf(
        AetherPlayer.SOLAR to (0..3).map { AetherToken(AetherPlayer.SOLAR, it) },
        AetherPlayer.LUNAR to (0..3).map { AetherToken(AetherPlayer.LUNAR, it) }
    )

    fun newGame(playVsBot: Boolean): AetherCrownState = AetherCrownState(playVsBot = playVsBot)

    fun isBridgeOpen(outerBridgeNode: Int, bridgesPhaseEven: Boolean): Boolean {
        return when (outerBridgeNode) {
            2, 10 -> bridgesPhaseEven
            6, 14 -> !bridgesPhaseEven
            else -> false
        }
    }

    fun rollDice(state: AetherCrownState, d1: Int, d2: Int): AetherCrownState {
        if (state.phase != AetherTurnPhase.ROLL_DICE) return state
        val v1 = d1.coerceIn(1, 4)
        val v2 = d2.coerceIn(1, 4)

        // Vault Passive Charge (+1 charge if holding at least 1 vault at start of turn)
        val myTokens = state.tokens[state.turn].orEmpty()
        val holdsVault = myTokens.any { it.zone == AetherZone.VAULT }
        val updatedCharges = state.charges.toMutableMap()
        if (holdsVault) {
            updatedCharges[state.turn] = (updatedCharges[state.turn] ?: 0) + 1
        }
        val winByCharge = holdsVault && (updatedCharges[state.turn] ?: 0) >= 7
        if (winByCharge) {
            return state.copy(
                charges = updatedCharges,
                phase = AetherTurnPhase.GAME_OVER,
                winner = state.turn,
                statusLog = "${state.turn.displayName} ascended via 7 Aether Charges + Sanctuary Vault!"
            )
        }

        val next = state.copy(
            dice = v1 to v2,
            remainingDice = listOf(v1, v2),
            charges = updatedCharges,
            phase = AetherTurnPhase.ALLOCATE_DICE
        )
        val actions = legalActions(next)
        if (actions.isEmpty()) {
            return advanceTurn(next, "${state.turn.displayName} rolled ($v1, $v2) — no valid moves.")
        }
        val doublesTag = if (v1 == v2) " (Doubles! Quantum Swap unlocked)" else ""
        return next.copy(
            statusLog = "${state.turn.displayName} rolled [$v1] & [$v2]$doublesTag"
        )
    }

    fun legalActions(state: AetherCrownState): List<AetherAction> {
        if (state.phase != AetherTurnPhase.ALLOCATE_DICE || state.remainingDice.isEmpty()) return emptyList()
        val out = mutableListOf<AetherAction>()
        val myTokens = state.tokens[state.turn].orEmpty()
        val distinctDice = state.remainingDice.distinct()

        for (die in distinctDice) {
            for (tk in myTokens) {
                when (tk.zone) {
                    AetherZone.DOCKED -> {
                        if (die == 1 || die == 4) {
                            out.add(
                                AetherAction(
                                    type = AetherMoveType.DEPLOY_OR_MOVE_OUTER,
                                    tokenId = tk.id,
                                    diceCost = die,
                                    description = "Deploy T${tk.id + 1} onto Home Node ${state.turn.homeOuterNode} (uses [$die])"
                                )
                            )
                        }
                    }

                    AetherZone.OUTER -> {
                        val dest = (tk.nodeIndex + die) % 16
                        out.add(
                            AetherAction(
                                type = AetherMoveType.DEPLOY_OR_MOVE_OUTER,
                                tokenId = tk.id,
                                diceCost = die,
                                description = "Move T${tk.id + 1} Outer ${tk.nodeIndex} → $dest ([$die])"
                            )
                        )
                        val vaultIdx = BRIDGE_OUTER_TO_VAULT[tk.nodeIndex]
                        if (vaultIdx != null && isBridgeOpen(tk.nodeIndex, state.bridgesPhaseEven) && die >= 2) {
                            // Ensure vault isn't occupied by enemy or self? If empty or enemy, can challenge; Vault shields inside!
                            val enemyInVault = state.tokens[state.turn.opponent()].orEmpty()
                                .any { it.zone == AetherZone.VAULT && it.nodeIndex == vaultIdx }
                            if (!enemyInVault) {
                                out.add(
                                    AetherAction(
                                        type = AetherMoveType.CROSS_FLUX_BRIDGE,
                                        tokenId = tk.id,
                                        diceCost = die,
                                        description = "Enter Flux Bridge → Vault V$vaultIdx ([$die])"
                                    )
                                )
                            }
                        }
                    }

                    AetherZone.VAULT -> {
                        if (die >= 3) {
                            out.add(
                                AetherAction(
                                    type = AetherMoveType.CROWN_FROM_VAULT,
                                    tokenId = tk.id,
                                    diceCost = die,
                                    description = "Ascend T${tk.id + 1} from Vault → Crowned Orbit! ([$die])"
                                )
                            )
                        }
                    }

                    AetherZone.CROWNED -> Unit
                }
            }
        }

        // Surge Move when both dice remain
        if (state.remainingDice.size == 2) {
            val total = state.remainingDice[0] + state.remainingDice[1]
            for (tk in myTokens.filter { it.zone == AetherZone.OUTER }) {
                val dest = (tk.nodeIndex + total) % 16
                out.add(
                    AetherAction(
                        type = AetherMoveType.SURGE_MOVE,
                        tokenId = tk.id,
                        diceCost = total,
                        description = "SURGE T${tk.id + 1}: Jump +$total to Node $dest & Shockwave!"
                    )
                )
            }
            // Quantum Swap when doubles
            if (state.remainingDice[0] == state.remainingDice[1]) {
                val activeMine = myTokens.filter { it.zone == AetherZone.OUTER }
                val activeOpp = state.tokens[state.turn.opponent()].orEmpty().filter { it.zone == AetherZone.OUTER }
                for (mine in activeMine) {
                    for (opp in activeOpp) {
                        out.add(
                            AetherAction(
                                type = AetherMoveType.QUANTUM_SWAP,
                                tokenId = mine.id,
                                secondTokenId = opp.id,
                                diceCost = total,
                                description = "Quantum Swap T${mine.id + 1} (${mine.nodeIndex}) ↔ Enemy T${opp.id + 1} (${opp.nodeIndex})"
                            )
                        )
                    }
                }
            }
        }

        return out
    }

    fun applyAction(state: AetherCrownState, action: AetherAction): AetherCrownState {
        if (state.phase != AetherTurnPhase.ALLOCATE_DICE) return state
        val player = state.turn
        val opponent = player.opponent()
        val myTokens = state.tokens[player].orEmpty().toMutableList()
        val oppTokens = state.tokens[opponent].orEmpty().toMutableList()
        val charges = state.charges.toMutableMap()
        var bridgePhase = state.bridgesPhaseEven
        var logEvent = action.description

        when (action.type) {
            AetherMoveType.DEPLOY_OR_MOVE_OUTER -> {
                val idx = myTokens.indexOfFirst { it.id == action.tokenId }
                if (idx < 0) return state
                val tk = myTokens[idx]
                val dest = if (tk.zone == AetherZone.DOCKED) player.homeOuterNode else (tk.nodeIndex + action.diceCost) % 16
                myTokens[idx] = tk.copy(zone = AetherZone.OUTER, nodeIndex = dest)
                if (dest !in SWITCH_NODES) {
                    // Capture enemy on outer non-switch node
                    for (i in oppTokens.indices) {
                        if (oppTokens[i].zone == AetherZone.OUTER && oppTokens[i].nodeIndex == dest) {
                            oppTokens[i] = oppTokens[i].copy(zone = AetherZone.DOCKED, nodeIndex = 0)
                            charges[player] = (charges[player] ?: 0) + 1
                            logEvent = "${player.displayName} captured Enemy T${oppTokens[i].id + 1} (+1 Charge)!"
                        }
                    }
                } else {
                    bridgePhase = !bridgePhase
                    charges[player] = (charges[player] ?: 0) + 1
                    logEvent = "${player.displayName} triggered Aether Switch ($dest): Flux Bridges flipped & +1 Charge!"
                }
            }

            AetherMoveType.CROSS_FLUX_BRIDGE -> {
                val idx = myTokens.indexOfFirst { it.id == action.tokenId }
                if (idx < 0) return state
                val tk = myTokens[idx]
                val vIdx = BRIDGE_OUTER_TO_VAULT[tk.nodeIndex] ?: return state
                myTokens[idx] = tk.copy(zone = AetherZone.VAULT, nodeIndex = vIdx)
                charges[player] = (charges[player] ?: 0) + 1
                logEvent = "${player.displayName} entered Shielded Sanctuary Vault V$vIdx (+1 Charge)!"
            }

            AetherMoveType.CROWN_FROM_VAULT -> {
                val idx = myTokens.indexOfFirst { it.id == action.tokenId }
                if (idx < 0) return state
                myTokens[idx] = myTokens[idx].copy(zone = AetherZone.CROWNED, nodeIndex = 0)
                charges[player] = (charges[player] ?: 0) + 2
                logEvent = "${player.displayName} Crowned T${myTokens[idx].id + 1}! (+2 Charges)"
            }

            AetherMoveType.SURGE_MOVE -> {
                val idx = myTokens.indexOfFirst { it.id == action.tokenId }
                if (idx < 0) return state
                val tk = myTokens[idx]
                val dest = (tk.nodeIndex + action.diceCost) % 16
                myTokens[idx] = tk.copy(zone = AetherZone.OUTER, nodeIndex = dest)
                // Shockwave pushes enemy at dest or dest+1 forward by 1 step
                for (i in oppTokens.indices) {
                    if (oppTokens[i].zone == AetherZone.OUTER && (oppTokens[i].nodeIndex == dest || oppTokens[i].nodeIndex == (dest + 1) % 16)) {
                        oppTokens[i] = oppTokens[i].copy(nodeIndex = (oppTokens[i].nodeIndex + 2) % 16)
                    }
                }
                if (dest in SWITCH_NODES) {
                    bridgePhase = !bridgePhase
                    charges[player] = (charges[player] ?: 0) + 1
                }
            }

            AetherMoveType.QUANTUM_SWAP -> {
                val mIdx = myTokens.indexOfFirst { it.id == action.tokenId }
                val oIdx = oppTokens.indexOfFirst { it.id == action.secondTokenId }
                if (mIdx < 0 || oIdx < 0) return state
                val mPos = myTokens[mIdx].nodeIndex
                val oPos = oppTokens[oIdx].nodeIndex
                myTokens[mIdx] = myTokens[mIdx].copy(nodeIndex = oPos)
                oppTokens[oIdx] = oppTokens[oIdx].copy(nodeIndex = mPos)
                bridgePhase = !bridgePhase
                logEvent = "Quantum Swap! Positions exchanged & Flux Bridges inverted!"
            }
        }

        val updatedMap = mapOf(player to myTokens, opponent to oppTokens)
        val remaining = consumeDice(state.remainingDice, action)

        // Check immediate Win Conditions
        val crownedCount = myTokens.count { it.zone == AetherZone.CROWNED }
        val holdsVault = myTokens.any { it.zone == AetherZone.VAULT }
        if (crownedCount >= 3 || (holdsVault && (charges[player] ?: 0) >= 7)) {
            val winReason = if (crownedCount >= 3) "Crowned 3 Tokens!" else "Reached 7 Aether Charges + Sanctuary Vault!"
            return state.copy(
                tokens = updatedMap,
                charges = charges,
                bridgesPhaseEven = bridgePhase,
                remainingDice = emptyList(),
                phase = AetherTurnPhase.GAME_OVER,
                winner = player,
                statusLog = "${player.displayName} WINS — $winReason"
            )
        }

        val midState = state.copy(
            tokens = updatedMap,
            charges = charges,
            bridgesPhaseEven = bridgePhase,
            remainingDice = remaining,
            statusLog = logEvent
        )

        if (remaining.isEmpty()) {
            return advanceTurn(midState, logEvent)
        }
        val nextLegal = legalActions(midState)
        if (nextLegal.isEmpty()) {
            return advanceTurn(midState, "$logEvent • Next die unusable, turn passes.")
        }
        return midState
    }

    private fun consumeDice(remaining: List<Int>, action: AetherAction): List<Int> {
        if (action.type == AetherMoveType.SURGE_MOVE || action.type == AetherMoveType.QUANTUM_SWAP) {
            return emptyList()
        }
        val copy = remaining.toMutableList()
        copy.remove(action.diceCost)
        return copy
    }

    private fun advanceTurn(state: AetherCrownState, lastLog: String): AetherCrownState {
        val nextPlayer = state.turn.opponent()
        val nextRound = if (nextPlayer == AetherPlayer.SOLAR) state.roundCount + 1 else state.roundCount
        val periodicFlip = (nextPlayer == AetherPlayer.SOLAR && nextRound % 3 == 0)
        val finalPhase = if (periodicFlip) !state.bridgesPhaseEven else state.bridgesPhaseEven
        val suffix = if (periodicFlip) " [Orbital Flip!]" else ""
        return state.copy(
            turn = nextPlayer,
            roundCount = nextRound,
            bridgesPhaseEven = finalPhase,
            remainingDice = emptyList(),
            phase = AetherTurnPhase.ROLL_DICE,
            statusLog = "$lastLog$suffix • ${nextPlayer.displayName}'s turn."
        )
    }
}

object AetherBotAi {
    fun chooseAction(state: AetherCrownState, accuracyPercent: Int, rng: Random = Random.Default): AetherAction? {
        val actions = AetherCrownEngine.legalActions(state)
        if (actions.isEmpty()) return null
        if (actions.size == 1) return actions.first()
        val acc = accuracyPercent.coerceIn(10, 100) / 100.0

        val scored = actions.map { act ->
            val next = AetherCrownEngine.applyAction(state, act)
            val eval = evaluateState(next, state.turn)
            act to eval
        }.sortedByDescending { it.second }

        return when {
            acc >= 0.85 -> if (rng.nextDouble() < acc) scored.first().first else scored[1].first
            acc >= 0.45 -> if (rng.nextDouble() < acc) scored.first().first else scored.take(3).random(rng).first
            else -> if (rng.nextDouble() < acc) scored.first().first else scored.random(rng).first
        }
    }

    private fun evaluateState(state: AetherCrownState, perspective: AetherPlayer): Double {
        if (state.winner == perspective) return 10_000.0
        if (state.winner == perspective.opponent()) return -10_000.0

        val myTokens = state.tokens[perspective].orEmpty()
        val oppTokens = state.tokens[perspective.opponent()].orEmpty()
        var score = 0.0
        score += (state.charges[perspective] ?: 0) * 18.0
        score -= (state.charges[perspective.opponent()] ?: 0) * 16.0

        for (tk in myTokens) {
            score += when (tk.zone) {
                AetherZone.DOCKED -> 0.0
                AetherZone.OUTER -> 12.0 + if (tk.nodeIndex in AetherCrownEngine.BRIDGE_OUTER_TO_VAULT &&
                    AetherCrownEngine.isBridgeOpen(tk.nodeIndex, state.bridgesPhaseEven)
                ) 14.0 else 0.0
                AetherZone.VAULT -> 45.0
                AetherZone.CROWNED -> 95.0
            }
        }
        for (tk in oppTokens) {
            score -= when (tk.zone) {
                AetherZone.DOCKED -> 0.0
                AetherZone.OUTER -> 10.0
                AetherZone.VAULT -> 40.0
                AetherZone.CROWNED -> 90.0
            }
        }
        return score
    }
}
