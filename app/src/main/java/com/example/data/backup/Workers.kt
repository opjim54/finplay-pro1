package com.example.data.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.RecurringFrequency
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.NotificationHelper
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesStore(applicationContext)
        val state = prefs.getSnapshot()
        if (state.connectedGmail.isBlank() && !state.driveAuthorized) {
            return Result.success()
        }

        val db = AppDatabase.getInstance(applicationContext)
        val snapshot = FullBackupSnapshot(
            scope = "FULL_DATABASE",
            ownerUserId = state.currentUserId.takeIf { it > 0L },
            preferences = state.toBackupPreferences(),
            users = db.userDao().getAllUsersOnce(),
            financeProfiles = db.financeProfileDao().getAllOnce(),
            accounts = db.accountDao().getAllOnce(),
            categories = db.categoryDao().getAllOnce(),
            transactions = db.transactionDao().getAllOnce(),
            budgets = db.budgetDao().getAllOnce(),
            savingsGoals = db.savingsGoalDao().getAllOnce(),
            recurringRules = db.recurringRuleDao().getAllOnce(),
            loansDebts = db.wealthExtrasDao().getAllLoansOnce(),
            wishlist = db.wealthExtrasDao().getAllWishlistOnce(),
            investments = db.wealthExtrasDao().getAllInvestmentsOnce(),
            financialNotes = db.wealthExtrasDao().getAllNotesOnce(),
            keepNotes = db.keepNoteDao().getAllOnce(),
            noteVersions = db.noteVersionDao().getAllOnce(),
            taskLists = db.taskListDao().getAllOnce(),
            taskItems = db.taskItemDao().getAllOnce(),
            itemReminders = db.itemReminderDao().getAllOnce(),
            aiProviders = db.aiProviderDao().getAllOnce(),
            aiChatMessages = db.aiChatDao().getAllOnce(),
            quickTodos = db.quickTodoDao().getAllOnce(),
            notifications = db.notificationDao().getAllOnce(),
            appConfig = db.appConfigDao().getAllOnce()
        )
        val payload = ExportManager.snapshotToJson(snapshot)
        val driveClient = GoogleDriveClient(applicationContext)
        return when (
            val res = driveClient.uploadBackup(
                jsonContent = payload,
                interactive = false,
                activity = null,
                knownEmail = state.connectedGmail
            )
        ) {
            is DriveSyncResult.Success -> {
                val resolvedEmail = res.email ?: state.connectedGmail
                if (resolvedEmail.isNotBlank()) {
                    prefs.setDriveConnection(resolvedEmail, authorized = true)
                }
                prefs.setDriveBackupStatus(
                    timestamp = res.timestamp,
                    status = "Auto-synced to ${resolvedEmail.ifBlank { "Google Drive" }}",
                    pending = false
                )
                Result.success()
            }
            is DriveSyncResult.NeedsConsent -> {
                prefs.setDriveBackupStatus(
                    timestamp = 0L,
                    status = "Re-authorization needed in Settings",
                    pending = true
                )
                Result.success()
            }
            is DriveSyncResult.Error -> {
                prefs.setDriveBackupStatus(
                    timestamp = 0L,
                    status = "Pending sync: ${res.message}",
                    pending = true
                )
                Result.retry()
            }
        }
    }
}

class RecurringWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val prefsStore = PreferencesStore(applicationContext)
        val prefs = prefsStore.getSnapshot()
        val now = System.currentTimeMillis()
        val dueRules = db.recurringRuleDao().getDueRules(now)
        var anyNewData = false

        dueRules.forEach { rule ->
            var next = rule.nextRunAt
            var created = 0
            while (next <= now && created < 12) {
                val tx = TransactionEntity(
                    ownerUserId = rule.ownerUserId,
                    profileId = rule.profileId,
                    type = rule.type,
                    amount = rule.amount,
                    categoryId = rule.categoryId,
                    accountId = rule.accountId,
                    paymentMethod = rule.paymentMethod,
                    timestamp = next,
                    note = "${rule.title} (Auto)",
                    recurringRuleId = rule.id
                )
                db.transactionDao().insert(tx)
                val delta = if (rule.type == TransactionType.INCOME) rule.amount else -rule.amount
                db.accountDao().adjustBalance(rule.accountId, delta)
                next = advanceTimestamp(next, rule.frequency)
                created++
            }
            db.recurringRuleDao().update(rule.copy(nextRunAt = next))
            if (created > 0) {
                anyNewData = true
                if (prefs.notificationsEnabled && prefs.billRemindersEnabled) {
                    val isBn = prefs.language == com.example.i18n.AppLanguage.BANGLA
                    val title = if (isBn) "রিকারিং / বিল লেনদেন সম্পন্ন" else "Recurring Entry Posted"
                    val message = if (isBn) "${rule.title}: $created টি স্বয়ংক্রিয় এন্ট্রি যুক্ত হয়েছে।"
                    else "${rule.title}: $created automatic entry posted."
                    db.notificationDao().insert(
                        NotificationEntity(
                            ownerUserId = rule.ownerUserId,
                            type = "RECURRING",
                            title = title,
                            message = message
                        )
                    )
                    NotificationHelper.postSystemNotification(
                        applicationContext,
                        NotificationHelper.CHANNEL_REMINDERS,
                        (rule.id + 5000).toInt(),
                        title,
                        message
                    )
                }
            }
        }

        if (prefs.notificationsEnabled && prefs.currentUserId > 0L) {
            val isBn = prefs.language == com.example.i18n.AppLanguage.BANGLA
            if (prefs.savingsRemindersEnabled) {
                val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(now))
                if (prefsStore.getLastSavingsReminderDay() != dayKey) {
                    val sevenDaysAhead = now + 7L * 24L * 60L * 60L * 1000L
                    val pendingGoals = db.savingsGoalDao().getByUserOnce(prefs.currentUserId)
                        .filter { it.savedAmount < it.targetAmount && it.expectedDate in 1L..sevenDaysAhead }
                    if (pendingGoals.isNotEmpty()) {
                        val firstGoal = pendingGoals.first()
                        val title = if (isBn) "সঞ্চয় লক্ষ্য রিমাইন্ডার" else "Savings Goal Reminder"
                        val message = if (isBn) {
                            "\"${firstGoal.name}\" লক্ষ্যের নির্ধারিত তারিখ নিকটবর্তী (${pendingGoals.size} টি লক্ষ্য সক্রিয়)।"
                        } else {
                            "Target date approaching for \"${firstGoal.name}\" (${pendingGoals.size} goal(s) due soon)."
                        }
                        val notifId = db.notificationDao().insert(
                            NotificationEntity(
                                ownerUserId = prefs.currentUserId,
                                type = "SAVINGS_REMINDER",
                                title = title,
                                message = message
                            )
                        )
                        NotificationHelper.postSystemNotification(
                            applicationContext,
                            NotificationHelper.CHANNEL_REMINDERS,
                            (notifId + 6000).toInt(),
                            title,
                            message
                        )
                        prefsStore.setLastSavingsReminderDay(dayKey)
                    }
                }
            }

            if (prefs.monthlyReportReminderEnabled) {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val monthKey = com.example.domain.DateTimeUtil.monthKey(now)
                if ((dayOfMonth <= 3 || dayOfMonth >= maxDay - 1) && prefsStore.getLastMonthlyReportMonth() != monthKey) {
                    val title = if (isBn) "মাসিক আর্থিক রিপোর্ট প্রস্তুত" else "Monthly Financial Report Ready"
                    val message = if (isBn) {
                        "আপনার মাসিক আয়, ব্যয় ও সঞ্চয়ের তুলনামূলক রিপোর্ট অ্যানালিটিক্স ট্যাবে দেখুন।"
                    } else {
                        "Review your monthly income, expense, and savings breakdown in Analytics."
                    }
                    val notifId = db.notificationDao().insert(
                        NotificationEntity(
                            ownerUserId = prefs.currentUserId,
                            type = "MONTHLY_REPORT",
                            title = title,
                            message = message
                        )
                    )
                    NotificationHelper.postSystemNotification(
                        applicationContext,
                        NotificationHelper.CHANNEL_REMINDERS,
                        (notifId + 7000).toInt(),
                        title,
                        message
                    )
                    prefsStore.setLastMonthlyReportMonth(monthKey)
                }
            }
        }

        com.example.domain.ReminderScheduler.evaluateDueRemindersNow(applicationContext)

        if (anyNewData && prefs.autoBackupOnNewData && (prefs.connectedGmail.isNotBlank() || prefs.driveAuthorized)) {
            BackupScheduler.enqueueOnChangeAutoBackup(applicationContext)
        }
        return Result.success()
    }

    companion object {
        fun advanceTimestamp(from: Long, frequency: RecurringFrequency): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = from }
            when (frequency) {
                RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
                RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
            }
            return cal.timeInMillis
        }
    }
}

object BackupScheduler {
    private const val WORK_ON_CHANGE_DRIVE = "smart_finance_drive_auto_backup"
    private const val WORK_DAILY_DRIVE = "smart_finance_daily_drive_backup"
    private const val WORK_RECURRING = "smart_finance_recurring_processor"

    fun enqueueOnChangeAutoBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_ON_CHANGE_DRIVE,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun configureDailyAutoBackup(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_DAILY_DRIVE)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_DAILY_DRIVE,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun ensureRecurringWorker(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_RECURRING,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
