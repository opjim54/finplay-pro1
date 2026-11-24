package com.example.ui.screens.notes

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ItemReminderEntity
import com.example.data.local.KeepNoteEntity
import com.example.domain.NoteFormat
import com.example.domain.NoteJsonCodec
import com.example.domain.SmartToolsEngine
import com.example.i18n.LocalStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LedgerCard
import com.example.ui.theme.LocalFinanceColors

enum class NoteBucketFilter(val label: String) {
    ACTIVE("Active"),
    PINNED("Pinned"),
    FAVORITES("Starred"),
    LOCKED("Locked"),
    ARCHIVE("Archive"),
    TRASH("Trash")
}

enum class NoteSortOrder(val label: String) {
    PINNED_MODIFIED("Pinned + Modified"),
    MODIFIED_DESC("Newest Edited"),
    CREATED_DESC("Newest Created"),
    TITLE_ASC("Title A-Z")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesStudioTab(
    activeNotes: List<KeepNoteEntity>,
    archivedNotes: List<KeepNoteEntity>,
    trashedNotes: List<KeepNoteEntity>,
    reminders: List<ItemReminderEntity>,
    searchQuery: String,
    onOpenNote: (KeepNoteEntity) -> Unit,
    onSaveNote: (KeepNoteEntity) -> Unit,
    onBulkUpdateNotes: (List<KeepNoteEntity>) -> Unit,
    onDeleteNoteForever: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var bucketFilter by remember { mutableStateOf(NoteBucketFilter.ACTIVE) }
    var selectedLabel by remember { mutableStateOf("All") }
    var selectedColorFilter by remember { mutableStateOf<Long?>(null) }
    var sortOrder by remember { mutableStateOf(NoteSortOrder.PINNED_MODIFIED) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { mutableStateListOf<Long>() }
    var showBulkColorDialog by remember { mutableStateOf(false) }
    var showBulkLabelDialog by remember { mutableStateOf(false) }
    var bulkLabelInput by remember { mutableStateOf("") }

    val availableLabels = remember(activeNotes, archivedNotes) {
        val defaults = listOf("Personal", "Work", "Study", "Ideas", "Finance")
        val extracted = (activeNotes + archivedNotes)
            .flatMap { it.tagsCsv.split(',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        (listOf("All") + (defaults + extracted).distinct())
    }

    val baseNotes = remember(bucketFilter, activeNotes, archivedNotes, trashedNotes) {
        when (bucketFilter) {
            NoteBucketFilter.ACTIVE -> activeNotes
            NoteBucketFilter.PINNED -> activeNotes.filter { it.isPinned }
            NoteBucketFilter.FAVORITES -> activeNotes.filter { it.isFavorite }
            NoteBucketFilter.LOCKED -> (activeNotes + archivedNotes).filter { it.isLocked }
            NoteBucketFilter.ARCHIVE -> archivedNotes
            NoteBucketFilter.TRASH -> trashedNotes
        }
    }

    val filteredNotes = remember(baseNotes, searchQuery, selectedLabel, selectedColorFilter, sortOrder) {
        val q = searchQuery.trim()
        val matching = baseNotes.filter { n ->
            val qMatch = q.isEmpty() ||
                n.title.contains(q, true) ||
                n.content.contains(q, true) ||
                n.tagsCsv.contains(q, true) ||
                n.checklistJson.contains(q, true) ||
                n.urlLink.contains(q, true) ||
                n.noteFormat.contains(q, true)
            val labelMatch = selectedLabel == "All" ||
                n.tagsCsv.split(',').any { it.trim().equals(selectedLabel, ignoreCase = true) }
            val colorMatch = selectedColorFilter == null || n.colorHex == selectedColorFilter
            qMatch && labelMatch && colorMatch
        }
        when (sortOrder) {
            NoteSortOrder.PINNED_MODIFIED -> matching.sortedWith(compareByDescending<KeepNoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
            NoteSortOrder.MODIFIED_DESC -> matching.sortedByDescending { it.updatedAt }
            NoteSortOrder.CREATED_DESC -> matching.sortedByDescending { it.createdAt }
            NoteSortOrder.TITLE_ASC -> matching.sortedBy { it.title.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Buckets + Sort + Multi-Select Controls
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(NoteBucketFilter.entries) { bucket ->
                val count = when (bucket) {
                    NoteBucketFilter.ACTIVE -> activeNotes.size
                    NoteBucketFilter.PINNED -> activeNotes.count { it.isPinned }
                    NoteBucketFilter.FAVORITES -> activeNotes.count { it.isFavorite }
                    NoteBucketFilter.LOCKED -> (activeNotes + archivedNotes).count { it.isLocked }
                    NoteBucketFilter.ARCHIVE -> archivedNotes.size
                    NoteBucketFilter.TRASH -> trashedNotes.size
                }
                FilterChip(
                    selected = bucketFilter == bucket,
                    onClick = {
                        bucketFilter = bucket
                        selectedNoteIds.clear()
                    },
                    label = { Text("${bucket.label} ($count)", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Labels, Color Filter & Sort/Bulk Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(availableLabels) { lbl ->
                    FilterChip(
                        selected = selectedLabel.equals(lbl, ignoreCase = true),
                        onClick = { selectedLabel = lbl },
                        label = { Text(if (lbl == "All") "All Labels" else "#$lbl", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.testTag("notes_sort_btn")
                ) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort notes")
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    NoteSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                sortOrder = order
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedNoteIds.clear()
                },
                modifier = Modifier.testTag("notes_bulk_select_btn")
            ) {
                Icon(
                    Icons.Default.SelectAll,
                    contentDescription = "Bulk select notes",
                    tint = if (selectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Color filter circles
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedColorFilter == null,
                    onClick = { selectedColorFilter = null },
                    label = { Text("All Colors", style = MaterialTheme.typography.labelSmall) }
                )
            }
            items(NOTE_SWATCHES) { swatch ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                        .border(
                            width = if (selectedColorFilter == swatch) 2.5.dp else 1.dp,
                            color = if (selectedColorFilter == swatch) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            selectedColorFilter = if (selectedColorFilter == swatch) null else swatch
                        }
                )
            }
        }

        // Bulk Action Bar when in Multi-Select Mode
        if (selectionMode) {
            LedgerCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedNoteIds.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(onClick = {
                            if (selectedNoteIds.size == filteredNotes.size) selectedNoteIds.clear()
                            else {
                                selectedNoteIds.clear()
                                selectedNoteIds.addAll(filteredNotes.map { it.id })
                            }
                        }) {
                            Text(if (selectedNoteIds.size == filteredNotes.size) "Clear" else "All")
                        }
                        TextButton(onClick = {
                            selectionMode = false
                            selectedNoteIds.clear()
                        }) { Text("Done") }
                    }
                }
                if (selectedNoteIds.isNotEmpty()) {
                    val chosen = filteredNotes.filter { it.id in selectedNoteIds }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = {
                            val pinTarget = chosen.any { !it.isPinned }
                            onBulkUpdateNotes(chosen.map { it.copy(isPinned = pinTarget) })
                            selectedNoteIds.clear()
                        }) { Text("Pin/Unpin") }
                        OutlinedButton(onClick = { showBulkColorDialog = true }) { Text("Color") }
                        OutlinedButton(onClick = { showBulkLabelDialog = true }) { Text("Label") }
                        OutlinedButton(onClick = {
                            val archTarget = chosen.any { !it.isArchived }
                            onBulkUpdateNotes(chosen.map { it.copy(isArchived = archTarget, isPinned = false) })
                            selectedNoteIds.clear()
                        }) { Text(if (bucketFilter == NoteBucketFilter.ARCHIVE) "Unarchive" else "Archive") }
                        OutlinedButton(onClick = {
                            val trashTarget = chosen.any { !it.isTrashed }
                            onBulkUpdateNotes(chosen.map { it.copy(isTrashed = trashTarget, isPinned = false) })
                            selectedNoteIds.clear()
                        }) { Text(if (bucketFilter == NoteBucketFilter.TRASH) "Restore" else "Trash") }
                    }
                }
            }
        }

        // Notes List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredNotes.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Notes,
                        title = strings.keepNotesTab,
                        subtitle = strings.noDataYet
                    )
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    val noteReminders = reminders.count { it.targetType == "NOTE" && it.targetId == note.id && !it.isDismissed }
                    val isSelected = note.id in selectedNoteIds
                    KeepNoteCardPro(
                        note = note,
                        reminderCount = noteReminders,
                        selectionMode = selectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (selectionMode) {
                                if (isSelected) selectedNoteIds.remove(note.id) else selectedNoteIds.add(note.id)
                            } else {
                                onOpenNote(note)
                            }
                        },
                        onLongClick = {
                            selectionMode = true
                            if (!isSelected) selectedNoteIds.add(note.id)
                        },
                        onTogglePin = { onSaveNote(note.copy(isPinned = !note.isPinned)) },
                        onToggleFavorite = { onSaveNote(note.copy(isFavorite = !note.isFavorite)) },
                        onToggleArchive = { onSaveNote(note.copy(isArchived = !note.isArchived, isPinned = false)) },
                        onTrashOrRestore = { onSaveNote(note.copy(isTrashed = !note.isTrashed, isPinned = false)) },
                        onDeleteForever = { onDeleteNoteForever(note.id) },
                        onToggleChecklistItem = { itemId ->
                            val updated = NoteJsonCodec.decodeChecklist(note.checklistJson).map { item ->
                                if (item.id == itemId) item.copy(checked = !item.checked) else item
                            }
                            onSaveNote(note.copy(checklistJson = NoteJsonCodec.encodeChecklist(updated)))
                        }
                    )
                }
            }
        }
    }

    if (showBulkColorDialog) {
        val chosen = filteredNotes.filter { it.id in selectedNoteIds }
        AlertDialog(
            onDismissRequest = { showBulkColorDialog = false },
            title = { Text("Apply Color to ${chosen.size} Notes") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NOTE_SWATCHES.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable {
                                    onBulkUpdateNotes(chosen.map { it.copy(colorHex = hex) })
                                    showBulkColorDialog = false
                                    selectedNoteIds.clear()
                                }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBulkColorDialog = false }) { Text("Close") } }
        )
    }

    if (showBulkLabelDialog) {
        val chosen = filteredNotes.filter { it.id in selectedNoteIds }
        AlertDialog(
            onDismissRequest = { showBulkLabelDialog = false },
            title = { Text("Add Label to ${chosen.size} Notes") },
            text = {
                OutlinedTextField(
                    value = bulkLabelInput,
                    onValueChange = { bulkLabelInput = it },
                    label = { Text("Label (e.g., Work, Study)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val lbl = bulkLabelInput.trim()
                    if (lbl.isNotEmpty()) {
                        onBulkUpdateNotes(
                            chosen.map { n ->
                                val tags = n.tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                val nextCsv = (tags + lbl).distinct().joinToString(",")
                                n.copy(tagsCsv = nextCsv)
                            }
                        )
                        bulkLabelInput = ""
                        selectedNoteIds.clear()
                    }
                    showBulkLabelDialog = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showBulkLabelDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun KeepNoteCardPro(
    note: KeepNoteEntity,
    reminderCount: Int,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onTrashOrRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onToggleChecklistItem: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val financeColors = LocalFinanceColors.current

    val checklist = remember(note.checklistJson) { NoteJsonCodec.decodeChecklist(note.checklistJson) }
    val attachments = remember(note.attachmentsJson) { NoteJsonCodec.decodeAttachments(note.attachmentsJson) }
    val blocks = remember(note.blocksJson) { NoteJsonCodec.decodeBlocks(note.blocksJson) }

    val stats = remember(note.title, note.content, checklist) {
        val combined = buildString {
            append(note.title).append(' ').append(note.content).append(' ')
            checklist.forEach { append(it.text).append(' ') }
        }.trim()
        SmartToolsEngine.analyzeText(combined)
    }

    val isDefaultSwatch = note.colorHex == 0xFFFFFFFFL
    val isDarkSwatch = note.colorHex == 0xFF0F2D24L
    val bgTint = if (isDefaultSwatch) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(note.colorHex).copy(alpha = if (isDarkSwatch) 0.96f else 0.88f)
    }
    val contentTint = when {
        isDefaultSwatch -> MaterialTheme.colorScheme.onSurface
        isDarkSwatch -> Color(0xFFECFDF5)
        else -> Color(0xFF0F172A)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgTint,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectionMode) {
                        Icon(
                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.CheckBox,
                            contentDescription = "Select note",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else contentTint.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (note.isLocked) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked note", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = note.title.ifBlank { strings.editNote },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!note.isTrashed) {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) financeColors.champagneGold else contentTint.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = strings.pinNote,
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else contentTint.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onToggleArchive, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (note.isArchived) strings.unarchiveNote else strings.archiveNote,
                                tint = contentTint.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onTrashOrRestore, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = strings.restore, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = { if (note.isTrashed) onDeleteForever() else onTrashOrRestore() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (note.isLocked) {
                Text(
                    text = "Protected Note — Tap to unlock with PIN or Biometric",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentTint.copy(alpha = 0.88f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.urlLink.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable {
                            val formatted = if (note.urlLink.startsWith("http")) note.urlLink else "https://${note.urlLink}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formatted))) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(note.urlLink, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (checklist.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        checklist.take(4).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { if (!selectionMode) onToggleChecklistItem(item.id) }
                            ) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { if (!selectionMode) onToggleChecklistItem(item.id) },
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentTint,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (checklist.size > 4) {
                            Text("+${checklist.size - 4} more checklist items", style = MaterialTheme.typography.labelSmall, color = contentTint.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Metadata & Attachment Chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val formatLabel = NoteFormat.entries.firstOrNull { it.name == note.noteFormat }?.label ?: note.noteFormat
                Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.09f)) {
                    Text(formatLabel, style = MaterialTheme.typography.labelSmall, color = contentTint, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                if (blocks.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.08f)) {
                        Text("${blocks.size} block(s)", style = MaterialTheme.typography.labelSmall, color = contentTint, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                val voiceCount = attachments.count { it.kind == "VOICE" }
                val sketchCount = attachments.count { it.kind == "DRAWING" }
                val mediaCount = attachments.size - voiceCount - sketchCount
                if (voiceCount > 0) {
                    Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(12.dp), tint = contentTint)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("$voiceCount", style = MaterialTheme.typography.labelSmall, color = contentTint)
                        }
                    }
                }
                if (sketchCount > 0) {
                    Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(12.dp), tint = contentTint)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("$sketchCount", style = MaterialTheme.typography.labelSmall, color = contentTint)
                        }
                    }
                }
                if (mediaCount > 0) {
                    Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(12.dp), tint = contentTint)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("$mediaCount", style = MaterialTheme.typography.labelSmall, color = contentTint)
                        }
                    }
                }
                if (reminderCount > 0) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("$reminderCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                note.tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                    Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.08f)) {
                        Text("#$tag", style = MaterialTheme.typography.labelSmall, color = contentTint, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                if (!note.isLocked) {
                    Surface(shape = RoundedCornerShape(50), color = contentTint.copy(alpha = 0.06f)) {
                        Text(
                            text = "${stats.words}w • ${stats.charsWithSpaces}c",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentTint.copy(alpha = 0.72f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
