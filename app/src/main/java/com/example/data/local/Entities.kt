package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

@Serializable
enum class AccountType {
    CASH, BANK, BKASH, NAGAD, ROCKET, CARD, OTHER
}

@Serializable
enum class RecurringFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

@Serializable
enum class LoanKind {
    LOAN_GIVEN, DEBT_OWED, EMI
}

@Serializable
enum class WishlistPriority {
    HIGH, MEDIUM, LOW
}

@Serializable
enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
enum class TaskRepeat {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

@Serializable
enum class AiProviderKind {
    GEMINI, XKIRO, OPENAI_COMPATIBLE
}

@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val displayName: String,
    val isLocked: Boolean = false,
    val securityQuestion: String = "Favorite city?",
    val securityAnswerHash: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "finance_profiles")
data class FinanceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val iconKey: String = "person",
    val colorHex: Long = 0xFF047857,
    val description: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val initialBalance: Double,
    val colorHex: Long = 0xFF047857,
    val iconKey: String = "wallet",
    val createdAt: Long = System.currentTimeMillis(),
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val localizationKey: String? = null,
    val type: TransactionType,
    val iconKey: String = "category",
    val colorHex: Long = 0xFF047857,
    val monthlyBudget: Double = 0.0,
    val notes: String = "",
    val isDefault: Boolean = false,
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val paymentMethod: String = "Cash",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptUri: String? = null,
    val qrReference: String? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val recurringRuleId: Long? = null,
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val categoryId: Long,
    val monthKey: String, // YYYY-MM
    val limitAmount: Double,
    val alert80Sent: Boolean = false,
    val alert90Sent: Boolean = false,
    val alert100Sent: Boolean = false,
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val expectedDate: Long,
    val linkedAccountId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val title: String,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val accountId: Long,
    val paymentMethod: String = "Auto",
    val frequency: RecurringFrequency,
    val nextRunAt: Long,
    val isActive: Boolean = true,
    val note: String = "",
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "loans_debts")
data class LoanDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val kind: LoanKind,
    val counterparty: String,
    val principalAmount: Double,
    val paidAmount: Double = 0.0,
    val interestRate: Double = 0.0,
    val monthlyEmi: Double = 0.0,
    val totalMonths: Int = 12,
    val paidMonths: Int = 0,
    val dueDate: Long,
    val notes: String = "",
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val itemName: String,
    val targetPrice: Double,
    val savedAmount: Double = 0.0,
    val priority: WishlistPriority = WishlistPriority.MEDIUM,
    val notes: String = "",
    val isPurchased: Boolean = false,
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val assetName: String,
    val assetType: String,
    val investedAmount: Double,
    val currentValue: Double,
    val dateAdded: Long = System.currentTimeMillis(),
    val notes: String = "",
    val profileId: Long = 1L
)

@Serializable
@Entity(tableName = "financial_notes")
data class FinancialNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val title: String,
    val body: String,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "keep_notes")
data class KeepNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val title: String = "",
    val content: String = "",
    val colorHex: Long = 0xFFFFFFFF,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val tagsCsv: String = "",
    val checklistJson: String = "[]",
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val noteFormat: String = "RICH",
    val blocksJson: String = "[]",
    val attachmentsJson: String = "[]",
    val urlLink: String = "",
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val versionCount: Int = 1
)

@Serializable
@Entity(tableName = "note_versions")
data class NoteVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val noteId: Long,
    val titleSnapshot: String = "",
    val contentSnapshot: String = "",
    val blocksJsonSnapshot: String = "[]",
    val checklistJsonSnapshot: String = "[]",
    val changeSummary: String = "Saved edit",
    val savedAt: Long = System.currentTimeMillis()
)

@Serializable
data class NoteChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)

@Serializable
@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val colorHex: Long = 0xFF046C4E,
    val isSystemDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "task_items")
data class TaskItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val categoryLabel: String = "My Tasks",
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val repeatRule: TaskRepeat = TaskRepeat.NONE,
    val subtasksJson: String = "[]",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val listId: Long = 1L,
    val isStarred: Boolean = false,
    val manualOrder: Int = 0,
    val repeatInterval: Int = 1,
    val repeatUnit: String = "DAY",
    val repeatUntilAt: Long? = null,
    val repeatRemainingCount: Int? = null
)

@Serializable
@Entity(tableName = "item_reminders")
data class ItemReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val targetType: String, // "NOTE" or "TASK"
    val targetId: Long,
    val titleSnapshot: String = "",
    val triggerAt: Long,
    val offsetMinutes: Int = 0, // 0 = at time, 5, 15, 30, 60, 1440, or custom
    val repeatType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    val repeatIntervalDays: Int = 1,
    val isTriggered: Boolean = false,
    val isDismissed: Boolean = false,
    val snoozedUntil: Long? = null,
    val escalationLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val providerKind: AiProviderKind = AiProviderKind.GEMINI,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val supportsVision: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val availableModelsCsv: String = ""
)

fun AiProviderEntity.allModelsList(): List<String> {
    val parsed = availableModelsCsv
        .split(',', '\n', ';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val combined = buildList {
        if (modelName.isNotBlank()) add(modelName.trim())
        addAll(parsed)
    }
    return combined.distinct()
}

@Serializable
@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val providerId: Long,
    val sessionTitle: String = "General",
    val role: String, // "USER" or "ASSISTANT"
    val textContent: String,
    val imageBase64Thumb: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val servedByProviderName: String = "",
    val wasAutoSwitched: Boolean = false
)

@Serializable
@Entity(tableName = "quick_todos")
data class QuickTodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val type: String, // BUDGET_WARN, SAVINGS, MONTHLY_REPORT, BILL, RECURRING, BACKUP
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Serializable
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

enum class GameMatchResult {
    WIN,
    LOSS,
    DRAW
}

@Serializable
@Entity(tableName = "game_leaderboard_stats")
data class GameLeaderboardEntity(
    @PrimaryKey val gameId: String,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val highScore: Int = 0,
    val totalScore: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val highestBotAccuracyBeaten: Int = 0,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

fun GameLeaderboardEntity.winRatePercent(): Float =
    if (gamesPlayed <= 0) 0f else (wins * 100f) / gamesPlayed.toFloat()

@Serializable
enum class WalletPaymentChannel {
    BKASH,
    NAGAD,
    ROCKET,
    BINANCE
}

@Serializable
enum class WalletRequestType {
    DEPOSIT,
    WITHDRAW
}

@Serializable
enum class WalletRequestStatus {
    PENDING,
    APPROVED,
    PAID,
    REJECTED
}

@Serializable
enum class WalletLedgerKind {
    DEPOSIT_APPROVED,
    WITHDRAW_REQUESTED,
    WITHDRAW_REFUNDED,
    TRANSFER_TO_GAME_WALLET,
    TOURNAMENT_ENTRY,
    TOURNAMENT_WIN_PRIZE,
    TOURNAMENT_LOSS_CASHBACK,
    TOURNAMENT_DRAW_REFUND,
    ARMORY_PURCHASE,
    ADMIN_ADJUSTMENT,
    EXTERNAL_TOURNAMENT_ENTRY,
    EXTERNAL_TOURNAMENT_PRIZE
}

@Serializable
@Entity(tableName = "play_wallets")
data class PlayWalletEntity(
    @PrimaryKey val ownerUserId: Long,
    val tournamentBalance: Double = 0.0,
    val winningBalance: Double = 0.0, // ONLY Winning Coins are withdrawable!
    val gameBalance: Double = 0.0,
    val equippedBoardSkinId: String = "skin_royal_emerald",
    val equippedAvatarId: String = "avatar_monarch",
    val ownedItemsCsv: String = "skin_royal_emerald,avatar_monarch",
    val skillChargesJson: String = "{}",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "play_wallet_requests")
data class PlayWalletRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val usernameSnapshot: String = "",
    val requestType: WalletRequestType,
    val channel: WalletPaymentChannel,
    val amount: Double,
    val accountNumber: String,
    val transactionId: String = "",
    val status: WalletRequestStatus = WalletRequestStatus.PENDING,
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "play_wallet_ledger")
data class PlayWalletLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val walletBucket: String, // "TOURNAMENT", "WINNING", or "GAME"
    val kind: WalletLedgerKind,
    val amountDelta: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "tournament_tier_configs")
data class TournamentTierConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String, // "LUDO", "CHESS", "AETHER_CROWN", "REVERSI", "CONNECT_FOUR"
    val tierLabel: String, // e.g., "Bronze", "Silver", "Gold", "Diamond"
    val entryFee: Double,
    val winPrize: Double,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
@Entity(tableName = "game_bot_configs")
data class GameBotConfigEntity(
    @PrimaryKey val gameId: String, // "LUDO", "CHESS", "AETHER_CROWN", "REVERSI", "CONNECT_FOUR"
    val botAccuracyPercent: Int = 65,
    val maxConcurrentSeats: Int = 10,
    val multiplayerEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "external_tournaments")
data class ExternalTournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameCategory: String = "FREE_FIRE", // e.g. "FREE_FIRE", "PUBG", "EFOOTBALL"
    val title: String,
    val modeLabel: String = "SQUAD", // SOLO, DUO, SQUAD, CS 4v4, LONE WOLF
    val mapName: String = "Bermuda",
    val bannerKey: String = "BERMUDA", // BERMUDA, CLASH_SQUAD, CRAFTLAND, LONE_WOLF, CUSTOM_MAP
    val entryFee: Double = 20.0,
    val prizePool: Double = 500.0,
    val perKillReward: Double = 10.0,
    val prizeRanksJson: String = "[]", // JSON list of RankPrizeTier(rankLabel, amount)
    val maxSlots: Int = 48,
    val joinedCount: Int = 0,
    val startTimeLabel: String = "Tonight 9:00 PM",
    val matchStartTimeMillis: Long = 0L, // Scheduled epoch millis for live countdown & auto pre-match alert
    val preMatchAlertMinutes: Int = 10,
    val roomAlertSent: Boolean = false,
    val versionLabel: String = "Mobile Only • No Emulator",
    val rulesNotice: String = "",
    val adminCommissionPercent: Double = 10.0,
    val roomIdCode: String = "",
    val roomPassword: String = "",
    val status: String = "UPCOMING", // UPCOMING, LIVE, COMPLETED
    val winnerName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "external_tournament_entries")
data class ExternalTournamentEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tournamentId: Long,
    val ownerUserId: Long,
    val usernameSnapshot: String = "",
    val inGameName: String = "",
    val inGameUid: String = "",
    val entryFeePaid: Double = 0.0,
    val joinedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "broadcast_notifications")
data class BroadcastNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val priority: String = "INFO", // INFO, TOURNAMENT, PROMO, URGENT
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "admin_earnings")
data class AdminEarningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String, // LUDO, CHESS, AETHER_CROWN, REVERSI, CONNECT_FOUR, FREE_FIRE, ESPORTS
    val sourceType: String, // INBUILT_MATCH, EXTERNAL_TOURNAMENT, MANUAL_ADJUST
    val referenceTitle: String = "",
    val grossPool: Double = 0.0,
    val prizePaid: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)



