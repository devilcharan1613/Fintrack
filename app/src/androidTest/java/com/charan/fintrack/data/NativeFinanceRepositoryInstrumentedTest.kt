package com.charan.fintrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charan.fintrack.data.local.FinTrackDatabase
import com.charan.fintrack.data.repository.NativeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeFinanceRepositoryInstrumentedTest {
    private lateinit var database: FinTrackDatabase
    private lateinit var repository: NativeFinanceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
        repository = NativeFinanceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun accountMetadataAndCategorySubcategoriesArePersisted() = runBlocking {
        val accountId = repository.addAccount(
            name = "Primary",
            accountType = "Bank",
            openingBalance = MoneyPaise.fromRupees("1250.75"),
            institution = "SBI",
            accountNumber = "Local reference 01",
            notes = "Salary account"
        )
        val account = database.accountDao().getAccountById(accountId)!!
        assertEquals("SBI", account.institution)
        assertEquals("Local reference 01", account.accountNumber)
        assertEquals("Salary account", account.notes)
        assertEquals(125_075L, account.openingBalancePaise)

        val categoryId = repository.addCategory(
            "Food",
            "EXPENSE",
            "🍽️",
            listOf("Groceries", "Dining", " groceries ", "")
        )
        val category = database.financeDao().allCategories().single { it.id == categoryId }
        val subcategories = JSONArray(category.subcategoriesJson)
        assertEquals(2, subcategories.length())
        assertEquals("Groceries", subcategories.getString(0))
        assertEquals("Dining", subcategories.getString(1))
    }

    @Test
    fun budgetsGoalsAndLoanRepaymentsPersistExactPaiseValues() = runBlocking {
        val budgetId = repository.addBudget(
            category = "Food", limit = MoneyPaise.fromRupees("2500.75"),
            month = "2026-11", period = "Monthly"
        )
        val budget = database.financeDao().allBudgets().single { it.id == budgetId }
        assertEquals(250_075L, budget.limitAmountPaise)
        assertEquals("2026-11", budget.month)

        val goalId = repository.addGoal("Emergency fund", MoneyPaise.fromRupees("10000"))
        repository.addGoalContribution(goalId, MoneyPaise.fromRupees("1250.50"), "First deposit")
        val goal = database.financeDao().allGoals().single { it.id == goalId }
        assertEquals(125_050L, goal.savedAmountPaise)
        assertEquals(1, database.financeDao().allGoalContributions().count { it.goalId == goalId })

        val loanId = repository.addLoan(
            "Laptop", MoneyPaise.fromRupees("50000"), MoneyPaise.fromRupees("5000"), 12.5
        )
        repository.addLoanRepayment(loanId, MoneyPaise.fromRupees("7500.25"), "First repayment")
        val loan = database.financeDao().allLoans().single { it.id == loanId }
        assertEquals(750_025L, loan.paidAmountPaise)
        assertEquals(1, database.financeDao().allLoanPayments().count { it.loanId == loanId })
    }

    @Test
    fun ordinaryTransactionsDoNotKeepHiddenDefaultPersonLinks() = runBlocking {
        val accountId = repository.addAccount("Cash", "Wallet", MoneyPaise.ZERO)
        val personId = repository.addPerson("Default person")

        val expenseId = repository.saveTransaction(
            accountId = accountId,
            type = "EXPENSE",
            amount = MoneyPaise.fromRupees("10"),
            category = "Food",
            personId = personId
        )
        var expense = database.financeDao().allTransactions().single { it.id == expenseId }
        assertNull("An expense must not retain the hidden default person", expense.personId)

        val borrowId = repository.saveTransaction(
            accountId = accountId,
            type = "BORROW",
            amount = MoneyPaise.fromRupees("20"),
            category = "Borrowed",
            personId = personId
        )
        val borrow = database.financeDao().allTransactions().single { it.id == borrowId }
        assertEquals(personId, borrow.personId)

        repository.replaceTransaction(
            borrow.copy(type = "EXPENSE", category = "Other", personId = personId),
            destinationAccountId = null
        )
        expense = database.financeDao().allTransactions().single { it.id == borrowId }
        assertNull("Converting Borrow to Expense must clear its person link", expense.personId)
    }

    @Test
    fun editingExpenseIntoTransferReplacesOriginalWithBalancedPair() = runBlocking {
        val sourceId = repository.addAccount(
            "Wallet", "Wallet", MoneyPaise.fromRupees("100.00")
        )
        val destinationId = repository.addAccount(
            "Bank", "Bank", MoneyPaise.ZERO
        )
        val expenseId = repository.saveTransaction(
            accountId = sourceId,
            type = "EXPENSE",
            amount = MoneyPaise.fromRupees("30.00"),
            category = "Food",
            note = "Move money instead"
        )
        val original = database.financeDao().allTransactions().single { it.id == expenseId }

        repository.replaceTransaction(
            original.copy(
                type = "TRANSFER",
                amount = 25.00,
                amountPaise = 2_500,
                category = "Transfer",
                note = "Move to bank"
            ),
            destinationId
        )

        val rows = database.financeDao().allTransactions()
        val outgoing = rows.single { it.type == "TRANSFER_OUT" }
        val incoming = rows.single { it.type == "TRANSFER_IN" }
        assertEquals(expenseId, outgoing.id)
        assertEquals(sourceId, outgoing.accountId)
        assertEquals(destinationId, outgoing.transferAccountId)
        assertEquals(destinationId, incoming.accountId)
        assertEquals(sourceId, incoming.transferAccountId)
        assertEquals(outgoing.transferId, incoming.transferId)
        assertEquals(2_500L, outgoing.amountPaise)
        assertEquals(2_500L, incoming.amountPaise)
        assertEquals(2, rows.size)
        assertEquals(
            7_500L,
            NativeLedgerCalculator.accountBalancePaise(
                database.accountDao().getAccountById(sourceId)!!, rows
            )
        )
        assertEquals(
            2_500L,
            NativeLedgerCalculator.accountBalancePaise(
                database.accountDao().getAccountById(destinationId)!!, rows
            )
        )
    }

    @Test
    fun resetDeletesAllFinanceRecordsInsideSingleDatabaseTransaction() = runBlocking {
        val accountId = repository.addAccount(
            "Cash", "Wallet", MoneyPaise.fromRupees("1000")
        )
        val personId = repository.addPerson("Alex")
        repository.addCategory("Food", "EXPENSE", "🍽️", listOf("Groceries"))
        repository.addBudget("Food", MoneyPaise.fromRupees("500"), "2026-11")
        val goalId = repository.addGoal("Trip", MoneyPaise.fromRupees("5000"))
        repository.addGoalContribution(goalId, MoneyPaise.fromRupees("100"), "Monthly savings")
        val loanId = repository.addLoan(
            "Phone", MoneyPaise.fromRupees("10000"), MoneyPaise.fromRupees("1000"), 0.0
        )
        repository.addLoanRepayment(loanId, MoneyPaise.fromRupees("250"), "First payment")
        repository.saveTransaction(
            accountId = accountId,
            type = "BORROW",
            amount = MoneyPaise.fromRupees("200"),
            category = "Borrowed",
            personId = personId
        )

        assertTrue(database.accountDao().allAccounts().isNotEmpty())
        assertTrue(database.financeDao().allTransactions().isNotEmpty())
        assertTrue(database.financeDao().allPersons().isNotEmpty())
        assertTrue(database.financeDao().allCategories().isNotEmpty())
        assertTrue(database.financeDao().allBudgets().isNotEmpty())
        assertTrue(database.financeDao().allGoals().isNotEmpty())
        assertTrue(database.financeDao().allGoalContributions().isNotEmpty())
        assertTrue(database.financeDao().allLoans().isNotEmpty())
        assertTrue(database.financeDao().allLoanPayments().isNotEmpty())

        repository.resetAllFinanceData()

        assertTrue(database.accountDao().allAccounts().isEmpty())
        assertTrue(database.financeDao().allTransactions().isEmpty())
        assertTrue(database.financeDao().allPersons().isEmpty())
        assertTrue(database.financeDao().allCategories().isEmpty())
        assertTrue(database.financeDao().allBudgets().isEmpty())
        assertTrue(database.financeDao().allGoals().isEmpty())
        assertTrue(database.financeDao().allGoalContributions().isEmpty())
        assertTrue(database.financeDao().allLoans().isEmpty())
        assertTrue(database.financeDao().allLoanPayments().isEmpty())
    }

    @Test
    fun repositoryRejectsInvalidNamesDatesAndFinancialTargets() = runBlocking {
        val accountId = repository.addAccount("Cash", "Wallet", MoneyPaise.ZERO)
        val account = database.accountDao().getAccountById(accountId)!!
        val blankTypeError = runCatching {
            repository.updateAccount(account.copy(accountType = " "))
        }.exceptionOrNull()
        assertTrue(blankTypeError is IllegalArgumentException)

        val invalidMonthError = runCatching {
            repository.addBudget("Food", MoneyPaise.fromRupees("100"), "2026-99")
        }.exceptionOrNull()
        assertTrue(invalidMonthError is IllegalArgumentException)

        val blankCategoryError = runCatching {
            repository.updateCategory(com.charan.fintrack.data.local.CategoryEntity(name = " ", type = "EXPENSE"))
        }.exceptionOrNull()
        assertTrue(blankCategoryError is IllegalArgumentException)

        val zeroGoalError = runCatching {
            repository.addGoal("No target", MoneyPaise.ZERO)
        }.exceptionOrNull()
        assertTrue(zeroGoalError is IllegalArgumentException)

        val invalidLoanRateError = runCatching {
            repository.addLoan("Loan", MoneyPaise.fromRupees("100"), MoneyPaise.ZERO, Double.NaN)
        }.exceptionOrNull()
        assertTrue(invalidLoanRateError is IllegalArgumentException)
    }
}
