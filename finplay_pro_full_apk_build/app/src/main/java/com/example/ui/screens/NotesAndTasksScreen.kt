package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.NoteAiToolAction
import com.example.data.local.ItemReminderEntity
import com.example.data.local.KeepNoteEntity
import com.example.data.local.NoteVersionEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskListEntity
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.screens.notes.NoteEditorSheet
import com.example.ui.screens.notes.NotesStudioTab
import com.example.ui.screens.notes.PrivateNoteUnlockDialog
import com.example.ui.screens.notes.RemindersAndCalendarTab
import com.example.ui.screens.notes.TaskDetailsSheet
import com.example.ui.screens.notes.TasksProTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun NotesAndTasksScreen(
    userId: Long,
    activeNotes: List<KeepNoteEntity>,
    archivedNotes: List<KeepNoteEntity>,
    trashedNotes: List<KeepNoteEntity>,
    tasks: List<TaskItemEntity>,
    taskLists: List<TaskListEntity>,
    reminders: List<ItemReminderEntity>,
    pinEnabled: Boolean,
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    observeNoteVersions: (Long) -> Flow<List<NoteVersionEntity>>,
    onRunNoteAiAction: suspend (NoteAiToolAction, String, String, String) -> Pair<String, String?>,
    onCreateTasksFromAi: (List<String>, Long, String) -> Unit,
    onSaveNote: (KeepNoteEntity, Boolean, List<ItemReminderEntity>) -> Unit,
    onRestoreNoteVersion: (Long, NoteVersionEntity) -> Unit,
    onBulkUpdateNotes: (List<KeepNoteEntity>) -> Unit,
    onDeleteNoteForever: (Long) -> Unit,
    onSaveTaskList: (TaskListEntity) -> Unit,
    onDeleteTaskList: (Long) -> Unit,
    onSaveTask: (TaskItemEntity, List<ItemReminderEntity>) -> Unit,
    onToggleTaskCompleted: (TaskItemEntity) -> Unit,
    onReorderTasks: (List<TaskItemEntity>) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onSaveReminder: (ItemReminderEntity) -> Unit,
    onSnoozeReminder: (Long, Int) -> Unit,
    onRescheduleReminder1Day: (Long) -> Unit,
    onDismissReminder: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val isBn = LocalAppLanguage.current == AppLanguage.BANGLA
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Notes Studio, 1 = Tasks Pro, 2 = Reminders & Agenda
    var searchQuery by remember { mutableStateOf("") }
    var selectedTaskListId by remember { mutableStateOf<Long?>(null) }

    var editingNote by remember { mutableStateOf<KeepNoteEntity?>(null) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var pendingLockedNote by remember { mutableStateOf<KeepNoteEntity?>(null) }

    var editingTask by remember { mutableStateOf<TaskItemEntity?>(null) }
    var showTaskEditor by remember { mutableStateOf(false) }

    if (selectedSection != 0) {
        BackHandler { selectedSection = 0 }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(reminders) {
        now = System.currentTimeMillis()
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    val missedReminderCount = remember(reminders, now) {
        reminders.count { !it.isDismissed && (it.snoozedUntil ?: it.triggerAt) <= now }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.notesAndTasks,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        if (selectedSection == 1) {
                            editingTask = null
                            showTaskEditor = true
                        } else {
                            editingNote = null
                            showNoteEditor = true
                        }
                    },
                    modifier = Modifier.testTag("add_note_or_task_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectedSection == 1) strings.addTask else strings.addNote)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                    icon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_keep_notes")
                ) { Text("${if (isBn) "নোটস" else "Notes"} (${activeNotes.size})", maxLines = 1) }

                SegmentedButton(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_good_tasks")
                ) { Text("${if (isBn) "টাস্ক" else "Tasks"} (${tasks.count { !it.isCompleted }})", maxLines = 1) }

                SegmentedButton(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_reminders_agenda")
                ) {
                    val base = if (isBn) "অ্যালার্ট" else "Alerts"
                    val label = if (missedReminderCount > 0) "$base ($missedReminderCount!)" else "$base (${reminders.size})"
                    Text(label, maxLines = 1)
                }
            }

            if (selectedSection != 2) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = strings.cancel)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("notes_tasks_search_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = selectedSection,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))) togetherWith
                        fadeOut(tween(140))
                },
                label = "notes_tasks_section",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { section ->
                when (section) {
                    0 -> NotesStudioTab(
                        activeNotes = activeNotes,
                        archivedNotes = archivedNotes,
                        trashedNotes = trashedNotes,
                        reminders = reminders,
                        searchQuery = searchQuery,
                        onOpenNote = { note ->
                            if (note.isLocked) {
                                pendingLockedNote = note
                            } else {
                                editingNote = note
                                showNoteEditor = true
                            }
                        },
                        onSaveNote = { n -> onSaveNote(n, false, emptyList()) },
                        onBulkUpdateNotes = onBulkUpdateNotes,
                        onDeleteNoteForever = onDeleteNoteForever
                    )

                    1 -> TasksProTab(
                        userId = userId,
                        tasks = tasks,
                        taskLists = taskLists,
                        reminders = reminders,
                        searchQuery = searchQuery,
                        selectedListId = selectedTaskListId,
                        onSelectListId = { selectedTaskListId = it },
                        onOpenTask = { task ->
                            editingTask = task
                            showTaskEditor = true
                        },
                        onToggleTaskCompleted = onToggleTaskCompleted,
                        onSaveTask = { t -> onSaveTask(t, emptyList()) },
                        onReorderTasks = onReorderTasks,
                        onDeleteTask = onDeleteTask,
                        onSaveTaskList = onSaveTaskList,
                        onDeleteTaskList = onDeleteTaskList
                    )

                    else -> RemindersAndCalendarTab(
                        reminders = reminders,
                        tasks = tasks,
                        onSnoozeReminder = onSnoozeReminder,
                        onReschedule1Day = onRescheduleReminder1Day,
                        onDismissReminder = onDismissReminder,
                        onDeleteReminder = onDeleteReminder,
                        onToggleTask = onToggleTaskCompleted,
                        onOpenTask = { task ->
                            editingTask = task
                            showTaskEditor = true
                        }
                    )
                }
            }
        }
    }

    pendingLockedNote?.let { locked ->
        PrivateNoteUnlockDialog(
            pinEnabled = pinEnabled,
            biometricEnabled = biometricEnabled,
            onVerifyPin = onVerifyPin,
            onSuccess = {
                editingNote = locked
                pendingLockedNote = null
                showNoteEditor = true
            },
            onDismiss = { pendingLockedNote = null }
        )
    }

    if (showNoteEditor) {
        val currentNoteId = editingNote?.id ?: 0L
        val noteReminders = remember(reminders, currentNoteId) {
            if (currentNoteId > 0L) reminders.filter { it.targetType == "NOTE" && it.targetId == currentNoteId && !it.isDismissed }
            else emptyList()
        }
        val versionsFlow = remember(currentNoteId) {
            if (currentNoteId > 0L) observeNoteVersions(currentNoteId) else null
        }
        val defaultTaskList = taskLists.firstOrNull { it.id == selectedTaskListId } ?: taskLists.firstOrNull()
        NoteEditorSheet(
            userId = userId,
            initial = editingNote,
            initialReminders = noteReminders,
            versionsFlow = versionsFlow,
            onRunAiAction = onRunNoteAiAction,
            onCreateTasksFromAi = { lines ->
                onCreateTasksFromAi(
                    lines,
                    defaultTaskList?.id ?: 1L,
                    defaultTaskList?.name ?: "My Tasks"
                )
            },
            onSaveReminder = onSaveReminder,
            onDeleteReminder = onDeleteReminder,
            onRestoreVersion = { ver ->
                if (currentNoteId > 0L) onRestoreNoteVersion(currentNoteId, ver)
            },
            onSave = { savedNote, pendingRems ->
                onSaveNote(savedNote, true, pendingRems)
                showNoteEditor = false
            },
            onDismiss = { showNoteEditor = false }
        )
    }

    if (showTaskEditor) {
        val currentTaskId = editingTask?.id ?: 0L
        val taskReminders = remember(reminders, currentTaskId) {
            if (currentTaskId > 0L) reminders.filter { it.targetType == "TASK" && it.targetId == currentTaskId && !it.isDismissed }
            else emptyList()
        }
        TaskDetailsSheet(
            userId = userId,
            initial = editingTask,
            taskLists = taskLists,
            initialReminders = taskReminders,
            onSaveReminder = onSaveReminder,
            onDeleteReminder = onDeleteReminder,
            onSave = { savedTask, pendingRems ->
                onSaveTask(savedTask, pendingRems)
                showTaskEditor = false
            },
            onDismiss = { showTaskEditor = false }
        )
    }
}
