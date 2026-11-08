package com.charan.fintrack.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charan.fintrack.data.NativeLedgerCalculator
import com.charan.fintrack.data.local.FinTrackDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeBackupServiceInstrumentedTest {
    private lateinit var database: FinTrackDatabase
    private lateinit var service: NativeBackupService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
        service = NativeBackupService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun legacyMergeReconcilesCurrentBalancesAndIsIdempotent() = runBlocking {
        val backup = """
            {
              "backupMetadata":{"app":"FinTrack","schemaVersion":1},
              "accounts":[
                {"id":"cash-1","name":"Cash","type":"wallet","balance":1350.00,"currency":"INR"},
                {"id":"bank-1","name":"Bank","type":"bank","balance":200.00,"currency":"INR"}
              ],
              "transactions":[
                {"id":"t-income","type":"income","amount":500.00,"accountId":"cash-1","category":"salary","date":"2026-11-08","time":"09:00"},
                {"id":"t-expense","type":"expense","amount":50.00,"accountId":"cash-1","category":"food","date":"2026-11-08","time":"10:00"},
                {"id":"t-transfer","type":"transfer","amount":100.00,"accountId":"cash-1","toAccountId":"bank-1","date":"2026-11-08","time":"11:00"}
              ],
              "persons":[],
              "categories":[{"id":"salary","name":"Salary","type":"income","icon":"₹","subcategories":["Pay"]},{"id":"food","name":"Food","type":"expense","icon":"🍲","subcategories":["Lunch"]}],
              "budgets":[{"id":"budget-food","name":"Food","amount":400.00,"period":"Monthly"}],
              "goals":[{"id":"goal-trip","name":"Trip","target":1000.00,"saved":100.00,"contributions":[{"amount":25.00,"date":"2026-11-07","createdAt":"2026-11-07T12:00:00.000Z"}]}],
              "loans":[{"id":"loan-phone","name":"Phone","principal":500.00,"paid":50.00,"emi":25.00,"repayments":[{"amount":50.00,"date":"2026-11-06","createdAt":"2026-11-06T12:00:00.000Z"}]}]
            }
        """.trimIndent()

        val first = service.mergeJson(backup)
        assertEquals(2, first.accountsAdded)
        assertEquals(4, first.transactionsAdded) // one transfer becomes two ledger rows
        val accounts = database.accountDao().allAccounts()
        val rows = database.financeDao().allTransactions()
        val cash = accounts.single { it.name == "Cash" }
        val bank = accounts.single { it.name == "Bank" }

        assertEquals(100_000L, cash.openingBalancePaise)
        assertEquals(10_000L, bank.openingBalancePaise)
        assertEquals(135_000L, NativeLedgerCalculator.accountBalancePaise(cash, rows))
        assertEquals(20_000L, NativeLedgerCalculator.accountBalancePaise(bank, rows))
        assertEquals(4, rows.size)
        assertEquals(1, database.financeDao().allGoalContributions().size)
        assertEquals(1, database.financeDao().allLoanPayments().size)

        val second = service.mergeJson(backup)
        assertEquals(0, second.totalAdded)
        assertEquals(4, database.financeDao().allTransactions().size)
        assertEquals(1, database.financeDao().allGoalContributions().size)
        assertEquals(1, database.financeDao().allLoanPayments().size)
        assertTrue(second.duplicatesSkipped > 0)
    }

    @Test
    fun nativeExportCanBeMergedIntoAnotherEmptyDatabase() = runBlocking {
        val legacy = """
            {
              "accounts":[{"id":"a","name":"Wallet","type":"wallet","balance":88.25}],
              "transactions":[{"id":"t","type":"income","amount":8.25,"accountId":"a","category":"Gift","date":"2026-11-08"}],
              "persons":[],"categories":[],"budgets":[],"goals":[],"loans":[]
            }
        """.trimIndent()
        service.mergeJson(legacy)
        val nativeBackup = service.exportJson()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val destination = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
        try {
            val restored = NativeBackupService(destination).mergeJson(nativeBackup)
            assertEquals(1, restored.accountsAdded)
            assertEquals(1, restored.transactionsAdded)
            val restoredAccount = destination.accountDao().allAccounts().single()
            val restoredTransactions = destination.financeDao().allTransactions()
            assertEquals(8_000L, restoredAccount.openingBalancePaise)
            assertEquals(8_825L, NativeLedgerCalculator.accountBalancePaise(restoredAccount, restoredTransactions))
        } finally {
            destination.close()
        }
    }
}
