package com.tracemeo.app.data

import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.LoanEntity
import com.tracemeo.app.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculatorTest {
    @Test
    fun accountBalanceAndMathEvaluatorWork() {
        val acc = AccountEntity(id = 1L, name = "Bank", accountType = "Bank", balance = 1000.0)
        val txs = listOf(
            TransactionEntity(id = 1L, accountId = 1L, type = "INCOME", amount = 500.0, category = "Family & Income"),
            TransactionEntity(id = 2L, accountId = 1L, type = "EXPENSE", amount = 250.0, category = "Food & Chai")
        )
        assertEquals(1250.0, FinancialCalculator.balance(acc, txs), 0.01)
        assertEquals(250.0, FinancialCalculator.netCashFlow(txs), 0.01)
        assertEquals(195.0, FinancialCalculator.evaluateMathExpression("120 + 45 + 30")!!, 0.01)
        assertEquals(150.0, FinancialCalculator.evaluateMathExpression("₹300 / 2")!!, 0.01)
    }

    @Test
    fun safeToSpendAndRunwayReflectLiveBalances() {
        val accounts = listOf(AccountEntity(id = 1L, name = "Savings", accountType = "Bank", balancePaise = 1_000_000L))
        val txs = listOf(
            TransactionEntity(id = 1L, accountId = 1L, type = "EXPENSE", amount = 1000.0, category = "Food & Chai")
        )
        val result = NativeLedgerCalculator.computeSafeToSpend(accounts, txs)
        assertTrue(result.remainingDaysInMonth > 0)
        assertEquals(9000.0, result.unallocatedBalance, 0.01)
        assertTrue(result.dailyAllowance > 0.0)

        val runway = NativeLedgerCalculator.computeEmergencyRunwayMonths(accounts, txs)
        assertEquals(9.0, runway, 0.01)
    }

    @Test
    fun cashDenominationAndDebtPayoffCalculatorsWork() {
        val lines = listOf(
            CashCountLine("NOTE", 500, 2),
            CashCountLine("NOTE", 100, 3),
            CashCountLine("COIN", 10, 5)
        )
        assertEquals(1350.0, NativeLedgerCalculator.computeCashDenominationTotal(lines), 0.01)
        assertEquals(5.0, NativeLedgerCalculator.computeRoundUpSpareChange(45.0, 10), 0.01)
        assertEquals(0.0, NativeLedgerCalculator.computeRoundUpSpareChange(100.0, 10), 0.01)

        val loans = listOf(
            LoanEntity(id = 1L, name = "Card", principal = 10000.0, principalPaise = 1_000_000L, interestRate = 24.0, monthlyPayment = 1500.0),
            LoanEntity(id = 2L, name = "Bike", principal = 20000.0, principalPaise = 2_000_000L, interestRate = 10.0, monthlyPayment = 2000.0)
        )
        val avalanche = NativeLedgerCalculator.simulateDebtPayoff(loans, emptyList(), 1000.0, isAvalanche = true)
        val snowball = NativeLedgerCalculator.simulateDebtPayoff(loans, emptyList(), 1000.0, isAvalanche = false)
        assertTrue(avalanche.totalMonths > 0)
        assertTrue(snowball.totalMonths > 0)
    }

    @Test
    fun csvAndXlsxRoundTripAndMoneyPaiseWork() {
        assertEquals(1500L, MoneyPaise.toPaise(15.0))
        assertEquals(15.0, MoneyPaise.toRupees(1500L), 0.001)
        assertEquals("₹••••••", MoneyPaise.formatCurrency(1234.0, "₹", stealthMode = false, privacyMask = true))
        val decoy = MoneyPaise.formatCurrency(1234.0, "₹", stealthMode = true, privacyMask = false)
        assertTrue(decoy.startsWith("₹") && decoy != "₹1,234")

        val txs = listOf(
            TransactionEntity(id = 1L, accountId = 1L, type = "EXPENSE", amount = 120.0, category = "Food & Chai", note = "Tea #morning")
        )
        val csv = CsvParser.exportTransactionsToCsv(txs)
        val parsed = CsvParser.parseCsvTransactions(csv, defaultAccountId = 1L)
        assertEquals(1, parsed.size)
        assertEquals(120.0, parsed.first().amount, 0.01)

        val xlsx = XlsxExporter.createWorkbookBytes(emptyList(), txs)
        assertNotNull(xlsx)
        assertTrue(xlsx.size > 100)
    }
}
