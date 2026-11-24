package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.FinanceProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserEntity
import com.example.data.local.UserPreferencesState
import com.example.domain.CurrencyUtil
import com.example.domain.DashboardMetrics
import com.example.domain.DateTimeUtil
import com.example.domain.DeepAnalyticsSummary
import com.example.domain.UpcomingObligation
import com.example.i18n.LocalStrings
import com.example.ui.components.AnimatedMoneyText
import com.example.ui.components.BouncySurface
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FinancialHealthGaugeCard
import com.example.ui.components.Interactive30DayCashflowChart
import com.example.ui.components.InteractiveBarComparisonChart
import com.example.ui.components.InteractiveLineAreaChart
import com.example.ui.components.InteractivePieDonutChart
import com.example.ui.components.LedgerCard
import com.example.ui.components.LivePulseDot
import com.example.ui.components.MoneyText
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LocalFinanceColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class DashboardTimeframe {
    LIFETIME, YEAR, MONTH, TODAY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    user: UserEntity?,
    activeProfile: FinanceProfileEntity?,
    prefs: UserPreferencesState,
    metrics: DashboardMetrics,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    recentTransactions: List<TransactionEntity>,
    unreadNotifications: Int,
    deepAnalytics: DeepAnalyticsSummary? = null,
    allProfilesNetWorth: Double = metrics.totalBalance,
    profileCount: Int = 1,
    playWallet: com.example.data.local.PlayWalletEntity? = null,
    onOpenProfileSwitcher: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenGamesArena: () -> Unit = {},
    onOpenAnalytics: () -> Unit,
    onOpenWealthHub: () -> Unit,
    onQuickAdd: (TransactionType, Boolean) -> Unit,
    onOpenQuickCalculator: () -> Unit,
    onPayObligation: (UpcomingObligation) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDuplicateTransaction: (TransactionEntity) -> Unit,
    onToggleFavorite: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenDriveBackup: () -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenBudgets: () -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    var chartTab by remember { mutableIntStateOf(0) } // 0=30-Day Cashflow, 1=Bar, 2=Pie, 3=Area
    var selectedTimeframe by remember { mutableStateOf(DashboardTimeframe.LIFETIME) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header with Switch Profile Pill & Notifications
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.displayName ?: strings.appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenDriveBackup)
                                .testTag("dashboard_drive_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (prefs.connectedGmail.isNotBlank() || prefs.driveAuthorized) {
                                    LivePulseDot(
                                        color = if (prefs.pendingCloudSync) financeColors.warning else financeColors.income,
                                        sizeDp = 7.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = strings.googleDriveBackup,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                val pillText = when {
                                    prefs.connectedGmail.isNotBlank() && prefs.pendingCloudSync -> "${prefs.connectedGmail} • Sync..."
                                    prefs.connectedGmail.isNotBlank() -> "${prefs.connectedGmail} • Cloud Vault"
                                    prefs.driveAuthorized -> strings.dualCloudVaultBadge
                                    else -> strings.connectGmailDrive
                                }
                                Text(
                                    text = pillText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val pColor = Color(activeProfile?.colorHex ?: 0xFF047857L)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = pColor.copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, pColor.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenProfileSwitcher)
                                .testTag("switch_profile_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = profileIconForKey(activeProfile?.iconKey ?: "person"),
                                    contentDescription = strings.switchProfile,
                                    tint = pColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeProfile?.name ?: "Personal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = pColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = pColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenGamesArena,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_header_games_button")
                        ) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = strings.gamesArenaTitle,
                                tint = financeColors.champagneGold
                            )
                        }

                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge { Text(unreadNotifications.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = strings.notifications
                                )
                            }
                        }
                    }
                }
            }

            // Hero Total Balance Card with Savings Rate & Net Flow Pills
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd)))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_emerald_hero_1794656207945),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.22f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.totalBalance,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFD1FAE5)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${strings.savingsRateLabel}: ${metrics.monthlySavingsRate.roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = prefs.currencyCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        AnimatedMoneyText(
                            amount = metrics.totalBalance,
                            currencyCode = prefs.currencyCode,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Today's Net Flow Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${strings.todayNetFlowLabel}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = CurrencyUtil.format(metrics.todayNetFlow, prefs.currencyCode, showSign = true),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (metrics.todayNetFlow >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                                )
                            }
                        }

                        // Timeframe Switcher (Lifetime permanent default + This Year + This Month + Today)
                        val (tfIncome, tfExpense, tfSavings) = when (selectedTimeframe) {
                            DashboardTimeframe.LIFETIME -> Triple(metrics.totalIncome, metrics.totalExpense, metrics.totalSavings)
                            DashboardTimeframe.YEAR -> Triple(metrics.yearlyIncome, metrics.yearlyExpense, metrics.yearlySavings)
                            DashboardTimeframe.MONTH -> Triple(metrics.monthlyIncome, metrics.monthlyExpense, metrics.monthlySavings)
                            DashboardTimeframe.TODAY -> Triple(metrics.todayIncome, metrics.todayExpense, max(0.0, metrics.todayNetFlow))
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.testTag("dashboard_timeframe_bar")
                        ) {
                            val items = listOf(
                                DashboardTimeframe.LIFETIME to strings.timeframeLifetime,
                                DashboardTimeframe.YEAR to strings.timeframeThisYear,
                                DashboardTimeframe.MONTH to strings.timeframeThisMonth,
                                DashboardTimeframe.TODAY to strings.timeframeToday
                            )
                            items(items) { (tf, label) ->
                                val isSel = selectedTimeframe == tf
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedTimeframe = tf },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFDE68A),
                                        selectedLabelColor = Color(0xFF06231C),
                                        containerColor = Color.Black.copy(alpha = 0.24f),
                                        labelColor = Color.White.copy(alpha = 0.9f)
                                    ),
                                    border = null,
                                    modifier = Modifier.testTag("timeframe_chip_${tf.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroSubStat(
                                label = if (selectedTimeframe == DashboardTimeframe.LIFETIME) strings.totalIncome else strings.income,
                                amount = tfIncome,
                                currencyCode = prefs.currencyCode,
                                accentColor = Color(0xFF6EE7B7),
                                modifier = Modifier.weight(1f)
                            )
                            HeroSubStat(
                                label = if (selectedTimeframe == DashboardTimeframe.LIFETIME) strings.totalExpense else strings.expense,
                                amount = tfExpense,
                                currencyCode = prefs.currencyCode,
                                accentColor = Color(0xFFFCA5A5),
                                modifier = Modifier.weight(1f)
                            )
                            HeroSubStat(
                                label = if (selectedTimeframe == DashboardTimeframe.LIFETIME) strings.totalSavings else strings.catSavings,
                                amount = tfSavings,
                                currencyCode = prefs.currencyCode,
                                accentColor = Color(0xFFFDE68A),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "∞ ${strings.lifetimeSavedHint}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD1FAE5).copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 5 Quick Actions: +Income, -Expense, Transfer, QR Pay, Calculator/Converter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Default.ArrowDownward,
                        label = strings.addIncome,
                        color = financeColors.income,
                        container = financeColors.incomeContainer,
                        onClick = { onQuickAdd(TransactionType.INCOME, false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_income")
                    )
                    QuickActionTile(
                        icon = Icons.Default.ArrowUpward,
                        label = strings.addExpense,
                        color = financeColors.expense,
                        container = financeColors.expenseContainer,
                        onClick = { onQuickAdd(TransactionType.EXPENSE, false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_expense")
                    )
                    QuickActionTile(
                        icon = Icons.AutoMirrored.Filled.CompareArrows,
                        label = strings.transfer,
                        color = financeColors.transfer,
                        container = financeColors.transferContainer,
                        onClick = { onQuickAdd(TransactionType.TRANSFER, false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_transfer")
                    )
                    QuickActionTile(
                        icon = Icons.Default.QrCode2,
                        label = strings.qrPayment,
                        color = financeColors.savings,
                        container = financeColors.savingsContainer,
                        onClick = { onQuickAdd(TransactionType.EXPENSE, true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_qr")
                    )
                    QuickActionTile(
                        icon = Icons.Default.Calculate,
                        label = strings.quickCalculator,
                        color = MaterialTheme.colorScheme.primary,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onOpenQuickCalculator,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_open_calculator")
                    )
                }
            }

            // Feature Hub Shortcuts: Chat with AI, Games Arena, Deep Analytics & Wealth Hub
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BouncySurface(
                            onClick = onOpenAiAssistant,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF064E3B),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("dashboard_open_ai_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.chatWithAi,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        BouncySurface(
                            onClick = onOpenAnalytics,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_open_analytics_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.navAnalytics,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        BouncySurface(
                            onClick = onOpenWealthHub,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_open_wealth_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.navWealth,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    BouncySurface(
                        onClick = onOpenGamesArena,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF132A23),
                        border = androidx.compose.foundation.BorderStroke(1.dp, financeColors.champagneGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_open_games_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(financeColors.champagneGold.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = financeColors.champagneGold, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.gamesArenaTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = strings.gamesArenaSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (playWallet != null) {
                                    Text(
                                        text = "🏆 ৳${playWallet.tournamentBalance.toInt()}  •  🎮 ৳${playWallet.gameBalance.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = financeColors.champagneGold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = financeColors.champagneGold
                            ) {
                                Text(
                                    text = strings.playGames,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF06231C),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Financial Health Score Gauge & Combined All-Profiles Net Worth
            if (deepAnalytics != null) {
                item {
                    FinancialHealthGaugeCard(
                        score = deepAnalytics.financialHealthScore,
                        allProfilesNetWorth = allProfilesNetWorth,
                        profileCount = profileCount.coerceAtLeast(1),
                        currencyCode = prefs.currencyCode,
                        insightText = deepAnalytics.insights.firstOrNull() ?: strings.startAddingTip,
                        titleLabel = strings.financialHealthScore,
                        netWorthLabel = strings.allProfilesNetWorthLabel,
                        askAiLabel = strings.askAiCfoLabel,
                        onAskAi = onOpenAiAssistant,
                        onOpenAnalytics = onOpenAnalytics,
                        modifier = Modifier.testTag("dashboard_health_gauge_card")
                    )
                }
            }

            // Smart Bill, Recurring & EMI Due Countdown Strip
            if (metrics.upcomingObligations.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EventRepeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.upcomingBillsAndEmi,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(metrics.upcomingObligations, key = { "${it.kind}_${it.id}" }) { ob ->
                                UpcomingObligationCard(
                                    obligation = ob,
                                    currencyCode = prefs.currencyCode,
                                    onPayOrPost = { onPayObligation(ob) }
                                )
                            }
                        }
                    }
                }
            }

            // Today & Monthly Snapshot Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMiniCard(
                            label = strings.todaysIncome,
                            amount = metrics.todayIncome,
                            currencyCode = prefs.currencyCode,
                            accent = financeColors.income,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            label = strings.todaysExpense,
                            amount = metrics.todayExpense,
                            currencyCode = prefs.currencyCode,
                            accent = financeColors.expense,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMiniCard(
                            label = strings.monthlyIncome,
                            amount = metrics.monthlyIncome,
                            currencyCode = prefs.currencyCode,
                            accent = financeColors.income,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            label = strings.monthlyExpense,
                            amount = metrics.monthlyExpense,
                            currencyCode = prefs.currencyCode,
                            accent = financeColors.expense,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            label = strings.monthlySavings,
                            amount = metrics.monthlySavings,
                            currencyCode = prefs.currencyCode,
                            accent = financeColors.savings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Current Budget Status & High/Low Categories
            item {
                LedgerCard(onClick = onOpenBudgets, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.currentBudgetStatus,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val pct = (metrics.budgetProgress * 100f).roundToInt()
                        Text(
                            text = if (metrics.totalBudgetLimit > 0.0) "$pct%" else strings.addBudget,
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                pct >= 100 -> financeColors.expense
                                pct >= 80 -> financeColors.warning
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (metrics.totalBudgetLimit > 0.0) {
                        val barColor = when {
                            metrics.budgetProgress >= 1f -> financeColors.expense
                            metrics.budgetProgress >= 0.8f -> financeColors.warning
                            else -> MaterialTheme.colorScheme.primary
                        }
                        LinearProgressIndicator(
                            progress = { metrics.budgetProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.usedBudget}: ${CurrencyUtil.format(metrics.totalBudgetUsed, prefs.currencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val rem = max(0.0, metrics.totalBudgetLimit - metrics.totalBudgetUsed)
                            Text(
                                text = "${strings.remainingBudget}: ${CurrencyUtil.format(rem, prefs.currencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "${strings.monthlyExpense}: ${CurrencyUtil.format(metrics.monthlyExpense, prefs.currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (metrics.highestSpendingCategory != null || metrics.lowestSpendingCategory != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            metrics.highestSpendingCategory?.let { high ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = financeColors.expenseContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = strings.highestSpendingCategory,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${high.name} • ${CurrencyUtil.formatCompact(high.amount, prefs.currencyCode)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            metrics.lowestSpendingCategory?.let { low ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = financeColors.incomeContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = strings.lowestSpendingCategory,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${low.name} • ${CurrencyUtil.formatCompact(low.amount, prefs.currencyCode)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accounts Strip
            if (accounts.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(accounts, key = { it.id }) { acc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, financeColors.cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(136.dp)
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(acc.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MoneyText(
                                        amount = acc.balance,
                                        currencyCode = prefs.currencyCode,
                                        fontSize = 15.sp,
                                        color = if (acc.balance < 0) financeColors.expense else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Overview & Interactive 30-Day Cashflow + Category Charts
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.monthlyOverview,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = chartTab == 0,
                            onClick = { chartTab = 0 },
                            label = { Text(strings.chart30DayCashflow, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("chart_tab_30day")
                        )
                        FilterChip(
                            selected = chartTab == 1,
                            onClick = { chartTab = 1 },
                            label = { Text(strings.barChart, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("chart_tab_bar")
                        )
                        FilterChip(
                            selected = chartTab == 2,
                            onClick = { chartTab = 2 },
                            label = { Text(strings.pieChart, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("chart_tab_pie")
                        )
                        FilterChip(
                            selected = chartTab == 3,
                            onClick = { chartTab = 3 },
                            label = { Text(strings.areaChart, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("chart_tab_area")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedVisibility(visible = true, enter = fadeIn()) {
                        when (chartTab) {
                            0 -> Interactive30DayCashflowChart(
                                points = metrics.last30DaysTrend,
                                currencyCode = prefs.currencyCode
                            )
                            1 -> InteractiveBarComparisonChart(
                                points = metrics.last6MonthsTrend,
                                currencyCode = prefs.currencyCode
                            )
                            2 -> InteractivePieDonutChart(
                                items = metrics.monthlyCategoryBreakdowns,
                                currencyCode = prefs.currencyCode
                            )
                            else -> InteractiveLineAreaChart(
                                points = metrics.last6MonthsTrend,
                                currencyCode = prefs.currencyCode,
                                showArea = true
                            )
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentTransactions,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onOpenAllTransactions) {
                        Text(strings.viewAll)
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = strings.noDataYet,
                        subtitle = strings.startAddingTip
                    )
                }
            } else {
                items(recentTransactions.take(6), key = { it.id }) { tx ->
                    TransactionItemCard(
                        tx = tx,
                        category = catMap[tx.categoryId],
                        account = accMap[tx.accountId],
                        targetAccount = tx.targetAccountId?.let { accMap[it] },
                        currencyCode = prefs.currencyCode,
                        onClick = { onEditTransaction(tx) },
                        onEdit = { onEditTransaction(tx) },
                        onDuplicate = { onDuplicateTransaction(tx) },
                        onToggleFavorite = { onToggleFavorite(tx) },
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun UpcomingObligationCard(
    obligation: UpcomingObligation,
    currencyCode: String,
    onPayOrPost: () -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val badgeColor = when {
        obligation.daysUntilDue < 0 -> financeColors.expense
        obligation.daysUntilDue <= 3 -> financeColors.warning
        else -> MaterialTheme.colorScheme.primary
    }
    val badgeText = when {
        obligation.daysUntilDue < 0 -> "${strings.overdueBadge} (${abs(obligation.daysUntilDue)}${strings.daysSuffix})"
        obligation.daysUntilDue == 0 -> strings.dueTodayBadge
        else -> "${strings.dueInDaysPrefix} ${obligation.daysUntilDue}${strings.daysSuffix}"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
        modifier = Modifier.width(220.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = DateTimeUtil.formatDate(obligation.dueTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = obligation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = obligation.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoneyText(
                    amount = obligation.amount,
                    currencyCode = currencyCode,
                    fontSize = 15.sp,
                    color = Color(obligation.colorHex)
                )
                FilledTonalButton(
                    onClick = onPayOrPost,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.payOrPostNow, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun HeroSubStat(
    label: String,
    amount: Double,
    currencyCode: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.22f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            MoneyText(
                amount = amount,
                currencyCode = currencyCode,
                color = accentColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    color: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BouncySurface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(19.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryMiniCard(
    label: String,
    amount: Double,
    currencyCode: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    LedgerCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        MoneyText(
            amount = amount,
            currencyCode = currencyCode,
            color = accent,
            fontSize = 15.sp
        )
    }
}
