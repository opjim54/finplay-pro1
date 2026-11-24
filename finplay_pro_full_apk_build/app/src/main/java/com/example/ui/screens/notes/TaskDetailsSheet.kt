package com.example.ui.screens.notes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.ItemReminderEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskListEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRepeat
import com.example.domain.DateTimeUtil
import com.example.domain.NoteJsonCodec
import com.example.domain.TaskSubtaskItem
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.theme.LocalFinanceColors
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailsSheet(
    userId: Long,
    initial: TaskItemEntity?,
    taskLists: List<TaskListEntity>,
    initialReminders: List<ItemReminderEntity>,
    onSaveReminder: (ItemReminderEntity) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onSave: (TaskItemEntity, List<ItemReminderEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val isBn = LocalAppLanguage.current == AppLanguage.BANGLA
    val context = LocalContext.current
    val financeColors = LocalFinanceColors.current

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: TaskPriority.MEDIUM) }
    var selectedListId by remember { mutableStateOf(initial?.listId ?: (taskLists.firstOrNull()?.id ?: 1L)) }
    var isStarred by remember { mutableStateOf(initial?.isStarred ?: false) }
    var dueAt by remember { mutableStateOf(initial?.dueAt) }

    var repeatRule by remember { mutableStateOf(initial?.repeatRule ?: TaskRepeat.NONE) }
    var repeatIntervalStr by remember { mutableStateOf((initial?.repeatInterval ?: 1).toString()) }
    var repeatUnit by remember { mutableStateOf(initial?.repeatUnit ?: "DAY") }
    var repeatUntilAt by remember { mutableStateOf(initial?.repeatUntilAt) }
    var repeatCountStr by remember { mutableStateOf(initial?.repeatRemainingCount?.toString() ?: "") }

    val subtasks = remember {
        mutableStateListOf<TaskSubtaskItem>().apply {
            addAll(NoteJsonCodec.decodeSubtasks(initial?.subtasksJson ?: "[]"))
        }
    }
    var newSubtask by remember { mutableStateOf("") }
    var showReminderDialog by remember { mutableStateOf(false) }
    val pendingReminders = remember { mutableStateListOf<ItemReminderEntity>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initial == null) strings.addTask else strings.editTask,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { isStarred = !isStarred }) {
                    Icon(
                        if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star task",
                        tint = if (isStarred) financeColors.champagneGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text(strings.taskTitleLabel) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(if (isBn) "টাস্কের শিরোনাম লিখুন" else "Please enter a task title") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task_title_input")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(strings.taskDescLabel) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Task Lists Selection
            Text(if (isBn) "টাস্ক তালিকা (List)" else "Task List", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                taskLists.forEach { lst ->
                    FilterChip(
                        selected = selectedListId == lst.id,
                        onClick = { selectedListId = lst.id },
                        label = { Text(lst.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Priority Chips
            Text(strings.priority, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Due Date & Time Pickers
            Text(strings.dueDateLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dueAt ?: System.currentTimeMillis() }
                        DatePickerDialog(context, { _, y, m, d ->
                            cal.set(y, m, d)
                            dueAt = cal.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dueAt?.let { DateTimeUtil.formatDate(it) } ?: "Pick Date")
                }
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dueAt ?: System.currentTimeMillis() }
                        TimePickerDialog(context, { _, h, min ->
                            cal.set(Calendar.HOUR_OF_DAY, h)
                            cal.set(Calendar.MINUTE, min)
                            cal.set(Calendar.SECOND, 0)
                            dueAt = cal.timeInMillis
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dueAt?.let { DateTimeUtil.formatTime(it) } ?: "Pick Time")
                }
                if (dueAt != null) {
                    IconButton(onClick = { dueAt = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear due date")
                    }
                }
            }

            // Complete Repeat Rules
            Text(strings.repeatLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskRepeat.entries.forEach { rep ->
                    FilterChip(
                        selected = repeatRule == rep,
                        onClick = { repeatRule = rep },
                        label = { Text(rep.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (repeatRule != TaskRepeat.NONE) {
                if (repeatRule == TaskRepeat.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = repeatIntervalStr,
                            onValueChange = { repeatIntervalStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Every N") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        listOf("DAY", "WEEK", "MONTH", "YEAR").forEach { unit ->
                            FilterChip(selected = repeatUnit == unit, onClick = { repeatUnit = unit }, label = { Text(unit) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = repeatUntilAt ?: (System.currentTimeMillis() + 30L * 86_400_000L) }
                            DatePickerDialog(context, { _, y, m, d ->
                                cal.set(y, m, d)
                                repeatUntilAt = cal.timeInMillis
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(repeatUntilAt?.let { "Until ${DateTimeUtil.formatDate(it)}" } ?: "End Date (Optional)")
                    }
                    OutlinedTextField(
                        value = repeatCountStr,
                        onValueChange = { repeatCountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Runs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.75f)
                    )
                }
            }

            // Subtasks Manager with live ratio (e.g., 4/5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.subtasksLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (subtasks.isNotEmpty()) {
                    val doneCount = subtasks.count { it.done }
                    Text(
                        "$doneCount / ${subtasks.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (subtasks.isNotEmpty()) {
                val targetRatio = subtasks.count { it.done }.toFloat() / subtasks.size.coerceAtLeast(1)
                val ratio by animateFloatAsState(
                    targetValue = targetRatio,
                    animationSpec = tween(280),
                    label = "subtask_progress"
                )
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    label = { Text(strings.addSubtaskHint) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (newSubtask.isNotBlank()) {
                        subtasks.add(TaskSubtaskItem(title = newSubtask.trim(), done = false))
                        newSubtask = ""
                    }
                }) { Icon(Icons.Default.Add, contentDescription = "Add subtask") }
            }
            subtasks.forEachIndexed { idx, sub ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = sub.done,
                        onCheckedChange = { subtasks[idx] = sub.copy(done = it) }
                    )
                    Text(
                        text = sub.title,
                        textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { subtasks.removeAt(idx) }) {
                        Icon(Icons.Default.Close, contentDescription = strings.delete, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Multiple Reminders on this Task
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reminders (${initialReminders.size + pendingReminders.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { showReminderDialog = true }) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Reminder")
                }
            }
            initialReminders.forEach { rem ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${DateTimeUtil.formatDateTime(rem.triggerAt)} (${rem.repeatType})",
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
                            "Pending: ${DateTimeUtil.formatDateTime(rem.triggerAt)} (${rem.repeatType})",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { pendingReminders.removeAt(rIdx) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove pending reminder", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val chosenListName = taskLists.firstOrNull { it.id == selectedListId }?.name ?: "My Tasks"
                        val base = initial ?: TaskItemEntity(ownerUserId = userId, title = title.trim())
                        onSave(
                            base.copy(
                                title = title.trim(),
                                description = description.trim(),
                                priority = priority,
                                listId = selectedListId,
                                categoryLabel = chosenListName,
                                isStarred = isStarred,
                                dueAt = dueAt,
                                repeatRule = repeatRule,
                                repeatInterval = (repeatIntervalStr.toIntOrNull() ?: 1).coerceAtLeast(1),
                                repeatUnit = repeatUnit,
                                repeatUntilAt = if (repeatRule != TaskRepeat.NONE) repeatUntilAt else null,
                                repeatRemainingCount = if (repeatRule != TaskRepeat.NONE) repeatCountStr.toIntOrNull() else null,
                                subtasksJson = NoteJsonCodec.encodeSubtasks(subtasks)
                            ),
                            pendingReminders.toList()
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("save_task_btn")
            ) {
                Text(strings.save)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showReminderDialog) {
        ReminderPickerDialog(
            userId = userId,
            targetType = "TASK",
            targetId = initial?.id ?: 0L,
            itemTitle = title.ifBlank { "Task Reminder" },
            baseDueAt = dueAt,
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
}
