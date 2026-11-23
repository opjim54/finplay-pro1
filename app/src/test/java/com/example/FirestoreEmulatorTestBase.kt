package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var db: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected val projectId = "demo-test"

    // Must match R.string.firestore_database_id in res/values/firebase_applet_config.xml
    protected val databaseId = "ai-studio-android-smartfin-8bea01c1-7401-4c50-8ad3-14afa960619b"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    @Before
    open fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId("1:1234567890:android:abcdef")
                .setApiKey("fake-api-key-for-emulator")
                .build()
            FirebaseApp.initializeApp(context, options)
        }

        auth = FirebaseAuth.getInstance()
        try {
            auth.useEmulator("127.0.0.1", 9099)
        } catch (e: IllegalStateException) {
            // Already connected
        }

        db = FirebaseFirestore.getInstance(databaseId)
        try {
            db.useEmulator("127.0.0.1", 8085)
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        } catch (e: IllegalStateException) {
            // Already configured
        }

        pushRulesToEmulator()
        clearEmulatorData()
    }

    @After
    open fun tearDown() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    protected fun <T> awaitTask(task: Task<T>, timeoutMs: Long = 10_000L): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!task.isComplete && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(15)
        }
        shadowOf(Looper.getMainLooper()).idle()
        if (!task.isComplete) {
            throw TimeoutException("Task timed out after ${timeoutMs}ms")
        }
        if (task.isCanceled) {
            throw IllegalStateException("Task was cancelled")
        }
        task.exception?.let { throw ExecutionException(it) }
        @Suppress("UNCHECKED_CAST")
        return task.result as T
    }

    private fun pushRulesToEmulator() {
        val rulesFile = File("../firestore.rules").takeIf { it.exists() }
            ?: File("firestore.rules").takeIf { it.exists() }
            ?: throw IllegalStateException("firestore.rules not found")

        val rulesContent = rulesFile.readText()
        val escapedRules = rulesContent
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")

        val jsonPayload = """
            {
              "ignore_errors": true,
              "rules": {
                "files": [
                  {
                    "name": "firestore.rules",
                    "content": "$escapedRules"
                  }
                ]
              }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("http://127.0.0.1:8085/emulator/v1/projects/$projectId:securityRules?database=projects/$projectId/databases/$databaseId")
            .put(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to push rules to emulator: ${response.code} ${response.body?.string()}")
            }
        }
    }

    protected fun clearEmulatorData() {
        val firestoreReq = Request.Builder()
            .url("http://127.0.0.1:8085/emulator/v1/projects/$projectId/databases/$databaseId/documents")
            .delete()
            .build()
        httpClient.newCall(firestoreReq).execute().close()

        val authReq = Request.Builder()
            .url("http://127.0.0.1:9099/emulator/v1/projects/$projectId/accounts")
            .delete()
            .build()
        httpClient.newCall(authReq).execute().close()
    }

    protected fun signInTestUser(
        email: String = "md2005jim@gmail.com",
        password: String = "password123"
    ): String {
        val result = try {
            awaitTask(auth.createUserWithEmailAndPassword(email, password))
        } catch (e: Exception) {
            awaitTask(auth.signInWithEmailAndPassword(email, password))
        }
        return result.user!!.uid
    }
}
