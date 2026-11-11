package com.charan.tracmio.data

import com.charan.tracmio.DailyReminderReceiver
import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.GoalContributionEntity
import com.charan.tracmio.data.local.GoalEntity
import com.charan.tracmio.data.local.LoanEntity
import com.charan.tracmio.data.local.LoanPaymentEntity
import com.charan.tracmio.data.local.PersonEntity
import com.charan.tracmio.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class NativeFinanceAccuracyTest {

    @Test
    fun moneyPaiseRoundTripsWithoutFloatingPointDrift() {
        assertEquals(19999L, MoneyPaise.parsePaise("₹199.99"))
        assertEquals(199.99, MoneyPaise.toRupees(19999L), 0.0001)
        assertEquals("199.99", MoneyPaise.formatInput(19999L))
    }

    @Test
    fun nativeLedgerCalculatorComputesExactPaiseBalances() {
        val acc1 = AccountEntity(id = 1, name = "HDFC", accountType = "Bank", balance = 1000.0, balancePaise = 100_000L)
        val acc2 = AccountEntity(id = 2, name = "Cash", accountType = "Cash", balance = 500.0, balancePaise = 50_000L)
        val txns = listOf(
            TransactionEntity(accountId = 1, type = "INCOME", amount = 250.50, amountPaise = 25_050L, category = "Salary"),
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 50.25, amountPaise = 5_025L, category = "Food", paymentMethod = "UPI", upiApp = "GPay"),
            TransactionEntity(accountId = 1, type = "TRANSFER_OUT", amount = 100.0, amountPaise = 10_000L, category = "Transfer", transferAccountId = 2),
            TransactionEntity(accountId = 2, type = "TRANSFER_IN", amount = 100.0, amountPaise = 10_000L, category = "Transfer", transferAccountId = 1)
        )

        assertEquals(110_025L, NativeLedgerCalculator.accountBalancePaise(acc1, txns))
        assertEquals(60_000L, NativeLedgerCalculator.accountBalancePaise(acc2, txns))
        assertEquals(1700.25, NativeLedgerCalculator.totalBalance(listOf(acc1, acc2), txns), 0.001)
    }

    @Test
    fun goalAndLoanTotalsIncludeContributionsAndPayments() {
        val goal = GoalEntity(id = 10, name = "Emergency Fund", targetAmount = 1000.0, savedAmount = 100.0)
        val contribs = listOf(
            GoalContributionEntity(goalId = 10, amount = 150.0),
            GoalContributionEntity(goalId = 10, amount = 250.0)
        )
        assertEquals(40_000L, NativeLedgerCalculator.goalSavedPaise(goal, contribs))

        val loan = LoanEntity(id = 20, name = "Bike Loan", principal = 5000.0, paidAmount = 500.0)
        val payments = listOf(
            LoanPaymentEntity(loanId = 20, amount = 1000.0),
            LoanPaymentEntity(loanId = 20, amount = 500.0)
        )
        assertEquals(150_000L, NativeLedgerCalculator.loanPaidPaise(loan, payments))
        assertEquals(350_000L, NativeLedgerCalculator.loanRemainingPaise(loan, payments))
    }

    @Test
    fun personSummariesTrackBorrowAndLendNetBalances() {
        val person = PersonEntity(id = 5, name = "Arjun")
        val txns = listOf(
            TransactionEntity(accountId = 1, type = "LEND", amount = 500.0, category = "Lent", personId = 5),
            TransactionEntity(accountId = 1, type = "BORROW", amount = 200.0, category = "Borrowed", personId = 5)
        )
        val summaries = NativeLedgerCalculator.personSummaries(listOf(person), txns)
        assertEquals(1, summaries.size)
        assertEquals(300.0, summaries.first().net, 0.001)
    }

    @Test
    fun legacyMigrationGuardProtectsExistingDatabases() {
        assertTrue(
            LegacyMigrationGuard.shouldImportLegacyJson(
                alreadyMigrated = false,
                existingAccountCount = 0,
                existingTransactionCount = 0,
                legacyJson = """{"accounts":[],"transactions":[]}"""
            )
        )
        assertFalse(
            LegacyMigrationGuard.shouldImportLegacyJson(
                alreadyMigrated = true,
                existingAccountCount = 0,
                existingTransactionCount = 0,
                legacyJson = """{"accounts":[],"transactions":[]}"""
            )
        )
        assertFalse(
            LegacyMigrationGuard.shouldImportLegacyJson(
                alreadyMigrated = false,
                existingAccountCount = 1,
                existingTransactionCount = 0,
                legacyJson = """{"accounts":[],"transactions":[]}"""
            )
        )
    }

    @Test
    fun xlsxExporterGeneratesValidOpenXmlZipWithWorksheets() {
        val json = """
            {
              "accounts": [{"id": 1, "name": "HDFC Bank", "type": "Bank", "balance": 15000, "currency": "INR"}],
              "transactions": [{"id": 101, "date": "2025-05-01", "time": "14:30", "type": "expense", "category": "Food", "accountId": 1, "paymentMethod": "UPI", "upiApp": "PhonePe", "amount": 240.5, "note": "Lunch"}]
            }
        """.trimIndent()
        val bytes = XlsxExporter.create(json)
        assertTrue(bytes.isNotEmpty())

        val entries = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries += entry.name
                entry = zis.nextEntry
            }
        }
        assertTrue(entries.contains("[Content_Types].xml"))
        assertTrue(entries.contains("xl/workbook.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet2.xml"))
    }

    @Test
    fun dailyReminderReceiverContainsSixMemeAndThreeStandardMessages() {
        assertEquals(6, DailyReminderReceiver.MEME_MESSAGES.size)
        assertEquals(3, DailyReminderReceiver.STANDARD_MESSAGES.size)
        assertEquals("tracmio_daily", DailyReminderReceiver.CHANNEL_ID)
    }
}
