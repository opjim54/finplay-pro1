package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.domain.DeepAnalyticsSummary
import com.example.domain.ReportPeriod
import com.example.domain.ReportSummary
import com.example.i18n.LocalStrings
import com.example.ui.components.CategoryComparisonBars
import com.example.ui.components.InteractiveBarComparisonChart
import com.example.ui.components.InteractiveLineAreaChart
import com.example.ui.components.InteractivePieDonutChart
import com.example.ui.components.LedgerCard
import com.example.ui.components.MoneyText
import com.example.ui.theme.LocalFinanceColors
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun AnalyticsReportsScreen(
    currencyCode: String,
    deepAnalytics: DeepAnalyticsSummary,
    computeReport: (ReportPeriod, Long?, Long?) -> ReportSummary
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    val context = LocalContext.current

    var mainTab by remember { mutableIntStateOf(0) } // 0 = Reports, 1 = Deep Analytics & Charts
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    var customStart by remember { mutableLongStateOf(DateTimeUtil.startOfMonth()) }
    var customEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var chartMode by remember { mutableIntStateOf(0) } // 0=Pie, 1=Bar, 2=Line, 3=Area, 4=Monthly, 5=Yearly, 6=Category

    val report = remember(selectedPeriod, customStart, customEnd, deepAnalytics) {
        computeReport(selectedPeriod, customStart, customEnd)
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
                    text = "${strings.reports} & ${strings.analytics}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(strings.reports, strings.analytics).forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = mainTab == idx,
                            onClick = { mainTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, 2)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (mainTab == 0) {
                // Report Period Selector
                item {
                    val periods = listOf(
                        ReportPeriod.DAILY to strings.dailyReport,
                        ReportPeriod.WEEKLY to strings.weeklyReport,
                        ReportPeriod.MONTHLY to strings.monthlyReport,
                        ReportPeriod.YEARLY to strings.yearlyReport,
                        ReportPeriod.CUSTOM to strings.customReport
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(periods) { (p, lbl) ->
                            FilterChip(
                                selected = selectedPeriod == p,
                                onClick = { selectedPeriod = p },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }

                if (selectedPeriod == ReportPeriod.CUSTOM) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = customStart }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(y, m, d)
                                            customStart = cal.timeInMillis
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
                                Text(DateTimeUtil.formatDate(customStart))
                            }
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = customEnd }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(y, m, d)
                                            customEnd = cal.timeInMillis
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
                                Text(DateTimeUtil.formatDate(customEnd))
                            }
                        }
                    }
                }

                // Report KPIs
                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReportStatBox(strings.income, report.income, currencyCode, financeColors.income, Modifier.weight(1f))
                            ReportStatBox(strings.expense, report.expense, currencyCode, financeColors.expense, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReportStatBox(strings.savingsGoals, report.savings, currencyCode, financeColors.savings, Modifier.weight(1f))
                            ReportStatBox(strings.netBalance, report.netBalance, currencyCode, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.growthRate, style = MaterialTheme.typography.titleSmall)
                                val g = report.growthRatePercent.roundToInt()
                                Text(
                                    text = if (g >= 0) "+$g%" else "$g%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (g >= 0) financeColors.income else financeColors.expense
                                )
                            }
                        }
                    }
                }

                // Period Comparison Chart
                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.comparison,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        InteractiveBarComparisonChart(points = report.points, currencyCode = currencyCode)
                    }
                }

                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.categoryComparison,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CategoryComparisonBars(items = report.categoryBreakdowns, currencyCode = currencyCode)
                    }
                }
            } else {
                // Deep Analytics: Financial Health Score & Insights & Prediction
                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.financialHealthScore,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${deepAnalytics.financialHealthScore}/100",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    deepAnalytics.financialHealthScore >= 75 -> financeColors.income
                                    deepAnalytics.financialHealthScore >= 45 -> financeColors.warning
                                    else -> financeColors.expense
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { deepAnalytics.financialHealthScore / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = when {
                                deepAnalytics.financialHealthScore >= 75 -> financeColors.income
                                deepAnalytics.financialHealthScore >= 45 -> financeColors.warning
                                else -> financeColors.expense
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = strings.predictedNextMonth,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                MoneyText(
                                    amount = deepAnalytics.predictedNextMonthExpense,
                                    currencyCode = currencyCode,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Smart Spending Insights
                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.smartSpendingInsights,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        deepAnalytics.insights.forEach { insight ->
                            Text(
                                text = "• $insight",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }

                // Averages & Records Grid
                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnalyticsMetricRow(
                                strings.highestSpendingMonth,
                                deepAnalytics.highestSpendingMonth?.let {
                                    "${it.first} (${CurrencyUtil.format(it.second, currencyCode)})"
                                } ?: "—"
                            )
                            AnalyticsMetricRow(
                                strings.lowestSpendingMonth,
                                deepAnalytics.lowestSpendingMonth?.let {
                                    "${it.first} (${CurrencyUtil.format(it.second, currencyCode)})"
                                } ?: "—"
                            )
                            AnalyticsMetricRow(
                                strings.highestIncomeMonth,
                                deepAnalytics.highestIncomeMonth?.let {
                                    "${it.first} (${CurrencyUtil.format(it.second, currencyCode)})"
                                } ?: "—"
                            )
                            AnalyticsMetricRow(
                                strings.highestExpenseCat,
                                deepAnalytics.highestExpenseCategory?.let {
                                    "${it.name} (${CurrencyUtil.format(it.amount, currencyCode)})"
                                } ?: "—"
                            )
                            AnalyticsMetricRow(
                                strings.lowestExpenseCat,
                                deepAnalytics.lowestExpenseCategory?.let {
                                    "${it.name} (${CurrencyUtil.format(it.amount, currencyCode)})"
                                } ?: "—"
                            )
                            AnalyticsMetricRow(
                                strings.avgDailyExpense,
                                CurrencyUtil.format(deepAnalytics.avgDailyExpense, currencyCode)
                            )
                            AnalyticsMetricRow(
                                strings.avgMonthlyExpense,
                                CurrencyUtil.format(deepAnalytics.avgMonthlyExpense, currencyCode)
                            )
                            AnalyticsMetricRow(
                                strings.avgYearlyExpense,
                                CurrencyUtil.format(deepAnalytics.avgYearlyExpense, currencyCode)
                            )
                        }
                    }
                }

                // Interactive Charts Gallery
                item {
                    val chartTypes = listOf(
                        strings.pieChart,
                        strings.barChart,
                        strings.lineChart,
                        strings.areaChart,
                        strings.monthlyComparison,
                        strings.yearlyComparison,
                        strings.categoryComparison
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chartTypes.indices.toList()) { idx ->
                            FilterChip(
                                selected = chartMode == idx,
                                onClick = { chartMode = idx },
                                label = { Text(chartTypes[idx]) }
                            )
                        }
                    }
                }

                item {
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        when (chartMode) {
                            0 -> InteractivePieDonutChart(
                                items = deepAnalytics.categoryBreakdowns,
                                currencyCode = currencyCode
                            )
                            1, 4 -> InteractiveBarComparisonChart(
                                points = deepAnalytics.monthlyPoints,
                                currencyCode = currencyCode
                            )
                            2 -> InteractiveLineAreaChart(
                                points = deepAnalytics.monthlyPoints,
                                currencyCode = currencyCode,
                                showArea = false
                            )
                            3 -> InteractiveLineAreaChart(
                                points = deepAnalytics.monthlyPoints,
                                currencyCode = currencyCode,
                                showArea = true
                            )
                            5 -> InteractiveBarComparisonChart(
                                points = deepAnalytics.yearlyPoints,
                                currencyCode = currencyCode
                            )
                            else -> CategoryComparisonBars(
                                items = deepAnalytics.categoryBreakdowns,
                                currencyCode = currencyCode
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ReportStatBox(
    label: String,
    amount: Double,
    currencyCode: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            MoneyText(amount = amount, currencyCode = currencyCode, color = color, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AnalyticsMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
