package com.example.ui.screens.notes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.ai.NoteAiToolAction
import com.example.data.local.ItemReminderEntity
import com.example.data.local.TaskItemEntity
import com.example.domain.DateTimeUtil
import com.example.i18n.LocalStrings
import com.example.security.BiometricHelper
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LedgerCard
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteAiToolsSheet(
    noteTitle: String,
    noteContent: String,
    onRunAiAction: suspend (NoteAiToolAction, String, String, String) -> Pair<String, String?>,
    onApplyContent: (String) -> Unit,
    onApplyTitle: (String) -> Unit,
    onApplyTags: (String) -> Unit,
    onExtractTasksToList: (List<String>) -> Unit,
    onAddChecklistLines: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val financeColors = LocalFinanceColors.current
    var selectedAction by remember { mutableStateOf(NoteAiToolAction.SUMMARIZE) }
    var extraInput by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var outputText by remember { mutableStateOf("") }
    var servedByBadge by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = financeColors.champagneGold)
                Text("AI Note Assistant (12 Tools)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NoteAiToolAction.entries.forEach { act ->
                    FilterChip(
                        selected = selectedAction == act,
                        onClick = { selectedAction = act },
                        label = { Text(act.title, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Text(selectedAction.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (selectedAction == NoteAiToolAction.GENERATE_NOTE ||
                selectedAction == NoteAiToolAction.TRANSLATE ||
                selectedAction == NoteAiToolAction.ASK_YOUR_NOTES
            ) {
                OutlinedTextField(
                    value = extraInput,
                    onValueChange = { extraInput = it },
                    label = {
                        Text(
                            when (selectedAction) {
                                NoteAiToolAction.GENERATE_NOTE -> "Topic or outline prompt"
                                NoteAiToolAction.TRANSLATE -> "Target language (e.g., Bangla / English)"
                                else -> "Ask a question across your saved notes"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        val (resText, badge) = onRunAiAction(selectedAction, noteTitle, noteContent, extraInput)
                        outputText = resText
                        servedByBadge = badge
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("run_note_ai_action_btn")
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Run ${selectedAction.title}")
            }

            if (outputText.isNotBlank()) {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    servedByBadge?.let { b ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = b,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    OutlinedTextField(
                        value = outputText,
                        onValueChange = { outputText = it },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (selectedAction) {
                            NoteAiToolAction.GENERATE_TITLE -> Button(onClick = { onApplyTitle(outputText.trim()); onDismiss() }) {
                                Text("Use Title")
                            }
                            NoteAiToolAction.SMART_TAGS -> Button(onClick = { onApplyTags(outputText.trim()); onDismiss() }) {
                                Text("Apply Tags")
                            }
                            NoteAiToolAction.EXTRACT_TASKS -> Button(onClick = {
                                val lines = outputText.lines().map { it.trim().removePrefix("• ").removePrefix("- ") }.filter { it.isNotEmpty() }
                                onExtractTasksToList(lines)
                                onDismiss()
                            }) { Text("Create Good Tasks") }
                            NoteAiToolAction.GENERATE_CHECKLIST -> Button(onClick = {
                                val lines = outputText.lines().map { it.trim().removePrefix("• ").removePrefix("- ") }.filter { it.isNotEmpty() }
                                onAddChecklistLines(lines)
                                onDismiss()
                            }) { Text("Add to Checklist") }
                            else -> Button(onClick = { onApplyContent(outputText); onDismiss() }) {
                                Text("Apply to Note")
                            }
                        }
                        OutlinedButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderPickerDialog(
    userId: Long,
    targetType: String,
    targetId: Long,
    itemTitle: String,
    baseDueAt: Long? = null,
    onSaveReminder: (ItemReminderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cal = remember {
        Calendar.getInstance().apply {
            timeInMillis = baseDueAt ?: (System.currentTimeMillis() + 3_600_000L)
        }
    }
    var pickedTimeMillis by remember { mutableLongStateOf(cal.timeInMillis) }
    var offsetMinutes by remember { mutableIntStateOf(0) }
    var repeatType by remember { mutableStateOf("NONE") }
    var customIntervalDays by remember { mutableStateOf("3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(itemTitle.ifBlank { targetType }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = pickedTimeMillis }
                            DatePickerDialog(context, { _, y, m, d ->
                                c.set(y, m, d)
                                pickedTimeMillis = c.timeInMillis
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(DateTimeUtil.formatDate(pickedTimeMillis))
                    }
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = pickedTimeMillis }
                            TimePickerDialog(context, { _, h, min ->
                                c.set(Calendar.HOUR_OF_DAY, h)
                                c.set(Calendar.MINUTE, min)
                                c.set(Calendar.SECOND, 0)
                                pickedTimeMillis = c.timeInMillis
                            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(DateTimeUtil.formatTime(pickedTimeMillis))
                    }
                }

                Text("Notify Offset", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "At time", 5 to "5m early", 15 to "15m early", 30 to "30m early", 60 to "1h early", 1440 to "1d early").forEach { (mins, lbl) ->
                        FilterChip(selected = offsetMinutes == mins, onClick = { offsetMinutes = mins }, label = { Text(lbl) })
                    }
                }

                Text("Repeat Rule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "CUSTOM").forEach { rep ->
                        FilterChip(selected = repeatType == rep, onClick = { repeatType = rep }, label = { Text(rep) })
                    }
                }
                if (repeatType == "CUSTOM") {
                    OutlinedTextField(
                        value = customIntervalDays,
                        onValueChange = { customIntervalDays = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Repeat every (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val triggerAt = (pickedTimeMillis - offsetMinutes * 60_000L).coerceAtLeast(System.currentTimeMillis() + 5_000L)
                onSaveReminder(
                    ItemReminderEntity(
                        ownerUserId = userId,
                        targetType = targetType,
                        targetId = targetId,
                        titleSnapshot = itemTitle.ifBlank { "$targetType Alert" },
                        triggerAt = triggerAt,
                        offsetMinutes = offsetMinutes,
                        repeatType = repeatType,
                        repeatIntervalDays = (customIntervalDays.toIntOrNull() ?: 1).coerceAtLeast(1)
                    )
                )
                onDismiss()
            }) { Text("Save Reminder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersAndCalendarTab(
    reminders: List<ItemReminderEntity>,
    tasks: List<TaskItemEntity>,
    onSnoozeReminder: (Long, Int) -> Unit,
    onReschedule1Day: (Long) -> Unit,
    onDismissReminder: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit,
    onOpenTask: (TaskItemEntity) -> Unit
) {
    val now = System.currentTimeMillis()
    val financeColors = LocalFinanceColors.current
    val missedOrDue = remember(reminders, now) {
        reminders.filter { !it.isDismissed && (it.snoozedUntil ?: it.triggerAt) <= now }
    }
    val upcomingReminders = remember(reminders, now) {
        reminders.filter { !it.isDismissed && (it.snoozedUntil ?: it.triggerAt) > now }
    }
    val datedTasks = remember(tasks) {
        tasks.filter { !it.isCompleted && it.dueAt != null }.sortedBy { it.dueAt }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (missedOrDue.isNotEmpty()) {
            item {
                Text(
                    "Missed & Overdue Reminders (${missedOrDue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(missedOrDue, key = { "missed_${it.id}" }) { rem ->
                LedgerCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rem.titleSnapshot.ifBlank { rem.targetType }, fontWeight = FontWeight.Bold)
                            Text(
                                "${rem.targetType} • Due ${DateTimeUtil.formatDateTime(rem.triggerAt)}" +
                                    if (rem.escalationLevel > 0) " • ESCALATED L${rem.escalationLevel}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = { onSnoozeReminder(rem.id, 10) }) {
                            Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("10m")
                        }
                        OutlinedButton(onClick = { onSnoozeReminder(rem.id, 30) }) { Text("30m") }
                        OutlinedButton(onClick = { onReschedule1Day(rem.id) }) { Text("+1 Day") }
                        Button(onClick = { onDismissReminder(rem.id) }) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done")
                        }
                    }
                }
            }
        }

        item {
            Text("Upcoming Scheduled Reminders (${upcomingReminders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (upcomingReminders.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Alarm,
                    title = "No Pending Reminders",
                    subtitle = "Attach one or multiple reminders from any Note or Task."
                )
            }
        } else {
            items(upcomingReminders, key = { "up_${it.id}" }) { rem ->
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = financeColors.champagneGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rem.titleSnapshot, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${rem.targetType} • ${DateTimeUtil.formatDateTime(rem.snoozedUntil ?: rem.triggerAt)} (${rem.repeatType})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteReminder(rem.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete reminder", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Task Calendar Agenda (${datedTasks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (datedTasks.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.CalendarMonth,
                    title = "No Scheduled Tasks",
                    subtitle = "Set a Due Date & Time on any Task to see it in your Agenda."
                )
            }
        } else {
            items(datedTasks, key = { "agenda_${it.id}" }) { task ->
                val overdue = (task.dueAt ?: Long.MAX_VALUE) < now
                LedgerCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTask(task) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Bold)
                            Text(
                                "${task.categoryLabel} • ${DateTimeUtil.formatDateTime(task.dueAt!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(onClick = { onToggleTask(task) }) { Text("Done") }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun PrivateNoteUnlockDialog(
    pinEnabled: Boolean,
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val activity = context as? FragmentActivity
    val bioAvailable = remember(activity) { activity != null && BiometricHelper.isBiometricAvailable(activity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Unlock Private Note", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This note is protected. Verify your App PIN or Biometric to view or edit.", style = MaterialTheme.typography.bodySmall)
                if (pinEnabled) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it.filter { c -> c.isDigit() }.take(6)
                            errorMsg = null
                        },
                        label = { Text(strings.enterPinPrompt) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Set an App PIN or Biometric lock in Settings for extra security, or tap Unlock below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (bioAvailable && (biometricEnabled || !pinEnabled) && activity != null) {
                    OutlinedButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = "Unlock Private Note",
                                subtitle = "Authenticate to open locked note",
                                negativeText = strings.cancel,
                                onSuccess = onSuccess,
                                onError = { errorMsg = it }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.useBiometric)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!pinEnabled) {
                    onSuccess()
                } else if (onVerifyPin(pinInput)) {
                    onSuccess()
                } else {
                    errorMsg = strings.wrongPinError
                }
            }) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
