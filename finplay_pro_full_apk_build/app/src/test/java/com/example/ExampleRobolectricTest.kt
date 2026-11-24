package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Finance Life", appName)
  }

  @Test
  fun `bilingual app strings resolve cleanly`() {
    val bn = stringsFor(AppLanguage.BANGLA)
    val en = stringsFor(AppLanguage.ENGLISH)
    assertTrue(bn.appTitle.isNotBlank())
    assertEquals("Smart Finance Life", en.appTitle)
    assertEquals("Home", en.navDashboard)
  }

  @Test
  fun `main activity launches login screen without crash`() {
    composeTestRule.onNodeWithTag("app_logo_admin_trigger").assertExists()
    composeTestRule.onNodeWithTag("login_username_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `default web client id is generated from google services`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    assertTrue(resId != 0)
    assertTrue(context.getString(resId).endsWith(".apps.googleusercontent.com"))
  }

  @Test
  fun `new phone snapshot restore recovers user accounts transactions and preferences`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.FinanceRepository(context)
    val snapshot = com.example.data.backup.FullBackupSnapshot(
      ownerUserId = 2L,
      preferences = com.example.data.backup.BackupPreferencesSnapshot(
        languageCode = "en",
        currencyCode = "USD",
        connectedGmail = "abulha1975shem1975@gmail.com",
        autoBackupOnNewData = true,
        dailyAutoBackupEnabled = true
      ),
      users = listOf(
        com.example.data.local.UserEntity(id = 1L, username = "user", passwordHash = "h1", salt = "s1", displayName = "Default"),
        com.example.data.local.UserEntity(id = 2L, username = "abul", passwordHash = "h2", salt = "s2", displayName = "Abul Hasem")
      ),
      accounts = listOf(
        com.example.data.local.AccountEntity(
          id = 10L,
          ownerUserId = 2L,
          name = "Bkash",
          type = com.example.data.local.AccountType.BKASH,
          balance = 12500.0,
          initialBalance = 12500.0
        )
      ),
      transactions = listOf(
        com.example.data.local.TransactionEntity(
          id = 100L,
          ownerUserId = 2L,
          type = com.example.data.local.TransactionType.INCOME,
          amount = 12500.0,
          categoryId = 1L,
          accountId = 10L,
          note = "Salary"
        )
      )
    )
    repo.restoreFullDatabaseSnapshot(snapshot)
    val restoredPrefs = repo.prefs.getSnapshot()
    assertEquals(2L, restoredPrefs.currentUserId)
    assertEquals("USD", restoredPrefs.currencyCode)
    assertEquals(AppLanguage.ENGLISH, restoredPrefs.language)
    assertEquals("abulha1975shem1975@gmail.com", restoredPrefs.connectedGmail)
    val accounts = repo.adminGetAllAccountsOnce()
    assertEquals(1, accounts.size)
    assertEquals(12500.0, accounts.first().balance, 0.001)

    // Clean up shared DataStore/session state for subsequent tests
    repo.logout()
    repo.prefs.clearDriveConnection()
    repo.prefs.setPin(false)
    repo.prefs.setLanguage(AppLanguage.BANGLA)
    repo.prefs.setCurrencyCode("BDT")
  }

  @Test
  fun `settings reset zeroes balances and clears ledger while preserving accounts and categories`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.FinanceRepository(context)
    repo.ensureInitialized()
    val authResult = repo.authenticateUser("user", "1", remember = true)
    assertTrue(authResult.isSuccess)
    val userId = authResult.getOrThrow().id

    // Add a custom account, custom category, income transaction, and savings goal
    repo.saveAccount(
      com.example.data.local.AccountEntity(
        ownerUserId = userId,
        name = "Custom Wallet",
        type = com.example.data.local.AccountType.OTHER,
        balance = 0.0,
        initialBalance = 2000.0
      )
    )
    repo.saveCategory(
      com.example.data.local.CategoryEntity(
        ownerUserId = userId,
        name = "Bonus",
        type = com.example.data.local.TransactionType.INCOME,
        iconKey = "work",
        colorHex = 0xFF059669L
      )
    )
    val accountsBefore = repo.adminGetAllAccountsOnce().filter { it.ownerUserId == userId }
    val customAcc = accountsBefore.first { it.name == "Custom Wallet" }
    val catsBefore = repo.adminGetAllCategoriesOnce().filter { it.ownerUserId == userId }
    val bonusCat = catsBefore.first { it.name == "Bonus" }

    repo.saveTransaction(
      com.example.data.local.TransactionEntity(
        ownerUserId = userId,
        type = com.example.data.local.TransactionType.INCOME,
        amount = 5000.0,
        categoryId = bonusCat.id,
        accountId = customAcc.id,
        note = "Test Income"
      )
    )
    repo.saveSavingsGoal(
      com.example.data.local.SavingsGoalEntity(
        ownerUserId = userId,
        name = "Laptop",
        targetAmount = 80000.0,
        savedAmount = 10000.0,
        expectedDate = System.currentTimeMillis() + 86_400_000L
      )
    )

    // Verify password & 6-digit PIN verification
    val salt = com.example.security.CryptoUtil.generateSalt()
    repo.prefs.setPin(
      enabled = true,
      hash = com.example.security.CryptoUtil.hashSecret("123456", salt),
      salt = salt,
      pinLength = 6
    )
    assertEquals(6, repo.prefs.getSnapshot().pinLength)
    assertTrue(repo.verifyResetSecret(userId, "1"))
    assertTrue(repo.verifyResetSecret(userId, "123456"))
    assertTrue(!repo.verifyResetSecret(userId, "wrong_pass"))

    // Reset all user financial data
    repo.resetUserFinanceData(userId)

    val accountsAfter = repo.adminGetAllAccountsOnce().filter { it.ownerUserId == userId }
    val categoriesAfter = repo.adminGetAllCategoriesOnce().filter { it.ownerUserId == userId }
    val txsAfter = repo.adminGetAllTransactionsOnce().filter { it.ownerUserId == userId }

    assertTrue(accountsAfter.any { it.name == "Custom Wallet" })
    assertTrue(accountsAfter.all { it.balance == 0.0 && it.initialBalance == 0.0 })
    assertTrue(categoriesAfter.any { it.name == "Bonus" })
    assertEquals(0, txsAfter.size)

    // Clean up shared DataStore/session state for subsequent tests
    repo.logout()
    repo.prefs.clearDriveConnection()
    repo.prefs.setPin(false)
    repo.prefs.setLanguage(AppLanguage.BANGLA)
    repo.prefs.setCurrencyCode("BDT")
  }

  @Test
  fun `multi profile creation isolates finance and backups include profiles notes tasks and ai keys`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.FinanceRepository(context)
    repo.ensureInitialized()
    val userId = repo.authenticateUser("user", "1", remember = true).getOrThrow().id

    val farmProfileId = repo.createFinanceProfile(userId, "Farm", "farm", 0xFF047857L, "Agriculture Ledger")
    val familyProfileId = repo.createFinanceProfile(userId, "Family", "family", 0xFFD97706L, "Family Expenses")

    val farmAccounts = repo.observeAccounts(userId, farmProfileId).first()
    val familyAccounts = repo.observeAccounts(userId, familyProfileId).first()
    assertTrue(farmAccounts.isNotEmpty())
    assertTrue(familyAccounts.isNotEmpty())

    repo.saveKeepNote(com.example.data.local.KeepNoteEntity(ownerUserId = userId, title = "Seed Order", content = "Order 50kg rice seed"))
    repo.saveTaskItem(com.example.data.local.TaskItemEntity(ownerUserId = userId, title = "Pay Fertilizer Bill", priority = com.example.data.local.TaskPriority.HIGH))
    repo.saveAiProvider(
      com.example.data.local.AiProviderEntity(
        ownerUserId = userId,
        name = "xKiro",
        providerKind = com.example.data.local.AiProviderKind.XKIRO,
        baseUrl = "https://api.xkiro.com/v1",
        apiKey = "xkiro-test-key",
        modelName = "openai/gpt-5.6-sol",
        isDefault = true
      )
    )

    val snapshot = repo.buildFullDatabaseSnapshot()
    assertTrue(snapshot.financeProfiles.any { it.name == "Farm" })
    assertTrue(snapshot.financeProfiles.any { it.name == "Family" })
    assertTrue(snapshot.keepNotes.any { it.title == "Seed Order" })
    assertTrue(snapshot.taskItems.any { it.title == "Pay Fertilizer Bill" })
    assertTrue(snapshot.aiProviders.any { it.apiKey == "xkiro-test-key" })

    repo.logout()
    repo.prefs.clearDriveConnection()
    repo.prefs.setPin(false)
    repo.prefs.setLanguage(AppLanguage.BANGLA)
    repo.prefs.setCurrencyCode("BDT")
  }
}
