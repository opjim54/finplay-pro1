package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionType
import com.example.domain.CurrencyUtil
import com.example.i18n.LocalStrings
import com.example.ui.theme.LocalFinanceColors
import java.util.Locale

private val bdtBaseRates = mapOf(
    "BDT" to 1.0,
    "USD" to 120.0,
    "EUR" to 130.0,
    "GBP" to 152.0,
    "INR" to 1.43,
    "SAR" to 32.0,
    "AED" to 32.65,
    "MYR" to 27.2
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCalculatorSheet(
    activeCurrencyCode: String,
    onDismiss: () -> Unit,
    onUseAmountForTransaction: (Double, TransactionType) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var expression by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf(activeCurrencyCode) }
    var toCurrency by remember {
        mutableStateOf(if (activeCurrencyCode == "BDT") "USD" else "BDT")
    }

    val evaluatedValue = remember(expression) { evaluateSimpleExpression(expression) }
    val convertedValue = remember(evaluatedValue, fromCurrency, toCurrency) {
        val fromRate = bdtBaseRates[fromCurrency] ?: 1.0
        val toRate = bdtBaseRates[toCurrency] ?: 1.0
        (evaluatedValue * fromRate) / toRate
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.calculatorConverterTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Display Box
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifBlank { "0" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtil.format(evaluatedValue, fromCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calculator_result_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "≈ ${CurrencyUtil.format(convertedValue, toCurrency)} ($toCurrency)",
                        style = MaterialTheme.typography.labelLarge,
                        color = financeColors.savings,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Currency Converter Selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${strings.convertCurrencyTitle}: $fromCurrency → $toCurrency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CurrencyUtil.supportedCurrencies.forEach { cur ->
                        FilterChip(
                            selected = toCurrency == cur.code,
                            onClick = { toCurrency = cur.code },
                            label = { Text("${cur.symbol} ${cur.code}", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Keypad Grid
            val rows = listOf(
                listOf("C", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("00", "0", ".", "=")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val isOp = key in listOf("÷", "×", "-", "+", "=", "%")
                            val isClear = key in listOf("C", "⌫")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            key == "=" -> MaterialTheme.colorScheme.primary
                                            isOp -> MaterialTheme.colorScheme.primaryContainer
                                            isClear -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable {
                                        when (key) {
                                            "C" -> expression = ""
                                            "⌫" -> if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                            }
                                            "=" -> {
                                                val v = evaluateSimpleExpression(expression)
                                                expression = if (v % 1.0 == 0.0) {
                                                    v.toLong().toString()
                                                } else {
                                                    String.format(Locale.US, "%.2f", v)
                                                }
                                            }
                                            else -> expression += key
                                        }
                                    }
                                    .testTag("calc_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        key == "=" -> MaterialTheme.colorScheme.onPrimary
                                        isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isClear -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 1-Tap Use as Income / Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (evaluatedValue > 0.0) {
                            onUseAmountForTransaction(evaluatedValue, TransactionType.INCOME)
                        }
                    },
                    enabled = evaluatedValue > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = financeColors.income),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_use_income_button")
                ) {
                    Text(strings.useAsIncome, maxLines = 1)
                }
                Button(
                    onClick = {
                        if (evaluatedValue > 0.0) {
                            onUseAmountForTransaction(evaluatedValue, TransactionType.EXPENSE)
                        }
                    },
                    enabled = evaluatedValue > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = financeColors.expense),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_use_expense_button")
                ) {
                    Text(strings.useAsExpense, maxLines = 1)
                }
            }
        }
    }
}

internal fun evaluateSimpleExpression(expr: String): Double {
    val cleaned = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace(" ", "")
        .trim()
    if (cleaned.isEmpty()) return 0.0
    return runCatching {
        val tokens = mutableListOf<String>()
        val numBuf = StringBuilder()
        for (i in cleaned.indices) {
            val c = cleaned[i]
            if (c.isDigit() || c == '.') {
                numBuf.append(c)
            } else if (c in listOf('+', '-', '*', '/', '%')) {
                if (numBuf.isNotEmpty()) {
                    tokens.add(numBuf.toString())
                    numBuf.clear()
                }
                tokens.add(c.toString())
            }
        }
        if (numBuf.isNotEmpty()) {
            tokens.add(numBuf.toString())
        }
        if (tokens.isEmpty()) return 0.0

        var acc = tokens.first().toDoubleOrNull() ?: 0.0
        var idx = 1
        while (idx < tokens.size) {
            val op = tokens[idx]
            if (op == "%") {
                acc /= 100.0
                idx += 1
                continue
            }
            val nextVal = tokens.getOrNull(idx + 1)?.toDoubleOrNull() ?: break
            acc = when (op) {
                "+" -> acc + nextVal
                "-" -> acc - nextVal
                "*" -> acc * nextVal
                "/" -> if (nextVal != 0.0) acc / nextVal else acc
                else -> acc
            }
            idx += 2
        }
        acc
    }.getOrDefault(0.0)
}
