package com.charan.fintrack.data.repository

import androidx.room.withTransaction
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.NativeLedgerCalculator
import com.charan.fintrack.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID

data class NativeDashboardTotals(
    val balancePaise: Long = 0L,
    val incomePaise: Long = 0L,
    val expensePaise: Long = 0L,
    val borrowedPaise: Long = 0L,
    val lentPaise: Long = 0L,
    val netWorthPaise: Long = 0L,
    val transactionCount: Int = 0
)

/** Room-backed repository for the future native Compose UI. */
class NativeFinanceRepository(private val database: FinTrackDatabase) {
    private val accountDao = database.accountDao()
    private val dao = database.financeDao()

    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.transactions()
    val categories: Flow<List<CategoryEntity>> = dao.categories()
    val people: Flow<List<PersonEntity>> = dao.persons()
    val budgets: Flow<List<BudgetEntity>> = dao.budgets()
    val goals: Flow<List<GoalEntity>> = dao.goals()
    val loans: Flow<List<LoanEntity>> = dao.loans()

    val dashboard: Flow<NativeDashboardTotals> = combine(accounts, transactions) { accountRows, transactionRows ->
        NativeDashboardTotals(
            balancePaise = NativeLedgerCalculator.totalBalancePaise(accountRows, transactionRows),
            incomePaise = NativeLedgerCalculator.incomePaise(transactionRows),
            expensePaise = NativeLedgerCalculator.expensePaise(transactionRows),
            borrowedPaise = NativeLedgerCalculator.borrowedPaise(transactionRows),
            lentPaise = NativeLedgerCalculator.lentPaise(transactionRows),
            netWorthPaise = NativeLedgerCalculator.netWorthPaise(accountRows, transactionRows),
            transactionCount = transactionRows.size
        )
    }

    suspend fun addAccount(
        name: String,
        accountType: String,
        openingBalance: MoneyPaise,
        creditLimit: MoneyPaise = MoneyPaise.ZERO
    ): Long {
        require(name.isNotBlank()) { "Account name is required" }
        require(creditLimit.paise >= 0L) { "Credit limit cannot be negative" }
        val legacy = openingBalance.toRupeesDecimal().toDouble()
        return accountDao.insertAccount(
            AccountEntity(
                name = name.trim(), accountType = accountType,
                balance = legacy, openingBalancePaise = openingBalance.paise,
                creditLimit = creditLimit.toRupeesDecimal().toDouble(),
                creditLimitPaise = creditLimit.paise
            )
        )
    }

    suspend fun updateAccount(account: AccountEntity) {
        require(account.name.isNotBlank()) { "Account name is required" }
        accountDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(account: AccountEntity) {
        val linked = transactions.first().any { it.accountId == account.id || it.transferAccountId == account.id }
        require(!linked) { "This account has linked transactions. Archive it instead." }
        accountDao.deleteAccount(account)
    }

    suspend fun deletePerson(person: PersonEntity) {
        val linked = transactions.first().any { it.personId == person.id }
        require(!linked) { "This person has linked transactions." }
        dao.deletePerson(person)
    }

    suspend fun updatePerson(person: PersonEntity) = dao.updatePerson(person)
    suspend fun deleteCategory(category: CategoryEntity) = dao.deleteCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = dao.updateCategory(category)
    suspend fun deleteBudget(budget: BudgetEntity) = dao.deleteBudget(budget)
    suspend fun updateBudget(budget: BudgetEntity) = dao.updateBudget(budget)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = dao.deleteGoal(goal)
    suspend fun updateLoan(loan: LoanEntity) = dao.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) {
        database.withTransaction {
            dao.deleteLoanPayments(loan.id)
            dao.deleteLoan(loan)
        }
    }

    suspend fun replaceTransaction(
        transaction: TransactionEntity,
        destinationAccountId: Long? = null
    ) {
        require(transaction.amountPaise > 0L) { "Transaction amount must be positive" }
        val type = transaction.type.uppercase()
        require(type in setOf("INCOME", "EXPENSE", "BORROW", "LEND", "TRANSFER")) {
            "Unsupported transaction type"
        }
        require(accountDao.getAccountById(transaction.accountId) != null) { "Account does not exist" }
        if (type == "TRANSFER") {
            val destination = destinationAccountId ?: throw IllegalArgumentException("Choose a destination account")
            require(destination != transaction.accountId) { "Transfer accounts must be different" }
            require(accountDao.getAccountById(destination) != null) { "Destination account does not exist" }
        }
        if (type == "BORROW" || type == "LEND") {
            require(transaction.personId != null && dao.allPersons().any { it.id == transaction.personId }) {
                "Select a valid person for borrowed/lent money"
            }
        }
        database.withTransaction {
            val original = dao.allTransactions().firstOrNull { it.id == transaction.id }
                ?: throw IllegalArgumentException("Transaction does not exist")
            val oldTransferId = original.transferId
            if (oldTransferId != null) dao.deleteTransfer(oldTransferId)

            if (type == "TRANSFER") {
                val transferId = oldTransferId ?: UUID.randomUUID().toString()
                val legacyAmount = MoneyPaise.fromSignedPaise(transaction.amountPaise).toRupeesDecimal().toDouble()
                dao.insertTransaction(
                    transaction.copy(
                        id = original.id, type = "TRANSFER_OUT", amount = legacyAmount,
                        category = "Transfer", transferAccountId = destinationAccountId,
                        transferId = transferId, stableUid = original.stableUid
                    )
                )
                dao.insertTransaction(
                    TransactionEntity(
                        accountId = destinationAccountId!!, type = "TRANSFER_IN",
                        amount = legacyAmount, amountPaise = transaction.amountPaise,
                        category = "Transfer", note = transaction.note, timestamp = transaction.timestamp,
                        transferAccountId = transaction.accountId, transferId = transferId,
                        stableUid = original.stableUid + ":in"
                    )
                )
            } else {
                val updated = transaction.copy(
                    id = original.id, type = type,
                    amount = MoneyPaise.fromSignedPaise(transaction.amountPaise).toRupeesDecimal().toDouble(),
                    transferAccountId = null, transferId = null, stableUid = original.stableUid
                )
                if (oldTransferId != null) dao.insertTransaction(updated)
                else dao.updateTransaction(updated)
            }
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) =
        replaceTransaction(transaction, transaction.transferAccountId)

    suspend fun archiveAccount(accountId: Long) = accountDao.archiveAccount(accountId)

    suspend fun saveTransaction(
        accountId: Long,
        type: String,
        amount: MoneyPaise,
        category: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        subcategory: String = "",
        personId: Long? = null,
        paymentMethod: String = "",
        upiApp: String = ""
    ): Long {
        require(amount.paise > 0L) { "Amount must be greater than zero" }
        require(category.isNotBlank()) { "Category is required" }
        require(type.uppercase() in setOf("INCOME", "EXPENSE", "BORROW", "LEND")) {
            "Unsupported transaction type"
        }
        require(accountDao.getAccountById(accountId) != null) { "Account does not exist" }
        if (type.equals("BORROW", true) || type.equals("LEND", true)) {
            require(personId != null) { "Select a person for borrowed/lent money" }
            require(dao.personByStableUid(personId.toString()) != null || dao.allPersons().any { it.id == personId }) {
                "Selected person does not exist"
            }
        }
        return dao.insertTransaction(
            TransactionEntity(
                accountId = accountId, type = type.uppercase(),
                amount = amount.toRupeesDecimal().toDouble(), amountPaise = amount.paise,
                category = category.trim(), note = note.trim(), timestamp = timestamp,
                subcategory = subcategory.trim(), personId = personId,
                paymentMethod = paymentMethod.trim(), upiApp = upiApp.trim()
            )
        )
    }

    suspend fun addTransfer(
        fromAccountId: Long, toAccountId: Long, amount: MoneyPaise,
        note: String = "", timestamp: Long = System.currentTimeMillis()
    ) {
        require(fromAccountId != toAccountId) { "Choose two different accounts" }
        require(amount.paise > 0L) { "Transfer amount must be greater than zero" }
        require(accountDao.getAccountById(fromAccountId) != null) { "Source account does not exist" }
        require(accountDao.getAccountById(toAccountId) != null) { "Destination account does not exist" }
        val transferUid = UUID.randomUUID().toString()
        val legacy = amount.toRupeesDecimal().toDouble()
        database.withTransaction {
            dao.insertTransaction(
                TransactionEntity(
                    accountId = fromAccountId, type = "TRANSFER_OUT", amount = legacy,
                    amountPaise = amount.paise, category = "Transfer", note = note.trim(),
                    timestamp = timestamp, transferAccountId = toAccountId, transferId = transferUid
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    accountId = toAccountId, type = "TRANSFER_IN", amount = legacy,
                    amountPaise = amount.paise, category = "Transfer", note = note.trim(),
                    timestamp = timestamp, transferAccountId = fromAccountId, transferId = transferUid
                )
            )
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            if (transaction.transferId != null) dao.deleteTransfer(transaction.transferId)
            else dao.deleteTransaction(transaction)
        }
    }

    suspend fun addPerson(name: String, phone: String = "", notes: String = ""): Long {
        require(name.isNotBlank()) { "Person name is required" }
        return dao.insertPerson(PersonEntity(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
    }

    suspend fun addCategory(name: String, type: String, icon: String = "•"): Long {
        require(name.isNotBlank()) { "Category name is required" }
        require(type.uppercase() in setOf("INCOME", "EXPENSE")) { "Invalid category type" }
        return dao.insertCategory(CategoryEntity(name = name.trim(), type = type.uppercase(), icon = icon))
    }

    suspend fun addBudget(
        category: String,
        limit: MoneyPaise,
        month: String,
        period: String = "Monthly"
    ): Long {
        require(category.isNotBlank() && limit.paise > 0L) { "Enter a category and positive budget" }
        require(period in setOf("Daily", "Weekly", "Monthly", "Yearly")) { "Invalid budget period" }
        require(month.matches(Regex("\\d{4}-\\d{2}"))) { "Budget month must use YYYY-MM" }
        return dao.insertBudget(
            BudgetEntity(
                category = category.trim(), limitAmount = limit.toRupeesDecimal().toDouble(),
                limitAmountPaise = limit.paise, month = month, period = period
            )
        )
    }

    suspend fun addGoal(name: String, target: MoneyPaise, deadline: Long? = null): Long {
        require(name.isNotBlank() && target.paise > 0L) { "Enter a goal name and positive target" }
        return dao.insertGoal(
            GoalEntity(
                name = name.trim(), targetAmount = target.toRupeesDecimal().toDouble(),
                targetAmountPaise = target.paise, deadline = deadline
            )
        )
    }

    fun goalContributions(goalId: Long): Flow<List<GoalContributionEntity>> = dao.goalContributions(goalId)

    /** Tracking-only contributions. They do not create ledger entries. */
    suspend fun addGoalContribution(goalId: Long, contribution: MoneyPaise, note: String = "") {
        require(contribution.paise > 0L) { "Contribution must be positive" }
        database.withTransaction {
            val goal = dao.allGoals().firstOrNull { it.id == goalId }
                ?: throw IllegalArgumentException("Savings goal does not exist")
            val saved = Math.addExact(goal.savedAmountPaise, contribution.paise)
            require(saved <= goal.targetAmountPaise) { "Contribution exceeds remaining goal amount" }
            dao.insertGoalContribution(
                GoalContributionEntity(
                    goalId = goalId,
                    amount = contribution.toRupeesDecimal().toDouble(),
                    amountPaise = contribution.paise,
                    note = note.trim(),
                    stableUid = UUID.randomUUID().toString()
                )
            )
            dao.updateGoal(
                goal.copy(
                    savedAmount = MoneyPaise.fromSignedPaise(saved).toRupeesDecimal().toDouble(),
                    savedAmountPaise = saved
                )
            )
        }
    }

    suspend fun addLoan(
        name: String, principal: MoneyPaise,
        monthlyPayment: MoneyPaise, interestRate: Double
    ): Long {
        require(name.isNotBlank() && principal.paise > 0L) { "Enter a loan name and positive principal" }
        require(monthlyPayment.paise >= 0L && interestRate >= 0.0) { "Loan values cannot be negative" }
        return dao.insertLoan(
            LoanEntity(
                name = name.trim(), principal = principal.toRupeesDecimal().toDouble(),
                principalPaise = principal.paise,
                monthlyPayment = monthlyPayment.toRupeesDecimal().toDouble(),
                monthlyPaymentPaise = monthlyPayment.paise, interestRate = interestRate
            )
        )
    }

    /** Repayments are tracked separately; cash changes only via a separate transaction. */
    fun loanPayments(loanId: Long): Flow<List<LoanPaymentEntity>> = dao.loanPayments(loanId)

    suspend fun addLoanRepayment(loanId: Long, amount: MoneyPaise, note: String = "") {
        require(amount.paise > 0L) { "Repayment must be positive" }
        database.withTransaction {
            val loan = loans.first().firstOrNull { it.id == loanId }
                ?: throw IllegalArgumentException("Loan does not exist")
            val paid = Math.addExact(loan.paidAmountPaise, amount.paise)
            require(paid <= loan.principalPaise) { "Repayment exceeds the remaining principal" }
            dao.insertLoanPayment(
                LoanPaymentEntity(
                    loanId = loanId, amount = amount.toRupeesDecimal().toDouble(),
                    amountPaise = amount.paise, note = note.trim(),
                    stableUid = UUID.randomUUID().toString()
                )
            )
            dao.updateLoan(
                loan.copy(
                    paidAmount = MoneyPaise.fromSignedPaise(paid).toRupeesDecimal().toDouble(),
                    paidAmountPaise = paid
                )
            )
        }
    }
}
