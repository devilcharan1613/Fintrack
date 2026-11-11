package com.charan.tracmio.data

import com.charan.tracmio.DailyReminderReceiver
import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun liquidGlassWebViewUiAnd100NotificationsCatalogAreConfigured() {
        val htmlCandidates = listOf(
            File("src/main/assets/index.html"),
            File("app/src/main/assets/index.html")
        )
        val htmlFile = htmlCandidates.firstOrNull { it.exists() }
        assertTrue("index.html must exist in assets", htmlFile != null)
        val html = htmlFile!!.readText()
        assertTrue(html.contains("TRACMIO · OFFLINE FINANCE MANAGER"))
        assertTrue(html.contains("v1.00"))
        assertTrue(html.contains("id=\"page-themes\""))
        assertTrue(html.contains("setTracmioGlassUi"))
        assertTrue(html.contains("tracmio-3d-badge"))
        assertTrue(html.contains("nav-scroll-hidden"))
        assertFalse(html.contains("Cloud Sync"))
        assertFalse(html.contains("Install Update from APK"))

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
}
