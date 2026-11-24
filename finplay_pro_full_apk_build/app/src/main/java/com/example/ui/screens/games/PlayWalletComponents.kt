package com.example.ui.screens.games

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.games.CloudWalletLedgerDoc
import com.example.data.games.CloudWalletSummaryDoc
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletLedgerEntity
import com.example.data.local.PlayWalletRequestEntity
import com.example.data.local.WalletPaymentChannel
import com.example.data.local.WalletRequestStatus
import com.example.data.local.WalletRequestType
import com.example.domain.DateTimeUtil
import com.example.domain.games.PlayArmoryCatalog
import com.example.domain.games.PlayPaymentConfig
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.OnlineAuthProfile
import java.util.Locale

fun openTelegramSupportChat(context: Context) {
    val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PlayArmoryCatalog.TELEGRAM_APP_URI)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(tgIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PlayArmoryCatalog.TELEGRAM_WEB_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(webIntent) }
            .onFailure {
                Toast.makeText(context, "Telegram: @${PlayArmoryCatalog.TELEGRAM_SUPPORT_USERNAME}", Toast.LENGTH_LONG).show()
            }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
}

private fun formatCoins(amount: Double): String =
    if (amount % 1.0 == 0.0) "৳${amount.toLong()}" else "৳${String.format(Locale.US, "%.2f", amount)}"

@Composable
fun DualWalletCompactStrip(
    wallet: PlayWalletEntity,
    gStrings: GameUiStrings,
    onOpenWalletsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, financeColors.champagneGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenWalletsTab)
                .testTag("compact_tournament_wallet_card")
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF047857))))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (gStrings.isBn) "ডিপোজিট" else "Deposit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA7F3D0),
                    maxLines = 1
                )
                Text(
                    text = formatCoins(wallet.tournamentBalance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.7f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenWalletsTab)
                .testTag("compact_winning_wallet_card")
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Color(0xFF78350F), Color(0xFFD97706))))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (gStrings.isBn) "🏆 উইনিং" else "🏆 Winning",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFEF3C7),
                    maxLines = 1
                )
                Text(
                    text = formatCoins(wallet.winningBalance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenWalletsTab)
                .testTag("compact_game_wallet_card")
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF5B21B6))))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (gStrings.isBn) "🎮 গেম" else "🎮 Game",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFDDD6FE),
                    maxLines = 1
                )
                Text(
                    text = formatCoins(wallet.gameBalance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PlayWalletsTabContent(
    wallet: PlayWalletEntity,
    requests: List<PlayWalletRequestEntity>,
    ledger: List<PlayWalletLedgerEntity>,
    paymentConfig: PlayPaymentConfig,
    gStrings: GameUiStrings,
    cloudWallet: CloudWalletSummaryDoc? = null,
    cloudLedger: List<CloudWalletLedgerDoc> = emptyList(),
    onlineAuth: OnlineAuthProfile? = null,
    onSignInGoogle: () -> Unit = {},
    onSubmitDeposit: (WalletPaymentChannel, Double, String, String, () -> Unit) -> Unit,
    onSubmitWithdraw: (WalletPaymentChannel, Double, String, () -> Unit) -> Unit,
    onTransferToGame: (Double, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val financeColors = LocalFinanceColors.current
    var showDepositSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    if (showDepositSheet) {
        DepositModalSheet(
            paymentConfig = paymentConfig,
            gStrings = gStrings,
            onDismiss = { showDepositSheet = false },
            onSubmit = { ch, amt, phone, trx ->
                onSubmitDeposit(ch, amt, phone, trx) { showDepositSheet = false }
            }
        )
    }

    if (showWithdrawSheet) {
        WithdrawModalSheet(
            wallet = wallet,
            paymentConfig = paymentConfig,
            gStrings = gStrings,
            onDismiss = { showWithdrawSheet = false },
            onSubmit = { ch, amt, phone ->
                onSubmitWithdraw(ch, amt, phone) { showWithdrawSheet = false }
            }
        )
    }

    if (showTransferDialog) {
        OneWayTransferDialog(
            tournamentBalance = wallet.tournamentBalance,
            gStrings = gStrings,
            onDismiss = { showTransferDialog = false },
            onConfirm = { amt ->
                onTransferToGame(amt) { showTransferDialog = false }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Standalone Real-Time Firestore-Synced Tournament Wallet + Transaction History View
        item(key = "tournament_wallet_hero") {
            TournamentWalletView(
                localWallet = wallet,
                cloudWallet = cloudWallet,
                cloudLedger = cloudLedger,
                localLedger = ledger,
                onlineAuth = onlineAuth,
                isBn = gStrings.isBn,
                onSignInGoogle = onSignInGoogle,
                onOpenDeposit = { showDepositSheet = true },
                onOpenWithdraw = { showWithdrawSheet = true },
                onOpenTransferToGame = { showTransferDialog = true }
            )
        }

        // 2. Game Wallet Card (100% Cashback + Locked Reverse Badge)
        item(key = "game_wallet_hero") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.2.dp, Color(0xFF818CF8).copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF5B21B6))))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFFFDE68A))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = gStrings.gameWalletTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = gStrings.gameWalletHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDD6FE)
                                )
                            }
                        }
                    }

                    Text(
                        text = formatCoins(wallet.gameBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                            Text(
                                text = gStrings.lossCashbackBanner,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE0E7FF)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEF4444).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFECACA), modifier = Modifier.size(13.dp))
                            Text(
                                text = gStrings.reverseTransferLocked,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFECACA)
                            )
                        }
                    }
                }
            }
        }

        // 2B. Winning Coins Wallet Card (STRICTLY Withdrawable Balance)
        item(key = "winning_wallet_hero") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF78350F), Color(0xFFB45309), Color(0xFFD97706))))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (gStrings.isBn) "🏆 উইনিং কয়েন ব্যালেন্স (শুধুমাত্র উইথড্রযোগ্য)" else "🏆 Winning Coins Balance (Withdrawable ONLY)",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = formatCoins(wallet.winningBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (gStrings.isBn) "যেকোনো টুর্নামেন্ট বা Free Fire ম্যাচ জিতলে প্রাইজ এখানে জমা হয় এবং শুধুমাত্র এই ব্যালেন্স থেকেই উইথড্র করা যাবে।"
                        else "Tournament & Free Fire victory prizes are credited here. Withdrawals draw strictly from Winning Coins with instant Admin alert.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFEF3C7)
                    )
                    Button(
                        onClick = { showWithdrawSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF78350F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("winning_wallet_withdraw_btn")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (gStrings.isBn) "উইনিং ব্যালেন্স উইথড্র করুন" else "Withdraw Winning Coins",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 3. Telegram Live Support Card (@jim151)
        item(key = "telegram_support_card") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { openTelegramSupportChat(context) }
                    .testTag("wallet_live_chat_telegram_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = gStrings.liveChatLabel, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${gStrings.liveChatLabel} (Telegram @${PlayArmoryCatalog.TELEGRAM_SUPPORT_USERNAME})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (gStrings.isBn) "ডিপোজিট, উইথড্র বা যেকোনো সাহায্যের জন্য সরাসরি মেসেজ করুন"
                            else "Message Admin directly on Telegram for instant deposit/withdraw support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF0284C7))
                }
            }
        }

        // 4. Deposit & Withdrawal Requests Status
        if (requests.isNotEmpty()) {
            item(key = "requests_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (gStrings.isBn) "ডিপোজিট ও উইথড্র রিকোয়েস্ট" else "Deposit & Withdrawal Requests",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (gStrings.isBn) "সর্বোচ্চ ১ মাসের (৩০ দিন) ডিপোজিট/উইথড্র হিস্ট্রি দেখানো হয়"
                        else "Showing up to 1 month (30 days) of deposit & withdrawal history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(requests, key = { "req_${it.id}" }) { req ->
                val badgeColor = when (req.status) {
                    WalletRequestStatus.PENDING -> Color(0xFFD97706)
                    WalletRequestStatus.APPROVED, WalletRequestStatus.PAID -> Color(0xFF059669)
                    WalletRequestStatus.REJECTED -> Color(0xFFDC2626)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, financeColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${req.requestType.name} • ${req.channel.name} • ${formatCoins(req.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append(req.accountNumber)
                                    if (req.transactionId.isNotBlank()) append(" • Trx: ${req.transactionId}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (req.adminNote.isNotBlank()) {
                                Text(
                                    text = "Admin: ${req.adminNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor
                                )
                            }
                            Text(
                                text = DateTimeUtil.formatDateTime(req.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = req.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Wallet Ledger History
        if (ledger.isNotEmpty()) {
            item(key = "ledger_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (gStrings.isBn) "ওয়ালেট লেনদেনের ইতিহাস" else "Wallet Activity Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (gStrings.isBn) "গত ৩০ দিনের তাজা লেনদেন (১ মাসের পুরোনো অটো-রিমুভ হয়)"
                        else "Fresh last-30-days activity (older than 1 month auto-removed)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(ledger.take(30), key = { "led_${it.id}" }) { entry ->
                val isPositive = entry.amountDelta >= 0.0
                val deltaColor = if (isPositive) Color(0xFF059669) else Color(0xFFDC2626)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.note.ifBlank { entry.kind.name },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${entry.walletBucket} • ${DateTimeUtil.formatDateTime(entry.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${if (isPositive) "+" else "-"}${formatCoins(kotlin.math.abs(entry.amountDelta))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = deltaColor
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositModalSheet(
    paymentConfig: PlayPaymentConfig,
    gStrings: GameUiStrings,
    onDismiss: () -> Unit,
    onSubmit: (WalletPaymentChannel, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    val enabledChannels = remember(paymentConfig) { paymentConfig.enabledDepositChannels() }
    var channel by remember(enabledChannels) {
        mutableStateOf(enabledChannels.firstOrNull() ?: WalletPaymentChannel.BKASH)
    }
    var amountText by remember { mutableStateOf("100") }
    var senderNumber by remember { mutableStateOf("") }
    var trxId by remember { mutableStateOf("") }

    val receiverNumber = when (channel) {
        WalletPaymentChannel.BKASH -> paymentConfig.bkashNumber
        WalletPaymentChannel.NAGAD -> paymentConfig.nagadNumber
        WalletPaymentChannel.ROCKET -> paymentConfig.rocketNumber
        WalletPaymentChannel.BINANCE -> paymentConfig.binancePayId
    }
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val usdtEquivalent = if (paymentConfig.binanceRateBdt > 0.0) parsedAmount / paymentConfig.binanceRateBdt else 0.0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (gStrings.isBn) "টুর্নামেন্ট ওয়ালেট ডিপোজিট" else "Deposit to Tournament Wallet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (enabledChannels.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (gStrings.isBn) "অ্যাডমিন সাময়িকভাবে সব ডিপোজিট মেথড বন্ধ রেখেছেন। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                        else "All deposit methods are temporarily paused by Admin. Please try again shortly.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                return@ModalBottomSheet
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                enabledChannels.forEach { ch ->
                    FilterChip(
                        selected = channel == ch,
                        onClick = { channel = ch },
                        label = { Text(if (ch == WalletPaymentChannel.BINANCE) "Binance" else ch.name, maxLines = 1) },
                        modifier = Modifier.testTag("deposit_channel_${ch.name.lowercase()}")
                    )
                }
            }

            if (channel == WalletPaymentChannel.BINANCE) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (gStrings.isBn) "অফিশিয়াল Binance Pay ID" else "Official Binance Pay ID",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = paymentConfig.binancePayId,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            IconButton(onClick = { copyToClipboard(context, "Binance Pay ID", paymentConfig.binancePayId) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Binance Pay ID")
                            }
                        }
                        if (paymentConfig.binanceTrc20Address.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "USDT (TRC20) Address",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = paymentConfig.binanceTrc20Address,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { copyToClipboard(context, "USDT TRC20", paymentConfig.binanceTrc20Address) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy TRC20 Address")
                                }
                            }
                        }
                        Text(
                            text = "Rate: 1 USDT = ৳${paymentConfig.binanceRateBdt.toInt()}" +
                                if (parsedAmount > 0.0) "  •  Send ≈ ${String.format(Locale.US, "%.2f", usdtEquivalent)} USDT" else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (gStrings.isBn) "${channel.name} সেন্ড মানি নম্বর" else "Official ${channel.name} Send Money Number",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = receiverNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        IconButton(onClick = { copyToClipboard(context, channel.name, receiverNumber) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy number")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("${if (gStrings.isBn) "পরিমাণ ৳ (সর্বনিম্ন" else "Amount in ৳ (Min"} ৳${paymentConfig.minDeposit.toInt()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input")
            )
            OutlinedTextField(
                value = senderNumber,
                onValueChange = { senderNumber = it },
                label = {
                    Text(
                        if (channel == WalletPaymentChannel.BINANCE) {
                            if (gStrings.isBn) "আপনার Binance Pay ID / TRC20 অ্যাড্রেস" else "Your Binance Pay ID / TRC20 Address"
                        } else {
                            if (gStrings.isBn) "আপনার ${channel.name} নম্বর" else "Your Sender ${channel.name} Number"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (channel == WalletPaymentChannel.BINANCE) KeyboardType.Text else KeyboardType.Phone
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_sender_input")
            )
            OutlinedTextField(
                value = trxId,
                onValueChange = { trxId = it },
                label = {
                    Text(
                        if (channel == WalletPaymentChannel.BINANCE) {
                            if (gStrings.isBn) "Binance Order ID / TxHash" else "Binance Order ID / TxHash"
                        } else {
                            if (gStrings.isBn) "ট্রানজেকশন আইডি (TrxID)" else "Transaction ID (TrxID)"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("deposit_trx_input")
            )

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(channel, amt, senderNumber, trxId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("deposit_submit_btn")
            ) {
                Text(if (gStrings.isBn) "ডিপোজিট রিকোয়েস্ট পাঠান" else "Submit Deposit Request", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawModalSheet(
    wallet: PlayWalletEntity,
    paymentConfig: PlayPaymentConfig,
    gStrings: GameUiStrings,
    onDismiss: () -> Unit,
    onSubmit: (WalletPaymentChannel, Double, String) -> Unit
) {
    val enabledChannels = remember(paymentConfig) { paymentConfig.enabledWithdrawChannels() }
    var channel by remember(enabledChannels) {
        mutableStateOf(enabledChannels.firstOrNull() ?: WalletPaymentChannel.BKASH)
    }
    var amountText by remember { mutableStateOf("") }
    var receiverNumber by remember { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val usdtPayout = if (paymentConfig.binanceRateBdt > 0.0) parsedAmount / paymentConfig.binanceRateBdt else 0.0

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (gStrings.isBn) "🏆 উইনিং কয়েন উইথড্র" else "🏆 Withdraw Winning Coins",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (gStrings.isBn) "উইথড্রযোগ্য উইনিং ব্যালেন্স: ${formatCoins(wallet.winningBalance)} (শুধুমাত্র জিতে পাওয়া কয়েন উইথড্র করা যাবে — ডিপোজিট বা গেম ওয়ালেট নয়)"
                else "Withdrawable Winning Balance: ${formatCoins(wallet.winningBalance)} (ONLY Winning Coins can be withdrawn — Deposit & Game Wallet coins cannot be withdrawn)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (enabledChannels.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (gStrings.isBn) "অ্যাডমিন সাময়িকভাবে সব উইথড্র মেথড বন্ধ রেখেছেন। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                        else "All withdrawal methods are temporarily paused by Admin. Please check back soon.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                return@ModalBottomSheet
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                enabledChannels.forEach { ch ->
                    FilterChip(
                        selected = channel == ch,
                        onClick = { channel = ch },
                        label = { Text(if (ch == WalletPaymentChannel.BINANCE) "Binance" else ch.name, maxLines = 1) },
                        modifier = Modifier.testTag("withdraw_channel_${ch.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("${if (gStrings.isBn) "উইথড্র পরিমাণ ৳ (সর্বনিম্ন" else "Withdraw Amount ৳ (Min"} ৳${paymentConfig.minWithdraw.toInt()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
            )
            if (channel == WalletPaymentChannel.BINANCE) {
                Text(
                    text = "Binance USDT Payout ≈ ${String.format(Locale.US, "%.2f", usdtPayout)} USDT (Rate: ৳${paymentConfig.binanceRateBdt.toInt()}/USDT)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309)
                )
            }
            OutlinedTextField(
                value = receiverNumber,
                onValueChange = { receiverNumber = it },
                label = {
                    Text(
                        if (channel == WalletPaymentChannel.BINANCE) {
                            if (gStrings.isBn) "আপনার রিসিভার Binance Pay ID বা USDT TRC20" else "Your Binance Pay ID or USDT TRC20 Address"
                        } else {
                            if (gStrings.isBn) "রিসিভার ${channel.name} নম্বর" else "Your Receiver ${channel.name} Number"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (channel == WalletPaymentChannel.BINANCE) KeyboardType.Text else KeyboardType.Phone
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("withdraw_receiver_input")
            )

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(channel, amt, receiverNumber)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("withdraw_submit_btn")
            ) {
                Text(if (gStrings.isBn) "উইথড্র রিকোয়েস্ট পাঠান" else "Submit Withdrawal Request", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OneWayTransferDialog(
    tournamentBalance: Double,
    gStrings: GameUiStrings,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amtText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(gStrings.transferToGameLabel, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(gStrings.oneWayTransferNote, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = if (gStrings.isBn) "উপলব্ধ টুর্নামেন্ট ব্যালেন্স: ${formatCoins(tournamentBalance)}\nসতর্কতা: গেম ওয়ালেট থেকে টুর্নামেন্ট ওয়ালেটে ফেরত নেওয়া যাবে না।"
                    else "Available Tournament Wallet: ${formatCoins(tournamentBalance)}\nNote: This transfer is permanent and cannot be reversed to Tournament Wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amtText,
                    onValueChange = { amtText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (gStrings.isBn) "ট্রান্সফার পরিমাণ" else "Transfer Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amtText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(amt)
                },
                modifier = Modifier.testTag("transfer_confirm_btn")
            ) {
                Text(if (gStrings.isBn) "ট্রান্সফার করুন" else "Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (gStrings.isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
