package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface FinanceProfileDao {
    @Query("SELECT * FROM finance_profiles WHERE ownerUserId = :userId ORDER BY isDefault DESC, id ASC")
    fun observeByUser(userId: Long): Flow<List<FinanceProfileEntity>>

    @Query("SELECT * FROM finance_profiles WHERE ownerUserId = :userId ORDER BY isDefault DESC, id ASC")
    suspend fun getByUserOnce(userId: Long): List<FinanceProfileEntity>

    @Query("SELECT * FROM finance_profiles ORDER BY id ASC")
    suspend fun getAllOnce(): List<FinanceProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: FinanceProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<FinanceProfileEntity>)

    @Update
    suspend fun update(profile: FinanceProfileEntity)

    @Query("DELETE FROM finance_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM finance_profiles WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM finance_profiles")
    suspend fun clearAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY id ASC")
    fun observeByProfile(userId: Long, profileId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE ownerUserId = :userId ORDER BY id ASC")
    fun observeByUser(userId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY id ASC")
    suspend fun getByProfileOnce(userId: Long, profileId: Long): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE ownerUserId = :userId ORDER BY id ASC")
    suspend fun getByUserOnce(userId: Long): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllOnce(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun adjustBalance(accountId: Long, delta: Double)

    @Query("UPDATE accounts SET balance = 0.0, initialBalance = 0.0 WHERE ownerUserId = :userId")
    suspend fun resetBalancesByUser(userId: Long)

    @Query("UPDATE accounts SET balance = 0.0, initialBalance = 0.0 WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun resetBalancesByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accounts WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM accounts WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY isDefault DESC, id ASC")
    fun observeByProfile(userId: Long, profileId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE ownerUserId = :userId ORDER BY isDefault DESC, id ASC")
    fun observeByUser(userId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY id ASC")
    suspend fun getByProfileOnce(userId: Long, profileId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE ownerUserId = :userId ORDER BY id ASC")
    suspend fun getByUserOnce(userId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM categories WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE ownerUserId = :userId AND profileId = :profileId AND isDeleted = 0 ORDER BY timestamp DESC, id DESC")
    fun observeActiveByProfile(userId: Long, profileId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ownerUserId = :userId AND profileId = :profileId AND isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun observeTrashByProfile(userId: Long, profileId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ownerUserId = :userId AND isDeleted = 0 ORDER BY timestamp DESC, id DESC")
    fun observeActiveByUser(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ownerUserId = :userId AND isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun observeTrashByUser(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ownerUserId = :userId ORDER BY timestamp DESC")
    suspend fun getAllByUserOnce(userId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getAllByProfileOnce(userId: Long, profileId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<TransactionEntity>)

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE isDeleted = 1")
    suspend fun clearAllTrash()

    @Query("DELETE FROM transactions WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM transactions WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY monthKey DESC, id ASC")
    fun observeByProfile(userId: Long, profileId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE ownerUserId = :userId ORDER BY monthKey DESC, id ASC")
    fun observeByUser(userId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE ownerUserId = :userId AND profileId = :profileId AND monthKey = :monthKey")
    suspend fun getByProfileAndMonth(userId: Long, profileId: Long, monthKey: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE ownerUserId = :userId AND monthKey = :monthKey")
    suspend fun getByUserAndMonth(userId: Long, monthKey: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE ownerUserId = :userId")
    suspend fun getByUserOnce(userId: Long): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAllOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM budgets WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY expectedDate ASC")
    fun observeByProfile(userId: Long, profileId: Long): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE ownerUserId = :userId ORDER BY expectedDate ASC")
    fun observeByUser(userId: Long): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE ownerUserId = :userId")
    suspend fun getByUserOnce(userId: Long): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllOnce(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM savings_goals WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM savings_goals WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAll()
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY nextRunAt ASC")
    fun observeByProfile(userId: Long, profileId: Long): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE ownerUserId = :userId ORDER BY nextRunAt ASC")
    fun observeByUser(userId: Long): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE ownerUserId = :userId")
    suspend fun getByUserOnce(userId: Long): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1 AND nextRunAt <= :now")
    suspend fun getDueRules(now: Long): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAllOnce(): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_rules WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM recurring_rules WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun clearAll()
}

@Dao
interface WealthExtrasDao {
    @Query("SELECT * FROM loans_debts WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY dueDate ASC")
    fun observeLoansByProfile(userId: Long, profileId: Long): Flow<List<LoanDebtEntity>>

    @Query("SELECT * FROM loans_debts WHERE ownerUserId = :userId ORDER BY dueDate ASC")
    fun observeLoansByUser(userId: Long): Flow<List<LoanDebtEntity>>

    @Query("SELECT * FROM loans_debts WHERE ownerUserId = :userId")
    suspend fun getLoansByUserOnce(userId: Long): List<LoanDebtEntity>

    @Query("SELECT * FROM loans_debts")
    suspend fun getAllLoansOnce(): List<LoanDebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanDebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanDebtEntity>)

    @Update
    suspend fun updateLoan(loan: LoanDebtEntity)

    @Query("DELETE FROM loans_debts WHERE id = :id")
    suspend fun deleteLoan(id: Long)

    @Query("DELETE FROM loans_debts WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteLoansByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM loans_debts WHERE ownerUserId = :userId")
    suspend fun deleteLoansByUser(userId: Long)

    @Query("DELETE FROM loans_debts")
    suspend fun clearAllLoans()

    // Wishlist
    @Query("SELECT * FROM wishlist WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY isPurchased ASC, id DESC")
    fun observeWishlistByProfile(userId: Long, profileId: Long): Flow<List<WishlistEntity>>

    @Query("SELECT * FROM wishlist WHERE ownerUserId = :userId ORDER BY isPurchased ASC, id DESC")
    fun observeWishlistByUser(userId: Long): Flow<List<WishlistEntity>>

    @Query("SELECT * FROM wishlist WHERE ownerUserId = :userId")
    suspend fun getWishlistByUserOnce(userId: Long): List<WishlistEntity>

    @Query("SELECT * FROM wishlist")
    suspend fun getAllWishlistOnce(): List<WishlistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlist(item: WishlistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlists(items: List<WishlistEntity>)

    @Update
    suspend fun updateWishlist(item: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE id = :id")
    suspend fun deleteWishlist(id: Long)

    @Query("DELETE FROM wishlist WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteWishlistByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM wishlist WHERE ownerUserId = :userId")
    suspend fun deleteWishlistByUser(userId: Long)

    @Query("DELETE FROM wishlist")
    suspend fun clearAllWishlist()

    // Investments
    @Query("SELECT * FROM investments WHERE ownerUserId = :userId AND profileId = :profileId ORDER BY dateAdded DESC")
    fun observeInvestmentsByProfile(userId: Long, profileId: Long): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE ownerUserId = :userId ORDER BY dateAdded DESC")
    fun observeInvestmentsByUser(userId: Long): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE ownerUserId = :userId")
    suspend fun getInvestmentsByUserOnce(userId: Long): List<InvestmentEntity>

    @Query("SELECT * FROM investments")
    suspend fun getAllInvestmentsOnce(): List<InvestmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(inv: InvestmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestments(invs: List<InvestmentEntity>)

    @Update
    suspend fun updateInvestment(inv: InvestmentEntity)

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteInvestment(id: Long)

    @Query("DELETE FROM investments WHERE ownerUserId = :userId AND profileId = :profileId")
    suspend fun deleteInvestmentsByProfile(userId: Long, profileId: Long)

    @Query("DELETE FROM investments WHERE ownerUserId = :userId")
    suspend fun deleteInvestmentsByUser(userId: Long)

    @Query("DELETE FROM investments")
    suspend fun clearAllInvestments()

    // Financial Notes (legacy compatibility)
    @Query("SELECT * FROM financial_notes WHERE ownerUserId = :userId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotesByUser(userId: Long): Flow<List<FinancialNoteEntity>>

    @Query("SELECT * FROM financial_notes WHERE ownerUserId = :userId")
    suspend fun getNotesByUserOnce(userId: Long): List<FinancialNoteEntity>

    @Query("SELECT * FROM financial_notes")
    suspend fun getAllNotesOnce(): List<FinancialNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: FinancialNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<FinancialNoteEntity>)

    @Update
    suspend fun updateNote(note: FinancialNoteEntity)

    @Query("DELETE FROM financial_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM financial_notes WHERE ownerUserId = :userId")
    suspend fun deleteNotesByUser(userId: Long)

    @Query("DELETE FROM financial_notes")
    suspend fun clearAllNotes()
}

@Dao
interface KeepNoteDao {
    @Query("SELECT * FROM keep_notes WHERE ownerUserId = :userId AND isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveByUser(userId: Long): Flow<List<KeepNoteEntity>>

    @Query("SELECT * FROM keep_notes WHERE ownerUserId = :userId AND isTrashed = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedByUser(userId: Long): Flow<List<KeepNoteEntity>>

    @Query("SELECT * FROM keep_notes WHERE ownerUserId = :userId AND isTrashed = 1 ORDER BY updatedAt DESC")
    fun observeTrashedByUser(userId: Long): Flow<List<KeepNoteEntity>>

    @Query("SELECT * FROM keep_notes WHERE ownerUserId = :userId ORDER BY updatedAt DESC")
    suspend fun getByUserOnce(userId: Long): List<KeepNoteEntity>

    @Query("SELECT * FROM keep_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): KeepNoteEntity?

    @Query("SELECT * FROM keep_notes ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<KeepNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: KeepNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<KeepNoteEntity>)

    @Update
    suspend fun update(note: KeepNoteEntity)

    @Query("DELETE FROM keep_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM keep_notes WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM keep_notes")
    suspend fun clearAll()
}

@Dao
interface NoteVersionDao {
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC")
    fun observeByNote(noteId: Long): Flow<List<NoteVersionEntity>>

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC")
    suspend fun getByNoteOnce(noteId: Long): List<NoteVersionEntity>

    @Query("SELECT * FROM note_versions ORDER BY savedAt DESC")
    suspend fun getAllOnce(): List<NoteVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: NoteVersionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(versions: List<NoteVersionEntity>)

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteByNote(noteId: Long)

    @Query("DELETE FROM note_versions WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM note_versions")
    suspend fun clearAll()
}

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists WHERE ownerUserId = :userId ORDER BY sortOrder ASC, id ASC")
    fun observeByUser(userId: Long): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE ownerUserId = :userId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByUserOnce(userId: Long): List<TaskListEntity>

    @Query("SELECT * FROM task_lists ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<TaskListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: TaskListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lists: List<TaskListEntity>)

    @Update
    suspend fun update(list: TaskListEntity)

    @Query("DELETE FROM task_lists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM task_lists WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM task_lists")
    suspend fun clearAll()
}

@Dao
interface TaskItemDao {
    @Query("SELECT * FROM task_items WHERE ownerUserId = :userId ORDER BY isCompleted ASC, isStarred DESC, manualOrder ASC, CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, COALESCE(dueAt, 9223372036854775807) ASC, id DESC")
    fun observeByUser(userId: Long): Flow<List<TaskItemEntity>>

    @Query("SELECT * FROM task_items WHERE ownerUserId = :userId ORDER BY id DESC")
    suspend fun getByUserOnce(userId: Long): List<TaskItemEntity>

    @Query("SELECT * FROM task_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskItemEntity?

    @Query("SELECT * FROM task_items ORDER BY id DESC")
    suspend fun getAllOnce(): List<TaskItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskItemEntity>)

    @Update
    suspend fun update(task: TaskItemEntity)

    @Query("DELETE FROM task_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM task_items WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM task_items")
    suspend fun clearAll()
}

@Dao
interface ItemReminderDao {
    @Query("SELECT * FROM item_reminders WHERE ownerUserId = :userId AND isDismissed = 0 ORDER BY COALESCE(snoozedUntil, triggerAt) ASC")
    fun observeActiveByUser(userId: Long): Flow<List<ItemReminderEntity>>

    @Query("SELECT * FROM item_reminders WHERE ownerUserId = :userId ORDER BY triggerAt DESC")
    fun observeAllByUser(userId: Long): Flow<List<ItemReminderEntity>>

    @Query("SELECT * FROM item_reminders WHERE isDismissed = 0 AND COALESCE(snoozedUntil, triggerAt) <= :now")
    suspend fun getDueReminders(now: Long): List<ItemReminderEntity>

    @Query("SELECT * FROM item_reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ItemReminderEntity?

    @Query("SELECT * FROM item_reminders WHERE targetType = :targetType AND targetId = :targetId ORDER BY triggerAt ASC")
    suspend fun getByTargetOnce(targetType: String, targetId: Long): List<ItemReminderEntity>

    @Query("SELECT * FROM item_reminders ORDER BY triggerAt ASC")
    suspend fun getAllOnce(): List<ItemReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ItemReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ItemReminderEntity>)

    @Update
    suspend fun update(reminder: ItemReminderEntity)

    @Query("DELETE FROM item_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM item_reminders WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun deleteByTarget(targetType: String, targetId: Long)

    @Query("DELETE FROM item_reminders WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM item_reminders")
    suspend fun clearAll()
}

@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_providers WHERE ownerUserId = :userId ORDER BY isDefault DESC, id ASC")
    fun observeByUser(userId: Long): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE ownerUserId = :userId ORDER BY isDefault DESC, id ASC")
    suspend fun getByUserOnce(userId: Long): List<AiProviderEntity>

    @Query("SELECT * FROM ai_providers ORDER BY id ASC")
    suspend fun getAllOnce(): List<AiProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: AiProviderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(providers: List<AiProviderEntity>)

    @Update
    suspend fun update(provider: AiProviderEntity)

    @Query("UPDATE ai_providers SET isDefault = 0 WHERE ownerUserId = :userId")
    suspend fun clearDefaultForUser(userId: Long)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ai_providers WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM ai_providers")
    suspend fun clearAll()
}

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_messages WHERE ownerUserId = :userId AND providerId = :providerId ORDER BY timestamp ASC, id ASC")
    fun observeByUserAndProvider(userId: Long, providerId: Long): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE ownerUserId = :userId ORDER BY timestamp ASC, id ASC")
    fun observeAllByUser(userId: Long): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<AiChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: AiChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(msgs: List<AiChatMessageEntity>)

    @Query("DELETE FROM ai_chat_messages WHERE ownerUserId = :userId AND providerId = :providerId")
    suspend fun clearConversation(userId: Long, providerId: Long)

    @Query("DELETE FROM ai_chat_messages WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAll()
}

@Dao
interface QuickTodoDao {
    @Query("SELECT * FROM quick_todos WHERE ownerUserId = :userId ORDER BY isDone ASC, createdAt DESC")
    fun observeByUser(userId: Long): Flow<List<QuickTodoEntity>>

    @Query("SELECT * FROM quick_todos ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<QuickTodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QuickTodoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuickTodoEntity>)

    @Update
    suspend fun update(item: QuickTodoEntity)

    @Query("DELETE FROM quick_todos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quick_todos WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM quick_todos")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE ownerUserId = :userId")
    suspend fun getByUserOnce(userId: Long): List<NotificationEntity>

    @Query("SELECT * FROM notifications")
    suspend fun getAllOnce(): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE ownerUserId = :userId")
    suspend fun markAllRead(userId: Long)

    @Query("DELETE FROM notifications WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config")
    fun observeAll(): Flow<List<AppConfigEntity>>

    @Query("SELECT * FROM app_config")
    suspend fun getAllOnce(): List<AppConfigEntity>

    @Query("SELECT value FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(config: AppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<AppConfigEntity>)

    @Query("DELETE FROM app_config")
    suspend fun clearAll()
}

@Dao
interface GameLeaderboardDao {
    @Query("SELECT * FROM game_leaderboard_stats ORDER BY highScore DESC, wins DESC")
    fun observeAll(): Flow<List<GameLeaderboardEntity>>

    @Query("SELECT * FROM game_leaderboard_stats ORDER BY highScore DESC, wins DESC")
    suspend fun getAllOnce(): List<GameLeaderboardEntity>

    @Query("SELECT * FROM game_leaderboard_stats WHERE gameId = :gameId LIMIT 1")
    suspend fun getByGameId(gameId: String): GameLeaderboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: GameLeaderboardEntity)

    @Query("DELETE FROM game_leaderboard_stats WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: String)

    @Query("DELETE FROM game_leaderboard_stats")
    suspend fun clearAll()
}

@Dao
interface PlayWalletDao {
    @Query("SELECT * FROM play_wallets WHERE ownerUserId = :userId LIMIT 1")
    fun observeByUser(userId: Long): Flow<PlayWalletEntity?>

    @Query("SELECT * FROM play_wallets WHERE ownerUserId = :userId LIMIT 1")
    suspend fun getByUserOnce(userId: Long): PlayWalletEntity?

    @Query("SELECT * FROM play_wallets ORDER BY ownerUserId ASC")
    fun observeAll(): Flow<List<PlayWalletEntity>>

    @Query("SELECT * FROM play_wallets ORDER BY ownerUserId ASC")
    suspend fun getAllOnce(): List<PlayWalletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: PlayWalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<PlayWalletEntity>)

    @Query("DELETE FROM play_wallets WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM play_wallets")
    suspend fun clearAll()
}

@Dao
interface PlayWalletRequestDao {
    @Query("SELECT * FROM play_wallet_requests WHERE ownerUserId = :userId ORDER BY createdAt DESC, id DESC")
    fun observeByUser(userId: Long): Flow<List<PlayWalletRequestEntity>>

    @Query("SELECT * FROM play_wallet_requests ORDER BY CASE status WHEN 'PENDING' THEN 0 ELSE 1 END, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<PlayWalletRequestEntity>>

    @Query("SELECT * FROM play_wallet_requests ORDER BY createdAt DESC, id DESC")
    suspend fun getAllOnce(): List<PlayWalletRequestEntity>

    @Query("SELECT * FROM play_wallet_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PlayWalletRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: PlayWalletRequestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<PlayWalletRequestEntity>)

    @Update
    suspend fun update(request: PlayWalletRequestEntity)

    @Query("DELETE FROM play_wallet_requests WHERE createdAt < :cutoffMillis AND status != 'PENDING'")
    suspend fun deleteResolvedOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM play_wallet_requests WHERE createdAt < :cutoffMillis")
    suspend fun deleteAllOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM play_wallet_requests WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM play_wallet_requests")
    suspend fun clearAll()
}

@Dao
interface PlayWalletLedgerDao {
    @Query("SELECT * FROM play_wallet_ledger WHERE ownerUserId = :userId ORDER BY createdAt DESC, id DESC LIMIT 120")
    fun observeByUser(userId: Long): Flow<List<PlayWalletLedgerEntity>>

    @Query("SELECT * FROM play_wallet_ledger WHERE ownerUserId = :userId ORDER BY createdAt DESC, id DESC")
    suspend fun getByUserOnce(userId: Long): List<PlayWalletLedgerEntity>

    @Query("SELECT * FROM play_wallet_ledger ORDER BY createdAt DESC, id DESC")
    suspend fun getAllOnce(): List<PlayWalletLedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlayWalletLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PlayWalletLedgerEntity>)

    @Query("DELETE FROM play_wallet_ledger WHERE createdAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM play_wallet_ledger WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM play_wallet_ledger")
    suspend fun clearAll()
}

@Dao
interface TournamentTierConfigDao {
    @Query("SELECT * FROM tournament_tier_configs ORDER BY gameId ASC, sortOrder ASC, entryFee ASC")
    fun observeAll(): Flow<List<TournamentTierConfigEntity>>

    @Query("SELECT * FROM tournament_tier_configs ORDER BY gameId ASC, sortOrder ASC, entryFee ASC")
    suspend fun getAllOnce(): List<TournamentTierConfigEntity>

    @Query("SELECT * FROM tournament_tier_configs WHERE gameId = :gameId ORDER BY sortOrder ASC, entryFee ASC")
    fun observeByGame(gameId: String): Flow<List<TournamentTierConfigEntity>>

    @Query("SELECT * FROM tournament_tier_configs WHERE gameId = :gameId ORDER BY sortOrder ASC, entryFee ASC")
    suspend fun getByGameOnce(gameId: String): List<TournamentTierConfigEntity>

    @Query("SELECT COUNT(*) FROM tournament_tier_configs")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tier: TournamentTierConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tiers: List<TournamentTierConfigEntity>)

    @Query("DELETE FROM tournament_tier_configs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tournament_tier_configs")
    suspend fun clearAll()
}

@Dao
interface GameBotConfigDao {
    @Query("SELECT * FROM game_bot_configs ORDER BY gameId ASC")
    fun observeAll(): Flow<List<GameBotConfigEntity>>

    @Query("SELECT * FROM game_bot_configs ORDER BY gameId ASC")
    suspend fun getAllOnce(): List<GameBotConfigEntity>

    @Query("SELECT * FROM game_bot_configs WHERE gameId = :gameId LIMIT 1")
    suspend fun getByGameOnce(gameId: String): GameBotConfigEntity?

    @Query("SELECT COUNT(*) FROM game_bot_configs")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: GameBotConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<GameBotConfigEntity>)

    @Query("DELETE FROM game_bot_configs")
    suspend fun clearAll()
}

@Dao
interface ExternalTournamentDao {
    @Query("SELECT * FROM external_tournaments ORDER BY CASE status WHEN 'LIVE' THEN 0 WHEN 'UPCOMING' THEN 1 ELSE 2 END, createdAt DESC")
    fun observeAll(): Flow<List<ExternalTournamentEntity>>

    @Query("SELECT * FROM external_tournaments ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<ExternalTournamentEntity>

    @Query("SELECT * FROM external_tournaments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExternalTournamentEntity?

    @Query("SELECT id FROM external_tournaments WHERE gameCategory = :gameCategory AND status = 'COMPLETED'")
    suspend fun getCompletedIdsForCategory(gameCategory: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tournament: ExternalTournamentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tournaments: List<ExternalTournamentEntity>)

    @Update
    suspend fun update(tournament: ExternalTournamentEntity)

    @Query("DELETE FROM external_tournaments WHERE gameCategory = :gameCategory AND status = 'COMPLETED'")
    suspend fun deleteCompletedForCategory(gameCategory: String)

    @Query("DELETE FROM external_tournaments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM external_tournaments")
    suspend fun clearAll()
}

@Dao
interface ExternalTournamentEntryDao {
    @Query("SELECT * FROM external_tournament_entries WHERE ownerUserId = :userId ORDER BY joinedAt DESC")
    fun observeByUser(userId: Long): Flow<List<ExternalTournamentEntryEntity>>

    @Query("SELECT * FROM external_tournament_entries WHERE tournamentId = :tournamentId ORDER BY joinedAt ASC")
    fun observeByTournament(tournamentId: Long): Flow<List<ExternalTournamentEntryEntity>>

    @Query("SELECT * FROM external_tournament_entries WHERE tournamentId = :tournamentId ORDER BY joinedAt ASC")
    suspend fun getByTournamentOnce(tournamentId: Long): List<ExternalTournamentEntryEntity>

    @Query("SELECT * FROM external_tournament_entries ORDER BY joinedAt ASC")
    fun observeAll(): Flow<List<ExternalTournamentEntryEntity>>

    @Query("SELECT * FROM external_tournament_entries ORDER BY joinedAt DESC")
    suspend fun getAllOnce(): List<ExternalTournamentEntryEntity>

    @Query("SELECT * FROM external_tournament_entries WHERE tournamentId = :tournamentId AND ownerUserId = :userId LIMIT 1")
    suspend fun getByTournamentAndUser(tournamentId: Long, userId: Long): ExternalTournamentEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExternalTournamentEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ExternalTournamentEntryEntity>)

    @Query("DELETE FROM external_tournament_entries WHERE tournamentId = :tournamentId")
    suspend fun deleteByTournament(tournamentId: Long)

    @Query("DELETE FROM external_tournament_entries WHERE ownerUserId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM external_tournament_entries")
    suspend fun clearAll()
}

@Dao
interface BroadcastNotificationDao {
    @Query("SELECT * FROM broadcast_notifications ORDER BY createdAt DESC LIMIT 50")
    fun observeAll(): Flow<List<BroadcastNotificationEntity>>

    @Query("SELECT * FROM broadcast_notifications ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<BroadcastNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: BroadcastNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<BroadcastNotificationEntity>)

    @Query("DELETE FROM broadcast_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM broadcast_notifications")
    suspend fun clearAll()
}

@Dao
interface AdminEarningDao {
    @Query("SELECT * FROM admin_earnings ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<AdminEarningEntity>>

    @Query("SELECT * FROM admin_earnings ORDER BY createdAt DESC, id DESC")
    suspend fun getAllOnce(): List<AdminEarningEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(earning: AdminEarningEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(earnings: List<AdminEarningEntity>)

    @Query("DELETE FROM admin_earnings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM admin_earnings")
    suspend fun clearAll()
}



