package com.charan.fintrack.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.NativeLedgerCalculator
import com.charan.fintrack.data.local.FinTrackDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject

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
                {"id":"t-expense","type":"expense","amount":50.00,"accountId":"cash-1","category":"food","subcategory":"Restaurant","date":"2026-11-08","time":"10:00"},
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
        assertEquals("Restaurant", rows.single { it.type == "EXPENSE" }.subcategory)
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
              "accounts":[
                {"id":"a","name":"Wallet","type":"wallet","balance":83.25},
                {"id":"b","name":"Bank","type":"bank","balance":25.00}
              ],
              "transactions":[
                {"id":"t","type":"income","amount":8.25,"accountId":"a","category":"Gift","subcategory":"Bonus","date":"2026-11-08"},
                {"id":"move","type":"transfer","amount":5.00,"accountId":"a","toAccountId":"b","date":"2026-11-08"}
              ],
              "persons":[],"categories":[],"budgets":[],"goals":[],"loans":[]
            }
        """.trimIndent()
        service.mergeJson(legacy)
        val nativeBackup = service.exportJson()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val destination = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
        try {
            val restored = NativeBackupService(destination).mergeJson(nativeBackup)
            assertEquals(2, restored.accountsAdded)
            assertEquals(3, restored.transactionsAdded)
            val restoredAccounts = destination.accountDao().allAccounts()
            val restoredTransactions = destination.financeDao().allTransactions()
            val wallet = restoredAccounts.single { it.name == "Wallet" }
            val bank = restoredAccounts.single { it.name == "Bank" }
            assertEquals(80_000L, wallet.openingBalancePaise)
            assertEquals(20_000L, bank.openingBalancePaise)
            assertEquals(8_325L, NativeLedgerCalculator.accountBalancePaise(wallet, restoredTransactions))
            assertEquals("Bonus", restoredTransactions.single { it.type == "INCOME" }.subcategory)
            assertEquals(2_500L, NativeLedgerCalculator.accountBalancePaise(bank, restoredTransactions))
            assertEquals(1, restoredTransactions.count { it.type == "TRANSFER_OUT" })
            assertEquals(1, restoredTransactions.count { it.type == "TRANSFER_IN" })
        } finally {
            destination.close()
        }
    }

    @Test
    fun nativeRestoreRejectsIncompleteTransferPairWithoutWritingPartialData() = runBlocking {
        val backup = JSONObject()
            .put("format", NativeBackupService.FORMAT)
            .put("schemaVersion", NativeBackupService.SCHEMA_VERSION)
            .put("accounts", JSONArray().put(
                JSONObject().put("id", "cash").put("name", "Cash").put("accountType", "Wallet")
                    .put("openingBalancePaise", 10_000).put("stableUid", "account:cash")
            ))
            .put("transactions", JSONArray().put(
                JSONObject().put("id", "transfer-out").put("type", "TRANSFER_OUT")
                    .put("accountId", "cash").put("transferAccountId", "missing-account")
                    .put("transferId", "broken-transfer").put("amountPaise", 2_000)
                    .put("stableUid", "transaction:transfer-out").put("category", "Transfer")
            ))

        var rejected = false
        try {
            service.mergeJson(backup.toString())
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Incomplete transfer backup should be rejected", rejected)
        assertTrue("Restore must be atomic and leave no partial accounts", database.accountDao().allAccounts().isEmpty())
        assertTrue("Restore must not leave partial transactions", database.financeDao().allTransactions().isEmpty())
    }

    @Test
    fun csvExportWritesTransferOnceWithDestinationAccount() = runBlocking {
        val repository = com.charan.fintrack.data.repository.NativeFinanceRepository(database)
        val sourceId = repository.addAccount("Cash", "Wallet", MoneyPaise.fromRupees("500"))
        val destinationId = repository.addAccount("Bank", "Bank", MoneyPaise.ZERO)
        repository.addTransfer(sourceId, destinationId, MoneyPaise.fromRupees("100"), "Move funds")

        val csv = service.exportCsv()
        val rows = csv.lineSequence().filter { it.isNotBlank() }.toList()
        assertEquals(2, rows.size)
        assertTrue(rows[1].contains("TRANSFER"))
        assertTrue(rows[1].contains(destinationId.toString()))
        assertTrue(!csv.contains("TRANSFER_IN"))
    }

    @Test
    fun csvExportNeutralizesFormulaLikeUserText() = runBlocking {
        val repository = com.charan.fintrack.data.repository.NativeFinanceRepository(database)
        val accountId = repository.addAccount("Cash", "Wallet", MoneyPaise.fromRupees("100"))
        repository.saveTransaction(
            accountId = accountId,
            type = "EXPENSE",
            amount = MoneyPaise.fromRupees("1"),
            category = "Other",
            note = "  =HYPERLINK(\"example\",\"open\")"
        )
        val csv = service.exportCsv()
        assertTrue(csv.contains("\"'  =HYPERLINK"))
    }
}
