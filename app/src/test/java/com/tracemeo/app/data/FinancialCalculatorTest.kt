package com.tracemeo.app.data

import com.tracemeo.app.DailyReminderReceiver
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FinancialCalculatorTest {
    @Test
    fun balanceIncludesIncomeExpensesAndTransfers() {
        val account = AccountEntity(id = 7, name = "Checking", accountType = "Bank", balance = 100.0)
        val transactions = listOf(
            TransactionEntity(accountId = 7, type = "INCOME", amount = 250.0, category = "Salary"),
            TransactionEntity(accountId = 7, type = "EXPENSE", amount = 40.0, category = "Food"),
            TransactionEntity(accountId = 7, type = "TRANSFER_IN", amount = 75.0, category = "Transfer"),
            TransactionEntity(accountId = 9, type = "EXPENSE", amount = 999.0, category = "Other")
        )

        assertEquals(385.0, FinancialCalculator.balance(account, transactions), 0.001)
    }

    @Test
    fun netCashFlowSubtractsExpenses() {
        assertEquals(420.0, FinancialCalculator.netCashFlow(900.0, 480.0), 0.001)
    }

    @Test
    fun categoryTotalHonoursDateWindow() {
        val transactions = listOf(
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 30.0, category = "Food", timestamp = 100),
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 20.0, category = "Food", timestamp = 200),
            TransactionEntity(accountId = 1, type = "INCOME", amount = 500.0, category = "Food", timestamp = 150)
        )

        assertEquals(20.0, FinancialCalculator.categoryTotal(transactions, "food", 150, 250), 0.001)
    }

    @Test
    fun monthlyEmiSupportsInterestFreeLoans() {
        assertEquals(1000.0, FinancialCalculator.monthlyEmi(12000.0, 0.0, 12), 0.001)
    }

    @Test
    fun progressIsClampedToGoalBounds() {
        assertEquals(0.0, FinancialCalculator.progress(-10.0, 100.0), 0.001)
        assertEquals(1.0, FinancialCalculator.progress(125.0, 100.0), 0.001)
    }

    @Test
    fun borrowIncreasesBalanceAndLendDecreasesIt() {
        val account = AccountEntity(id = 7, name = "Cash", accountType = "Wallet", balance = 100.0)
        val transactions = listOf(
            TransactionEntity(accountId = 7, type = "BORROW", amount = 50.0, category = "Borrowed"),
            TransactionEntity(accountId = 7, type = "LEND", amount = 20.0, category = "Lent")
        )

        assertEquals(130.0, FinancialCalculator.balance(account, transactions), 0.001)
        assertEquals(30.0, FinancialCalculator.netCashFlow(transactions), 0.001)
    }

    @Test
    fun cashCounterSupportsNotesAndCoinsAndHashtagSummaryWorks() {
        val lines = listOf(
            CashCountLine("NOTE", 500, 2),
            CashCountLine("NOTE", 100, 3),
            CashCountLine("COIN", 20, 2),
            CashCountLine("COIN", 5, 4),
            CashCountLine("COIN", 1, 7)
        )
        assertEquals(1367.0, NativeLedgerCalculator.computeCashDenominationTotal(lines), 0.001)

        val txns = listOf(
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 120.0, category = "Food", note = "Coffee #Cafe #Weekend"),
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 80.0, category = "Travel", note = "Auto #weekend")
        )
        val tags = NativeLedgerCalculator.summarizeHashtags(txns)
        assertEquals(200.0, tags.first { it.tag == "#weekend" }.totalExpense, 0.001)
        assertEquals(120.0, tags.first { it.tag == "#cafe" }.totalExpense, 0.001)
    }

    @Test
    fun autoSplitRoundUpAndDebtPayoffCalculatorsWork() {
        val allocs = listOf(AccountAllocation("1", "Taxes", 1000.0, 20.0))
        val acc = AccountEntity(id = 1, name = "Bank", accountType = "Bank", allocationsJson = NativeLedgerCalculator.serializeAllocations(allocs))
        val updated = NativeLedgerCalculator.applyAutoSplitToAllocations(acc, 5000.0)
        val parsed = NativeLedgerCalculator.parseAllocations(updated.allocationsJson)
        assertEquals(2000.0, parsed.first().allocatedAmount, 0.001)

        assertEquals(7.0, NativeLedgerCalculator.computeRoundUpSpareChange(43.0, 10), 0.001)

        val loans = listOf(
            LoanEntity(id = 1, name = "Small Low APR", principal = 5000.0, monthlyPayment = 1000.0, interestRate = 6.0),
            LoanEntity(id = 2, name = "High APR Card", principal = 10000.0, monthlyPayment = 1500.0, interestRate = 24.0)
        )
        val snowball = NativeLedgerCalculator.simulateDebtPayoff(loans, emptyList(), 1000.0, isAvalanche = false)
        val avalanche = NativeLedgerCalculator.simulateDebtPayoff(loans, emptyList(), 1000.0, isAvalanche = true)
        assertTrue(avalanche.totalInterestPaid <= snowball.totalInterestPaid)
        assertTrue(snowball.totalMonths > 0)
    }

    @Test
    fun internalNotificationsCatalogRemainsComplete() {
        val notifCandidates = listOf(
            File("src/main/assets/notifications.json"),
            File("app/src/main/assets/notifications.json")
        )
        val notifFile = notifCandidates.firstOrNull { it.exists() }
        assertTrue("notifications.json must exist in assets", notifFile != null)
        val parsed = DailyReminderReceiver.parseNotificationCatalogJson(notifFile!!.readText())
        assertEquals(100, parsed.size)
        assertTrue(parsed.any { it.category == "MEME" })
        assertTrue(parsed.any { it.category == "HYGIENE" })
        assertTrue(parsed.any { it.category == "MILESTONES" })
    }

    @Test
    fun intelligenceCalculatorsAndAllocationAdjustmentsWorkAccurately() {
        val draft = NativeIntelligenceCalculators.parseUpiSmsToDraft(
            "INR 450.50 debited from A/c XX1234 to Zomato via GPay UPI ref 987654"
        )
        assertTrue(draft != null)
        assertEquals("EXPENSE", draft!!.type)
        assertEquals(450.50, draft.amount, 0.001)
        assertEquals("GPay", draft.upiApp)
        assertEquals("UPI", draft.paymentMethod)

        val now = System.currentTimeMillis()
        val txns = listOf(
            TransactionEntity(accountId = 1, type = "INCOME", amount = 20000.0, category = "Salary", timestamp = now),
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 5000.0, category = "Food", timestamp = now),
            TransactionEntity(accountId = 1, type = "EXPENSE", amount = 2000.0, category = "Shopping", timestamp = now)
        )
        val pace = NativeIntelligenceCalculators.computeMonthlyPace(
            accounts = listOf(AccountEntity(id = 1, name = "Bank", accountType = "Bank", balance = 10000.0)),
            transactions = txns,
            budgets = emptyList(),
            nowMillis = now
        )
        assertEquals(7000.0, pace.currentMonthExpense, 0.001)
        assertTrue(pace.safePerDay >= 0.0)

        val split = NativeIntelligenceCalculators.compute503020Breakdown(txns)
        assertEquals(5000.0, split.needs, 0.001)
        assertEquals(2000.0, split.wants, 0.001)
        assertEquals(13000.0, split.savings, 0.001)

        val xray = NativeIntelligenceCalculators.computeFinancialXRay(
            accounts = listOf(AccountEntity(id = 1, name = "Bank", accountType = "Bank", balance = 15000.0)),
            transactions = txns,
            budgets = emptyList(),
            goals = emptyList(),
            loans = emptyList(),
            loanPayments = emptyList(),
            subscriptions = emptyList(),
            academicEvents = emptyList()
        )
        assertEquals(10, xray.metrics.size)
        assertTrue(xray.overallScore in 0..100)

        val allocJson = NativeLedgerCalculator.serializeAllocations(listOf(AccountAllocation("p1", "Food Pocket", 3000.0, 0.0)))
        val acc = AccountEntity(id = 1, name = "Bank", accountType = "Bank", allocationsJson = allocJson)
        val updatedAcc = NativeLedgerCalculator.adjustAllocationsForTransaction(acc, "p1", "EXPENSE", 450.0)
        val adjusted = NativeLedgerCalculator.parseAllocations(updatedAcc.allocationsJson)
        assertEquals(2550.0, adjusted.first().allocatedAmount, 0.001)

        assertTrue(MoneyPaise.formatCurrency(1250.0, symbol = "$").startsWith("$"))
        assertEquals(25000L, MoneyPaise.parsePaise("$250.00"))
    }
}
