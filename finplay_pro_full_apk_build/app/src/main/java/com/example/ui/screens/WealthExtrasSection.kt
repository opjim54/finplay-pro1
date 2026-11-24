package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.FinancialNoteEntity
import com.example.data.local.InvestmentEntity
import com.example.data.local.LoanDebtEntity
import com.example.data.local.LoanKind
import com.example.data.local.TransactionEntity
import com.example.data.local.WishlistEntity
import com.example.data.local.WishlistPriority
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.i18n.LocalStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LedgerCard
import com.example.ui.components.MoneyText
import com.example.ui.components.localizedCategoryName
import com.example.ui.theme.LocalFinanceColors
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoansAndEmiSection(
    userId: Long,
    profileId: Long = 1L,
    currencyCode: String,
    loans: List<LoanDebtEntity>,
    onSave: (LoanDebtEntity) -> Unit,
    onPay: (LoanDebtEntity, Double) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addRecord)
            }
        }
        items(loans, key = { it.id }) { loan ->
            val remaining = max(0.0, loan.principalAmount - loan.paidAmount)
            val pct = if (loan.principalAmount > 0) (loan.paidAmount / loan.principalAmount).toFloat().coerceIn(0f, 1f) else 1f
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${loan.counterparty} (${loan.kind.name})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${strings.principalAmount}: ${CurrencyUtil.format(loan.principalAmount, currencyCode)} • ${strings.interestRate}: ${loan.interestRate}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (loan.kind == LoanKind.EMI) {
                            Text(
                                "${strings.monthlyEmi}: ${CurrencyUtil.format(loan.monthlyEmi, currencyCode)} (${loan.paidMonths}/${loan.totalMonths})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { onDelete(loan.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (loan.kind == LoanKind.LOAN_GIVEN) financeColors.income else financeColors.warning
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${strings.remainingAmount}: ${CurrencyUtil.format(remaining, currencyCode)}", style = MaterialTheme.typography.bodySmall)
                    if (remaining > 0.0) {
                        OutlinedButton(onClick = {
                            val payAmt = if (loan.monthlyEmi > 0.0) loan.monthlyEmi else remaining
                            onPay(loan, payAmt)
                        }) { Text(strings.markPaid) }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var kind by remember { mutableStateOf(LoanKind.DEBT_OWED) }
        var counterparty by remember { mutableStateOf("") }
        var principalStr by remember { mutableStateOf("") }
        var interestStr by remember { mutableStateOf("0") }
        var monthsStr by remember { mutableStateOf("12") }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(strings.loansAndDebts) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LoanKind.entries.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.name) }) }
                    }
                    OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text(strings.counterparty) }, singleLine = true)
                    OutlinedTextField(value = principalStr, onValueChange = { principalStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.principalAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = interestStr, onValueChange = { interestStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.interestRate) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    if (kind == LoanKind.EMI) {
                        OutlinedTextField(value = monthsStr, onValueChange = { monthsStr = it.filter { c -> c.isDigit() } }, label = { Text("Total Months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = principalStr.toDoubleOrNull()
                    val months = (monthsStr.toIntOrNull() ?: 12).coerceAtLeast(1)
                    val rate = interestStr.toDoubleOrNull() ?: 0.0
                    if (counterparty.isNotBlank() && p != null && p > 0) {
                        val totalWithInterest = p * (1.0 + rate / 100.0)
                        onSave(
                            LoanDebtEntity(
                                ownerUserId = userId,
                                profileId = profileId,
                                kind = kind,
                                counterparty = counterparty.trim(),
                                principalAmount = totalWithInterest,
                                interestRate = rate,
                                monthlyEmi = if (kind == LoanKind.EMI) totalWithInterest / months else 0.0,
                                totalMonths = months,
                                dueDate = System.currentTimeMillis() + 30L * 86_400_000L
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

@Composable
fun InvestmentsSection(
    userId: Long,
    profileId: Long = 1L,
    currencyCode: String,
    investments: List<InvestmentEntity>,
    onSave: (InvestmentEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addRecord)
            }
        }
        items(investments, key = { it.id }) { inv ->
            val profit = inv.currentValue - inv.investedAmount
            val roi = if (inv.investedAmount > 0) ((profit / inv.investedAmount) * 100.0).roundToInt() else 0
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${inv.assetName} (${inv.assetType})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${strings.investedAmount}: ${CurrencyUtil.format(inv.investedAmount, currencyCode)} → ${
                                CurrencyUtil.format(inv.currentValue, currencyCode)
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = if (roi >= 0) "+$roi%" else "$roi%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (profit >= 0) financeColors.income else financeColors.expense
                    )
                    IconButton(onClick = { onDelete(inv.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("DPS / Stock") }
        var investedStr by remember { mutableStateOf("") }
        var currentStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(strings.investments) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.assetName) }, singleLine = true)
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Asset Type") }, singleLine = true)
                    OutlinedTextField(value = investedStr, onValueChange = { investedStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.investedAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = currentStr, onValueChange = { currentStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.currentValue) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val invAmt = investedStr.toDoubleOrNull()
                    val curAmt = currentStr.toDoubleOrNull()
                    if (name.isNotBlank() && invAmt != null && curAmt != null) {
                        onSave(
                            InvestmentEntity(
                                ownerUserId = userId,
                                profileId = profileId,
                                assetName = name.trim(),
                                assetType = type.trim(),
                                investedAmount = invAmt,
                                currentValue = curAmt
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
fun WishlistSection(
    userId: Long,
    profileId: Long = 1L,
    currencyCode: String,
    itemsList: List<WishlistEntity>,
    onSave: (WishlistEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addRecord)
            }
        }
        items(itemsList, key = { it.id }) { item ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.isPurchased,
                        onCheckedChange = { onSave(item.copy(isPurchased = it)) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.itemName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${item.priority.name} • ${CurrencyUtil.format(item.targetPrice, currencyCode)}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(item.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var name by remember { mutableStateOf("") }
        var priceStr by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf(WishlistPriority.MEDIUM) }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(strings.wishlist) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.itemName) }, singleLine = true)
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(strings.targetAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WishlistPriority.entries.forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name) }) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = priceStr.toDoubleOrNull()
                    if (name.isNotBlank() && price != null && price > 0) {
                        onSave(WishlistEntity(ownerUserId = userId, profileId = profileId, itemName = name.trim(), targetPrice = price, priority = priority))
                        showForm = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun FinancialNotesSection(
    userId: Long,
    notes: List<FinancialNoteEntity>,
    onSave: (FinancialNoteEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var showForm by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addRecord)
            }
        }
        items(notes, key = { it.id }) { note ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(note.body, style = MaterialTheme.typography.bodySmall)
                        Text(DateTimeUtil.formatDateTime(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onSave(note.copy(isPinned = !note.isPinned)) }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(note.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showForm) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(strings.financialNotes) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(strings.noteTitle) }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(strings.noteBody) }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        onSave(FinancialNoteEntity(ownerUserId = userId, title = title.trim(), body = body.trim()))
                        showForm = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun QrPaymentsLogSection(
    currencyCode: String,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>
) {
    val strings = LocalStrings.current
    val qrTxs = remember(transactions) {
        transactions.filter { !it.qrReference.isNullOrBlank() || it.paymentMethod.equals("QR Pay", ignoreCase = true) }
    }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val catMap = remember(categories) { categories.associateBy { it.id } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (qrTxs.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.QrCode2,
                    title = strings.qrRecords,
                    subtitle = strings.startAddingTip
                )
            }
        } else {
            items(qrTxs, key = { it.id }) { tx ->
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tx.qrReference ?: localizedCategoryName(catMap[tx.categoryId], strings), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${accMap[tx.accountId]?.name ?: "Account"} • ${DateTimeUtil.formatDateTime(tx.timestamp)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        MoneyText(amount = tx.amount, currencyCode = currencyCode)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
