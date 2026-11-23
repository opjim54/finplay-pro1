package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.AiProviderKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object AiChatClient {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun sendChatCompletion(
        provider: AiProviderEntity,
        history: List<AiChatMessageEntity>,
        promptText: String,
        imageBase64: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = provider.apiKey.trim()
            require(apiKey.isNotEmpty()) { "API key is empty." }

            when (provider.providerKind) {
                AiProviderKind.GEMINI -> callGemini(provider, history, promptText, imageBase64)
                AiProviderKind.XKIRO, AiProviderKind.OPENAI_COMPATIBLE ->
                    callOpenAiCompatible(provider, history, promptText, imageBase64)
            }
        }
    }

    private fun callGemini(
        provider: AiProviderEntity,
        history: List<AiChatMessageEntity>,
        promptText: String,
        imageBase64: String?
    ): String {
        val baseUrl = provider.baseUrl.trim().trimEnd('/').ifBlank {
            "https://generativelanguage.googleapis.com/v1beta"
        }
        val model = provider.modelName.trim().ifBlank { "gemini-2.5-flash" }
        val url = "$baseUrl/models/$model:generateContent"

        val recentHistory = history.takeLast(14)
        val contentsArray = buildJsonArray {
            recentHistory.forEach { msg ->
                add(
                    buildJsonObject {
                        put("role", JsonPrimitive(if (msg.role == "USER") "user" else "model"))
                        put(
                            "parts",
                            buildJsonArray {
                                if (msg.textContent.isNotBlank()) {
                                    add(buildJsonObject { put("text", JsonPrimitive(msg.textContent)) })
                                }
                                if (!msg.imageBase64Thumb.isNullOrBlank() && provider.supportsVision) {
                                    add(
                                        buildJsonObject {
                                            put(
                                                "inline_data",
                                                buildJsonObject {
                                                    put("mime_type", JsonPrimitive("image/jpeg"))
                                                    put("data", JsonPrimitive(msg.imageBase64Thumb))
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
            add(
                buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put(
                        "parts",
                        buildJsonArray {
                            val finalPrompt = promptText.ifBlank { "Analyze this image." }
                            add(buildJsonObject { put("text", JsonPrimitive(finalPrompt)) })
                            if (!imageBase64.isNullOrBlank() && provider.supportsVision) {
                                add(
                                    buildJsonObject {
                                        put(
                                            "inline_data",
                                            buildJsonObject {
                                                put("mime_type", JsonPrimitive("image/jpeg"))
                                                put("data", JsonPrimitive(imageBase64))
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }

        val payload = buildJsonObject {
            put("contents", contentsArray)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", provider.apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("Gemini HTTP ${resp.code}: ${extractErrorMessage(raw)}")
            }
            val root = jsonParser.parseToJsonElement(raw).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val first = candidates?.firstOrNull()?.jsonObject
            val parts = first?.get("content")?.jsonObject?.get("parts")?.jsonArray
            val text = parts?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }?.joinToString("\n")
            return text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty response from Gemini.")
        }
    }

    private fun callOpenAiCompatible(
        provider: AiProviderEntity,
        history: List<AiChatMessageEntity>,
        promptText: String,
        imageBase64: String?
    ): String {
        val defaultBase = if (provider.providerKind == AiProviderKind.XKIRO) {
            "https://api.xkiro.com/v1"
        } else {
            "https://api.openai.com/v1"
        }
        val cleanBase = provider.baseUrl.trim().trimEnd('/').ifBlank { defaultBase }
        val url = if (cleanBase.endsWith("/chat/completions")) cleanBase else "$cleanBase/chat/completions"
        val model = provider.modelName.trim().ifBlank {
            if (provider.providerKind == AiProviderKind.XKIRO) "openai/gpt-5.6-sol" else "gpt-4o-mini"
        }

        val recentHistory = history.takeLast(14)
        val messagesArray = buildJsonArray {
            add(
                buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive("You are a helpful, accurate assistant inside Smart Finance Life."))
                }
            )
            recentHistory.forEach { msg ->
                val role = if (msg.role == "USER") "user" else "assistant"
                if (!msg.imageBase64Thumb.isNullOrBlank() && provider.supportsVision && role == "user") {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put(
                                "content",
                                buildJsonArray {
                                    add(buildJsonObject {
                                        put("type", JsonPrimitive("text"))
                                        put("text", JsonPrimitive(msg.textContent))
                                    })
                                    add(buildJsonObject {
                                        put("type", JsonPrimitive("image_url"))
                                        put("image_url", buildJsonObject {
                                            put("url", JsonPrimitive("data:image/jpeg;base64,${msg.imageBase64Thumb}"))
                                        })
                                    })
                                }
                            )
                        }
                    )
                } else {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive(role))
                            put("content", JsonPrimitive(msg.textContent))
                        }
                    )
                }
            }
            add(
                buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    if (!imageBase64.isNullOrBlank() && provider.supportsVision) {
                        put(
                            "content",
                            buildJsonArray {
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("text"))
                                    put("text", JsonPrimitive(promptText.ifBlank { "Analyze this image." }))
                                })
                                add(buildJsonObject {
                                    put("type", JsonPrimitive("image_url"))
                                    put("image_url", buildJsonObject {
                                        put("url", JsonPrimitive("data:image/jpeg;base64,$imageBase64"))
                                    })
                                })
                            }
                        )
                    } else {
                        put("content", JsonPrimitive(promptText))
                    }
                }
            )
        }

        val payload = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", messagesArray)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${provider.apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: ${extractErrorMessage(raw)}")
            }
            val root = jsonParser.parseToJsonElement(raw).jsonObject
            val choices = root["choices"]?.jsonArray
            val message = choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            val contentElem = message?.get("content")
            val text = when (contentElem) {
                is JsonPrimitive -> contentElem.content
                is JsonArray -> contentElem.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }.joinToString("\n")
                else -> null
            }
            return text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty response from provider.")
        }
    }

    private fun extractErrorMessage(raw: String): String {
        return runCatching {
            val obj = jsonParser.parseToJsonElement(raw).jsonObject
            val errObj = obj["error"]
            when (errObj) {
                is JsonObject -> errObj["message"]?.jsonPrimitive?.content ?: raw.take(180)
                is JsonPrimitive -> errObj.content
                else -> obj["message"]?.jsonPrimitive?.content ?: raw.take(180)
            }
        }.getOrDefault(raw.take(180))
    }

    suspend fun downscaleUriToBase64(context: Context, uri: Uri, maxDim: Int = 720, quality: Int = 78): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
                val original = input.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
                val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
                val scaled = if (targetW != original.width || targetH != original.height) {
                    original.scale(targetW, targetH)
                } else {
                    original
                }
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }.getOrNull()
        }
}
