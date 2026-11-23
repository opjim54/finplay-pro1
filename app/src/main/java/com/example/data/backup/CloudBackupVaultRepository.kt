package com.example.data.backup

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

data class CloudVaultRecord(
    val userId: String,
    val email: String,
    val payloadJson: String,
    val exportedAt: Long,
    val accountCount: Int,
    val transactionCount: Int,
    val updatedAtMillis: Long
)

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?
) {
    val auth = runCatching { Firebase.auth }.getOrNull()
    val currentUser = auth?.currentUser

    val providerInfo = JSONArray()
    currentUser?.providerData?.forEach { profile ->
        val providerObj = JSONObject().apply {
            put("providerId", profile.providerId)
            put("displayName", profile.displayName)
            put("email", profile.email)
            put("photoUrl", profile.photoUrl?.toString())
        }
        providerInfo.put(providerObj)
    }

    val authInfo = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("isAnonymous", currentUser?.isAnonymous)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", providerInfo)
    }

    val errInfo = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path)
        put("authInfo", authInfo)
    }

    val jsonString = errInfo.toString()
    Log.e("FirestoreError", jsonString, error)
}

class CloudBackupVaultRepository(
    private val context: Context,
    private val firestoreProvider: () -> FirebaseFirestore = {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }
) {

    companion object {
        private const val BACKUP_DOC_ID = "latest"
        private const val MAX_PAYLOAD_CHARS = 940_000
    }

    fun currentFirebaseUid(): String? = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()

    fun currentFirebaseEmail(): String? = runCatching { Firebase.auth.currentUser?.email }.getOrNull()

    suspend fun uploadSnapshotToVault(
        payloadJson: String,
        snapshot: FullBackupSnapshot,
        fallbackEmail: String? = null,
        explicitUid: String? = null
    ): Result<CloudVaultRecord> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = explicitUid ?: currentFirebaseUid()
                ?: throw IllegalStateException("Firebase Auth session not signed in")
            val email = (currentFirebaseEmail() ?: fallbackEmail ?: "").trim()
                .ifBlank { "user@gmail.com" }
            if (payloadJson.length > MAX_PAYLOAD_CHARS) {
                throw IllegalArgumentException("Backup payload exceeds Firestore document limit")
            }

            val db = firestoreProvider()
            val path = "users/$uid/backups/$BACKUP_DOC_ID"
            val docRef = db.collection("users").document(uid).collection("backups").document(BACKUP_DOC_ID)

            val existing = try {
                docRef.get().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, path)
                null
            }

            val nowDate = Date(System.currentTimeMillis() - 500L)
            val nowTs = Timestamp(nowDate)
            val createdTs = existing?.getTimestamp("createdAt") ?: nowTs

            val data = mapOf(
                "userId" to uid,
                "email" to email,
                "payloadJson" to payloadJson,
                "exportedAt" to snapshot.exportedAt,
                "accountCount" to snapshot.accounts.size,
                "transactionCount" to snapshot.transactions.size,
                "schemaVersion" to snapshot.schemaVersion,
                "createdAt" to createdTs,
                "updatedAt" to nowTs
            )

            try {
                if (existing != null && existing.exists()) {
                    docRef.set(data, SetOptions.merge()).await()
                } else {
                    docRef.set(data).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(
                    e,
                    if (existing != null && existing.exists()) OperationType.UPDATE else OperationType.CREATE,
                    path
                )
                throw e
            }

            CloudVaultRecord(
                userId = uid,
                email = email,
                payloadJson = payloadJson,
                exportedAt = snapshot.exportedAt,
                accountCount = snapshot.accounts.size,
                transactionCount = snapshot.transactions.size,
                updatedAtMillis = nowDate.time
            )
        }
    }

    suspend fun downloadLatestVaultRecord(
        explicitUid: String? = null
    ): Result<CloudVaultRecord?> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = explicitUid ?: currentFirebaseUid()
                ?: throw IllegalStateException("Firebase Auth session not signed in")
            val db = firestoreProvider()
            val path = "users/$uid/backups/$BACKUP_DOC_ID"
            val docRef = db.collection("users").document(uid).collection("backups").document(BACKUP_DOC_ID)

            val snap = try {
                docRef.get().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, path)
                throw e
            }

            if (!snap.exists()) return@runCatching null
            val payload = snap.getString("payloadJson") ?: return@runCatching null
            val email = snap.getString("email") ?: currentFirebaseEmail().orEmpty()
            val exportedAt = snap.getLong("exportedAt") ?: System.currentTimeMillis()
            val accountCount = (snap.getLong("accountCount") ?: 0L).toInt()
            val txCount = (snap.getLong("transactionCount") ?: 0L).toInt()
            val updatedAtMillis = snap.getTimestamp("updatedAt")?.toDate()?.time ?: exportedAt

            CloudVaultRecord(
                userId = uid,
                email = email,
                payloadJson = payload,
                exportedAt = exportedAt,
                accountCount = accountCount,
                transactionCount = txCount,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}
