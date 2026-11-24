package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.data.backup.BackupScheduler
import com.example.data.repository.FinanceRepository
import com.example.domain.NotificationHelper
import com.example.i18n.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmartFinanceApp : Application(), Configuration.Provider {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var repository: FinanceRepository
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        repository = FinanceRepository(this)
        LanguageManager.bind(repository.prefs)
        NotificationHelper.ensureChannels(this)
        BackupScheduler.ensureRecurringWorker(this)
        com.example.domain.ReminderScheduler.ensurePeriodicWorker(this)

        applicationScope.launch {
            repository.ensureInitialized()
            val prefs = repository.prefs.getSnapshot()
            BackupScheduler.configureDailyAutoBackup(this@SmartFinanceApp, prefs.dailyAutoBackupEnabled)
            repository.processDueRecurringRules()
            com.example.domain.ReminderScheduler.evaluateDueRemindersNow(this@SmartFinanceApp)
            val oneDayMs = 24L * 60L * 60L * 1000L
            val isDailyOverdue = prefs.dailyAutoBackupEnabled &&
                (prefs.lastDriveBackupAt == 0L || System.currentTimeMillis() - prefs.lastDriveBackupAt >= oneDayMs)
            if ((prefs.connectedGmail.isNotBlank() || prefs.driveAuthorized) &&
                (prefs.pendingCloudSync || isDailyOverdue)
            ) {
                repository.syncPendingDriveBackupQuietly()
            }
        }
    }
}
