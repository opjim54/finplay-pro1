package com.example.data.games

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

/**
 * Strongly-typed game types supported by the Firestore `tournaments` collection.
 */
@Keep
enum class TournamentGameType(val wireValue: String, val displayName: String) {
    FREE_FIRE("FREE_FIRE", "Free Fire"),
    LUDO("LUDO", "Ludo"),
    CHESS("CHESS", "Chess"),
    PUBG("PUBG", "PUBG Mobile"),
    EFOOTBALL("EFOOTBALL", "eFootball"),
    ESPORTS("ESPORTS", "Esports"),
    CARROM("CARROM", "Carrom 3D"),
    SHOLO_GUTI("SHOLO_GUTI", "Sholo Guti (16 Beads)"),
    SNAKES_LADDERS("SNAKES_LADDERS", "Snakes & Ladders 3D"),
    FRUIT_SLASH("FRUIT_SLASH", "Fruit Slash 3D"),
    WAR_LAND("WAR_LAND", "WarLand 3D");

    companion object {
        val ALLOWED_WIRE_VALUES: Set<String> = entries.map { it.wireValue }.toSet()

        fun fromWire(raw: String?): TournamentGameType {
            val normalized = raw?.trim()?.uppercase()?.replace(" ", "_")?.replace("-", "_") ?: return ESPORTS
            return entries.firstOrNull { it.wireValue == normalized || it.name == normalized } ?: ESPORTS
        }
    }
}

/**
 * Lifecycle status for a Firestore [Tournament] document.
 */
@Keep
enum class TournamentStatus(val wireValue: String) {
    UPCOMING("UPCOMING"),
    LIVE("LIVE"),
    COMPLETED("COMPLETED");

    companion object {
        fun fromWire(raw: String?): TournamentStatus {
            val normalized = raw?.trim()?.uppercase() ?: return UPCOMING
            return entries.firstOrNull { it.wireValue == normalized } ?: UPCOMING
        }
    }
}

/**
 * Canonical Firestore data model for the `/tournaments/{tournamentId}` collection.
 *
 * Core fields (1:1 aligned with `firebase-blueprint.json` and `firestore.rules`):
 * - [title]: Tournament display title
 * - [entryFee]: Entry fee in Tournament Coins
 * - [prizePool]: Total prize pool in Winning Coins
 * - [playerUids]: Ordered list of registered player Firebase Auth UIDs
 * - [startTime]: Native Firestore [Timestamp] when the tournament begins
 * - [gameType]: Game type wire value (e.g. `LUDO`, `FREE_FIRE`, `CHESS`)
 *
 * Extended fields retain full compatibility with the FinPlay Pro Live Tournament Hub
 * (multi-rank prizes, Room ID/Password vault, map banners, and local Room mirror ID).
 */
@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val playerUids: List<String> = emptyList(),
    val startTime: Timestamp? = null,
    val gameType: String = TournamentGameType.FREE_FIRE.wireValue,
    val maxSlots: Int = 48,
    val status: String = TournamentStatus.UPCOMING.wireValue,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    // Extended Arena / Esports Hub metadata
    val gameCategory: String = gameType,
    val modeLabel: String = "SQUAD",
    val mapName: String = "Bermuda",
    val bannerKey: String = "BERMUDA",
    val perKillReward: Double = 0.0,
    val prizeRanksJson: String = "[]",
    val joinedCount: Int = playerUids.size,
    val startTimeLabel: String = "",
    val matchStartTimeMillis: Long = startTime?.toDate()?.time ?: 0L,
    val preMatchAlertMinutes: Int = 10,
    val roomAlertSent: Boolean = false,
    val versionLabel: String = "",
    val rulesNotice: String = "",
    val adminCommissionPercent: Double = 10.0,
    val roomIdCode: String = "",
    val roomPassword: String = "",
    val winnerName: String = "",
    val localRoomId: Long = 0L
) {
    @get:Exclude
    val gameTypeEnum: TournamentGameType
        get() = TournamentGameType.fromWire(gameType.ifBlank { gameCategory })

    @get:Exclude
    val statusEnum: TournamentStatus
        get() = TournamentStatus.fromWire(status)

    @get:Exclude
    val effectiveStartTimeMillis: Long
        get() = startTime?.toDate()?.time ?: matchStartTimeMillis

    @get:Exclude
    val effectiveJoinedCount: Int
        get() = maxOf(joinedCount, playerUids.size)

    @get:Exclude
    val availableSlots: Int
        get() = (maxSlots - effectiveJoinedCount).coerceAtLeast(0)

    fun hasPlayer(uid: String?): Boolean = !uid.isNullOrBlank() && uid in playerUids

    /**
     * Builds a zero-trust, rules-compliant Firestore payload for creating a new `/tournaments/{id}` doc.
     * Omits `id` (`@DocumentId`) and uses `FieldValue.serverTimestamp()` for `createdAt`/`updatedAt`.
     */
    fun toFirestoreCreateMap(): Map<String, Any> {
        val map = toBaseFirestoreFields(
            resolvedPlayerUids = playerUids.distinct().take(maxSlots.coerceIn(2, 500)),
            includeExtendedHubFields = true
        )
        map["createdAt"] = FieldValue.serverTimestamp()
        map["updatedAt"] = FieldValue.serverTimestamp()
        return map
    }

    /**
     * Builds a zero-trust, rules-compliant Firestore payload for updating an existing tournament doc.
     * Preserves immutable `createdAt` (never overwritten) and merges existing [existingPlayerUids].
     */
    fun toFirestoreUpdateMap(
        existingPlayerUids: List<String> = playerUids,
        existingJoinedCount: Int = joinedCount
    ): Map<String, Any> {
        val clampedMax = maxSlots.coerceIn(2, 500)
        val preservedUids = existingPlayerUids.distinct().take(clampedMax)
        val map = toBaseFirestoreFields(
            resolvedPlayerUids = preservedUids,
            includeExtendedHubFields = true
        )
        map["joinedCount"] = maxOf(joinedCount, existingJoinedCount, preservedUids.size).coerceIn(0, clampedMax)
        map["updatedAt"] = FieldValue.serverTimestamp()
        return map
    }

    private fun toBaseFirestoreFields(
        resolvedPlayerUids: List<String>,
        includeExtendedHubFields: Boolean
    ): MutableMap<String, Any> {
        val clampedMax = maxSlots.coerceIn(2, 500)
        val normalizedGameType = TournamentGameType.fromWire(gameType.ifBlank { gameCategory }).wireValue
        val normalizedStatus = TournamentStatus.fromWire(status).wireValue
        val resolvedStartTs = startTime ?: Timestamp(Date(matchStartTimeMillis.coerceAtLeast(0L)))
        val resolvedStartMs = resolvedStartTs.toDate().time.coerceAtLeast(0L)

        val fields = mutableMapOf<String, Any>(
            "title" to title.trim().ifBlank { "FinPlay Tournament" }.take(120),
            "gameType" to normalizedGameType,
            "entryFee" to entryFee.coerceIn(0.0, 100_000.0),
            "prizePool" to prizePool.coerceIn(0.0, 10_000_000.0),
            "playerUids" to resolvedPlayerUids,
            "startTime" to resolvedStartTs,
            "maxSlots" to clampedMax,
            "status" to normalizedStatus
        )
        if (includeExtendedHubFields) {
            fields["gameCategory"] = normalizedGameType
            fields["modeLabel"] = modeLabel.trim().ifBlank { "SQUAD" }.take(48)
            fields["mapName"] = mapName.trim().ifBlank { "Arena" }.take(64)
            fields["bannerKey"] = bannerKey.trim().ifBlank { "BERMUDA" }.take(48)
            fields["perKillReward"] = perKillReward.coerceIn(0.0, 100_000.0)
            fields["prizeRanksJson"] = prizeRanksJson.take(4000)
            fields["joinedCount"] = maxOf(joinedCount, resolvedPlayerUids.size).coerceIn(0, clampedMax)
            fields["startTimeLabel"] = startTimeLabel.take(80)
            fields["matchStartTimeMillis"] = resolvedStartMs
            fields["preMatchAlertMinutes"] = preMatchAlertMinutes.coerceIn(1, 180)
            fields["roomAlertSent"] = roomAlertSent
            fields["versionLabel"] = versionLabel.take(80)
            fields["rulesNotice"] = rulesNotice.take(600)
            fields["adminCommissionPercent"] = adminCommissionPercent.coerceIn(0.0, 100.0)
            fields["roomIdCode"] = roomIdCode.take(64)
            fields["roomPassword"] = roomPassword.take(64)
            fields["winnerName"] = winnerName.take(80)
            fields["localRoomId"] = localRoomId.coerceAtLeast(0L)
        }
        return fields
    }

    fun toCloudTournamentDoc(): CloudTournamentDoc {
        val resolvedStartMs = effectiveStartTimeMillis
        val resolvedGameType = gameTypeEnum.wireValue
        return CloudTournamentDoc(
            id = id,
            title = title,
            gameType = resolvedGameType,
            gameCategory = resolvedGameType,
            modeLabel = modeLabel,
            mapName = mapName,
            bannerKey = bannerKey,
            entryFee = entryFee,
            prizePool = prizePool,
            perKillReward = perKillReward,
            prizeRanksJson = prizeRanksJson,
            maxSlots = maxSlots,
            joinedCount = effectiveJoinedCount,
            playerUids = playerUids,
            startTime = startTime ?: if (resolvedStartMs > 0L) Timestamp(Date(resolvedStartMs)) else null,
            startTimeLabel = startTimeLabel,
            matchStartTimeMillis = resolvedStartMs,
            preMatchAlertMinutes = preMatchAlertMinutes,
            roomAlertSent = roomAlertSent,
            versionLabel = versionLabel,
            rulesNotice = rulesNotice,
            adminCommissionPercent = adminCommissionPercent,
            roomIdCode = roomIdCode,
            roomPassword = roomPassword,
            status = statusEnum.wireValue,
            winnerName = winnerName,
            localRoomId = localRoomId,
            createdAtMillis = createdAt?.toDate()?.time ?: 0L,
            updatedAtMillis = updatedAt?.toDate()?.time ?: 0L
        )
    }

    companion object {
        /**
         * Safely deserializes a Firestore [DocumentSnapshot] into a [Tournament] using
         * `ServerTimestampBehavior.ESTIMATE` and defensive numeric/list coercion.
         */
        fun fromSnapshot(doc: DocumentSnapshot): Tournament? {
            if (!doc.exists()) return null
            val title = doc.getString("title") ?: return null
            val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            val rawGameType = doc.getString("gameType") ?: doc.getString("gameCategory") ?: "FREE_FIRE"
            val normalizedGameType = TournamentGameType.fromWire(rawGameType).wireValue
            val rawUids = (doc.get("playerUids") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: emptyList()
            val startTs = doc.getTimestamp("startTime")
            val rawStartMs = (doc.get("matchStartTimeMillis") as? Number)?.toLong() ?: 0L
            val resolvedStartTs = startTs ?: if (rawStartMs > 0L) Timestamp(Date(rawStartMs)) else null
            val resolvedStartMs = startTs?.toDate()?.time ?: rawStartMs
            val maxSlots = (doc.get("maxSlots") as? Number)?.toInt()?.coerceIn(2, 500) ?: 48
            val joinedCount = maxOf((doc.get("joinedCount") as? Number)?.toInt() ?: 0, rawUids.size)

            return Tournament(
                id = doc.id,
                title = title,
                entryFee = (doc.get("entryFee") as? Number)?.toDouble() ?: 0.0,
                prizePool = (doc.get("prizePool") as? Number)?.toDouble() ?: 0.0,
                playerUids = rawUids,
                startTime = resolvedStartTs,
                gameType = normalizedGameType,
                maxSlots = maxSlots,
                status = TournamentStatus.fromWire(doc.getString("status")).wireValue,
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est),
                gameCategory = normalizedGameType,
                modeLabel = doc.getString("modeLabel") ?: "SQUAD",
                mapName = doc.getString("mapName") ?: "Arena",
                bannerKey = doc.getString("bannerKey") ?: "BERMUDA",
                perKillReward = (doc.get("perKillReward") as? Number)?.toDouble() ?: 0.0,
                prizeRanksJson = doc.getString("prizeRanksJson") ?: "[]",
                joinedCount = joinedCount,
                startTimeLabel = doc.getString("startTimeLabel") ?: "",
                matchStartTimeMillis = resolvedStartMs,
                preMatchAlertMinutes = (doc.get("preMatchAlertMinutes") as? Number)?.toInt() ?: 10,
                roomAlertSent = doc.getBoolean("roomAlertSent") ?: false,
                versionLabel = doc.getString("versionLabel") ?: "",
                rulesNotice = doc.getString("rulesNotice") ?: "",
                adminCommissionPercent = (doc.get("adminCommissionPercent") as? Number)?.toDouble() ?: 10.0,
                roomIdCode = doc.getString("roomIdCode") ?: "",
                roomPassword = doc.getString("roomPassword") ?: "",
                winnerName = doc.getString("winnerName") ?: "",
                localRoomId = (doc.get("localRoomId") as? Number)?.toLong() ?: 0L
            )
        }
    }

    /**
     * Calculates entry fee vs. potential earnings, net profit, and ROI for a participant.
     */
    fun calculatePotentialEarnings(expectedKills: Int = 0): TournamentEarningsEstimate =
        TournamentEarningsCalculator.calculateEntryVsPotentialEarnings(
            entryFee = entryFee,
            prizePool = prizePool,
            maxSlots = maxSlots,
            adminCommissionPercent = adminCommissionPercent,
            perKillReward = perKillReward,
            prizeRanksJson = prizeRanksJson,
            expectedKills = expectedKills
        )
}

/**
 * Breakdown of a tournament participant's entry fee against potential earnings and ROI.
 */
@Keep
data class TournamentEarningsEstimate(
    val entryFee: Double,
    val grossPrizePool: Double,
    val platformCommissionAmount: Double,
    val netDistributablePool: Double,
    val firstPlacePrize: Double,
    val secondPlacePrize: Double,
    val thirdPlacePrize: Double,
    val killBonusEstimate: Double,
    val totalPotentialFirstPlaceEarnings: Double,
    val potentialNetProfit: Double,
    val roiPercent: Double,
    val returnMultiplier: Double,
    val breakEvenKills: Int,
    val isFreeEntry: Boolean
) {
    val isPositiveReturn: Boolean
        get() = potentialNetProfit > 0.0 || (isFreeEntry && totalPotentialFirstPlaceEarnings > 0.0)
}

/**
 * Helper utility calculating tournament entry fees against potential participant earnings,
 * supporting both explicit `prizePool` values, rank payout JSON (`prizeRanksJson`),
 * per-kill rewards, and dynamic gross-pool fallback when prizePool is auto-derived.
 */
object TournamentEarningsCalculator {

    fun calculateEntryVsPotentialEarnings(
        entryFee: Double,
        prizePool: Double,
        maxSlots: Int = 48,
        adminCommissionPercent: Double = 10.0,
        perKillReward: Double = 0.0,
        prizeRanksJson: String = "[]",
        expectedKills: Int = 0
    ): TournamentEarningsEstimate {
        val safeEntryFee = entryFee.coerceAtLeast(0.0)
        val safeSlots = maxSlots.coerceIn(2, 500)
        val safeCommissionPct = adminCommissionPercent.coerceIn(0.0, 100.0)
        val safePerKill = perKillReward.coerceAtLeast(0.0)
        val safeKills = expectedKills.coerceAtLeast(0)

        val projectedGrossPool = safeEntryFee * safeSlots
        val explicitPool = prizePool.coerceAtLeast(0.0)
        val grossPool = if (explicitPool > 0.0) explicitPool else projectedGrossPool
        val commissionAmount = if (explicitPool > 0.0) {
            (projectedGrossPool * (safeCommissionPct / 100.0)).coerceAtLeast(0.0)
        } else {
            grossPool * (safeCommissionPct / 100.0)
        }
        val netPool = if (explicitPool > 0.0) {
            explicitPool
        } else {
            (grossPool - commissionAmount).coerceAtLeast(0.0)
        }

        val parsedRanks = com.example.domain.games.TournamentPrizeCodec.parseRanks(prizeRanksJson)
        val firstPrize: Double
        val secondPrize: Double
        val thirdPrize: Double
        if (parsedRanks.isNotEmpty()) {
            firstPrize = parsedRanks.getOrNull(0)?.amount?.coerceAtLeast(0.0) ?: netPool
            secondPrize = parsedRanks.getOrNull(1)?.amount?.coerceAtLeast(0.0) ?: 0.0
            thirdPrize = parsedRanks.getOrNull(2)?.amount?.coerceAtLeast(0.0) ?: 0.0
        } else if (safeSlots <= 2) {
            firstPrize = netPool
            secondPrize = 0.0
            thirdPrize = 0.0
        } else if (safeSlots <= 5) {
            firstPrize = netPool * 0.65
            secondPrize = netPool * 0.35
            thirdPrize = 0.0
        } else {
            firstPrize = netPool * 0.50
            secondPrize = netPool * 0.30
            thirdPrize = netPool * 0.20
        }

        val killBonus = safePerKill * safeKills
        val totalPotentialFirst = firstPrize + killBonus
        val netProfit = totalPotentialFirst - safeEntryFee
        val isFree = safeEntryFee <= 0.0
        val roiPct = if (isFree) {
            if (totalPotentialFirst > 0.0) 100.0 else 0.0
        } else {
            (netProfit / safeEntryFee) * 100.0
        }
        val multiplier = if (isFree) {
            if (totalPotentialFirst > 0.0) totalPotentialFirst else 0.0
        } else {
            totalPotentialFirst / safeEntryFee
        }
        val breakEvenKills = if (!isFree && safePerKill > 0.0) {
            kotlin.math.ceil(safeEntryFee / safePerKill).toInt().coerceAtLeast(1)
        } else {
            0
        }

        return TournamentEarningsEstimate(
            entryFee = safeEntryFee,
            grossPrizePool = grossPool,
            platformCommissionAmount = commissionAmount,
            netDistributablePool = netPool,
            firstPlacePrize = firstPrize,
            secondPlacePrize = secondPrize,
            thirdPlacePrize = thirdPrize,
            killBonusEstimate = killBonus,
            totalPotentialFirstPlaceEarnings = totalPotentialFirst,
            potentialNetProfit = netProfit,
            roiPercent = roiPct,
            returnMultiplier = multiplier,
            breakEvenKills = breakEvenKills,
            isFreeEntry = isFree
        )
    }
}

/**
 * Real-time Firestore Tournament Leaderboard entry stored at `/tournament_leaderboard/{userId}`.
 * Queried via `orderBy("matchPoints", Query.Direction.DESCENDING).limit(10)` for the Top 10 rankings.
 */
@Keep
@IgnoreExtraProperties
data class TournamentLeaderboardEntry(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarId: String = "avatar_monarch",
    val matchPoints: Long = 0L,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val totalCoinsEarned: Double = 0.0,
    val favoriteGame: String = "WAR_LAND",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val winRatePercent: Int
        get() = if (matchesPlayed <= 0) 0 else ((wins * 100.0) / matchesPlayed).toInt().coerceIn(0, 100)

    fun toFirestoreCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId.trim().take(128),
        "displayName" to displayName.trim().ifBlank { "FinPlay Warrior" }.take(64),
        "avatarId" to avatarId.trim().ifBlank { "avatar_monarch" }.take(64),
        "matchPoints" to matchPoints.coerceIn(0L, 5000L),
        "matchesPlayed" to matchesPlayed.coerceIn(0, 5),
        "wins" to wins.coerceIn(0, matchesPlayed.coerceIn(0, 5)),
        "totalCoinsEarned" to totalCoinsEarned.coerceIn(0.0, 100_000.0),
        "favoriteGame" to favoriteGame.trim().ifBlank { "WAR_LAND" }.take(48),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toFirestoreUpdateMap(): Map<String, Any> {
        val safeMatches = matchesPlayed.coerceIn(0, 100_000)
        return mapOf(
            "displayName" to displayName.trim().ifBlank { "FinPlay Warrior" }.take(64),
            "avatarId" to avatarId.trim().ifBlank { "avatar_monarch" }.take(64),
            "matchPoints" to matchPoints.coerceIn(0L, 10_000_000L),
            "matchesPlayed" to safeMatches,
            "wins" to wins.coerceIn(0, safeMatches),
            "totalCoinsEarned" to totalCoinsEarned.coerceIn(0.0, 100_000_000.0),
            "favoriteGame" to favoriteGame.trim().ifBlank { "WAR_LAND" }.take(48),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): TournamentLeaderboardEntry? {
            if (!doc.exists()) return null
            val uid = doc.getString("userId")?.takeIf { it.isNotBlank() } ?: doc.id
            val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            return TournamentLeaderboardEntry(
                id = doc.id,
                userId = uid,
                displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: "Player",
                avatarId = doc.getString("avatarId")?.takeIf { it.isNotBlank() } ?: "avatar_monarch",
                matchPoints = (doc.get("matchPoints") as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
                matchesPlayed = (doc.get("matchesPlayed") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                wins = (doc.get("wins") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                totalCoinsEarned = (doc.get("totalCoinsEarned") as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0,
                favoriteGame = doc.getString("favoriteGame")?.takeIf { it.isNotBlank() } ?: "WAR_LAND",
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }
    }
}

