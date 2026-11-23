package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.FinancialNoteEntity
import com.example.data.local.InvestmentEntity
import com.example.data.local.LoanDebtEntity
import com.example.data.local.RecurringFrequency
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WishlistEntity
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.i18n.LocalStrings
import com.example.ui.components.IconMapper
import com.example.ui.components.LedgerCard
import com.example.ui.components.MoneyText
import com.example.ui.components.localizedCategoryName
import com.example.ui.theme.LocalFinanceColors
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

enum class WealthSection {
    ACCOUNTS, CATEGORIES, BUDGETS, SAVINGS, RECURRING, LOANS_EMI, INVESTMENTS, WISHLIST, NOTES, QR_LOG
}

@Composable
fun WealthHubScreen(
    userId: Long,
    profileId: Long = 1L,
    currencyCode: String,
    initialSection: WealthSection = WealthSection.ACCOUNTS,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    recurringRules: List<RecurringRuleEntity>,
    loans: List<LoanDebtEntity>,
    wishlist: List<WishlistEntity>,
    investments: List<InvestmentEntity>,
    notes: List<FinancialNoteEntity>,
    onSaveAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onSaveCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onSaveBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onDepositGoal: (SavingsGoalEntity, Double, Long?) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onSaveRecurring: (RecurringRuleEntity) -> Unit,
    onDeleteRecurring: (Long) -> Unit,
    onRunRecurringNow: () -> Unit,
    onSaveLoan: (LoanDebtEntity) -> Unit,
    onPayLoan: (LoanDebtEntity, Double) -> Unit,
    onDeleteLoan: (Long) -> Unit,
    onSaveWishlist: (WishlistEntity) -> Unit,
    onDeleteWishlist: (Long) -> Unit,
    onSaveInvestment: (InvestmentEntity) -> Unit,
    onDeleteInvestment: (Long) -> Unit,
    onSaveNote: (FinancialNoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var selectedSection by remember(initialSection) { mutableStateOf(initialSection) }

    val sections = listOf(
        Triple(WealthSection.ACCOUNTS, strings.accounts, Icons.Default.AccountBalanceWallet),
        Triple(WealthSection.BUDGETS, strings.budgets, Icons.Default.PieChart),
        Triple(WealthSection.SAVINGS, strings.savingsGoals, Icons.Default.Savings),
        Triple(WealthSection.CATEGORIES, strings.categories, Icons.Default.Category),
        Triple(WealthSection.RECURRING, strings.recurringTransactions, Icons.Default.Repeat),
        Triple(WealthSection.LOANS_EMI, strings.loansAndDebts, Icons.Default.CreditScore),
        Triple(WealthSection.INVESTMENTS, strings.investments, Icons.Default.TrendingUp),
        Triple(WealthSection.WISHLIST, strings.wishlist, Icons.Default.ShoppingCartCheckout),
        Triple(WealthSection.NOTES, strings.financialNotes, Icons.Default.Notes),
        Triple(WealthSection.QR_LOG, strings.qrRecords, Icons.Default.QrCode2)
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            Text(
                text = strings.wealthHub,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sections) { (sec, label, icon) ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedSection) {
                WealthSection.ACCOUNTS -> AccountsSection(
                    userId, profileId, currencyCode, accounts, onSaveAccount, onDeleteAccount
                )
                WealthSection.CATEGORIES -> CategoriesSection(
                    userId, profileId, currencyCode, categories, onSaveCategory, onDeleteCategory
                )
                WealthSection.BUDGETS -> BudgetsSection(
                    userId, profileId, currencyCode, budgets, categories, transactions, onSaveBudget, onDeleteBudget
                )
                WealthSection.SAVINGS -> SavingsSection(
                    userId, profileId, currencyCode, savingsGoals, accounts, onSaveGoal, onDepositGoal, onDeleteGoal
                )
                WealthSection.RECURRING -> RecurringSection(
                    userId, profileId, currencyCode, recurringRules, categories, accounts, onSaveRecurring, onDeleteRecurring, onRunRecurringNow
                )
                WealthSection.LOANS_EMI -> LoansAndEmiSection(
                    userId, profileId, currencyCode, loans, onSaveLoan, onPayLoan, onDeleteLoan
                )
                WealthSection.INVESTMENTS -> InvestmentsSection(
                    userId, profileId, currencyCode, investments, onSaveInvestment, onDeleteInvestment
                )
                WealthSection.WISHLIST -> WishlistSection(
                    userId, profileId, currencyCode, wishlist, onSaveWishlist, onDeleteWishlist
                )
                WealthSection.NOTES -> FinancialNotesSection(
                    userId, notes, onSaveNote, onDeleteNote
                )
                WealthSection.QR_LOG -> QrPaymentsLogSection(
                    currencyCode, transactions, accounts, categories
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsSection(
    userId: Long,
    profileId: Long,
    currencyCode: String,
    accounts: List<AccountEntity>,
    onSave: (AccountEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = {
                    editing = null
                    showForm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addAccount)
            }
        }
        items(accounts, key = { it.id }) { acc ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(acc.colorHex).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                IconMapper.iconFor(acc.iconKey),
                                contentDescription = acc.name,
                                tint = Color(acc.colorHex)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(acc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(acc.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    MoneyText(
                        amount = acc.balance,
                        currencyCode = currencyCode,
                        color = if (acc.balance < 0) financeColors.expense else financeColors.income,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = {
                        editing = acc
                        showForm = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.editAccount)
                    }
                    if (accounts.size > 1) {
                        IconButton(onClick = { onDelete(acc.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var balanceStr by remember { mutableStateOf((editing?.balance ?: 0.0).toString()) }
        var type by remember { mutableStateOf(editing?.type ?: AccountType.CASH) }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editing == null) strings.addAccount else strings.editAccount) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.accountName) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text(strings.initialBalance) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AccountType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.name) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val bal = balanceStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(
                            AccountEntity(
                                id = editing?.id ?: 0L,
                                ownerUserId = userId,
                                profileId = editing?.profileId ?: profileId,
                                name = name.trim(),
                                type = type,
                                balance = bal,
                                initialBalance = editing?.initialBalance ?: bal,
                                colorHex = editing?.colorHex ?: 0xFF047857
                            )
                        )
                        showForm = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(strings.cancel) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesSection(
    userId: Long,
    profileId: Long,
    currencyCode: String,
    categories: List<CategoryEntity>,
    onSave: (CategoryEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = {
                    editing = null
                    showForm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addCategory)
            }
        }
        items(categories, key = { it.id }) { cat ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(cat.colorHex).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            IconMapper.iconFor(cat.iconKey),
                            contentDescription = cat.name,
                            tint = Color(cat.colorHex)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(localizedCategoryName(cat, strings), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = buildString {
                                append(if (cat.type == TransactionType.INCOME) strings.income else strings.expense)
                                if (cat.monthlyBudget > 0.0) {
                                    append(" • ${strings.budgetLimit}: ${CurrencyUtil.format(cat.monthlyBudget, currencyCode)}")
                                }
                                if (cat.notes.isNotBlank()) append(" • ${cat.notes}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        editing = cat
                        showForm = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.editCategory)
                    }
                    IconButton(onClick = { onDelete(cat.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var type by remember { mutableStateOf(editing?.type ?: TransactionType.EXPENSE) }
        var iconKey by remember { mutableStateOf(editing?.iconKey ?: "category") }
        var colorHex by remember { mutableLongStateOf(editing?.colorHex ?: 0xFF047857) }
        var budgetStr by remember { mutableStateOf(editing?.monthlyBudget?.takeIf { it > 0.0 }?.toString() ?: "") }
        var notes by remember { mutableStateOf(editing?.notes ?: "") }
        val palette = listOf(0xFF047857L, 0xFFEF4444L, 0xFF3B82F6L, 0xFFEC4899L, 0xFFF59E0BL, 0xFF8B5CF6L, 0xFF14B8A6L)

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editing == null) strings.addCategory else strings.editCategory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.categoryName) }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE }, label = { Text(strings.expense) })
                        FilterChip(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME }, label = { Text(strings.income) })
                    }
                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.monthlyBudgetOptional) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(strings.note) }, singleLine = true)
                    Text(strings.customColor, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                    Text(strings.customIcon, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconMapper.availableIcons.take(10).forEach { (key, vec) ->
                            IconButton(
                                onClick = { iconKey = key },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (iconKey == key) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            ) {
                                Icon(vec, contentDescription = key, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            CategoryEntity(
                                id = editing?.id ?: 0L,
                                ownerUserId = userId,
                                profileId = editing?.profileId ?: profileId,
                                name = name.trim(),
                                localizationKey = if (editing?.name == name.trim()) editing?.localizationKey else null,
                                type = type,
                                iconKey = iconKey,
                                colorHex = colorHex,
                                monthlyBudget = budgetStr.toDoubleOrNull() ?: 0.0,
                                notes = notes.trim(),
                                isDefault = editing?.isDefault ?: false
                            )
                        )
                        showForm = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(strings.cancel) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetsSection(
    userId: Long,
    profileId: Long,
    currencyCode: String,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    onSave: (BudgetEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val currentMonthKey = DateTimeUtil.monthKey()
    val monthStart = DateTimeUtil.startOfMonth()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val monthSpentByCat = remember(transactions, monthStart) {
        transactions.filter { !it.isDeleted && it.type == TransactionType.EXPENSE && it.timestamp >= monthStart }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addBudget)
            }
        }
        items(budgets.filter { it.monthKey == currentMonthKey }, key = { it.id }) { b ->
            val used = monthSpentByCat[b.categoryId] ?: 0.0
            val rem = max(0.0, b.limitAmount - used)
            val pct = if (b.limitAmount > 0) (used / b.limitAmount).toFloat() else 0f
            val barColor = when {
                pct >= 1f -> financeColors.expense
                pct >= 0.8f -> financeColors.warning
                else -> financeColors.income
            }
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedCategoryName(catMap[b.categoryId], strings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${(pct * 100).roundToInt()}%", color = barColor, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDelete(b.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { pct.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = barColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${strings.usedBudget}: ${CurrencyUtil.format(used, currencyCode)}", style = MaterialTheme.typography.bodySmall)
                    Text("${strings.remainingBudget}: ${CurrencyUtil.format(rem, currencyCode)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                if (pct >= 0.8f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            pct >= 1f -> strings.budgetWarning100
                            pct >= 0.9f -> strings.budgetWarning90
                            else -> strings.budgetWarning80
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        val expenseCats = categories.filter { it.type == TransactionType.EXPENSE }
        var selectedCatId by remember { mutableLongStateOf(expenseCats.firstOrNull()?.id ?: 0L) }
        var limitStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(strings.addBudget) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        expenseCats.forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(localizedCategoryName(cat, strings)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.budgetLimit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val limit = limitStr.toDoubleOrNull()
                    if (limit != null && limit > 0 && selectedCatId != 0L) {
                        onSave(
                            BudgetEntity(
                                ownerUserId = userId,
                                profileId = profileId,
                                categoryId = selectedCatId,
                                monthKey = currentMonthKey,
                                limitAmount = limit
                            )
                        )
                        showForm = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(strings.cancel) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavingsSection(
    userId: Long,
    profileId: Long,
    currencyCode: String,
    goals: List<SavingsGoalEntity>,
    accounts: List<AccountEntity>,
    onSave: (SavingsGoalEntity) -> Unit,
    onDeposit: (SavingsGoalEntity, Double, Long?) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var depositGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addGoal)
            }
        }
        items(goals, key = { it.id }) { goal ->
            val remaining = max(0.0, goal.targetAmount - goal.savedAmount)
            val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 1f
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${strings.expectedDate}: ${DateTimeUtil.formatDate(goal.expectedDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(goal.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
                    color = financeColors.savings
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${strings.currentSaved}: ${CurrencyUtil.format(goal.savedAmount, currencyCode)}", style = MaterialTheme.typography.bodySmall)
                    Text("${strings.remainingAmount}: ${CurrencyUtil.format(remaining, currencyCode)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { depositGoal = goal }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.addContribution)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var targetStr by remember { mutableStateOf("") }
        var savedStr by remember { mutableStateOf("0") }
        var expectedDate by remember { mutableLongStateOf(System.currentTimeMillis() + 90L * 86_400_000L) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(strings.addGoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.goalName) }, singleLine = true)
                    OutlinedTextField(value = targetStr, onValueChange = { targetStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.targetAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = savedStr, onValueChange = { savedStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.currentSaved) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedButton(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = expectedDate }
                        DatePickerDialog(context, { _, y, m, d ->
                            c.set(y, m, d)
                            expectedDate = c.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("${strings.expectedDate}: ${DateTimeUtil.formatDate(expectedDate)}") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = targetStr.toDoubleOrNull()
                    if (name.isNotBlank() && target != null && target > 0) {
                        onSave(
                            SavingsGoalEntity(
                                ownerUserId = userId,
                                profileId = profileId,
                                name = name.trim(),
                                targetAmount = target,
                                savedAmount = savedStr.toDoubleOrNull() ?: 0.0,
                                expectedDate = expectedDate
                            )
                        )
                        showCreate = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text(strings.cancel) } }
        )
    }

    depositGoal?.let { goal ->
        var amountStr by remember { mutableStateOf("") }
        var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { depositGoal = null },
            title = { Text("${strings.addContribution}: ${goal.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.forEach { acc ->
                            FilterChip(selected = selectedAccId == acc.id, onClick = { selectedAccId = acc.id }, label = { Text(acc.name) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onDeposit(goal, amt, selectedAccId)
                        depositGoal = null
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { depositGoal = null }) { Text(strings.cancel) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringSection(
    userId: Long,
    profileId: Long,
    currencyCode: String,
    rules: List<RecurringRuleEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onSave: (RecurringRuleEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onRunNow: () -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showForm = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addRecurring)
                }
                OutlinedButton(onClick = onRunNow) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.runDueNow)
                }
            }
        }
        items(rules, key = { it.id }) { rule ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${rule.frequency.name} • ${DateTimeUtil.formatDate(rule.nextRunAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MoneyText(
                        amount = rule.amount,
                        currencyCode = currencyCode,
                        color = if (rule.type == TransactionType.INCOME) financeColors.income else financeColors.expense
                    )
                    IconButton(onClick = { onDelete(rule.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(TransactionType.EXPENSE) }
        var freq by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
        var catId by remember { mutableLongStateOf(categories.firstOrNull()?.id ?: 0L) }
        var accId by remember { mutableLongStateOf(accounts.firstOrNull()?.id ?: 0L) }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(strings.addRecurring) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (Salary, Rent, Wi-Fi...)") }, singleLine = true)
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE }, label = { Text(strings.expense) })
                        FilterChip(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME }, label = { Text(strings.income) })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecurringFrequency.entries.forEach { f ->
                            FilterChip(selected = freq == f, onClick = { freq = f }, label = { Text(f.name) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.forEach { a -> FilterChip(selected = accId == a.id, onClick = { accId = a.id }, label = { Text(a.name) }) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.filter { it.type == type }.take(6).forEach { c ->
                            FilterChip(selected = catId == c.id, onClick = { catId = c.id }, label = { Text(localizedCategoryName(c, strings)) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (title.isNotBlank() && amt != null && amt > 0 && accId != 0L && catId != 0L) {
                        onSave(
                            RecurringRuleEntity(
                                ownerUserId = userId,
                                profileId = profileId,
                                title = title.trim(),
                                type = type,
                                amount = amt,
                                categoryId = catId,
                                accountId = accId,
                                frequency = freq,
                                nextRunAt = System.currentTimeMillis()
                            )
                        )
                        showForm = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(strings.cancel) } }
        )
    }
}
