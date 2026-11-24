package com.example.domain.games

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class GameSoundEffect {
    TAP,
    DICE_TICK,
    MOVE,
    CAPTURE,
    CHECK,
    SPECIAL,
    WIN
}

/**
 * Pure offline procedural sound synthesizer using AudioTrack.
 * Requires zero audio resource files and never crashes on headless/emulator/Robolectric targets.
 */
object GameSoundEngine {
    private const val PREFS_NAME = "smart_finance_game_prefs"
    private const val KEY_SOUND_ENABLED = "game_sound_enabled"
    private const val SAMPLE_RATE = 22050

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    @Volatile
    private var prefsInitialized = false

    fun init(context: Context) {
        if (prefsInitialized) return
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _soundEnabled.value = prefs.getBoolean(KEY_SOUND_ENABLED, true)
            prefsInitialized = true
        }
    }

    fun setSoundEnabled(context: Context?, enabled: Boolean) {
        _soundEnabled.value = enabled
        if (context != null) {
            runCatching {
                context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_SOUND_ENABLED, enabled)
                    .apply()
            }
        }
    }

    fun toggleSound(context: Context?): Boolean {
        val next = !_soundEnabled.value
        setSoundEnabled(context, next)
        if (next) play(GameSoundEffect.TAP)
        return next
    }

    fun play(effect: GameSoundEffect) {
        if (!_soundEnabled.value) return
        scope.launch {
            runCatching {
                val pcm = when (effect) {
                    GameSoundEffect.TAP -> synthToneSeq(listOf(640f to 36), volume = 0.22f)
                    GameSoundEffect.DICE_TICK -> synthToneSeq(listOf(340f to 24, 520f to 24), volume = 0.20f)
                    GameSoundEffect.MOVE -> synthToneSeq(listOf(440f to 45, 587f to 55), volume = 0.26f)
                    GameSoundEffect.CAPTURE -> synthToneSeq(listOf(780f to 50, 520f to 85), volume = 0.34f)
                    GameSoundEffect.CHECK -> synthToneSeq(listOf(880f to 75, 698f to 75, 988f to 95), volume = 0.32f)
                    GameSoundEffect.SPECIAL -> synthToneSeq(listOf(523f to 55, 659f to 55, 784f to 70, 1046f to 110), volume = 0.30f)
                    GameSoundEffect.WIN -> synthToneSeq(
                        listOf(523.25f to 95, 659.25f to 95, 783.99f to 110, 1046.5f to 220),
                        volume = 0.36f
                    )
                }
                if (pcm.isEmpty()) return@runCatching
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@runCatching
                val byteCount = pcm.size * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, byteCount))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val totalDurationMs = (pcm.size * 1000L) / SAMPLE_RATE + 40L
                kotlinx.coroutines.delay(totalDurationMs)
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }
    }

    private fun synthToneSeq(notes: List<Pair<Float, Int>>, volume: Float): ShortArray {
        val totalSamples = notes.sumOf { (SAMPLE_RATE * it.second) / 1000 }
        if (totalSamples <= 0) return ShortArray(0)
        val out = ShortArray(totalSamples)
        var cursor = 0
        for ((freqHz, durMs) in notes) {
            val count = (SAMPLE_RATE * durMs) / 1000
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toFloat() / count.coerceAtLeast(1)
                val attack = (i / (SAMPLE_RATE * 0.006f)).coerceIn(0f, 1f)
                val decay = exp((-3.2 * progress).toDouble()).toFloat()
                val harmonic = (sin(2.0 * PI * freqHz * t) * 0.78 + sin(4.0 * PI * freqHz * t) * 0.22)
                val sample = (harmonic * volume * attack * decay * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[cursor + i] = sample.toShort()
            }
            cursor += count
        }
        return out
    }
}
