package com.charan.fintrack.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun transactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun allTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE stableUid = :stableUid LIMIT 1")
    suspend fun transactionByStableUid(stableUid: String): TransactionEntity?

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun categories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE stableUid = :stableUid LIMIT 1")
    suspend fun categoryByStableUid(stableUid: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM persons ORDER BY name")
    fun persons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons")
    suspend fun allPersons(): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE stableUid = :stableUid LIMIT 1")
    suspend fun personByStableUid(stableUid: String): PersonEntity?

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

    @Query("SELECT * FROM budgets")
    suspend fun allBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE stableUid = :stableUid LIMIT 1")
    suspend fun budgetByStableUid(stableUid: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun goals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE stableUid = :stableUid LIMIT 1")
    suspend fun goalByStableUid(stableUid: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun loans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans")
    suspend fun allLoans(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE stableUid = :stableUid LIMIT 1")
    suspend fun loanByStableUid(stableUid: String): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY timestamp DESC")
    fun loanPayments(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments")
    suspend fun allLoanPayments(): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments WHERE stableUid = :stableUid LIMIT 1")
    suspend fun loanPaymentByStableUid(stableUid: String): LoanPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(payment: LoanPaymentEntity): Long

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun goalContributions(goalId: Long): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions")
    suspend fun allGoalContributions(): List<GoalContributionEntity>

    @Query("SELECT * FROM goal_contributions WHERE stableUid = :stableUid LIMIT 1")
    suspend fun goalContributionByStableUid(stableUid: String): GoalContributionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalContribution(contribution: GoalContributionEntity): Long

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deleteLoanPayments(loanId: Long)

    @Query("DELETE FROM loan_payments")
    suspend fun deleteAllLoanPayments()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM goal_contributions")
    suspend fun deleteAllGoalContributions()

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun deleteGoalContributions(goalId: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM persons")
    suspend fun deleteAllPersons()
}
