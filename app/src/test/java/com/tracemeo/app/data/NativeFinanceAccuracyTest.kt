package com.tracemeo.app.data

import com.tracemeo.app.DailyReminderReceiver
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.GoalContributionEntity
import com.tracemeo.app.data.local.GoalEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.LoanPaymentEntity
import com.tracemeo.app.data.local.PersonEntity
import com.tracemeo.app.data.local.TransactionEntity
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

    @Test
    fun inlineMathParserEvaluatesSafeArithmeticExpressions() {
        assertEquals(195.0, NativeLedgerCalculator.evaluateInlineMath("120+45+30") ?: 0.0, 0.001)
        assertEquals(150.0, NativeLedgerCalculator.evaluateInlineMath("50*3") ?: 0.0, 0.001)
        assertEquals(50.0, NativeLedgerCalculator.evaluateInlineMath("(100+50)/3=") ?: 0.0, 0.001)
        assertEquals("195", NativeLedgerCalculator.formatInlineMathResult("120+45+30"))
    }

    @Test
    fun spendingDistributionAndIncomeSourceSplitComputeAccurately() {
        val now = System.currentTimeMillis()
        val txns = listOf(
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 600.0, category = "Food & Chai", timestamp = now),
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 400.0, category = "Transport & Fuel", timestamp = now),
            TransactionEntity(accountId = 1, type = "INCOME", amount = 3000.0, category = "Family & Income", subcategory = "Family Allowance", timestamp = now),
            TransactionEntity(accountId = 1, type = "INCOME", amount = 2000.0, category = "Family & Income", subcategory = "Own Earnings", timestamp = now)
        )
        val dist = NativeLedgerCalculator.computeSpendingDistribution(
            transactions = txns,
            categories = emptyList(),
            window = SpendingChartWindow.WEEKLY,
            nowMillis = now
        )
        assertEquals(1000.0, dist.totalSpent, 0.001)
        assertEquals(2, dist.slices.size)
        assertEquals("Food & Chai", dist.slices.first().category)
        assertEquals(60f, dist.slices.first().percentage, 0.1f)

        val split = NativeLedgerCalculator.computeIncomeSourceSplit(txns)
        assertEquals(3000.0, split.familyIncome, 0.001)
        assertEquals(2000.0, split.ownEarnings, 0.001)
        assertEquals(60f, split.familyPct, 0.1f)
        assertEquals(40f, split.ownPct, 0.1f)
    }

    @Test
    fun toolsHubCatalogContainsAllTwentyFourUtilityTools() {
        val ids = com.tracemeo.app.ui.nativeapp.ALL_TOOLS_CATALOG.map { it.id }.toSet()
        assertTrue(ids.contains("khata"))
        assertTrue(ids.contains("recharge"))
        assertTrue(ids.contains("refunds"))
        assertTrue(ids.contains("wishlist"))
        assertTrue(ids.contains("cashback"))
        assertTrue(ids.contains("price_per_use"))
        assertTrue(ids.contains("round_up"))
        assertTrue(ids.contains("runway"))
        assertTrue(ids.contains("debt_planner"))
        assertTrue(ids.contains("warranty_vault"))
        assertEquals(24, ids.size)
    }

    @Test
    fun kidSimpleAppGuideFeaturesIncludeExamplesAndValidRoutes() {
        val features = com.tracemeo.app.ui.nativeapp.ALL_GUIDE_FEATURES
        assertTrue(features.size >= 30)
        features.forEach { item ->
            assertTrue("Missing whatItDoes on ${item.title}", item.whatItDoes.isNotBlank())
            assertTrue("Missing howToUse on ${item.title}", item.howToUse.isNotBlank())
            assertTrue("Missing kidExample on ${item.title}", item.kidExample.isNotBlank())
            assertTrue("Missing route on ${item.title}", item.route.isNotBlank())
        }
    }
}

