package com.example.data.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.util.Base64
import android.util.Log
import com.example.R
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

sealed class GoogleAccountSignInOutcome {
    data class Success(
        val email: String,
        val displayName: String?,
        val firebaseUid: String? = null
    ) : GoogleAccountSignInOutcome()
    data object Cancelled : GoogleAccountSignInOutcome()
    data class NoAccount(val message: String) : GoogleAccountSignInOutcome()
    data class Error(val message: String) : GoogleAccountSignInOutcome()
}

sealed class DriveAuthState {
    data class Authorized(val accessToken: String, val email: String?) : DriveAuthState()
    data class NeedsConsent(val pendingIntent: PendingIntent, val email: String?) : DriveAuthState()
    data class Error(val message: String) : DriveAuthState()
}

sealed class DriveSyncResult {
    data class Success(
        val email: String?,
        val timestamp: Long,
        val fileId: String,
        val channelLabel: String = "Gmail Cloud Vault"
    ) : DriveSyncResult()
    data class NeedsConsent(val pendingIntent: PendingIntent, val email: String?) : DriveSyncResult()
    data class Error(val message: String) : DriveSyncResult()
}

sealed class DriveRestoreResult {
    data class Success(
        val jsonPayload: String,
        val email: String?,
        val modifiedTime: String?,
        val fileId: String,
        val channelLabel: String = "Gmail Cloud Vault"
    ) : DriveRestoreResult()
    data class NeedsConsent(val pendingIntent: PendingIntent, val email: String?) : DriveRestoreResult()
    data class NotFound(val email: String?) : DriveRestoreResult()
    data class Error(val message: String) : DriveRestoreResult()
}

class GoogleDriveClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "GoogleDriveClient"
        private const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        private const val SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        private const val BACKUP_FILENAME = "smart_finance_pro_backup.json"
        private const val VISIBLE_BACKUP_FILENAME = "SmartFinancePro_Backup.json"
    }

    private fun resolveWebClientId(ctx: Context): String? {
        return runCatching { ctx.getString(R.string.default_web_client_id) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    private fun checkPlayServices(): String? {
        val availability = GoogleApiAvailability.getInstance()
        val code = availability.isGooglePlayServicesAvailable(context)
        return if (code == ConnectionResult.SUCCESS) null
        else availability.getErrorString(code)
    }

    suspend fun signInWithGoogleAccount(activity: Activity): GoogleAccountSignInOutcome {
        checkPlayServices()?.let { playErr ->
            return GoogleAccountSignInOutcome.Error("Google Play Services unavailable: $playErr")
        }
        val webClientId = resolveWebClientId(activity)
            ?: return GoogleAccountSignInOutcome.Error("Missing default_web_client_id OAuth configuration")

        val credentialManager = CredentialManager.create(activity)
        val rawNonce = generateNonce()
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(rawNonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val mutableContext = MutableContextWrapper(activity)
            val response = credentialManager.getCredential(
                context = mutableContext,
                request = request
            )
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseUser = runCatching {
                    val authCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                    Firebase.auth.signInWithCredential(authCred).await().user
                }.onFailure { err ->
                    Log.w(TAG, "Firebase Auth signInWithCredential warning", err)
                }.getOrNull()

                val resolvedEmail = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: googleCred.id
                val resolvedName = firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: googleCred.displayName
                GoogleAccountSignInOutcome.Success(
                    email = resolvedEmail,
                    displayName = resolvedName,
                    firebaseUid = firebaseUser?.uid
                )
            } else {
                GoogleAccountSignInOutcome.Error("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or rejected by Play Services", e)
            GoogleAccountSignInOutcome.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available in Credential Manager", e)
            GoogleAccountSignInOutcome.NoAccount(
                e.errorMessage?.toString()
                    ?: "No Google account found on this device. Add a Google account in Android Settings."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error", e)
            GoogleAccountSignInOutcome.Error(
                e.errorMessage?.toString() ?: e.localizedMessage ?: "Google sign-in failed"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error", e)
            GoogleAccountSignInOutcome.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    suspend fun signOutGoogleAccount() {
        runCatching {
            Firebase.auth.signOut()
        }
        runCatching {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { e ->
            Log.w(TAG, "Failed to clear credential state", e)
        }
    }

    suspend fun authorizeDrive(
        activity: Activity? = null,
        knownEmail: String? = null
    ): DriveAuthState = withContext(Dispatchers.IO) {
        checkPlayServices()?.let { playErr ->
            return@withContext DriveAuthState.Error("Google Play Services unavailable: $playErr")
        }
        runCatching {
            val authClient = if (activity != null) {
                Identity.getAuthorizationClient(activity)
            } else {
                Identity.getAuthorizationClient(context)
            }
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(
                    listOf(
                        Scope(SCOPE_DRIVE_APPDATA),
                        Scope(SCOPE_DRIVE_FILE),
                        Scope(SCOPE_EMAIL)
                    )
                )
                .build()
            val result = authClient.authorize(request).await()
            if (result.hasResolution()) {
                val pi = result.pendingIntent
                    ?: return@runCatching DriveAuthState.Error("Authorization intent unavailable")
                DriveAuthState.NeedsConsent(pi, knownEmail)
            } else {
                val token = result.accessToken
                    ?: return@runCatching DriveAuthState.Error("No OAuth access token returned")
                val email = fetchUserEmail(token) ?: knownEmail?.takeIf { it.isNotBlank() }
                DriveAuthState.Authorized(token, email)
            }
        }.getOrElse { e ->
            Log.e(TAG, "authorizeDrive failed", e)
            DriveAuthState.Error(formatAuthException(e))
        }
    }

    suspend fun handleAuthorizationIntentResult(
        data: Intent?,
        activity: Activity? = null,
        knownEmail: String? = null
    ): DriveAuthState = withContext(Dispatchers.IO) {
        runCatching {
            val authClient = if (activity != null) {
                Identity.getAuthorizationClient(activity)
            } else {
                Identity.getAuthorizationClient(context)
            }
            val result = authClient.getAuthorizationResultFromIntent(data)
            val token = result.accessToken
                ?: return@runCatching DriveAuthState.Error("Authorization cancelled or blocked by OAuth Testing Mode (403)")
            val email = fetchUserEmail(token) ?: knownEmail?.takeIf { it.isNotBlank() }
            DriveAuthState.Authorized(token, email)
        }.getOrElse { e ->
            Log.e(TAG, "handleAuthorizationIntentResult failed", e)
            DriveAuthState.Error(formatAuthException(e))
        }
    }

    private fun formatAuthException(e: Throwable): String {
        if (e is ApiException) {
            return when (e.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR ->
                    "OAuth configuration mismatch (code 10). Ensure app signature & package name match Google Cloud OAuth client."
                CommonStatusCodes.NETWORK_ERROR ->
                    "Network unavailable while connecting to Google Drive."
                CommonStatusCodes.SIGN_IN_REQUIRED ->
                    "Google sign-in required to access Drive."
                CommonStatusCodes.CANCELED ->
                    "Google Drive OAuth consent was cancelled or blocked (Error 403 Testing Mode)."
                else -> "Google Drive auth error (${e.statusCode}): ${e.localizedMessage ?: "Unknown"}"
            }
        }
        return e.localizedMessage ?: "Google Play Services authorization unavailable"
    }

    suspend fun uploadBackup(
        jsonContent: String,
        interactive: Boolean,
        activity: Activity? = null,
        knownEmail: String? = null
    ): DriveSyncResult = withContext(Dispatchers.IO) {
        saveLocalMirror(jsonContent)
        when (val auth = authorizeDrive(activity = activity, knownEmail = knownEmail)) {
            is DriveAuthState.NeedsConsent -> {
                if (interactive) DriveSyncResult.NeedsConsent(auth.pendingIntent, auth.email)
                else DriveSyncResult.Error("Drive consent required")
            }
            is DriveAuthState.Error -> DriveSyncResult.Error(auth.message)
            is DriveAuthState.Authorized -> {
                val appDataRes = uploadOrUpdateFile(
                    accessToken = auth.accessToken,
                    fileName = BACKUP_FILENAME,
                    spaces = "appDataFolder",
                    parentFolder = "appDataFolder",
                    jsonContent = jsonContent
                )
                runCatching {
                    uploadOrUpdateFile(
                        accessToken = auth.accessToken,
                        fileName = VISIBLE_BACKUP_FILENAME,
                        spaces = "drive",
                        parentFolder = null,
                        jsonContent = jsonContent
                    )
                }.onFailure { err ->
                    Log.w(TAG, "Visible Drive mirror upload warning", err)
                }
                appDataRes.fold(
                    onSuccess = { fileId ->
                        DriveSyncResult.Success(
                            email = auth.email ?: knownEmail?.takeIf { it.isNotBlank() },
                            timestamp = System.currentTimeMillis(),
                            fileId = fileId,
                            channelLabel = "Google Drive + Cloud Vault"
                        )
                    },
                    onFailure = { err ->
                        DriveSyncResult.Error(err.localizedMessage ?: "Drive upload failed")
                    }
                )
            }
        }
    }

    suspend fun restoreLatestBackup(
        activity: Activity? = null,
        knownEmail: String? = null
    ): DriveRestoreResult = withContext(Dispatchers.IO) {
        when (val auth = authorizeDrive(activity = activity, knownEmail = knownEmail)) {
            is DriveAuthState.NeedsConsent -> DriveRestoreResult.NeedsConsent(auth.pendingIntent, auth.email)
            is DriveAuthState.Error -> DriveRestoreResult.Error(auth.message)
            is DriveAuthState.Authorized -> {
                val appDataMeta = findFile(auth.accessToken, BACKUP_FILENAME, "appDataFolder")
                val visibleMeta = findFile(auth.accessToken, VISIBLE_BACKUP_FILENAME, "drive")
                val meta = chooseNewestMetadata(appDataMeta, visibleMeta)
                if (meta == null) {
                    DriveRestoreResult.NotFound(auth.email ?: knownEmail)
                } else {
                    downloadFile(auth.accessToken, meta.id).fold(
                        onSuccess = { payload ->
                            DriveRestoreResult.Success(
                                jsonPayload = payload,
                                email = auth.email ?: knownEmail,
                                modifiedTime = meta.modifiedTime,
                                fileId = meta.id,
                                channelLabel = "Google Drive"
                            )
                        },
                        onFailure = { err ->
                            DriveRestoreResult.Error(err.localizedMessage ?: "Failed to download backup")
                        }
                    )
                }
            }
        }
    }

    private fun chooseNewestMetadata(a: DriveFileMetadata?, b: DriveFileMetadata?): DriveFileMetadata? {
        if (a == null) return b
        if (b == null) return a
        val tA = a.modifiedTime.orEmpty()
        val tB = b.modifiedTime.orEmpty()
        return if (tB > tA) b else a
    }

    fun saveLocalMirror(jsonContent: String): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "latest_backup.json")
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    fun readLocalMirror(): String? {
        val file = File(File(context.filesDir, "backups"), "latest_backup.json")
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    private fun fetchUserEmail(accessToken: String): String? {
        return runCatching {
            val req = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer $accessToken")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "userinfo HTTP ${resp.code}")
                    return@use null
                }
                val body = resp.body?.string() ?: return@use null
                jsonParser.parseToJsonElement(body).jsonObject["email"]?.jsonPrimitive?.content
            }
        }.getOrNull()
    }

    private fun findFile(accessToken: String, fileName: String, spaces: String): DriveFileMetadata? {
        val query = "name='$fileName' and trashed=false"
        val encodedQ = java.net.URLEncoder.encode(query, "UTF-8")
        val url =
            "https://www.googleapis.com/drive/v3/files?spaces=$spaces&q=$encodedQ&fields=files(id,name,modifiedTime,size)&orderBy=modifiedTime desc"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "findFile($fileName, $spaces) HTTP ${resp.code}")
                    return@use null
                }
                val body = resp.body?.string() ?: return@use null
                val files = jsonParser.parseToJsonElement(body).jsonObject["files"]?.jsonArray ?: return@use null
                val first = files.firstOrNull()?.jsonObject ?: return@use null
                DriveFileMetadata(
                    id = first["id"]?.jsonPrimitive?.content ?: return@use null,
                    name = first["name"]?.jsonPrimitive?.content ?: fileName,
                    modifiedTime = first["modifiedTime"]?.jsonPrimitive?.content,
                    size = first["size"]?.jsonPrimitive?.content
                )
            }
        }.getOrNull()
    }

    private fun uploadOrUpdateFile(
        accessToken: String,
        fileName: String,
        spaces: String,
        parentFolder: String?,
        jsonContent: String
    ): Result<String> = runCatching {
        val existing = findFile(accessToken, fileName, spaces)
        val metadataObj = buildJsonObject {
            put("name", fileName)
            put("mimeType", "application/json")
            if (existing == null && parentFolder != null) {
                putJsonArray("parents") { add(kotlinx.serialization.json.JsonPrimitive(parentFolder)) }
            }
        }
        val metadataJson = metadataObj.toString()

        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addPart(jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val request = if (existing != null) {
            Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files/${existing.id}?uploadType=multipart")
                .header("Authorization", "Bearer $accessToken")
                .patch(multipart)
                .build()
        } else {
            Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .header("Authorization", "Bearer $accessToken")
                .post(multipart)
                .build()
        }

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Drive HTTP ${resp.code}: $body")
            }
            val parsed = jsonParser.parseToJsonElement(body).jsonObject
            parsed["id"]?.jsonPrimitive?.content ?: existing?.id ?: "uploaded"
        }
    }

    private fun downloadFile(accessToken: String, fileId: String): Result<String> = runCatching {
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Drive Download HTTP ${resp.code}")
            }
            body
        }
    }
}
