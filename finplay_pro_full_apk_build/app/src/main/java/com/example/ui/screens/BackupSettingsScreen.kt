package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferencesState
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.i18n.AppLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSettingsScreen(
    prefs: UserPreferencesState,
    activeProfileName: String = "Personal",
    driveBusy: Boolean,
    biometricAvailable: Boolean,
    onConnectGmailDrive: () -> Unit,
    onSwitchGmailDrive: () -> Unit,
    onDisconnectGmailDrive: () -> Unit,
    onBackupNowToDrive: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onDirectDriveOAuthSync: () -> Unit = {},
    onToggleAutoBackupOnChange: (Boolean) -> Unit,
    onToggleDailyAutoBackup: (Boolean) -> Unit,
    onExportFormat: (android.net.Uri, String) -> Unit,
    onRestoreJsonUri: (android.net.Uri) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSetPin: (Boolean, String) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onNotificationSettingsChange: (Boolean?, Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onNotificationPermissionDenied: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onResetAllData: (secret: String, onResult: (Boolean) -> Unit) -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showOAuthGuide by remember { mutableStateOf(false) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { onExportFormat(it, "JSON") } }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { onExportFormat(it, "CSV") } }

    val xlsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri -> uri?.let { onExportFormat(it, "XLS") } }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { onExportFormat(it, "PDF") } }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onRestoreJsonUri(it) } }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationSettingsChange(granted, null, null, null, null)
        if (!granted) {
            onNotificationPermissionDenied()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = strings.settings,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Dual Cloud Vault + Google Drive Backup & Reinstall Recovery
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                if (prefs.connectedGmail.isNotBlank()) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.googleDriveBackup,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Zero-403 Sync",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.dualCloudVaultBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${strings.connectedGmail}: ${prefs.connectedGmail.ifBlank { strings.notConnected }}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${strings.lastBackupLabel}: ${
                                    if (prefs.lastDriveBackupAt > 0L) DateTimeUtil.formatDateTime(prefs.lastDriveBackupAt)
                                    else strings.neverBackedUp
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val syncStatusLine = when {
                                driveBusy -> strings.syncingNow
                                prefs.pendingCloudSync -> strings.syncPendingOffline
                                prefs.lastDriveBackupStatus.isNotBlank() -> prefs.lastDriveBackupStatus
                                prefs.driveAuthorized || prefs.connectedGmail.isNotBlank() -> strings.syncedJustNow
                                else -> ""
                            }
                            if (syncStatusLine.isNotBlank()) {
                                Text(
                                    text = syncStatusLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (prefs.pendingCloudSync) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (prefs.connectedGmail.isBlank() && !prefs.driveAuthorized) {
                        OutlinedButton(
                            onClick = onConnectGmailDrive,
                            enabled = !driveBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_drive_gmail_button")
                        ) {
                            if (driveBusy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.connectGmailDrive)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onSwitchGmailDrive,
                                enabled = !driveBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("switch_drive_gmail_button")
                            ) {
                                Text(strings.switchGmail, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onDisconnectGmailDrive,
                                enabled = !driveBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("disconnect_drive_gmail_button")
                            ) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.disconnectGmail, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBackupNowToDrive,
                            enabled = !driveBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drive_backup_now_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.backupNowToDrive, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onRestoreFromDrive,
                            enabled = !driveBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drive_restore_now_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.restoreFromDrive, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // System Google Drive App File Picker & Saver (Zero OAuth Scope Check)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { jsonExportLauncher.launch("SmartFinance_GoogleDrive_Backup.json") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_to_drive_app_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.saveToDriveAppBtn, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { jsonImportLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_drive_app_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.restoreFromDriveAppBtn, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showOAuthGuide = !showOAuthGuide },
                            modifier = Modifier.testTag("toggle_oauth_403_guide_button")
                        ) {
                            Text(strings.oauth403GuideTitle, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = onDirectDriveOAuthSync,
                            enabled = !driveBusy
                        ) {
                            Text(strings.directDriveOAuthSync, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (showOAuthGuide) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("oauth_403_guide_card")
                        ) {
                            Text(
                                text = strings.oauth403GuideBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSwitchRow(
                        label = strings.autoBackupOnNewData,
                        checked = prefs.autoBackupOnNewData,
                        onCheckedChange = onToggleAutoBackupOnChange
                    )
                    SettingsSwitchRow(
                        label = strings.dailyAutoBackup,
                        checked = prefs.dailyAutoBackupEnabled,
                        onCheckedChange = onToggleDailyAutoBackup
                    )
                }
            }

            // Local Exports & Restore
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(strings.exportFormats, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { jsonExportLauncher.launch("SmartFinance_Backup.json") }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportJson)
                        }
                        OutlinedButton(onClick = { csvExportLauncher.launch("SmartFinance_Ledger.csv") }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportCsv)
                        }
                        OutlinedButton(onClick = { xlsExportLauncher.launch("SmartFinance_Ledger.xls") }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportExcel)
                        }
                        OutlinedButton(onClick = { pdfExportLauncher.launch("SmartFinance_Report.pdf") }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportPdf)
                        }
                        OutlinedButton(onClick = { jsonImportLauncher.launch(arrayOf("application/json", "*/*")) }) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.restoreLocalJson)
                        }
                    }
                }
            }

            // Language, Theme & Currency
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text(lang.flagLabel) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.themeMode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.LIGHT to strings.themeLight,
                            ThemeMode.DARK to strings.themeDark,
                            ThemeMode.SYSTEM to strings.themeSystem
                        ).forEach { (mode, lbl) ->
                            FilterChip(selected = prefs.themeMode == mode, onClick = { onThemeChange(mode) }, label = { Text(lbl) })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(strings.currencyLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CurrencyUtil.supportedCurrencies.forEach { cur ->
                            FilterChip(
                                selected = prefs.currencyCode == cur.code,
                                onClick = { onCurrencyChange(cur.code) },
                                label = { Text("${cur.symbol} ${cur.code}") }
                            )
                        }
                    }
                }
            }

            // Security & App Lock
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.securityLock, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSwitchRow(
                        label = strings.pinLock,
                        checked = prefs.pinEnabled,
                        onCheckedChange = { enable ->
                            if (enable) showPinDialog = true else onSetPin(false, "")
                        }
                    )
                    if (prefs.pinEnabled) {
                        TextButton(onClick = { showPinDialog = true }) {
                            Text(strings.setPin)
                        }
                    }
                    SettingsSwitchRow(
                        label = if (biometricAvailable) strings.biometricUnlock else "${strings.biometricUnlock} (${strings.biometricNotAvailable})",
                        checked = prefs.biometricEnabled,
                        enabled = biometricAvailable,
                        onCheckedChange = onToggleBiometric
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.changePassword)
                    }
                }
            }

            // Notifications Preferences
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.notifications, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSwitchRow(
                        label = strings.notifications,
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onNotificationSettingsChange(enable, null, null, null, null)
                            }
                        }
                    )
                    SettingsSwitchRow(
                        label = strings.budgetWarningsToggle,
                        checked = prefs.budgetWarningsEnabled,
                        enabled = prefs.notificationsEnabled,
                        onCheckedChange = { onNotificationSettingsChange(null, it, null, null, null) }
                    )
                    SettingsSwitchRow(
                        label = strings.savingsRemindersToggle,
                        checked = prefs.savingsRemindersEnabled,
                        enabled = prefs.notificationsEnabled,
                        onCheckedChange = { onNotificationSettingsChange(null, null, it, null, null) }
                    )
                    SettingsSwitchRow(
                        label = strings.monthlyReportReminderToggle,
                        checked = prefs.monthlyReportReminderEnabled,
                        enabled = prefs.notificationsEnabled,
                        onCheckedChange = { onNotificationSettingsChange(null, null, null, it, null) }
                    )
                    SettingsSwitchRow(
                        label = strings.billRemindersToggle,
                        checked = prefs.billRemindersEnabled,
                        enabled = prefs.notificationsEnabled,
                        onCheckedChange = { onNotificationSettingsChange(null, null, null, null, it) }
                    )
                }
            }

            // Developer / Creator Profile Section (MD Jim)
            item {
                DeveloperProfileSettingsCard(language = prefs.language)
            }

            // Reset & Logout
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_all_data_button")
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset: $activeProfileName", maxLines = 1)
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.logout)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showPinDialog) {
        var pinText by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.setPin) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            if (it.length <= 6) {
                                pinText = it.filter(Char::isDigit)
                                pinError = null
                            }
                        },
                        label = { Text(strings.enter4DigitPin) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError != null,
                        singleLine = true
                    )
                    pinError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinText.length in 4..6) {
                        onSetPin(true, pinText)
                        showPinDialog = false
                    } else {
                        pinError = strings.pinLengthError
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showPasswordDialog) {
        var cur by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(strings.changePassword) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cur,
                        onValueChange = { cur = it; passError = null },
                        label = { Text(strings.currentPassword) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passError != null,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; passError = null },
                        label = { Text(strings.newPassword) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passError != null,
                        singleLine = true
                    )
                    passError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (cur.isNotBlank() && newPass.isNotBlank()) {
                        onChangePassword(cur, newPass)
                        showPasswordDialog = false
                    } else {
                        passError = strings.passwordEmptyError
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showResetDialog) {
        var secretInput by remember { mutableStateOf("") }
        var resetError by remember { mutableStateOf<String?>(null) }
        var resetBusy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!resetBusy) showResetDialog = false },
            title = { Text("Reset \"$activeProfileName\" Data", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Only the active profile \"$activeProfileName\" will be reset (balances zeroed & its transactions/budgets/goals/loans cleared). All other profiles, Keep Notes, Tasks & AI Providers stay completely safe, and Google Drive / Cloud Vault will immediately update.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = {
                            secretInput = it
                            resetError = null
                        },
                        label = { Text(strings.resetVerifyLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = resetError != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_secret_input")
                    )
                    resetError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("reset_secret_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !resetBusy && secretInput.isNotBlank(),
                    onClick = {
                        resetBusy = true
                        resetError = null
                        onResetAllData(secretInput) { ok ->
                            resetBusy = false
                            if (ok) {
                                showResetDialog = false
                            } else {
                                resetError = strings.resetVerifyError
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    if (resetBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text(strings.resetConfirmAction)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !resetBusy,
                    onClick = { showResetDialog = false }
                ) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
