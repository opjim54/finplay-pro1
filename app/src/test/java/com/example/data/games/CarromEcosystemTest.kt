package com.example.data.games

import com.example.i18n.AppLanguage
import com.example.ui.screens.games.ArenaGameId
import com.example.ui.screens.games.gameStringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarromEcosystemTest {

    @Test
    fun tournamentGameType_resolvesCarromFromWire() {
        val type = TournamentGameType.fromWire("CARROM")
        assertEquals(TournamentGameType.CARROM, type)
        assertEquals("CARROM", type.wireValue)
        assertEquals("Carrom 3D", type.displayName)
    }

    @Test
    fun playWalletRepository_containsCarromInAllInbuiltGameIds() {
        assertTrue(PlayWalletRepository.ALL_INBUILT_GAME_IDS.contains("CARROM"))
    }

    @Test
    fun firestoreTournamentRepository_allowsCarromCategory() {
        assertTrue(FirestoreTournamentRepository.ALLOWED_GAME_CATEGORIES.contains("CARROM"))
        assertEquals("CARROM", FirestoreTournamentRepository.normalizeGameCategory("carrom"))
        assertEquals("CARROM", FirestoreTournamentRepository.normalizeGameCategory("CARROM"))
    }

    @Test
    fun arenaGameId_hasLocalizedStringsAndRules() {
        val carromId = ArenaGameId.valueOf("CARROM")
        assertNotNull(carromId)

        val bnStrings = gameStringsFor(AppLanguage.BANGLA)
        val enStrings = gameStringsFor(AppLanguage.ENGLISH)

        assertEquals("ক্যারম বোর্ড 3D", bnStrings.gameTitle(carromId))
        assertEquals("Carrom Board 3D", enStrings.gameTitle(carromId))

        val (bnTitle, bnSections) = bnStrings.rulesFor(carromId)
        assertFalse(bnTitle.isBlank())
        assertTrue(bnSections.isNotEmpty())

        val (enTitle, enSections) = enStrings.rulesFor(carromId)
        assertFalse(enTitle.isBlank())
        assertTrue(enSections.isNotEmpty())
    }
}
