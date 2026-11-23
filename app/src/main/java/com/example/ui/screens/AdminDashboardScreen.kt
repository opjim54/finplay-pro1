package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import com.example.data.local.TransactionType
import com.example.data.local.UserEntity
import com.example.data.local.WalletRequestStatus
import com.example.data.local.WalletRequestType
import com.example.domain.DateTimeUtil
import com.example.domain.games.PlayPaymentConfig
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.security.SecurityAuditReport
import com.example.security.SecurityShield
import com.example.ui.components.LedgerCard
import com.example.ui.components.MoneyText
import com.example.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    users: List<UserEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    configs: List<AppConfigEntity>,
    playWalletRequests: List<PlayWalletRequestEntity> = emptyList(),
    playPaymentConfig: PlayPaymentConfig = PlayPaymentConfig(),
    allPlayWallets: List<PlayWalletEntity> = emptyList(),
    tournamentTiers: List<TournamentTierConfigEntity> = emptyList(),
    gameBotConfigs: List<GameBotConfigEntity> = emptyList(),
    externalTournaments: List<ExternalTournamentEntity> = emptyList(),
    broadcastNotifications: List<BroadcastNotificationEntity> = emptyList(),
    adminEarnings: List<AdminEarningEntity> = emptyList(),
    securityAudit: SecurityAuditReport = SecurityShield.runIntegrityAudit(),
    screenShieldEnabled: Boolean = true,
    adminDriveEmail: String = "",
    adminAutoDriveSyncEnabled: Boolean = true,
    adminDriveSyncStatus: String = "",
    isDriveBusy: Boolean = false,
    currencyCode: String,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onAddUser: (String, String, String) -> Unit,
    onToggleLockUser: (UserEntity) -> Unit,
    onResetUserPassword: (UserEntity, String) -> Unit,
    onDeleteUser: (Long) -> Unit,
    onAdjustUserWallet: (Long, Double, Double, Double, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteTransaction: (Long) -> Unit,
    onEditTransactionAmount: (TransactionEntity, Double, String) -> Unit,
    onExportAll: (android.net.Uri, String) -> Unit,
    onRestoreEntireDb: (android.net.Uri) -> Unit,
    onClearTrash: () -> Unit,
    onUpdateConfig: (String, String) -> Unit,
    onUpdateSecretAdminCredentials: (String, String) -> Unit = { _, _ -> },
    onUpdateAdminPortalKey: (String) -> Unit = {},
    onApproveDeposit: (Long) -> Unit = {},
    onRejectDeposit: (Long) -> Unit = {},
    onMarkWithdrawalPaid: (Long) -> Unit = {},
    onRejectWithdrawalRefund: (Long) -> Unit = {},
    onSavePlayPaymentConfig: (PlayPaymentConfig) -> Unit = {},
    onSaveTournamentTier: (TournamentTierConfigEntity) -> Unit = {},
    onDeleteTournamentTier: (Long) -> Unit = {},
    onSaveGameBotConfig: (GameBotConfigEntity) -> Unit = {},
    onCreateExternalTournament: (ExternalTournamentEntity) -> Unit = {},
    onUpdateExternalTournament: (ExternalTournamentEntity) -> Unit = {},
    onSendRoomAlert: (Long) -> Unit = {},
    onCompleteExternalTournament: (Long, String, Double) -> Unit = { _, _, _ -> },
    onDeleteExternalTournament: (Long) -> Unit = {},
    onSendBroadcast: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteBroadcast: (Long) -> Unit = {},
    onToggleScreenShield: (Boolean) -> Unit = {},
    onRefreshSecurityAudit: () -> Unit = {},
    onConnectAdminDrive: () -> Unit = {},
    onSyncAdminDriveNow: () -> Unit = {},
    onToggleAdminAutoDriveSync: (Boolean) -> Unit = {},
    onExitAdmin: () -> Unit
) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { onExportAll(it, "JSON") }
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { onExportAll(it, "CSV") }
    }
    val pdfExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { onExportAll(it, "PDF") }
    }
    val jsonRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onRestoreEntireDb)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.adminDashboard, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onExitAdmin,
                        modifier = Modifier.testTag("admin_exit_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.exitAdmin)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                val pendingPlayCount = playWalletRequests.count { it.status == WalletRequestStatus.PENDING }
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.adminOverview, maxLines = 1) })
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Earnings", maxLines = 1) },
                        modifier = Modifier.testTag("admin_tab_earnings")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = if (pendingPlayCount > 0) "Finance ($pendingPlayCount)" else "Finance",
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("admin_tab_play_finance")
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Tournaments & Bots", maxLines = 1) },
                        modifier = Modifier.testTag("admin_tab_tournaments")
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Users & Coins", maxLines = 1) },
                        modifier = Modifier.testTag("admin_tab_users_coins")
                    )
                    Tab(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        text = { Text("Notify All", maxLines = 1) },
                        modifier = Modifier.testTag("admin_tab_broadcast")
                    )
                    Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text(strings.adminAllTransactions, maxLines = 1) })
                    Tab(selected = selectedTab == 7, onClick = { selectedTab = 7 }, text = { Text(strings.adminSystemConfig, maxLines = 1) })
                }

                when (selectedTab) {
                    0 -> AdminOverviewTab(users, transactions, categories, adminEarnings, currencyCode)
                    1 -> AdminEarningsTab(earnings = adminEarnings)
                    2 -> AdminPlayFinanceTab(
                        requests = playWalletRequests,
                        paymentConfig = playPaymentConfig,
                        onApproveDeposit = onApproveDeposit,
                        onRejectDeposit = onRejectDeposit,
                        onMarkWithdrawalPaid = onMarkWithdrawalPaid,
                        onRejectWithdrawalRefund = onRejectWithdrawalRefund,
                        onSavePaymentConfig = onSavePlayPaymentConfig
                    )
                    3 -> AdminTournamentsAndBotsTab(
                        tiers = tournamentTiers,
                        botConfigs = gameBotConfigs,
                        externalTournaments = externalTournaments,
                        onSaveTier = onSaveTournamentTier,
                        onDeleteTier = onDeleteTournamentTier,
                        onSaveBotConfig = onSaveGameBotConfig,
                        onCreateExternalTournament = onCreateExternalTournament,
                        onUpdateExternalTournament = onUpdateExternalTournament,
                        onSendRoomAlert = onSendRoomAlert,
                        onCompleteExternalTournament = onCompleteExternalTournament,
                        onDeleteExternalTournament = onDeleteExternalTournament
                    )
                    4 -> AdminUsersWithCoinsTab(
                        users = users,
                        wallets = allPlayWallets,
                        onAddUser = onAddUser,
                        onToggleLockUser = onToggleLockUser,
                        onResetUserPassword = onResetUserPassword,
                        onDeleteUser = onDeleteUser,
                        onAdjustUserWallet = onAdjustUserWallet
                    )
                    5 -> AdminBroadcastNotificationsTab(
                        broadcasts = broadcastNotifications,
                        onSendBroadcast = onSendBroadcast,
                        onDeleteBroadcast = onDeleteBroadcast
                    )
                    6 -> AdminTransactionsTab(transactions, users, currencyCode, onEditTransactionAmount, onDeleteTransaction)
                    7 -> AdminSystemTab(
                        configs = configs,
                        securityAudit = securityAudit,
                        screenShieldEnabled = screenShieldEnabled,
                        adminDriveEmail = adminDriveEmail,
                        adminAutoDriveSyncEnabled = adminAutoDriveSyncEnabled,
                        adminDriveSyncStatus = adminDriveSyncStatus,
                        isDriveBusy = isDriveBusy,
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                        onBackupDb = { jsonExport.launch("FinPlayPro_FullDB_Backup.json") },
                        onRestoreDb = { jsonRestore.launch(arrayOf("application/json", "*/*")) },
                        onExportCsv = { csvExport.launch("FinPlayPro_AllUsers.csv") },
                        onExportPdf = { pdfExport.launch("FinPlayPro_SystemReport.pdf") },
                        onClearTrash = onClearTrash,
                        onUpdateConfig = onUpdateConfig,
                        onUpdateSecretAdminCredentials = onUpdateSecretAdminCredentials,
                        onUpdateAdminPortalKey = onUpdateAdminPortalKey,
                        onToggleScreenShield = onToggleScreenShield,
                        onRefreshSecurityAudit = onRefreshSecurityAudit,
                        onConnectAdminDrive = onConnectAdminDrive,
                        onSyncAdminDriveNow = onSyncAdminDriveNow,
                        onToggleAdminAutoDriveSync = onToggleAdminAutoDriveSync
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminPlayFinanceTab(
    requests: List<PlayWalletRequestEntity>,
    paymentConfig: PlayPaymentConfig,
    onApproveDeposit: (Long) -> Unit,
    onRejectDeposit: (Long) -> Unit,
    onMarkWithdrawalPaid: (Long) -> Unit,
    onRejectWithdrawalRefund: (Long) -> Unit,
    onSavePaymentConfig: (PlayPaymentConfig) -> Unit
) {
    var bkash by remember(paymentConfig.bkashNumber) { mutableStateOf(paymentConfig.bkashNumber) }
    var nagad by remember(paymentConfig.nagadNumber) { mutableStateOf(paymentConfig.nagadNumber) }
    var rocket by remember(paymentConfig.rocketNumber) { mutableStateOf(paymentConfig.rocketNumber) }
    var binancePayId by remember(paymentConfig.binancePayId) { mutableStateOf(paymentConfig.binancePayId) }
    var binanceTrc20 by remember(paymentConfig.binanceTrc20Address) { mutableStateOf(paymentConfig.binanceTrc20Address) }
    var binanceRate by remember(paymentConfig.binanceRateBdt) { mutableStateOf(paymentConfig.binanceRateBdt.toInt().toString()) }
    var minDep by remember(paymentConfig.minDeposit) { mutableStateOf(paymentConfig.minDeposit.toInt().toString()) }
    var minWdr by remember(paymentConfig.minWithdraw) { mutableStateOf(paymentConfig.minWithdraw.toInt().toString()) }

    var bkashDepOn by remember(paymentConfig.bkashDepositEnabled) { mutableStateOf(paymentConfig.bkashDepositEnabled) }
    var nagadDepOn by remember(paymentConfig.nagadDepositEnabled) { mutableStateOf(paymentConfig.nagadDepositEnabled) }
    var rocketDepOn by remember(paymentConfig.rocketDepositEnabled) { mutableStateOf(paymentConfig.rocketDepositEnabled) }
    var binanceDepOn by remember(paymentConfig.binanceDepositEnabled) { mutableStateOf(paymentConfig.binanceDepositEnabled) }
    var bkashWdrOn by remember(paymentConfig.bkashWithdrawEnabled) { mutableStateOf(paymentConfig.bkashWithdrawEnabled) }
    var nagadWdrOn by remember(paymentConfig.nagadWithdrawEnabled) { mutableStateOf(paymentConfig.nagadWithdrawEnabled) }
    var rocketWdrOn by remember(paymentConfig.rocketWithdrawEnabled) { mutableStateOf(paymentConfig.rocketWithdrawEnabled) }
    var binanceWdrOn by remember(paymentConfig.binanceWithdrawEnabled) { mutableStateOf(paymentConfig.binanceWithdrawEnabled) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "admin_payment_numbers") {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Text("Payment Accounts & Binance USDT Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = bkash, onValueChange = { bkash = it }, label = { Text("bKash Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = nagad, onValueChange = { nagad = it }, label = { Text("Nagad Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = rocket, onValueChange = { rocket = it }, label = { Text("Rocket Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = binancePayId, onValueChange = { binancePayId = it }, label = { Text("Binance Pay ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = binanceTrc20, onValueChange = { binanceTrc20 = it }, label = { Text("Binance USDT (TRC20) Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = binanceRate, onValueChange = { binanceRate = it.filter(Char::isDigit) }, label = { Text("1 USDT = ৳") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minDep, onValueChange = { minDep = it.filter(Char::isDigit) }, label = { Text("Min Deposit") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minWdr, onValueChange = { minWdr = it.filter(Char::isDigit) }, label = { Text("Min Withdraw") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Deposit & Withdraw Method On/Off Control", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Turning a method OFF immediately hides it from the user's Deposit or Withdraw screen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                PaymentMethodToggleRow("bKash", bkashDepOn, { bkashDepOn = it }, bkashWdrOn, { bkashWdrOn = it })
                PaymentMethodToggleRow("Nagad", nagadDepOn, { nagadDepOn = it }, nagadWdrOn, { nagadWdrOn = it })
                PaymentMethodToggleRow("Rocket", rocketDepOn, { rocketDepOn = it }, rocketWdrOn, { rocketWdrOn = it })
                PaymentMethodToggleRow("Binance", binanceDepOn, { binanceDepOn = it }, binanceWdrOn, { binanceWdrOn = it })

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSavePaymentConfig(
                            PlayPaymentConfig(
                                bkashNumber = bkash.trim(),
                                nagadNumber = nagad.trim(),
                                rocketNumber = rocket.trim(),
                                binancePayId = binancePayId.trim(),
                                binanceTrc20Address = binanceTrc20.trim(),
                                binanceRateBdt = (binanceRate.toDoubleOrNull() ?: 120.0).coerceAtLeast(1.0),
                                minDeposit = minDep.toDoubleOrNull() ?: 20.0,
                                minWithdraw = minWdr.toDoubleOrNull() ?: 50.0,
                                bkashDepositEnabled = bkashDepOn,
                                nagadDepositEnabled = nagadDepOn,
                                rocketDepositEnabled = rocketDepOn,
                                binanceDepositEnabled = binanceDepOn,
                                bkashWithdrawEnabled = bkashWdrOn,
                                nagadWithdrawEnabled = nagadWdrOn,
                                rocketWithdrawEnabled = rocketWdrOn,
                                binanceWithdrawEnabled = binanceWdrOn
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_payment_numbers_btn")
                ) {
                    Text("Save Payment Accounts & Method Toggles", fontWeight = FontWeight.Bold)
                }
            }
        }

        item(key = "admin_requests_header") {
            Text(
                text = "Deposit & Withdrawal Queue (${requests.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (requests.isEmpty()) {
            item(key = "admin_no_requests") {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No deposit or withdrawal requests yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(requests, key = { "admin_req_${it.id}" }) { req ->
            val statusColor = when (req.status) {
                WalletRequestStatus.PENDING -> Color(0xFFD97706)
                WalletRequestStatus.APPROVED, WalletRequestStatus.PAID -> Color(0xFF059669)
                WalletRequestStatus.REJECTED -> Color(0xFFDC2626)
            }
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${req.usernameSnapshot} • ${req.requestType.name} • ${req.channel.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.16f)) {
                            Text(
                                text = req.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Amount: ৳${req.amount.toInt()} • Number: ${req.accountNumber}" +
                            if (req.transactionId.isNotBlank()) " • TrxID: ${req.transactionId}" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(DateTimeUtil.formatDateTime(req.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (req.status == WalletRequestStatus.PENDING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (req.requestType == WalletRequestType.DEPOSIT) {
                                Button(
                                    onClick = { onApproveDeposit(req.id) },
                                    modifier = Modifier.weight(1f).testTag("admin_approve_deposit_${req.id}")
                                ) { Text("Approve & Credit") }
                                OutlinedButton(
                                    onClick = { onRejectDeposit(req.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject") }
                            } else {
                                Button(
                                    onClick = { onMarkWithdrawalPaid(req.id) },
                                    modifier = Modifier.weight(1f).testTag("admin_mark_paid_${req.id}")
                                ) { Text("Mark Paid") }
                                OutlinedButton(
                                    onClick = { onRejectWithdrawalRefund(req.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject & Refund") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodToggleRow(
    label: String,
    depositEnabled: Boolean,
    onDepositToggle: (Boolean) -> Unit,
    withdrawEnabled: Boolean,
    onWithdrawToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(74.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Dep", style = MaterialTheme.typography.labelSmall)
            Switch(
                checked = depositEnabled,
                onCheckedChange = onDepositToggle,
                modifier = Modifier.testTag("admin_toggle_dep_${label.lowercase()}")
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Wdr", style = MaterialTheme.typography.labelSmall)
            Switch(
                checked = withdrawEnabled,
                onCheckedChange = onWithdrawToggle,
                modifier = Modifier.testTag("admin_toggle_wdr_${label.lowercase()}")
            )
        }
    }
}

@Composable
private fun AdminOverviewTab(
    users: List<UserEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    adminEarnings: List<AdminEarningEntity>,
    currencyCode: String
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val activeTx = transactions.filter { !it.isDeleted }
    val totalInc = activeTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExp = activeTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalAdminCommission = adminEarnings.sumOf { it.commissionAmount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LedgerCard(modifier = Modifier.weight(1f)) {
                    Text(strings.totalUsers, style = MaterialTheme.typography.labelMedium)
                    Text(users.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                LedgerCard(modifier = Modifier.weight(1f)) {
                    Text(strings.activeUsers, style = MaterialTheme.typography.labelMedium)
                    Text(users.count { !it.isLocked }.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = financeColors.income)
                }
                LedgerCard(modifier = Modifier.weight(1f)) {
                    Text(strings.lockedUsers, style = MaterialTheme.typography.labelMedium)
                    Text(users.count { it.isLocked }.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = financeColors.expense)
                }
            }
        }
        item {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Text("Admin Game Commission (All Time)", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "৳${totalAdminCommission.toLong()} (${adminEarnings.size} auto-logged entries)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD97706)
                )
            }
        }
        item {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.monthlyReport, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${strings.systemVolume}: ${activeTx.size} (${categories.size} ${strings.categories})")
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.totalIncome)
                    MoneyText(amount = totalInc, currencyCode = currencyCode, color = financeColors.income)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.totalExpense)
                    MoneyText(amount = totalExp, currencyCode = currencyCode, color = financeColors.expense)
                }
            }
        }
    }
}

@Composable
private fun AdminTransactionsTab(
    transactions: List<TransactionEntity>,
    users: List<UserEntity>,
    currencyCode: String,
    onEditTx: (TransactionEntity, Double, String) -> Unit,
    onDeleteTx: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val userMap = remember(users) { users.associateBy { it.id } }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(transactions, key = { it.id }) { tx ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("@${userMap[tx.ownerUserId]?.username ?: tx.ownerUserId} • ${tx.type.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${DateTimeUtil.formatDateTime(tx.timestamp)} • ${tx.note}", style = MaterialTheme.typography.bodySmall)
                    }
                    MoneyText(amount = tx.amount, currencyCode = currencyCode)
                    IconButton(onClick = { editingTx = tx }) { Text("Edit", style = MaterialTheme.typography.labelSmall) }
                    IconButton(onClick = { onDeleteTx(tx.id) }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    editingTx?.let { tx ->
        var amtStr by remember { mutableStateOf(tx.amount.toString()) }
        var note by remember { mutableStateOf(tx.note) }
        AlertDialog(
            onDismissRequest = { editingTx = null },
            title = { Text(strings.editTransaction) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amtStr, onValueChange = { amtStr = it }, label = { Text(strings.amount) }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(strings.note) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amtStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onEditTx(tx, amt, note)
                        editingTx = null
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { editingTx = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun AdminSystemTab(
    configs: List<AppConfigEntity>,
    securityAudit: SecurityAuditReport,
    screenShieldEnabled: Boolean,
    adminDriveEmail: String,
    adminAutoDriveSyncEnabled: Boolean,
    adminDriveSyncStatus: String,
    isDriveBusy: Boolean,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBackupDb: () -> Unit,
    onRestoreDb: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onClearTrash: () -> Unit,
    onUpdateConfig: (String, String) -> Unit,
    onUpdateSecretAdminCredentials: (String, String) -> Unit,
    onUpdateAdminPortalKey: (String) -> Unit,
    onToggleScreenShield: (Boolean) -> Unit,
    onRefreshSecurityAudit: () -> Unit,
    onConnectAdminDrive: () -> Unit,
    onSyncAdminDriveNow: () -> Unit,
    onToggleAdminAutoDriveSync: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "admin_security_and_drive") {
            AdminSecurityAndDriveVaultCard(
                securityAudit = securityAudit,
                screenShieldEnabled = screenShieldEnabled,
                adminDriveEmail = adminDriveEmail,
                adminAutoDriveSyncEnabled = adminAutoDriveSyncEnabled,
                adminDriveSyncStatus = adminDriveSyncStatus,
                isDriveBusy = isDriveBusy,
                onToggleScreenShield = onToggleScreenShield,
                onRefreshSecurityAudit = onRefreshSecurityAudit,
                onConnectAdminDrive = onConnectAdminDrive,
                onSyncAdminDriveNow = onSyncAdminDriveNow,
                onToggleAdminAutoDriveSync = onToggleAdminAutoDriveSync
            )
        }
        item {
            AdminSecretLoginConfigCard(
                onUpdateSecretCredentials = onUpdateSecretAdminCredentials,
                onUpdateAdminPortalKey = onUpdateAdminPortalKey
            )
        }
        item {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.databaseMaintenance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onBackupDb, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.backupEntireDb)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onRestoreDb, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.restoreEntireDb)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f)) { Text("${strings.exportAllData} CSV") }
                    OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) { Text("${strings.exportAllData} PDF") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClearTrash,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.clearAllTrash)
                }
            }
        }
        item {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = currentLanguage == lang,
                            onClick = {
                                onLanguageChange(lang)
                                onUpdateConfig("default_language", lang.code)
                            },
                            label = { Text(lang.flagLabel) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(strings.currencyLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BDT", "USD", "INR", "EUR").forEach { code ->
                        val active = configs.firstOrNull { it.key == "default_currency" }?.value == code
                        FilterChip(
                            selected = active,
                            onClick = { onUpdateConfig("default_currency", code) },
                            label = { Text(code) }
                        )
                    }
                }
            }
        }
    }
}
