package com.example.data.games

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.data.backup.OperationType
import com.example.data.backup.handleFirestoreError
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletRequestEntity
import com.example.domain.games.OnlineMatchSerializer
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom

@Keep
@IgnoreExtraProperties
data class MultiplayerRoomDoc(
    val roomCode: String = "",
    val gameType: String = "LUDO",
    val hostUid: String = "",
    val hostName: String = "",
    val guestUid: String = "",
    val guestName: String = "",
    val memberUids: List<String> = emptyList(),
    val publicRoom: Boolean = false,
    val entryFee: Double = 0.0,
    val status: String = "WAITING",
    val turnUid: String = "",
    val winnerUid: String = "",
    val statePayload: String = "{}",
    val moveCount: Int = 0,
    val lastMoveSummary: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudWalletSummaryDoc(
    val userId: String = "",
    val tournamentBalance: Double = 0.0,
    val winningBalance: Double = 0.0,
    val gameBalance: Double = 0.0,
    val equippedBoardSkinId: String = "skin_royal_emerald",
    val equippedAvatarId: String = "avatar_monarch",
    val ownedItemsCsv: String = "",
    val updatedAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class CloudWalletLedgerDoc(
    val id: String = "",
    val userId: String = "",
    val walletBucket: String = "TOURNAMENT",
    val kind: String = "",
    val amountDelta: Double = 0.0,
    val note: String = "",
    val createdAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class CloudBroadcastDoc(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val priority: String = "INFO",
    val senderEmail: String = "",
    val createdAtMillis: Long = 0L
)

class FirestoreMultiplayerRepository(
    private val context: Context,
    private val firestoreProvider: () -> FirebaseFirestore = {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }
) {
    companion object {
        private const val COLLECTION_ROOMS = "multiplayer_rooms"
        private const val COLLECTION_WALLET_REQUESTS = "wallet_requests"
        private const val COLLECTION_BROADCASTS = "broadcasts"
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val rng = SecureRandom()
    }

    fun currentUserUid(): String? = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()

    fun currentUserDisplayName(): String {
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        val raw = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Player"
        return raw.take(40)
    }

    fun currentUserEmail(): String? = runCatching { Firebase.auth.currentUser?.email }.getOrNull()

    private fun requireUid(): String =
        currentUserUid() ?: throw IllegalStateException("Please sign in with Google to play Online Multiplayer")

    private fun generateRoomCode(): String {
        val sb = StringBuilder(6)
        repeat(6) {
            sb.append(CODE_CHARS[rng.nextInt(CODE_CHARS.length)])
        }
        return sb.toString()
    }

    private fun parseRoomSnapshot(snap: DocumentSnapshot): MultiplayerRoomDoc? {
        if (!snap.exists()) return null
        val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return MultiplayerRoomDoc(
            roomCode = snap.getString("roomCode") ?: snap.id,
            gameType = snap.getString("gameType") ?: "LUDO",
            hostUid = snap.getString("hostUid") ?: "",
            hostName = snap.getString("hostName") ?: "",
            guestUid = snap.getString("guestUid") ?: "",
            guestName = snap.getString("guestName") ?: "",
            memberUids = (snap.get("memberUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            publicRoom = snap.getBoolean("publicRoom") ?: false,
            entryFee = (snap.getDouble("entryFee") ?: 0.0),
            status = snap.getString("status") ?: "WAITING",
            turnUid = snap.getString("turnUid") ?: "",
            winnerUid = snap.getString("winnerUid") ?: "",
            statePayload = snap.getString("statePayload") ?: "{}",
            moveCount = (snap.getLong("moveCount") ?: 0L).toInt(),
            lastMoveSummary = snap.getString("lastMoveSummary") ?: "",
            createdAt = snap.getTimestamp("createdAt", est),
            updatedAt = snap.getTimestamp("updatedAt", est)
        )
    }

    suspend fun createRoom(
        gameType: String,
        hostDisplayLabel: String,
        publicRoom: Boolean,
        entryFee: Double = 0.0
    ): Result<MultiplayerRoomDoc> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            val db = firestoreProvider()
            val code = generateRoomCode()
            val cleanHost = hostDisplayLabel.trim().ifBlank { currentUserDisplayName() }.take(70)
            val initialPayload = OnlineMatchSerializer.initialPayloadForGame(gameType)
            val path = "$COLLECTION_ROOMS/$code"

            val payload = mapOf(
                "roomCode" to code,
                "gameType" to gameType,
                "hostUid" to uid,
                "hostName" to cleanHost,
                "guestUid" to "",
                "guestName" to "",
                "memberUids" to listOf(uid),
                "publicRoom" to publicRoom,
                "entryFee" to entryFee.coerceIn(0.0, 100000.0),
                "status" to "WAITING",
                "turnUid" to uid,
                "winnerUid" to "",
                "statePayload" to initialPayload,
                "moveCount" to 0,
                "lastMoveSummary" to "Room created",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            try {
                db.collection(COLLECTION_ROOMS).document(code).set(payload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, path)
                throw e
            }

            MultiplayerRoomDoc(
                roomCode = code,
                gameType = gameType,
                hostUid = uid,
                hostName = cleanHost,
                memberUids = listOf(uid),
                publicRoom = publicRoom,
                entryFee = entryFee.coerceIn(0.0, 100000.0),
                status = "WAITING",
                turnUid = uid,
                statePayload = initialPayload,
                moveCount = 0,
                lastMoveSummary = "Room created"
            )
        }
    }

    suspend fun joinRoomByCode(
        rawCode: String,
        guestDisplayLabel: String
    ): Result<MultiplayerRoomDoc> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            val code = rawCode.trim().uppercase()
            require(code.length == 6) { "Enter a valid 6-character Room Code" }
            val db = firestoreProvider()
            val path = "$COLLECTION_ROOMS/$code"
            val docRef = db.collection(COLLECTION_ROOMS).document(code)

            val snap = try {
                docRef.get().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
            val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room $code not found")
            if (room.hostUid == uid || room.guestUid == uid) {
                return@runCatching room
            }
            require(room.status == "WAITING" && room.guestUid.isBlank()) {
                "Room $code is already full or finished"
            }

            val cleanGuest = guestDisplayLabel.trim().ifBlank { currentUserDisplayName() }.take(70)
            val updates = mapOf(
                "guestUid" to uid,
                "guestName" to cleanGuest,
                "memberUids" to listOf(room.hostUid, uid),
                "status" to "PLAYING",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updates).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                throw e
            }

            room.copy(
                guestUid = uid,
                guestName = cleanGuest,
                memberUids = listOf(room.hostUid, uid),
                status = "PLAYING"
            )
        }
    }

    suspend fun findOrCreateQuickMatch(
        gameType: String,
        playerDisplayLabel: String,
        entryFee: Double = 0.0
    ): Result<MultiplayerRoomDoc> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            val db = firestoreProvider()
            val cleanName = playerDisplayLabel.trim().ifBlank { currentUserDisplayName() }.take(70)

            val waitingDocs = try {
                db.collection(COLLECTION_ROOMS)
                    .whereEqualTo("status", "WAITING")
                    .whereEqualTo("publicRoom", true)
                    .whereEqualTo("gameType", gameType)
                    .limit(10)
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, COLLECTION_ROOMS)
                emptyList()
            }

            val candidate = waitingDocs
                .mapNotNull(::parseRoomSnapshot)
                .firstOrNull { it.hostUid != uid && it.guestUid.isBlank() && it.entryFee == entryFee }

            if (candidate != null) {
                val joined = joinRoomByCode(candidate.roomCode, cleanName)
                if (joined.isSuccess) return@runCatching joined.getOrThrow()
            }

            createRoom(
                gameType = gameType,
                hostDisplayLabel = cleanName,
                publicRoom = true,
                entryFee = entryFee
            ).getOrThrow()
        }
    }

    data class LiveGameSeatStatus(
        val gameType: String,
        val waitingRoomsCount: Int,
        val maxConcurrentSeats: Int,
        val waitingPlayerNames: List<String>,
        val multiplayerEnabled: Boolean
    ) {
        val isServerBusy: Boolean get() = !multiplayerEnabled || waitingRoomsCount >= maxConcurrentSeats
        val openSeats: Int get() = (maxConcurrentSeats - waitingRoomsCount).coerceAtLeast(0)
    }

    /**
     * Checks live public WAITING rooms for [gameType] to show real waiting players' names
     * and enforce server seat capacity before players enter matchmaking.
     */
    suspend fun fetchLiveSeatAvailability(
        gameType: String,
        maxConcurrentSeats: Int,
        multiplayerEnabled: Boolean
    ): LiveGameSeatStatus = withContext(Dispatchers.IO) {
        if (!multiplayerEnabled) {
            return@withContext LiveGameSeatStatus(
                gameType = gameType,
                waitingRoomsCount = maxConcurrentSeats,
                maxConcurrentSeats = maxConcurrentSeats,
                waitingPlayerNames = emptyList(),
                multiplayerEnabled = false
            )
        }
        val uid = currentUserUid()
        if (uid == null) {
            return@withContext LiveGameSeatStatus(
                gameType = gameType,
                waitingRoomsCount = 0,
                maxConcurrentSeats = maxConcurrentSeats,
                waitingPlayerNames = emptyList(),
                multiplayerEnabled = true
            )
        }
        val docs = try {
            firestoreProvider()
                .collection(COLLECTION_ROOMS)
                .whereEqualTo("status", "WAITING")
                .whereEqualTo("publicRoom", true)
                .whereEqualTo("gameType", gameType)
                .limit(maxConcurrentSeats.toLong().coerceAtLeast(10L))
                .get()
                .await()
                .documents
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_ROOMS)
            emptyList()
        }
        val rooms = docs.mapNotNull(::parseRoomSnapshot)
        LiveGameSeatStatus(
            gameType = gameType,
            waitingRoomsCount = rooms.size,
            maxConcurrentSeats = maxConcurrentSeats,
            waitingPlayerNames = rooms.map { it.hostName }.filter { it.isNotBlank() }.distinct(),
            multiplayerEnabled = true
        )
    }

    fun observeRoom(roomCode: String): Flow<MultiplayerRoomDoc?> {
        val code = roomCode.trim().uppercase()
        if (code.length != 6 || currentUserUid() == null) return flowOf(null)
        val path = "$COLLECTION_ROOMS/$code"
        return firestoreProvider()
            .collection(COLLECTION_ROOMS)
            .document(code)
            .snapshots()
            .map { snap -> parseRoomSnapshot(snap) }
            .catch { err ->
                val firestoreErr = (err as? FirebaseFirestoreException)
                    ?: (err.cause as? FirebaseFirestoreException)
                    ?: (err as? Exception)
                if (err is CancellationException && firestoreErr == null) throw err
                firestoreErr?.let { handleFirestoreError(it, OperationType.GET, path) }
                emit(null)
            }
    }

    fun observeMyRecentRooms(): Flow<List<MultiplayerRoomDoc>> {
        val uid = currentUserUid() ?: return flowOf(emptyList())
        return firestoreProvider()
            .collection(COLLECTION_ROOMS)
            .whereArrayContains("memberUids", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(10)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull(::parseRoomSnapshot) }
            .catch { err ->
                val firestoreErr = (err as? FirebaseFirestoreException)
                    ?: (err.cause as? FirebaseFirestoreException)
                    ?: (err as? Exception)
                if (err is CancellationException && firestoreErr == null) throw err
                firestoreErr?.let { handleFirestoreError(it, OperationType.LIST, COLLECTION_ROOMS) }
                emit(emptyList())
            }
    }

    suspend fun submitMove(
        roomCode: String,
        nextStatePayload: String,
        nextTurnUid: String,
        nextMoveCount: Int,
        lastMoveSummary: String,
        winnerUid: String = "",
        finished: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireUid()
            val code = roomCode.trim().uppercase()
            val path = "$COLLECTION_ROOMS/$code"
            val updates = mapOf(
                "statePayload" to nextStatePayload,
                "turnUid" to nextTurnUid,
                "moveCount" to nextMoveCount,
                "lastMoveSummary" to lastMoveSummary.take(190),
                "status" to if (finished) "FINISHED" else "PLAYING",
                "winnerUid" to winnerUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                firestoreProvider().collection(COLLECTION_ROOMS).document(code).update(updates).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                throw e
            }
            Unit
        }
    }

    suspend fun cancelOrForfeitRoom(room: MultiplayerRoomDoc): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = requireUid()
            val code = room.roomCode.trim().uppercase()
            val path = "$COLLECTION_ROOMS/$code"
            val updates = if (room.status == "WAITING" && room.hostUid == uid) {
                mapOf(
                    "status" to "CANCELLED",
                    "winnerUid" to "",
                    "lastMoveSummary" to "Room cancelled by host",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                val opponentUid = room.memberUids.firstOrNull { it != uid }.orEmpty()
                mapOf(
                    "status" to "FINISHED",
                    "winnerUid" to opponentUid,
                    "lastMoveSummary" to "Match forfeited",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            try {
                firestoreProvider().collection(COLLECTION_ROOMS).document(code).update(updates).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                throw e
            }
            Unit
        }
    }

    /**
     * Best-effort cloud sync of Dual Wallet summary (including withdrawable winningBalance)
     * when user is signed in with Google/Firebase Auth.
     */
    suspend fun syncWalletSummaryToCloud(wallet: PlayWalletEntity) = withContext(Dispatchers.IO) {
        val uid = currentUserUid() ?: return@withContext
        val path = "users/$uid/wallet/primary"
        val docRef = firestoreProvider().collection("users").document(uid).collection("wallet").document("primary")
        runCatching {
            val existing = docRef.get().await()
            val data = mutableMapOf<String, Any>(
                "userId" to uid,
                "tournamentBalance" to wallet.tournamentBalance.coerceIn(0.0, 10_000_000.0),
                "winningBalance" to wallet.winningBalance.coerceIn(0.0, 10_000_000.0),
                "gameBalance" to wallet.gameBalance.coerceIn(0.0, 10_000_000.0),
                "equippedBoardSkinId" to wallet.equippedBoardSkinId.take(64),
                "equippedAvatarId" to wallet.equippedAvatarId.take(64),
                "ownedItemsCsv" to wallet.ownedItemsCsv.take(950),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing.exists()) {
                data["createdAt"] = existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            } else {
                data["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(data).await()
        }.onFailure { err ->
            if (err is Exception) handleFirestoreError(err, OperationType.WRITE, path)
        }
    }

    /**
     * Real-time Firestore listener for the signed-in user's Tournament/Winning/Game wallet balances.
     */
    fun observeCloudWalletSummary(): Flow<CloudWalletSummaryDoc?> {
        val uid = currentUserUid() ?: return flowOf(null)
        val path = "users/$uid/wallet/primary"
        val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return firestoreProvider()
            .collection("users")
            .document(uid)
            .collection("wallet")
            .document("primary")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else CloudWalletSummaryDoc(
                    userId = snap.getString("userId") ?: uid,
                    tournamentBalance = snap.getDouble("tournamentBalance") ?: 0.0,
                    winningBalance = snap.getDouble("winningBalance") ?: 0.0,
                    gameBalance = snap.getDouble("gameBalance") ?: 0.0,
                    equippedBoardSkinId = snap.getString("equippedBoardSkinId") ?: "skin_royal_emerald",
                    equippedAvatarId = snap.getString("equippedAvatarId") ?: "avatar_monarch",
                    ownedItemsCsv = snap.getString("ownedItemsCsv") ?: "",
                    updatedAtMillis = snap.getTimestamp("updatedAt", est)?.toDate()?.time ?: System.currentTimeMillis()
                )
            }
            .catch { err ->
                val firestoreErr = (err as? FirebaseFirestoreException)
                    ?: (err.cause as? FirebaseFirestoreException)
                    ?: (err as? Exception)
                if (err is CancellationException && firestoreErr == null) throw err
                firestoreErr?.let { handleFirestoreError(it, OperationType.GET, path) }
                emit(null)
            }
    }

    /**
     * Real-time Firestore listener for the signed-in user's Wallet transaction ledger.
     */
    fun observeCloudWalletLedger(): Flow<List<CloudWalletLedgerDoc>> {
        val uid = currentUserUid() ?: return flowOf(emptyList())
        val path = "users/$uid/wallet_ledger"
        val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return firestoreProvider()
            .collection("users")
            .document(uid)
            .collection("wallet_ledger")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { snap ->
                val cutoff = System.currentTimeMillis() - PlayWalletRepository.WALLET_HISTORY_RETENTION_MS
                snap.documents.map { doc ->
                    CloudWalletLedgerDoc(
                        id = doc.id,
                        userId = doc.getString("userId") ?: uid,
                        walletBucket = doc.getString("walletBucket") ?: "TOURNAMENT",
                        kind = doc.getString("kind") ?: "TRANSACTION",
                        amountDelta = doc.getDouble("amountDelta") ?: 0.0,
                        note = doc.getString("note") ?: "",
                        createdAtMillis = doc.getTimestamp("createdAt", est)?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }.filter { it.createdAtMillis >= cutoff }
            }
            .catch { err ->
                val firestoreErr = (err as? FirebaseFirestoreException)
                    ?: (err.cause as? FirebaseFirestoreException)
                    ?: (err as? Exception)
                if (err is CancellationException && firestoreErr == null) throw err
                firestoreErr?.let { handleFirestoreError(it, OperationType.LIST, path) }
                emit(emptyList())
            }
    }

    /**
     * Appends an immutable transaction entry to `/users/{uid}/wallet_ledger/{entryId}` in real time.
     */
    suspend fun appendCloudWalletLedgerEntry(
        walletBucket: String,
        kind: String,
        amountDelta: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        val uid = currentUserUid() ?: return@withContext
        val safeBucket = walletBucket.uppercase().takeIf { it in setOf("TOURNAMENT", "WINNING", "GAME") } ?: "TOURNAMENT"
        val entryId = "led_${System.currentTimeMillis()}_${rng.nextInt(10000)}"
        val path = "users/$uid/wallet_ledger/$entryId"
        val payload = mapOf(
            "userId" to uid,
            "walletBucket" to safeBucket,
            "kind" to kind.take(48).ifBlank { "TRANSACTION" },
            "amountDelta" to amountDelta.coerceIn(-10_000_000.0, 10_000_000.0),
            "note" to note.take(240),
            "createdAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            firestoreProvider()
                .collection("users")
                .document(uid)
                .collection("wallet_ledger")
                .document(entryId)
                .set(payload)
                .await()
        }.onFailure { err ->
            if (err is Exception) handleFirestoreError(err, OperationType.CREATE, path)
        }
    }

    /**
     * Publishes a global real-time Admin Broadcast to Firestore `/broadcasts/{id}` so all signed-in devices receive it immediately.
     */
    suspend fun publishCloudBroadcast(
        title: String,
        message: String,
        priority: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUserUid() ?: throw IllegalStateException("Sign in with Admin Google account for cloud broadcast sync")
            val email = currentUserEmail().orEmpty()
            val safePriority = priority.trim().uppercase().takeIf { it in setOf("INFO", "TOURNAMENT", "PROMO", "URGENT") } ?: "INFO"
            val docId = "bc_${System.currentTimeMillis()}_${uid.take(6)}"
            val path = "$COLLECTION_BROADCASTS/$docId"
            val payload = mapOf(
                "title" to title.trim().take(120),
                "message" to message.trim().take(1000),
                "priority" to safePriority,
                "senderEmail" to email.take(254),
                "createdAt" to FieldValue.serverTimestamp()
            )
            try {
                firestoreProvider().collection(COLLECTION_BROADCASTS).document(docId).set(payload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, path)
                throw e
            }
            docId
        }
    }

    /**
     * Real-time Firestore listener for Admin Broadcasts & Free Fire Room Alerts.
     */
    fun observeCloudBroadcasts(): Flow<List<CloudBroadcastDoc>> {
        if (currentUserUid() == null) return flowOf(emptyList())
        val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return firestoreProvider()
            .collection(COLLECTION_BROADCASTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap ->
                snap.documents.map { doc ->
                    CloudBroadcastDoc(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        priority = doc.getString("priority") ?: "INFO",
                        senderEmail = doc.getString("senderEmail") ?: "",
                        createdAtMillis = doc.getTimestamp("createdAt", est)?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
            }
            .catch { err ->
                val firestoreErr = (err as? FirebaseFirestoreException)
                    ?: (err.cause as? FirebaseFirestoreException)
                    ?: (err as? Exception)
                if (err is CancellationException && firestoreErr == null) throw err
                firestoreErr?.let { handleFirestoreError(it, OperationType.LIST, COLLECTION_BROADCASTS) }
                emit(emptyList())
            }
    }

    /**
     * Best-effort cloud mirror of a submitted deposit/withdraw request when signed in with Firebase Auth.
     */
    suspend fun mirrorWalletRequestToCloud(req: PlayWalletRequestEntity) = withContext(Dispatchers.IO) {
        val uid = currentUserUid() ?: return@withContext
        val docId = "req_${uid.take(8)}_${req.id}_${req.createdAt}"
        val path = "$COLLECTION_WALLET_REQUESTS/$docId"
        val data = mapOf(
            "userId" to uid,
            "requestType" to req.requestType.name,
            "channel" to req.channel.name,
            "amount" to req.amount.coerceIn(1.0, 1_000_000.0),
            "accountNumber" to req.accountNumber.take(64),
            "transactionId" to req.transactionId.take(64),
            "status" to "PENDING",
            "adminNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            firestoreProvider().collection(COLLECTION_WALLET_REQUESTS).document(docId).set(data).await()
        }.onFailure { err ->
            if (err is Exception) handleFirestoreError(err, OperationType.CREATE, path)
        }
    }
}
