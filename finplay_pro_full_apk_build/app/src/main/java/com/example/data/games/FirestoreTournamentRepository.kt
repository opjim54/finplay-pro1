package com.example.data.games

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.data.backup.OperationType
import com.example.data.backup.handleFirestoreError
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Keep
@IgnoreExtraProperties
data class CloudTournamentDoc(
    val id: String = "",
    val title: String = "",
    val gameType: String = "FREE_FIRE",
    val gameCategory: String = gameType,
    val modeLabel: String = "SQUAD",
    val mapName: String = "Bermuda",
    val bannerKey: String = "BERMUDA",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val perKillReward: Double = 0.0,
    val prizeRanksJson: String = "[]",
    val maxSlots: Int = 48,
    val joinedCount: Int = 0,
    val playerUids: List<String> = emptyList(),
    val startTime: Timestamp? = null,
    val startTimeLabel: String = "",
    val matchStartTimeMillis: Long = startTime?.toDate()?.time ?: 0L,
    val preMatchAlertMinutes: Int = 10,
    val roomAlertSent: Boolean = false,
    val versionLabel: String = "",
    val rulesNotice: String = "",
    val adminCommissionPercent: Double = 10.0,
    val roomIdCode: String = "",
    val roomPassword: String = "",
    val status: String = "UPCOMING",
    val winnerName: String = "",
    val localRoomId: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun toTournament(): Tournament = Tournament(
        id = id,
        title = title,
        entryFee = entryFee,
        prizePool = prizePool,
        playerUids = playerUids,
        startTime = startTime ?: if (matchStartTimeMillis > 0L) Timestamp(Date(matchStartTimeMillis)) else null,
        gameType = TournamentGameType.fromWire(gameType.ifBlank { gameCategory }).wireValue,
        maxSlots = maxSlots,
        status = status,
        createdAt = if (createdAtMillis > 0L) Timestamp(Date(createdAtMillis)) else null,
        updatedAt = if (updatedAtMillis > 0L) Timestamp(Date(updatedAtMillis)) else null,
        gameCategory = TournamentGameType.fromWire(gameCategory.ifBlank { gameType }).wireValue,
        modeLabel = modeLabel,
        mapName = mapName,
        bannerKey = bannerKey,
        perKillReward = perKillReward,
        prizeRanksJson = prizeRanksJson,
        joinedCount = maxOf(joinedCount, playerUids.size),
        startTimeLabel = startTimeLabel,
        matchStartTimeMillis = startTime?.toDate()?.time ?: matchStartTimeMillis,
        preMatchAlertMinutes = preMatchAlertMinutes,
        roomAlertSent = roomAlertSent,
        versionLabel = versionLabel,
        rulesNotice = rulesNotice,
        adminCommissionPercent = adminCommissionPercent,
        roomIdCode = roomIdCode,
        roomPassword = roomPassword,
        winnerName = winnerName,
        localRoomId = localRoomId
    )
}

@Keep
@IgnoreExtraProperties
data class CloudTournamentEntryDoc(
    val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val usernameSnapshot: String = "",
    val inGameName: String = "",
    val inGameUid: String = "",
    val entryFeePaid: Double = 0.0,
    val joinedAtMillis: Long = 0L
)

/**
 * Unified presentation model merging real-time Firestore Tournament Hub documents
 * with local Room offline fallback records without duplicating mirrored matches.
 */
data class TournamentHubItem(
    val key: String,
    val cloudDocId: String?,
    val localId: Long?,
    val isLiveCloudSynced: Boolean,
    val title: String,
    val gameCategory: String,
    val modeLabel: String,
    val mapName: String,
    val bannerKey: String,
    val entryFee: Double,
    val prizePool: Double,
    val perKillReward: Double,
    val prizeRanksJson: String,
    val maxSlots: Int,
    val joinedCount: Int,
    val playerUids: List<String> = emptyList(),
    val startTime: Timestamp? = null,
    val startTimeLabel: String,
    val matchStartTimeMillis: Long,
    val preMatchAlertMinutes: Int,
    val roomAlertSent: Boolean,
    val versionLabel: String,
    val rulesNotice: String,
    val adminCommissionPercent: Double,
    val roomIdCode: String,
    val roomPassword: String,
    val status: String,
    val winnerName: String
) {
    fun toLocalEntitySnapshot(): ExternalTournamentEntity = ExternalTournamentEntity(
        id = localId ?: key.hashCode().toLong().let { if (it == 0L) 1L else kotlin.math.abs(it) },
        gameCategory = gameCategory,
        title = title,
        modeLabel = modeLabel,
        mapName = mapName,
        bannerKey = bannerKey,
        entryFee = entryFee,
        prizePool = prizePool,
        perKillReward = perKillReward,
        prizeRanksJson = prizeRanksJson,
        maxSlots = maxSlots,
        joinedCount = joinedCount,
        startTimeLabel = startTimeLabel,
        matchStartTimeMillis = matchStartTimeMillis,
        preMatchAlertMinutes = preMatchAlertMinutes,
        roomAlertSent = roomAlertSent,
        versionLabel = versionLabel,
        rulesNotice = rulesNotice,
        adminCommissionPercent = adminCommissionPercent,
        roomIdCode = roomIdCode,
        roomPassword = roomPassword,
        status = status,
        winnerName = winnerName
    )
}

data class TournamentRosterMember(
    val key: String,
    val inGameName: String,
    val inGameUid: String,
    val usernameSnapshot: String,
    val joinedAtMillis: Long,
    val isCurrentUser: Boolean,
    val isCloudVerified: Boolean
)

object TournamentHubMerger {
    fun mergeCloudAndLocal(
        cloudDocs: List<CloudTournamentDoc>,
        localEntities: List<ExternalTournamentEntity>
    ): List<TournamentHubItem> {
        val localById = localEntities.associateBy { it.id }
        val consumedLocalIds = mutableSetOf<Long>()

        val cloudItems = cloudDocs.map { doc ->
            val matchedLocal = doc.localRoomId.takeIf { it > 0L }?.let { localById[it] }
            if (matchedLocal != null) {
                consumedLocalIds.add(matchedLocal.id)
            }
            val resolvedCategory = doc.gameType.ifBlank { doc.gameCategory }
            val resolvedStartMs = doc.startTime?.toDate()?.time ?: doc.matchStartTimeMillis
            TournamentHubItem(
                key = "cloud_${doc.id}",
                cloudDocId = doc.id,
                localId = matchedLocal?.id ?: doc.localRoomId.takeIf { it > 0L },
                isLiveCloudSynced = true,
                title = doc.title,
                gameCategory = resolvedCategory,
                modeLabel = doc.modeLabel,
                mapName = doc.mapName,
                bannerKey = doc.bannerKey,
                entryFee = doc.entryFee,
                prizePool = doc.prizePool,
                perKillReward = doc.perKillReward,
                prizeRanksJson = doc.prizeRanksJson,
                maxSlots = doc.maxSlots,
                joinedCount = maxOf(doc.joinedCount, doc.playerUids.size, matchedLocal?.joinedCount ?: 0),
                playerUids = doc.playerUids,
                startTime = doc.startTime ?: if (resolvedStartMs > 0L) Timestamp(Date(resolvedStartMs)) else null,
                startTimeLabel = doc.startTimeLabel,
                matchStartTimeMillis = resolvedStartMs,
                preMatchAlertMinutes = doc.preMatchAlertMinutes,
                roomAlertSent = doc.roomAlertSent || (matchedLocal?.roomAlertSent == true),
                versionLabel = doc.versionLabel,
                rulesNotice = doc.rulesNotice,
                adminCommissionPercent = doc.adminCommissionPercent,
                roomIdCode = doc.roomIdCode.ifBlank { matchedLocal?.roomIdCode.orEmpty() },
                roomPassword = doc.roomPassword.ifBlank { matchedLocal?.roomPassword.orEmpty() },
                status = doc.status,
                winnerName = doc.winnerName.ifBlank { matchedLocal?.winnerName.orEmpty() }
            )
        }

        val localOnlyItems = localEntities
            .filter { it.id !in consumedLocalIds }
            .map { e ->
                TournamentHubItem(
                    key = "local_${e.id}",
                    cloudDocId = null,
                    localId = e.id,
                    isLiveCloudSynced = false,
                    title = e.title,
                    gameCategory = e.gameCategory,
                    modeLabel = e.modeLabel,
                    mapName = e.mapName,
                    bannerKey = e.bannerKey,
                    entryFee = e.entryFee,
                    prizePool = e.prizePool,
                    perKillReward = e.perKillReward,
                    prizeRanksJson = e.prizeRanksJson,
                    maxSlots = e.maxSlots,
                    joinedCount = e.joinedCount,
                    startTimeLabel = e.startTimeLabel,
                    matchStartTimeMillis = e.matchStartTimeMillis,
                    preMatchAlertMinutes = e.preMatchAlertMinutes,
                    roomAlertSent = e.roomAlertSent,
                    versionLabel = e.versionLabel,
                    rulesNotice = e.rulesNotice,
                    adminCommissionPercent = e.adminCommissionPercent,
                    roomIdCode = e.roomIdCode,
                    roomPassword = e.roomPassword,
                    status = e.status,
                    winnerName = e.winnerName
                )
            }

        return (cloudItems + localOnlyItems).sortedWith(
            compareBy<TournamentHubItem> {
                when (it.status) {
                    "LIVE" -> 0
                    "UPCOMING" -> 1
                    else -> 2
                }
            }.thenBy {
                if (it.matchStartTimeMillis > 0L) it.matchStartTimeMillis else Long.MAX_VALUE
            }
        )
    }

    fun mergeRosterMembers(
        cloudEntries: List<CloudTournamentEntryDoc>,
        localEntries: List<ExternalTournamentEntryEntity>,
        currentCloudUid: String?,
        currentLocalUserId: Long
    ): List<TournamentRosterMember> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<TournamentRosterMember>()

        cloudEntries.forEach { ce ->
            val dedupKey = "${ce.inGameName.trim().lowercase()}|${ce.inGameUid.trim().lowercase()}"
            seenKeys.add(dedupKey)
            result.add(
                TournamentRosterMember(
                    key = "cloud_${ce.id}",
                    inGameName = ce.inGameName,
                    inGameUid = ce.inGameUid,
                    usernameSnapshot = ce.usernameSnapshot,
                    joinedAtMillis = ce.joinedAtMillis,
                    isCurrentUser = !currentCloudUid.isNullOrBlank() && ce.userId == currentCloudUid,
                    isCloudVerified = true
                )
            )
        }

        localEntries.forEach { le ->
            val dedupKey = "${le.inGameName.trim().lowercase()}|${le.inGameUid.trim().lowercase()}"
            if (dedupKey !in seenKeys) {
                seenKeys.add(dedupKey)
                result.add(
                    TournamentRosterMember(
                        key = "local_${le.id}",
                        inGameName = le.inGameName,
                        inGameUid = le.inGameUid,
                        usernameSnapshot = le.usernameSnapshot,
                        joinedAtMillis = le.joinedAt,
                        isCurrentUser = currentLocalUserId > 0L && le.ownerUserId == currentLocalUserId,
                        isCloudVerified = false
                    )
                )
            }
        }

        return result.sortedBy { it.joinedAtMillis }
    }
}

class FirestoreTournamentRepository(
    private val context: Context,
    private val firestoreProvider: () -> FirebaseFirestore = {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }
) {
    companion object {
        const val COLLECTION_TOURNAMENTS = "tournaments"
        const val SUBCOLLECTION_ENTRIES = "entries"

        val ALLOWED_GAME_CATEGORIES = setOf(
            "FREE_FIRE", "LUDO", "CHESS", "AETHER_CROWN", "REVERSI", "CONNECT_FOUR", "PUBG", "EFOOTBALL", "ESPORTS", "CARROM", "SNAKES_LADDERS", "FRUIT_SLASH"
        )

        fun stableDocIdForLocal(localTournamentId: Long): String = "tour_local_${localTournamentId.coerceAtLeast(1L)}"

        fun normalizeGameCategory(raw: String): String {
            val upper = raw.trim().uppercase().replace(" ", "_")
            return if (upper in ALLOWED_GAME_CATEGORIES) upper else "ESPORTS"
        }
    }

    fun currentUserUid(): String? = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()

    private fun parseTournamentSnapshot(doc: DocumentSnapshot): CloudTournamentDoc? {
        return Tournament.fromSnapshot(doc)?.toCloudTournamentDoc()
    }

    private fun parseEntrySnapshot(doc: DocumentSnapshot): CloudTournamentEntryDoc? {
        if (!doc.exists()) return null
        val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return CloudTournamentEntryDoc(
            id = doc.id,
            tournamentId = doc.getString("tournamentId") ?: "",
            userId = doc.getString("userId") ?: doc.id,
            usernameSnapshot = doc.getString("usernameSnapshot") ?: "",
            inGameName = doc.getString("inGameName") ?: "",
            inGameUid = doc.getString("inGameUid") ?: "",
            entryFeePaid = (doc.get("entryFeePaid") as? Number)?.toDouble() ?: 0.0,
            joinedAtMillis = doc.getTimestamp("joinedAt", est)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    /**
     * Real-time Firestore listener for all Centralized Tournament Hub matches as [CloudTournamentDoc].
     */
    fun observeTournaments(): Flow<List<CloudTournamentDoc>> {
        return observeTournamentModels().map { list -> list.map { it.toCloudTournamentDoc() } }
    }

    /**
     * Real-time Firestore listener returning canonical [Tournament] data models ordered by `startTime`.
     */
    fun observeTournamentModels(): Flow<List<Tournament>> {
        if (currentUserUid() == null) return flowOf(emptyList())
        return firestoreProvider()
            .collection(COLLECTION_TOURNAMENTS)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .limit(60)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull(Tournament::fromSnapshot) }
            .catch { err ->
                val firestoreErr = generateSequence<Throwable>(err) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (err is CancellationException && firestoreErr == null) throw err
                (firestoreErr ?: err as? Exception)?.let {
                    handleFirestoreError(it, OperationType.LIST, COLLECTION_TOURNAMENTS)
                }
                emit(emptyList())
            }
    }

    /**
     * Real-time Firestore listener for tournaments joined by a specific player UID via `playerUids`.
     */
    fun observeMyTournaments(uid: String): Flow<List<Tournament>> {
        val cleanUid = uid.trim()
        if (currentUserUid() == null || cleanUid.isEmpty()) return flowOf(emptyList())
        return firestoreProvider()
            .collection(COLLECTION_TOURNAMENTS)
            .whereArrayContains("playerUids", cleanUid)
            .limit(60)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull(Tournament::fromSnapshot)
                    .sortedBy { it.effectiveStartTimeMillis }
            }
            .catch { err ->
                val firestoreErr = generateSequence<Throwable>(err) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (err is CancellationException && firestoreErr == null) throw err
                (firestoreErr ?: err as? Exception)?.let {
                    handleFirestoreError(it, OperationType.LIST, COLLECTION_TOURNAMENTS)
                }
                emit(emptyList())
            }
    }

    /**
     * Creates a canonical [Tournament] document directly in `/tournaments/{docId}` (Admin only).
     */
    suspend fun createTournament(
        tournament: Tournament,
        explicitDocId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            currentUserUid() ?: return@withContext Result.failure(
                IllegalStateException("Sign in with Admin Google account to create tournaments")
            )
            val col = firestoreProvider().collection(COLLECTION_TOURNAMENTS)
            val docRef = explicitDocId?.takeIf { it.isNotBlank() }?.let { col.document(it) } ?: col.document()
            docRef.set(tournament.toFirestoreCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$COLLECTION_TOURNAMENTS/${explicitDocId ?: "auto"}")
            Result.failure(e)
        }
    }

    /**
     * Real-time Firestore listener for the joined players roster of a specific tournament.
     */
    fun observeTournamentEntries(tournamentDocId: String): Flow<List<CloudTournamentEntryDoc>> {
        if (currentUserUid() == null || tournamentDocId.isBlank()) return flowOf(emptyList())
        val path = "$COLLECTION_TOURNAMENTS/$tournamentDocId/$SUBCOLLECTION_ENTRIES"
        return firestoreProvider()
            .collection(COLLECTION_TOURNAMENTS)
            .document(tournamentDocId)
            .collection(SUBCOLLECTION_ENTRIES)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .limit(200)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull(::parseEntrySnapshot) }
            .catch { err ->
                val firestoreErr = generateSequence<Throwable>(err) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (err is CancellationException && firestoreErr == null) throw err
                (firestoreErr ?: err as? Exception)?.let {
                    handleFirestoreError(it, OperationType.LIST, path)
                }
                emit(emptyList())
            }
    }

    /**
     * Admin create/update mirror of a tournament into Cloud Firestore `/tournaments/{docId}`.
     * Preserves immutable `createdAt` on updates.
     */
    suspend fun upsertCloudTournament(
        entity: ExternalTournamentEntity,
        explicitDocId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            currentUserUid() ?: return@withContext Result.failure(
                IllegalStateException("Sign in with Admin Google account to sync tournaments live")
            )
            val docId = explicitDocId?.takeIf { it.isNotBlank() } ?: stableDocIdForLocal(entity.id)
            val docRef = firestoreProvider().collection(COLLECTION_TOURNAMENTS).document(docId)
            val existing = docRef.get().await()

            val normalizedGameType = normalizeGameCategory(entity.gameCategory)
            val resolvedStartMs = entity.matchStartTimeMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
            val model = Tournament(
                id = docId,
                title = entity.title,
                entryFee = entity.entryFee,
                prizePool = entity.prizePool,
                playerUids = emptyList(),
                startTime = Timestamp(Date(resolvedStartMs)),
                gameType = normalizedGameType,
                maxSlots = entity.maxSlots,
                status = entity.status,
                gameCategory = normalizedGameType,
                modeLabel = entity.modeLabel,
                mapName = entity.mapName,
                bannerKey = entity.bannerKey,
                perKillReward = entity.perKillReward,
                prizeRanksJson = entity.prizeRanksJson,
                joinedCount = entity.joinedCount,
                startTimeLabel = entity.startTimeLabel,
                matchStartTimeMillis = resolvedStartMs,
                preMatchAlertMinutes = entity.preMatchAlertMinutes,
                roomAlertSent = entity.roomAlertSent,
                versionLabel = entity.versionLabel,
                rulesNotice = entity.rulesNotice,
                adminCommissionPercent = entity.adminCommissionPercent,
                roomIdCode = entity.roomIdCode,
                roomPassword = entity.roomPassword,
                winnerName = entity.winnerName,
                localRoomId = entity.id
            )

            if (existing.exists()) {
                val existingJoined = (existing.get("joinedCount") as? Number)?.toInt() ?: 0
                val existingUids = (existing.get("playerUids") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
                docRef.update(model.toFirestoreUpdateMap(existingUids, existingJoined)).await()
            } else {
                docRef.set(model.toFirestoreCreateMap()).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_TOURNAMENTS/${explicitDocId ?: entity.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteCloudTournament(docId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            currentUserUid() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))
            firestoreProvider().collection(COLLECTION_TOURNAMENTS).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$COLLECTION_TOURNAMENTS/$docId")
            Result.failure(e)
        }
    }

    /**
     * Atomic player registration matching zero-trust `firestore.rules`:
     * Creates `/tournaments/{id}/entries/{uid}` and increments parent `joinedCount` by +1 in one batch.
     */
    suspend fun joinCloudTournamentAtomic(
        tournamentDocId: String,
        usernameSnapshot: String,
        inGameName: String,
        inGameUid: String,
        entryFeePaid: Double
    ): Result<CloudTournamentEntryDoc> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserUid() ?: return@withContext Result.failure(
                IllegalStateException("Please sign in with Google to join live cloud tournaments")
            )
            val cleanIgn = inGameName.trim().take(64)
            val cleanUid = inGameUid.trim().take(64)
            require(cleanIgn.isNotEmpty()) { "In-Game Name is required" }
            require(cleanUid.isNotEmpty()) { "Player ID / UID is required" }

            val db = firestoreProvider()
            val tournamentRef = db.collection(COLLECTION_TOURNAMENTS).document(tournamentDocId)
            val entryRef = tournamentRef.collection(SUBCOLLECTION_ENTRIES).document(uid)

            val existingEntry = entryRef.get().await()
            if (existingEntry.exists()) {
                return@withContext Result.failure(IllegalStateException("You have already joined this tournament"))
            }

            val entryPayload = mapOf(
                "tournamentId" to tournamentDocId,
                "userId" to uid,
                "usernameSnapshot" to usernameSnapshot.trim().take(64),
                "inGameName" to cleanIgn,
                "inGameUid" to cleanUid,
                "entryFeePaid" to entryFeePaid.coerceIn(0.0, 100_000.0),
                "joinedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(entryRef, entryPayload)
            batch.update(
                tournamentRef,
                mapOf(
                    "playerUids" to FieldValue.arrayUnion(uid),
                    "joinedCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(
                CloudTournamentEntryDoc(
                    id = uid,
                    tournamentId = tournamentDocId,
                    userId = uid,
                    usernameSnapshot = usernameSnapshot.trim().take(64),
                    inGameName = cleanIgn,
                    inGameUid = cleanUid,
                    entryFeePaid = entryFeePaid.coerceIn(0.0, 100_000.0),
                    joinedAtMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_TOURNAMENTS/$tournamentDocId")
            Result.failure(e)
        }
    }
}
