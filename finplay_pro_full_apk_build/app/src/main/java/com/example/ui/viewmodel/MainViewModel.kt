package com.example.ui.viewmodel

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiChatClient
import com.example.data.ai.AiFailoverOrchestrator
import com.example.data.ai.NoteAiAssistantEngine
import com.example.data.ai.NoteAiToolAction
import com.example.data.backup.BackupScheduler
import com.example.data.backup.DriveAuthState
import com.example.data.backup.DriveBackupPreview
import com.example.data.backup.DriveRestoreResult
import com.example.data.backup.DriveSyncResult
import com.example.data.backup.ExportManager
import com.example.data.backup.GoogleAccountSignInOutcome
import com.example.data.local.AccountEntity
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.allModelsList
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
import com.example.data.local.QuickTodoEntity
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskListEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferencesState
import com.example.data.local.WishlistEntity
import com.example.data.repository.FinanceRepository
import com.example.domain.AnalyticsEngine
import com.example.domain.DashboardMetrics
import com.example.domain.DeepAnalyticsSummary
import com.example.domain.ObligationKind
import com.example.domain.ReportPeriod
import com.example.domain.ReportSummary
import com.example.domain.UpcomingObligation
import com.example.i18n.AppLanguage
import com.example.i18n.LanguageManager
import com.example.security.CryptoUtil
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val appContext: Context,
    val repository: FinanceRepository
) : ViewModel() {

    val prefsState: StateFlow<UserPreferencesState> = repository.prefsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesState())

    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _driveBusy = MutableStateFlow(false)
    val driveBusy: StateFlow<Boolean> = _driveBusy.asStateFlow()

    private val _pendingDriveConsent = MutableStateFlow<PendingIntent?>(null)
    val pendingDriveConsent: StateFlow<PendingIntent?> = _pendingDriveConsent.asStateFlow()

    private val _pendingDriveAction = MutableStateFlow<String?>(null) // "CONNECT", "BACKUP", "RESTORE", "RESTORE_PREVIEW"
    private val _pendingSignedInEmail = MutableStateFlow<String?>(null)

    private val _driveBackupPreview = MutableStateFlow<DriveBackupPreview?>(null)
    val driveBackupPreview: StateFlow<DriveBackupPreview?> = _driveBackupPreview.asStateFlow()

    init {
        viewModelScope.launch {
            prefsState
                .map { Triple(it.pendingCloudSync, it.autoBackupOnNewData, it.connectedGmail.isNotBlank() || it.driveAuthorized) }
                .distinctUntilChanged()
                .collectLatest { (pending, autoEnabled, isConnected) ->
                    if (pending && autoEnabled && isConnected) {
                        delay(2500)
                        repository.syncPendingDriveBackupQuietly()
                    }
                }
        }
    }

    private val activeUserIdFlow = prefsState.map { it.currentUserId }.distinctUntilChanged()
    private val activeProfileIdFlow = prefsState.map { it.activeProfileId }.distinctUntilChanged()
    private val activeScopeFlow = combine(activeUserIdFlow, activeProfileIdFlow) { uid, pid -> uid to pid }.distinctUntilChanged()

    val currentUser: StateFlow<UserEntity?> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeUser(uid) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<FinanceProfileEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeProfiles(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<FinanceProfileEntity?> = combine(profiles, activeProfileIdFlow) { list, pid ->
        list.firstOrNull { it.id == pid } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUserAccounts: StateFlow<List<AccountEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeAllAccountsByUser(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserTransactions: StateFlow<List<TransactionEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeAllTransactionsByUser(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeAccounts(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeCategories(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeTransactions(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashTransactions: StateFlow<List<TransactionEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeTrash(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeBudgets(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeSavingsGoals(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringRuleEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeRecurringRules(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanDebtEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeLoans(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlist: StateFlow<List<WishlistEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeWishlist(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investments: StateFlow<List<InvestmentEntity>> = activeScopeFlow.flatMapLatest { (uid, pid) ->
        if (uid > 0L) repository.observeInvestments(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<FinancialNoteEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeNotes(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeNotifications(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Shared Keep Notes, Good Tasks, AI Assistant & Quick To-Do ---
    val keepNotes: StateFlow<List<KeepNoteEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeKeepNotes(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedKeepNotes: StateFlow<List<KeepNoteEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeArchivedKeepNotes(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedKeepNotes: StateFlow<List<KeepNoteEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeTrashedKeepNotes(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskLists: StateFlow<List<TaskListEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeTaskLists(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskItems: StateFlow<List<TaskItemEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeTaskItems(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<ItemReminderEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeActiveReminders(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<ItemReminderEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeAllReminders(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickTodos: StateFlow<List<QuickTodoEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeQuickTodos(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiProviders: StateFlow<List<AiProviderEntity>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid > 0L) repository.observeAiProviders(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAiProviderId = MutableStateFlow<Long?>(null)
    val selectedAiProviderId: StateFlow<Long?> = _selectedAiProviderId.asStateFlow()

    val activeAiProvider: StateFlow<AiProviderEntity?> = combine(aiProviders, _selectedAiProviderId) { list, selId ->
        list.firstOrNull { it.id == selId } ?: list.firstOrNull { it.isDefault } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiChatMessages: StateFlow<List<AiChatMessageEntity>> = combine(activeUserIdFlow, activeAiProvider) { uid, prov ->
        uid to prov?.id
    }.distinctUntilChanged().flatMapLatest { (uid, pid) ->
        if (uid > 0L && pid != null) repository.observeAiChatMessages(uid, pid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiChatBusy = MutableStateFlow(false)
    val aiChatBusy: StateFlow<Boolean> = _aiChatBusy.asStateFlow()

    private val _aiChatError = MutableStateFlow<String?>(null)
    val aiChatError: StateFlow<String?> = _aiChatError.asStateFlow()

    val gameLeaderboard: StateFlow<List<GameLeaderboardEntity>> = repository.observeGameLeaderboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        accounts,
        categories,
        transactions,
        budgets,
        savingsGoals,
        recurringRules,
        loans
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val accs = values[0] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val cats = values[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val txs = values[2] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val bgs = values[3] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = values[4] as List<SavingsGoalEntity>
        @Suppress("UNCHECKED_CAST")
        val rules = values[5] as List<RecurringRuleEntity>
        @Suppress("UNCHECKED_CAST")
        val lns = values[6] as List<LoanDebtEntity>
        AnalyticsEngine.computeDashboard(
            accounts = accs,
            categories = cats,
            transactions = txs,
            budgets = bgs,
            savingsGoals = goals,
            recurringRules = rules,
            loans = lns
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsEngine.computeDashboard(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )

    val deepAnalytics: StateFlow<DeepAnalyticsSummary> = combine(
        transactions,
        categories,
        budgets,
        savingsGoals,
        loans,
        prefsState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txs = values[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val cats = values[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val bgs = values[2] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = values[3] as List<SavingsGoalEntity>
        @Suppress("UNCHECKED_CAST")
        val ln = values[4] as List<LoanDebtEntity>
        val prefs = values[5] as UserPreferencesState
        AnalyticsEngine.computeDeepAnalytics(
            txs, cats, bgs, goals, ln, prefs.currencyCode, prefs.language
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsEngine.computeDeepAnalytics(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), "BDT", AppLanguage.BANGLA
        )
    )

    fun computeReport(period: ReportPeriod, customStart: Long?, customEnd: Long?): ReportSummary {
        return AnalyticsEngine.computeReport(
            period = period,
            transactions = transactions.value,
            categories = categories.value,
            customStart = customStart,
            customEnd = customEnd
        )
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(message: String) {
        _uiMessage.value = message
    }

    fun clearPendingDriveConsent() {
        _pendingDriveConsent.value = null
    }

    fun login(username: String, password: String, remember: Boolean, invalidMsg: String, lockedMsg: String) {
        if (_authBusy.value) return
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            try {
                repository.authenticateUserOrSecretAdmin(username, password, remember)
                    .onSuccess {
                        _sessionUnlocked.value = true
                    }
                    .onFailure { err ->
                        _authError.value = if (err.message == "LOCKED") lockedMsg else invalidMsg
                    }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: invalidMsg
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun register(
        username: String,
        password: String,
        displayName: String,
        securityQuestion: String,
        securityAnswer: String,
        existsMsg: String
    ) {
        if (_authBusy.value) return
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            try {
                repository.registerUser(username, password, displayName, securityQuestion, securityAnswer)
                    .onSuccess {
                        _sessionUnlocked.value = true
                    }
                    .onFailure {
                        _authError.value = existsMsg
                    }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: existsMsg
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun resetForgottenPassword(
        username: String,
        answer: String,
        newPass: String,
        successMsg: String,
        wrongAnsMsg: String,
        notFoundMsg: String
    ) {
        viewModelScope.launch {
            repository.resetForgottenPassword(username, answer, newPass)
                .onSuccess { _uiMessage.value = successMsg }
                .onFailure { err ->
                    _authError.value = if (err.message == "WRONG_ANSWER") wrongAnsMsg else notFoundMsg
                }
        }
    }

    fun loginAdmin(key: String, invalidMsg: String) {
        if (_authBusy.value) return
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            try {
                when (val result = repository.authenticateAdminDetailed(key)) {
                    is FinanceRepository.AdminAuthResult.Success -> {
                        _sessionUnlocked.value = true
                    }
                    is FinanceRepository.AdminAuthResult.LockedOut -> {
                        _authError.value = "Too many failed attempts. Try again in ${result.remainingSeconds}s."
                    }
                    is FinanceRepository.AdminAuthResult.InvalidKey -> {
                        _authError.value = invalidMsg
                    }
                }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: invalidMsg
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _sessionUnlocked.value = false
        }
    }

    fun unlockWithBiometric() {
        _sessionUnlocked.value = true
    }

    fun verifyPin(pin: String, wrongMsg: String): Boolean {
        val state = prefsState.value
        val ok = CryptoUtil.verifySecret(pin, state.pinSalt, state.pinHash)
        if (ok) {
            _sessionUnlocked.value = true
        } else {
            _authError.value = wrongMsg
        }
        return ok
    }

    fun checkPinMatch(pin: String): Boolean {
        val state = prefsState.value
        return CryptoUtil.verifySecret(pin, state.pinSalt, state.pinHash)
    }

    fun setPin(enabled: Boolean, newPin: String) {
        viewModelScope.launch {
            if (enabled && newPin.length in 4..6) {
                val salt = CryptoUtil.generateSalt()
                repository.prefs.setPin(
                    enabled = true,
                    hash = CryptoUtil.hashSecret(newPin, salt),
                    salt = salt,
                    pinLength = newPin.length
                )
            } else {
                repository.prefs.setPin(false)
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.prefs.setBiometricEnabled(enabled) }
    }

    fun setLanguage(lang: AppLanguage) {
        LanguageManager.setLanguage(lang)
        viewModelScope.launch { repository.prefs.setLanguage(lang) }
    }

    fun toggleLanguage() {
        val next = LanguageManager.toggleLanguage()
        viewModelScope.launch { repository.prefs.setLanguage(next) }
    }

    fun recordGameMatch(
        gameId: String,
        outcome: GameMatchResult,
        score: Int,
        vsBot: Boolean,
        botAccuracy: Int
    ) {
        viewModelScope.launch {
            repository.recordGameResult(
                gameId = gameId,
                outcome = outcome,
                score = score,
                vsBot = vsBot,
                botAccuracy = botAccuracy
            )
        }
    }

    fun resetGameLeaderboard() {
        viewModelScope.launch { repository.resetGameLeaderboard() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.prefs.setThemeMode(mode) }
    }

    fun setCurrencyCode(code: String) {
        viewModelScope.launch { repository.prefs.setCurrencyCode(code) }
    }

    fun changePassword(current: String, newPass: String, successMsg: String, invalidMsg: String) {
        val uid = prefsState.value.currentUserId
        if (uid <= 0L) return
        viewModelScope.launch {
            repository.changePassword(uid, current, newPass)
                .onSuccess { _uiMessage.value = successMsg }
                .onFailure { _uiMessage.value = invalidMsg }
        }
    }

    // --- Ledger Mutations ---
    fun saveTransaction(tx: TransactionEntity) {
        viewModelScope.launch { repository.saveTransaction(tx) }
    }

    fun softDeleteTransaction(txId: Long) {
        viewModelScope.launch { repository.softDeleteTransaction(txId) }
    }

    fun restoreTransaction(txId: Long) {
        viewModelScope.launch { repository.restoreTransaction(txId) }
    }

    fun permanentDeleteTransaction(txId: Long) {
        viewModelScope.launch { repository.permanentDeleteTransaction(txId) }
    }

    fun toggleFavorite(tx: TransactionEntity) {
        viewModelScope.launch { repository.toggleFavorite(tx) }
    }

    fun duplicateTransaction(tx: TransactionEntity) {
        viewModelScope.launch { repository.duplicateTransaction(tx) }
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch { repository.saveAccount(account) }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch { repository.deleteAccount(id) }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch { repository.saveCategory(category) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    fun saveBudget(budget: BudgetEntity) {
        viewModelScope.launch { repository.saveBudget(budget) }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch { repository.deleteBudget(id) }
    }

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch { repository.saveSavingsGoal(goal) }
    }

    fun depositSavings(goal: SavingsGoalEntity, amount: Double, accountId: Long?) {
        viewModelScope.launch { repository.addSavingsContribution(goal, amount, accountId) }
    }

    fun deleteSavingsGoal(id: Long) {
        viewModelScope.launch { repository.deleteSavingsGoal(id) }
    }

    fun saveRecurringRule(rule: RecurringRuleEntity) {
        viewModelScope.launch { repository.saveRecurringRule(rule) }
    }

    fun deleteRecurringRule(id: Long) {
        viewModelScope.launch { repository.deleteRecurringRule(id) }
    }

    fun processDueRecurringNow() {
        viewModelScope.launch {
            val count = repository.processDueRecurringRules()
            if (count > 0) _uiMessage.value = "Processed $count recurring entries"
        }
    }

    fun payUpcomingObligation(obligation: UpcomingObligation, doneMsg: String) {
        viewModelScope.launch {
            when (obligation.kind) {
                ObligationKind.RECURRING_RULE -> {
                    val rule = recurringRules.value.firstOrNull { it.id == obligation.id } ?: return@launch
                    repository.executeRecurringRuleNow(rule)
                }
                ObligationKind.LOAN_EMI -> {
                    val loan = loans.value.firstOrNull { it.id == obligation.id } ?: return@launch
                    repository.recordLoanPayment(loan, obligation.amount)
                }
                ObligationKind.SAVINGS_GOAL -> {
                    val goal = savingsGoals.value.firstOrNull { it.id == obligation.id } ?: return@launch
                    val defaultAccId = goal.linkedAccountId ?: accounts.value.firstOrNull()?.id
                    val deposit = obligation.amount.coerceAtLeast(100.0)
                    repository.addSavingsContribution(goal, deposit, defaultAccId)
                }
            }
            _uiMessage.value = doneMsg
        }
    }

    fun saveLoan(loan: LoanDebtEntity) {
        viewModelScope.launch { repository.saveLoan(loan) }
    }

    fun payLoan(loan: LoanDebtEntity, amount: Double) {
        viewModelScope.launch { repository.recordLoanPayment(loan, amount) }
    }

    fun deleteLoan(id: Long) {
        viewModelScope.launch { repository.deleteLoan(id) }
    }

    fun saveWishlist(item: WishlistEntity) {
        viewModelScope.launch { repository.saveWishlist(item) }
    }

    fun deleteWishlist(id: Long) {
        viewModelScope.launch { repository.deleteWishlist(id) }
    }

    fun saveInvestment(inv: InvestmentEntity) {
        viewModelScope.launch { repository.saveInvestment(inv) }
    }

    fun deleteInvestment(id: Long) {
        viewModelScope.launch { repository.deleteInvestment(id) }
    }

    fun saveNote(note: FinancialNoteEntity) {
        viewModelScope.launch { repository.saveNote(note) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    // --- Multi-Profile Finance Actions ---
    fun switchProfile(profileId: Long) {
        viewModelScope.launch { repository.switchProfile(profileId) }
    }

    fun createProfile(name: String, iconKey: String, colorHex: Long, description: String, createdMsg: String) {
        val uid = prefsState.value.currentUserId
        if (uid <= 0L || name.isBlank()) return
        viewModelScope.launch {
            repository.createFinanceProfile(uid, name, iconKey, colorHex, description)
            _uiMessage.value = "$createdMsg: ${name.trim()}"
        }
    }

    fun updateProfile(profile: FinanceProfileEntity) {
        viewModelScope.launch { repository.updateFinanceProfile(profile) }
    }

    fun deleteProfile(profileId: Long) {
        val uid = prefsState.value.currentUserId
        if (uid <= 0L) return
        viewModelScope.launch { repository.deleteFinanceProfile(uid, profileId) }
    }

    // --- Keep Notes, Versions, Good Tasks, Lists & Reminders Actions ---
    fun observeNoteVersions(noteId: Long) = repository.observeNoteVersions(noteId)

    fun saveKeepNote(
        note: KeepNoteEntity,
        saveVersionSnapshot: Boolean = true,
        pendingReminders: List<ItemReminderEntity> = emptyList()
    ) {
        viewModelScope.launch {
            val savedId = repository.saveKeepNote(note, saveVersionSnapshot)
            pendingReminders.forEach { rem ->
                repository.saveItemReminder(rem.copy(targetId = savedId))
            }
        }
    }

    fun restoreNoteVersion(noteId: Long, version: NoteVersionEntity) {
        viewModelScope.launch { repository.restoreNoteVersion(noteId, version) }
    }

    fun bulkUpdateNotes(notes: List<KeepNoteEntity>) {
        viewModelScope.launch { repository.bulkUpdateNotes(notes) }
    }

    fun deleteKeepNotePermanently(noteId: Long) {
        viewModelScope.launch { repository.deleteKeepNotePermanently(noteId) }
    }

    fun saveTaskList(list: TaskListEntity) {
        viewModelScope.launch { repository.saveTaskList(list) }
    }

    fun deleteTaskList(listId: Long) {
        viewModelScope.launch { repository.deleteTaskList(listId) }
    }

    fun saveTaskItem(
        task: TaskItemEntity,
        pendingReminders: List<ItemReminderEntity> = emptyList()
    ) {
        viewModelScope.launch {
            val savedId = repository.saveTaskItem(task)
            pendingReminders.forEach { rem ->
                repository.saveItemReminder(rem.copy(targetId = savedId))
            }
        }
    }

    fun toggleTaskCompleted(task: TaskItemEntity) {
        viewModelScope.launch { repository.toggleTaskCompleted(task) }
    }

    fun reorderTasks(tasks: List<TaskItemEntity>) {
        viewModelScope.launch { repository.reorderTasks(tasks) }
    }

    fun deleteTaskItem(taskId: Long) {
        viewModelScope.launch { repository.deleteTaskItem(taskId) }
    }

    fun saveItemReminder(reminder: ItemReminderEntity) {
        viewModelScope.launch { repository.saveItemReminder(reminder) }
    }

    fun snoozeReminder(reminderId: Long, minutes: Int) {
        viewModelScope.launch { repository.snoozeReminder(reminderId, minutes) }
    }

    fun rescheduleReminder1Day(reminderId: Long) {
        viewModelScope.launch { repository.rescheduleReminder1Day(reminderId) }
    }

    fun dismissReminder(reminderId: Long) {
        viewModelScope.launch { repository.dismissReminder(reminderId) }
    }

    fun deleteItemReminder(reminderId: Long) {
        viewModelScope.launch { repository.deleteItemReminder(reminderId) }
    }

    fun createTasksBatchFromAi(lines: List<String>, listId: Long, listName: String) {
        val uid = prefsState.value.currentUserId
        if (uid <= 0L) return
        viewModelScope.launch {
            lines.map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { idx, title ->
                repository.saveTaskItem(
                    TaskItemEntity(
                        ownerUserId = uid,
                        title = title,
                        listId = listId,
                        categoryLabel = listName,
                        manualOrder = idx
                    )
                )
            }
            _uiMessage.value = "Added ${lines.size} task(s) from note"
        }
    }

    suspend fun runNoteAiAction(
        action: NoteAiToolAction,
        noteTitle: String,
        noteContent: String,
        userPromptOrLang: String
    ): Pair<String, String?> {
        return NoteAiAssistantEngine.execute(
            action = action,
            noteTitle = noteTitle,
            noteContent = noteContent,
            userPromptOrLang = userPromptOrLang,
            allNotes = keepNotes.value,
            preferredProvider = activeAiProvider.value,
            allProviders = aiProviders.value,
            autoFailoverEnabled = prefsState.value.aiAutoFailoverEnabled
        )
    }

    // --- Quick To-Do Actions ---
    fun saveQuickTodo(item: QuickTodoEntity) {
        viewModelScope.launch { repository.saveQuickTodo(item) }
    }

    fun deleteQuickTodo(id: Long) {
        viewModelScope.launch { repository.deleteQuickTodo(id) }
    }

    // --- AI Provider & Multimodal Chat Actions ---
    fun setAiAutoFailoverEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.prefs.setAiAutoFailoverEnabled(enabled) }
    }

    fun selectAiProvider(providerId: Long) {
        _selectedAiProviderId.value = providerId
        _aiChatError.value = null
    }

    fun selectAiProviderModel(provider: AiProviderEntity, modelId: String) {
        val cleanModel = modelId.trim()
        if (cleanModel.isEmpty()) return
        viewModelScope.launch {
            val models = (provider.allModelsList() + cleanModel).distinct().joinToString(",")
            repository.saveAiProvider(
                provider.copy(
                    modelName = cleanModel,
                    availableModelsCsv = models
                )
            )
            _selectedAiProviderId.value = provider.id
            _aiChatError.value = null
        }
    }

    fun saveAiProvider(provider: AiProviderEntity) {
        viewModelScope.launch {
            val savedId = repository.saveAiProvider(provider)
            _selectedAiProviderId.value = savedId
            _aiChatError.value = null
        }
    }

    fun deleteAiProvider(providerId: Long) {
        viewModelScope.launch {
            repository.deleteAiProvider(providerId)
            if (_selectedAiProviderId.value == providerId) {
                _selectedAiProviderId.value = null
            }
        }
    }

    fun clearAiChatError() {
        _aiChatError.value = null
    }

    fun clearAiConversation() {
        val uid = prefsState.value.currentUserId
        val provId = activeAiProvider.value?.id ?: return
        if (uid <= 0L) return
        viewModelScope.launch {
            repository.clearAiChatConversation(uid, provId)
        }
    }

    fun sendAiMessage(promptText: String, imageUri: Uri?) {
        val uid = prefsState.value.currentUserId
        val provider = activeAiProvider.value ?: return
        if (uid <= 0L) return
        val cleanPrompt = promptText.trim()
        if (cleanPrompt.isEmpty() && imageUri == null) return

        viewModelScope.launch {
            _aiChatBusy.value = true
            _aiChatError.value = null
            try {
                val base64Image = if (imageUri != null) {
                    AiChatClient.downscaleUriToBase64(appContext, imageUri, maxDim = 640, quality = 75)
                } else null
                val smallThumb = if (imageUri != null) {
                    AiChatClient.downscaleUriToBase64(appContext, imageUri, maxDim = 220, quality = 60)
                } else null

                val historySnapshot = aiChatMessages.value
                val userMsg = AiChatMessageEntity(
                    ownerUserId = uid,
                    providerId = provider.id,
                    role = "USER",
                    textContent = cleanPrompt.ifBlank { "Image attached" },
                    imageBase64Thumb = smallThumb
                )
                repository.saveAiChatMessage(userMsg)

                val result = AiFailoverOrchestrator.sendWithFailover(
                    preferredProvider = provider,
                    allProviders = aiProviders.value,
                    autoFailoverEnabled = prefsState.value.aiAutoFailoverEnabled,
                    history = historySnapshot,
                    promptText = cleanPrompt,
                    imageBase64 = base64Image
                )
                result.onSuccess { ok ->
                    repository.saveAiChatMessage(
                        AiChatMessageEntity(
                            ownerUserId = uid,
                            providerId = provider.id,
                            role = "ASSISTANT",
                            textContent = ok.text,
                            servedByProviderName = ok.providerName,
                            wasAutoSwitched = ok.wasAutoSwitched
                        )
                    )
                }.onFailure { err ->
                    _aiChatError.value = err.localizedMessage ?: "Failed to get response from ${provider.name}."
                }
            } finally {
                _aiChatBusy.value = false
            }
        }
    }

    fun markAllNotificationsRead() {
        val uid = prefsState.value.currentUserId
        if (uid > 0L) {
            viewModelScope.launch { repository.markAllNotificationsRead(uid) }
        }
    }

    fun setNotificationSettings(
        enabled: Boolean? = null,
        budgetWarnings: Boolean? = null,
        savingsReminders: Boolean? = null,
        monthlyReport: Boolean? = null,
        billReminders: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.prefs.setNotificationSettings(
                enabled, budgetWarnings, savingsReminders, monthlyReport, billReminders
            )
        }
    }

    fun verifyAndResetUserData(secret: String, doneMsg: String, onResult: (Boolean) -> Unit) {
        val uid = prefsState.value.currentUserId
        if (uid <= 0L) {
            onResult(false)
            return
        }
        val pid = activeProfile.value?.id ?: prefsState.value.activeProfileId
        val pName = activeProfile.value?.name ?: "Active Profile"
        viewModelScope.launch {
            val valid = repository.verifyResetSecret(uid, secret)
            if (!valid) {
                onResult(false)
                return@launch
            }
            repository.resetProfileFinanceData(uid, pid)
            _uiMessage.value = "$pName: $doneMsg"
            onResult(true)
        }
    }

    // --- Dual Cloud Sync: Gmail Cloud Vault (Zero-403) + Google Drive Backup & Recovery ---
    fun connectGmailDrive(
        activity: Activity,
        connectedMsg: String,
        switchAccount: Boolean = false
    ) {
        viewModelScope.launch {
            _driveBusy.value = true
            _pendingDriveAction.value = "CONNECT"
            try {
                if (switchAccount) {
                    repository.driveClient.signOutGoogleAccount()
                    repository.prefs.clearDriveConnection()
                    _pendingSignedInEmail.value = null
                }

                when (val signIn = repository.driveClient.signInWithGoogleAccount(activity)) {
                    is GoogleAccountSignInOutcome.Success -> {
                        val email = signIn.email
                        _pendingSignedInEmail.value = email
                        repository.prefs.setDriveConnection(email, authorized = true)
                        _pendingDriveAction.value = null
                        // Perform immediate zero-403 Cloud Vault + silent Drive sync
                        val syncRes = repository.backupNowToGoogleDrive(
                            interactive = false,
                            activity = activity,
                            forceDirectDriveOAuth = false
                        )
                        if (syncRes is DriveSyncResult.Success) {
                            _uiMessage.value = "$connectedMsg: $email (${syncRes.channelLabel})"
                        } else {
                            _uiMessage.value = "$connectedMsg: $email"
                        }
                    }
                    is GoogleAccountSignInOutcome.Cancelled -> {
                        _pendingDriveAction.value = null
                    }
                    is GoogleAccountSignInOutcome.NoAccount -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = signIn.message
                    }
                    is GoogleAccountSignInOutcome.Error -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = signIn.message
                    }
                }
            } finally {
                _driveBusy.value = false
            }
        }
    }

    fun disconnectGmailDrive(disconnectedMsg: String) {
        viewModelScope.launch {
            _driveBusy.value = true
            repository.driveClient.signOutGoogleAccount()
            repository.prefs.clearDriveConnection()
            _pendingSignedInEmail.value = null
            _pendingDriveAction.value = null
            _uiMessage.value = disconnectedMsg
            _driveBusy.value = false
        }
    }

    fun backupNowToDrive(
        activity: Activity,
        successMsg: String,
        forceDirectDriveOAuth: Boolean = false
    ) {
        viewModelScope.launch {
            _driveBusy.value = true
            _pendingDriveAction.value = "BACKUP"
            try {
                val state = prefsState.value
                var knownEmail = state.connectedGmail.takeIf { it.isNotBlank() } ?: _pendingSignedInEmail.value
                val needsSignIn = knownEmail == null || repository.cloudVault.currentFirebaseUid() == null
                if (needsSignIn) {
                    when (val signIn = repository.driveClient.signInWithGoogleAccount(activity)) {
                        is GoogleAccountSignInOutcome.Success -> {
                            knownEmail = signIn.email
                            _pendingSignedInEmail.value = signIn.email
                            repository.prefs.setDriveConnection(signIn.email, authorized = true)
                        }
                        is GoogleAccountSignInOutcome.Cancelled -> {
                            _pendingDriveAction.value = null
                            return@launch
                        }
                        is GoogleAccountSignInOutcome.NoAccount -> {
                            if (!forceDirectDriveOAuth) {
                                _pendingDriveAction.value = null
                                _uiMessage.value = signIn.message
                                return@launch
                            }
                        }
                        is GoogleAccountSignInOutcome.Error -> {
                            if (!forceDirectDriveOAuth) {
                                _pendingDriveAction.value = null
                                _uiMessage.value = signIn.message
                                return@launch
                            }
                        }
                    }
                }
                when (
                    val res = repository.backupNowToGoogleDrive(
                        interactive = forceDirectDriveOAuth,
                        activity = activity,
                        forceDirectDriveOAuth = forceDirectDriveOAuth
                    )
                ) {
                    is DriveSyncResult.Success -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = "$successMsg (${res.channelLabel})"
                    }
                    is DriveSyncResult.NeedsConsent -> {
                        _pendingSignedInEmail.value = res.email ?: knownEmail
                        _pendingDriveConsent.value = res.pendingIntent
                    }
                    is DriveSyncResult.Error -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = res.message
                    }
                }
            } finally {
                _driveBusy.value = false
            }
        }
    }

    fun restoreFromDrive(
        activity: Activity,
        successMsg: String,
        previewFirst: Boolean = true,
        forceDirectDriveOAuth: Boolean = false
    ) {
        viewModelScope.launch {
            _driveBusy.value = true
            _pendingDriveAction.value = if (previewFirst) "RESTORE_PREVIEW" else "RESTORE"
            try {
                val state = prefsState.value
                var knownEmail = state.connectedGmail.takeIf { it.isNotBlank() } ?: _pendingSignedInEmail.value
                val needsSignIn = knownEmail == null || repository.cloudVault.currentFirebaseUid() == null
                if (needsSignIn) {
                    when (val signIn = repository.driveClient.signInWithGoogleAccount(activity)) {
                        is GoogleAccountSignInOutcome.Success -> {
                            knownEmail = signIn.email
                            _pendingSignedInEmail.value = signIn.email
                            repository.prefs.setDriveConnection(signIn.email, authorized = true)
                        }
                        is GoogleAccountSignInOutcome.Cancelled -> {
                            _pendingDriveAction.value = null
                            return@launch
                        }
                        else -> Unit
                    }
                }

                // 1. Check Zero-403 Gmail Cloud Vault first unless forceDirectDriveOAuth = true
                if (!forceDirectDriveOAuth) {
                    val preview = repository.inspectGoogleDriveBackup(activity = activity, forceDirectDriveOAuth = false)
                    if (preview != null) {
                        val resolvedEmail = preview.email ?: knownEmail.orEmpty()
                        if (resolvedEmail.isNotBlank()) {
                            repository.prefs.setDriveConnection(resolvedEmail, authorized = true)
                        }
                        if (previewFirst) {
                            _driveBackupPreview.value = preview
                        } else {
                            repository.restoreFullDatabaseSnapshot(preview.snapshot)
                            repository.prefs.setDriveBackupStatus(
                                System.currentTimeMillis(),
                                "Restored from Gmail Cloud Vault",
                                pending = false
                            )
                            _sessionUnlocked.value = true
                            _uiMessage.value = successMsg
                        }
                        _pendingDriveAction.value = null
                        return@launch
                    }
                }

                // 2. Fallback or explicit Direct Google Drive OAuth restore
                when (val res = repository.driveClient.restoreLatestBackup(activity = activity, knownEmail = knownEmail)) {
                    is DriveRestoreResult.Success -> {
                        val resolvedEmail = res.email ?: knownEmail.orEmpty()
                        repository.prefs.setDriveConnection(resolvedEmail, authorized = true)
                        val parsed = ExportManager.jsonToSnapshot(res.jsonPayload)
                        if (parsed.isSuccess) {
                            val snap = parsed.getOrThrow()
                            if (previewFirst) {
                                _driveBackupPreview.value = DriveBackupPreview(
                                    email = resolvedEmail.ifBlank { null },
                                    fileId = res.fileId,
                                    modifiedTime = res.modifiedTime,
                                    jsonPayload = res.jsonPayload,
                                    snapshot = snap
                                )
                            } else {
                                repository.restoreFullDatabaseSnapshot(snap)
                                if (resolvedEmail.isNotBlank()) {
                                    repository.prefs.setDriveConnection(resolvedEmail, authorized = true)
                                }
                                repository.prefs.setDriveBackupStatus(System.currentTimeMillis(), "Restored from Google Drive", pending = false)
                                _sessionUnlocked.value = true
                                _uiMessage.value = successMsg
                            }
                        } else {
                            _uiMessage.value = "Backup file is unreadable or corrupted."
                        }
                        _pendingDriveAction.value = null
                    }
                    is DriveRestoreResult.NeedsConsent -> {
                        if (forceDirectDriveOAuth) {
                            _pendingSignedInEmail.value = res.email ?: knownEmail
                            _pendingDriveConsent.value = res.pendingIntent
                        } else {
                            _pendingDriveAction.value = null
                            _uiMessage.value = "No backup found yet in Cloud Vault for ${knownEmail ?: "this Gmail"}. Tap 'Backup Now' first!"
                        }
                    }
                    is DriveRestoreResult.NotFound -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = "No backup found in Cloud Vault or Google Drive (${res.email ?: knownEmail.orEmpty()})"
                    }
                    is DriveRestoreResult.Error -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = res.message
                    }
                }
            } finally {
                _driveBusy.value = false
            }
        }
    }

    fun confirmRestorePreview(successMsg: String) {
        val preview = _driveBackupPreview.value ?: return
        viewModelScope.launch {
            _driveBusy.value = true
            _driveBackupPreview.value = null
            repository.restoreFullDatabaseSnapshot(preview.snapshot)
            preview.email?.takeIf { it.isNotBlank() }?.let {
                repository.prefs.setDriveConnection(it, authorized = true)
            }
            repository.prefs.setDriveBackupStatus(
                System.currentTimeMillis(),
                "Restored from ${preview.email ?: "Cloud Vault"}",
                pending = false
            )
            _sessionUnlocked.value = true
            _uiMessage.value = successMsg
            _driveBusy.value = false
        }
    }

    fun dismissRestorePreview() {
        _driveBackupPreview.value = null
    }

    fun onDriveConsentCancelled(cancelMsg: String) {
        _pendingDriveConsent.value = null
        _pendingDriveAction.value = null
        _driveBusy.value = false
        _uiMessage.value = cancelMsg
    }

    fun onDriveAuthorizationResult(
        activity: Activity,
        data: Intent?,
        backupSuccessMsg: String,
        restoreSuccessMsg: String
    ) {
        viewModelScope.launch {
            _pendingDriveConsent.value = null
            _driveBusy.value = true
            try {
                val fallbackEmail = _pendingSignedInEmail.value ?: prefsState.value.connectedGmail.takeIf { it.isNotBlank() }
                when (
                    val auth = repository.driveClient.handleAuthorizationIntentResult(
                        data = data,
                        activity = activity,
                        knownEmail = fallbackEmail
                    )
                ) {
                    is DriveAuthState.Authorized -> {
                        val finalEmail = auth.email ?: fallbackEmail.orEmpty()
                        repository.prefs.setDriveConnection(finalEmail, authorized = true)
                        when (_pendingDriveAction.value) {
                            "RESTORE_PREVIEW" -> restoreFromDrive(activity, restoreSuccessMsg, previewFirst = true, forceDirectDriveOAuth = true)
                            "RESTORE" -> restoreFromDrive(activity, restoreSuccessMsg, previewFirst = false, forceDirectDriveOAuth = true)
                            else -> backupNowToDrive(activity, backupSuccessMsg, forceDirectDriveOAuth = true)
                        }
                    }
                    is DriveAuthState.Error -> {
                        _pendingDriveAction.value = null
                        _uiMessage.value = auth.message
                    }
                    is DriveAuthState.NeedsConsent -> _pendingDriveConsent.value = auth.pendingIntent
                }
            } finally {
                _driveBusy.value = false
            }
        }
    }

    fun setAutoBackupOnNewData(enabled: Boolean) {
        viewModelScope.launch { repository.prefs.setAutoBackupOnNewData(enabled) }
    }

    fun setDailyAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            repository.prefs.setDailyAutoBackupEnabled(enabled)
            BackupScheduler.configureDailyAutoBackup(appContext, enabled)
        }
    }

    // --- Local Export & Restore ---
    fun exportToUri(uri: Uri, format: String, successMsg: String) {
        viewModelScope.launch {
            val state = prefsState.value
            val jsonPayload = if (format == "JSON") {
                val snap = repository.buildFullDatabaseSnapshot("USER_EXPORT", state.currentUserId)
                ExportManager.snapshotToJson(snap)
            } else null

            val result = ExportManager.writeToUri(appContext, uri) { out ->
                when (format) {
                    "CSV" -> out.write(
                        ExportManager.generateCsv(
                            transactions.value, categories.value, accounts.value
                        ).toByteArray(Charsets.UTF_8)
                    )
                    "XLS" -> out.write(
                        ExportManager.generateExcelXml(
                            transactions.value, categories.value, accounts.value, state.currencyCode
                        ).toByteArray(Charsets.UTF_8)
                    )
                    "PDF" -> ExportManager.writePdfReport(
                        out, transactions.value, categories.value, accounts.value, state.currencyCode
                    )
                    "JSON" -> out.write((jsonPayload ?: "{}").toByteArray(Charsets.UTF_8))
                }
            }
            result.onSuccess { _uiMessage.value = successMsg }
                .onFailure { _uiMessage.value = it.localizedMessage }
        }
    }

    fun restoreFromJsonUri(uri: Uri, successMsg: String) {
        viewModelScope.launch {
            ExportManager.readFromUri(appContext, uri).onSuccess { raw ->
                ExportManager.jsonToSnapshot(raw).onSuccess { snap ->
                    repository.restoreFullDatabaseSnapshot(snap)
                    _sessionUnlocked.value = true
                    _uiMessage.value = successMsg
                }.onFailure { _uiMessage.value = "Invalid backup JSON" }
            }.onFailure { _uiMessage.value = it.localizedMessage }
        }
    }
}

class MainViewModelFactory(
    private val context: Context,
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(context.applicationContext, repository) as T
    }
}
