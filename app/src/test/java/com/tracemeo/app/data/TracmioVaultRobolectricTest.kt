package com.tracemeo.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tracemeo.app.DailyReminderReceiver
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.BudgetEntity
import com.tracemeo.app.data.local.GoalEntity
import com.tracemeo.app.data.local.KhataEntryEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.SubscriptionEntity
import com.tracemeo.app.data.local.TracmioDatabase
import com.tracemeo.app.data.local.TransactionEntity
import com.tracemeo.app.data.repository.FinanceRepository
import com.tracemeo.app.data.security.AppLockManager
import com.tracemeo.app.data.services.NativeBackupService
import com.tracemeo.app.ui.nativeapp.LiquidGlassSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TracmioVaultRobolectricTest {
    @Test
    fun notificationCatalogHas100ValidItems() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawJson = context.assets.open("notifications.json").bufferedReader().use { it.readText() }
        val parsed = DailyReminderReceiver.parseNotificationCatalogJson(rawJson)
        assertEquals(100, parsed.size)
        assertTrue(parsed.all { it.title.isNotBlank() && it.body.isNotBlank() })
    }

    @Test
    fun appLockEnableVerifyAndRecoveryWork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lockMgr = AppLockManager(context)
        val recoveryKey = lockMgr.enableLock("2580", biometricEnabled = true, "Pet?", "Rocky")
        assertTrue(recoveryKey.startsWith("TRAC-"))
        assertTrue(lockMgr.verifyPin("2580"))
        assertFalse(lockMgr.verifyPin("0000"))

        assertTrue(lockMgr.verifyRecoveryAndReset("rocky", "1357"))
        assertTrue(lockMgr.verifyPin("1357"))
        assertTrue(lockMgr.verifyRecoveryAndReset(recoveryKey, null))
        assertFalse(lockMgr.getState().isEnabled)
    }

    @Test
    fun roomDatabaseBackupAndRestoreRoundtripWorks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TracmioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = FinanceRepository(db)
            val accId = repo.insertAccount(AccountEntity(name = "Primary Bank", accountType = "Bank", balance = 5000.0, balancePaise = 500_000L))
            val txId = repo.insertTransaction(
                TransactionEntity(
                    accountId = accId,
                    type = "EXPENSE",
                    amount = 150.0,
                    amountPaise = 15_000L,
                    category = "Food & Chai",
                    note = "Lunch #office",
                    timestamp = 1_700_000_000_000L
                )
            )
            repo.insertBudget(BudgetEntity(category = "Food & Chai", monthlyLimit = 4000.0))
            repo.insertGoal(GoalEntity(name = "Laptop", title = "Laptop", targetAmount = 50000.0, currentAmount = 5000.0))
            repo.insertLoan(LoanEntity(name = "Bike", principal = 25000.0, interestRate = 9.5, monthlyPayment = 1800.0))
            repo.insertKhataEntry(KhataEntryEntity(shopOrFriend = "Raju Kirana", itemDescription = "Groceries", amount = 320.0))
            repo.insertSubscription(SubscriptionEntity(name = "Jio 5G", amount = 299.0, billingCycle = "28d", isSimOrWifi = true, validityDays = 28))

            val fetched = repo.getTransactionById(txId)
            assertNotNull(fetched)
            assertEquals(1_700_000_000_000L, fetched!!.timestamp)

            val health = repo.runDatabaseHealthCheck(1, 1, 0)
            assertTrue(health.isHealthy)

            val uiState = NativeTracmioUiState(
                accounts = repo.accounts.first(),
                transactions = repo.transactions.first(),
                budgets = repo.budgets.first(),
                goals = repo.goals.first(),
                loans = repo.loans.first(),
                khataEntries = repo.khataEntries.first(),
                subscriptions = repo.subscriptions.first(),
                pinnedToolIds = listOf("khata", "cash_counter")
            )
            val backupJson = NativeBackupService.buildFullBackupJson(uiState, LiquidGlassSettings())
            assertTrue(backupJson.contains("Primary Bank"))
            assertTrue(backupJson.contains("Lunch #office"))
            assertTrue(backupJson.contains("Raju Kirana"))
            assertTrue(backupJson.contains("Jio 5G"))
            assertTrue(backupJson.contains("cash_counter"))

            val importedCount = repo.importBackupJson(backupJson)
            assertEquals(7, importedCount)
            assertEquals(2, repo.accounts.first().size)
            assertEquals(2, repo.transactions.first().size)
            assertEquals(2, repo.budgets.first().size)
            assertEquals(2, repo.goals.first().size)
            assertEquals(2, repo.loans.first().size)
            assertEquals(2, repo.khataEntries.first().size)
            assertEquals(2, repo.subscriptions.first().size)
        } finally {
            db.close()
        }
    }
}
