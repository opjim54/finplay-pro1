package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.DriveSyncResult
import com.example.data.backup.ExportManager
import com.example.data.backup.GoogleAccountSignInOutcome
import com.example.data.games.FirestoreMultiplayerRepository
import com.example.data.games.FirestoreTournamentRepository
import com.example.data.local.AdminEarningEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.BroadcastNotificationEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.GameBotConfigEntity
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletRequestEntity
import com.example.data.local.TournamentTierConfigEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.FinanceRepository
import com.example.domain.games.PlayPaymentConfig
import com.example.security.SecurityAuditReport
import com.example.security.SecurityShield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val appContext: Context,
    private val repository: FinanceRepository
) : ViewModel() {

    private val multiplayerRepo = FirestoreMultiplayerRepository(appContext)
    private val cloudTournamentRepo = FirestoreTournamentRepository(appContext)

    val allUsers: StateFlow<List<UserEntity>> = repository.adminObserveAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.adminObserveAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.adminObserveAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfigs: StateFlow<List<AppConfigEntity>> = repository.adminObserveAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playWalletRequests: StateFlow<List<PlayWalletRequestEntity>> = repository.playWalletRepo.observeAllRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playPaymentConfig: StateFlow<PlayPaymentConfig> = repository.playWalletRepo.observePaymentConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayPaymentConfig())

    val allPlayWallets: StateFlow<List<PlayWalletEntity>> = repository.playWalletRepo.observeAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tournamentTiers: StateFlow<List<TournamentTierConfigEntity>> = repository.playWalletRepo.observeAllTiers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameBotConfigs: StateFlow<List<GameBotConfigEntity>> = repository.playWalletRepo.observeAllBotConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val externalTournaments: StateFlow<List<ExternalTournamentEntity>> = repository.playWalletRepo.observeExternalTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val broadcastNotifications: StateFlow<List<BroadcastNotificationEntity>> = repository.playWalletRepo.observeBroadcastNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminEarnings: StateFlow<List<AdminEarningEntity>> = repository.playWalletRepo.observeAdminEarnings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminDriveEmail: StateFlow<String> = appConfigs
        .map { list -> list.firstOrNull { it.key == "admin_master_drive_email" }?.value.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val adminAutoDriveSyncEnabled: StateFlow<Boolean> = appConfigs
        .map { list -> list.firstOrNull { it.key == "admin_master_auto_drive_sync" }?.value?.lowercase() != "false" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val adminDriveSyncStatus: StateFlow<String> = appConfigs
        .map { list -> list.firstOrNull { it.key == "admin_master_drive_last_status" }?.value.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Opt-in FLAG_SECURE (defaults to false so Streaming Emulators / screen-mirrored sessions render Admin Panel cleanly;
    // Admin can toggle ON anytime inside System -> Security Shield on physical devices).
    private val _screenShieldEnabled = MutableStateFlow(false)
    val screenShieldEnabled: StateFlow<Boolean> = _screenShieldEnabled.asStateFlow()

    private val _securityAudit = MutableStateFlow(SecurityShield.runIntegrityAudit(screenShieldEnabled = false))
    val securityAudit: StateFlow<SecurityAuditReport> = _securityAudit.asStateFlow()

    private val _isDriveBusy = MutableStateFlow(false)
    val isDriveBusy: StateFlow<Boolean> = _isDriveBusy.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.playWalletRepo.ensureDefaultGameConfigs()
            _securityAudit.value = SecurityShield.runIntegrityAudit(_screenShieldEnabled.value)
        }
    }

    fun clearMessage() {
        _adminMessage.value = null
    }

    fun toggleScreenShield(enabled: Boolean) {
        _screenShieldEnabled.value = enabled
        _securityAudit.value = SecurityShield.runIntegrityAudit(enabled)
        _adminMessage.value = if (enabled) "Screen-Capture Shield (FLAG_SECURE) Enabled" else "Screen-Capture Shield Disabled"
    }

    fun refreshSecurityAudit() {
        _securityAudit.value = SecurityShield.runIntegrityAudit(_screenShieldEnabled.value)
    }

    fun toggleAdminAutoDriveSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAdminAutoDriveSyncEnabled(enabled)
            _adminMessage.value = if (enabled) {
                "Auto-Save All Users Database to Admin Google Drive ENABLED"
            } else {
                "Admin Google Drive Auto-Save paused"
            }
        }
    }

    fun connectAdminGoogleDrive(activity: Activity) {
        viewModelScope.launch {
            _isDriveBusy.value = true
            try {
                when (val outcome = repository.driveClient.signInWithGoogleAccount(activity)) {
                    is GoogleAccountSignInOutcome.Success -> {
                        val email = outcome.email
                        repository.prefs.setDriveConnection(email, authorized = true)
                        repository.recordAdminDriveSyncStatus(email, "Connected Admin Drive ($email) — syncing database...")
                        val syncRes = repository.triggerAdminMasterDriveAutoSync(interactive = true, activity = activity)
                        _adminMessage.value = when (syncRes) {
                            is DriveSyncResult.Success -> "Admin Drive Connected ($email) & Full Database Auto-Saved!"
                            is DriveSyncResult.Error -> "Connected ($email). ${syncRes.message}"
                            else -> "Admin Drive connected ($email)"
                        }
                    }
                    is GoogleAccountSignInOutcome.Cancelled -> {
                        _adminMessage.value = "Google Drive connection cancelled"
                    }
                    is GoogleAccountSignInOutcome.NoAccount -> {
                        _adminMessage.value = outcome.message
                    }
                    is GoogleAccountSignInOutcome.Error -> {
                        _adminMessage.value = outcome.message
                    }
                }
            } finally {
                _isDriveBusy.value = false
            }
        }
    }

    fun syncAdminMasterDriveNow(activity: Activity) {
        viewModelScope.launch {
            _isDriveBusy.value = true
            try {
                val res = repository.triggerAdminMasterDriveAutoSync(interactive = true, activity = activity)
                _adminMessage.value = when (res) {
                    is DriveSyncResult.Success -> "All Users Database Saved & Rewritten (${res.channelLabel})"
                    is DriveSyncResult.Error -> res.message
                    else -> "Connect an Admin Google account first to sync Drive"
                }
            } finally {
                _isDriveBusy.value = false
            }
        }
    }

    fun addUser(username: String, password: String, displayName: String, existsMsg: String, addedMsg: String) {
        viewModelScope.launch {
            repository.registerUser(username, password, displayName, "Admin created", "admin")
                .onSuccess {
                    repository.prefs.setSession(userId = -1L, remember = false, isAdmin = true)
                    _adminMessage.value = addedMsg
                }
                .onFailure { _adminMessage.value = existsMsg }
        }
    }

    fun toggleUserLock(user: UserEntity) {
        viewModelScope.launch { repository.adminToggleUserLock(user) }
    }

    fun resetUserPassword(user: UserEntity, newPassword: String, msg: String) {
        viewModelScope.launch {
            repository.adminResetUserPassword(user, newPassword)
            _adminMessage.value = msg
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch { repository.adminDeleteUser(userId) }
    }

    fun adjustUserWallet(
        userId: Long,
        tournamentDelta: Double,
        winningDelta: Double,
        gameDelta: Double,
        note: String
    ) {
        viewModelScope.launch {
            repository.playWalletRepo.adminAdjustUserWallet(userId, tournamentDelta, winningDelta, gameDelta, note)
                .onSuccess {
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "User wallet coins updated & auto-saved"
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to adjust wallet" }
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch { repository.saveTransaction(tx) }
    }

    fun deleteTransactionPermanently(txId: Long) {
        viewModelScope.launch { repository.permanentDeleteTransaction(txId) }
    }

    fun saveCategory(cat: CategoryEntity) {
        viewModelScope.launch { repository.saveCategory(cat) }
    }

    fun deleteCategory(catId: Long) {
        viewModelScope.launch { repository.deleteCategory(catId) }
    }

    fun updateConfig(key: String, value: String) {
        viewModelScope.launch { repository.adminSetConfig(key, value) }
    }

    fun updateSecretAdminCredentials(newUsername: String, newPassword: String) {
        viewModelScope.launch {
            repository.adminUpdateSecretCredentials(newUsername, newPassword)
                .onSuccess {
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "Secret Admin Login re-encrypted (PBKDF2 120K)!"
                }
                .onFailure { _adminMessage.value = it.message ?: "Invalid credentials" }
        }
    }

    fun updateAdminPortalKey(newAdminKey: String) {
        viewModelScope.launch {
            repository.adminUpdateAdminKey(newAdminKey)
                .onSuccess {
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "Admin Portal Key re-encrypted (PBKDF2 120K)!"
                }
                .onFailure { _adminMessage.value = it.message ?: "Invalid Admin Key" }
        }
    }

    fun approveDepositRequest(requestId: Long, note: String = "Approved by Admin") {
        viewModelScope.launch {
            repository.playWalletRepo.adminApproveDeposit(requestId, note)
                .onSuccess {
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "Deposit approved & credited to Tournament Wallet"
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to approve deposit" }
        }
    }

    fun rejectDepositRequest(requestId: Long, note: String = "Rejected by Admin") {
        viewModelScope.launch {
            repository.playWalletRepo.adminRejectDeposit(requestId, note)
                .onSuccess { _adminMessage.value = "Deposit request rejected" }
                .onFailure { _adminMessage.value = it.message ?: "Failed to reject deposit" }
        }
    }

    fun markWithdrawalPaid(requestId: Long, note: String = "Paid by Admin") {
        viewModelScope.launch {
            repository.playWalletRepo.adminMarkWithdrawalPaid(requestId, note)
                .onSuccess {
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "Withdrawal marked as PAID"
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to mark paid" }
        }
    }

    fun rejectWithdrawalAndRefund(requestId: Long, note: String = "Refunded by Admin") {
        viewModelScope.launch {
            repository.playWalletRepo.adminRejectWithdrawal(requestId, note)
                .onSuccess {
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "Withdrawal rejected & refunded to Winning Balance"
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to refund withdrawal" }
        }
    }

    fun savePlayPaymentConfig(config: PlayPaymentConfig) {
        viewModelScope.launch {
            repository.playWalletRepo.adminSavePaymentConfig(config)
            launch { repository.triggerAdminMasterDriveAutoSync() }
            _adminMessage.value = "bKash / Nagad / Rocket / Binance settings & toggles saved!"
        }
    }

    fun saveTournamentTier(tier: TournamentTierConfigEntity) {
        viewModelScope.launch {
            repository.playWalletRepo.adminSaveTournamentTier(tier)
                .onSuccess { _adminMessage.value = "${tier.gameId} tier (${tier.tierLabel}) saved" }
                .onFailure { _adminMessage.value = it.message ?: "Failed to save tier" }
        }
    }

    fun deleteTournamentTier(tierId: Long) {
        viewModelScope.launch {
            repository.playWalletRepo.adminDeleteTournamentTier(tierId)
            _adminMessage.value = "Tournament entry tier deleted"
        }
    }

    fun saveGameBotConfig(config: GameBotConfigEntity) {
        viewModelScope.launch {
            repository.playWalletRepo.adminSaveGameBotConfig(config)
            _adminMessage.value = "${config.gameId} Bot Accuracy (${config.botAccuracyPercent}%) & Seats (${config.maxConcurrentSeats}) saved"
        }
    }

    fun createExternalTournament(tournament: ExternalTournamentEntity) {
        viewModelScope.launch {
            repository.playWalletRepo.adminCreateExternalTournament(tournament)
                .onSuccess { created ->
                    val cloudRes = cloudTournamentRepo.upsertCloudTournament(created)
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = if (cloudRes.isSuccess) {
                        "New ${created.gameCategory} tournament launched & synced LIVE to Firestore Hub!"
                    } else {
                        "New ${created.gameCategory} tournament launched locally! (Sign in with Admin Google for live cloud sync)"
                    }
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to create tournament" }
        }
    }

    fun updateExternalTournament(tournament: ExternalTournamentEntity) {
        viewModelScope.launch {
            repository.playWalletRepo.adminUpdateExternalTournament(tournament)
                .onSuccess {
                    val cloudRes = cloudTournamentRepo.upsertCloudTournament(tournament)
                    _adminMessage.value = if (cloudRes.isSuccess) {
                        "Tournament updated & synced LIVE to Firestore Hub"
                    } else {
                        "Tournament details updated locally"
                    }
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to update tournament" }
        }
    }

    fun sendRoomAlertToJoinedPlayers(tournamentId: Long) {
        viewModelScope.launch {
            val target = externalTournaments.value.firstOrNull { it.id == tournamentId }
            repository.playWalletRepo.adminDispatchPreMatchRoomAlert(tournamentId, appContext)
                .onSuccess { count ->
                    if (target != null) {
                        val updated = target.copy(roomAlertSent = true)
                        launch { cloudTournamentRepo.upsertCloudTournament(updated) }
                        val msg = if (target.roomIdCode.isNotBlank()) {
                            "Room ID: ${target.roomIdCode} | Pass: ${target.roomPassword.ifBlank { "None" }} — দ্রুত গেমে লগইন করে জয়েন করুন!"
                        } else {
                            "${target.title} ম্যাচ শুরু হতে যাচ্ছে! দ্রুত Room ID/Pass দেখে জয়েন করুন!"
                        }
                        launch { multiplayerRepo.publishCloudBroadcast("🔥 Room Ready: ${target.title}", msg, "URGENT") }
                    }
                    _adminMessage.value = "Room ID & Pass Alert dispatched to $count joined player(s) + Live Cloud Push Sent!"
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to dispatch room alert" }
        }
    }

    fun completeExternalTournament(tournamentId: Long, winnerUsername: String, winnerPrize: Double) {
        viewModelScope.launch {
            val target = externalTournaments.value.firstOrNull { it.id == tournamentId }
            repository.playWalletRepo.adminCompleteExternalTournament(tournamentId, winnerUsername, winnerPrize)
                .onSuccess {
                    if (target != null) {
                        launch {
                            cloudTournamentRepo.upsertCloudTournament(
                                target.copy(
                                    status = "COMPLETED",
                                    winnerName = winnerUsername.trim().ifBlank { target.winnerName }
                                )
                            )
                        }
                    }
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = "Tournament marked COMPLETED & synced to Firestore" +
                        if (winnerUsername.isNotBlank() && winnerPrize > 0.0) " • ৳${winnerPrize.toInt()} Winning Prize credited to $winnerUsername" else ""
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to complete tournament" }
        }
    }

    fun deleteExternalTournament(tournamentId: Long) {
        viewModelScope.launch {
            repository.playWalletRepo.adminDeleteExternalTournament(tournamentId)
            launch {
                cloudTournamentRepo.deleteCloudTournament(
                    FirestoreTournamentRepository.stableDocIdForLocal(tournamentId)
                )
            }
            _adminMessage.value = "Tournament deleted from local & Firestore Hub"
        }
    }

    fun sendBroadcastNotification(title: String, message: String, priority: String) {
        viewModelScope.launch {
            repository.playWalletRepo.adminSendBroadcastNotification(title, message, priority, appContext)
                .onSuccess {
                    val cloudRes = multiplayerRepo.publishCloudBroadcast(title, message, priority)
                    launch { repository.triggerAdminMasterDriveAutoSync() }
                    _adminMessage.value = if (cloudRes.isSuccess) {
                        "Broadcast sent via Android Push + All Users Inbox + Real-Time Cloud Sync!"
                    } else {
                        "Broadcast sent via Android Push + All Registered Users Inbox!"
                    }
                }
                .onFailure { _adminMessage.value = it.message ?: "Failed to send broadcast" }
        }
    }

    fun deleteBroadcastNotification(id: Long) {
        viewModelScope.launch {
            repository.playWalletRepo.adminDeleteBroadcastNotification(id)
        }
    }

    fun clearAllTrash(doneMsg: String) {
        viewModelScope.launch {
            repository.adminClearAllTrash()
            _adminMessage.value = doneMsg
        }
    }

    fun exportEntireDatabase(uri: Uri, format: String, doneMsg: String) {
        viewModelScope.launch {
            val snap = repository.buildFullDatabaseSnapshot("FULL_DATABASE", null)
            val result = ExportManager.writeToUri(appContext, uri) { out ->
                when (format) {
                    "CSV" -> out.write(
                        ExportManager.generateCsv(snap.transactions, snap.categories, snap.accounts)
                            .toByteArray(Charsets.UTF_8)
                    )
                    "PDF" -> ExportManager.writePdfReport(
                        out, snap.transactions, snap.categories, snap.accounts, "BDT"
                    )
                    else -> out.write(ExportManager.snapshotToJson(snap).toByteArray(Charsets.UTF_8))
                }
            }
            result.onSuccess { _adminMessage.value = doneMsg }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun restoreEntireDatabase(uri: Uri, doneMsg: String) {
        viewModelScope.launch {
            ExportManager.readFromUri(appContext, uri).onSuccess { raw ->
                ExportManager.jsonToSnapshot(raw).onSuccess { snap ->
                    repository.restoreFullDatabaseSnapshot(snap)
                    repository.prefs.setSession(-1L, remember = false, isAdmin = true)
                    _adminMessage.value = doneMsg
                }.onFailure { _adminMessage.value = "Invalid JSON backup" }
            }
        }
    }
}

class AdminViewModelFactory(
    private val context: Context,
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AdminViewModel(context.applicationContext, repository) as T
    }
}
