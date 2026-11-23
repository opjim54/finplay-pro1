package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.backup.BackupPreferencesSnapshot
import com.example.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_finance_prefs")

data class UserPreferencesState(
    val currentUserId: Long = -1L,
    val activeProfileId: Long = 1L,
    val rememberSession: Boolean = true,
    val isAdminSession: Boolean = false,
    val language: AppLanguage = AppLanguage.BANGLA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = "BDT",
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val pinLength: Int = 4,
    val biometricEnabled: Boolean = false,
    val connectedGmail: String = "",
    val driveAuthorized: Boolean = false,
    val pendingCloudSync: Boolean = false,
    val lastDriveBackupAt: Long = 0L,
    val lastDriveBackupStatus: String = "",
    val autoBackupOnNewData: Boolean = true,
    val dailyAutoBackupEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val budgetWarningsEnabled: Boolean = true,
    val savingsRemindersEnabled: Boolean = true,
    val monthlyReportReminderEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val aiAutoFailoverEnabled: Boolean = true
)

class PreferencesStore(private val context: Context) {
    private object Keys {
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        val REMEMBER_SESSION = booleanPreferencesKey("remember_session")
        val IS_ADMIN_SESSION = booleanPreferencesKey("is_admin_session")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CONNECTED_GMAIL = stringPreferencesKey("connected_gmail")
        val DRIVE_AUTHORIZED = booleanPreferencesKey("drive_authorized")
        val PENDING_CLOUD_SYNC = booleanPreferencesKey("pending_cloud_sync")
        val LAST_DRIVE_BACKUP_AT = longPreferencesKey("last_drive_backup_at")
        val LAST_DRIVE_BACKUP_STATUS = stringPreferencesKey("last_drive_backup_status")
        val AUTO_BACKUP_ON_NEW_DATA = booleanPreferencesKey("auto_backup_on_new_data")
        val DAILY_AUTO_BACKUP = booleanPreferencesKey("daily_auto_backup")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_WARNINGS = booleanPreferencesKey("budget_warnings")
        val SAVINGS_REMINDERS = booleanPreferencesKey("savings_reminders")
        val MONTHLY_REPORT_REMINDER = booleanPreferencesKey("monthly_report_reminder")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val LAST_SAVINGS_REMINDER_DAY = stringPreferencesKey("last_savings_reminder_day")
        val LAST_MONTHLY_REPORT_MONTH = stringPreferencesKey("last_monthly_report_month")
        val AI_AUTO_FAILOVER = booleanPreferencesKey("ai_auto_failover")
    }

    val state: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        UserPreferencesState(
            currentUserId = prefs[Keys.CURRENT_USER_ID] ?: -1L,
            activeProfileId = prefs[Keys.ACTIVE_PROFILE_ID] ?: 1L,
            rememberSession = prefs[Keys.REMEMBER_SESSION] ?: true,
            isAdminSession = prefs[Keys.IS_ADMIN_SESSION] ?: false,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: "BDT",
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            pinLength = (prefs[Keys.PIN_LENGTH] ?: 4).coerceIn(4, 6),
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            connectedGmail = prefs[Keys.CONNECTED_GMAIL] ?: "",
            driveAuthorized = prefs[Keys.DRIVE_AUTHORIZED] ?: false,
            pendingCloudSync = prefs[Keys.PENDING_CLOUD_SYNC] ?: false,
            lastDriveBackupAt = prefs[Keys.LAST_DRIVE_BACKUP_AT] ?: 0L,
            lastDriveBackupStatus = prefs[Keys.LAST_DRIVE_BACKUP_STATUS] ?: "",
            autoBackupOnNewData = prefs[Keys.AUTO_BACKUP_ON_NEW_DATA] ?: true,
            dailyAutoBackupEnabled = prefs[Keys.DAILY_AUTO_BACKUP] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            budgetWarningsEnabled = prefs[Keys.BUDGET_WARNINGS] ?: true,
            savingsRemindersEnabled = prefs[Keys.SAVINGS_REMINDERS] ?: true,
            monthlyReportReminderEnabled = prefs[Keys.MONTHLY_REPORT_REMINDER] ?: true,
            billRemindersEnabled = prefs[Keys.BILL_REMINDERS] ?: true,
            aiAutoFailoverEnabled = prefs[Keys.AI_AUTO_FAILOVER] ?: true
        )
    }

    suspend fun getSnapshot(): UserPreferencesState = state.first()

    suspend fun setSession(userId: Long, remember: Boolean, isAdmin: Boolean = false) {
        context.dataStore.edit {
            it[Keys.CURRENT_USER_ID] = userId
            it[Keys.REMEMBER_SESSION] = remember
            it[Keys.IS_ADMIN_SESSION] = isAdmin
        }
    }

    suspend fun setActiveProfileId(profileId: Long) {
        context.dataStore.edit {
            it[Keys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it[Keys.CURRENT_USER_ID] = -1L
            it[Keys.IS_ADMIN_SESSION] = false
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setPin(enabled: Boolean, hash: String = "", salt: String = "", pinLength: Int = 4) {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = enabled
            if (hash.isNotEmpty()) {
                it[Keys.PIN_HASH] = hash
                it[Keys.PIN_SALT] = salt
                it[Keys.PIN_LENGTH] = pinLength.coerceIn(4, 6)
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setConnectedGmail(email: String) {
        context.dataStore.edit {
            if (email.isNotBlank()) {
                it[Keys.CONNECTED_GMAIL] = email
            }
        }
    }

    suspend fun setDriveConnection(email: String, authorized: Boolean) {
        context.dataStore.edit {
            if (email.isNotBlank()) it[Keys.CONNECTED_GMAIL] = email
            it[Keys.DRIVE_AUTHORIZED] = authorized
        }
    }

    suspend fun clearDriveConnection() {
        context.dataStore.edit {
            it[Keys.CONNECTED_GMAIL] = ""
            it[Keys.DRIVE_AUTHORIZED] = false
            it[Keys.PENDING_CLOUD_SYNC] = false
            it[Keys.LAST_DRIVE_BACKUP_STATUS] = ""
        }
    }

    suspend fun setPendingCloudSync(pending: Boolean) {
        context.dataStore.edit { it[Keys.PENDING_CLOUD_SYNC] = pending }
    }

    suspend fun setDriveBackupStatus(timestamp: Long, status: String, pending: Boolean? = null) {
        context.dataStore.edit {
            if (timestamp > 0L) it[Keys.LAST_DRIVE_BACKUP_AT] = timestamp
            it[Keys.LAST_DRIVE_BACKUP_STATUS] = status
            pending?.let { p -> it[Keys.PENDING_CLOUD_SYNC] = p }
        }
    }

    suspend fun setAutoBackupOnNewData(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ON_NEW_DATA] = enabled }
    }

    suspend fun setDailyAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_AUTO_BACKUP] = enabled }
    }

    suspend fun setNotificationSettings(
        enabled: Boolean? = null,
        budgetWarnings: Boolean? = null,
        savingsReminders: Boolean? = null,
        monthlyReport: Boolean? = null,
        billReminders: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            enabled?.let { prefs[Keys.NOTIFICATIONS_ENABLED] = it }
            budgetWarnings?.let { prefs[Keys.BUDGET_WARNINGS] = it }
            savingsReminders?.let { prefs[Keys.SAVINGS_REMINDERS] = it }
            monthlyReport?.let { prefs[Keys.MONTHLY_REPORT_REMINDER] = it }
            billReminders?.let { prefs[Keys.BILL_REMINDERS] = it }
        }
    }

    suspend fun getLastSavingsReminderDay(): String =
        context.dataStore.data.map { it[Keys.LAST_SAVINGS_REMINDER_DAY] ?: "" }.first()

    suspend fun setLastSavingsReminderDay(dayKey: String) {
        context.dataStore.edit { it[Keys.LAST_SAVINGS_REMINDER_DAY] = dayKey }
    }

    suspend fun getLastMonthlyReportMonth(): String =
        context.dataStore.data.map { it[Keys.LAST_MONTHLY_REPORT_MONTH] ?: "" }.first()

    suspend fun setLastMonthlyReportMonth(monthKey: String) {
        context.dataStore.edit { it[Keys.LAST_MONTHLY_REPORT_MONTH] = monthKey }
    }

    suspend fun setAiAutoFailoverEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_AUTO_FAILOVER] = enabled }
    }

    suspend fun restoreFromSnapshot(snapshot: BackupPreferencesSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = snapshot.languageCode
            prefs[Keys.THEME_MODE] = runCatching {
                ThemeMode.valueOf(snapshot.themeMode).name
            }.getOrDefault(ThemeMode.SYSTEM.name)
            prefs[Keys.CURRENCY_CODE] = snapshot.currencyCode
            if (snapshot.activeProfileId > 0L) {
                prefs[Keys.ACTIVE_PROFILE_ID] = snapshot.activeProfileId
            }
            if (snapshot.connectedGmail.isNotBlank()) {
                prefs[Keys.CONNECTED_GMAIL] = snapshot.connectedGmail
            }
            prefs[Keys.AUTO_BACKUP_ON_NEW_DATA] = snapshot.autoBackupOnNewData
            prefs[Keys.DAILY_AUTO_BACKUP] = snapshot.dailyAutoBackupEnabled
            prefs[Keys.NOTIFICATIONS_ENABLED] = snapshot.notificationsEnabled
            prefs[Keys.BUDGET_WARNINGS] = snapshot.budgetWarningsEnabled
            prefs[Keys.SAVINGS_REMINDERS] = snapshot.savingsRemindersEnabled
            prefs[Keys.MONTHLY_REPORT_REMINDER] = snapshot.monthlyReportReminderEnabled
            prefs[Keys.BILL_REMINDERS] = snapshot.billRemindersEnabled
            prefs[Keys.AI_AUTO_FAILOVER] = snapshot.aiAutoFailoverEnabled
            prefs[Keys.PENDING_CLOUD_SYNC] = false
        }
    }
}
