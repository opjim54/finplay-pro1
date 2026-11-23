package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class FinanceTypeConverters {
    @TypeConverter fun fromTxType(v: TransactionType): String = v.name
    @TypeConverter fun toTxType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromRecurringFreq(v: RecurringFrequency): String = v.name
    @TypeConverter fun toRecurringFreq(v: String): RecurringFrequency = RecurringFrequency.valueOf(v)

    @TypeConverter fun fromLoanKind(v: LoanKind): String = v.name
    @TypeConverter fun toLoanKind(v: String): LoanKind = LoanKind.valueOf(v)

    @TypeConverter fun fromWishlistPriority(v: WishlistPriority): String = v.name
    @TypeConverter fun toWishlistPriority(v: String): WishlistPriority = WishlistPriority.valueOf(v)

    @TypeConverter fun fromTaskPriority(v: TaskPriority): String = v.name
    @TypeConverter fun toTaskPriority(v: String): TaskPriority = runCatching { TaskPriority.valueOf(v) }.getOrDefault(TaskPriority.MEDIUM)

    @TypeConverter fun fromTaskRepeat(v: TaskRepeat): String = v.name
    @TypeConverter fun toTaskRepeat(v: String): TaskRepeat = runCatching { TaskRepeat.valueOf(v) }.getOrDefault(TaskRepeat.NONE)

    @TypeConverter fun fromAiProviderKind(v: AiProviderKind): String = v.name
    @TypeConverter fun toAiProviderKind(v: String): AiProviderKind = runCatching { AiProviderKind.valueOf(v) }.getOrDefault(AiProviderKind.OPENAI_COMPATIBLE)

    @TypeConverter fun fromWalletPaymentChannel(v: WalletPaymentChannel): String = v.name
    @TypeConverter fun toWalletPaymentChannel(v: String): WalletPaymentChannel = runCatching { WalletPaymentChannel.valueOf(v) }.getOrDefault(WalletPaymentChannel.BKASH)

    @TypeConverter fun fromWalletRequestType(v: WalletRequestType): String = v.name
    @TypeConverter fun toWalletRequestType(v: String): WalletRequestType = runCatching { WalletRequestType.valueOf(v) }.getOrDefault(WalletRequestType.DEPOSIT)

    @TypeConverter fun fromWalletRequestStatus(v: WalletRequestStatus): String = v.name
    @TypeConverter fun toWalletRequestStatus(v: String): WalletRequestStatus = runCatching { WalletRequestStatus.valueOf(v) }.getOrDefault(WalletRequestStatus.PENDING)

    @TypeConverter fun fromWalletLedgerKind(v: WalletLedgerKind): String = v.name
    @TypeConverter fun toWalletLedgerKind(v: String): WalletLedgerKind = runCatching { WalletLedgerKind.valueOf(v) }.getOrDefault(WalletLedgerKind.TOURNAMENT_ENTRY)
}

@Database(
    entities = [
        UserEntity::class,
        FinanceProfileEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        RecurringRuleEntity::class,
        LoanDebtEntity::class,
        WishlistEntity::class,
        InvestmentEntity::class,
        FinancialNoteEntity::class,
        KeepNoteEntity::class,
        NoteVersionEntity::class,
        TaskListEntity::class,
        TaskItemEntity::class,
        ItemReminderEntity::class,
        AiProviderEntity::class,
        AiChatMessageEntity::class,
        QuickTodoEntity::class,
        NotificationEntity::class,
        AppConfigEntity::class,
        GameLeaderboardEntity::class,
        PlayWalletEntity::class,
        PlayWalletRequestEntity::class,
        PlayWalletLedgerEntity::class,
        TournamentTierConfigEntity::class,
        GameBotConfigEntity::class,
        ExternalTournamentEntity::class,
        ExternalTournamentEntryEntity::class,
        BroadcastNotificationEntity::class,
        AdminEarningEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(FinanceTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun financeProfileDao(): FinanceProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun wealthExtrasDao(): WealthExtrasDao
    abstract fun keepNoteDao(): KeepNoteDao
    abstract fun noteVersionDao(): NoteVersionDao
    abstract fun taskListDao(): TaskListDao
    abstract fun taskItemDao(): TaskItemDao
    abstract fun itemReminderDao(): ItemReminderDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun aiChatDao(): AiChatDao
    abstract fun quickTodoDao(): QuickTodoDao
    abstract fun notificationDao(): NotificationDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun gameLeaderboardDao(): GameLeaderboardDao
    abstract fun playWalletDao(): PlayWalletDao
    abstract fun playWalletRequestDao(): PlayWalletRequestDao
    abstract fun playWalletLedgerDao(): PlayWalletLedgerDao
    abstract fun tournamentTierConfigDao(): TournamentTierConfigDao
    abstract fun gameBotConfigDao(): GameBotConfigDao
    abstract fun externalTournamentDao(): ExternalTournamentDao
    abstract fun externalTournamentEntryDao(): ExternalTournamentEntryDao
    abstract fun broadcastNotificationDao(): BroadcastNotificationDao
    abstract fun adminEarningDao(): AdminEarningDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `finance_profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL DEFAULT 'person',
                        `colorHex` INTEGER NOT NULL DEFAULT 4278483031,
                        `description` TEXT NOT NULL DEFAULT '',
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `budgets` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `recurring_rules` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `loans_debts` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `wishlist` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `investments` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `keep_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL DEFAULT '',
                        `colorHex` INTEGER NOT NULL DEFAULT 4294967295,
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `isTrashed` INTEGER NOT NULL DEFAULT 0,
                        `tagsCsv` TEXT NOT NULL DEFAULT '',
                        `checklistJson` TEXT NOT NULL DEFAULT '[]',
                        `reminderAt` INTEGER,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `categoryLabel` TEXT NOT NULL DEFAULT 'General',
                        `dueAt` INTEGER,
                        `reminderAt` INTEGER,
                        `repeatRule` TEXT NOT NULL DEFAULT 'NONE',
                        `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_providers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `providerKind` TEXT NOT NULL DEFAULT 'GEMINI',
                        `baseUrl` TEXT NOT NULL,
                        `apiKey` TEXT NOT NULL,
                        `modelName` TEXT NOT NULL,
                        `supportsVision` INTEGER NOT NULL DEFAULT 1,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `providerId` INTEGER NOT NULL,
                        `sessionTitle` TEXT NOT NULL DEFAULT 'General',
                        `role` TEXT NOT NULL,
                        `textContent` TEXT NOT NULL,
                        `imageBase64Thumb` TEXT,
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_todos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `isDone` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Preserve legacy financial notes into Keep Notes on upgrade
                db.execSQL(
                    """
                    INSERT INTO `keep_notes` (ownerUserId, title, content, isPinned, createdAt, updatedAt)
                    SELECT ownerUserId, title, body, isPinned, updatedAt, updatedAt FROM `financial_notes`
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // KeepNoteEntity extensions
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `noteFormat` TEXT NOT NULL DEFAULT 'RICH'")
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `blocksJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `attachmentsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `urlLink` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `isLocked` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `keep_notes` ADD COLUMN `versionCount` INTEGER NOT NULL DEFAULT 1")

                // TaskItemEntity extensions
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `listId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `isStarred` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `manualOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `repeatInterval` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `repeatUnit` TEXT NOT NULL DEFAULT 'DAY'")
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `repeatUntilAt` INTEGER")
                db.execSQL("ALTER TABLE `task_items` ADD COLUMN `repeatRemainingCount` INTEGER")

                // AiChatMessageEntity extensions
                db.execSQL("ALTER TABLE `ai_chat_messages` ADD COLUMN `servedByProviderName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `ai_chat_messages` ADD COLUMN `wasAutoSwitched` INTEGER NOT NULL DEFAULT 0")

                // NoteVersionEntity table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `note_versions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `titleSnapshot` TEXT NOT NULL DEFAULT '',
                        `contentSnapshot` TEXT NOT NULL DEFAULT '',
                        `blocksJsonSnapshot` TEXT NOT NULL DEFAULT '[]',
                        `checklistJsonSnapshot` TEXT NOT NULL DEFAULT '[]',
                        `changeSummary` TEXT NOT NULL DEFAULT 'Saved edit',
                        `savedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // TaskListEntity table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_lists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL DEFAULT 4278480078,
                        `isSystemDefault` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // ItemReminderEntity table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `item_reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetId` INTEGER NOT NULL,
                        `titleSnapshot` TEXT NOT NULL DEFAULT '',
                        `triggerAt` INTEGER NOT NULL,
                        `offsetMinutes` INTEGER NOT NULL DEFAULT 0,
                        `repeatType` TEXT NOT NULL DEFAULT 'NONE',
                        `repeatIntervalDays` INTEGER NOT NULL DEFAULT 1,
                        `isTriggered` INTEGER NOT NULL DEFAULT 0,
                        `isDismissed` INTEGER NOT NULL DEFAULT 0,
                        `snoozedUntil` INTEGER,
                        `escalationLevel` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `ai_providers` ADD COLUMN `availableModelsCsv` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `game_leaderboard_stats` (
                        `gameId` TEXT NOT NULL,
                        `gamesPlayed` INTEGER NOT NULL DEFAULT 0,
                        `wins` INTEGER NOT NULL DEFAULT 0,
                        `losses` INTEGER NOT NULL DEFAULT 0,
                        `draws` INTEGER NOT NULL DEFAULT 0,
                        `highScore` INTEGER NOT NULL DEFAULT 0,
                        `totalScore` INTEGER NOT NULL DEFAULT 0,
                        `currentWinStreak` INTEGER NOT NULL DEFAULT 0,
                        `bestWinStreak` INTEGER NOT NULL DEFAULT 0,
                        `highestBotAccuracyBeaten` INTEGER NOT NULL DEFAULT 0,
                        `lastPlayedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`gameId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `play_wallets` (
                        `ownerUserId` INTEGER NOT NULL,
                        `tournamentBalance` REAL NOT NULL DEFAULT 0.0,
                        `gameBalance` REAL NOT NULL DEFAULT 0.0,
                        `equippedBoardSkinId` TEXT NOT NULL DEFAULT 'skin_royal_emerald',
                        `equippedAvatarId` TEXT NOT NULL DEFAULT 'avatar_monarch',
                        `ownedItemsCsv` TEXT NOT NULL DEFAULT 'skin_royal_emerald,avatar_monarch',
                        `skillChargesJson` TEXT NOT NULL DEFAULT '{}',
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`ownerUserId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `play_wallet_requests` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `usernameSnapshot` TEXT NOT NULL DEFAULT '',
                        `requestType` TEXT NOT NULL,
                        `channel` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `accountNumber` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `adminNote` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `play_wallet_ledger` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `walletBucket` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `amountDelta` REAL NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `play_wallets` ADD COLUMN `winningBalance` REAL NOT NULL DEFAULT 0.0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tournament_tier_configs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameId` TEXT NOT NULL,
                        `tierLabel` TEXT NOT NULL,
                        `entryFee` REAL NOT NULL,
                        `winPrize` REAL NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `game_bot_configs` (
                        `gameId` TEXT NOT NULL,
                        `botAccuracyPercent` INTEGER NOT NULL DEFAULT 65,
                        `maxConcurrentSeats` INTEGER NOT NULL DEFAULT 10,
                        `multiplayerEnabled` INTEGER NOT NULL DEFAULT 1,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`gameId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `external_tournaments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameCategory` TEXT NOT NULL DEFAULT 'FREE_FIRE',
                        `title` TEXT NOT NULL,
                        `modeLabel` TEXT NOT NULL DEFAULT 'SQUAD',
                        `mapName` TEXT NOT NULL DEFAULT 'Bermuda',
                        `entryFee` REAL NOT NULL DEFAULT 20.0,
                        `prizePool` REAL NOT NULL DEFAULT 500.0,
                        `perKillReward` REAL NOT NULL DEFAULT 10.0,
                        `maxSlots` INTEGER NOT NULL DEFAULT 48,
                        `joinedCount` INTEGER NOT NULL DEFAULT 0,
                        `startTimeLabel` TEXT NOT NULL DEFAULT 'Tonight 9:00 PM',
                        `roomIdCode` TEXT NOT NULL DEFAULT '',
                        `roomPassword` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'UPCOMING',
                        `winnerName` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `external_tournament_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tournamentId` INTEGER NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `usernameSnapshot` TEXT NOT NULL DEFAULT '',
                        `inGameName` TEXT NOT NULL DEFAULT '',
                        `inGameUid` TEXT NOT NULL DEFAULT '',
                        `entryFeePaid` REAL NOT NULL DEFAULT 0.0,
                        `joinedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `broadcast_notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `priority` TEXT NOT NULL DEFAULT 'INFO',
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `bannerKey` TEXT NOT NULL DEFAULT 'BERMUDA'")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `prizeRanksJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `matchStartTimeMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `preMatchAlertMinutes` INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `roomAlertSent` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `versionLabel` TEXT NOT NULL DEFAULT 'Mobile Only • No Emulator'")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `rulesNotice` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `external_tournaments` ADD COLUMN `adminCommissionPercent` REAL NOT NULL DEFAULT 10.0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `admin_earnings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameId` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `referenceTitle` TEXT NOT NULL DEFAULT '',
                        `grossPool` REAL NOT NULL DEFAULT 0.0,
                        `prizePaid` REAL NOT NULL DEFAULT 0.0,
                        `commissionAmount` REAL NOT NULL DEFAULT 0.0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_finance_pro.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
