package com.example.data.games

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.WalletPaymentChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Sends real-time Telegram Bot alerts when a user submits a withdrawal from their Winning Balance.
 * Reads credentials strictly from BuildConfig (injected via AI Studio Secrets panel / .env).
 */
object TelegramAlertService {
    private const val TAG = "TelegramAlertService"
    private const val TOKEN_PLACEHOLDER = "TELEGRAM_BOT_TOKEN_DEFAULT_VALUE"
    private const val CHAT_PLACEHOLDER = "TELEGRAM_CHAT_ID_DEFAULT_VALUE"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun isConfigured(): Boolean {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        val chatId = BuildConfig.TELEGRAM_CHAT_ID.trim()
        return token.isNotBlank() &&
            chatId.isNotBlank() &&
            token != TOKEN_PLACEHOLDER &&
            chatId != CHAT_PLACEHOLDER
    }

    suspend fun sendWithdrawalAlert(
        requestId: Long,
        username: String,
        amount: Double,
        channel: WalletPaymentChannel,
        receiverNumber: String,
        remainingWinningBalance: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        val chatId = BuildConfig.TELEGRAM_CHAT_ID.trim()
        if (!isConfigured()) {
            Log.w(TAG, "Telegram withdrawal alert skipped: configure TELEGRAM_BOT_TOKEN & TELEGRAM_CHAT_ID in AI Studio Secrets panel.")
            return@withContext Result.failure(IllegalStateException("TELEGRAM_NOT_CONFIGURED"))
        }

        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        val text = buildString {
            appendLine("🚨 NEW WITHDRAWAL REQUEST — FinPlay Pro")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🆔 Request ID: #$requestId")
            appendLine("👤 User: $username")
            appendLine("🏆 Amount (Winning Balance): ৳${amount.toInt()}")
            appendLine("💳 Channel: ${channel.name}")
            appendLine("📱 Receiver Number: $receiverNumber")
            appendLine("💰 Remaining Winning Balance: ৳${remainingWinningBalance.toInt()}")
            appendLine("🕒 Time: $timestamp")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            append("Open FinPlay Pro Admin Panel → Play & Earn tab to Mark PAID or Reject & Refund.")
        }

        runCatching {
            val formBody = FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", text)
                .build()
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendMessage")
                .post(formBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty().take(200)
                    Log.e(TAG, "Telegram API returned HTTP ${response.code}: $errBody")
                    throw IllegalStateException("Telegram API HTTP ${response.code}")
                }
            }
            Unit
        }
    }
}
