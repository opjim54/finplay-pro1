package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import com.example.data.backup.DriveBackupPreview
import com.example.data.local.KeepNoteEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.DateTimeUtil
import com.example.i18n.LanguageManager
import com.example.i18n.LocalAppLanguage
import com.example.i18n.LocalStrings
import com.example.security.BiometricHelper
import com.example.security.SecurityShield
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AnalyticsReportsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.NotesAndTasksScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileSwitcherSheet
import com.example.ui.screens.QuickCalculatorSheet
import com.example.ui.screens.SmartToolsScreen
import com.example.ui.screens.TransactionFormSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WealthHubScreen
import com.example.ui.screens.WealthSection
import com.example.ui.screens.games.GamesArenaScreen
import com.example.ui.theme.SmartFinanceTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AdminViewModelFactory
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.example.ui.viewmodel.PlayEarnViewModel
import com.example.ui.viewmodel.PlayEarnViewModelFactory
import com.example.widget.BalanceSummaryWidget
import kotlinx.coroutines.launch

private enum class MainTab {
    DASHBOARD, LEDGER, NOTES_TASKS, TOOLS, SETTINGS
}

class MainActivity : FragmentActivity() {
    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingWidgetAction = intent?.getStringExtra(BalanceSummaryWidget.EXTRA_WIDGET_ACTION)
        val app = application as SmartFinanceApp

        setContent {
            val mainVm: MainViewModel = viewModel(
                factory = MainViewModelFactory(applicationContext, app.repository)
            )
            val adminVm: AdminViewModel = viewModel(
                factory = AdminViewModelFactory(applicationContext, app.repository)
            )
            val playEarnVm: PlayEarnViewModel = viewModel(
                factory = PlayEarnViewModelFactory(applicationContext, app.repository)
            )

            val prefs by mainVm.prefsState.collectAsStateWithLifecycle()
            val activeLanguage by LanguageManager.currentLanguage.collectAsStateWithLifecycle()
            val strings by LanguageManager.appStrings.collectAsStateWithLifecycle()

            SmartFinanceTheme(themeMode = prefs.themeMode) {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalAppLanguage provides activeLanguage
                ) {
                    SmartFinanceRoot(
                        activity = this@MainActivity,
                        mainVm = mainVm,
                        adminVm = adminVm,
                        playEarnVm = playEarnVm,
                        pendingWidgetAction = pendingWidgetAction,
                        onConsumeWidgetAction = { pendingWidgetAction = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction = intent.getStringExtra(BalanceSummaryWidget.EXTRA_WIDGET_ACTION)
    }
}

@Composable
private fun SmartFinanceRoot(
    activity: FragmentActivity,
    mainVm: MainViewModel,
    adminVm: AdminViewModel,
    playEarnVm: PlayEarnViewModel,
    pendingWidgetAction: String?,
    onConsumeWidgetAction: () -> Unit
) {
    val strings = LocalStrings.current
    val prefs by mainVm.prefsState.collectAsStateWithLifecycle()
    val sessionUnlocked by mainVm.sessionUnlocked.collectAsStateWithLifecycle()
    val authError by mainVm.authError.collectAsStateWithLifecycle()
    val authBusy by mainVm.authBusy.collectAsStateWithLifecycle()
    val uiMessage by mainVm.uiMessage.collectAsStateWithLifecycle()
    val driveBusy by mainVm.driveBusy.collectAsStateWithLifecycle()
    val pendingDriveConsent by mainVm.pendingDriveConsent.collectAsStateWithLifecycle()

    val currentUser by mainVm.currentUser.collectAsStateWithLifecycle()
    val profiles by mainVm.profiles.collectAsStateWithLifecycle()
    val activeProfile by mainVm.activeProfile.collectAsStateWithLifecycle()
    val allUserAccounts by mainVm.allUserAccounts.collectAsStateWithLifecycle()
    val allUserTransactions by mainVm.allUserTransactions.collectAsStateWithLifecycle()
    val accounts by mainVm.accounts.collectAsStateWithLifecycle()
    val categories by mainVm.categories.collectAsStateWithLifecycle()
    val transactions by mainVm.transactions.collectAsStateWithLifecycle()
    val trashTransactions by mainVm.trashTransactions.collectAsStateWithLifecycle()
    val budgets by mainVm.budgets.collectAsStateWithLifecycle()
    val savingsGoals by mainVm.savingsGoals.collectAsStateWithLifecycle()
    val recurringRules by mainVm.recurringRules.collectAsStateWithLifecycle()
    val loans by mainVm.loans.collectAsStateWithLifecycle()
    val wishlist by mainVm.wishlist.collectAsStateWithLifecycle()
    val investments by mainVm.investments.collectAsStateWithLifecycle()
    val notes by mainVm.notes.collectAsStateWithLifecycle()
    val notifications by mainVm.notifications.collectAsStateWithLifecycle()
    val keepNotes by mainVm.keepNotes.collectAsStateWithLifecycle()
    val archivedKeepNotes by mainVm.archivedKeepNotes.collectAsStateWithLifecycle()
    val trashedKeepNotes by mainVm.trashedKeepNotes.collectAsStateWithLifecycle()
    val taskLists by mainVm.taskLists.collectAsStateWithLifecycle()
    val taskItems by mainVm.taskItems.collectAsStateWithLifecycle()
    val allReminders by mainVm.allReminders.collectAsStateWithLifecycle()
    val quickTodos by mainVm.quickTodos.collectAsStateWithLifecycle()
    val aiProviders by mainVm.aiProviders.collectAsStateWithLifecycle()
    val activeAiProvider by mainVm.activeAiProvider.collectAsStateWithLifecycle()
    val aiChatMessages by mainVm.aiChatMessages.collectAsStateWithLifecycle()
    val aiChatBusy by mainVm.aiChatBusy.collectAsStateWithLifecycle()
    val aiChatError by mainVm.aiChatError.collectAsStateWithLifecycle()
    val dashboardMetrics by mainVm.dashboardMetrics.collectAsStateWithLifecycle()
    val deepAnalytics by mainVm.deepAnalytics.collectAsStateWithLifecycle()
    val gameLeaderboard by mainVm.gameLeaderboard.collectAsStateWithLifecycle()
    val playWallet by playEarnVm.playWallet.collectAsStateWithLifecycle()
    val playWalletRequests by playEarnVm.walletRequests.collectAsStateWithLifecycle()
    val playWalletLedger by playEarnVm.walletLedger.collectAsStateWithLifecycle()
    val cloudWalletSummary by playEarnVm.cloudWalletSummary.collectAsStateWithLifecycle()
    val cloudWalletLedger by playEarnVm.cloudWalletLedger.collectAsStateWithLifecycle()
    val playPaymentConfig by playEarnVm.paymentConfig.collectAsStateWithLifecycle()
    val tournamentTiers by playEarnVm.tournamentTiers.collectAsStateWithLifecycle()
    val gameBotConfigs by playEarnVm.gameBotConfigs.collectAsStateWithLifecycle()
    val externalTournaments by playEarnVm.externalTournaments.collectAsStateWithLifecycle()
    val cloudTournaments by playEarnVm.cloudTournaments.collectAsStateWithLifecycle()
    val myExternalEntries by playEarnVm.myExternalEntries.collectAsStateWithLifecycle()
    val allExternalEntries by playEarnVm.allExternalEntries.collectAsStateWithLifecycle()
    val joinedCloudEntries by playEarnVm.joinedCloudEntries.collectAsStateWithLifecycle()
    val inspectedCloudRoster by playEarnVm.inspectedCloudRoster.collectAsStateWithLifecycle()
    val broadcastNotifications by playEarnVm.broadcastNotifications.collectAsStateWithLifecycle()
    val liveSeatStatuses by playEarnVm.liveSeatStatuses.collectAsStateWithLifecycle()
    val matchmakingScanState by playEarnVm.matchmakingScanState.collectAsStateWithLifecycle()
    val onlineAuth by playEarnVm.onlineAuth.collectAsStateWithLifecycle()
    val activeOnlineRoom by playEarnVm.activeRoom.collectAsStateWithLifecycle()
    val recentOnlineRooms by playEarnVm.recentOnlineRooms.collectAsStateWithLifecycle()
    val selectedEntryFee by playEarnVm.selectedEntryFee.collectAsStateWithLifecycle()
    val tournamentLeaderboard by playEarnVm.tournamentLeaderboard.collectAsStateWithLifecycle()
    val lastSettlement by playEarnVm.lastSettlement.collectAsStateWithLifecycle()
    val playFeedback by playEarnVm.feedbackMessage.collectAsStateWithLifecycle()
    val isOnlineBusy by playEarnVm.isBusy.collectAsStateWithLifecycle()

    val driveBackupPreview by mainVm.driveBackupPreview.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember(activity) { BiometricHelper.isBiometricAvailable(activity) }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mainVm.onDriveAuthorizationResult(
                activity = activity,
                data = result.data,
                backupSuccessMsg = strings.driveBackupSuccess,
                restoreSuccessMsg = strings.driveRestoreSuccess
            )
        } else {
            mainVm.onDriveConsentCancelled(strings.driveConsentCancelled)
        }
    }

    driveBackupPreview?.let { preview ->
        RestoreBackupPreviewDialog(
            preview = preview,
            onConfirm = { mainVm.confirmRestorePreview(strings.driveRestoreSuccess) },
            onDismiss = mainVm::dismissRestorePreview
        )
    }

    LaunchedEffect(pendingDriveConsent) {
        pendingDriveConsent?.let { pi ->
            runCatching {
                driveAuthLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            }.onFailure {
                mainVm.clearPendingDriveConsent()
            }
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainVm.clearUiMessage()
        }
    }

    // 1. Admin Portal (Completely hidden from normal users)
    if (prefs.isAdminSession) {
        val allUsers by adminVm.allUsers.collectAsStateWithLifecycle()
        val allTransactions by adminVm.allTransactions.collectAsStateWithLifecycle()
        val allCategories by adminVm.allCategories.collectAsStateWithLifecycle()
        val appConfigs by adminVm.appConfigs.collectAsStateWithLifecycle()
        val adminPlayRequests by adminVm.playWalletRequests.collectAsStateWithLifecycle()
        val adminPlayPaymentConfig by adminVm.playPaymentConfig.collectAsStateWithLifecycle()
        val adminAllPlayWallets by adminVm.allPlayWallets.collectAsStateWithLifecycle()
        val adminTournamentTiers by adminVm.tournamentTiers.collectAsStateWithLifecycle()
        val adminGameBotConfigs by adminVm.gameBotConfigs.collectAsStateWithLifecycle()
        val adminExternalTournaments by adminVm.externalTournaments.collectAsStateWithLifecycle()
        val adminBroadcasts by adminVm.broadcastNotifications.collectAsStateWithLifecycle()
        val adminEarnings by adminVm.adminEarnings.collectAsStateWithLifecycle()
        val securityAudit by adminVm.securityAudit.collectAsStateWithLifecycle()
        val screenShieldEnabled by adminVm.screenShieldEnabled.collectAsStateWithLifecycle()
        val adminDriveEmail by adminVm.adminDriveEmail.collectAsStateWithLifecycle()
        val adminAutoDriveSyncEnabled by adminVm.adminAutoDriveSyncEnabled.collectAsStateWithLifecycle()
        val adminDriveSyncStatus by adminVm.adminDriveSyncStatus.collectAsStateWithLifecycle()
        val isAdminDriveBusy by adminVm.isDriveBusy.collectAsStateWithLifecycle()
        val adminMsg by adminVm.adminMessage.collectAsStateWithLifecycle()

        DisposableEffect(screenShieldEnabled) {
            SecurityShield.applyScreenCaptureShield(activity, screenShieldEnabled)
            onDispose { SecurityShield.applyScreenCaptureShield(activity, false) }
        }

        LaunchedEffect(adminMsg) {
            adminMsg?.let {
                snackbarHostState.showSnackbar(it)
                adminVm.clearMessage()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AdminDashboardScreen(
                users = allUsers,
                transactions = allTransactions,
                categories = allCategories,
                configs = appConfigs,
                playWalletRequests = adminPlayRequests,
                playPaymentConfig = adminPlayPaymentConfig,
                allPlayWallets = adminAllPlayWallets,
                tournamentTiers = adminTournamentTiers,
                gameBotConfigs = adminGameBotConfigs,
                externalTournaments = adminExternalTournaments,
                broadcastNotifications = adminBroadcasts,
                adminEarnings = adminEarnings,
                securityAudit = securityAudit,
                screenShieldEnabled = screenShieldEnabled,
                adminDriveEmail = adminDriveEmail,
                adminAutoDriveSyncEnabled = adminAutoDriveSyncEnabled,
                adminDriveSyncStatus = adminDriveSyncStatus,
                isDriveBusy = isAdminDriveBusy,
                currencyCode = prefs.currencyCode,
                currentLanguage = prefs.language,
                onLanguageChange = mainVm::setLanguage,
                onAddUser = { u, p, d ->
                    adminVm.addUser(u, p, d, strings.userAlreadyExists, strings.save)
                },
                onToggleLockUser = adminVm::toggleUserLock,
                onResetUserPassword = { user, pass ->
                    adminVm.resetUserPassword(user, pass, strings.passwordUpdatedSuccess)
                },
                onDeleteUser = adminVm::deleteUser,
                onAdjustUserWallet = adminVm::adjustUserWallet,
                onDeleteTransaction = adminVm::deleteTransactionPermanently,
                onEditTransactionAmount = { tx, amt, note ->
                    adminVm.updateTransaction(tx.copy(amount = amt, note = note))
                },
                onExportAll = { uri, format ->
                    adminVm.exportEntireDatabase(uri, format, strings.exportSuccess)
                },
                onRestoreEntireDb = { uri ->
                    adminVm.restoreEntireDatabase(uri, strings.driveRestoreSuccess)
                },
                onClearTrash = { adminVm.clearAllTrash(strings.emptyTrash) },
                onUpdateConfig = adminVm::updateConfig,
                onUpdateSecretAdminCredentials = adminVm::updateSecretAdminCredentials,
                onUpdateAdminPortalKey = adminVm::updateAdminPortalKey,
                onApproveDeposit = { id -> adminVm.approveDepositRequest(id) },
                onRejectDeposit = { id -> adminVm.rejectDepositRequest(id) },
                onMarkWithdrawalPaid = { id -> adminVm.markWithdrawalPaid(id) },
                onRejectWithdrawalRefund = { id -> adminVm.rejectWithdrawalAndRefund(id) },
                onSavePlayPaymentConfig = adminVm::savePlayPaymentConfig,
                onSaveTournamentTier = adminVm::saveTournamentTier,
                onDeleteTournamentTier = adminVm::deleteTournamentTier,
                onSaveGameBotConfig = adminVm::saveGameBotConfig,
                onCreateExternalTournament = adminVm::createExternalTournament,
                onUpdateExternalTournament = adminVm::updateExternalTournament,
                onSendRoomAlert = adminVm::sendRoomAlertToJoinedPlayers,
                onCompleteExternalTournament = adminVm::completeExternalTournament,
                onDeleteExternalTournament = adminVm::deleteExternalTournament,
                onSendBroadcast = adminVm::sendBroadcastNotification,
                onDeleteBroadcast = adminVm::deleteBroadcastNotification,
                onToggleScreenShield = adminVm::toggleScreenShield,
                onRefreshSecurityAudit = adminVm::refreshSecurityAudit,
                onConnectAdminDrive = { adminVm.connectAdminGoogleDrive(activity) },
                onSyncAdminDriveNow = { adminVm.syncAdminMasterDriveNow(activity) },
                onToggleAdminAutoDriveSync = adminVm::toggleAdminAutoDriveSync,
                onExitAdmin = mainVm::logout
            )
            SnackbarHost(hostState = snackbarHostState)
        }
        return
    }

    // 2. Authentication Gate
    val isLoggedIn = prefs.currentUserId > 0L
    if (!isLoggedIn) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(
                currentLanguage = prefs.language,
                authError = authError,
                authBusy = authBusy,
                driveBusy = driveBusy,
                onLanguageChange = mainVm::setLanguage,
                onLogin = { u, p, rem ->
                    mainVm.login(u, p, rem, strings.invalidCredentials, strings.accountLockedError)
                },
                onRegister = { u, p, d, q, a ->
                    mainVm.register(u, p, d, q, a, strings.userAlreadyExists)
                },
                onForgotPassword = { u, a, np ->
                    mainVm.resetForgottenPassword(
                        u, a, np,
                        strings.passwordUpdatedSuccess,
                        strings.wrongSecurityAnswer,
                        strings.invalidCredentials
                    )
                },
                onAdminLogin = { key ->
                    mainVm.loginAdmin(key, strings.invalidAdminKey)
                },
                onRestoreFromDrive = { mainVm.restoreFromDrive(activity, strings.driveRestoreSuccess, previewFirst = true) },
                onClearError = mainVm::clearAuthError
            )
            SnackbarHost(hostState = snackbarHostState)
        }
        return
    }

    // 3. PIN / Biometric App Lock Gate
    val requiresLock = (prefs.pinEnabled || prefs.biometricEnabled) && !sessionUnlocked
    if (requiresLock) {
        LockScreen(
            biometricAvailable = biometricAvailable,
            biometricEnabled = prefs.biometricEnabled,
            expectedPinLength = prefs.pinLength,
            authError = authError,
            onVerifyPin = { pin -> mainVm.verifyPin(pin, strings.wrongPinError) },
            onClearError = mainVm::clearAuthError,
            onRequestBiometric = {
                BiometricHelper.authenticate(
                    activity = activity,
                    title = strings.biometricPromptTitle,
                    subtitle = strings.biometricPromptSubtitle,
                    negativeText = strings.cancel,
                    onSuccess = { mainVm.unlockWithBiometric() },
                    onError = {}
                )
            },
            onLogout = mainVm::logout
        )
        return
    }

    // 4. Authenticated Main App Shell
    var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var wealthInitialSection by remember { mutableStateOf(WealthSection.ACCOUNTS) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var showAnalyticsOverlay by remember { mutableStateOf(false) }
    var showWealthOverlay by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var showGamesArena by remember { mutableStateOf(false) }

    var showTxSheet by remember { mutableStateOf(false) }
    var txSheetType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var txSheetQrFocus by remember { mutableStateOf(false) }
    var txSheetInitialAmount by remember { mutableStateOf<Double?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showQuickCalculator by remember { mutableStateOf(false) }

    LaunchedEffect(pendingWidgetAction) {
        if (pendingWidgetAction == BalanceSummaryWidget.ACTION_NEW_TRANSACTION) {
            showNotifications = false
            showAiAssistant = false
            showAnalyticsOverlay = false
            showWealthOverlay = false
            showProfileSwitcher = false
            showGamesArena = false
            activeTab = MainTab.DASHBOARD
            editingTransaction = null
            txSheetInitialAmount = null
            txSheetType = TransactionType.EXPENSE
            txSheetQrFocus = false
            showTxSheet = true
            onConsumeWidgetAction()
        }
    }

    if (showGamesArena) {
        BackHandler { showGamesArena = false }
        GamesArenaScreen(
            leaderboard = gameLeaderboard,
            playWallet = playWallet,
            walletRequests = playWalletRequests,
            walletLedger = playWalletLedger,
            cloudWalletSummary = cloudWalletSummary,
            cloudWalletLedger = cloudWalletLedger,
            paymentConfig = playPaymentConfig,
            tournamentTiers = tournamentTiers,
            gameBotConfigs = gameBotConfigs,
            externalTournaments = externalTournaments,
            cloudTournaments = cloudTournaments,
            myExternalEntries = myExternalEntries,
            allExternalEntries = allExternalEntries,
            joinedCloudEntries = joinedCloudEntries,
            inspectedCloudRoster = inspectedCloudRoster,
            tournamentLeaderboard = tournamentLeaderboard,
            currentUserId = prefs.currentUserId,
            broadcastNotifications = broadcastNotifications,
            liveSeatStatuses = liveSeatStatuses,
            matchmakingScanState = matchmakingScanState,
            onlineAuth = onlineAuth,
            activeOnlineRoom = activeOnlineRoom,
            recentOnlineRooms = recentOnlineRooms,
            selectedEntryFee = selectedEntryFee,
            lastSettlement = lastSettlement,
            playFeedbackMessage = playFeedback,
            isOnlineBusy = isOnlineBusy,
            onClearPlayFeedback = playEarnVm::clearFeedback,
            onClearSettlement = playEarnVm::clearSettlement,
            onClearScanState = playEarnVm::clearScanState,
            onRefreshLiveSeats = playEarnVm::refreshLiveSeatStatuses,
            onSelectTournamentFee = { fee -> playEarnVm.selectTournamentFee(fee) },
            onEnterTournamentMatch = playEarnVm::enterTournamentMatch,
            onStartSmartMatchmaking = playEarnVm::startSmartMatchmaking,
            onSettleTournamentMatch = playEarnVm::settleTournamentMatch,
            onJoinExternalTournament = playEarnVm::joinExternalTournament,
            onJoinHubTournament = playEarnVm::joinHubTournament,
            onInspectTournamentRoster = playEarnVm::inspectTournamentRoster,
            onTriggerPreMatchAlert = playEarnVm::triggerPreMatchRoomAlertIfDue,
            onSubmitDeposit = playEarnVm::submitDeposit,
            onSubmitWithdraw = playEarnVm::submitWithdraw,
            onTransferTournamentToGame = playEarnVm::transferTournamentToGame,
            onBuyArmoryItem = playEarnVm::buyArmoryItem,
            onEquipArmoryItem = playEarnVm::equipArmoryItem,
            onConsumeSkill = playEarnVm::consumeSkill,
            onSignInGoogleForOnline = { playEarnVm.signInWithGoogleForOnline(activity) },
            onSignOutOnline = playEarnVm::signOutOnlineAccount,
            onCreateOnlineRoom = playEarnVm::createOnlineRoom,
            onJoinOnlineRoom = playEarnVm::joinOnlineRoom,
            onQuickMatchOnline = playEarnVm::startQuickMatch,
            onReopenOnlineRoom = playEarnVm::reopenRoom,
            onSubmitOnlineMove = playEarnVm::submitOnlineMove,
            onLeaveOnlineRoom = playEarnVm::leaveOrForfeitActiveRoom,
            onRecordMatch = mainVm::recordGameMatch,
            onAwardMatchCoins = playEarnVm::awardMatchCompletionCoins,
            onRecordLeaderboardPoints = { gid, outcome, score ->
                playEarnVm.recordCloudLeaderboardPoints(gid, outcome, score)
            },
            onResetLeaderboard = mainVm::resetGameLeaderboard,
            onBack = { showGamesArena = false }
        )
        return
    }

    if (showNotifications) {
        BackHandler { showNotifications = false }
        NotificationsScreen(
            notifications = notifications,
            onBack = { showNotifications = false },
            onMarkAllRead = mainVm::markAllNotificationsRead
        )
        return
    }

    if (showAiAssistant) {
        BackHandler { showAiAssistant = false }
        val pendingTasksCount = remember(taskItems) { taskItems.count { !it.isCompleted } }
        val profileSummary = remember(activeProfile, dashboardMetrics, deepAnalytics, pendingTasksCount, prefs.currencyCode) {
            "Profile: ${activeProfile?.name ?: "Personal"} | Currency: ${prefs.currencyCode} | " +
                "Balance: ${dashboardMetrics.totalBalance} | Monthly Income: ${dashboardMetrics.monthlyIncome} | " +
                "Monthly Expense: ${dashboardMetrics.monthlyExpense} | Savings Rate: ${dashboardMetrics.monthlySavingsRate.toInt()}% | " +
                "Health Score: ${deepAnalytics.financialHealthScore}/100 | Pending Tasks: $pendingTasksCount"
        }
        AiAssistantScreen(
            userId = prefs.currentUserId,
            providers = aiProviders,
            activeProvider = activeAiProvider,
            messages = aiChatMessages,
            busy = aiChatBusy,
            errorText = aiChatError,
            autoFailoverEnabled = prefs.aiAutoFailoverEnabled,
            profileContextSummary = profileSummary,
            onBack = { showAiAssistant = false },
            onToggleAutoFailover = mainVm::setAiAutoFailoverEnabled,
            onSelectProvider = mainVm::selectAiProvider,
            onSelectModel = mainVm::selectAiProviderModel,
            onSaveProvider = mainVm::saveAiProvider,
            onDeleteProvider = mainVm::deleteAiProvider,
            onSendMessage = mainVm::sendAiMessage,
            onClearChat = mainVm::clearAiConversation,
            onClearError = mainVm::clearAiChatError,
            onSaveReplyToNote = { replyText ->
                mainVm.saveKeepNote(
                    KeepNoteEntity(
                        ownerUserId = prefs.currentUserId,
                        title = "AI Note",
                        content = replyText,
                        tagsCsv = "AI"
                    )
                )
            },
            onNotify = mainVm::showMessage
        )
        return
    }

    if (showAnalyticsOverlay) {
        BackHandler { showAnalyticsOverlay = false }
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            topBar = {
                TopAppBar(
                    title = { Text(strings.analytics, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { showAnalyticsOverlay = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                        }
                    }
                )
            }
        ) { pad ->
            Box(modifier = Modifier.fillMaxSize().padding(pad)) {
                AnalyticsReportsScreen(
                    currencyCode = prefs.currencyCode,
                    deepAnalytics = deepAnalytics,
                    computeReport = mainVm::computeReport
                )
            }
        }
        return
    }

    if (showWealthOverlay) {
        BackHandler { showWealthOverlay = false }
        @OptIn(ExperimentalMaterial3Api::class)
        Scaffold(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("${activeProfile?.name ?: "Personal"} • ${strings.navWealth}", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { showWealthOverlay = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                        }
                    }
                )
            }
        ) { pad ->
            Box(modifier = Modifier.fillMaxSize().padding(pad)) {
                WealthHubScreen(
                    userId = prefs.currentUserId,
                    profileId = activeProfile?.id ?: prefs.activeProfileId,
                    currencyCode = prefs.currencyCode,
                    initialSection = wealthInitialSection,
                    accounts = accounts,
                    categories = categories,
                    transactions = transactions,
                    budgets = budgets,
                    savingsGoals = savingsGoals,
                    recurringRules = recurringRules,
                    loans = loans,
                    wishlist = wishlist,
                    investments = investments,
                    notes = notes,
                    onSaveAccount = mainVm::saveAccount,
                    onDeleteAccount = mainVm::deleteAccount,
                    onSaveCategory = mainVm::saveCategory,
                    onDeleteCategory = mainVm::deleteCategory,
                    onSaveBudget = mainVm::saveBudget,
                    onDeleteBudget = mainVm::deleteBudget,
                    onSaveGoal = mainVm::saveSavingsGoal,
                    onDepositGoal = mainVm::depositSavings,
                    onDeleteGoal = mainVm::deleteSavingsGoal,
                    onSaveRecurring = mainVm::saveRecurringRule,
                    onDeleteRecurring = mainVm::deleteRecurringRule,
                    onRunRecurringNow = mainVm::processDueRecurringNow,
                    onSaveLoan = mainVm::saveLoan,
                    onPayLoan = mainVm::payLoan,
                    onDeleteLoan = mainVm::deleteLoan,
                    onSaveWishlist = mainVm::saveWishlist,
                    onDeleteWishlist = mainVm::deleteWishlist,
                    onSaveInvestment = mainVm::saveInvestment,
                    onDeleteInvestment = mainVm::deleteInvestment,
                    onSaveNote = mainVm::saveNote,
                    onDeleteNote = mainVm::deleteNote
                )
            }
        }
        return
    }

    if (activeTab != MainTab.DASHBOARD) {
        BackHandler { activeTab = MainTab.DASHBOARD }
    }

    val navItems = listOf(
        Triple(MainTab.DASHBOARD, strings.navDashboard, Pair(Icons.Filled.Dashboard, Icons.Outlined.Dashboard)),
        Triple(MainTab.LEDGER, strings.navLedger, Pair(Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong)),
        Triple(MainTab.NOTES_TASKS, strings.navNotesTasks, Pair(Icons.Filled.NoteAlt, Icons.Outlined.NoteAlt)),
        Triple(MainTab.TOOLS, strings.navTools, Pair(Icons.Filled.Build, Icons.Outlined.Build)),
        Triple(MainTab.SETTINGS, strings.navSettings, Pair(Icons.Filled.Settings, Icons.Outlined.Settings))
    )

    val triggerSoftDelete: (TransactionEntity) -> Unit = { tx ->
        mainVm.softDeleteTransaction(tx.id)
        scope.launch {
            val res = snackbarHostState.showSnackbar(
                message = strings.transactionDeleted,
                actionLabel = strings.undo,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                mainVm.restoreTransaction(tx.id)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (activeTab == MainTab.DASHBOARD || activeTab == MainTab.LEDGER) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            txSheetInitialAmount = null
                            txSheetType = TransactionType.EXPENSE
                            txSheetQrFocus = false
                            showTxSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = strings.addRecord)
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }

                val allProfilesNetWorth = remember(allUserAccounts) { allUserAccounts.sumOf { it.balance } }
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))) togetherWith
                            fadeOut(animationSpec = tween(140))
                    },
                    label = "mainTabTransition",
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) { currentTab ->
                    when (currentTab) {
                        MainTab.DASHBOARD -> DashboardScreen(
                            user = currentUser,
                            activeProfile = activeProfile,
                            prefs = prefs,
                            metrics = dashboardMetrics,
                            accounts = accounts,
                            categories = categories,
                            recentTransactions = transactions,
                            unreadNotifications = notifications.count { !it.isRead },
                            deepAnalytics = deepAnalytics,
                            allProfilesNetWorth = allProfilesNetWorth,
                            profileCount = profiles.size.coerceAtLeast(1),
                            playWallet = playWallet,
                            onOpenProfileSwitcher = { showProfileSwitcher = true },
                            onOpenAiAssistant = { showAiAssistant = true },
                            onOpenGamesArena = { showGamesArena = true },
                            onOpenAnalytics = { showAnalyticsOverlay = true },
                            onOpenWealthHub = {
                                wealthInitialSection = WealthSection.ACCOUNTS
                                showWealthOverlay = true
                            },
                            onQuickAdd = { type, qr ->
                                editingTransaction = null
                                txSheetInitialAmount = null
                                txSheetType = type
                                txSheetQrFocus = qr
                                showTxSheet = true
                            },
                            onOpenQuickCalculator = { showQuickCalculator = true },
                            onPayObligation = { ob ->
                                mainVm.payUpcomingObligation(ob, strings.save)
                            },
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                txSheetInitialAmount = null
                                txSheetType = tx.type
                                txSheetQrFocus = false
                                showTxSheet = true
                            },
                            onDuplicateTransaction = mainVm::duplicateTransaction,
                            onToggleFavorite = mainVm::toggleFavorite,
                            onDeleteTransaction = triggerSoftDelete,
                            onOpenNotifications = { showNotifications = true },
                            onOpenDriveBackup = { activeTab = MainTab.SETTINGS },
                            onOpenAllTransactions = { activeTab = MainTab.LEDGER },
                            onOpenBudgets = {
                                wealthInitialSection = WealthSection.BUDGETS
                                showWealthOverlay = true
                            }
                        )

                        MainTab.LEDGER -> TransactionsScreen(
                            transactions = transactions,
                            trashTransactions = trashTransactions,
                            categories = categories,
                            accounts = accounts,
                            currencyCode = prefs.currencyCode,
                            onEditTransaction = { tx ->
                                editingTransaction = tx
                                txSheetType = tx.type
                                txSheetQrFocus = false
                                showTxSheet = true
                            },
                            onDuplicateTransaction = mainVm::duplicateTransaction,
                            onToggleFavorite = mainVm::toggleFavorite,
                            onDeleteTransaction = triggerSoftDelete,
                            onRestoreFromTrash = mainVm::restoreTransaction,
                            onPermanentDelete = mainVm::permanentDeleteTransaction
                        )

                        MainTab.NOTES_TASKS -> NotesAndTasksScreen(
                            userId = prefs.currentUserId,
                            activeNotes = keepNotes,
                            archivedNotes = archivedKeepNotes,
                            trashedNotes = trashedKeepNotes,
                            tasks = taskItems,
                            taskLists = taskLists,
                            reminders = allReminders,
                            pinEnabled = prefs.pinEnabled,
                            biometricEnabled = prefs.biometricEnabled,
                            onVerifyPin = mainVm::checkPinMatch,
                            observeNoteVersions = mainVm::observeNoteVersions,
                            onRunNoteAiAction = mainVm::runNoteAiAction,
                            onCreateTasksFromAi = mainVm::createTasksBatchFromAi,
                            onSaveNote = mainVm::saveKeepNote,
                            onRestoreNoteVersion = mainVm::restoreNoteVersion,
                            onBulkUpdateNotes = mainVm::bulkUpdateNotes,
                            onDeleteNoteForever = mainVm::deleteKeepNotePermanently,
                            onSaveTaskList = mainVm::saveTaskList,
                            onDeleteTaskList = mainVm::deleteTaskList,
                            onSaveTask = mainVm::saveTaskItem,
                            onToggleTaskCompleted = mainVm::toggleTaskCompleted,
                            onReorderTasks = mainVm::reorderTasks,
                            onDeleteTask = mainVm::deleteTaskItem,
                            onSaveReminder = mainVm::saveItemReminder,
                            onSnoozeReminder = mainVm::snoozeReminder,
                            onRescheduleReminder1Day = mainVm::rescheduleReminder1Day,
                            onDismissReminder = mainVm::dismissReminder,
                            onDeleteReminder = mainVm::deleteItemReminder
                        )

                        MainTab.TOOLS -> SmartToolsScreen(
                            userId = prefs.currentUserId,
                            quickTodos = quickTodos,
                            onSaveQuickTodo = mainVm::saveQuickTodo,
                            onDeleteQuickTodo = mainVm::deleteQuickTodo,
                            onOpenAiAssistant = { showAiAssistant = true },
                            onOpenGamesArena = { showGamesArena = true },
                            onSaveToNote = { noteTitle, noteContent ->
                                mainVm.saveKeepNote(
                                    KeepNoteEntity(
                                        ownerUserId = prefs.currentUserId,
                                        title = noteTitle,
                                        content = noteContent,
                                        tagsCsv = "SmartTool"
                                    )
                                )
                            },
                            onNotify = mainVm::showMessage
                        )

                        MainTab.SETTINGS -> BackupSettingsScreen(
                            prefs = prefs,
                            activeProfileName = activeProfile?.name ?: "Personal",
                            driveBusy = driveBusy,
                            biometricAvailable = biometricAvailable,
                            onConnectGmailDrive = { mainVm.connectGmailDrive(activity, strings.connectedGmail, switchAccount = false) },
                            onSwitchGmailDrive = { mainVm.connectGmailDrive(activity, strings.connectedGmail, switchAccount = true) },
                            onDisconnectGmailDrive = { mainVm.disconnectGmailDrive(strings.driveDisconnectedMsg) },
                            onBackupNowToDrive = { mainVm.backupNowToDrive(activity, strings.driveBackupSuccess, forceDirectDriveOAuth = false) },
                            onRestoreFromDrive = { mainVm.restoreFromDrive(activity, strings.driveRestoreSuccess, previewFirst = true, forceDirectDriveOAuth = false) },
                            onDirectDriveOAuthSync = { mainVm.backupNowToDrive(activity, strings.driveBackupSuccess, forceDirectDriveOAuth = true) },
                            onToggleAutoBackupOnChange = mainVm::setAutoBackupOnNewData,
                            onToggleDailyAutoBackup = mainVm::setDailyAutoBackup,
                            onExportFormat = { uri, fmt -> mainVm.exportToUri(uri, fmt, strings.exportSuccess) },
                            onRestoreJsonUri = { uri -> mainVm.restoreFromJsonUri(uri, strings.driveRestoreSuccess) },
                            onLanguageChange = mainVm::setLanguage,
                            onThemeChange = mainVm::setThemeMode,
                            onCurrencyChange = mainVm::setCurrencyCode,
                            onSetPin = mainVm::setPin,
                            onToggleBiometric = mainVm::setBiometricEnabled,
                            onNotificationSettingsChange = mainVm::setNotificationSettings,
                            onNotificationPermissionDenied = { mainVm.showMessage(strings.notificationPermissionDenied) },
                            onChangePassword = { cur, newPass ->
                                mainVm.changePassword(cur, newPass, strings.passwordUpdatedSuccess, strings.invalidCredentials)
                            },
                            onResetAllData = { secret, onResult ->
                                mainVm.verifyAndResetUserData(secret, strings.resetSuccessMsg, onResult)
                            },
                            onLogout = mainVm::logout
                        )
                    }
                }
            }
        }
    }

    if (showProfileSwitcher) {
        ProfileSwitcherSheet(
            profiles = profiles,
            activeProfileId = activeProfile?.id ?: prefs.activeProfileId,
            allUserAccounts = allUserAccounts,
            allUserTransactions = allUserTransactions,
            currencyCode = prefs.currencyCode,
            onSelectProfile = mainVm::switchProfile,
            onCreateProfile = { name, iconKey, colorHex, desc ->
                mainVm.createProfile(name, iconKey, colorHex, desc, strings.profileCreatedMsg)
            },
            onUpdateProfile = mainVm::updateProfile,
            onDeleteProfile = mainVm::deleteProfile,
            onDismiss = { showProfileSwitcher = false }
        )
    }

    if (showQuickCalculator) {
        QuickCalculatorSheet(
            activeCurrencyCode = prefs.currencyCode,
            onDismiss = { showQuickCalculator = false },
            onUseAmountForTransaction = { calcAmount, txType ->
                showQuickCalculator = false
                editingTransaction = null
                txSheetInitialAmount = calcAmount
                txSheetType = txType
                txSheetQrFocus = false
                showTxSheet = true
            }
        )
    }

    if (showTxSheet) {
        TransactionFormSheet(
            userId = prefs.currentUserId,
            profileId = activeProfile?.id ?: prefs.activeProfileId,
            initialType = txSheetType,
            initialQrFocus = txSheetQrFocus,
            editingTransaction = editingTransaction,
            initialAmount = txSheetInitialAmount,
            accounts = accounts,
            categories = categories,
            onDismiss = { showTxSheet = false },
            onSave = { tx ->
                mainVm.saveTransaction(tx)
                showTxSheet = false
            }
        )
    }
}

@Composable
private fun RestoreBackupPreviewDialog(
    preview: DriveBackupPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val snap = preview.snapshot
    val ownerName = snap.ownerUserId?.let { id -> snap.users.firstOrNull { it.id == id }?.displayName }
        ?: snap.users.lastOrNull()?.displayName
        ?: "Smart User"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.driveBackupFoundTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                preview.email?.let {
                    Text("${strings.connectedGmail}: $it", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "${strings.lastBackupLabel}: ${DateTimeUtil.formatDateTime(snap.exportedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("${strings.displayName}: $ownerName", style = MaterialTheme.typography.bodyMedium)
                Text("Profiles: ${snap.financeProfiles.size.coerceAtLeast(1)} • ${strings.accounts}: ${snap.accounts.size}", style = MaterialTheme.typography.bodyMedium)
                Text("${strings.transactions}: ${snap.transactions.size}", style = MaterialTheme.typography.bodyMedium)
                Text("${strings.keepNotesTab}: ${snap.keepNotes.size} • ${strings.goodTasksTab}: ${snap.taskItems.size}", style = MaterialTheme.typography.bodyMedium)
                Text("AI Providers: ${snap.aiProviders.size}", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_drive_restore_button")
            ) {
                Text(strings.restoreNowAndLogin)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
