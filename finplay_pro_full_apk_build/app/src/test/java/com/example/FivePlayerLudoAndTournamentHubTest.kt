package com.example

import com.example.data.games.CloudTournamentDoc
import com.example.data.games.CloudTournamentEntryDoc
import com.example.data.games.FirestoreTournamentRepository
import com.example.data.games.TournamentHubMerger
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.example.domain.games.LudoBotAi
import com.example.domain.games.LudoColor
import com.example.domain.games.LudoEngine
import com.example.domain.games.LudoGameState
import com.example.domain.games.LudoPhase
import com.example.domain.games.LudoToken
import com.example.domain.games.OnlineMatchSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FivePlayerLudoAndTournamentHubTest {

    // -------------------------------------------------------------------------
    // 1. 5-Player Ludo Engine Tests
    // -------------------------------------------------------------------------

    @Test
    fun `5-player Ludo initializes 5 colors, 65-square track, and 10 safe stars`() {
        val state = LudoEngine.newGame(playerCount = 5, playVsBot = true)

        assertEquals(5, state.activeColors.size)
        assertTrue(LudoColor.PURPLE in state.activeColors)
        assertEquals(
            setOf(LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE, LudoColor.PURPLE),
            state.botColors
        )
        assertTrue(state.isFivePlayerBoard)
        assertEquals(65, state.outerTrackLength)
        assertEquals(63, state.maxOuterProgress)
        assertEquals(64, state.homeStretchStart)
        assertEquals(69, state.finishProgress)
        assertEquals(10, state.safeOuterIndices.size)
        assertTrue(52 in state.safeOuterIndices)
        assertTrue(60 in state.safeOuterIndices)
        assertEquals(5, state.tokens.size)
        assertTrue(state.tokens.values.all { it.size == 4 && it.all { t -> t.progress == -1 } })
    }

    @Test
    fun `classic 2 to 4 player Ludo preserves 52-square track and finish 56`() {
        val state4P = LudoEngine.newGame(playerCount = 4, playVsBot = false)
        assertFalse(state4P.isFivePlayerBoard)
        assertEquals(52, state4P.outerTrackLength)
        assertEquals(56, state4P.finishProgress)
        assertEquals(8, state4P.safeOuterIndices.size)
        assertTrue(state4P.botColors.isEmpty())
    }

    @Test
    fun `5-player Amethyst wrap-around outerTrackIndex and home stretch boundaries`() {
        assertEquals(52, LudoEngine.outerTrackIndex(LudoColor.PURPLE, progress = 0, isFivePlayer = true))
        assertEquals(64, LudoEngine.outerTrackIndex(LudoColor.PURPLE, progress = 12, isFivePlayer = true))
        assertEquals(0, LudoEngine.outerTrackIndex(LudoColor.PURPLE, progress = 13, isFivePlayer = true))
        assertEquals(50, LudoEngine.outerTrackIndex(LudoColor.PURPLE, progress = 63, isFivePlayer = true))
        // Progress 64 enters home stretch -> null on outer track
        assertNull(LudoEngine.outerTrackIndex(LudoColor.PURPLE, progress = 64, isFivePlayer = true))
    }

    @Test
    fun `5-arm pentagonal star board coordinates stay normalized within 0 to 1`() {
        assertEquals(65, LudoEngine.FIVE_PLAYER_OUTER_CELLS.size)
        LudoEngine.FIVE_PLAYER_OUTER_CELLS.forEach { cell ->
            assertTrue("Cell X out of bounds: ${cell.centerX}", cell.centerX in 0.01f..0.99f)
            assertTrue("Cell Y out of bounds: ${cell.centerY}", cell.centerY in 0.01f..0.99f)
        }

        val state5P = LudoEngine.newGame(5, playVsBot = false)
        LudoEngine.FIVE_PLAYER_COLORS.forEach { color ->
            val yardCoords = LudoEngine.fivePlayerYardCoords(color)
            assertEquals(4, yardCoords.size)
            yardCoords.forEach { (x, y) ->
                assertTrue("Yard X out of bounds for $color: $x", x in 0.01f..0.99f)
                assertTrue("Yard Y out of bounds for $color: $y", y in 0.01f..0.99f)
            }
            for (step in 1..5) {
                val home = LudoEngine.fivePlayerHomeCell(color, step)
                assertTrue("Home X out of bounds", home.centerX in 0.05f..0.95f)
                assertTrue("Home Y out of bounds", home.centerY in 0.05f..0.95f)
            }
            val (gx, gy) = LudoEngine.fivePlayerGoalCoord(color)
            assertTrue("Goal X out of bounds", gx in 0.35f..0.65f)
            assertTrue("Goal Y out of bounds", gy in 0.35f..0.65f)

            // Verify unified tokenNormalizedCoord across all progress phases
            for (prog in listOf(-1, 0, 25, 63, 64, 68, 69)) {
                val (nx, ny) = LudoEngine.tokenNormalizedCoord(state5P, LudoToken(color, 0, prog))
                assertTrue("Normalized X invalid at progress $prog", nx in 0.01f..0.99f)
                assertTrue("Normalized Y invalid at progress $prog", ny in 0.01f..0.99f)
            }
        }
    }

    @Test
    fun `5-player capture on unsafe square grants bonus turn and safe star protects opponent`() {
        // RED at progress 51 -> outer index 51. Rolling 2 -> progress 53 -> outer index 53 (unsafe).
        // PURPLE at progress 1 -> outer index (52 + 1) % 65 = 53.
        val base = LudoEngine.newGame(5, playVsBot = false)
        val tokensCapture = base.tokens.toMutableMap()
        tokensCapture[LudoColor.RED] = listOf(
            LudoToken(LudoColor.RED, 0, 51),
            LudoToken(LudoColor.RED, 1, -1),
            LudoToken(LudoColor.RED, 2, -1),
            LudoToken(LudoColor.RED, 3, -1)
        )
        tokensCapture[LudoColor.PURPLE] = listOf(
            LudoToken(LudoColor.PURPLE, 0, 1),
            LudoToken(LudoColor.PURPLE, 1, -1),
            LudoToken(LudoColor.PURPLE, 2, -1),
            LudoToken(LudoColor.PURPLE, 3, -1)
        )
        val afterRoll = LudoEngine.applyRoll(base.copy(tokens = tokensCapture), rolledValue = 2)
        assertEquals(LudoPhase.WAIT_MOVE, afterRoll.phase)
        val afterCapture = LudoEngine.applyMove(afterRoll, tokenIndex = 0)
        // Purple token sent back to yard (-1) & Red gets bonus turn (turnIndex stays 0)
        assertEquals(-1, afterCapture.tokens[LudoColor.PURPLE]!![0].progress)
        assertEquals(0, afterCapture.turnIndex)
        assertEquals(LudoPhase.WAIT_ROLL, afterCapture.phase)

        // Now test Safe Star protection at index 60 (PURPLE progress 8 -> (52+8)%65 = 60; RED progress 58 + dice 2 -> 60)
        val tokensSafe = base.tokens.toMutableMap()
        tokensSafe[LudoColor.RED] = listOf(
            LudoToken(LudoColor.RED, 0, 58),
            LudoToken(LudoColor.RED, 1, -1),
            LudoToken(LudoColor.RED, 2, -1),
            LudoToken(LudoColor.RED, 3, -1)
        )
        tokensSafe[LudoColor.PURPLE] = listOf(
            LudoToken(LudoColor.PURPLE, 0, 8),
            LudoToken(LudoColor.PURPLE, 1, -1),
            LudoToken(LudoColor.PURPLE, 2, -1),
            LudoToken(LudoColor.PURPLE, 3, -1)
        )
        val safeRoll = LudoEngine.applyRoll(base.copy(tokens = tokensSafe), rolledValue = 2)
        val afterSafeMove = LudoEngine.applyMove(safeRoll, tokenIndex = 0)
        // Purple stays safe at progress 8, turn advances to GREEN (turnIndex 1)
        assertEquals(8, afterSafeMove.tokens[LudoColor.PURPLE]!![0].progress)
        assertEquals(1, afterSafeMove.turnIndex)
    }

    @Test
    fun `5-player exact finish at progress 69 wins match and prevents overshoot`() {
        val base = LudoEngine.newGame(5, playVsBot = false)
        val purpleTokens = listOf(
            LudoToken(LudoColor.PURPLE, 0, 69),
            LudoToken(LudoColor.PURPLE, 1, 69),
            LudoToken(LudoColor.PURPLE, 2, 69),
            LudoToken(LudoColor.PURPLE, 3, 67)
        )
        val state = base.copy(
            turnIndex = 4, // PURPLE's turn
            tokens = base.tokens + (LudoColor.PURPLE to purpleTokens)
        )

        // Rolling 3 overshoots 67 + 3 = 70 > 69 -> no legal move -> turn wraps back to RED (0)
        val overshoot = LudoEngine.applyRoll(state, rolledValue = 3)
        assertEquals(LudoPhase.WAIT_ROLL, overshoot.phase)
        assertEquals(0, overshoot.turnIndex)

        // Rolling exact 2 -> moves 67 to 69 -> PURPLE wins!
        val exactRoll = LudoEngine.applyRoll(state, rolledValue = 2)
        assertEquals(listOf(3), LudoEngine.legalTokenIndices(exactRoll))
        val wonState = LudoEngine.applyMove(exactRoll, tokenIndex = 3)
        assertEquals(LudoPhase.GAME_OVER, wonState.phase)
        assertEquals(LudoColor.PURPLE, wonState.winner)
    }

    @Test
    fun `5-player Bot AI selects crowning token and serializer preserves PURPLE state`() {
        val base = LudoEngine.newGame(5, playVsBot = true)
        val purpleTokens = listOf(
            LudoToken(LudoColor.PURPLE, 0, 10),
            LudoToken(LudoColor.PURPLE, 1, 67), // exact finish on roll 2!
            LudoToken(LudoColor.PURPLE, 2, -1),
            LudoToken(LudoColor.PURPLE, 3, -1)
        )
        val state = base.copy(
            turnIndex = 4,
            phase = LudoPhase.WAIT_MOVE,
            diceValue = 2,
            tokens = base.tokens + (LudoColor.PURPLE to purpleTokens)
        )
        val chosen = LudoBotAi.chooseToken(state, accuracyPercent = 100, rng = Random(42))
        assertEquals(1, chosen)

        // Verify OnlineMatchSerializer roundtrip with 5P PURPLE state
        val encoded = OnlineMatchSerializer.encodeLudo(state.copy(winner = LudoColor.PURPLE, phase = LudoPhase.GAME_OVER))
        val decoded = OnlineMatchSerializer.decodeLudo(encoded)
        assertEquals(5, decoded?.activeColors?.size)
        assertTrue(LudoColor.PURPLE in decoded!!.activeColors)
        assertEquals(LudoColor.PURPLE, decoded.winner)
        assertEquals(67, decoded.tokens[LudoColor.PURPLE]!![1].progress)
    }

    // -------------------------------------------------------------------------
    // 2. Centralized Firestore Tournament Hub Merger & Category Tests
    // -------------------------------------------------------------------------

    @Test
    fun `TournamentHubMerger deduplicates mirrored cloud and local matches and sorts by status`() {
        val now = 1_800_000_000_000L
        val cloudDocs = listOf(
            CloudTournamentDoc(
                id = "tour_local_10",
                title = "Bermuda Clash Squad Pro",
                gameCategory = "FREE_FIRE",
                joinedCount = 18,
                maxSlots = 48,
                matchStartTimeMillis = now + 120_000L,
                status = "UPCOMING",
                localRoomId = 10L
            ),
            CloudTournamentDoc(
                id = "tour_cloud_ludo",
                title = "5P Star Ludo Grand Cup",
                gameCategory = "LUDO",
                joinedCount = 4,
                maxSlots = 5,
                matchStartTimeMillis = now - 60_000L,
                status = "LIVE",
                localRoomId = 0L
            )
        )
        val localEntities = listOf(
            ExternalTournamentEntity(
                id = 10L,
                gameCategory = "FREE_FIRE",
                title = "Bermuda Clash Squad Old Local",
                joinedCount = 12,
                maxSlots = 48,
                matchStartTimeMillis = now + 120_000L,
                status = "UPCOMING"
            ),
            ExternalTournamentEntity(
                id = 20L,
                gameCategory = "CHESS",
                title = "Offline Blitz Cup",
                joinedCount = 6,
                maxSlots = 16,
                matchStartTimeMillis = now - 3_600_000L,
                status = "COMPLETED"
            )
        )

        val merged = TournamentHubMerger.mergeCloudAndLocal(cloudDocs, localEntities)
        assertEquals(3, merged.size)

        // Order must be: LIVE first, then UPCOMING, then COMPLETED
        assertEquals("LIVE", merged[0].status)
        assertEquals("tour_cloud_ludo", merged[0].cloudDocId)
        assertTrue(merged[0].isLiveCloudSynced)

        assertEquals("UPCOMING", merged[1].status)
        assertEquals("tour_local_10", merged[1].cloudDocId)
        assertEquals(10L, merged[1].localId)
        assertEquals(18, merged[1].joinedCount)
        assertEquals("Bermuda Clash Squad Pro", merged[1].title)

        assertEquals("COMPLETED", merged[2].status)
        assertNull(merged[2].cloudDocId)
        assertFalse(merged[2].isLiveCloudSynced)
        assertEquals(20L, merged[2].localId)
    }

    @Test
    fun `TournamentHubMerger merges cloud and local rosters without duplicates`() {
        val cloudEntries = listOf(
            CloudTournamentEntryDoc(
                id = "uid_alpha",
                tournamentId = "tour_local_10",
                userId = "uid_alpha",
                usernameSnapshot = "Alpha",
                inGameName = "FF_Sniper",
                inGameUid = "12345678",
                joinedAtMillis = 1000L
            )
        )
        val localEntries = listOf(
            ExternalTournamentEntryEntity(
                id = 1L,
                tournamentId = 10L,
                ownerUserId = 1L,
                usernameSnapshot = "Alpha",
                inGameName = "ff_sniper", // duplicate case-insensitive
                inGameUid = "12345678",
                entryFeePaid = 50.0,
                joinedAt = 1005L
            ),
            ExternalTournamentEntryEntity(
                id = 2L,
                tournamentId = 10L,
                ownerUserId = 2L,
                usernameSnapshot = "Bravo",
                inGameName = "LudoMaster",
                inGameUid = "99887766",
                entryFeePaid = 50.0,
                joinedAt = 500L
            )
        )

        val roster = TournamentHubMerger.mergeRosterMembers(
            cloudEntries = cloudEntries,
            localEntries = localEntries,
            currentCloudUid = "uid_alpha",
            currentLocalUserId = 1L
        )
        assertEquals(2, roster.size)
        // Sorted chronologically: Bravo (500L) first, Alpha (1000L) second
        assertEquals("LudoMaster", roster[0].inGameName)
        assertFalse(roster[0].isCloudVerified)
        assertEquals("FF_Sniper", roster[1].inGameName)
        assertTrue(roster[1].isCloudVerified)
        assertTrue(roster[1].isCurrentUser)
    }

    @Test
    fun `normalizeGameCategory handles all Arena and Esports categories safely`() {
        assertEquals("FREE_FIRE", FirestoreTournamentRepository.normalizeGameCategory("free fire"))
        assertEquals("LUDO", FirestoreTournamentRepository.normalizeGameCategory("ludo"))
        assertEquals("AETHER_CROWN", FirestoreTournamentRepository.normalizeGameCategory("Aether Crown"))
        assertEquals("CONNECT_FOUR", FirestoreTournamentRepository.normalizeGameCategory("connect_four"))
        assertEquals("ESPORTS", FirestoreTournamentRepository.normalizeGameCategory("unknown_title_xyz"))
    }

    @Test
    fun `Tournament data model serializes title, entryFee, prizePool, playerUids, startTime Timestamp, and gameType cleanly`() {
        val startDate = java.util.Date(1_850_000_000_000L)
        val startTs = com.google.firebase.Timestamp(startDate)
        val tournament = com.example.data.games.Tournament(
            id = "tour_ludo_99",
            title = "5P Star Ludo Championship",
            entryFee = 50.0,
            prizePool = 225.0,
            playerUids = listOf("uid_a", "uid_b", "uid_a"),
            startTime = startTs,
            gameType = com.example.data.games.TournamentGameType.LUDO.wireValue,
            maxSlots = 5,
            status = com.example.data.games.TournamentStatus.UPCOMING.wireValue
        )

        assertEquals(com.example.data.games.TournamentGameType.LUDO, tournament.gameTypeEnum)
        assertEquals(com.example.data.games.TournamentStatus.UPCOMING, tournament.statusEnum)
        assertEquals(startDate.time, tournament.effectiveStartTimeMillis)
        assertTrue(tournament.hasPlayer("uid_a"))
        assertFalse(tournament.hasPlayer("uid_unknown"))

        // Create map must omit @DocumentId ("id"), deduplicate playerUids, include startTime Timestamp & server timestamps
        val createMap = tournament.toFirestoreCreateMap()
        assertFalse(createMap.containsKey("id"))
        assertEquals("5P Star Ludo Championship", createMap["title"])
        assertEquals(50.0, createMap["entryFee"])
        assertEquals(225.0, createMap["prizePool"])
        assertEquals(listOf("uid_a", "uid_b"), createMap["playerUids"])
        assertEquals(startTs, createMap["startTime"])
        assertEquals("LUDO", createMap["gameType"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))

        // Update map must preserve existing playerUids and never overwrite createdAt
        val updateMap = tournament.toFirestoreUpdateMap(
            existingPlayerUids = listOf("uid_a", "uid_b", "uid_c"),
            existingJoinedCount = 3
        )
        assertFalse(updateMap.containsKey("id"))
        assertFalse(updateMap.containsKey("createdAt"))
        assertEquals(listOf("uid_a", "uid_b", "uid_c"), updateMap["playerUids"])
        assertEquals(3, updateMap["joinedCount"])

        // CloudTournamentDoc roundtrip keeps playerUids and startTime
        val cloudDoc = tournament.toCloudTournamentDoc()
        assertEquals(listOf("uid_a", "uid_b", "uid_a"), cloudDoc.playerUids)
        assertEquals(startTs, cloudDoc.startTime)
        assertEquals("LUDO", cloudDoc.gameType)
    }
}
