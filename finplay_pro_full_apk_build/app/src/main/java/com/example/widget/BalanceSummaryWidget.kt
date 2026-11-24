package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesStore
import com.example.data.local.TransactionType
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.i18n.AppLanguage

data class WidgetSnapshot(
    val profileName: String,
    val balance: Double,
    val budgetLimit: Double,
    val budgetSpent: Double,
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val currencyCode: String,
    val isBangla: Boolean
)

class BalanceSummaryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PreferencesStore(context).getSnapshot()
        val db = AppDatabase.getInstance(context)
        val userId = if (prefs.currentUserId > 0L) prefs.currentUserId else 1L
        val profiles = db.financeProfileDao().getByUserOnce(userId)
        val activeProfile = profiles.firstOrNull { it.id == prefs.activeProfileId } ?: profiles.firstOrNull()
        val profileId = activeProfile?.id ?: prefs.activeProfileId

        val accounts = db.accountDao().getByProfileOnce(userId, profileId)
        val txs = db.transactionDao().getAllByProfileOnce(userId, profileId).filter { !it.isDeleted }
        val monthKey = DateTimeUtil.monthKey()
        val budgets = db.budgetDao().getByProfileAndMonth(userId, profileId, monthKey)
        val totalBudgetLimit = budgets.sumOf { it.limitAmount }
        val startMonth = DateTimeUtil.startOfMonth()
        val monthlyIncome = txs.filter { it.type == TransactionType.INCOME && it.timestamp >= startMonth }
            .sumOf { it.amount }
        val monthlyExpense = txs.filter { it.type == TransactionType.EXPENSE && it.timestamp >= startMonth }
            .sumOf { it.amount }
        val budgetedCategoryIds = budgets.map { it.categoryId }.toSet()
        val budgetSpent = if (budgetedCategoryIds.isNotEmpty()) {
            txs.filter { it.type == TransactionType.EXPENSE && it.timestamp >= startMonth && it.categoryId in budgetedCategoryIds }
                .sumOf { it.amount }
        } else {
            monthlyExpense
        }

        val data = WidgetSnapshot(
            profileName = activeProfile?.name ?: "Personal",
            balance = accounts.sumOf { it.balance },
            budgetLimit = totalBudgetLimit,
            budgetSpent = budgetSpent,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            currencyCode = prefs.currencyCode,
            isBangla = prefs.language == AppLanguage.BANGLA
        )

        provideContent {
            WidgetContent(data)
        }
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "widget_action"
        const val ACTION_NEW_TRANSACTION = "new_transaction"
        val ACTION_KEY = ActionParameters.Key<String>(EXTRA_WIDGET_ACTION)

        suspend fun refreshAll(context: Context) {
            runCatching {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(BalanceSummaryWidget::class.java)
                ids.forEach { BalanceSummaryWidget().update(context, it) }
            }
        }
    }
}

@Composable
private fun WidgetContent(data: WidgetSnapshot) {
    val newTxAction = actionStartActivity<MainActivity>(
        actionParametersOf(BalanceSummaryWidget.ACTION_KEY to BalanceSummaryWidget.ACTION_NEW_TRANSACTION)
    )
    val openAppAction = actionStartActivity<MainActivity>()

    val hasBudget = data.budgetLimit > 0.0
    val budgetRatio = if (hasBudget) (data.budgetSpent / data.budgetLimit).coerceIn(0.0, 1.0) else 0.0
    val remainingBudget = (data.budgetLimit - data.budgetSpent).coerceAtLeast(0.0)
    val budgetOver = hasBudget && data.budgetSpent > data.budgetLimit

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF06231C)))
            .padding(14.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${data.profileName.uppercase()} • SMART FINANCE",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFD4AF37)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = CurrencyUtil.format(data.balance, data.currencyCode),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFECFDF5)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Box(
                modifier = GlanceModifier
                    .cornerRadius(14.dp)
                    .background(ColorProvider(Color(0xFFD4AF37)))
                    .clickable(newTxAction)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (data.isBangla) "+ হিসাব" else "+ New Tx",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF06231C)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (hasBudget) {
            val pct = ((data.budgetSpent / data.budgetLimit) * 100.0).toInt()
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = if (data.isBangla) {
                        "মাসিক বাজেট: ${CurrencyUtil.formatCompact(data.budgetSpent, data.currencyCode)} / ${CurrencyUtil.formatCompact(data.budgetLimit, data.currencyCode)}"
                    } else {
                        "Budget: ${CurrencyUtil.formatCompact(data.budgetSpent, data.currencyCode)} / ${CurrencyUtil.formatCompact(data.budgetLimit, data.currencyCode)}"
                    },
                    style = TextStyle(color = ColorProvider(Color(0xFFD1FAE5)), fontSize = 10.sp),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = if (budgetOver) "Over ($pct%)" else "${CurrencyUtil.formatCompact(remainingBudget, data.currencyCode)} left",
                    style = TextStyle(
                        color = ColorProvider(if (budgetOver) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            val filledSegments = (budgetRatio * 10.0).toInt().coerceIn(0, 10)
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                for (seg in 1..10) {
                    val isFilled = seg <= filledSegments
                    val segColor = when {
                        !isFilled -> Color(0xFF144235)
                        budgetOver -> Color(0xFFEF4444)
                        budgetRatio >= 0.85 -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(6.dp)
                            .cornerRadius(3.dp)
                            .background(ColorProvider(segColor))
                    ) {}
                    if (seg < 10) {
                        Spacer(modifier = GlanceModifier.padding(horizontal = 1.dp))
                    }
                }
            }
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    text = if (data.isBangla) "আয়: ${CurrencyUtil.formatCompact(data.monthlyIncome, data.currencyCode)}"
                    else "In: ${CurrencyUtil.formatCompact(data.monthlyIncome, data.currencyCode)}",
                    style = TextStyle(color = ColorProvider(Color(0xFF6EE7B7)), fontSize = 10.sp),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = if (data.isBangla) "ব্যয়: ${CurrencyUtil.formatCompact(data.monthlyExpense, data.currencyCode)}"
                    else "Out: ${CurrencyUtil.formatCompact(data.monthlyExpense, data.currencyCode)}",
                    style = TextStyle(color = ColorProvider(Color(0xFFFCA5A5)), fontSize = 10.sp)
                )
            }
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if (data.isBangla) "মাসিক আয়" else "Month Income",
                        style = TextStyle(color = ColorProvider(Color(0xFFA7F3D0)), fontSize = 10.sp)
                    )
                    Text(
                        text = CurrencyUtil.formatCompact(data.monthlyIncome, data.currencyCode),
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF6EE7B7)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if (data.isBangla) "মাসিক ব্যয়" else "Month Expense",
                        style = TextStyle(color = ColorProvider(Color(0xFFFECACA)), fontSize = 10.sp)
                    )
                    Text(
                        text = CurrencyUtil.formatCompact(data.monthlyExpense, data.currencyCode),
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFCA5A5)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class BalanceSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceSummaryWidget()
}
