package com.example.ui.screens.notes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ItemReminderEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskListEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRepeat
import com.example.domain.DateTimeUtil
import com.example.domain.NoteJsonCodec
import com.example.i18n.LocalStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LedgerCard
import com.example.ui.theme.LocalFinanceColors

enum class TaskViewFilter(val label: String) {
    ALL("Pending"),
    STARRED("Starred"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}

enum class TaskSortMode(val label: String) {
    MANUAL("Manual Order"),
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    STARRED("Starred First"),
    TITLE("Title A-Z")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksProTab(
    userId: Long,
    tasks: List<TaskItemEntity>,
    taskLists: List<TaskListEntity>,
    reminders: List<ItemReminderEntity>,
    searchQuery: String,
    selectedListId: Long?,
    onSelectListId: (Long?) -> Unit,
    onOpenTask: (TaskItemEntity) -> Unit,
    onToggleTaskCompleted: (TaskItemEntity) -> Unit,
    onSaveTask: (TaskItemEntity) -> Unit,
    onReorderTasks: (List<TaskItemEntity>) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onSaveTaskList: (TaskListEntity) -> Unit,
    onDeleteTaskList: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val now = System.currentTimeMillis()
    var viewFilter by remember { mutableStateOf(TaskViewFilter.ALL) }
    var sortMode by remember { mutableStateOf(TaskSortMode.MANUAL) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    var showNewListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    val overdueTasks = remember(tasks, now) {
        tasks.filter { !it.isCompleted && it.dueAt != null && it.dueAt < now }
    }

    val listScopedTasks = remember(tasks, selectedListId) {
        if (selectedListId == null) tasks
        else tasks.filter { it.listId == selectedListId }
    }

    val filteredTasks = remember(listScopedTasks, viewFilter, sortMode, searchQuery, now) {
        val byView = when (viewFilter) {
            TaskViewFilter.ALL -> listScopedTasks.filter { !it.isCompleted }
            TaskViewFilter.STARRED -> listScopedTasks.filter { !it.isCompleted && it.isStarred }
            TaskViewFilter.UPCOMING -> listScopedTasks.filter { !it.isCompleted && it.dueAt != null && it.dueAt >= now }
            TaskViewFilter.OVERDUE -> listScopedTasks.filter { !it.isCompleted && it.dueAt != null && it.dueAt < now }
            TaskViewFilter.COMPLETED -> listScopedTasks.filter { it.isCompleted }
        }
        val q = searchQuery.trim()
        val matched = if (q.isEmpty()) byView else byView.filter {
            it.title.contains(q, true) || it.description.contains(q, true) || it.categoryLabel.contains(q, true)
        }
        when (sortMode) {
            TaskSortMode.MANUAL -> matched.sortedWith(compareBy<TaskItemEntity> { it.manualOrder }.thenBy { it.id })
            TaskSortMode.DUE_DATE -> matched.sortedBy { it.dueAt ?: Long.MAX_VALUE }
            TaskSortMode.PRIORITY -> matched.sortedByDescending {
                when (it.priority) {
                    TaskPriority.URGENT -> 4
                    TaskPriority.HIGH -> 3
                    TaskPriority.MEDIUM -> 2
                    TaskPriority.LOW -> 1
                }
            }
            TaskSortMode.STARRED -> matched.sortedWith(compareByDescending<TaskItemEntity> { it.isStarred }.thenBy { it.manualOrder })
            TaskSortMode.TITLE -> matched.sortedBy { it.title.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Overdue Tasks Spotlight Banner
        if (overdueTasks.isNotEmpty() && viewFilter != TaskViewFilter.COMPLETED) {
            LedgerCard(
                onClick = { viewFilter = TaskViewFilter.OVERDUE },
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${overdueTasks.size} Overdue Task(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Next: ${overdueTasks.first().title}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { viewFilter = TaskViewFilter.OVERDUE }) {
                        Text("Focus")
                    }
                }
            }
        }

        // Custom Task Lists Horizontal Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedListId == null,
                    onClick = { onSelectListId(null) },
                    label = { Text("All Lists (${tasks.count { !it.isCompleted }})", style = MaterialTheme.typography.labelSmall) }
                )
            }
            items(taskLists, key = { it.id }) { lst ->
                val cnt = tasks.count { !it.isCompleted && it.listId == lst.id }
                FilterChip(
                    selected = selectedListId == lst.id,
                    onClick = { onSelectListId(lst.id) },
                    label = { Text("${lst.name} ($cnt)", style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = if (!lst.isSystemDefault) {
                        {
                            IconButton(
                                onClick = {
                                    if (selectedListId == lst.id) onSelectListId(null)
                                    onDeleteTaskList(lst.id)
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete list ${lst.name}", modifier = Modifier.size(13.dp))
                            }
                        }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { showNewListDialog = true },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("New List", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("add_task_list_btn")
                )
            }
        }

        // View Filters & Sort Mode Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(TaskViewFilter.entries) { vf ->
                    val cnt = when (vf) {
                        TaskViewFilter.ALL -> listScopedTasks.count { !it.isCompleted }
                        TaskViewFilter.STARRED -> listScopedTasks.count { !it.isCompleted && it.isStarred }
                        TaskViewFilter.UPCOMING -> listScopedTasks.count { !it.isCompleted && it.dueAt != null && it.dueAt >= now }
                        TaskViewFilter.OVERDUE -> listScopedTasks.count { !it.isCompleted && it.dueAt != null && it.dueAt < now }
                        TaskViewFilter.COMPLETED -> listScopedTasks.count { it.isCompleted }
                    }
                    FilterChip(
                        selected = viewFilter == vf,
                        onClick = { viewFilter = vf },
                        label = { Text("${vf.label} ($cnt)", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Box {
                IconButton(onClick = { sortMenuOpen = true }, modifier = Modifier.testTag("tasks_sort_btn")) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort tasks")
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    TaskSortMode.entries.forEach { sm ->
                        DropdownMenuItem(
                            text = { Text(sm.label) },
                            onClick = {
                                sortMode = sm
                                sortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Task Items List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.TaskAlt,
                        title = strings.goodTasksTab,
                        subtitle = strings.noDataYet
                    )
                }
            } else {
                itemsIndexed(filteredTasks, key = { _, item -> item.id }) { idx, task ->
                    val remCount = reminders.count { it.targetType == "TASK" && it.targetId == task.id && !it.isDismissed }
                    TaskProCard(
                        task = task,
                        reminderCount = remCount,
                        canMoveUp = sortMode == TaskSortMode.MANUAL && idx > 0,
                        canMoveDown = sortMode == TaskSortMode.MANUAL && idx < filteredTasks.lastIndex,
                        onClick = { onOpenTask(task) },
                        onToggleComplete = { onToggleTaskCompleted(task) },
                        onToggleStar = { onSaveTask(task.copy(isStarred = !task.isStarred)) },
                        onToggleSubtask = { subId ->
                            val updatedSubs = NoteJsonCodec.decodeSubtasks(task.subtasksJson).map {
                                if (it.id == subId) it.copy(done = !it.done) else it
                            }
                            onSaveTask(task.copy(subtasksJson = NoteJsonCodec.encodeSubtasks(updatedSubs)))
                        },
                        onMoveUp = {
                            if (idx > 0) {
                                val mutable = filteredTasks.toMutableList()
                                val swapped = mutable.removeAt(idx)
                                mutable.add(idx - 1, swapped)
                                onReorderTasks(mutable)
                            }
                        },
                        onMoveDown = {
                            if (idx < filteredTasks.lastIndex) {
                                val mutable = filteredTasks.toMutableList()
                                val swapped = mutable.removeAt(idx)
                                mutable.add(idx + 1, swapped)
                                onReorderTasks(mutable)
                            }
                        },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }

    if (showNewListDialog) {
        AlertDialog(
            onDismissRequest = { showNewListDialog = false },
            title = { Text("Create Task List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name (e.g., Farm Ops, Exams)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clean = newListName.trim()
                    if (clean.isNotEmpty()) {
                        onSaveTaskList(
                            TaskListEntity(
                                ownerUserId = userId,
                                name = clean,
                                sortOrder = taskLists.size + 1
                            )
                        )
                        newListName = ""
                    }
                    showNewListDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewListDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskProCard(
    task: TaskItemEntity,
    reminderCount: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val subtasks = remember(task.subtasksJson) { NoteJsonCodec.decodeSubtasks(task.subtasksJson) }
    val priorityColor = when (task.priority) {
        TaskPriority.URGENT -> Color(0xFFDC2626)
        TaskPriority.HIGH -> Color(0xFFD97706)
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        TaskPriority.LOW -> Color(0xFF64748B)
    }
    val now = System.currentTimeMillis()
    val isOverdue = !task.isCompleted && task.dueAt != null && task.dueAt < now

    LedgerCard(onClick = onClick, modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = priorityColor.copy(alpha = 0.14f)) {
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)) {
                        Text(
                            text = task.categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (task.dueAt != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = DateTimeUtil.formatDateTime(task.dueAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.repeatRule != TaskRepeat.NONE) {
                        val repText = if (task.repeatRule == TaskRepeat.CUSTOM) {
                            "Every ${task.repeatInterval} ${task.repeatUnit}"
                        } else task.repeatRule.name
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(repText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (reminderCount > 0) {
                        Surface(shape = RoundedCornerShape(50), color = financeColors.champagneGold.copy(alpha = 0.2f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(12.dp), tint = financeColors.champagneGold)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("$reminderCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (task.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star task",
                            tint = if (task.isStarred) financeColors.champagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                if (canMoveUp || canMoveDown) {
                    Row {
                        if (canMoveUp) {
                            IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move task up", modifier = Modifier.size(15.dp))
                            }
                        }
                        if (canMoveDown) {
                            IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move task down", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }

        if (subtasks.isNotEmpty()) {
            val done = subtasks.count { it.done }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { done.toFloat() / subtasks.size.coerceAtLeast(1) },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("$done/${subtasks.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            subtasks.take(5).forEach { sub ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleSubtask(sub.id) }
                ) {
                    Checkbox(
                        checked = sub.done,
                        onCheckedChange = { onToggleSubtask(sub.id) },
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = sub.title,
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (subtasks.size > 5) {
                Text("+${subtasks.size - 5} more subtasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
