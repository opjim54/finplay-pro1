package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AdminEarningEntity
import com.example.domain.DateTimeUtil
import com.example.security.SecurityAuditReport
import com.example.ui.components.LedgerCard
import java.util.Locale

private enum class EarningsTimeRange(val label: String, val windowMs: Long?) {
    TODAY("Today", 24L * 60L * 60L * 1000L),
    WEEKLY("Weekly (7d)", 7L * 24L * 60L * 60L * 1000L),
    MONTHLY("Monthly (30d)", 30L * 24L * 60L * 60L * 1000L),
    ALL_TIME("All Time", null)
}

private data class GameEarningBreakdown(
    val gameId: String,
    val totalCommission: Double,
    val totalGross: Double,
    val totalPrizes: Double,
    val matchCount: Int
)

private fun fmtTaka(value: Double): String =
    if (value % 1.0 == 0.0) "৳${value.toLong()}" else "৳${String.format(Locale.US, "%.2f", value)}"

/**
 * Automatic Admin Commission & Earnings Analytics Dashboard.
 * Aggregates commissions automatically captured from Free Fire tournaments and all in-built games
 * across Today, Weekly, Monthly, and All Time windows with per-game breakdown and live ledger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminEarningsTab(
    earnings: List<AdminEarningEntity>
) {
    var selectedRange by remember { mutableStateOf(EarningsTimeRange.ALL_TIME) }
    val nowMs = remember(earnings, selectedRange) { System.currentTimeMillis() }

    val filtered = remember(earnings, selectedRange, nowMs) {
        val win = selectedRange.windowMs
        if (win == null) earnings
        else earnings.filter { (nowMs - it.createdAt) <= win }
    }

    val totalCommission = remember(filtered) { filtered.sumOf { it.commissionAmount } }
    val totalGross = remember(filtered) { filtered.sumOf { it.grossPool } }
    val totalPrizes = remember(filtered) { filtered.sumOf { it.prizePaid } }

    val perGameBreakdown = remember(filtered) {
        filtered
            .groupBy { it.gameId.ifBlank { "OTHER" } }
            .map { (gid, rows) ->
                GameEarningBreakdown(
                    gameId = gid,
                    totalCommission = rows.sumOf { it.commissionAmount },
                    totalGross = rows.sumOf { it.grossPool },
                    totalPrizes = rows.sumOf { it.prizePaid },
                    matchCount = rows.size
                )
            }
            .sortedByDescending { it.totalCommission }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- TIME FILTER CHIPS ----
        item(key = "earnings_filter_chips") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EarningsTimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range.label, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("admin_earnings_filter_${range.name.lowercase()}")
                    )
                }
            }
        }

        // ---- OBSIDIAN-GOLD NET COMMISSION HERO ----
        item(key = "earnings_hero_card") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.3.dp, Color(0xFFF59E0B).copy(alpha = 0.72f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_earnings_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF78350F))
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Admin Net Commission (${selectedRange.label})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Auto-calculated from every Free Fire join & in-built tournament match",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }

                    Text(
                        text = fmtTaka(totalCommission),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.testTag("admin_total_commission_value")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EarningsMiniMetric("GROSS POOL", fmtTaka(totalGross), Color(0xFF38BDF8), Modifier.weight(1f))
                        EarningsMiniMetric("PRIZES PAID", fmtTaka(totalPrizes), Color(0xFF34D399), Modifier.weight(1f))
                        EarningsMiniMetric("ENTRIES", "${filtered.size}", Color(0xFFE879F9), Modifier.weight(1f))
                    }
                }
            }
        }

        // ---- PER-GAME AUTO-CALCULATED BREAKDOWN ----
        item(key = "earnings_by_game_header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Per-Game Commission Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (perGameBreakdown.isEmpty()) {
            item(key = "earnings_empty_games") {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No commissions recorded in ${selectedRange.label} yet. Whenever players join Free Fire tournaments or finish paid Ludo / Sholo Guti / Snakes & Ladders / Carrom / Chess / Fruit Slash / War Land matches, Admin commission is automatically logged here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(perGameBreakdown, key = { "game_brk_${it.gameId}" }) { row ->
                val share = if (totalCommission > 0.0) {
                    (row.totalCommission / totalCommission).toFloat().coerceIn(0f, 1f)
                } else 0f
                val accent = when (row.gameId.uppercase()) {
                    "FREE_FIRE" -> Color(0xFFEF4444)
                    "LUDO" -> Color(0xFF059669)
                    "SHOLO_GUTI" -> Color(0xFFE11D48)
                    "SNAKES_LADDERS" -> Color(0xFF10B981)
                    "CHESS" -> Color(0xFF0284C7)
                    "FRUIT_SLASH" -> Color(0xFFF59E0B)
                    "WAR_LAND" -> Color(0xFF6366F1)
                    "CARROM" -> Color(0xFFD97706)
                    else -> Color(0xFFF59E0B)
                }
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.gameId.replace("_", " "),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${row.matchCount} match(es) • Gross ${fmtTaka(row.totalGross)} • Paid ${fmtTaka(row.totalPrizes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = fmtTaka(row.totalCommission),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent
                            )
                            Text(
                                text = "${(share * 100).toInt()}% share",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { share },
                        color = accent,
                        trackColor = accent.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        // ---- RECENT COMMISSION LEDGER ----
        if (filtered.isNotEmpty()) {
            item(key = "earnings_ledger_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Recent Commission Ledger (${filtered.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(filtered.take(40), key = { "earn_led_${it.id}" }) { e ->
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${e.gameId.replace("_", " ")} • ${e.referenceTitle.ifBlank { e.sourceType }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gross ${fmtTaka(e.grossPool)} • Prize ${fmtTaka(e.prizePaid)} • ${DateTimeUtil.formatDateTime(e.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "+${fmtTaka(e.commissionAmount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsMiniMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

/**
 * Security Hardening Audit Panel + Admin Master Google Drive Auto-Save Vault Card.
 * Placed inside Admin -> System Tab.
 */
@Composable
fun AdminSecurityAndDriveVaultCard(
    securityAudit: SecurityAuditReport,
    screenShieldEnabled: Boolean,
    adminDriveEmail: String,
    adminAutoDriveSyncEnabled: Boolean,
    adminDriveSyncStatus: String,
    isDriveBusy: Boolean,
    onToggleScreenShield: (Boolean) -> Unit,
    onRefreshSecurityAudit: () -> Unit,
    onConnectAdminDrive: () -> Unit,
    onSyncAdminDriveNow: () -> Unit,
    onToggleAdminAutoDriveSync: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ---- CARD 1: APP SECURITY & ANTI-TAMPER SHIELD ----
        LedgerCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Zero-Plaintext Security Shield",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Integrity: ${securityAudit.integrityScore}% (${securityAudit.statusLabel})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (securityAudit.integrityScore >= 85) Color(0xFF059669) else Color(0xFFD97706),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onRefreshSecurityAudit) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Security Audit")
                }
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (securityAudit.integrityScore / 100f).coerceIn(0f, 1f) },
                color = if (securityAudit.integrityScore >= 85) Color(0xFF059669) else Color(0xFFF59E0B),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.height(8.dp))

            SecurityAuditLine("PBKDF2-HMAC-SHA256 (120,000 iters) Sealed Admin Key", ok = securityAudit.pbkdf2HardeningActive)
            SecurityAuditLine("Debugger / Tamper Check", ok = !securityAudit.isDebuggerAttached)
            SecurityAuditLine("Root / SU Binary Scan", ok = !securityAudit.isRootIndicatorFound)
            SecurityAuditLine(
                "Brute-Force Lockout Guard (Failed Attempts: ${securityAudit.failedAdminAttempts})",
                ok = securityAudit.lockoutRemainingSeconds == 0L
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Screen-Capture Shield (FLAG_SECURE)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Blocks screenshots & screen recordings while Admin Portal is open",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = screenShieldEnabled,
                    onCheckedChange = onToggleScreenShield,
                    modifier = Modifier.testTag("admin_screen_shield_switch")
                )
            }
        }

        // ---- CARD 2: ADMIN MASTER GOOGLE DRIVE AUTO-SAVE VAULT ----
        LedgerCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (adminDriveEmail.isNotBlank()) Icons.Default.CloudDone else Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = Color(0xFF0284C7)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Admin Master Google Drive Auto-Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (adminDriveEmail.isNotBlank()) "Connected: $adminDriveEmail"
                        else "Not connected — link Admin Google Drive to auto-backup ALL users DB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (adminDriveEmail.isNotBlank()) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Automatically saves & rewrites the complete All-Users database (Users, Wallets, Ledger, Free Fire Tournaments, Commissions, Broadcasts & Finance) to Admin's Google Drive after every critical update so no data is ever lost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (adminDriveSyncStatus.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = adminDriveSyncStatus,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Auto-Save & Rewrite on Changes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = adminAutoDriveSyncEnabled,
                    onCheckedChange = onToggleAdminAutoDriveSync,
                    modifier = Modifier.testTag("admin_auto_drive_switch")
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnectAdminDrive,
                    enabled = !isDriveBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_connect_drive_btn")
                ) {
                    if (isDriveBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(if (adminDriveEmail.isBlank()) "Connect Admin Drive" else "Switch Drive Account", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onSyncAdminDriveNow,
                    enabled = !isDriveBusy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_sync_drive_now_btn")
                ) {
                    Text("Save DB Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SecurityAuditLine(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                if (ok) Icons.Default.GppGood else Icons.Default.Shield,
                contentDescription = null,
                tint = if (ok) Color(0xFF059669) else Color(0xFFDC2626),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text = if (ok) "SECURE" else "ALERT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (ok) Color(0xFF059669) else Color(0xFFDC2626)
        )
    }
}
