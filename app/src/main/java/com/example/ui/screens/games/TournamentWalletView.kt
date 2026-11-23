package com.example.ui.screens.games

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.games.CloudWalletLedgerDoc
import com.example.data.games.CloudWalletSummaryDoc
import com.example.data.games.PlayWalletRepository
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletLedgerEntity
import com.example.domain.DateTimeUtil
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.OnlineAuthProfile
import java.util.Locale
import kotlin.math.abs

private data class UnifiedWalletTxnItem(
    val id: String,
    val walletBucket: String,
    val kindLabel: String,
    val amountDelta: Double,
    val note: String,
    val createdAtMillis: Long,
    val isCloudSynced: Boolean
)

private fun formatTakaAmount(amount: Double): String =
    if (amount % 1.0 == 0.0) "৳${amount.toLong()}" else "৳${String.format(Locale.US, "%.2f", amount)}"

/**
 * Standalone TournamentWalletView Composable that displays the user's real-time
 * Tournament Wallet balance and synced transaction history backed by Firebase Firestore
 * (/users/{uid}/wallet/primary & /users/{uid}/wallet_ledger/{entryId}), with a graceful
 * Google Sign-In prompt and seamless Room fallback when offline.
 */
@Composable
fun TournamentWalletView(
    localWallet: PlayWalletEntity,
    cloudWallet: CloudWalletSummaryDoc?,
    cloudLedger: List<CloudWalletLedgerDoc>,
    localLedger: List<PlayWalletLedgerEntity>,
    onlineAuth: OnlineAuthProfile?,
    isBn: Boolean,
    onSignInGoogle: () -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onOpenTransferToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val isLiveCloud = onlineAuth != null

    val effectiveTournamentBalance = if (isLiveCloud && cloudWallet != null) {
        maxOf(cloudWallet.tournamentBalance, localWallet.tournamentBalance)
    } else {
        localWallet.tournamentBalance
    }
    val effectiveWinningBalance = if (isLiveCloud && cloudWallet != null) {
        maxOf(cloudWallet.winningBalance, localWallet.winningBalance)
    } else {
        localWallet.winningBalance
    }
    val effectiveGameBalance = if (isLiveCloud && cloudWallet != null) {
        maxOf(cloudWallet.gameBalance, localWallet.gameBalance)
    } else {
        localWallet.gameBalance
    }

    val unifiedHistory = remember(cloudLedger, localLedger, isLiveCloud) {
        val cutoffMillis = System.currentTimeMillis() - PlayWalletRepository.WALLET_HISTORY_RETENTION_MS
        val raw = if (isLiveCloud && cloudLedger.isNotEmpty()) {
            cloudLedger.map { doc ->
                UnifiedWalletTxnItem(
                    id = "cloud_${doc.id}",
                    walletBucket = doc.walletBucket.ifBlank { "TOURNAMENT" },
                    kindLabel = doc.kind.replace("_", " "),
                    amountDelta = doc.amountDelta,
                    note = doc.note.ifBlank { doc.kind.replace("_", " ") },
                    createdAtMillis = doc.createdAtMillis,
                    isCloudSynced = true
                )
            }
        } else {
            localLedger.map { entry ->
                UnifiedWalletTxnItem(
                    id = "local_${entry.id}",
                    walletBucket = entry.walletBucket,
                    kindLabel = entry.kind.name.replace("_", " "),
                    amountDelta = entry.amountDelta,
                    note = entry.note.ifBlank { entry.kind.name.replace("_", " ") },
                    createdAtMillis = entry.createdAt,
                    isCloudSynced = false
                )
            }
        }
        raw.filter { it.createdAtMillis == 0L || it.createdAtMillis >= cutoffMillis }
    }

    val pulseAlpha by rememberInfiniteTransition(label = "firestoreSyncPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "syncDotAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_wallet_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- REAL-TIME TOURNAMENT WALLET HERO CARD ----
        Surface(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.3.dp, financeColors.champagneGold.copy(alpha = 0.72f)),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF042F2E), Color(0xFF065F46), Color(0xFF047857))
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(financeColors.champagneGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = financeColors.champagneGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBn) "টুর্নামেন্ট ওয়ালেট" else "Tournament Wallet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = if (isBn) "ডিপোজিট ও লাইভ টুর্নামেন্ট এন্ট্রি ব্যালেন্স" else "Deposit & Live Tournament Entry Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }

                    // Live Firestore Sync Status Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isLiveCloud) Color(0xFF10B981).copy(alpha = 0.24f) else Color(0xFFF59E0B).copy(alpha = 0.22f),
                        border = BorderStroke(
                            1.dp,
                            if (isLiveCloud) Color(0xFF34D399) else Color(0xFFFBBF24)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isLiveCloud) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .graphicsLayer { alpha = pulseAlpha }
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                )
                                Text(
                                    text = "LIVE SYNC",
                                    color = Color(0xFFD1FAE5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "LOCAL",
                                    color = Color(0xFFFEF3C7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Primary Balance
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatTakaAmount(effectiveTournamentBalance),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("tournament_wallet_balance_text")
                    )
                    if (isLiveCloud) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isBn) "ফায়ারস্টোর রিয়েল-টাইম সিঙ্ক: ${onlineAuth?.email}"
                                else "Real-time Firestore sync active • ${onlineAuth?.email}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }

                // Sub-bucket summary chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.22f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                            Text(
                                text = if (isBn) "🏆 উইনিং (উইথড্রযোগ্য)" else "🏆 Winning (Withdrawable)",
                                fontSize = 10.sp,
                                color = Color(0xFFFDE68A),
                                maxLines = 1
                            )
                            Text(
                                text = formatTakaAmount(effectiveWinningBalance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.22f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                            Text(
                                text = if (isBn) "🎮 গেম ওয়ালেট" else "🎮 Game Wallet",
                                fontSize = 10.sp,
                                color = Color(0xFFDDD6FE),
                                maxLines = 1
                            )
                            Text(
                                text = formatTakaAmount(effectiveGameBalance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenDeposit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = financeColors.champagneGold,
                            contentColor = Color(0xFF042F2E)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallet_deposit_btn")
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (isBn) "ডিপোজিট" else "Deposit", fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = onOpenWithdraw,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallet_withdraw_btn")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (isBn) "উইথড্র" else "Withdraw", fontWeight = FontWeight.ExtraBold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenTransferToGame,
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE0F2FE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_transfer_to_game_btn")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "টুর্নামেন্ট → গেম ওয়ালেট ট্রান্সফার (একমুখী)" else "Transfer Tournament → Game Wallet (One-Way)",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Visible Google Sign-In CTA when not yet signed in for Live Firestore Sync
                if (!isLiveCloud) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) "রিয়েল-টাইম ক্লাউড সিঙ্ক চালু করুন" else "Enable Real-Time Firestore Sync",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = if (isBn) "গুগল সাইন-ইন করলে ব্যালেন্স ও লেনদেন লাইভ সিঙ্ক হবে"
                                    else "Sign in with Google to sync balance & history live across devices",
                                    color = Color(0xFFD1FAE5),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onSignInGoogle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF042F2E)
                                ),
                                modifier = Modifier.testTag("tournament_wallet_google_signin_btn")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isBn) "সিঙ্ক" else "Sign In", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // ---- REAL-TIME TRANSACTION HISTORY PANEL ----
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, financeColors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tournament_wallet_history_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "টুর্নামেন্ট ওয়ালেট লেনদেনের ইতিহাস" else "Tournament Wallet History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isLiveCloud && cloudLedger.isNotEmpty()) Color(0xFF059669).copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isLiveCloud && cloudLedger.isNotEmpty()) "Firestore Live (${unifiedHistory.size})"
                            else "Saved (${unifiedHistory.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveCloud && cloudLedger.isNotEmpty()) Color(0xFF059669)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = if (isBn) "🗓️ সর্বোচ্চ ১ মাস (৩০ দিন) পর্যন্ত লেনদেনের ইতিহাস থাকে • পুরোনো হিস্ট্রি অটো-মুছে যায়"
                    else "🗓️ Max 1 month (30 days) history retained • Older records auto-cleaned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("wallet_retention_notice")
                )

                if (unifiedHistory.isEmpty()) {
                    Text(
                        text = if (isBn) "এখনো কোনো লেনদেন হয়নি। ডিপোজিট করুন বা টুর্নামেন্ট খেলুন — সব হিসাব এখানে রিয়েল-টাইমে দেখাবে।"
                        else "No transactions yet. Deposit coins or join a tournament — every transaction streams here in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    unifiedHistory.take(25).forEachIndexed { idx, item ->
                        if (idx > 0) HorizontalDivider(color = financeColors.cardBorder.copy(alpha = 0.5f))
                        val isPositive = item.amountDelta >= 0.0
                        val deltaColor = if (isPositive) Color(0xFF059669) else Color(0xFFDC2626)
                        val bucketColor = when (item.walletBucket.uppercase()) {
                            "WINNING" -> Color(0xFFD97706)
                            "GAME" -> Color(0xFF7C3AED)
                            else -> Color(0xFF059669)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = bucketColor.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = item.walletBucket,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = bucketColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = if (item.createdAtMillis > 0L) DateTimeUtil.formatDateTime(item.createdAtMillis) else "Just now",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${if (isPositive) "+" else "-"}${formatTakaAmount(abs(item.amountDelta))}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = deltaColor
                            )
                        }
                    }
                }
            }
        }
    }
}
