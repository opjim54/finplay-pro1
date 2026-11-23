package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.DateTimeUtil
import com.example.i18n.LocalStrings
import com.example.ui.components.IconMapper
import com.example.ui.components.localizedCategoryName
import java.io.File
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    userId: Long,
    profileId: Long = 1L,
    initialType: TransactionType,
    initialQrFocus: Boolean,
    editingTransaction: TransactionEntity?,
    initialAmount: Double? = null,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    var amountText by remember {
        mutableStateOf(
            (editingTransaction?.amount ?: initialAmount)?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else String.format(java.util.Locale.US, "%.2f", it)
            } ?: ""
        )
    }

    val filteredCategories = remember(categories, type) {
        when (type) {
            TransactionType.INCOME -> categories.filter { it.type == TransactionType.INCOME }
            TransactionType.EXPENSE -> categories.filter { it.type == TransactionType.EXPENSE }
            TransactionType.TRANSFER -> categories
        }
    }

    var selectedCategoryId by remember {
        mutableLongStateOf(
            editingTransaction?.categoryId
                ?: filteredCategories.firstOrNull()?.id
                ?: categories.firstOrNull()?.id
                ?: 0L
        )
    }
    var selectedAccountId by remember {
        mutableLongStateOf(editingTransaction?.accountId ?: accounts.firstOrNull()?.id ?: 0L)
    }
    var targetAccountId by remember {
        mutableLongStateOf(
            editingTransaction?.targetAccountId
                ?: accounts.getOrNull(1)?.id
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }
    var paymentMethod by remember {
        mutableStateOf(
            editingTransaction?.paymentMethod
                ?: if (initialQrFocus) "QR Pay" else (accounts.firstOrNull()?.name ?: "Cash")
        )
    }
    var timestamp by remember { mutableLongStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(editingTransaction?.note ?: "") }
    var qrReference by remember { mutableStateOf(editingTransaction?.qrReference ?: "") }
    var receiptUri by remember { mutableStateOf(editingTransaction?.receiptUri) }
    var isFavorite by remember { mutableStateOf(editingTransaction?.isFavorite ?: false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptUri = persistReceiptLocally(context, uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) strings.editTransaction else strings.addRecord,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = strings.favorite,
                        tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Type Segmented Control
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(
                    TransactionType.INCOME to strings.income,
                    TransactionType.EXPENSE to strings.expense,
                    TransactionType.TRANSFER to strings.transferLabel
                )
                types.forEachIndexed { idx, (t, label) ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = {
                            type = t
                            val firstCat = when (t) {
                                TransactionType.INCOME -> categories.firstOrNull { it.type == TransactionType.INCOME }
                                TransactionType.EXPENSE -> categories.firstOrNull { it.type == TransactionType.EXPENSE }
                                TransactionType.TRANSFER -> categories.firstOrNull()
                            }
                            if (firstCat != null) selectedCategoryId = firstCat.id
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = types.size)
                    ) {
                        Text(label)
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                    formError = null
                },
                label = { Text(strings.amount) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            // Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(Calendar.YEAR, y)
                                cal.set(Calendar.MONTH, m)
                                cal.set(Calendar.DAY_OF_MONTH, d)
                                timestamp = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(DateTimeUtil.formatDate(timestamp))
                }
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h)
                                cal.set(Calendar.MINUTE, min)
                                timestamp = cal.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(DateTimeUtil.formatTime(timestamp))
                }
            }

            // Account Selection
            Text(strings.account, style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = { selectedAccountId = acc.id },
                        label = { Text(acc.name) }
                    )
                }
            }

            // Target Account for Transfer
            if (type == TransactionType.TRANSFER) {
                Text(strings.targetAccount, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = targetAccountId == acc.id,
                            onClick = { targetAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }
            } else {
                // Category Selection
                Text(strings.category, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            leadingIcon = {
                                Icon(
                                    IconMapper.iconFor(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(localizedCategoryName(cat, strings)) }
                        )
                    }
                }
            }

            // Payment Methods
            Text(strings.paymentMethod, style = MaterialTheme.typography.labelLarge)
            val methods = listOf("Cash", "Bkash", "Nagad", "Rocket", "Bank", "Card", "QR Pay")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                methods.forEach { m ->
                    FilterChip(
                        selected = paymentMethod == m,
                        onClick = { paymentMethod = m },
                        label = { Text(m) }
                    )
                }
            }

            // Note & QR Ref
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.note) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            OutlinedTextField(
                value = qrReference,
                onValueChange = { qrReference = it },
                label = { Text(strings.qrRefLabel) },
                leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Receipt Photo Picker (Zero-permission Android Photo Picker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.attachReceipt)
                }
                if (!receiptUri.isNullOrBlank()) {
                    Box {
                        AsyncImage(
                            model = receiptUri,
                            contentDescription = strings.receiptImage,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        IconButton(
                            onClick = { receiptUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = strings.removeReceipt,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (!formError.isNullOrBlank()) {
                Text(
                    text = formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        when {
                            amt == null || amt <= 0.0 -> formError = strings.invalidAmountError
                            selectedAccountId == 0L -> formError = strings.selectAccountError
                            type == TransactionType.TRANSFER && selectedAccountId == targetAccountId ->
                                formError = strings.sameAccountTransferError
                            type != TransactionType.TRANSFER && selectedCategoryId == 0L ->
                                formError = strings.selectCategoryError
                            else -> {
                                onSave(
                                    TransactionEntity(
                                        id = editingTransaction?.id ?: 0L,
                                        ownerUserId = userId,
                                        profileId = editingTransaction?.profileId ?: profileId,
                                        type = type,
                                        amount = amt,
                                        categoryId = selectedCategoryId,
                                        accountId = selectedAccountId,
                                        targetAccountId = if (type == TransactionType.TRANSFER) targetAccountId else null,
                                        paymentMethod = paymentMethod,
                                        timestamp = timestamp,
                                        note = note.trim(),
                                        receiptUri = receiptUri,
                                        qrReference = qrReference.trim().ifBlank { null },
                                        isFavorite = isFavorite
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_save_button")
                ) {
                    Text(strings.save)
                }
            }
        }
    }
}

private fun persistReceiptLocally(context: Context, uri: Uri): String {
    return runCatching {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "receipt_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrDefault(uri.toString())
}
