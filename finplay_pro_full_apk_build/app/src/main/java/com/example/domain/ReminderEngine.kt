package com.example.domain

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.ItemReminderEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object ReminderActions {
    const val ACTION_SNOOZE_10M = "com.example.REMINDER_SNOOZE_10M"
    const val ACTION_SNOOZE_30M = "com.example.REMINDER_SNOOZE_30M"
    const val ACTION_RESCHEDULE_1D = "com.example.REMINDER_RESCHEDULE_1D"
    const val ACTION_MARK_DONE = "com.example.REMINDER_MARK_DONE"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
}

object ReminderScheduler {
    private const val WORK_REMINDER_POLL = "smart_finance_reminder_poller"
    private const val WORK_REMINDER_NEXT = "smart_finance_reminder_next_exact"

    fun ensurePeriodicWorker(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_REMINDER_POLL,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleNextExactCheck(context: Context, triggerAtMillis: Long) {
        val delayMs = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val req = OneTimeWorkRequestBuilder<ReminderCheckWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_REMINDER_NEXT,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    suspend fun evaluateDueRemindersNow(context: Context): Int {
        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesStore(context).getSnapshot()
        if (!prefs.notificationsEnabled) return 0

        val now = System.currentTimeMillis()
        val dueList = db.itemReminderDao().getDueReminders(now)
        var firedCount = 0

        dueList.forEach { rem ->
            val effectiveAt = rem.snoozedUntil ?: rem.triggerAt
            if (!rem.isTriggered || (rem.snoozedUntil != null && now >= rem.snoozedUntil)) {
                fireNotificationForReminder(context, db, rem, overdueNotice = false)
                db.itemReminderDao().update(
                    rem.copy(
                        isTriggered = true,
                        snoozedUntil = null
                    )
                )
                firedCount++
            } else {
                // Overdue escalation if > 3 hours past trigger and still undismissed
                val hoursLate = (now - effectiveAt) / 3_600_000L
                val nextEsc = when {
                    hoursLate >= 24L && rem.escalationLevel < 2 -> 2
                    hoursLate >= 3L && rem.escalationLevel < 1 -> 1
                    else -> rem.escalationLevel
                }
                if (nextEsc > rem.escalationLevel) {
                    fireNotificationForReminder(context, db, rem.copy(escalationLevel = nextEsc), overdueNotice = true)
                    db.itemReminderDao().update(rem.copy(escalationLevel = nextEsc))
                    firedCount++
                }
            }
        }
        return firedCount
    }

    private suspend fun fireNotificationForReminder(
        context: Context,
        db: AppDatabase,
        rem: ItemReminderEntity,
        overdueNotice: Boolean
    ) {
        val notifDbId = db.notificationDao().insert(
            NotificationEntity(
                ownerUserId = rem.ownerUserId,
                type = if (overdueNotice) "REMINDER_ESCALATED" else "ITEM_REMINDER",
                title = if (overdueNotice) "Overdue Escalation: ${rem.titleSnapshot}" else "Reminder: ${rem.titleSnapshot}",
                message = "${rem.targetType} alert • ${DateTimeUtil.formatDateTime(rem.triggerAt)}"
            )
        )
        val systemNotifId = (rem.id.toInt() + 9000)

        fun buildAction(action: String, label: String): NotificationCompat.Action {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(ReminderActions.EXTRA_REMINDER_ID, rem.id)
                putExtra(ReminderActions.EXTRA_NOTIFICATION_ID, systemNotifId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                (rem.id.toInt() * 10 + action.hashCode()).and(0x7FFFFFFF),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action.Builder(R.mipmap.ic_launcher, label, pi).build()
        }

        val actions = listOf(
            buildAction(ReminderActions.ACTION_SNOOZE_10M, "Snooze 10m"),
            buildAction(ReminderActions.ACTION_RESCHEDULE_1D, "+1 Day"),
            buildAction(ReminderActions.ACTION_MARK_DONE, "Done")
        )
        NotificationHelper.postSystemNotification(
            context = context,
            channelId = NotificationHelper.CHANNEL_REMINDERS,
            notificationId = systemNotifId.coerceAtLeast(notifDbId.toInt()),
            title = if (overdueNotice) "Overdue Reminder: ${rem.titleSnapshot}" else rem.titleSnapshot.ifBlank { "Reminder" },
            message = if (overdueNotice) "This ${rem.targetType.lowercase()} reminder is overdue — tap Snooze, +1 Day, or Done."
            else "Scheduled ${rem.targetType.lowercase()} reminder (${DateTimeUtil.formatDateTime(rem.triggerAt)})",
            actions = actions
        )
    }
}

class ReminderCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ReminderScheduler.evaluateDueRemindersNow(applicationContext)
        return Result.success()
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remId = intent.getLongExtra(ReminderActions.EXTRA_REMINDER_ID, -1L)
        val notifId = intent.getIntExtra(ReminderActions.EXTRA_NOTIFICATION_ID, -1)
        if (notifId != -1) {
            runCatching { NotificationManagerCompat.from(context).cancel(notifId) }
        }
        if (remId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val existing = db.itemReminderDao().getById(remId) ?: return@launch
                val now = System.currentTimeMillis()
                when (intent.action) {
                    ReminderActions.ACTION_SNOOZE_10M -> {
                        val until = now + 10L * 60L * 1000L
                        db.itemReminderDao().update(existing.copy(snoozedUntil = until, isDismissed = false))
                        ReminderScheduler.scheduleNextExactCheck(context, until)
                    }
                    ReminderActions.ACTION_SNOOZE_30M -> {
                        val until = now + 30L * 60L * 1000L
                        db.itemReminderDao().update(existing.copy(snoozedUntil = until, isDismissed = false))
                        ReminderScheduler.scheduleNextExactCheck(context, until)
                    }
                    ReminderActions.ACTION_RESCHEDULE_1D -> {
                        val nextDay = existing.triggerAt.coerceAtLeast(now) + 24L * 60L * 60L * 1000L
                        db.itemReminderDao().update(
                            existing.copy(triggerAt = nextDay, snoozedUntil = null, isTriggered = false, isDismissed = false)
                        )
                        ReminderScheduler.scheduleNextExactCheck(context, nextDay)
                    }
                    ReminderActions.ACTION_MARK_DONE -> {
                        if (existing.repeatType == "NONE") {
                            db.itemReminderDao().update(existing.copy(isDismissed = true, snoozedUntil = null))
                        } else {
                            val stepDays = when (existing.repeatType) {
                                "DAILY" -> 1L
                                "WEEKLY" -> 7L
                                "MONTHLY" -> 30L
                                "YEARLY" -> 365L
                                else -> existing.repeatIntervalDays.coerceAtLeast(1).toLong()
                            }
                            val nextAt = maxOf(existing.triggerAt, now) + stepDays * 86_400_000L
                            db.itemReminderDao().update(
                                existing.copy(triggerAt = nextAt, snoozedUntil = null, isTriggered = false, isDismissed = false)
                            )
                            ReminderScheduler.scheduleNextExactCheck(context, nextAt)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
