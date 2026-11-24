package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.i18n.LocalStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LedgerCard
import com.example.ui.components.MoneyText
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.localizedCategoryName
import com.example.ui.theme.LocalFinanceColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class DateFilter {
    ALL, TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, CUSTOM
}

private enum class TxSort {
    NEWEST, OLDEST, HIGHEST, LOWEST
}

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    trashTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    currencyCode: String,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDuplicateTransaction: (TransactionEntity) -> Unit,
    onToggleFavorite: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onRestoreFromTrash: (Long) -> Unit,
    onPermanentDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val context = LocalContext.current
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }

    var showCalendarMode by remember { mutableStateOf(false) }
    var showTrashMode by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var categoryFilterId by remember { mutableLongStateOf(-1L) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showAdvancedFilter by remember { mutableStateOf(false) }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }
    var customStart by remember { mutableLongStateOf(DateTimeUtil.startOfMonth()) }
    var customEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sortOrder by remember { mutableStateOf(TxSort.NEWEST) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val filtered = remember(
        transactions, searchQuery, dateFilter, typeFilter, categoryFilterId,
        favoritesOnly, minAmountText, maxAmountText, customStart, customEnd, sortOrder, catMap, accMap
    ) {
        val now = System.currentTimeMillis()
        val todayStart = DateTimeUtil.startOfToday(now)
        val todayEnd = DateTimeUtil.endOfDay(now)
        val minAmt = minAmountText.toDoubleOrNull()
        val maxAmt = maxAmountText.toDoubleOrNull()
        val q = searchQuery.trim().lowercase()

        transactions.filter { tx ->
            val inDate = when (dateFilter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> tx.timestamp in todayStart..todayEnd
                DateFilter.YESTERDAY -> tx.timestamp in (todayStart - 86_400_000L)..(todayStart - 1L)
                DateFilter.THIS_WEEK -> tx.timestamp in DateTimeUtil.startOfWeek(now)..todayEnd
                DateFilter.THIS_MONTH -> tx.timestamp in DateTimeUtil.startOfMonth(now)..todayEnd
                DateFilter.LAST_MONTH -> {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = DateTimeUtil.startOfMonth(now)
                        add(Calendar.MONTH, -1)
                    }
                    val ls = c.timeInMillis
                    val le = DateTimeUtil.startOfMonth(now) - 1L
                    tx.timestamp in ls..le
                }
                DateFilter.THIS_YEAR -> tx.timestamp in DateTimeUtil.startOfYear(now)..todayEnd
                DateFilter.CUSTOM -> tx.timestamp in DateTimeUtil.startOfToday(customStart)..DateTimeUtil.endOfDay(customEnd)
            }
            val matchType = typeFilter == null || tx.type == typeFilter
            val matchCat = categoryFilterId == -1L || tx.categoryId == categoryFilterId
            val matchFav = !favoritesOnly || tx.isFavorite
            val matchMin = minAmt == null || tx.amount >= minAmt
            val matchMax = maxAmt == null || tx.amount <= maxAmt
            val matchSearch = if (q.isEmpty()) true else {
                val catName = localizedCategoryName(catMap[tx.categoryId], strings).lowercase()
                val accName = (accMap[tx.accountId]?.name ?: "").lowercase()
                val dateStr = DateTimeUtil.formatDate(tx.timestamp).lowercase()
                catName.contains(q) ||
                    accName.contains(q) ||
                    tx.paymentMethod.lowercase().contains(q) ||
                    tx.note.lowercase().contains(q) ||
                    tx.amount.toString().contains(q) ||
                    dateStr.contains(q)
            }
            inDate && matchType && matchCat && matchFav && matchMin && matchMax && matchSearch
        }.let { list ->
            when (sortOrder) {
                TxSort.NEWEST -> list.sortedByDescending { it.timestamp }
                TxSort.OLDEST -> list.sortedBy { it.timestamp }
                TxSort.HIGHEST -> list.sortedByDescending { it.amount }
                TxSort.LOWEST -> list.sortedBy { it.amount }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        showTrashMode -> strings.trashBin
                        showCalendarMode -> strings.calendarView
                        else -> strings.transactions
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = {
                        showTrashMode = false
                        showCalendarMode = !showCalendarMode
                    }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = strings.calendarView,
                            tint = if (showCalendarMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        showCalendarMode = false
                        showTrashMode = !showTrashMode
                    }) {
                        BadgedBox(badge = {
                            if (trashTransactions.isNotEmpty()) {
                                Badge { Text(trashTransactions.size.toString()) }
                            }
                        }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = strings.trashBin,
                                tint = if (showTrashMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (showTrashMode) {
                if (trashTransactions.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateView(
                            icon = Icons.Default.DeleteSweep,
                            title = strings.emptyTrash,
                            subtitle = strings.emptyTrashDesc
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trashTransactions, key = { it.id }) { tx ->
                            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = localizedCategoryName(catMap[tx.categoryId], strings),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${DateTimeUtil.formatDateTime(tx.timestamp)} • ${tx.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    MoneyText(
                                        amount = tx.amount,
                                        currencyCode = currencyCode,
                                        color = if (tx.type == TransactionType.INCOME) financeColors.income else financeColors.expense
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onRestoreFromTrash(tx.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.restore)
                                    }
                                    OutlinedButton(
                                        onClick = { onPermanentDelete(tx.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.permanentDelete, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (showCalendarMode) {
                CalendarTransactionsView(
                    transactions = transactions,
                    catMap = catMap,
                    accMap = accMap,
                    currencyCode = currencyCode,
                    onEditTransaction = onEditTransaction,
                    onDuplicateTransaction = onDuplicateTransaction,
                    onToggleFavorite = onToggleFavorite,
                    onDeleteTransaction = onDeleteTransaction
                )
            } else {
                // Search Bar + Filter & Sort
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchHint, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            Row {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                                IconButton(onClick = { showAdvancedFilter = !showAdvancedFilter }) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = "Filters",
                                        tint = if (showAdvancedFilter) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    IconButton(onClick = { sortMenuOpen = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false }
                                    ) {
                                        listOf(
                                            TxSort.NEWEST to strings.sortNewest,
                                            TxSort.OLDEST to strings.sortOldest,
                                            TxSort.HIGHEST to strings.sortHighest,
                                            TxSort.LOWEST to strings.sortLowest
                                        ).forEach { (s, lbl) ->
                                            DropdownMenuItem(
                                                text = { Text(lbl) },
                                                onClick = {
                                                    sortOrder = s
                                                    sortMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val dateOptions = listOf(
                            DateFilter.ALL to strings.filterAll,
                            DateFilter.TODAY to strings.filterToday,
                            DateFilter.YESTERDAY to strings.filterYesterday,
                            DateFilter.THIS_WEEK to strings.filterThisWeek,
                            DateFilter.THIS_MONTH to strings.filterThisMonth,
                            DateFilter.LAST_MONTH to strings.filterLastMonth,
                            DateFilter.THIS_YEAR to strings.filterThisYear,
                            DateFilter.CUSTOM to strings.filterCustomDate
                        )
                        items(dateOptions) { (df, lbl) ->
                            FilterChip(
                                selected = dateFilter == df,
                                onClick = {
                                    dateFilter = df
                                    if (df == DateFilter.CUSTOM) {
                                        val c = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                c.set(y, m, d)
                                                customStart = c.timeInMillis
                                                DatePickerDialog(
                                                    context,
                                                    { _, y2, m2, d2 ->
                                                        c.set(y2, m2, d2)
                                                        customEnd = c.timeInMillis
                                                    },
                                                    c.get(Calendar.YEAR),
                                                    c.get(Calendar.MONTH),
                                                    c.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                            c.get(Calendar.YEAR),
                                            c.get(Calendar.MONTH),
                                            c.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    // Type & Favorites Quick Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = typeFilter == TransactionType.INCOME,
                                onClick = {
                                    typeFilter = if (typeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                                },
                                label = { Text(strings.income) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = typeFilter == TransactionType.EXPENSE,
                                onClick = {
                                    typeFilter = if (typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                },
                                label = { Text(strings.expense) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = favoritesOnly,
                                onClick = { favoritesOnly = !favoritesOnly },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(strings.favorite) }
                            )
                        }
                    }

                    if (showAdvancedFilter) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minAmountText,
                                onValueChange = { minAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Min ${strings.amount}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxAmountText,
                                onValueChange = { maxAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Max ${strings.amount}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = categoryFilterId == -1L,
                                    onClick = { categoryFilterId = -1L },
                                    label = { Text(strings.filterAll) }
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = categoryFilterId == cat.id,
                                    onClick = { categoryFilterId = if (categoryFilterId == cat.id) -1L else cat.id },
                                    label = { Text(localizedCategoryName(cat, strings)) }
                                )
                            }
                        }
                    }
                }

                // Filtered Totals Bar
                val totalInc = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExp = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filtered.size} ${strings.transactions}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${CurrencyUtil.formatCompact(totalInc, currencyCode)}  /  -${
                            CurrencyUtil.formatCompact(totalExp, currencyCode)
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateView(
                            icon = Icons.Default.ReceiptLong,
                            title = strings.noDataYet,
                            subtitle = strings.startAddingTip
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { tx ->
                            TransactionItemCard(
                                tx = tx,
                                category = catMap[tx.categoryId],
                                account = accMap[tx.accountId],
                                targetAccount = tx.targetAccountId?.let { accMap[it] },
                                currencyCode = currencyCode,
                                onClick = { onEditTransaction(tx) },
                                onEdit = { onEditTransaction(tx) },
                                onDuplicate = { onDuplicateTransaction(tx) },
                                onToggleFavorite = { onToggleFavorite(tx) },
                                onDelete = { onDeleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTransactionsView(
    transactions: List<TransactionEntity>,
    catMap: Map<Long, CategoryEntity>,
    accMap: Map<Long, AccountEntity>,
    currencyCode: String,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDuplicateTransaction: (TransactionEntity) -> Unit,
    onToggleFavorite: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var monthOffset by remember { mutableIntStateOf(0) }
    val cal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - 1
    var selectedDay by remember(monthOffset) {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtMost(daysInMonth))
    }

    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(cal.timeInMillis))
    val dayTxsMap = remember(transactions, year, month) {
        transactions.filter { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        }.groupBy { tx ->
            Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.DAY_OF_MONTH)
        }
    }

    val selectedDayTxs = dayTxsMap[selectedDay].orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset-- }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                    }
                    Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { monthOffset++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val weekHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekHeaders.forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                val totalCells = firstDayOfWeekOffset + daysInMonth
                val rows = (totalCells + 6) / 7
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOfWeekOffset + 1
                            if (dayNum !in 1..daysInMonth) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val txs = dayTxsMap[dayNum].orEmpty()
                                val hasInc = txs.any { it.type == TransactionType.INCOME }
                                val hasExp = txs.any { it.type == TransactionType.EXPENSE }
                                val isSel = dayNum == selectedDay
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    border = if (isSel) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedDay = dayNum }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (hasInc) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(financeColors.income)
                                                )
                                            }
                                            if (hasExp) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(financeColors.expense)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "$selectedDay $monthLabel (${selectedDayTxs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedDayTxs.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.CalendarMonth,
                    title = strings.noDataYet,
                    subtitle = strings.startAddingTip
                )
            }
        } else {
            items(selectedDayTxs, key = { it.id }) { tx ->
                TransactionItemCard(
                    tx = tx,
                    category = catMap[tx.categoryId],
                    account = accMap[tx.accountId],
                    targetAccount = tx.targetAccountId?.let { accMap[it] },
                    currencyCode = currencyCode,
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
