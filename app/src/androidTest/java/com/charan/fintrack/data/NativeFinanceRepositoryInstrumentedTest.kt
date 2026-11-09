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
