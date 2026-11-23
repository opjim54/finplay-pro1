package com.example.ui.screens.notes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.NoteAiToolAction
import com.example.data.local.ItemReminderEntity
import com.example.data.local.KeepNoteEntity
import com.example.data.local.NoteChecklistItem
import com.example.data.local.NoteVersionEntity
import com.example.domain.DateTimeUtil
import com.example.domain.NoteAttachmentMeta
import com.example.domain.NoteBlock
import com.example.domain.NoteFormat
import com.example.domain.NoteJsonCodec
import com.example.domain.NoteTemplates
import com.example.domain.SmartNoteMediaHelper
import com.example.domain.SmartToolsEngine
import com.example.domain.VoiceNotePlayer
import com.example.domain.VoiceNoteRecorder
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import com.example.ui.screens.MarkdownLinesRenderer
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.util.UUID

val NOTE_SWATCHES = listOf(
    0xFFFFFFFF,
    0xFFD1FAE5,
    0xFFFEF3C7,
    0xFFDBEAFE,
    0xFFFCE7F3,
    0xFFEDE9FE,
    0xFFFEE2E2,
    0xFF0F2D24
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorSheet(
    userId: Long,
    initial: KeepNoteEntity?,
    initialReminders: List<ItemReminderEntity>,
    versionsFlow: Flow<List<NoteVersionEntity>>?,
    onRunAiAction: suspend (NoteAiToolAction, String, String, String) -> Pair<String, String?>,
    onCreateTasksFromAi: (List<String>) -> Unit,
    onSaveReminder: (ItemReminderEntity) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onRestoreVersion: (NoteVersionEntity) -> Unit,
    onSave: (KeepNoteEntity, List<ItemReminderEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val isBn = LocalAppLanguage.current == AppLanguage.BANGLA
    val context = LocalContext.current
    val financeColors = LocalFinanceColors.current

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var tagsCsv by remember { mutableStateOf(initial?.tagsCsv ?: "") }
    var urlLink by remember { mutableStateOf(initial?.urlLink ?: "") }
    var noteFormat by remember {
        mutableStateOf(
            NoteFormat.entries.firstOrNull { it.name == (initial?.noteFormat ?: "RICH") } ?: NoteFormat.RICH
        )
    }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: 0xFFFFFFFF) }
    var isPinned by remember { mutableStateOf(initial?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(initial?.isFavorite ?: false) }
    var isLocked by remember { mutableStateOf(initial?.isLocked ?: false) }
    var showMarkdownPreview by remember { mutableStateOf(false) }

    val checklist = remember {
        mutableStateListOf<NoteChecklistItem>().apply {
            addAll(NoteJsonCodec.decodeChecklist(initial?.checklistJson ?: "[]"))
        }
    }
    val blocks = remember {
        mutableStateListOf<NoteBlock>().apply {
            addAll(NoteJsonCodec.decodeBlocks(initial?.blocksJson ?: "[]"))
        }
    }
    val attachments = remember {
        mutableStateListOf<NoteAttachmentMeta>().apply {
            addAll(NoteJsonCodec.decodeAttachments(initial?.attachmentsJson ?: "[]"))
        }
    }

    var newCheckText by remember { mutableStateOf("") }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showDrawingDialog by remember { mutableStateOf(false) }
    var playingVideoPath by remember { mutableStateOf<String?>(null) }
    val pendingReminders = remember { mutableStateListOf<ItemReminderEntity>() }

    // Voice Recorder & Player + Media/Validation state
    val voiceRecorder = remember { VoiceNoteRecorder() }
    val voicePlayer = remember { VoiceNotePlayer() }
    var isRecording by remember { mutableStateOf(false) }
    var activeVoicePath by remember { mutableStateOf<String?>(null) }
    var mediaErrorMsg by remember { mutableStateOf<String?>(null) }
    var emptyNoteError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.stopSilently()
            voicePlayer.stop()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.start(context)
                .onSuccess {
                    mediaErrorMsg = null
                    isRecording = true
                }
                .onFailure { err ->
                    mediaErrorMsg = err.message ?: if (isBn) "ভয়েস রেকর্ড শুরু করা যায়নি" else "Microphone unavailable"
                }
        } else {
            mediaErrorMsg = if (isBn) "অডিও রেকর্ডের অনুমতি প্রয়োজন" else "Microphone permission is required"
        }
    }

    val multiImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { uri ->
            SmartNoteMediaHelper.copyUriToInternalMedia(context, uri, "IMAGE", "jpg")?.let {
                attachments.add(it)
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            SmartNoteMediaHelper.copyUriToInternalMedia(context, uri, "VIDEO", "mp4")?.let {
                attachments.add(it)
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            SmartNoteMediaHelper.copyUriToInternalMedia(context, uri, "DOCUMENT", "pdf")?.let {
                attachments.add(it)
            }
        }
    }

    val combinedWordText = remember(title, content, checklist.toList(), blocks.toList()) {
        buildString {
            append(title).append(' ').append(content).append(' ')
            checklist.forEach { append(it.text).append(' ') }
            blocks.forEach { append(it.content).append(' ') }
        }.trim()
    }
    val stats = remember(combinedWordText) { SmartToolsEngine.analyzeText(combinedWordText) }
    val safeVersionsFlow = remember(versionsFlow) { versionsFlow ?: flowOf(emptyList()) }
    val versions by safeVersionsFlow.collectAsStateWithLifecycle(emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top bar: Actions + Security + AI + Templates + Versions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initial == null) strings.addNote else strings.editNote,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = strings.pinNote,
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite note",
                            tint = if (isFavorite) financeColors.champagneGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock note",
                            tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (initial != null && versions.isNotEmpty()) {
                        IconButton(onClick = { showVersionHistory = true }) {
                            Icon(Icons.Default.History, contentDescription = "Version history")
                        }
                    }
                }
            }

            // Templates, AI Tools, & Reminders Row
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { showAiSheet = true },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = financeColors.champagneGold) },
                    label = { Text("AI Tools (12)") }
                )
                FilterChip(
                    selected = false,
                    onClick = { showTemplatesDialog = true },
                    label = { Text("Templates") }
                )
                FilterChip(
                    selected = showMarkdownPreview,
                    onClick = { showMarkdownPreview = !showMarkdownPreview },
                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text(if (showMarkdownPreview) "Edit Mode" else "Preview") }
                )
                FilterChip(
                    selected = false,
                    onClick = { showReminderDialog = true },
                    leadingIcon = { Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("Reminder (${initialReminders.size + pendingReminders.size})") }
                )
            }

            // Format Type Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(NoteFormat.entries) { fmt ->
                    FilterChip(
                        selected = noteFormat == fmt,
                        onClick = { noteFormat = fmt },
                        label = { Text(fmt.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) emptyNoteError = false
                },
                label = { Text(strings.noteTitle) },
                isError = emptyNoteError,
                supportingText = if (emptyNoteError) {
                    { Text(if (isBn) "শিরোনাম বা বিবরণ লিখুন অথবা চেকলিস্ট/মিডিয়া যোগ করুন" else "Add a title, content, checklist, or attachment") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("keep_note_title_input")
            )

            // Rich Text Formatting Toolbar
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { content = if (content.isBlank()) "# Heading 1\n" else "$content\n# " }) {
                    Text("H1", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { content = if (content.isBlank()) "## Heading 2\n" else "$content\n## " }) {
                    Text("H2", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { content = "$content **bold**" }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = { content = "$content *italic*" }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = { content = "$content __underline__" }) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = {
                    blocks.add(NoteBlock(type = "CODE", language = "kotlin", content = "// snippet"))
                }) {
                    Icon(Icons.Default.Code, contentDescription = "Add Code Block", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = {
                    blocks.add(
                        NoteBlock(
                            type = "TABLE",
                            rows = listOf(listOf("Column A", "Column B"), listOf("Item 1", "100"))
                        )
                    )
                }) {
                    Icon(Icons.Default.TableChart, contentDescription = "Add Table", modifier = Modifier.size(16.dp))
                }
            }

            AnimatedContent(targetState = showMarkdownPreview, label = "note_preview_toggle") { preview ->
                if (preview) {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        MarkdownLinesRenderer(content.ifBlank { if (isBn) "(নোট ফাঁকা)" else "(Empty note)" })
                    }
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            if (it.isNotBlank()) emptyNoteError = false
                        },
                        label = { Text(strings.noteBody) },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("keep_note_content_input")
                    )
                }
            }

            // Live Word & Character Counter Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = "${strings.wordCountLabel}: ${stats.words}  •  ${strings.charCountLabel}: ${stats.charsWithSpaces} (${stats.charsWithoutSpaces} no spaces)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("keep_note_word_char_counter")
                )
            }

            // URL Note input
            if (noteFormat == NoteFormat.URL || urlLink.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = urlLink,
                        onValueChange = { urlLink = it },
                        label = { Text("Website URL") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (urlLink.isNotBlank()) {
                        IconButton(onClick = {
                            val formatted = if (urlLink.startsWith("http")) urlLink else "https://$urlLink"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formatted))) }
                        }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open URL")
                        }
                    }
                }
            }

            // Interactive Code & Table Blocks
            blocks.forEachIndexed { bIdx, block ->
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${block.type} Block", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { blocks.removeAt(bIdx) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove block", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (block.type == "TABLE") {
                        block.rows.forEachIndexed { rIdx, rowCells ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                                rowCells.forEachIndexed { cIdx, cell ->
                                    OutlinedTextField(
                                        value = cell,
                                        onValueChange = { newVal ->
                                            val updatedRows = block.rows.mapIndexed { ri, rList ->
                                                if (ri == rIdx) rList.mapIndexed { ci, cv -> if (ci == cIdx) newVal else cv }
                                                else rList
                                            }
                                            blocks[bIdx] = block.copy(rows = updatedRows)
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        TextButton(onClick = {
                            val cols = block.rows.firstOrNull()?.size ?: 2
                            blocks[bIdx] = block.copy(rows = block.rows + listOf(List(cols) { "" }))
                        }) { Text("+ Add Table Row") }
                    } else {
                        OutlinedTextField(
                            value = block.content,
                            onValueChange = { blocks[bIdx] = block.copy(content = it) },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Checklist Items
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCheckText,
                    onValueChange = { newCheckText = it },
                    label = { Text(strings.addChecklistItem) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (newCheckText.isNotBlank()) {
                        checklist.add(NoteChecklistItem(UUID.randomUUID().toString(), newCheckText.trim(), false))
                        newCheckText = ""
                    }
                }) { Icon(Icons.Default.Add, contentDescription = "Add checklist item") }
            }
            checklist.forEachIndexed { idx, item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { checklist[idx] = item.copy(checked = it) }
                    )
                    Text(item.text, modifier = Modifier.weight(1f))
                    IconButton(onClick = { checklist.removeAt(idx) }) {
                        Icon(Icons.Default.Close, contentDescription = strings.delete, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Media & Attachments Bar (Photos, Video, Docs, Voice, Sketch)
            Text("Media, Voice & Sketches", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = {
                    multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Images")
                }
                OutlinedButton(onClick = {
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }) {
                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video")
                }
                OutlinedButton(onClick = { docPicker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("File")
                }
                OutlinedButton(onClick = { showDrawingDialog = true }) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Draw")
                }
                Button(onClick = {
                    if (isRecording) {
                        isRecording = false
                        val savedVoice = voiceRecorder.stop()
                        if (savedVoice != null) {
                            attachments.add(savedVoice)
                            mediaErrorMsg = null
                        } else {
                            mediaErrorMsg = if (isBn) "ভয়েস রেকর্ড সংরক্ষণ করা যায়নি" else "Could not save voice recording"
                        }
                    } else {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            voiceRecorder.start(context)
                                .onSuccess {
                                    mediaErrorMsg = null
                                    isRecording = true
                                }
                                .onFailure { err ->
                                    mediaErrorMsg = err.message ?: if (isBn) "মাইক্রোফোন পাওয়া যায়নি" else "Microphone unavailable"
                                }
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }) {
                    Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) (if (isBn) "থামান" else "Stop Voice") else (if (isBn) "ভয়েস" else "Voice"))
                }
            }

            AnimatedVisibility(visible = mediaErrorMsg != null) {
                mediaErrorMsg?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Attachments Preview List
            attachments.forEachIndexed { idx, att ->
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (att.kind == "IMAGE" || att.kind == "DRAWING") {
                            val bmp = remember(att.localPath) { BitmapFactory.decodeFile(att.localPath)?.asImageBitmap() }
                            bmp?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = att.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(att.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(att.kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (att.kind == "VOICE") {
                            val playingThis = activeVoicePath == att.localPath
                            IconButton(onClick = {
                                if (playingThis) {
                                    voicePlayer.stop()
                                    activeVoicePath = null
                                } else {
                                    activeVoicePath = att.localPath
                                    voicePlayer.play(att.localPath) { activeVoicePath = null }
                                }
                            }) {
                                Icon(if (playingThis) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Play voice note")
                            }
                        } else if (att.kind == "VIDEO") {
                            IconButton(onClick = { playingVideoPath = att.localPath }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play video")
                            }
                        }
                        IconButton(onClick = { attachments.removeAt(idx) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove attachment", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Active Reminders on this Note
            initialReminders.forEach { rem ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Reminder: ${DateTimeUtil.formatDateTime(rem.triggerAt)} (${rem.repeatType})",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteReminder(rem.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete reminder", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            pendingReminders.forEachIndexed { rIdx, rem ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Pending Reminder: ${DateTimeUtil.formatDateTime(rem.triggerAt)} (${rem.repeatType})",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingReminders.removeAt(rIdx) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove pending reminder", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Labels + Quick Presets
            OutlinedTextField(
                value = tagsCsv,
                onValueChange = { tagsCsv = it },
                label = { Text(strings.noteTagsHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Personal", "Work", "Study", "Ideas", "Finance").forEach { presetTag ->
                    val currentTags = tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    val has = currentTags.any { it.equals(presetTag, ignoreCase = true) }
                    FilterChip(
                        selected = has,
                        onClick = {
                            val next = if (has) currentTags.filterNot { it.equals(presetTag, ignoreCase = true) }
                            else currentTags + presetTag
                            tagsCsv = next.joinToString(",")
                        },
                        label = { Text(presetTag, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Color Coding Swatches
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(NOTE_SWATCHES) { hex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (colorHex == hex) 2.5.dp else 1.dp,
                                color = if (colorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { colorHex = hex }
                    )
                }
            }

            Button(
                onClick = {
                    val hasAnyData = title.isNotBlank() ||
                        content.isNotBlank() ||
                        urlLink.isNotBlank() ||
                        checklist.isNotEmpty() ||
                        blocks.isNotEmpty() ||
                        attachments.isNotEmpty()
                    if (!hasAnyData) {
                        emptyNoteError = true
                        return@Button
                    }
                    val base = initial ?: KeepNoteEntity(ownerUserId = userId)
                    onSave(
                        base.copy(
                            title = title.trim(),
                            content = content.trim(),
                            tagsCsv = tagsCsv.trim(),
                            colorHex = colorHex,
                            isPinned = isPinned,
                            isFavorite = isFavorite,
                            isLocked = isLocked,
                            noteFormat = noteFormat.name,
                            urlLink = urlLink.trim(),
                            checklistJson = NoteJsonCodec.encodeChecklist(checklist),
                            blocksJson = NoteJsonCodec.encodeBlocks(blocks),
                            attachmentsJson = NoteJsonCodec.encodeAttachments(attachments)
                        ),
                        pendingReminders.toList()
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_keep_note_btn")
            ) {
                Text(strings.save)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            title = { Text("Choose Note Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteTemplates.presets.forEach { tpl ->
                        LedgerCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                title = tpl.title
                                content = tpl.content
                                tagsCsv = tpl.tagsCsv
                                if (tpl.checklist.isNotEmpty()) checklist.addAll(tpl.checklist)
                                if (tpl.blocks.isNotEmpty()) blocks.addAll(tpl.blocks)
                                showTemplatesDialog = false
                            }
                        ) {
                            Text(tpl.name, fontWeight = FontWeight.Bold)
                            Text(tpl.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplatesDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showAiSheet) {
        NoteAiToolsSheet(
            noteTitle = title,
            noteContent = content,
            onRunAiAction = onRunAiAction,
            onApplyContent = { content = it },
            onApplyTitle = { title = it },
            onApplyTags = { tagsCsv = it },
            onExtractTasksToList = onCreateTasksFromAi,
            onAddChecklistLines = { lines ->
                lines.forEach { checklist.add(NoteChecklistItem(UUID.randomUUID().toString(), it, false)) }
            },
            onDismiss = { showAiSheet = false }
        )
    }

    if (showVersionHistory) {
        AlertDialog(
            onDismissRequest = { showVersionHistory = false },
            title = { Text("Note Version History (${versions.size})") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    versions.forEach { ver ->
                        LedgerCard(modifier = Modifier.fillMaxWidth()) {
                            Text("${ver.changeSummary} • ${DateTimeUtil.formatDateTime(ver.savedAt)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(ver.titleSnapshot.ifBlank { "(Untitled)" }, fontWeight = FontWeight.SemiBold)
                            Text(ver.contentSnapshot.take(120), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            TextButton(onClick = {
                                onRestoreVersion(ver)
                                title = ver.titleSnapshot
                                content = ver.contentSnapshot
                                checklist.clear()
                                checklist.addAll(NoteJsonCodec.decodeChecklist(ver.checklistJsonSnapshot))
                                blocks.clear()
                                blocks.addAll(NoteJsonCodec.decodeBlocks(ver.blocksJsonSnapshot))
                                showVersionHistory = false
                            }) { Text(if (isBn) "এই ভার্সন রিস্টোর করুন" else "Restore This Version") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVersionHistory = false }) { Text("Close") } }
        )
    }

    if (showReminderDialog) {
        ReminderPickerDialog(
            userId = userId,
            targetType = "NOTE",
            targetId = initial?.id ?: 0L,
            itemTitle = title.ifBlank { "Note Reminder" },
            onSaveReminder = { rem ->
                if (initial != null && initial.id > 0L) {
                    onSaveReminder(rem)
                } else {
                    pendingReminders.add(rem)
                }
            },
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showDrawingDialog) {
        DrawingPadDialog(
            onSaveBitmap = { bmp ->
                SmartNoteMediaHelper.saveDrawingBitmap(context, bmp)?.let { attachments.add(it) }
                showDrawingDialog = false
            },
            onDismiss = { showDrawingDialog = false }
        )
    }

    playingVideoPath?.let { videoPath ->
        AlertDialog(
            onDismissRequest = { playingVideoPath = null },
            title = { Text("Video Preview") },
            text = {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setOnErrorListener { _, _, _ ->
                                playingVideoPath = null
                                mediaErrorMsg = if (isBn) "ভিডিও প্লে করা যায়নি" else "Unable to play video"
                                true
                            }
                            setVideoURI(Uri.fromFile(File(videoPath)))
                            val mc = MediaController(ctx)
                            mc.setAnchorView(this)
                            setMediaController(mc)
                            start()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
            },
            confirmButton = { TextButton(onClick = { playingVideoPath = null }) { Text("Close") } }
        )
    }
}

private data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float
)

@Composable
private fun DrawingPadDialog(
    onSaveBitmap: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedColor by remember { mutableStateOf(Color(0xFF046C4E)) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 600)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sketch / Drawing Pad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Color(0xFF046C4E), Color.Black, Color(0xFFDC2626), Color(0xFF2563EB), Color(0xFFD97706)).forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (selectedColor == col) 2.5.dp else 1.dp, Color.Gray, CircleShape)
                                .clickable { selectedColor = col }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(selectedColor, strokeWidth) {
                            detectDragGestures(
                                onDragStart = { start -> currentPoints = listOf(start) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        strokes.add(DrawStroke(currentPoints, selectedColor, strokeWidth))
                                        currentPoints = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        (strokes + listOf(DrawStroke(currentPoints, selectedColor, strokeWidth))).forEach { s ->
                            if (s.points.size == 1) {
                                drawCircle(s.color, radius = s.widthPx / 2f, center = s.points.first())
                            } else if (s.points.size >= 2) {
                                val path = Path().apply {
                                    moveTo(s.points.first().x, s.points.first().y)
                                    s.points.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path, s.color, style = Stroke(width = s.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) { Text("Undo") }
                    TextButton(onClick = { strokes.clear() }) { Text("Clear") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val w = canvasSize.width.coerceAtLeast(300)
                val h = canvasSize.height.coerceAtLeast(300)
                val bmp = createBitmap(w, h)
                val c = android.graphics.Canvas(bmp)
                c.drawColor(android.graphics.Color.WHITE)
                strokes.forEach { s ->
                    val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = s.color.toArgb()
                        this.strokeWidth = s.widthPx
                        style = AndroidPaint.Style.STROKE
                        strokeCap = AndroidPaint.Cap.ROUND
                        strokeJoin = AndroidPaint.Join.ROUND
                    }
                    if (s.points.size == 1) {
                        val dotPaint = AndroidPaint(p).apply { style = AndroidPaint.Style.FILL }
                        c.drawCircle(s.points.first().x, s.points.first().y, s.widthPx / 2f, dotPaint)
                    } else {
                        for (i in 1 until s.points.size) {
                            c.drawLine(s.points[i - 1].x, s.points[i - 1].y, s.points[i].x, s.points[i].y, p)
                        }
                    }
                }
                onSaveBitmap(bmp)
            }) { Text("Save Sketch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
