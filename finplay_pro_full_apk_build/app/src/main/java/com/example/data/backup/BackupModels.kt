package com.example.data.backup

import com.example.data.local.AccountEntity
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.FinanceProfileEntity
import com.example.data.local.FinancialNoteEntity
import com.example.data.local.InvestmentEntity
import com.example.data.local.ItemReminderEntity
import com.example.data.local.KeepNoteEntity
import com.example.data.local.LoanDebtEntity
import com.example.data.local.NoteVersionEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.QuickTodoEntity
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskListEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferencesState
import com.example.data.local.WishlistEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupPreferencesSnapshot(
    val languageCode: String = "bn",
    val themeMode: String = "SYSTEM",
    val currencyCode: String = "BDT",
    val activeProfileId: Long = 1L,
    val connectedGmail: String = "",
    val autoBackupOnNewData: Boolean = true,
    val dailyAutoBackupEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val budgetWarningsEnabled: Boolean = true,
    val savingsRemindersEnabled: Boolean = true,
    val monthlyReportReminderEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val aiAutoFailoverEnabled: Boolean = true
)

fun UserPreferencesState.toBackupPreferences(): BackupPreferencesSnapshot = BackupPreferencesSnapshot(
    languageCode = language.code,
    themeMode = themeMode.name,
    currencyCode = currencyCode,
    activeProfileId = activeProfileId,
    connectedGmail = connectedGmail,
    autoBackupOnNewData = autoBackupOnNewData,
    dailyAutoBackupEnabled = dailyAutoBackupEnabled,
    notificationsEnabled = notificationsEnabled,
    budgetWarningsEnabled = budgetWarningsEnabled,
    savingsRemindersEnabled = savingsRemindersEnabled,
    monthlyReportReminderEnabled = monthlyReportReminderEnabled,
    billRemindersEnabled = billRemindersEnabled,
    aiAutoFailoverEnabled = aiAutoFailoverEnabled
)

@Serializable
data class FullBackupSnapshot(
    val schemaVersion: Int = 4,
    val exportedAt: Long = System.currentTimeMillis(),
    val scope: String = "FULL_DATABASE",
    val ownerUserId: Long? = null,
    val appVersion: String = "3.0",
    val preferences: BackupPreferencesSnapshot? = null,
    val users: List<UserEntity> = emptyList(),
    val financeProfiles: List<FinanceProfileEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val loansDebts: List<LoanDebtEntity> = emptyList(),
    val wishlist: List<WishlistEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val financialNotes: List<FinancialNoteEntity> = emptyList(),
    val keepNotes: List<KeepNoteEntity> = emptyList(),
    val noteVersions: List<NoteVersionEntity> = emptyList(),
    val taskLists: List<TaskListEntity> = emptyList(),
    val taskItems: List<TaskItemEntity> = emptyList(),
    val itemReminders: List<ItemReminderEntity> = emptyList(),
    val aiProviders: List<AiProviderEntity> = emptyList(),
    val aiChatMessages: List<AiChatMessageEntity> = emptyList(),
    val quickTodos: List<QuickTodoEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val appConfig: List<AppConfigEntity> = emptyList(),
    val playWallets: List<com.example.data.local.PlayWalletEntity> = emptyList(),
    val playWalletRequests: List<com.example.data.local.PlayWalletRequestEntity> = emptyList(),
    val playWalletLedger: List<com.example.data.local.PlayWalletLedgerEntity> = emptyList(),
    val tournamentTierConfigs: List<com.example.data.local.TournamentTierConfigEntity> = emptyList(),
    val gameBotConfigs: List<com.example.data.local.GameBotConfigEntity> = emptyList(),
    val externalTournaments: List<com.example.data.local.ExternalTournamentEntity> = emptyList(),
    val externalTournamentEntries: List<com.example.data.local.ExternalTournamentEntryEntity> = emptyList(),
    val broadcastNotifications: List<com.example.data.local.BroadcastNotificationEntity> = emptyList(),
    val adminEarnings: List<com.example.data.local.AdminEarningEntity> = emptyList()
)

@Serializable
data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val size: String? = null
)

data class DriveBackupPreview(
    val email: String?,
    val fileId: String,
    val modifiedTime: String?,
    val jsonPayload: String,
    val snapshot: FullBackupSnapshot
)
