package com.charan.tracmio.data.repository

import androidx.room.withTransaction
import com.charan.tracmio.data.MoneyPaise
import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.BudgetEntity
import com.charan.tracmio.data.local.CategoryEntity
import com.charan.tracmio.data.local.GoalContributionEntity
import com.charan.tracmio.data.local.GoalEntity
import com.charan.tracmio.data.local.LoanEntity
import com.charan.tracmio.data.local.LoanPaymentEntity
import com.charan.tracmio.data.local.PersonEntity
import com.charan.tracmio.data.local.TracmioDatabase
import com.charan.tracmio.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class FinanceRepository(private val database: TracmioDatabase) {
    private val accounts = database.accountDao()
    private val finance = database.financeDao()

    val accountList: Flow<List<AccountEntity>> = accounts.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = finance.transactions()
    val budgets: Flow<List<BudgetEntity>> = finance.budgets()
    val goals: Flow<List<GoalEntity>> = finance.goals()
    val goalContributions: Flow<List<GoalContributionEntity>> = finance.allGoalContributions()
    val loans: Flow<List<LoanEntity>> = finance.loans()
    val loanPaymentsAll: Flow<List<LoanPaymentEntity>> = finance.allLoanPayments()
    val categories: Flow<List<CategoryEntity>> = finance.categories()
    val persons: Flow<List<PersonEntity>> = finance.persons()

    suspend fun seedDefaultCategories() {
        if (categories.first().isNotEmpty()) return
        listOf(
            "Food & Dining" to "EXPENSE",
            "Transport" to "EXPENSE",
            "Shopping" to "EXPENSE",
            "Bills & Utilities" to "EXPENSE",
            "Health" to "EXPENSE",
            "Entertainment" to "EXPENSE",
            "Salary" to "INCOME",
            "Business" to "INCOME",
            "Other income" to "INCOME"
        ).forEach { (name, type) ->
            finance.insertCategory(CategoryEntity(name = name, type = type, isSystem = true))
        }
    }

    val totalBalance: Flow<Double> = accounts.getComputedTotalBalance()
    val income: Flow<Double> = finance.totalIncome()
    val expense: Flow<Double> = finance.totalExpense()

    suspend fun addAccount(account: AccountEntity) = accounts.insertAccount(
        account.copy(balancePaise = MoneyPaise.toPaise(account.balance))
    )

    suspend fun updateAccount(account: AccountEntity) = accounts.updateAccount(
        account.copy(
            balancePaise = MoneyPaise.toPaise(account.balance),
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun archiveAccount(account: AccountEntity) = accounts.archiveAccount(account.id)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        require(transaction.category.isNotBlank()) { "Transaction category is required" }
        require(accounts.getAccountById(transaction.accountId) != null) { "Account does not exist" }
        return finance.insertTransaction(
            transaction.copy(amountPaise = MoneyPaise.toPaise(transaction.amount))
        )
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        database.withTransaction {
            if (transaction.transferId != null) finance.deleteTransfer(transaction.transferId)
            finance.insertTransaction(
                transaction.copy(amountPaise = MoneyPaise.toPaise(transaction.amount))
            )
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            if (transaction.transferId != null) {
                finance.deleteTransfer(transaction.transferId)
            } else {
                finance.deleteTransaction(transaction)
            }
        }
    }

    suspend fun addTransfer(
        from: Long,
        to: Long,
        amount: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: String = "Transfer",
        upiApp: String = ""
    ) {
        require(from != to) { "Transfer accounts must be different" }
        require(amount > 0) { "Transfer amount must be positive" }
        require(accounts.getAccountById(from) != null) { "Source account does not exist" }
        require(accounts.getAccountById(to) != null) { "Destination account does not exist" }
        val transferId = UUID.randomUUID().toString()
        val paise = MoneyPaise.toPaise(amount)
        database.withTransaction {
            finance.insertTransaction(
                TransactionEntity(
                    accountId = from,
                    type = "TRANSFER_OUT",
                    amount = amount,
                    amountPaise = paise,
                    category = "Transfer",
                    note = note,
                    timestamp = timestamp,
                    transferAccountId = to,
                    transferId = transferId,
                    paymentMethod = paymentMethod,
                    upiApp = upiApp
                )
            )
            finance.insertTransaction(
                TransactionEntity(
                    accountId = to,
                    type = "TRANSFER_IN",
                    amount = amount,
                    amountPaise = paise,
                    category = "Transfer",
                    note = note,
                    timestamp = timestamp,
                    transferAccountId = from,
                    transferId = transferId,
                    paymentMethod = paymentMethod,
                    upiApp = upiApp
                )
            )
        }
    }

    suspend fun addBudget(budget: BudgetEntity) = finance.insertBudget(
        budget.copy(limitAmountPaise = MoneyPaise.toPaise(budget.limitAmount))
    )

    suspend fun updateBudget(budget: BudgetEntity) = finance.updateBudget(
        budget.copy(limitAmountPaise = MoneyPaise.toPaise(budget.limitAmount))
    )

    suspend fun deleteBudget(budget: BudgetEntity) = finance.deleteBudget(budget)

    suspend fun addGoal(goal: GoalEntity) = finance.insertGoal(
        goal.copy(
            targetAmountPaise = MoneyPaise.toPaise(goal.targetAmount),
            savedAmountPaise = MoneyPaise.toPaise(goal.savedAmount)
        )
    )

    suspend fun updateGoal(goal: GoalEntity) = finance.updateGoal(
        goal.copy(
            targetAmountPaise = MoneyPaise.toPaise(goal.targetAmount),
            savedAmountPaise = MoneyPaise.toPaise(goal.savedAmount)
        )
    )

    suspend fun deleteGoal(goal: GoalEntity) {
        database.withTransaction {
            val contributions = finance.goalContributions(goal.id).first()
            contributions.forEach { contrib ->
                contrib.transactionId?.let { txId -> finance.deleteTransactionById(txId) }
            }
            finance.deleteGoalContributionsForGoal(goal.id)
            finance.deleteGoal(goal)
        }
    }

    suspend fun addGoalContribution(
        goalId: Long,
        amount: Double,
        accountId: Long? = null,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        require(amount > 0) { "Contribution amount must be positive" }
        val goal = finance.getGoalById(goalId) ?: error("Goal not found")
        val paise = MoneyPaise.toPaise(amount)
        database.withTransaction {
            val txId = if (accountId != null && accounts.getAccountById(accountId) != null) {
                finance.insertTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        type = "EXPENSE",
                        amount = amount,
                        amountPaise = paise,
                        category = "Savings Goal",
                        subcategory = goal.name,
                        note = note.ifBlank { "Saved towards ${goal.name}" },
                        timestamp = timestamp
                    )
                )
            } else null

            finance.insertGoalContribution(
                GoalContributionEntity(
                    goalId = goalId,
                    accountId = accountId,
                    transactionId = txId,
                    amount = amount,
                    amountPaise = paise,
                    timestamp = timestamp,
                    note = note
                )
            )
            val newSavedPaise = goal.savedAmountPaise + paise
            finance.updateGoal(
                goal.copy(
                    savedAmount = MoneyPaise.toRupees(newSavedPaise),
                    savedAmountPaise = newSavedPaise
                )
            )
        }
    }

    fun goalContributions(goalId: Long): Flow<List<GoalContributionEntity>> =
        finance.goalContributions(goalId)

    suspend fun addLoan(loan: LoanEntity) = finance.insertLoan(
        loan.copy(
            principalPaise = MoneyPaise.toPaise(loan.principal),
            paidAmountPaise = MoneyPaise.toPaise(loan.paidAmount),
            monthlyPaymentPaise = MoneyPaise.toPaise(loan.monthlyPayment)
        )
    )

    suspend fun updateLoan(loan: LoanEntity) = finance.updateLoan(
        loan.copy(
            principalPaise = MoneyPaise.toPaise(loan.principal),
            paidAmountPaise = MoneyPaise.toPaise(loan.paidAmount),
            monthlyPaymentPaise = MoneyPaise.toPaise(loan.monthlyPayment)
        )
    )

    suspend fun deleteLoan(loan: LoanEntity) {
        database.withTransaction {
            val payments = finance.loanPayments(loan.id).first()
            payments.forEach { payment ->
                payment.transactionId?.let { txId -> finance.deleteTransactionById(txId) }
            }
            finance.deleteLoanPaymentsForLoan(loan.id)
            finance.deleteLoan(loan)
        }
    }

    suspend fun addLoanPayment(payment: LoanPaymentEntity): Long {
        require(payment.amount > 0) { "Payment amount must be positive" }
        val paise = MoneyPaise.toPaise(payment.amount)
        var insertedId = 0L
        database.withTransaction {
            val loan = finance.getLoanById(payment.loanId)
            val txId = if (payment.accountId != null && accounts.getAccountById(payment.accountId) != null) {
                finance.insertTransaction(
                    TransactionEntity(
                        accountId = payment.accountId,
                        type = "EXPENSE",
                        amount = payment.amount,
                        amountPaise = paise,
                        category = "Loan EMI",
                        subcategory = loan?.name.orEmpty(),
                        note = payment.note.ifBlank { "Loan payment: ${loan?.name.orEmpty()}" },
                        timestamp = payment.timestamp
                    )
                )
            } else payment.transactionId

            insertedId = finance.insertLoanPayment(
                payment.copy(
                    transactionId = txId,
                    amountPaise = paise
                )
            )
            if (loan != null) {
                val newPaidPaise = loan.paidAmountPaise + paise
                finance.updateLoan(
                    loan.copy(
                        paidAmount = MoneyPaise.toRupees(newPaidPaise),
                        paidAmountPaise = newPaidPaise
                    )
                )
            }
        }
        return insertedId
    }

    fun loanPayments(loanId: Long): Flow<List<LoanPaymentEntity>> = finance.loanPayments(loanId)

    suspend fun addCategory(category: CategoryEntity) = finance.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = finance.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = finance.deleteCategory(category)
    suspend fun addPerson(person: PersonEntity) = finance.insertPerson(person)
    suspend fun updatePerson(person: PersonEntity) = finance.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = finance.deletePerson(person)

    suspend fun exportJson(): String = NativeBackupService(database).exportJson()

    suspend fun restoreJson(json: String) {
        NativeBackupService(database).restoreJson(json)
    }

    suspend fun exportCsv(): String {
        val rows = transactions.first()
        return buildString {
            appendLine("id,accountId,type,amount,category,note,timestamp")
            rows.forEach {
                appendLine(
                    listOf(it.id, it.accountId, it.type, it.amount, it.category, it.note, it.timestamp)
                        .joinToString(",") { value -> "\"${value.toString().replace("\"", "\"\"")}\"" }
                )
            }
        }
    }

    suspend fun importCsv(csv: String): Int {
        val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size > 1) { "CSV file is empty" }
        val header = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val required = listOf("accountid", "type", "amount", "category")
        require(required.all { it in header }) { "CSV headers are invalid" }
        val index = header.withIndex().associate { it.value to it.index }
        var imported = 0
        database.withTransaction {
            lines.drop(1).forEach { line ->
                val values = parseCsvLine(line)
                fun value(name: String): String = values.getOrNull(index.getValue(name)).orEmpty()
                val accountId = value("accountid").toLongOrNull() ?: return@forEach
                if (accounts.getAccountById(accountId) == null) return@forEach
                val amount = value("amount").toDoubleOrNull() ?: return@forEach
                val type = value("type").uppercase()
                if (amount <= 0 || type !in setOf("INCOME", "EXPENSE", "BORROW", "LEND")) return@forEach
                finance.insertTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        type = type,
                        amount = amount,
                        amountPaise = MoneyPaise.toPaise(amount),
                        category = value("category").ifBlank { "Other" },
                        note = value("note"),
                        timestamp = value("timestamp").toLongOrNull() ?: System.currentTimeMillis()
                    )
                )
                imported++
            }
        }
        require(imported > 0) { "No valid transactions found" }
        return imported
    }

    suspend fun clearAllData() {
        database.withTransaction {
            finance.deleteAllGoalContributions()
            finance.deleteAllLoanPayments()
            finance.deleteAllTransactions()
            finance.deleteAllBudgets()
            finance.deleteAllGoals()
            finance.deleteAllLoans()
            finance.deleteAllCategories()
            finance.deleteAllPersons()
            accounts.deleteAll()
        }
    }
}
