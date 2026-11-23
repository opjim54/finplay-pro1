package com.example.data.repository

import android.app.Activity
import android.content.Context
import androidx.room.withTransaction
import com.example.data.backup.BackupScheduler
import com.example.data.backup.CloudBackupVaultRepository
import com.example.data.backup.DriveBackupPreview
import com.example.data.backup.DriveRestoreResult
import com.example.data.backup.DriveSyncResult
import com.example.data.backup.ExportManager
import com.example.data.backup.FullBackupSnapshot
import com.example.data.backup.GoogleDriveClient
import com.example.data.backup.RecurringWorker
import com.example.data.backup.toBackupPreferences
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.FinanceProfileEntity
import com.example.data.local.FinancialNoteEntity
import com.example.data.local.GameLeaderboardEntity
import com.example.data.local.GameMatchResult
import com.example.data.local.InvestmentEntity
import com.example.data.local.ItemReminderEntity
import com.example.data.local.KeepNoteEntity
import com.example.data.local.LoanDebtEntity
import com.example.data.local.NoteVersionEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.QuickTodoEntity
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskListEntity
import com.example.data.local.TaskRepeat
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferencesState
import com.example.data.local.WishlistEntity
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.domain.NotificationHelper
import com.example.domain.ReminderScheduler
import com.example.domain.TaskRepeatEngine
import com.example.i18n.AppLanguage
import com.example.security.CryptoUtil
import com.example.security.SecurityShield
import com.example.widget.BalanceSummaryWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FinanceRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    val prefs = PreferencesStore(context)
    val playWalletRepo = com.example.data.games.PlayWalletRepository(db)
    val driveClient = GoogleDriveClient(context)
    val cloudVault = CloudBackupVaultRepository(context)
    private val seedMutex = Mutex()

    val prefsFlow: Flow<UserPreferencesState> = prefs.state

    suspend fun ensureInitialized() = seedMutex.withLock {
        val users = db.userDao().getAllUsersOnce()
        if (users.isEmpty()) {
            val (salt, passHash, ansHash) = withContext(Dispatchers.Default) {
                val s = CryptoUtil.generateSalt()
                Triple(s, CryptoUtil.hashSecret("1", s), CryptoUtil.hashSecret("dhaka", s))
            }
            val userId = db.userDao().insert(
                UserEntity(
                    username = "user",
                    passwordHash = passHash,
                    salt = salt,
                    displayName = "Smart User",
                    securityQuestion = "Favorite city?",
                    securityAnswerHash = ansHash
                )
            )
            seedDefaultsForUser(userId)
        } else {
            users.forEach { user ->
                ensureDefaultProfileForUser(user.id)
                ensureDefaultTaskListsForUser(user.id)
            }
        }
        val adminHash = db.appConfigDao().getValue("admin_key_hash")
        val adminKeyRotated = db.appConfigDao().getValue("admin_key_rotated") == "true"
        val needsSealedAdminKeyRefresh = adminHash.isNullOrBlank() ||
            (!adminKeyRotated && (!adminHash.startsWith(CryptoUtil.PBKDF2_PREFIX) || adminHash == CryptoUtil.LEGACY_SEALED_ADMIN_KEY_HASH))
        if (needsSealedAdminKeyRefresh) {
            db.appConfigDao().set(AppConfigEntity("admin_key_salt", CryptoUtil.SEALED_ADMIN_KEY_SALT))
            db.appConfigDao().set(AppConfigEntity("admin_key_hash", CryptoUtil.SEALED_ADMIN_KEY_HASH))
            if (db.appConfigDao().getValue("default_language").isNullOrBlank()) {
                db.appConfigDao().set(AppConfigEntity("default_language", AppLanguage.BANGLA.code))
            }
            if (db.appConfigDao().getValue("default_currency").isNullOrBlank()) {
                db.appConfigDao().set(AppConfigEntity("default_currency", "BDT"))
            }
        }
        val secretUserHash = db.appConfigDao().getValue("secret_admin_user_hash")
        val secretPassHash = db.appConfigDao().getValue("secret_admin_pass_hash")
        if (secretUserHash.isNullOrBlank() || secretPassHash.isNullOrBlank()) {
            // Migrate any legacy plain username if present, or seed zero-plaintext PBKDF2 digests
            val legacyUser = db.appConfigDao().getValue("secret_admin_username")
            if (!legacyUser.isNullOrBlank()) {
                val (uSalt, uHash) = withContext(Dispatchers.Default) {
                    val s = CryptoUtil.generateSalt()
                    s to CryptoUtil.hashSecret(legacyUser, s)
                }
                db.appConfigDao().set(AppConfigEntity("secret_admin_user_salt", uSalt))
                db.appConfigDao().set(AppConfigEntity("secret_admin_user_hash", uHash))
                db.appConfigDao().set(AppConfigEntity("secret_admin_username", ""))
            } else {
                db.appConfigDao().set(AppConfigEntity("secret_admin_user_salt", CryptoUtil.SEALED_SECRET_ADMIN_USER_SALT))
                db.appConfigDao().set(AppConfigEntity("secret_admin_user_hash", CryptoUtil.SEALED_SECRET_ADMIN_USER_HASH))
            }
            if (secretPassHash.isNullOrBlank()) {
                db.appConfigDao().set(AppConfigEntity("secret_admin_pass_salt", CryptoUtil.SEALED_SECRET_ADMIN_PASS_SALT))
                db.appConfigDao().set(AppConfigEntity("secret_admin_pass_hash", CryptoUtil.SEALED_SECRET_ADMIN_PASS_HASH))
            }
        }
        playWalletRepo.ensureDefaultGameConfigs()
    }

    private suspend fun ensureDefaultProfileForUser(userId: Long): Long {
        val existing = db.financeProfileDao().getByUserOnce(userId)
        if (existing.isNotEmpty()) {
            return existing.firstOrNull { it.isDefault }?.id ?: existing.first().id
        }
        return db.financeProfileDao().insert(
            FinanceProfileEntity(
                ownerUserId = userId,
                name = "Personal",
                iconKey = "person",
                colorHex = 0xFF047857,
                description = "Main personal finance ledger",
                isDefault = true
            )
        )
    }

    private suspend fun ensureDefaultTaskListsForUser(userId: Long) {
        if (db.taskListDao().getByUserOnce(userId).isNotEmpty()) return
        val defaults = listOf(
            TaskListEntity(ownerUserId = userId, name = "My Tasks", colorHex = 0xFF046C4E, isSystemDefault = true, sortOrder = 0),
            TaskListEntity(ownerUserId = userId, name = "Personal", colorHex = 0xFF0F766E, isSystemDefault = true, sortOrder = 1),
            TaskListEntity(ownerUserId = userId, name = "University", colorHex = 0xFF2563EB, isSystemDefault = true, sortOrder = 2),
            TaskListEntity(ownerUserId = userId, name = "Work", colorHex = 0xFFD97706, isSystemDefault = true, sortOrder = 3),
            TaskListEntity(ownerUserId = userId, name = "Shopping", colorHex = 0xFFDB2777, isSystemDefault = true, sortOrder = 4),
            TaskListEntity(ownerUserId = userId, name = "Important", colorHex = 0xFFDC2626, isSystemDefault = true, sortOrder = 5),
            TaskListEntity(ownerUserId = userId, name = "Future Plans", colorHex = 0xFF7C3AED, isSystemDefault = true, sortOrder = 6)
        )
        db.taskListDao().insertAll(defaults)
    }

    suspend fun seedDefaultsForUser(userId: Long) {
        val defaultProfileId = ensureDefaultProfileForUser(userId)
        seedDefaultsForProfile(userId, defaultProfileId)
        ensureDefaultTaskListsForUser(userId)
    }

    suspend fun seedDefaultsForProfile(userId: Long, profileId: Long) {
        if (db.accountDao().getByProfileOnce(userId, profileId).isEmpty()) {
            db.accountDao().insertAll(
                listOf(
                    AccountEntity(ownerUserId = userId, profileId = profileId, name = "Cash", type = AccountType.CASH, balance = 0.0, initialBalance = 0.0, colorHex = 0xFF047857, iconKey = "payments"),
                    AccountEntity(ownerUserId = userId, profileId = profileId, name = "Bank", type = AccountType.BANK, balance = 0.0, initialBalance = 0.0, colorHex = 0xFF0284C7, iconKey = "account_balance"),
                    AccountEntity(ownerUserId = userId, profileId = profileId, name = "Bkash", type = AccountType.BKASH, balance = 0.0, initialBalance = 0.0, colorHex = 0xFFD946EF, iconKey = "smartphone"),
                    AccountEntity(ownerUserId = userId, profileId = profileId, name = "Nagad", type = AccountType.NAGAD, balance = 0.0, initialBalance = 0.0, colorHex = 0xFFEA580C, iconKey = "smartphone"),
                    AccountEntity(ownerUserId = userId, profileId = profileId, name = "Rocket", type = AccountType.ROCKET, balance = 0.0, initialBalance = 0.0, colorHex = 0xFF7C3AED, iconKey = "bolt"),
                    AccountEntity(ownerUserId = userId, profileId = profileId, name = "Card", type = AccountType.CARD, balance = 0.0, initialBalance = 0.0, colorHex = 0xFF0F766E, iconKey = "credit_card")
                )
            )
        }
        if (db.categoryDao().getByProfileOnce(userId, profileId).isEmpty()) {
            db.categoryDao().insertAll(
                listOf(
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Food", localizationKey = "cat_food", type = TransactionType.EXPENSE, iconKey = "restaurant", colorHex = 0xFFEF4444, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Transport", localizationKey = "cat_transport", type = TransactionType.EXPENSE, iconKey = "directions_car", colorHex = 0xFF3B82F6, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Shopping", localizationKey = "cat_shopping", type = TransactionType.EXPENSE, iconKey = "shopping_bag", colorHex = 0xFFEC4899, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Medical", localizationKey = "cat_medical", type = TransactionType.EXPENSE, iconKey = "medical_services", colorHex = 0xFF10B981, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Education", localizationKey = "cat_education", type = TransactionType.EXPENSE, iconKey = "school", colorHex = 0xFF6366F1, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Family", localizationKey = "cat_family", type = TransactionType.EXPENSE, iconKey = "family_restroom", colorHex = 0xFFF59E0B, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Business", localizationKey = "cat_business", type = TransactionType.EXPENSE, iconKey = "business_center", colorHex = 0xFF0EA5E9, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Bills", localizationKey = "cat_bills", type = TransactionType.EXPENSE, iconKey = "receipt_long", colorHex = 0xFFF97316, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Entertainment", localizationKey = "cat_entertainment", type = TransactionType.EXPENSE, iconKey = "movie", colorHex = 0xFF8B5CF6, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Travel", localizationKey = "cat_travel", type = TransactionType.EXPENSE, iconKey = "flight", colorHex = 0xFF14B8A6, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Others", localizationKey = "cat_others", type = TransactionType.EXPENSE, iconKey = "category", colorHex = 0xFF64748B, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Salary", localizationKey = "cat_salary", type = TransactionType.INCOME, iconKey = "work", colorHex = 0xFF059669, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Freelancing", localizationKey = "cat_freelancing", type = TransactionType.INCOME, iconKey = "laptop_mac", colorHex = 0xFF0D9488, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Investment", localizationKey = "cat_investment", type = TransactionType.INCOME, iconKey = "trending_up", colorHex = 0xFF2563EB, isDefault = true),
                    CategoryEntity(ownerUserId = userId, profileId = profileId, name = "Savings", localizationKey = "cat_savings", type = TransactionType.INCOME, iconKey = "savings", colorHex = 0xFF7C3AED, isDefault = true)
                )
            )
        }
    }

    // --- Authentication ---
    suspend fun authenticateUser(username: String, password: String, remember: Boolean): Result<UserEntity> {
        ensureInitialized()
        val user = db.userDao().getByUsername(username.trim())
            ?: return Result.failure(IllegalArgumentException("INVALID"))
        if (user.isLocked) {
            return Result.failure(IllegalStateException("LOCKED"))
        }
        val valid = withContext(Dispatchers.Default) {
            CryptoUtil.verifySecret(password, user.salt, user.passwordHash)
        }
        if (!valid) {
            return Result.failure(IllegalArgumentException("INVALID"))
        }
        val profiles = db.financeProfileDao().getByUserOnce(user.id)
        val curActive = prefs.getSnapshot().activeProfileId
        val validProfileId = profiles.firstOrNull { it.id == curActive }?.id
            ?: profiles.firstOrNull()?.id
            ?: ensureDefaultProfileForUser(user.id)

        prefs.setSession(user.id, remember, isAdmin = false)
        prefs.setActiveProfileId(validProfileId)
        processDueRecurringRules()
        BalanceSummaryWidget.refreshAll(context)
        return Result.success(user)
    }

    suspend fun registerUser(
        username: String,
        password: String,
        displayName: String,
        securityQuestion: String,
        securityAnswer: String
    ): Result<UserEntity> {
        ensureInitialized()
        val trimmed = username.trim()
        if (db.userDao().getByUsername(trimmed) != null) {
            return Result.failure(IllegalStateException("EXISTS"))
        }
        val (salt, passHash, ansHash) = withContext(Dispatchers.Default) {
            val s = CryptoUtil.generateSalt()
            Triple(
                s,
                CryptoUtil.hashSecret(password, s),
                CryptoUtil.hashSecret(securityAnswer.trim().lowercase(), s)
            )
        }
        val user = UserEntity(
            username = trimmed,
            passwordHash = passHash,
            salt = salt,
            displayName = displayName.trim().ifEmpty { trimmed },
            securityQuestion = securityQuestion.trim().ifEmpty { "Favorite city?" },
            securityAnswerHash = ansHash
        )
        val newId = db.userDao().insert(user)
        val created = user.copy(id = newId)
        seedDefaultsForUser(newId)
        val profileId = ensureDefaultProfileForUser(newId)
        prefs.setSession(newId, remember = true, isAdmin = false)
        prefs.setActiveProfileId(profileId)
        triggerAutoBackupIfEnabled()
        return Result.success(created)
    }

    suspend fun resetForgottenPassword(username: String, securityAnswer: String, newPassword: String): Result<Unit> {
        val user = db.userDao().getByUsername(username.trim())
            ?: return Result.failure(IllegalArgumentException("INVALID"))
        val ok = CryptoUtil.verifySecret(securityAnswer.trim().lowercase(), user.salt, user.securityAnswerHash)
        if (!ok) return Result.failure(IllegalArgumentException("WRONG_ANSWER"))
        val newSalt = CryptoUtil.generateSalt()
        db.userDao().update(
            user.copy(
                salt = newSalt,
                passwordHash = CryptoUtil.hashSecret(newPassword, newSalt)
            )
        )
        triggerAutoBackupIfEnabled()
        return Result.success(Unit)
    }

    suspend fun changePassword(userId: Long, currentPassword: String, newPassword: String): Result<Unit> {
        val user = db.userDao().getById(userId) ?: return Result.failure(IllegalStateException("NOT_FOUND"))
        if (!CryptoUtil.verifySecret(currentPassword, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("INVALID"))
        }
        val newSalt = CryptoUtil.generateSalt()
        db.userDao().update(
            user.copy(salt = newSalt, passwordHash = CryptoUtil.hashSecret(newPassword, newSalt))
        )
        triggerAutoBackupIfEnabled()
        return Result.success(Unit)
    }

    sealed interface LoginOutcome {
        data class UserSession(val user: UserEntity) : LoginOutcome
        data object AdminSession : LoginOutcome
    }

    sealed interface AdminAuthResult {
        data object Success : AdminAuthResult
        data object InvalidKey : AdminAuthResult
        data class LockedOut(val remainingSeconds: Long) : AdminAuthResult
    }

    /**
     * Same-APK Hardened Dual Login (Zero-Plaintext PBKDF2-HMAC-SHA256 + Brute-Force Lockout):
     * - Verifies encrypted Secret Admin Username & Password digests off the Main thread without any plaintext keys in bytecode.
     * - Regular users authenticate cleanly into the FinPlay Pro user experience.
     */
    suspend fun authenticateUserOrSecretAdmin(
        username: String,
        password: String,
        remember: Boolean
    ): Result<LoginOutcome> {
        ensureInitialized()
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val userSalt = db.appConfigDao().getValue("secret_admin_user_salt")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_SECRET_ADMIN_USER_SALT
        val userHash = db.appConfigDao().getValue("secret_admin_user_hash")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_SECRET_ADMIN_USER_HASH
        val passSalt = db.appConfigDao().getValue("secret_admin_pass_salt")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_SECRET_ADMIN_PASS_SALT
        val passHash = db.appConfigDao().getValue("secret_admin_pass_hash")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_SECRET_ADMIN_PASS_HASH

        val isSecretUserMatch = withContext(Dispatchers.Default) {
            CryptoUtil.verifySecret(cleanUser, userSalt, userHash)
        }
        if (isSecretUserMatch) {
            val lockoutSec = SecurityShield.remainingAdminLockoutSeconds()
            if (lockoutSec > 0L) {
                return Result.failure(IllegalStateException("LOCKED"))
            }
            val isPassMatch = withContext(Dispatchers.Default) {
                CryptoUtil.verifySecret(password, passSalt, passHash) ||
                    (cleanPass != password && CryptoUtil.verifySecret(cleanPass, passSalt, passHash))
            }
            if (isPassMatch) {
                SecurityShield.resetAdminAttempts()
                prefs.setSession(userId = -1L, remember = false, isAdmin = true)
                return Result.success(LoginOutcome.AdminSession)
            } else {
                SecurityShield.recordFailedAdminAttempt()
                return Result.failure(IllegalArgumentException("INVALID"))
            }
        }
        return authenticateUser(cleanUser, password, remember).map { LoginOutcome.UserSession(it) }
    }

    suspend fun adminUpdateSecretCredentials(newUsername: String, newPassword: String): Result<Unit> = runCatching {
        val cleanUser = newUsername.trim()
        val cleanPass = newPassword.trim()
        require(cleanUser.length >= 4) { "Secret admin username must be at least 4 characters" }
        require(cleanPass.length >= 8) { "Secret admin password must be at least 8 characters for PBKDF2 hardening" }
        val (userSalt, userHash, passSalt, passHash) = withContext(Dispatchers.Default) {
            val uS = CryptoUtil.generateSalt()
            val pS = CryptoUtil.generateSalt()
            listOf(uS, CryptoUtil.hashSecret(cleanUser, uS), pS, CryptoUtil.hashSecret(cleanPass, pS))
        }
        db.appConfigDao().set(AppConfigEntity("secret_admin_username", ""))
        db.appConfigDao().set(AppConfigEntity("secret_admin_user_salt", userSalt))
        db.appConfigDao().set(AppConfigEntity("secret_admin_user_hash", userHash))
        db.appConfigDao().set(AppConfigEntity("secret_admin_pass_salt", passSalt))
        db.appConfigDao().set(AppConfigEntity("secret_admin_pass_hash", passHash))
    }

    suspend fun adminUpdateAdminKey(newAdminKey: String): Result<Unit> = runCatching {
        val cleanKey = newAdminKey.trim()
        require(cleanKey.length >= 6) { "Admin Portal Key must be at least 6 characters" }
        val (salt, hash) = withContext(Dispatchers.Default) {
            val s = CryptoUtil.generateSalt()
            s to CryptoUtil.hashSecret(cleanKey, s)
        }
        db.appConfigDao().set(AppConfigEntity("admin_key_salt", salt))
        db.appConfigDao().set(AppConfigEntity("admin_key_hash", hash))
        db.appConfigDao().set(AppConfigEntity("admin_key_rotated", "true"))
    }

    suspend fun authenticateAdminDetailed(adminKey: String): AdminAuthResult {
        ensureInitialized()
        val lockoutSec = SecurityShield.remainingAdminLockoutSeconds()
        if (lockoutSec > 0L) return AdminAuthResult.LockedOut(lockoutSec)

        val cleanKey = adminKey.trim()
        if (cleanKey.isEmpty()) return AdminAuthResult.InvalidKey

        val salt = db.appConfigDao().getValue("admin_key_salt")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_ADMIN_KEY_SALT
        val hash = db.appConfigDao().getValue("admin_key_hash")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_ADMIN_KEY_HASH
        val isRotated = db.appConfigDao().getValue("admin_key_rotated") == "true"

        val secretPassSalt = db.appConfigDao().getValue("secret_admin_pass_salt")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_SECRET_ADMIN_PASS_SALT
        val secretPassHash = db.appConfigDao().getValue("secret_admin_pass_hash")
            ?.takeIf { it.isNotBlank() } ?: CryptoUtil.SEALED_SECRET_ADMIN_PASS_HASH

        val ok = withContext(Dispatchers.Default) {
            CryptoUtil.verifySecret(cleanKey, salt, hash) ||
                (!isRotated && hash != CryptoUtil.SEALED_ADMIN_KEY_HASH &&
                    CryptoUtil.verifySecret(cleanKey, CryptoUtil.SEALED_ADMIN_KEY_SALT, CryptoUtil.SEALED_ADMIN_KEY_HASH)) ||
                (!isRotated && CryptoUtil.verifySecret(cleanKey, CryptoUtil.SEALED_ADMIN_KEY_SALT, CryptoUtil.LEGACY_SEALED_ADMIN_KEY_HASH)) ||
                CryptoUtil.verifySecret(cleanKey, secretPassSalt, secretPassHash)
        }

        return if (ok) {
            SecurityShield.resetAdminAttempts()
            prefs.setSession(userId = -1L, remember = false, isAdmin = true)
            AdminAuthResult.Success
        } else {
            val newLockout = SecurityShield.recordFailedAdminAttempt()
            if (newLockout > 0L) AdminAuthResult.LockedOut(newLockout) else AdminAuthResult.InvalidKey
        }
    }

    suspend fun authenticateAdmin(adminKey: String): Boolean {
        return authenticateAdminDetailed(adminKey) is AdminAuthResult.Success
    }

    suspend fun logout() {
        prefs.clearSession()
    }

    // --- Finance Profile Management ---
    fun observeProfiles(userId: Long): Flow<List<FinanceProfileEntity>> =
        db.financeProfileDao().observeByUser(userId)

    suspend fun switchProfile(profileId: Long) {
        prefs.setActiveProfileId(profileId)
    }

    suspend fun createFinanceProfile(
        userId: Long,
        name: String,
        iconKey: String,
        colorHex: Long,
        description: String = ""
    ): Long {
        val newProfileId = db.financeProfileDao().insert(
            FinanceProfileEntity(
                ownerUserId = userId,
                name = name.trim(),
                iconKey = iconKey,
                colorHex = colorHex,
                description = description.trim(),
                isDefault = false
            )
        )
        seedDefaultsForProfile(userId, newProfileId)
        prefs.setActiveProfileId(newProfileId)
        onDataMutated()
        return newProfileId
    }

    suspend fun updateFinanceProfile(profile: FinanceProfileEntity) {
        db.financeProfileDao().update(profile)
        onDataMutated()
    }

    suspend fun deleteFinanceProfile(userId: Long, profileId: Long): Boolean {
        val profiles = db.financeProfileDao().getByUserOnce(userId)
        if (profiles.size <= 1) return false
        val target = profiles.firstOrNull { it.id == profileId } ?: return false
        db.withTransaction {
            db.transactionDao().deleteByProfile(userId, profileId)
            db.budgetDao().deleteByProfile(userId, profileId)
            db.savingsGoalDao().deleteByProfile(userId, profileId)
            db.recurringRuleDao().deleteByProfile(userId, profileId)
            db.wealthExtrasDao().deleteLoansByProfile(userId, profileId)
            db.wealthExtrasDao().deleteWishlistByProfile(userId, profileId)
            db.wealthExtrasDao().deleteInvestmentsByProfile(userId, profileId)
            db.accountDao().deleteByProfile(userId, profileId)
            db.categoryDao().deleteByProfile(userId, profileId)
            db.financeProfileDao().deleteById(target.id)
        }
        val remaining = db.financeProfileDao().getByUserOnce(userId)
        remaining.firstOrNull()?.let { prefs.setActiveProfileId(it.id) }
        onDataMutated(syncCloudImmediately = true)
        return true
    }

    // --- User & Profile-Scoped Reactive Streams ---
    fun observeUser(userId: Long): Flow<UserEntity?> = db.userDao().observeById(userId)

    fun observeAccounts(userId: Long, profileId: Long): Flow<List<AccountEntity>> =
        db.accountDao().observeByProfile(userId, profileId)

    fun observeCategories(userId: Long, profileId: Long): Flow<List<CategoryEntity>> =
        db.categoryDao().observeByProfile(userId, profileId)

    fun observeTransactions(userId: Long, profileId: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().observeActiveByProfile(userId, profileId)

    fun observeAllTransactionsByUser(userId: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().observeActiveByUser(userId)

    fun observeAllAccountsByUser(userId: Long): Flow<List<AccountEntity>> =
        db.accountDao().observeByUser(userId)

    fun observeTrash(userId: Long, profileId: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().observeTrashByProfile(userId, profileId)

    fun observeBudgets(userId: Long, profileId: Long): Flow<List<BudgetEntity>> =
        db.budgetDao().observeByProfile(userId, profileId)

    fun observeSavingsGoals(userId: Long, profileId: Long): Flow<List<SavingsGoalEntity>> =
        db.savingsGoalDao().observeByProfile(userId, profileId)

    fun observeRecurringRules(userId: Long, profileId: Long): Flow<List<RecurringRuleEntity>> =
        db.recurringRuleDao().observeByProfile(userId, profileId)

    fun observeLoans(userId: Long, profileId: Long): Flow<List<LoanDebtEntity>> =
        db.wealthExtrasDao().observeLoansByProfile(userId, profileId)

    fun observeWishlist(userId: Long, profileId: Long): Flow<List<WishlistEntity>> =
        db.wealthExtrasDao().observeWishlistByProfile(userId, profileId)

    fun observeInvestments(userId: Long, profileId: Long): Flow<List<InvestmentEntity>> =
        db.wealthExtrasDao().observeInvestmentsByProfile(userId, profileId)

    fun observeNotes(userId: Long): Flow<List<FinancialNoteEntity>> = db.wealthExtrasDao().observeNotesByUser(userId)
    fun observeNotifications(userId: Long): Flow<List<NotificationEntity>> = db.notificationDao().observeByUser(userId)

    // --- Shared Keep Notes, Tasks, Versions, Lists, Reminders, AI & Quick To-Do ---
    fun observeKeepNotes(userId: Long): Flow<List<KeepNoteEntity>> = db.keepNoteDao().observeActiveByUser(userId)
    fun observeArchivedKeepNotes(userId: Long): Flow<List<KeepNoteEntity>> = db.keepNoteDao().observeArchivedByUser(userId)
    fun observeTrashedKeepNotes(userId: Long): Flow<List<KeepNoteEntity>> = db.keepNoteDao().observeTrashedByUser(userId)
    fun observeNoteVersions(noteId: Long): Flow<List<NoteVersionEntity>> = db.noteVersionDao().observeByNote(noteId)
    fun observeTaskLists(userId: Long): Flow<List<TaskListEntity>> = db.taskListDao().observeByUser(userId)
    fun observeTaskItems(userId: Long): Flow<List<TaskItemEntity>> = db.taskItemDao().observeByUser(userId)
    fun observeActiveReminders(userId: Long): Flow<List<ItemReminderEntity>> = db.itemReminderDao().observeActiveByUser(userId)
    fun observeAllReminders(userId: Long): Flow<List<ItemReminderEntity>> = db.itemReminderDao().observeAllByUser(userId)
    fun observeAiProviders(userId: Long): Flow<List<AiProviderEntity>> = db.aiProviderDao().observeByUser(userId)
    fun observeAiChatMessages(userId: Long, providerId: Long): Flow<List<AiChatMessageEntity>> =
        db.aiChatDao().observeByUserAndProvider(userId, providerId)
    fun observeQuickTodos(userId: Long): Flow<List<QuickTodoEntity>> = db.quickTodoDao().observeByUser(userId)

    // --- Keep Notes & Tasks Mutations ---
    suspend fun saveKeepNote(note: KeepNoteEntity, saveVersionSnapshot: Boolean = true): Long {
        val now = System.currentTimeMillis()
        val existing = if (note.id != 0L) db.keepNoteDao().getById(note.id) else null
        val contentChanged = existing == null ||
            existing.title != note.title ||
            existing.content != note.content ||
            existing.blocksJson != note.blocksJson ||
            existing.checklistJson != note.checklistJson
        val nextVersion = if (existing == null) 1 else if (saveVersionSnapshot && contentChanged) existing.versionCount + 1 else existing.versionCount

        val id = db.keepNoteDao().insert(
            note.copy(
                createdAt = existing?.createdAt ?: if (note.createdAt > 0L && note.id != 0L) note.createdAt else now,
                updatedAt = now,
                versionCount = nextVersion
            )
        )
        if (saveVersionSnapshot && contentChanged && (note.title.isNotBlank() || note.content.isNotBlank() || note.blocksJson != "[]")) {
            db.noteVersionDao().insert(
                NoteVersionEntity(
                    ownerUserId = note.ownerUserId,
                    noteId = id,
                    titleSnapshot = note.title,
                    contentSnapshot = note.content,
                    blocksJsonSnapshot = note.blocksJson,
                    checklistJsonSnapshot = note.checklistJson,
                    changeSummary = if (existing == null) "Created note" else "Updated v$nextVersion",
                    savedAt = now
                )
            )
        }
        onDataMutated()
        return id
    }

    suspend fun restoreNoteVersion(noteId: Long, version: NoteVersionEntity) {
        val existing = db.keepNoteDao().getById(noteId) ?: return
        saveKeepNote(
            note = existing.copy(
                title = version.titleSnapshot,
                content = version.contentSnapshot,
                blocksJson = version.blocksJsonSnapshot,
                checklistJson = version.checklistJsonSnapshot
            ),
            saveVersionSnapshot = true
        )
    }

    suspend fun bulkUpdateNotes(notes: List<KeepNoteEntity>) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            notes.forEach { db.keepNoteDao().update(it.copy(updatedAt = now)) }
        }
        onDataMutated()
    }

    suspend fun deleteKeepNotePermanently(noteId: Long) {
        db.withTransaction {
            db.noteVersionDao().deleteByNote(noteId)
            db.itemReminderDao().deleteByTarget("NOTE", noteId)
            db.keepNoteDao().deleteById(noteId)
        }
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveTaskList(list: TaskListEntity): Long {
        val id = db.taskListDao().insert(list)
        onDataMutated()
        return id
    }

    suspend fun deleteTaskList(listId: Long) {
        db.taskListDao().deleteById(listId)
        onDataMutated()
    }

    suspend fun saveTaskItem(task: TaskItemEntity): Long {
        val id = db.taskItemDao().insert(task)
        onDataMutated()
        return id
    }

    suspend fun toggleTaskCompleted(task: TaskItemEntity) {
        val now = System.currentTimeMillis()
        val markingDone = !task.isCompleted
        if (markingDone && task.repeatRule != TaskRepeat.NONE) {
            val nextDue = TaskRepeatEngine.advanceDueAt(
                currentDueAt = task.dueAt,
                repeatRule = task.repeatRule,
                repeatInterval = task.repeatInterval,
                repeatUnit = task.repeatUnit,
                now = now
            )
            if (TaskRepeatEngine.shouldSpawnNextOccurrence(
                    repeatRule = task.repeatRule,
                    nextDueAt = nextDue,
                    repeatUntilAt = task.repeatUntilAt,
                    repeatRemainingCount = task.repeatRemainingCount
                )
            ) {
                db.withTransaction {
                    // Save completed history item
                    db.taskItemDao().insert(
                        task.copy(
                            id = 0L,
                            isCompleted = true,
                            completedAt = now,
                            repeatRule = TaskRepeat.NONE
                        )
                    )
                    // Advance active recurring task to next due time
                    val nextRem = if (task.reminderAt != null && task.dueAt != null && nextDue != null) {
                        val delta = task.reminderAt - task.dueAt
                        nextDue + delta
                    } else null
                    db.taskItemDao().update(
                        task.copy(
                            dueAt = nextDue,
                            reminderAt = nextRem,
                            isCompleted = false,
                            completedAt = null,
                            repeatRemainingCount = TaskRepeatEngine.decrementedCount(task.repeatRemainingCount)
                        )
                    )
                }
                onDataMutated()
                return
            }
        }
        db.taskItemDao().update(
            task.copy(
                isCompleted = markingDone,
                completedAt = if (markingDone) now else null
            )
        )
        onDataMutated()
    }

    suspend fun reorderTasks(tasks: List<TaskItemEntity>) {
        db.withTransaction {
            tasks.forEachIndexed { idx, task ->
                db.taskItemDao().update(task.copy(manualOrder = idx))
            }
        }
        onDataMutated()
    }

    suspend fun deleteTaskItem(taskId: Long) {
        db.withTransaction {
            db.itemReminderDao().deleteByTarget("TASK", taskId)
            db.taskItemDao().deleteById(taskId)
        }
        onDataMutated(syncCloudImmediately = true)
    }

    // --- Item Reminder Mutations ---
    suspend fun saveItemReminder(reminder: ItemReminderEntity): Long {
        val id = db.itemReminderDao().insert(reminder)
        ReminderScheduler.scheduleNextExactCheck(context, reminder.snoozedUntil ?: reminder.triggerAt)
        onDataMutated()
        return id
    }

    suspend fun snoozeReminder(reminderId: Long, minutes: Int) {
        val existing = db.itemReminderDao().getById(reminderId) ?: return
        val until = System.currentTimeMillis() + minutes * 60_000L
        db.itemReminderDao().update(
            existing.copy(snoozedUntil = until, isDismissed = false)
        )
        ReminderScheduler.scheduleNextExactCheck(context, until)
        onDataMutated()
    }

    suspend fun rescheduleReminder1Day(reminderId: Long) {
        val existing = db.itemReminderDao().getById(reminderId) ?: return
        val now = System.currentTimeMillis()
        val nextAt = maxOf(existing.triggerAt, now) + 86_400_000L
        db.itemReminderDao().update(
            existing.copy(triggerAt = nextAt, snoozedUntil = null, isTriggered = false, isDismissed = false)
        )
        ReminderScheduler.scheduleNextExactCheck(context, nextAt)
        onDataMutated()
    }

    suspend fun dismissReminder(reminderId: Long) {
        val existing = db.itemReminderDao().getById(reminderId) ?: return
        db.itemReminderDao().update(existing.copy(isDismissed = true, snoozedUntil = null))
        onDataMutated()
    }

    suspend fun deleteItemReminder(reminderId: Long) {
        db.itemReminderDao().deleteById(reminderId)
        onDataMutated()
    }

    // --- AI Providers & Chat Mutations ---
    suspend fun saveAiProvider(provider: AiProviderEntity): Long {
        val id = db.withTransaction {
            if (provider.isDefault) {
                db.aiProviderDao().clearDefaultForUser(provider.ownerUserId)
            }
            db.aiProviderDao().insert(provider)
        }
        onDataMutated()
        return id
    }

    suspend fun setDefaultAiProvider(userId: Long, providerId: Long) {
        db.withTransaction {
            db.aiProviderDao().clearDefaultForUser(userId)
            val existing = db.aiProviderDao().getByUserOnce(userId).firstOrNull { it.id == providerId }
            if (existing != null) {
                db.aiProviderDao().update(existing.copy(isDefault = true))
            }
        }
        onDataMutated()
    }

    suspend fun deleteAiProvider(providerId: Long) {
        db.aiProviderDao().deleteById(providerId)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveAiChatMessage(message: AiChatMessageEntity): Long {
        val id = db.aiChatDao().insert(message)
        onDataMutated()
        return id
    }

    suspend fun clearAiChatConversation(userId: Long, providerId: Long) {
        db.aiChatDao().clearConversation(userId, providerId)
        onDataMutated(syncCloudImmediately = true)
    }

    // --- Quick To-Do Mutations ---
    suspend fun saveQuickTodo(item: QuickTodoEntity): Long {
        val id = db.quickTodoDao().insert(item)
        onDataMutated()
        return id
    }

    suspend fun deleteQuickTodo(id: Long) {
        db.quickTodoDao().deleteById(id)
        onDataMutated(syncCloudImmediately = true)
    }

    // --- Atomic Transaction Operations ---
    suspend fun saveTransaction(tx: TransactionEntity): Long {
        val savedId = db.withTransaction {
            val existing = if (tx.id != 0L) db.transactionDao().getById(tx.id) else null
            if (existing != null && !existing.isDeleted) {
                applyBalanceEffect(existing, reverse = true)
            }
            val id = db.transactionDao().insert(tx)
            if (!tx.isDeleted) {
                applyBalanceEffect(tx.copy(id = id), reverse = false)
            }
            id
        }
        if (tx.type == TransactionType.EXPENSE && !tx.isDeleted) {
            evaluateBudgetThresholds(tx.ownerUserId, tx.profileId, tx.categoryId, tx.timestamp)
        }
        onDataMutated()
        return savedId
    }

    suspend fun softDeleteTransaction(txId: Long) {
        db.withTransaction {
            val tx = db.transactionDao().getById(txId) ?: return@withTransaction
            if (!tx.isDeleted) {
                applyBalanceEffect(tx, reverse = true)
                db.transactionDao().update(tx.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
            }
        }
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun restoreTransaction(txId: Long) {
        db.withTransaction {
            val tx = db.transactionDao().getById(txId) ?: return@withTransaction
            if (tx.isDeleted) {
                applyBalanceEffect(tx, reverse = false)
                db.transactionDao().update(tx.copy(isDeleted = false, deletedAt = null))
            }
        }
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun permanentDeleteTransaction(txId: Long) {
        db.withTransaction {
            val tx = db.transactionDao().getById(txId) ?: return@withTransaction
            if (!tx.isDeleted) {
                applyBalanceEffect(tx, reverse = true)
            }
            db.transactionDao().deleteById(txId)
        }
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun toggleFavorite(tx: TransactionEntity) {
        db.transactionDao().update(tx.copy(isFavorite = !tx.isFavorite))
        onDataMutated()
    }

    suspend fun duplicateTransaction(tx: TransactionEntity) {
        saveTransaction(
            tx.copy(
                id = 0L,
                timestamp = System.currentTimeMillis(),
                isDeleted = false,
                deletedAt = null
            )
        )
    }

    private suspend fun applyBalanceEffect(tx: TransactionEntity, reverse: Boolean) {
        val sign = if (reverse) -1.0 else 1.0
        when (tx.type) {
            TransactionType.INCOME -> db.accountDao().adjustBalance(tx.accountId, sign * tx.amount)
            TransactionType.EXPENSE -> db.accountDao().adjustBalance(tx.accountId, -sign * tx.amount)
            TransactionType.TRANSFER -> {
                db.accountDao().adjustBalance(tx.accountId, -sign * tx.amount)
                tx.targetAccountId?.let { targetId ->
                    db.accountDao().adjustBalance(targetId, sign * tx.amount)
                }
            }
        }
    }

    private suspend fun evaluateBudgetThresholds(userId: Long, profileId: Long, categoryId: Long, timestamp: Long) {
        val state = prefs.getSnapshot()
        if (!state.notificationsEnabled || !state.budgetWarningsEnabled) return

        val monthKey = DateTimeUtil.monthKey(timestamp)
        val budgets = db.budgetDao().getByProfileAndMonth(userId, profileId, monthKey)
        val budget = budgets.firstOrNull { it.categoryId == categoryId } ?: return
        if (budget.limitAmount <= 0.0) return

        val monthStart = DateTimeUtil.startOfMonth(timestamp)
        val monthEnd = DateTimeUtil.endOfDay(System.currentTimeMillis())
        val spent = db.transactionDao().getAllByProfileOnce(userId, profileId)
            .filter {
                !it.isDeleted &&
                    it.type == TransactionType.EXPENSE &&
                    it.categoryId == categoryId &&
                    it.timestamp in monthStart..monthEnd
            }
            .sumOf { it.amount }

        val pct = (spent / budget.limitAmount) * 100.0
        val catName = db.categoryDao().getByProfileOnce(userId, profileId).firstOrNull { it.id == categoryId }?.name ?: "Budget"
        val isBn = state.language == AppLanguage.BANGLA

        var updated = budget
        val formattedSpent = CurrencyUtil.format(spent, state.currencyCode)
        val formattedLimit = CurrencyUtil.format(budget.limitAmount, state.currencyCode)

        if (pct >= 100.0 && !budget.alert100Sent) {
            updated = updated.copy(alert100Sent = true, alert90Sent = true, alert80Sent = true)
            postAlert(
                userId,
                "BUDGET_WARN",
                if (isBn) "বাজেট ১০০% পূর্ণ: $catName" else "100% Budget Reached: $catName",
                if (isBn) "$catName খাতে $formattedSpent / $formattedLimit ব্যয় হয়েছে!"
                else "Spent $formattedSpent of $formattedLimit in $catName!",
                NotificationHelper.CHANNEL_BUDGET
            )
        } else if (pct >= 90.0 && !budget.alert90Sent) {
            updated = updated.copy(alert90Sent = true, alert80Sent = true)
            postAlert(
                userId,
                "BUDGET_WARN",
                if (isBn) "বাজেট ৯০% সতর্কতা: $catName" else "90% Budget Warning: $catName",
                if (isBn) "$catName বাজেটের ৯০% ব্যবহৃত ($formattedSpent / $formattedLimit)"
                else "90% used in $catName ($formattedSpent / $formattedLimit)",
                NotificationHelper.CHANNEL_BUDGET
            )
        } else if (pct >= 80.0 && !budget.alert80Sent) {
            updated = updated.copy(alert80Sent = true)
            postAlert(
                userId,
                "BUDGET_WARN",
                if (isBn) "বাজেট ৮০% সতর্কতা: $catName" else "80% Budget Alert: $catName",
                if (isBn) "$catName বাজেটের ৮০% পূর্ণ হয়েছে ($formattedSpent / $formattedLimit)"
                else "80% reached in $catName ($formattedSpent / $formattedLimit)",
                NotificationHelper.CHANNEL_BUDGET
            )
        }
        if (updated != budget) {
            db.budgetDao().update(updated)
        }
    }

    private suspend fun postAlert(
        userId: Long,
        type: String,
        title: String,
        message: String,
        channel: String
    ) {
        val id = db.notificationDao().insert(
            NotificationEntity(
                ownerUserId = userId,
                type = type,
                title = title,
                message = message
            )
        )
        NotificationHelper.postSystemNotification(context, channel, id.toInt(), title, message)
    }

    // --- Accounts, Categories, Budgets, Savings, Recurring, Extras ---
    suspend fun saveAccount(account: AccountEntity) {
        if (account.id == 0L) {
            db.accountDao().insert(account.copy(balance = account.initialBalance))
        } else {
            db.accountDao().update(account)
        }
        onDataMutated()
    }

    suspend fun deleteAccount(accountId: Long) {
        db.accountDao().deleteById(accountId)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveCategory(category: CategoryEntity) {
        db.categoryDao().insert(category)
        onDataMutated()
    }

    suspend fun deleteCategory(categoryId: Long) {
        db.categoryDao().deleteById(categoryId)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        db.budgetDao().insert(budget)
        onDataMutated()
    }

    suspend fun deleteBudget(budgetId: Long) {
        db.budgetDao().deleteById(budgetId)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveSavingsGoal(goal: SavingsGoalEntity) {
        db.savingsGoalDao().insert(goal)
        onDataMutated()
    }

    suspend fun addSavingsContribution(goal: SavingsGoalEntity, amount: Double, accountId: Long?) {
        db.withTransaction {
            val newSaved = goal.savedAmount + amount
            db.savingsGoalDao().update(goal.copy(savedAmount = newSaved))
            if (accountId != null) {
                val savingsCat = db.categoryDao().getByProfileOnce(goal.ownerUserId, goal.profileId)
                    .firstOrNull { it.localizationKey == "cat_savings" }
                val catId = savingsCat?.id ?: 0L
                db.transactionDao().insert(
                    TransactionEntity(
                        ownerUserId = goal.ownerUserId,
                        profileId = goal.profileId,
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        categoryId = catId,
                        accountId = accountId,
                        paymentMethod = "Savings",
                        note = "Deposit to ${goal.name}"
                    )
                )
                db.accountDao().adjustBalance(accountId, -amount)
            }
        }
        onDataMutated()
    }

    suspend fun deleteSavingsGoal(goalId: Long) {
        db.savingsGoalDao().deleteById(goalId)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveRecurringRule(rule: RecurringRuleEntity) {
        db.recurringRuleDao().insert(rule)
        processDueRecurringRules()
        onDataMutated()
    }

    suspend fun deleteRecurringRule(ruleId: Long) {
        db.recurringRuleDao().deleteById(ruleId)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun executeRecurringRuleNow(rule: RecurringRuleEntity) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val tx = TransactionEntity(
                ownerUserId = rule.ownerUserId,
                profileId = rule.profileId,
                type = rule.type,
                amount = rule.amount,
                categoryId = rule.categoryId,
                accountId = rule.accountId,
                paymentMethod = rule.paymentMethod,
                timestamp = now,
                note = "${rule.title} (Recurring)",
                recurringRuleId = rule.id
            )
            db.transactionDao().insert(tx)
            applyBalanceEffect(tx, reverse = false)
            val next = RecurringWorker.advanceTimestamp(maxOf(now, rule.nextRunAt), rule.frequency)
            db.recurringRuleDao().update(rule.copy(nextRunAt = next))
        }
        onDataMutated()
    }

    suspend fun processDueRecurringRules(): Int {
        val now = System.currentTimeMillis()
        val due = db.recurringRuleDao().getDueRules(now)
        if (due.isEmpty()) return 0
        var count = 0
        db.withTransaction {
            due.forEach { rule ->
                var next = rule.nextRunAt
                var loops = 0
                while (next <= now && loops < 12) {
                    val tx = TransactionEntity(
                        ownerUserId = rule.ownerUserId,
                        profileId = rule.profileId,
                        type = rule.type,
                        amount = rule.amount,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        paymentMethod = rule.paymentMethod,
                        timestamp = next,
                        note = "${rule.title} (Recurring)",
                        recurringRuleId = rule.id
                    )
                    db.transactionDao().insert(tx)
                    applyBalanceEffect(tx, reverse = false)
                    next = RecurringWorker.advanceTimestamp(next, rule.frequency)
                    count++
                    loops++
                }
                db.recurringRuleDao().update(rule.copy(nextRunAt = next))
            }
        }
        if (count > 0) onDataMutated()
        return count
    }

    suspend fun saveLoan(loan: LoanDebtEntity) {
        db.wealthExtrasDao().insertLoan(loan)
        onDataMutated()
    }

    suspend fun recordLoanPayment(loan: LoanDebtEntity, payAmount: Double) {
        val updated = loan.copy(
            paidAmount = (loan.paidAmount + payAmount).coerceAtMost(loan.principalAmount),
            paidMonths = (loan.paidMonths + 1).coerceAtMost(loan.totalMonths)
        )
        db.wealthExtrasDao().updateLoan(updated)
        onDataMutated()
    }

    suspend fun deleteLoan(id: Long) {
        db.wealthExtrasDao().deleteLoan(id)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveWishlist(item: WishlistEntity) {
        db.wealthExtrasDao().insertWishlist(item)
        onDataMutated()
    }

    suspend fun deleteWishlist(id: Long) {
        db.wealthExtrasDao().deleteWishlist(id)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveInvestment(inv: InvestmentEntity) {
        db.wealthExtrasDao().insertInvestment(inv)
        onDataMutated()
    }

    suspend fun deleteInvestment(id: Long) {
        db.wealthExtrasDao().deleteInvestment(id)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun saveNote(note: FinancialNoteEntity) {
        db.wealthExtrasDao().insertNote(note.copy(updatedAt = System.currentTimeMillis()))
        onDataMutated()
    }

    suspend fun deleteNote(id: Long) {
        db.wealthExtrasDao().deleteNote(id)
        onDataMutated(syncCloudImmediately = true)
    }

    suspend fun markAllNotificationsRead(userId: Long) {
        db.notificationDao().markAllRead(userId)
    }

    suspend fun verifyResetSecret(userId: Long, secret: String): Boolean {
        if (secret.isBlank()) return false
        val user = db.userDao().getById(userId)
        if (user != null && CryptoUtil.verifySecret(secret, user.salt, user.passwordHash)) {
            return true
        }
        val state = prefs.getSnapshot()
        if (state.pinEnabled && state.pinHash.isNotEmpty() && state.pinSalt.isNotEmpty()) {
            if (CryptoUtil.verifySecret(secret, state.pinSalt, state.pinHash)) {
                return true
            }
        }
        return false
    }

    suspend fun resetProfileFinanceData(userId: Long, profileId: Long): Boolean {
        db.withTransaction {
            db.transactionDao().deleteByProfile(userId, profileId)
            db.budgetDao().deleteByProfile(userId, profileId)
            db.savingsGoalDao().deleteByProfile(userId, profileId)
            db.recurringRuleDao().deleteByProfile(userId, profileId)
            db.wealthExtrasDao().deleteLoansByProfile(userId, profileId)
            db.wealthExtrasDao().deleteWishlistByProfile(userId, profileId)
            db.wealthExtrasDao().deleteInvestmentsByProfile(userId, profileId)
            db.accountDao().resetBalancesByProfile(userId, profileId)
        }
        seedDefaultsForProfile(userId, profileId)
        onDataMutated(syncCloudImmediately = true)
        return true
    }

    suspend fun resetUserFinanceData(userId: Long): Boolean {
        db.withTransaction {
            db.transactionDao().deleteByUser(userId)
            db.budgetDao().deleteByUser(userId)
            db.savingsGoalDao().deleteByUser(userId)
            db.recurringRuleDao().deleteByUser(userId)
            db.wealthExtrasDao().deleteLoansByUser(userId)
            db.wealthExtrasDao().deleteWishlistByUser(userId)
            db.wealthExtrasDao().deleteInvestmentsByUser(userId)
            db.wealthExtrasDao().deleteNotesByUser(userId)
            db.notificationDao().deleteByUser(userId)
            db.accountDao().resetBalancesByUser(userId)
        }
        seedDefaultsForUser(userId)
        onDataMutated(syncCloudImmediately = true)
        return true
    }

    // --- Backup, Restore & Dual Cloud Sync (Gmail Cloud Vault + Google Drive) ---
    suspend fun buildFullDatabaseSnapshot(scope: String = "FULL_DATABASE", ownerUserId: Long? = null): FullBackupSnapshot {
        val state = prefs.getSnapshot()
        val effectiveOwner = ownerUserId ?: state.currentUserId.takeIf { it > 0L }
        return FullBackupSnapshot(
            scope = scope,
            ownerUserId = effectiveOwner,
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
            appConfig = db.appConfigDao().getAllOnce(),
            playWallets = db.playWalletDao().getAllOnce(),
            playWalletRequests = db.playWalletRequestDao().getAllOnce(),
            playWalletLedger = db.playWalletLedgerDao().getAllOnce(),
            tournamentTierConfigs = db.tournamentTierConfigDao().getAllOnce(),
            gameBotConfigs = db.gameBotConfigDao().getAllOnce(),
            externalTournaments = db.externalTournamentDao().getAllOnce(),
            externalTournamentEntries = db.externalTournamentEntryDao().getAllOnce(),
            broadcastNotifications = db.broadcastNotificationDao().getAllOnce(),
            adminEarnings = db.adminEarningDao().getAllOnce()
        )
    }

    private fun compactSnapshotForFirestore(snapshot: FullBackupSnapshot, rawJson: String): String {
        if (rawJson.length <= 900_000) return rawJson
        val trimmedChats = snapshot.aiChatMessages.map { it.copy(imageBase64Thumb = null) }
        val compact = snapshot.copy(aiChatMessages = trimmedChats)
        val compactJson = ExportManager.snapshotToJson(compact)
        return if (compactJson.length <= 900_000) compactJson else compact.copy(aiChatMessages = trimmedChats.takeLast(50)).let {
            ExportManager.snapshotToJson(it)
        }
    }

    suspend fun exportFullBackupJsonString(): String {
        val snapshot = buildFullDatabaseSnapshot()
        val payload = ExportManager.snapshotToJson(snapshot)
        driveClient.saveLocalMirror(payload)
        return payload
    }

    suspend fun restoreFromRawJsonString(rawJson: String, sourceLabel: String = "Google Drive File"): Result<FullBackupSnapshot> {
        val parsed = ExportManager.jsonToSnapshot(rawJson)
        if (parsed.isSuccess) {
            val snap = parsed.getOrThrow()
            restoreFullDatabaseSnapshot(snap)
            driveClient.saveLocalMirror(rawJson)
            prefs.setDriveBackupStatus(System.currentTimeMillis(), "Restored ($sourceLabel)", pending = false)
        }
        return parsed
    }

    suspend fun restoreFullDatabaseSnapshot(snapshot: FullBackupSnapshot) {
        db.withTransaction {
            db.transactionDao().clearAll()
            db.budgetDao().clearAll()
            db.savingsGoalDao().clearAll()
            db.recurringRuleDao().clearAll()
            db.wealthExtrasDao().clearAllLoans()
            db.wealthExtrasDao().clearAllWishlist()
            db.wealthExtrasDao().clearAllInvestments()
            db.wealthExtrasDao().clearAllNotes()
            db.keepNoteDao().clearAll()
            db.noteVersionDao().clearAll()
            db.taskListDao().clearAll()
            db.taskItemDao().clearAll()
            db.itemReminderDao().clearAll()
            db.aiChatDao().clearAll()
            db.aiProviderDao().clearAll()
            db.quickTodoDao().clearAll()
            db.notificationDao().clearAll()
            db.accountDao().clearAll()
            db.categoryDao().clearAll()
            db.financeProfileDao().clearAll()
            db.playWalletDao().clearAll()
            db.playWalletRequestDao().clearAll()
            db.playWalletLedgerDao().clearAll()
            db.tournamentTierConfigDao().clearAll()
            db.gameBotConfigDao().clearAll()
            db.externalTournamentDao().clearAll()
            db.externalTournamentEntryDao().clearAll()
            db.broadcastNotificationDao().clearAll()
            db.adminEarningDao().clearAll()
            db.userDao().clearAll()

            snapshot.users.forEach { db.userDao().insertReplace(it) }
            if (snapshot.financeProfiles.isNotEmpty()) db.financeProfileDao().insertAll(snapshot.financeProfiles)
            if (snapshot.accounts.isNotEmpty()) db.accountDao().insertAll(snapshot.accounts)
            if (snapshot.categories.isNotEmpty()) db.categoryDao().insertAll(snapshot.categories)
            if (snapshot.transactions.isNotEmpty()) db.transactionDao().insertAll(snapshot.transactions)
            if (snapshot.budgets.isNotEmpty()) db.budgetDao().insertAll(snapshot.budgets)
            if (snapshot.savingsGoals.isNotEmpty()) db.savingsGoalDao().insertAll(snapshot.savingsGoals)
            if (snapshot.recurringRules.isNotEmpty()) db.recurringRuleDao().insertAll(snapshot.recurringRules)
            if (snapshot.loansDebts.isNotEmpty()) db.wealthExtrasDao().insertLoans(snapshot.loansDebts)
            if (snapshot.wishlist.isNotEmpty()) db.wealthExtrasDao().insertWishlists(snapshot.wishlist)
            if (snapshot.investments.isNotEmpty()) db.wealthExtrasDao().insertInvestments(snapshot.investments)
            if (snapshot.financialNotes.isNotEmpty()) db.wealthExtrasDao().insertNotes(snapshot.financialNotes)
            if (snapshot.keepNotes.isNotEmpty()) db.keepNoteDao().insertAll(snapshot.keepNotes)
            if (snapshot.noteVersions.isNotEmpty()) db.noteVersionDao().insertAll(snapshot.noteVersions)
            if (snapshot.taskLists.isNotEmpty()) db.taskListDao().insertAll(snapshot.taskLists)
            if (snapshot.taskItems.isNotEmpty()) db.taskItemDao().insertAll(snapshot.taskItems)
            if (snapshot.itemReminders.isNotEmpty()) db.itemReminderDao().insertAll(snapshot.itemReminders)
            if (snapshot.aiProviders.isNotEmpty()) db.aiProviderDao().insertAll(snapshot.aiProviders)
            if (snapshot.aiChatMessages.isNotEmpty()) db.aiChatDao().insertAll(snapshot.aiChatMessages)
            if (snapshot.quickTodos.isNotEmpty()) db.quickTodoDao().insertAll(snapshot.quickTodos)
            if (snapshot.notifications.isNotEmpty()) db.notificationDao().insertAll(snapshot.notifications)
            if (snapshot.appConfig.isNotEmpty()) db.appConfigDao().insertAll(snapshot.appConfig)
            if (snapshot.playWallets.isNotEmpty()) db.playWalletDao().insertAll(snapshot.playWallets)
            if (snapshot.playWalletRequests.isNotEmpty()) db.playWalletRequestDao().insertAll(snapshot.playWalletRequests)
            if (snapshot.playWalletLedger.isNotEmpty()) db.playWalletLedgerDao().insertAll(snapshot.playWalletLedger)
            if (snapshot.tournamentTierConfigs.isNotEmpty()) db.tournamentTierConfigDao().insertAll(snapshot.tournamentTierConfigs)
            if (snapshot.gameBotConfigs.isNotEmpty()) db.gameBotConfigDao().insertAll(snapshot.gameBotConfigs)
            if (snapshot.externalTournaments.isNotEmpty()) db.externalTournamentDao().insertAll(snapshot.externalTournaments)
            if (snapshot.externalTournamentEntries.isNotEmpty()) db.externalTournamentEntryDao().insertAll(snapshot.externalTournamentEntries)
            if (snapshot.broadcastNotifications.isNotEmpty()) db.broadcastNotificationDao().insertAll(snapshot.broadcastNotifications)
            if (snapshot.adminEarnings.isNotEmpty()) db.adminEarningDao().insertAll(snapshot.adminEarnings)
        }
        ensureInitialized()
        snapshot.preferences?.let { restoredPrefs ->
            prefs.restoreFromSnapshot(restoredPrefs)
            BackupScheduler.configureDailyAutoBackup(context, restoredPrefs.dailyAutoBackupEnabled)
        }
        val allUsers = db.userDao().getAllUsersOnce()
        val targetUser = snapshot.ownerUserId?.let { id -> allUsers.firstOrNull { it.id == id } }
            ?: allUsers.lastOrNull { it.username != "user" }
            ?: allUsers.firstOrNull()
        if (targetUser != null) {
            prefs.setSession(targetUser.id, remember = true, isAdmin = false)
            val profiles = db.financeProfileDao().getByUserOnce(targetUser.id)
            val restoredProfileId = snapshot.preferences?.activeProfileId?.takeIf { pid -> profiles.any { it.id == pid } }
                ?: profiles.firstOrNull()?.id
                ?: ensureDefaultProfileForUser(targetUser.id)
            prefs.setActiveProfileId(restoredProfileId)
        }
        BalanceSummaryWidget.refreshAll(context)
    }

    suspend fun backupNowToGoogleDrive(
        interactive: Boolean = true,
        activity: Activity? = null,
        forceDirectDriveOAuth: Boolean = false
    ): DriveSyncResult {
        val state = prefs.getSnapshot()
        val snapshot = buildFullDatabaseSnapshot()
        val payload = ExportManager.snapshotToJson(snapshot)
        driveClient.saveLocalMirror(payload)
        val firestoreSafePayload = compactSnapshotForFirestore(snapshot, payload)

        // 1. Primary Zero-403 Gmail Cloud Vault Sync
        val vaultRes = cloudVault.uploadSnapshotToVault(
            payloadJson = firestoreSafePayload,
            snapshot = snapshot,
            fallbackEmail = state.connectedGmail
        )

        if (!forceDirectDriveOAuth && vaultRes.isSuccess) {
            val record = vaultRes.getOrThrow()
            val silentDrive = runCatching {
                driveClient.uploadBackup(
                    jsonContent = payload,
                    interactive = false,
                    activity = null,
                    knownEmail = record.email
                )
            }.getOrNull()
            val channelLabel = if (silentDrive is DriveSyncResult.Success) {
                "Synced: Cloud Vault + Google Drive"
            } else {
                "Synced: Gmail Cloud Vault"
            }
            val resolvedEmail = record.email.ifBlank { state.connectedGmail }
            prefs.setDriveConnection(resolvedEmail, authorized = true)
            prefs.setDriveBackupStatus(record.updatedAtMillis, channelLabel, pending = false)
            return DriveSyncResult.Success(
                email = resolvedEmail,
                timestamp = record.updatedAtMillis,
                fileId = "cloud_vault:${record.userId}",
                channelLabel = channelLabel
            )
        }

        // 2. Direct Google Drive API Sync
        val result = driveClient.uploadBackup(
            jsonContent = payload,
            interactive = interactive,
            activity = activity,
            knownEmail = state.connectedGmail
        )
        if (result is DriveSyncResult.Success) {
            val resolvedEmail = result.email ?: state.connectedGmail
            prefs.setDriveConnection(resolvedEmail, authorized = true)
            prefs.setDriveBackupStatus(result.timestamp, "Synced: Google Drive + Cloud Vault", pending = false)
            return result
        }

        if (vaultRes.isSuccess && result is DriveSyncResult.Error) {
            val record = vaultRes.getOrThrow()
            val resolvedEmail = record.email.ifBlank { state.connectedGmail }
            prefs.setDriveConnection(resolvedEmail, authorized = true)
            prefs.setDriveBackupStatus(record.updatedAtMillis, "Synced: Gmail Cloud Vault", pending = false)
            return DriveSyncResult.Success(
                email = resolvedEmail,
                timestamp = record.updatedAtMillis,
                fileId = "cloud_vault:${record.userId}",
                channelLabel = "Gmail Cloud Vault"
            )
        }
        return result
    }

    suspend fun inspectGoogleDriveBackup(
        activity: Activity? = null,
        forceDirectDriveOAuth: Boolean = false
    ): DriveBackupPreview? {
        val state = prefs.getSnapshot()
        if (!forceDirectDriveOAuth) {
            val vaultRecord = cloudVault.downloadLatestVaultRecord().getOrNull()
            if (vaultRecord != null) {
                val snap = ExportManager.jsonToSnapshot(vaultRecord.payloadJson).getOrNull()
                if (snap != null) {
                    return DriveBackupPreview(
                        email = vaultRecord.email.ifBlank { state.connectedGmail.ifBlank { null } },
                        fileId = "cloud_vault:${vaultRecord.userId}",
                        modifiedTime = DateTimeUtil.formatDateTime(vaultRecord.updatedAtMillis),
                        jsonPayload = vaultRecord.payloadJson,
                        snapshot = snap
                    )
                }
            }
        }

        val res = driveClient.restoreLatestBackup(activity = activity, knownEmail = state.connectedGmail)
        if (res is DriveRestoreResult.Success) {
            val snap = ExportManager.jsonToSnapshot(res.jsonPayload).getOrNull() ?: return null
            return DriveBackupPreview(
                email = res.email ?: state.connectedGmail.ifBlank { null },
                fileId = res.fileId,
                modifiedTime = res.modifiedTime,
                jsonPayload = res.jsonPayload,
                snapshot = snap
            )
        }
        return null
    }

    suspend fun restoreFromGoogleDrive(
        activity: Activity? = null,
        forceDirectDriveOAuth: Boolean = false
    ): DriveRestoreResult {
        val state = prefs.getSnapshot()
        if (!forceDirectDriveOAuth) {
            val vaultRes = cloudVault.downloadLatestVaultRecord()
            val vaultRecord = vaultRes.getOrNull()
            if (vaultRecord != null) {
                val parsed = ExportManager.jsonToSnapshot(vaultRecord.payloadJson)
                if (parsed.isSuccess) {
                    val resolvedEmail = vaultRecord.email.ifBlank { state.connectedGmail }
                    restoreFullDatabaseSnapshot(parsed.getOrThrow())
                    if (resolvedEmail.isNotBlank()) {
                        prefs.setDriveConnection(resolvedEmail, authorized = true)
                    }
                    prefs.setDriveBackupStatus(
                        System.currentTimeMillis(),
                        "Restored from Gmail Cloud Vault",
                        pending = false
                    )
                    return DriveRestoreResult.Success(
                        jsonPayload = vaultRecord.payloadJson,
                        email = resolvedEmail,
                        modifiedTime = DateTimeUtil.formatDateTime(vaultRecord.updatedAtMillis),
                        fileId = "cloud_vault:${vaultRecord.userId}",
                        channelLabel = "Gmail Cloud Vault"
                    )
                }
            }
        }

        val res = driveClient.restoreLatestBackup(activity = activity, knownEmail = state.connectedGmail)
        if (res is DriveRestoreResult.Success) {
            val resolvedEmail = res.email ?: state.connectedGmail
            prefs.setDriveConnection(resolvedEmail, authorized = true)
            val parsed = ExportManager.jsonToSnapshot(res.jsonPayload)
            if (parsed.isSuccess) {
                restoreFullDatabaseSnapshot(parsed.getOrThrow())
                if (resolvedEmail.isNotBlank()) {
                    prefs.setDriveConnection(resolvedEmail, authorized = true)
                }
                prefs.setDriveBackupStatus(System.currentTimeMillis(), "Restored from Google Drive", pending = false)
            } else {
                return DriveRestoreResult.Error("Backup file corrupted")
            }
        }
        return res
    }

    suspend fun syncPendingDriveBackupQuietly(): Boolean {
        val state = prefs.getSnapshot()
        if (state.connectedGmail.isBlank() && !state.driveAuthorized && cloudVault.currentFirebaseUid() == null) {
            return false
        }
        val res = backupNowToGoogleDrive(interactive = false, activity = null, forceDirectDriveOAuth = false)
        return res is DriveSyncResult.Success
    }

    private suspend fun onDataMutated(syncCloudImmediately: Boolean = false): Boolean {
        BalanceSummaryWidget.refreshAll(context)
        return triggerAutoBackupIfEnabled(syncNow = syncCloudImmediately)
    }

    private suspend fun triggerAutoBackupIfEnabled(syncNow: Boolean = false): Boolean {
        val state = prefs.getSnapshot()
        runCatching {
            val payload = ExportManager.snapshotToJson(buildFullDatabaseSnapshot())
            driveClient.saveLocalMirror(payload)
        }
        val hasCloud = state.connectedGmail.isNotBlank() || state.driveAuthorized || cloudVault.currentFirebaseUid() != null
        if (!hasCloud) return false
        if (!syncNow && !state.autoBackupOnNewData) return false

        prefs.setPendingCloudSync(true)
        if (syncNow) {
            val syncedNow = runCatching { syncPendingDriveBackupQuietly() }.getOrDefault(false)
            if (syncedNow) return true
        }
        BackupScheduler.enqueueOnChangeAutoBackup(context)
        return false
    }

    // --- Admin Panel Operations ---
    fun adminObserveAllUsers(): Flow<List<UserEntity>> = db.userDao().observeAllUsers()
    fun adminObserveAllTransactions(): Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    fun adminObserveAllCategories(): Flow<List<CategoryEntity>> = db.categoryDao().observeAll()
    fun adminObserveAppConfig(): Flow<List<AppConfigEntity>> = db.appConfigDao().observeAll()

    suspend fun adminToggleUserLock(user: UserEntity) {
        db.userDao().update(user.copy(isLocked = !user.isLocked))
        triggerAutoBackupIfEnabled()
    }

    suspend fun adminResetUserPassword(user: UserEntity, newPassword: String) {
        val salt = CryptoUtil.generateSalt()
        db.userDao().update(user.copy(salt = salt, passwordHash = CryptoUtil.hashSecret(newPassword, salt)))
        triggerAutoBackupIfEnabled()
    }

    suspend fun adminDeleteUser(userId: Long) {
        db.withTransaction {
            db.transactionDao().deleteByUser(userId)
            db.budgetDao().deleteByUser(userId)
            db.savingsGoalDao().deleteByUser(userId)
            db.recurringRuleDao().deleteByUser(userId)
            db.wealthExtrasDao().deleteLoansByUser(userId)
            db.wealthExtrasDao().deleteWishlistByUser(userId)
            db.wealthExtrasDao().deleteInvestmentsByUser(userId)
            db.wealthExtrasDao().deleteNotesByUser(userId)
            db.keepNoteDao().deleteByUser(userId)
            db.noteVersionDao().deleteByUser(userId)
            db.taskListDao().deleteByUser(userId)
            db.taskItemDao().deleteByUser(userId)
            db.itemReminderDao().deleteByUser(userId)
            db.aiChatDao().deleteByUser(userId)
            db.aiProviderDao().deleteByUser(userId)
            db.quickTodoDao().deleteByUser(userId)
            db.notificationDao().deleteByUser(userId)
            db.accountDao().deleteByUser(userId)
            db.categoryDao().deleteByUser(userId)
            db.financeProfileDao().deleteByUser(userId)
            db.playWalletDao().deleteByUser(userId)
            db.playWalletRequestDao().deleteByUser(userId)
            db.playWalletLedgerDao().deleteByUser(userId)
            db.userDao().deleteById(userId)
        }
        triggerAutoBackupIfEnabled()
    }

    suspend fun adminClearAllTrash() {
        db.transactionDao().clearAllTrash()
        triggerAutoBackupIfEnabled()
    }

    suspend fun adminSetConfig(key: String, value: String) {
        db.appConfigDao().set(AppConfigEntity(key, value))
    }

    suspend fun getAdminDriveEmail(): String =
        db.appConfigDao().getValue("admin_master_drive_email").orEmpty()
            .ifBlank { prefs.getSnapshot().connectedGmail }

    suspend fun isAdminAutoDriveSyncEnabled(): Boolean =
        db.appConfigDao().getValue("admin_master_auto_drive_sync")?.lowercase() != "false"

    suspend fun setAdminAutoDriveSyncEnabled(enabled: Boolean) {
        db.appConfigDao().set(AppConfigEntity("admin_master_auto_drive_sync", enabled.toString()))
        prefs.setAutoBackupOnNewData(enabled)
    }

    suspend fun recordAdminDriveSyncStatus(email: String, status: String) {
        if (email.isNotBlank()) {
            db.appConfigDao().set(AppConfigEntity("admin_master_drive_email", email.trim()))
        }
        db.appConfigDao().set(AppConfigEntity("admin_master_drive_last_status", status))
    }

    /**
     * Automatically saves/rewrites the FULL all-users database (users, wallets, tournaments,
     * ledger, earnings, finance) to the Admin's connected Google Drive & Cloud Vault whenever
     * critical records change so no user data is ever lost.
     */
    suspend fun triggerAdminMasterDriveAutoSync(
        interactive: Boolean = false,
        activity: Activity? = null
    ): DriveSyncResult? {
        val enabled = isAdminAutoDriveSyncEnabled()
        if (!enabled && !interactive) return null
        val adminEmail = getAdminDriveEmail()
        if (adminEmail.isNotBlank()) {
            val state = prefs.getSnapshot()
            if (state.connectedGmail.isBlank()) {
                prefs.setDriveConnection(adminEmail, authorized = true)
            }
        }
        val res = runCatching {
            backupNowToGoogleDrive(
                interactive = interactive,
                activity = activity,
                forceDirectDriveOAuth = false
            )
        }.getOrNull()
        if (res is DriveSyncResult.Success) {
            val resolvedEmail = res.email?.takeIf { it.isNotBlank() } ?: adminEmail
            recordAdminDriveSyncStatus(
                email = resolvedEmail,
                status = "Auto-saved at ${DateTimeUtil.formatDateTime(res.timestamp)} (${res.channelLabel})"
            )
        }
        return res
    }

    suspend fun adminGetAllAccountsOnce(): List<AccountEntity> = db.accountDao().getAllOnce()
    suspend fun adminGetAllCategoriesOnce(): List<CategoryEntity> = db.categoryDao().getAllOnce()
    suspend fun adminGetAllTransactionsOnce(): List<TransactionEntity> = db.transactionDao().getAllOnce()
    suspend fun adminGetAllUsersOnce(): List<UserEntity> = db.userDao().getAllUsersOnce()

    // --- Local Game Arena Leaderboard (Room) ---
    fun observeGameLeaderboard(): Flow<List<GameLeaderboardEntity>> =
        db.gameLeaderboardDao().observeAll()

    suspend fun getGameLeaderboardOnce(): List<GameLeaderboardEntity> =
        db.gameLeaderboardDao().getAllOnce()

    suspend fun recordGameResult(
        gameId: String,
        outcome: GameMatchResult,
        score: Int,
        vsBot: Boolean,
        botAccuracy: Int
    ): GameLeaderboardEntity {
        val safeScore = score.coerceAtLeast(0)
        val existing = db.gameLeaderboardDao().getByGameId(gameId)
            ?: GameLeaderboardEntity(gameId = gameId)
        val newCurrentStreak = if (outcome == GameMatchResult.WIN) existing.currentWinStreak + 1 else 0
        val updated = existing.copy(
            gamesPlayed = existing.gamesPlayed + 1,
            wins = existing.wins + if (outcome == GameMatchResult.WIN) 1 else 0,
            losses = existing.losses + if (outcome == GameMatchResult.LOSS) 1 else 0,
            draws = existing.draws + if (outcome == GameMatchResult.DRAW) 1 else 0,
            highScore = maxOf(existing.highScore, safeScore),
            totalScore = existing.totalScore + safeScore,
            currentWinStreak = newCurrentStreak,
            bestWinStreak = maxOf(existing.bestWinStreak, newCurrentStreak),
            highestBotAccuracyBeaten = if (outcome == GameMatchResult.WIN && vsBot) {
                maxOf(existing.highestBotAccuracyBeaten, botAccuracy.coerceIn(10, 100))
            } else {
                existing.highestBotAccuracyBeaten
            },
            lastPlayedAt = System.currentTimeMillis()
        )
        db.gameLeaderboardDao().upsert(updated)
        return updated
    }

    suspend fun resetGameLeaderboard() {
        db.gameLeaderboardDao().clearAll()
    }
}
