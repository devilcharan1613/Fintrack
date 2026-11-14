package com.tracemeo.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tracemeo.app.DailyReminderReceiver
import com.tracemeo.app.data.local.AccountEntity
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
            repo.insertTransaction(TransactionEntity(accountId = accId, type = "EXPENSE", amount = 150.0, amountPaise = 15_000L, category = "Food & Chai", note = "Lunch #office"))

            val health = repo.runDatabaseHealthCheck(1, 1, 0)
            assertTrue(health.isHealthy)

            val uiState = NativeTracmioUiState(
                accounts = repo.accounts.first(),
                transactions = repo.transactions.first()
            )
            val backupJson = NativeBackupService.buildFullBackupJson(uiState, LiquidGlassSettings())
            assertTrue(backupJson.contains("Primary Bank"))
            assertTrue(backupJson.contains("Lunch #office"))

            val importedCount = repo.importBackupJson(backupJson)
            assertEquals(2, importedCount)
            assertEquals(2, repo.accounts.first().size)
            assertEquals(2, repo.transactions.first().size)
        } finally {
            db.close()
        }
    }
}
