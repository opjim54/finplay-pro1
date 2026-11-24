package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.data.local.NoteChecklistItem
import com.example.data.local.TaskRepeat
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID

enum class NoteFormat(val label: String) {
    RICH("Rich Note"),
    MARKDOWN("Markdown"),
    CHECKLIST("Checklist"),
    URL("Bookmark URL"),
    VOICE("Voice Note"),
    DRAWING("Sketch / Drawing")
}

@Serializable
data class NoteBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "TEXT", // H1, H2, TEXT, CODE, TABLE, DIVIDER
    val content: String = "",
    val language: String = "kotlin",
    val rows: List<List<String>> = emptyList()
)

@Serializable
data class NoteAttachmentMeta(
    val id: String = UUID.randomUUID().toString(),
    val kind: String, // IMAGE, VIDEO, DOCUMENT, VOICE, DRAWING
    val localPath: String,
    val displayName: String,
    val durationMs: Long = 0L,
    val mimeType: String = ""
)

@Serializable
data class TaskSubtaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false
)

data class NoteTemplatePreset(
    val name: String,
    val title: String,
    val content: String,
    val tagsCsv: String,
    val checklist: List<NoteChecklistItem> = emptyList(),
    val blocks: List<NoteBlock> = emptyList(),
    val format: NoteFormat = NoteFormat.RICH
)

object NoteTemplates {
    val presets: List<NoteTemplatePreset> = listOf(
        NoteTemplatePreset(
            name = "Meeting Notes",
            title = "Team Sync & Decisions",
            content = "## Agenda\n- Review project milestones\n- Budget allocation check\n\n## Key Decisions\n- Approved next sprint goals",
            tagsCsv = "Work,Meeting",
            checklist = listOf(
                NoteChecklistItem(UUID.randomUUID().toString(), "Share recap summary", false),
                NoteChecklistItem(UUID.randomUUID().toString(), "Assign follow-up owners", false)
            ),
            blocks = listOf(
                NoteBlock(type = "H1", content = "Team Sync & Decisions"),
                NoteBlock(type = "TEXT", content = "Capture key takeaways, owners, and deadlines.")
            )
        ),
        NoteTemplatePreset(
            name = "Daily Journal",
            title = "Daily Reflection",
            content = "### Top 3 Priorities Today\n1. Complete deep work block\n2. Track every expense\n\n### Gratitude & Highlights\n- ",
            tagsCsv = "Personal,Journal"
        ),
        NoteTemplatePreset(
            name = "Study Guide",
            title = "Lecture & Study Summary",
            content = "## Core Concepts\nSummarize main definitions and formulas here.\n\n## Practice Questions\n- Question 1:\n- Question 2:",
            tagsCsv = "Study,University",
            blocks = listOf(
                NoteBlock(type = "TABLE", rows = listOf(listOf("Topic", "Confidence", "Next Review"), listOf("Chapter 1", "High", "Friday")))
            )
        ),
        NoteTemplatePreset(
            name = "Project Brief",
            title = "Project Launch Blueprint",
            content = "## Objective\nDefine deliverable scope, timeline, and budget cap.\n\n## Architecture & Code",
            tagsCsv = "Work,Ideas",
            blocks = listOf(
                NoteBlock(type = "CODE", language = "kotlin", content = "// Key snippet or API endpoint spec")
            )
        ),
        NoteTemplatePreset(
            name = "Expense Log",
            title = "Trip / Event Expense Sheet",
            content = "Record shared or itemized receipts before adding them to the main ledger.",
            tagsCsv = "Personal,Finance",
            blocks = listOf(
                NoteBlock(type = "TABLE", rows = listOf(listOf("Item", "Paid By", "Amount"), listOf("Transport", "Me", "500"), listOf("Snacks", "Me", "250")))
            )
        )
    )
}

object NoteJsonCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun decodeChecklist(raw: String): List<NoteChecklistItem> = runCatching {
        if (raw.isBlank() || raw == "[]") emptyList()
        else json.decodeFromString(ListSerializer(NoteChecklistItem.serializer()), raw)
    }.getOrDefault(emptyList())

    fun encodeChecklist(items: List<NoteChecklistItem>): String =
        json.encodeToString(ListSerializer(NoteChecklistItem.serializer()), items)

    fun decodeBlocks(raw: String): List<NoteBlock> = runCatching {
        if (raw.isBlank() || raw == "[]") emptyList()
        else json.decodeFromString(ListSerializer(NoteBlock.serializer()), raw)
    }.getOrDefault(emptyList())

    fun encodeBlocks(blocks: List<NoteBlock>): String =
        json.encodeToString(ListSerializer(NoteBlock.serializer()), blocks)

    fun decodeAttachments(raw: String): List<NoteAttachmentMeta> = runCatching {
        if (raw.isBlank() || raw == "[]") emptyList()
        else json.decodeFromString(ListSerializer(NoteAttachmentMeta.serializer()), raw)
    }.getOrDefault(emptyList())

    fun encodeAttachments(items: List<NoteAttachmentMeta>): String =
        json.encodeToString(ListSerializer(NoteAttachmentMeta.serializer()), items)

    fun decodeSubtasks(raw: String): List<TaskSubtaskItem> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        // Support both structured TaskSubtaskItem JSON and legacy plain-text string arrays
        val typed = runCatching {
            json.decodeFromString(ListSerializer(TaskSubtaskItem.serializer()), raw)
        }.getOrNull()
        if (typed != null) return typed
        return runCatching {
            val legacy = json.decodeFromString(ListSerializer(kotlinx.serialization.serializer<String>()), raw)
            legacy.map { title ->
                val done = title.startsWith("[x] ", ignoreCase = true)
                val clean = title.removePrefix("[x] ").removePrefix("[X] ").removePrefix("[ ] ")
                TaskSubtaskItem(title = clean, done = done)
            }
        }.getOrDefault(emptyList())
    }

    fun encodeSubtasks(items: List<TaskSubtaskItem>): String =
        json.encodeToString(ListSerializer(TaskSubtaskItem.serializer()), items)
}

object SmartNoteMediaHelper {
    private fun mediaDir(context: Context): File {
        return File(context.filesDir, "note_media").apply { if (!exists()) mkdirs() }
    }

    fun copyUriToInternalMedia(
        context: Context,
        uri: Uri,
        kind: String,
        fallbackExt: String = "bin"
    ): NoteAttachmentMeta? = runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        var displayName = "${kind.lowercase()}_${System.currentTimeMillis()}.$fallbackExt"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val found = cursor.getString(idx)
                    if (!found.isNullOrBlank()) displayName = found
                }
            }
        }
        val safeName = "${UUID.randomUUID().toString().take(8)}_${displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
        val destFile = File(mediaDir(context), safeName)
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: return null
        NoteAttachmentMeta(
            kind = kind,
            localPath = destFile.absolutePath,
            displayName = displayName,
            mimeType = mime
        )
    }.getOrNull()

    fun saveDrawingBitmap(context: Context, bitmap: Bitmap): NoteAttachmentMeta? = runCatching {
        val file = File(mediaDir(context), "sketch_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        NoteAttachmentMeta(
            kind = "DRAWING",
            localPath = file.absolutePath,
            displayName = file.name,
            mimeType = "image/png"
        )
    }.getOrNull()

    fun createVoiceOutputFile(context: Context): File =
        File(mediaDir(context), "voice_${System.currentTimeMillis()}.m4a")
}

class VoiceNoteRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0L

    fun start(context: Context): Result<File> = runCatching {
        stopSilently()
        val file = SmartNoteMediaHelper.createVoiceOutputFile(context)
        @Suppress("DEPRECATION")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioEncodingBitRate(96_000)
        mr.setAudioSamplingRate(44_100)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()
        recorder = mr
        outputFile = file
        startedAt = System.currentTimeMillis()
        file
    }

    fun stop(): NoteAttachmentMeta? {
        val file = outputFile ?: return null
        val duration = (System.currentTimeMillis() - startedAt).coerceAtLeast(500L)
        return runCatching {
            recorder?.stop()
            recorder?.release()
            recorder = null
            outputFile = null
            if (file.exists() && file.length() > 0L) {
                NoteAttachmentMeta(
                    kind = "VOICE",
                    localPath = file.absolutePath,
                    displayName = "Voice Note (${duration / 1000}s)",
                    durationMs = duration,
                    mimeType = "audio/mp4"
                )
            } else null
        }.getOrElse {
            stopSilently()
            null
        }
    }

    fun stopSilently() {
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        outputFile = null
    }
}

class VoiceNotePlayer {
    private var player: MediaPlayer? = null
    var activePath: String? = null
        private set

    fun play(path: String, onCompleted: () -> Unit) {
        stop()
        runCatching {
            val mp = MediaPlayer()
            mp.setDataSource(path)
            mp.prepare()
            mp.setOnCompletionListener {
                stop()
                onCompleted()
            }
            mp.start()
            player = mp
            activePath = path
        }.onFailure {
            stop()
            onCompleted()
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        activePath = null
    }
}

object TaskRepeatEngine {
    fun advanceDueAt(
        currentDueAt: Long?,
        repeatRule: TaskRepeat,
        repeatInterval: Int = 1,
        repeatUnit: String = "DAY",
        now: Long = System.currentTimeMillis()
    ): Long? {
        if (repeatRule == TaskRepeat.NONE) return null
        val base = currentDueAt ?: now
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        val step = repeatInterval.coerceAtLeast(1)
        when (repeatRule) {
            TaskRepeat.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TaskRepeat.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TaskRepeat.MONTHLY -> cal.add(Calendar.MONTH, 1)
            TaskRepeat.YEARLY -> cal.add(Calendar.YEAR, 1)
            TaskRepeat.CUSTOM -> when (repeatUnit.uppercase()) {
                "WEEK" -> cal.add(Calendar.WEEK_OF_YEAR, step)
                "MONTH" -> cal.add(Calendar.MONTH, step)
                "YEAR" -> cal.add(Calendar.YEAR, step)
                else -> cal.add(Calendar.DAY_OF_YEAR, step)
            }
            TaskRepeat.NONE -> return null
        }
        return cal.timeInMillis
    }

    fun shouldSpawnNextOccurrence(
        repeatRule: TaskRepeat,
        nextDueAt: Long?,
        repeatUntilAt: Long?,
        repeatRemainingCount: Int?
    ): Boolean {
        if (repeatRule == TaskRepeat.NONE || nextDueAt == null) return false
        if (repeatUntilAt != null && nextDueAt > repeatUntilAt) return false
        if (repeatRemainingCount != null && repeatRemainingCount <= 1) return false
        return true
    }

    fun decrementedCount(repeatRemainingCount: Int?): Int? {
        return repeatRemainingCount?.let { (it - 1).coerceAtLeast(0) }
    }
}
