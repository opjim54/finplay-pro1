package com.example.domain

import com.example.data.local.AccountEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.LoanDebtEntity
import com.example.data.local.LoanKind
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppLanguage
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ReportPeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

enum class ObligationKind {
    RECURRING_RULE, LOAN_EMI, SAVINGS_GOAL
}

data class UpcomingObligation(
    val id: Long,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dueTimestamp: Long,
    val daysUntilDue: Int,
    val kind: ObligationKind,
    val colorHex: Long
)

data class CategoryBreakdown(
    val categoryId: Long,
    val name: String,
    val amount: Double,
    val colorHex: Long,
    val percentage: Float
)

data class PeriodComparisonPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val savings: Double
)

data class DashboardMetrics(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalSavings: Double,
    val todayIncome: Double,
    val todayExpense: Double,
    val todayNetFlow: Double = 0.0,
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val monthlySavings: Double,
    val monthlySavingsRate: Float = 0f,
    val yearlyIncome: Double = 0.0,
    val yearlyExpense: Double = 0.0,
    val yearlySavings: Double = 0.0,
    val totalBudgetLimit: Double,
    val totalBudgetUsed: Double,
    val budgetProgress: Float,
    val highestSpendingCategory: CategoryBreakdown?,
    val lowestSpendingCategory: CategoryBreakdown?,
    val monthlyCategoryBreakdowns: List<CategoryBreakdown>,
    val last6MonthsTrend: List<PeriodComparisonPoint>,
    val last30DaysTrend: List<PeriodComparisonPoint> = emptyList(),
    val upcomingObligations: List<UpcomingObligation> = emptyList()
)

data class ReportSummary(
    val income: Double,
    val expense: Double,
    val savings: Double,
    val netBalance: Double,
    val growthRatePercent: Double,
    val previousIncome: Double,
    val previousExpense: Double,
    val points: List<PeriodComparisonPoint>,
    val categoryBreakdowns: List<CategoryBreakdown>
)

data class DeepAnalyticsSummary(
    val highestSpendingMonth: Pair<String, Double>?,
    val lowestSpendingMonth: Pair<String, Double>?,
    val highestIncomeMonth: Pair<String, Double>?,
    val highestExpenseCategory: CategoryBreakdown?,
    val lowestExpenseCategory: CategoryBreakdown?,
    val avgDailyExpense: Double,
    val avgMonthlyExpense: Double,
    val avgYearlyExpense: Double,
    val predictedNextMonthExpense: Double,
    val financialHealthScore: Int,
    val insights: List<String>,
    val monthlyPoints: List<PeriodComparisonPoint>,
    val yearlyPoints: List<PeriodComparisonPoint>,
    val categoryBreakdowns: List<CategoryBreakdown>
)

object AnalyticsEngine {

    fun computeDashboard(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        now: Long = System.currentTimeMillis(),
        recurringRules: List<RecurringRuleEntity> = emptyList(),
        loans: List<LoanDebtEntity> = emptyList()
    ): DashboardMetrics {
        val active = transactions.filter { !it.isDeleted }
        val totalBalance = accounts.sumOf { it.balance }
        val totalIncome = active.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = active.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val goalsSaved = savingsGoals.sumOf { it.savedAmount }
        val totalSavings = max(goalsSaved, max(0.0, totalIncome - totalExpense))

        val startToday = DateTimeUtil.startOfToday(now)
        val endToday = DateTimeUtil.endOfDay(now)
        val todayTx = active.filter { it.timestamp in startToday..endToday }
        val todayIncome = todayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val todayExpense = todayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val todayNetFlow = todayIncome - todayExpense

        val startMonth = DateTimeUtil.startOfMonth(now)
        val monthTx = active.filter { it.timestamp in startMonth..endToday }
        val monthlyIncome = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthlyExpense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val monthlySavings = max(0.0, monthlyIncome - monthlyExpense)
        val monthlySavingsRate = if (monthlyIncome > 0.0) {
            (((monthlyIncome - monthlyExpense) / monthlyIncome) * 100.0).toFloat().coerceIn(-100f, 100f)
        } else 0f

        val startYear = DateTimeUtil.startOfYear(now)
        val yearTx = active.filter { it.timestamp in startYear..endToday }
        val yearlyIncome = yearTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val yearlyExpense = yearTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val yearlySavings = max(0.0, yearlyIncome - yearlyExpense)

        val currentMonthKey = DateTimeUtil.monthKey(now)
        val monthBudgets = budgets.filter { it.monthKey == currentMonthKey }
        val totalBudgetLimit = monthBudgets.sumOf { it.limitAmount }
        val totalBudgetUsed = if (monthBudgets.isNotEmpty()) {
            val budgetCatIds = monthBudgets.map { it.categoryId }.toSet()
            monthTx.filter { it.type == TransactionType.EXPENSE && it.categoryId in budgetCatIds }
                .sumOf { it.amount }
        } else {
            monthlyExpense
        }
        val budgetProgress = if (totalBudgetLimit > 0.0) {
            (totalBudgetUsed / totalBudgetLimit).toFloat().coerceIn(0f, 1.5f)
        } else 0f

        val catMap = categories.associateBy { it.id }
        val expenseByCat = monthTx
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapNotNull { (catId, list) ->
                val sum = list.sumOf { it.amount }
                if (sum <= 0.0) null else {
                    val cat = catMap[catId]
                    CategoryBreakdown(
                        categoryId = catId,
                        name = cat?.name ?: "Others",
                        amount = sum,
                        colorHex = cat?.colorHex ?: 0xFF047857,
                        percentage = if (monthlyExpense > 0.0) ((sum / monthlyExpense) * 100f).toFloat() else 0f
                    )
                }
            }
            .sortedByDescending { it.amount }

        val highest = expenseByCat.firstOrNull()
        val lowest = expenseByCat.lastOrNull()
        val last6Months = buildLastMonthsTrend(active, 6, now)
        val last30Days = buildDailyTrend(active, 30, now)

        val dayMs = 86_400_000L
        val obligations = buildList {
            recurringRules.filter { it.isActive }.forEach { rule ->
                val diffDays = ((DateTimeUtil.startOfToday(rule.nextRunAt) - startToday) / dayMs).toInt()
                add(
                    UpcomingObligation(
                        id = rule.id,
                        title = rule.title,
                        subtitle = "${rule.frequency.name} • ${rule.paymentMethod}",
                        amount = rule.amount,
                        dueTimestamp = rule.nextRunAt,
                        daysUntilDue = diffDays,
                        kind = ObligationKind.RECURRING_RULE,
                        colorHex = if (rule.type == TransactionType.INCOME) 0xFF10B981 else 0xFFF97316
                    )
                )
            }
            loans.filter { it.paidAmount < it.principalAmount }.forEach { loan ->
                val dueTs = loan.dueDate
                val diffDays = ((DateTimeUtil.startOfToday(dueTs) - startToday) / dayMs).toInt()
                val nextPay = if (loan.monthlyEmi > 0.0) loan.monthlyEmi else (loan.principalAmount - loan.paidAmount)
                add(
                    UpcomingObligation(
                        id = loan.id,
                        title = loan.counterparty,
                        subtitle = loan.kind.name,
                        amount = nextPay,
                        dueTimestamp = dueTs,
                        daysUntilDue = diffDays,
                        kind = ObligationKind.LOAN_EMI,
                        colorHex = 0xFFEF4444
                    )
                )
            }
            savingsGoals.filter { it.savedAmount < it.targetAmount && it.expectedDate > 0L }.forEach { goal ->
                val dueTs = goal.expectedDate
                val diffDays = ((DateTimeUtil.startOfToday(dueTs) - startToday) / dayMs).toInt()
                add(
                    UpcomingObligation(
                        id = goal.id,
                        title = goal.name,
                        subtitle = "Goal Target",
                        amount = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0),
                        dueTimestamp = dueTs,
                        daysUntilDue = diffDays,
                        kind = ObligationKind.SAVINGS_GOAL,
                        colorHex = 0xFF7C3AED
                    )
                )
            }
        }.sortedBy { it.daysUntilDue }.take(8)

        return DashboardMetrics(
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalSavings = totalSavings,
            todayIncome = todayIncome,
            todayExpense = todayExpense,
            todayNetFlow = todayNetFlow,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlySavings = monthlySavings,
            monthlySavingsRate = monthlySavingsRate,
            yearlyIncome = yearlyIncome,
            yearlyExpense = yearlyExpense,
            yearlySavings = yearlySavings,
            totalBudgetLimit = totalBudgetLimit,
            totalBudgetUsed = totalBudgetUsed,
            budgetProgress = budgetProgress,
            highestSpendingCategory = highest,
            lowestSpendingCategory = lowest,
            monthlyCategoryBreakdowns = expenseByCat,
            last6MonthsTrend = last6Months,
            last30DaysTrend = last30Days,
            upcomingObligations = obligations
        )
    }

    fun computeReport(
        period: ReportPeriod,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        customStart: Long? = null,
        customEnd: Long? = null,
        now: Long = System.currentTimeMillis()
    ): ReportSummary {
        val active = transactions.filter { !it.isDeleted }
        val (start, end, prevStart, prevEnd) = when (period) {
            ReportPeriod.DAILY -> {
                val s = DateTimeUtil.startOfToday(now)
                val e = DateTimeUtil.endOfDay(now)
                val ps = s - 86_400_000L
                val pe = s - 1L
                listOf(s, e, ps, pe)
            }
            ReportPeriod.WEEKLY -> {
                val s = DateTimeUtil.startOfWeek(now)
                val e = DateTimeUtil.endOfDay(now)
                val ps = s - 7 * 86_400_000L
                val pe = s - 1L
                listOf(s, e, ps, pe)
            }
            ReportPeriod.MONTHLY -> {
                val s = DateTimeUtil.startOfMonth(now)
                val e = DateTimeUtil.endOfDay(now)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = s
                    add(Calendar.MONTH, -1)
                }
                val ps = cal.timeInMillis
                val pe = s - 1L
                listOf(s, e, ps, pe)
            }
            ReportPeriod.YEARLY -> {
                val s = DateTimeUtil.startOfYear(now)
                val e = DateTimeUtil.endOfDay(now)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = s
                    add(Calendar.YEAR, -1)
                }
                val ps = cal.timeInMillis
                val pe = s - 1L
                listOf(s, e, ps, pe)
            }
            ReportPeriod.CUSTOM -> {
                val s = DateTimeUtil.startOfToday(customStart ?: (now - 30L * 86_400_000L))
                val e = DateTimeUtil.endOfDay(customEnd ?: now)
                val span = max(86_400_000L, e - s)
                listOf(s, e, s - span - 1L, s - 1L)
            }
        }

        val current = active.filter { it.timestamp in start..end }
        val previous = active.filter { it.timestamp in prevStart..prevEnd }

        val income = current.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = current.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = max(0.0, income - expense)
        val netBalance = income - expense

        val prevIncome = previous.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val prevExpense = previous.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevNet = prevIncome - prevExpense
        val growthRate = when {
            prevNet != 0.0 -> ((netBalance - prevNet) / kotlin.math.abs(prevNet)) * 100.0
            netBalance > 0.0 -> 100.0
            netBalance < 0.0 -> -100.0
            else -> 0.0
        }

        val catMap = categories.associateBy { it.id }
        val catBreakdowns = current.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.amount }
                val cat = catMap[catId]
                CategoryBreakdown(
                    categoryId = catId,
                    name = cat?.name ?: "Others",
                    amount = sum,
                    colorHex = cat?.colorHex ?: 0xFF047857,
                    percentage = if (expense > 0.0) ((sum / expense) * 100f).toFloat() else 0f
                )
            }
            .sortedByDescending { it.amount }

        val points = when (period) {
            ReportPeriod.DAILY, ReportPeriod.WEEKLY -> buildDailyTrend(current, 7, end)
            ReportPeriod.MONTHLY, ReportPeriod.CUSTOM -> buildLastMonthsTrend(active, 6, end)
            ReportPeriod.YEARLY -> buildYearlyTrend(active, 4, end)
        }

        return ReportSummary(
            income = income,
            expense = expense,
            savings = savings,
            netBalance = netBalance,
            growthRatePercent = growthRate,
            previousIncome = prevIncome,
            previousExpense = prevExpense,
            points = points,
            categoryBreakdowns = catBreakdowns
        )
    }

    fun computeDeepAnalytics(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        savingsGoals: List<SavingsGoalEntity>,
        loans: List<LoanDebtEntity>,
        currencyCode: String,
        language: AppLanguage,
        now: Long = System.currentTimeMillis()
    ): DeepAnalyticsSummary {
        val active = transactions.filter { !it.isDeleted }
        val byMonthExpense = active.filter { it.type == TransactionType.EXPENSE }
            .groupBy { DateTimeUtil.monthKey(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val byMonthIncome = active.filter { it.type == TransactionType.INCOME }
            .groupBy { DateTimeUtil.monthKey(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val highestSpendingMonth = byMonthExpense.maxByOrNull { it.value }?.let {
            DateTimeUtil.formatMonthLabel(it.key) to it.value
        }
        val lowestSpendingMonth = byMonthExpense.filterValues { it > 0.0 }
            .minByOrNull { it.value }?.let { DateTimeUtil.formatMonthLabel(it.key) to it.value }
        val highestIncomeMonth = byMonthIncome.maxByOrNull { it.value }?.let {
            DateTimeUtil.formatMonthLabel(it.key) to it.value
        }

        val catMap = categories.associateBy { it.id }
        val allExpense = active.filter { it.type == TransactionType.EXPENSE }
        val totalExp = allExpense.sumOf { it.amount }
        val totalInc = active.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val catBreakdowns = allExpense.groupBy { it.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.amount }
                val cat = catMap[catId]
                CategoryBreakdown(
                    categoryId = catId,
                    name = cat?.name ?: "Others",
                    amount = sum,
                    colorHex = cat?.colorHex ?: 0xFF047857,
                    percentage = if (totalExp > 0.0) ((sum / totalExp) * 100f).toFloat() else 0f
                )
            }
            .sortedByDescending { it.amount }

        val highestCat = catBreakdowns.firstOrNull()
        val lowestCat = catBreakdowns.lastOrNull()

        val activeDays = max(
            1,
            active.map { DateTimeUtil.startOfToday(it.timestamp) }.distinct().size
        )
        val activeMonths = max(1, byMonthExpense.keys.union(byMonthIncome.keys).size)
        val activeYears = max(
            1,
            active.map {
                Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.YEAR)
            }.distinct().size
        )

        val avgDaily = totalExp / activeDays
        val avgMonthly = totalExp / activeMonths
        val avgYearly = totalExp / activeYears

        // Weighted moving average of up to last 3 months for expense prediction
        val monthlyPoints = buildLastMonthsTrend(active, 6, now)
        val recentExpenses = monthlyPoints.takeLast(3).map { it.expense }.filter { it > 0.0 }
        val predictedNextMonth = when (recentExpenses.size) {
            0 -> 0.0
            1 -> recentExpenses[0]
            2 -> recentExpenses[0] * 0.4 + recentExpenses[1] * 0.6
            else -> recentExpenses[0] * 0.2 + recentExpenses[1] * 0.3 + recentExpenses[2] * 0.5
        }

        // Deterministic Financial Health Score (0-100)
        val savingsRate = if (totalInc > 0.0) ((totalInc - totalExp) / totalInc).coerceIn(-0.5, 1.0) else 0.0
        val savingsPoints = (savingsRate * 45.0).coerceIn(0.0, 45.0)
        val currentMonthKey = DateTimeUtil.monthKey(now)
        val currentBudgets = budgets.filter { it.monthKey == currentMonthKey }
        val budgetPoints = if (currentBudgets.isEmpty()) 20.0 else {
            val startM = DateTimeUtil.startOfMonth(now)
            val mExp = active.filter { it.type == TransactionType.EXPENSE && it.timestamp >= startM }
                .groupBy { it.categoryId }
                .mapValues { (_, l) -> l.sumOf { it.amount } }
            val withinCount = currentBudgets.count { (mExp[it.categoryId] ?: 0.0) <= it.limitAmount }
            25.0 * (withinCount.toDouble() / currentBudgets.size)
        }
        val goalProgressAvg = if (savingsGoals.isEmpty()) 10.0 else {
            val avg = savingsGoals.map {
                if (it.targetAmount > 0.0) (it.savedAmount / it.targetAmount).coerceIn(0.0, 1.0) else 1.0
            }.average()
            15.0 * avg
        }
        val debtOwed = loans.filter { it.kind == LoanKind.DEBT_OWED || it.kind == LoanKind.EMI }
            .sumOf { max(0.0, it.principalAmount - it.paidAmount) }
        val debtPoints = if (totalInc <= 0.0) 10.0 else {
            val ratio = (debtOwed / max(1.0, totalInc)).coerceIn(0.0, 1.0)
            15.0 * (1.0 - ratio)
        }
        val healthScore = (savingsPoints + budgetPoints + goalProgressAvg + debtPoints).roundToInt().coerceIn(0, 100)

        val insights = buildList {
            val isBn = language == AppLanguage.BANGLA
            if (totalInc > 0.0) {
                val srPct = ((totalInc - totalExp) / totalInc * 100.0).roundToInt()
                add(
                    if (isBn) "আপনার সামগ্রিক সঞ্চয়ের হার $srPct%।"
                    else "Your overall savings rate stands at $srPct%."
                )
            }
            if (highestCat != null) {
                val amtStr = CurrencyUtil.format(highestCat.amount, currencyCode)
                add(
                    if (isBn) "${highestCat.name} খাতে সর্বোচ্চ ব্যয় হয়েছে ($amtStr, ${highestCat.percentage.roundToInt()}%)।"
                    else "${highestCat.name} accounts for your highest spending ($amtStr, ${highestCat.percentage.roundToInt()}%)."
                )
            }
            if (predictedNextMonth > 0.0) {
                val predStr = CurrencyUtil.format(predictedNextMonth, currencyCode)
                add(
                    if (isBn) "সাম্প্রতিক ধারার ভিত্তিতে আগামী মাসে সম্ভাব্য ব্যয় প্রায় $predStr।"
                    else "Based on your 3-month trend, projected next-month spend is $predStr."
                )
            }
            if (debtOwed > 0.0) {
                val debtStr = CurrencyUtil.format(debtOwed, currencyCode)
                add(
                    if (isBn) "বকেয়া দেনা/EMI রয়েছে $debtStr — নিয়মিত কিস্তি পরিশোধ স্কোর বাড়াবে।"
                    else "Outstanding debt/EMI is $debtStr — steady repayments improve your health score."
                )
            }
            if (isEmpty()) {
                add(
                    if (isBn) "স্মার্ট বিশ্লেষণ দেখতে কয়েকটি আয় ও ব্যয়ের লেনদেন যোগ করুন।"
                    else "Log income and expense entries to unlock tailored spending insights."
                )
            }
        }

        val yearlyPoints = buildYearlyTrend(active, 4, now)

        return DeepAnalyticsSummary(
            highestSpendingMonth = highestSpendingMonth,
            lowestSpendingMonth = lowestSpendingMonth,
            highestIncomeMonth = highestIncomeMonth,
            highestExpenseCategory = highestCat,
            lowestExpenseCategory = lowestCat,
            avgDailyExpense = avgDaily,
            avgMonthlyExpense = avgMonthly,
            avgYearlyExpense = avgYearly,
            predictedNextMonthExpense = predictedNextMonth,
            financialHealthScore = healthScore,
            insights = insights,
            monthlyPoints = monthlyPoints,
            yearlyPoints = yearlyPoints,
            categoryBreakdowns = catBreakdowns
        )
    }

    private fun buildLastMonthsTrend(
        active: List<TransactionEntity>,
        months: Int,
        now: Long
    ): List<PeriodComparisonPoint> {
        val result = mutableListOf<PeriodComparisonPoint>()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, -(months - 1))
        for (i in 0 until months) {
            val key = DateTimeUtil.monthKey(cal.timeInMillis)
            val mTx = active.filter { DateTimeUtil.monthKey(it.timestamp) == key }
            val inc = mTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = mTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            result.add(
                PeriodComparisonPoint(
                    label = DateTimeUtil.formatMonthLabel(key),
                    income = inc,
                    expense = exp,
                    savings = max(0.0, inc - exp)
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    private fun buildDailyTrend(
        active: List<TransactionEntity>,
        days: Int,
        end: Long
    ): List<PeriodComparisonPoint> {
        val result = mutableListOf<PeriodComparisonPoint>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = DateTimeUtil.startOfToday(end)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        for (i in 0 until days) {
            val s = cal.timeInMillis
            val e = DateTimeUtil.endOfDay(s)
            val dTx = active.filter { it.timestamp in s..e }
            val inc = dTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = dTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val label = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}"
            result.add(PeriodComparisonPoint(label, inc, exp, max(0.0, inc - exp)))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun buildYearlyTrend(
        active: List<TransactionEntity>,
        years: Int,
        now: Long
    ): List<PeriodComparisonPoint> {
        val currentYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        val result = mutableListOf<PeriodComparisonPoint>()
        for (y in (currentYear - years + 1)..currentYear) {
            val yTx = active.filter {
                Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.YEAR) == y
            }
            val inc = yTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = yTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            result.add(PeriodComparisonPoint(y.toString(), inc, exp, max(0.0, inc - exp)))
        }
        return result
    }
}
