package com.tracmio.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategorySpendProjection(
    val category: String,
    val totalPaise: Long,
    val txCount: Int
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun transactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT category AS category, COALESCE(SUM(amountPaise), 0) AS totalPaise, COUNT(*) AS txCount
        FROM transactions
        WHERE UPPER(type) = 'EXPENSE' AND timestamp >= :fromMillis AND timestamp <= :toMillis
        GROUP BY category
        ORDER BY totalPaise DESC
        """
    )
    fun observeExpenseCategoryTotals(fromMillis: Long, toMillis: Long): Flow<List<CategorySpendProjection>>

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun categories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM persons ORDER BY name")
    fun persons(): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE transferId = :transferId")
    suspend fun deleteTransfer(transferId: String)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type IN ('INCOME', 'BORROW')")
    fun totalIncome(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type IN ('EXPENSE', 'LEND')")
    fun totalExpense(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type IN ('EXPENSE', 'LEND') AND category = :category AND timestamp >= :from AND timestamp < :to")
    fun spentForCategory(category: String, from: Long, to: Long): Flow<Double>

    @Query("SELECT * FROM budgets ORDER BY category")
    fun budgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun goals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goal_contributions ORDER BY timestamp DESC, id DESC")
    fun allGoalContributions(): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY timestamp DESC, id DESC")
    fun goalContributions(goalId: Long): Flow<List<GoalContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalContribution(contribution: GoalContributionEntity): Long

    @Delete
    suspend fun deleteGoalContribution(contribution: GoalContributionEntity)

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun deleteGoalContributionsForGoal(goalId: Long)

    @Query("DELETE FROM goal_contributions")
    suspend fun deleteAllGoalContributions()

    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun loans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT * FROM loan_payments ORDER BY timestamp DESC, id DESC")
    fun allLoanPayments(): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY timestamp DESC, id DESC")
    fun loanPayments(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(payment: LoanPaymentEntity): Long

    @Delete
    suspend fun deleteLoanPayment(payment: LoanPaymentEntity)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deleteLoanPaymentsForLoan(loanId: Long)

    @Query("DELETE FROM loan_payments")
    suspend fun deleteAllLoanPayments()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM persons")
    suspend fun deleteAllPersons()
}
