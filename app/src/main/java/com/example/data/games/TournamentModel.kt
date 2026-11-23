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
    AETHER_CROWN("AETHER_CROWN", "Aether Crown"),
    REVERSI("REVERSI", "Reversi"),
    CONNECT_FOUR("CONNECT_FOUR", "Connect 4"),
    PUBG("PUBG", "PUBG Mobile"),
    EFOOTBALL("EFOOTBALL", "eFootball"),
    ESPORTS("ESPORTS", "Esports"),
    CARROM("CARROM", "Carrom 3D"),
    SNAKES_LADDERS("SNAKES_LADDERS", "Snakes & Ladders 3D"),
    FRUIT_SLASH("FRUIT_SLASH", "Fruit Slash 3D");

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
}
