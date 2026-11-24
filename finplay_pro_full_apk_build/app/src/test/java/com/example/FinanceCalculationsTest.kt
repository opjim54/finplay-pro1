package com.example

import com.example.data.backup.ExportManager
import com.example.data.backup.FullBackupSnapshot
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.AiProviderKind
import com.example.data.local.FinanceProfileEntity
import com.example.data.local.KeepNoteEntity
import com.example.data.local.QuickTodoEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserEntity
import com.example.domain.AnalyticsEngine
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import com.example.domain.ReportPeriod
import com.example.domain.SmartToolsEngine
import com.example.domain.TextCaseMode
import com.example.security.CryptoUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculationsTest {

    @Test
    fun dashboardCalculationsAreExact() {
        val now = System.currentTimeMillis()
        val accounts = listOf(
            AccountEntity(id = 1, ownerUserId = 1, name = "Cash", type = AccountType.CASH, balance = 15000.0, initialBalance = 0.0),
            AccountEntity(id = 2, ownerUserId = 1, name = "Bkash", type = AccountType.BKASH, balance = 5000.0, initialBalance = 0.0)
        )
        val categories = listOf(
            CategoryEntity(id = 10, ownerUserId = 1, name = "Salary", type = TransactionType.INCOME),
            CategoryEntity(id = 11, ownerUserId = 1, name = "Food", type = TransactionType.EXPENSE),
            CategoryEntity(id = 12, ownerUserId = 1, name = "Transport", type = TransactionType.EXPENSE)
        )
        val txs = listOf(
            TransactionEntity(id = 1, ownerUserId = 1, type = TransactionType.INCOME, amount = 25000.0, categoryId = 10, accountId = 1, timestamp = now),
            TransactionEntity(id = 2, ownerUserId = 1, type = TransactionType.EXPENSE, amount = 4000.0, categoryId = 11, accountId = 1, timestamp = now),
            TransactionEntity(id = 3, ownerUserId = 1, type = TransactionType.EXPENSE, amount = 1000.0, categoryId = 12, accountId = 2, timestamp = now),
            TransactionEntity(id = 4, ownerUserId = 1, type = TransactionType.EXPENSE, amount = 999.0, categoryId = 11, accountId = 1, timestamp = now, isDeleted = true)
        )
        val budgets = listOf(
            BudgetEntity(id = 1, ownerUserId = 1, categoryId = 11, monthKey = DateTimeUtil.monthKey(now), limitAmount = 5000.0)
        )
        val goals = listOf(
            SavingsGoalEntity(id = 1, ownerUserId = 1, name = "Emergency", targetAmount = 50000.0, savedAmount = 12000.0, expectedDate = now + 86_400_000L)
        )

        val metrics = AnalyticsEngine.computeDashboard(accounts, categories, txs, budgets, goals, now)

        assertEquals(20000.0, metrics.totalBalance, 0.001)
        assertEquals(25000.0, metrics.totalIncome, 0.001)
        assertEquals(5000.0, metrics.totalExpense, 0.001)
        assertEquals(20000.0, metrics.monthlySavings, 0.001)
        assertEquals(4000.0, metrics.totalBudgetUsed, 0.001)
        assertEquals(0.8f, metrics.budgetProgress, 0.001f)
        assertEquals("Food", metrics.highestSpendingCategory?.name)
        assertEquals("Transport", metrics.lowestSpendingCategory?.name)
    }

    @Test
    fun reportComputesGrowthAndNet() {
        val now = System.currentTimeMillis()
        val cats = listOf(CategoryEntity(id = 1, ownerUserId = 1, name = "Salary", type = TransactionType.INCOME))
        val txs = listOf(
            TransactionEntity(id = 1, ownerUserId = 1, type = TransactionType.INCOME, amount = 10000.0, categoryId = 1, accountId = 1, timestamp = now)
        )
        val report = AnalyticsEngine.computeReport(ReportPeriod.MONTHLY, txs, cats, now = now)
        assertEquals(10000.0, report.income, 0.001)
        assertEquals(10000.0, report.netBalance, 0.001)
        assertEquals(100.0, report.growthRatePercent, 0.001)
    }

    @Test
    fun saltedHashVerificationWorks() {
        val salt = CryptoUtil.generateSalt()
        val hash = CryptoUtil.hashSecret("Jim", salt)
        assertTrue(CryptoUtil.verifySecret("Jim", salt, hash))
        assertFalse(CryptoUtil.verifySecret("jim", salt, hash))
    }

    @Test
    fun jsonSnapshotRoundTripPreservesAllRecords() {
        val snapshot = FullBackupSnapshot(
            ownerUserId = 2L,
            preferences = com.example.data.backup.BackupPreferencesSnapshot(
                languageCode = "en",
                themeMode = "DARK",
                currencyCode = "USD",
                connectedGmail = "test@gmail.com",
                autoBackupOnNewData = true,
                dailyAutoBackupEnabled = true
            ),
            users = listOf(
                UserEntity(id = 1, username = "user", passwordHash = "h", salt = "s", displayName = "Smart User"),
                UserEntity(id = 2, username = "abul", passwordHash = "h2", salt = "s2", displayName = "Abul Hasem")
            ),
            accounts = listOf(AccountEntity(id = 1, ownerUserId = 2, name = "Bkash", type = AccountType.BKASH, balance = 3500.0, initialBalance = 3500.0))
        )
        val json = ExportManager.snapshotToJson(snapshot)
        val restored = ExportManager.jsonToSnapshot(json).getOrThrow()
        assertEquals(2L, restored.ownerUserId)
        assertEquals("abul", restored.users.last().username)
        assertEquals("test@gmail.com", restored.preferences?.connectedGmail)
        assertEquals("USD", restored.preferences?.currencyCode)
        assertEquals(3500.0, restored.accounts.first().balance, 0.001)
        assertEquals("৳3,500", CurrencyUtil.format(3500.0, "BDT"))
    }

    @Test
    fun smartToolsTextFileAndCalculatorsWorkAccurately() {
        val sample = "Hello Smart Finance Life!\nSecond line."
        val stats = SmartToolsEngine.analyzeText(sample)
        assertEquals(6, stats.words)
        assertEquals(sample.length, stats.charsWithSpaces)
        assertEquals(sample.count { !it.isWhitespace() }, stats.charsWithoutSpaces)
        assertEquals(2, stats.lines)

        assertEquals("HELLO", SmartToolsEngine.convertCase("Hello", TextCaseMode.UPPER))
        assertEquals("hello world", SmartToolsEngine.removeExtraSpaces("  hello   world  "))
        assertEquals("dlrow olleh", SmartToolsEngine.reverseText("hello world", byWordsOnly = false))
        assertEquals("world hello", SmartToolsEngine.reverseText("hello world", byWordsOnly = true))

        val jsonOut = SmartToolsEngine.csvToJson("name,qty\nRice,10").getOrThrow()
        assertTrue(jsonOut.contains("\"name\": \"Rice\""))
        assertTrue(jsonOut.contains("\"qty\": 10"))

        val pretty = SmartToolsEngine.formatJson("{\"a\":1}", minify = false).getOrThrow()
        assertTrue(pretty.contains("\n"))

        val bmi = SmartToolsEngine.calculateBmi(70.0, 175.0)
        assertEquals(22.9, bmi.bmi, 0.1)
        assertEquals("Normal Weight", bmi.category)

        val emi = SmartToolsEngine.calculateEmi(100000.0, 12.0, 12)
        assertEquals(8884.88, emi.monthlyEmi, 1.0)
        assertEquals(22.046, SmartToolsEngine.convertUnit(10.0, "KG_TO_LB"), 0.01)
    }

    @Test
    fun fullBackupSnapshotV3PreservesProfilesNotesTasksAiAndTodos() {
        val snap = FullBackupSnapshot(
            ownerUserId = 1L,
            financeProfiles = listOf(FinanceProfileEntity(id = 2, ownerUserId = 1, name = "Farm")),
            keepNotes = listOf(KeepNoteEntity(id = 3, ownerUserId = 1, title = "Note", content = "Hello world")),
            taskItems = listOf(TaskItemEntity(id = 4, ownerUserId = 1, title = "Pay bill", priority = TaskPriority.URGENT)),
            aiProviders = listOf(
                AiProviderEntity(id = 5, ownerUserId = 1, name = "Gemini", providerKind = AiProviderKind.GEMINI, baseUrl = "https://generativelanguage.googleapis.com/v1beta", apiKey = "AIzaTest123", modelName = "gemini-2.5-flash")
            ),
            quickTodos = listOf(QuickTodoEntity(id = 6, ownerUserId = 1, title = "Call bank"))
        )
        val raw = ExportManager.snapshotToJson(snap)
        val restored = ExportManager.jsonToSnapshot(raw).getOrThrow()
        assertEquals("Farm", restored.financeProfiles.first().name)
        assertEquals("Note", restored.keepNotes.first().title)
        assertEquals(TaskPriority.URGENT, restored.taskItems.first().priority)
        assertEquals("AIzaTest123", restored.aiProviders.first().apiKey)
        assertEquals("Call bank", restored.quickTodos.first().title)
    }
}
