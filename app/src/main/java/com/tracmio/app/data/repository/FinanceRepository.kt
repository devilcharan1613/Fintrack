package com.tracmio.app.data.repository

import androidx.room.withTransaction
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.local.BudgetEntity
import com.tracmio.app.data.local.CategoryEntity
import com.tracmio.app.data.local.CustomTrackerEntity
import com.tracmio.app.data.local.GoalContributionEntity
import com.tracmio.app.data.local.GoalEntity
import com.tracmio.app.data.local.LoanEntity
import com.tracmio.app.data.local.LoanPaymentEntity
import com.tracmio.app.data.local.PersonEntity
import com.tracmio.app.data.local.TracmioDatabase
import com.tracmio.app.data.local.TrackerActivityLogEntity
import com.tracmio.app.data.local.TrackerAttachmentEntity
import com.tracmio.app.data.local.TrackerPersonEntity
import com.tracmio.app.data.local.TrackerSettlementEntity
import com.tracmio.app.data.local.TrackerTransactionCrossRefEntity
import com.tracmio.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class FinanceRepository(val database: TracmioDatabase) {
    private val accounts = database.accountDao()
    private val finance = database.financeDao()
    private val customTrackerDao = database.customTrackerDao()

    val accountList: Flow<List<AccountEntity>> = accounts.getAllAccounts()
    val allAccountsIncludingArchived: Flow<List<AccountEntity>> = accounts.getAllAccountsIncludingArchived()
    val transactions: Flow<List<TransactionEntity>> = finance.transactions()
    val budgets: Flow<List<BudgetEntity>> = finance.budgets()
    val goals: Flow<List<GoalEntity>> = finance.goals()
    val goalContributions: Flow<List<GoalContributionEntity>> = finance.allGoalContributions()
    val loans: Flow<List<LoanEntity>> = finance.loans()
    val loanPaymentsAll: Flow<List<LoanPaymentEntity>> = finance.allLoanPayments()
    val categories: Flow<List<CategoryEntity>> = finance.categories()
    val persons: Flow<List<PersonEntity>> = finance.persons()

    val activeTrackers: Flow<List<CustomTrackerEntity>> = customTrackerDao.getActiveTrackers()
    val archivedTrackers: Flow<List<CustomTrackerEntity>> = customTrackerDao.getArchivedTrackers()
    val trackerLinks: Flow<List<TrackerTransactionCrossRefEntity>> = customTrackerDao.observeAllTrackerLinks()
    val trackerPeople: Flow<List<TrackerPersonEntity>> = customTrackerDao.observeAllTrackerPeople()
    val trackerSettlements: Flow<List<TrackerSettlementEntity>> = customTrackerDao.observeAllTrackerSettlements()
    val trackerAttachments: Flow<List<TrackerAttachmentEntity>> = customTrackerDao.observeAllTrackerAttachments()

    suspend fun seedDefaultCategories() {
        val current = categories.first()
        if (current.isNotEmpty()) return
        listOf(
            Triple("Food & Dining", "EXPENSE", "🍔"),
            Triple("Groceries", "EXPENSE", "🛒"),
            Triple("Transport", "EXPENSE", "🚕"),
            Triple("Shopping", "EXPENSE", "🛍️"),
            Triple("Bills & Utilities", "EXPENSE", "⚡"),
            Triple("Rent & Housing", "EXPENSE", "🏠"),
            Triple("Health", "EXPENSE", "💊"),
            Triple("Entertainment", "EXPENSE", "🎬"),
            Triple("Education", "EXPENSE", "🎓"),
            Triple("Travel", "EXPENSE", "✈️"),
            Triple("Salary", "INCOME", "💼"),
            Triple("Business", "INCOME", "📈"),
            Triple("Freelance", "INCOME", "💻"),
            Triple("Other Income", "INCOME", "💰")
        ).forEach { (name, type, icon) ->
            finance.insertCategory(CategoryEntity(name = name, type = type, icon = icon, isSystem = true))
        }
    }

    suspend fun ensureDefaultAccountIfEmpty() {
        val current = accounts.getAllAccountsIncludingArchived().first()
        if (current.isEmpty()) {
            accounts.insertAccount(
                AccountEntity(
                    name = "Primary Cash Wallet",
                    accountType = "Cash",
                    balance = 0.0,
                    balancePaise = 0L,
                    currency = "INR"
                )
            )
        }
    }

    val totalBalance: Flow<Double> = accounts.getComputedTotalBalance()
    val income: Flow<Double> = finance.totalIncome()
    val expense: Flow<Double> = finance.totalExpense()

    /**
     * Adds a new account while preventing duplicate accounts with the same name & type.
     */
    suspend fun addAccount(account: AccountEntity): Long {
        val cleanName = account.name.trim()
        require(cleanName.isNotEmpty()) { "Account name cannot be empty" }
        val cleanType = account.accountType.trim().ifBlank { "Bank" }
        val existing = accounts.getAllAccountsIncludingArchived().first().firstOrNull {
            !it.isArchived &&
                it.name.equals(cleanName, ignoreCase = true) &&
                it.accountType.equals(cleanType, ignoreCase = true)
        }
        require(existing == null) { "An account named '$cleanName' ($cleanType) already exists" }

        return accounts.insertAccount(
            account.copy(
                name = cleanName,
                accountType = cleanType,
                balancePaise = MoneyPaise.toPaise(account.balance)
            )
        )
    }

    suspend fun updateAccount(account: AccountEntity) = accounts.updateAccount(
        account.copy(
            name = account.name.trim().ifBlank { "Account" },
            accountType = account.accountType.trim().ifBlank { "Bank" },
            balancePaise = MoneyPaise.toPaise(account.balance),
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun archiveAccount(account: AccountEntity) = accounts.archiveAccount(account.id)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        require(transaction.category.isNotBlank()) { "Transaction category is required" }
        require(accounts.getAccountById(transaction.accountId) != null) { "Selected account does not exist" }
        return finance.insertTransaction(
            transaction.copy(
                amountPaise = MoneyPaise.toPaise(transaction.amount)
            )
        )
    }

    /**
     * Updates the existing transaction in-place by primary ID and stableUid without creating duplicates.
     */
    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.id > 0L) { "Cannot update transaction without a valid ID" }
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        require(transaction.category.isNotBlank()) { "Transaction category is required" }
        require(accounts.getAccountById(transaction.accountId) != null) { "Selected account does not exist" }

        val updatedEntity = transaction.copy(
            amountPaise = MoneyPaise.toPaise(transaction.amount)
        )
        database.withTransaction {
            val existing = finance.getTransactionById(transaction.id)
            if (existing != null) {
                finance.updateTransaction(
                    updatedEntity.copy(
                        stableUid = existing.stableUid.ifBlank { transaction.stableUid }
                    )
                )
            } else {
                finance.insertTransaction(updatedEntity)
            }
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

    suspend fun addCategory(category: CategoryEntity): Long {
        val cleanName = category.name.trim()
        require(cleanName.isNotEmpty()) { "Category name is required" }
        val existing = categories.first().firstOrNull {
            it.name.equals(cleanName, ignoreCase = true) && it.type.equals(category.type, ignoreCase = true)
        }
        if (existing != null) {
            finance.updateCategory(existing.copy(icon = category.icon.ifBlank { existing.icon }))
            return existing.id
        }
        return finance.insertCategory(category.copy(name = cleanName))
    }

    suspend fun updateCategory(category: CategoryEntity) = finance.updateCategory(category)

    /**
     * Protects categories that are currently referenced by transactions or budgets.
     */
    suspend fun deleteCategorySafely(category: CategoryEntity): Result<String> {
        val txCount = transactions.first().count { it.category.equals(category.name, ignoreCase = true) }
        val budgetCount = budgets.first().count { it.category.equals(category.name, ignoreCase = true) }
        if (txCount > 0 || budgetCount > 0) {
            return Result.failure(
                IllegalStateException(
                    "Category '${category.name}' is protected because it is used by $txCount transaction(s) and $budgetCount budget(s)."
                )
            )
        }
        finance.deleteCategory(category)
        return Result.success("Category '${category.name}' deleted.")
    }

    suspend fun deleteCategory(category: CategoryEntity) = finance.deleteCategory(category)

    suspend fun addPerson(person: PersonEntity): Long {
        val cleanName = person.name.trim()
        require(cleanName.isNotEmpty()) { "Person name is required" }
        return finance.insertPerson(person.copy(name = cleanName))
    }

    suspend fun updatePerson(person: PersonEntity) = finance.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = finance.deletePerson(person)

    // Custom Trackers Operations
    suspend fun upsertCustomTracker(tracker: CustomTrackerEntity): Long {
        val now = System.currentTimeMillis()
        val id = customTrackerDao.upsertTracker(tracker.copy(updatedAt = now))
        customTrackerDao.insertActivityLog(
            TrackerActivityLogEntity(
                logUid = UUID.randomUUID().toString(),
                trackerUid = tracker.stableUid,
                actionType = "UPSERT",
                description = "Saved tracker '${tracker.name}'",
                timestamp = now
            )
        )
        return id
    }

    suspend fun archiveCustomTracker(trackerUid: String, archived: Boolean) {
        val status = if (archived) "ARCHIVED" else "ACTIVE"
        customTrackerDao.setTrackerArchived(trackerUid, archived, status)
    }

    suspend fun deleteCustomTracker(trackerUid: String) {
        customTrackerDao.deleteTrackerByUid(trackerUid)
    }

    suspend fun linkTransactionToTracker(
        trackerUid: String,
        transactionUid: String,
        isRefund: Boolean = false,
        paidByPersonUid: String = "",
        splitMode: String = "EQUAL"
    ) {
        customTrackerDao.linkTransactionToTracker(
            TrackerTransactionCrossRefEntity(
                trackerUid = trackerUid,
                transactionUid = transactionUid,
                isRefund = isRefund,
                paidByPersonUid = paidByPersonUid,
                splitMode = splitMode
            )
        )
    }

    suspend fun unlinkTransactionFromTracker(trackerUid: String, transactionUid: String) {
        customTrackerDao.unlinkTransactionFromTracker(trackerUid, transactionUid)
    }

    suspend fun upsertTrackerPerson(person: TrackerPersonEntity) {
        customTrackerDao.upsertTrackerPerson(person)
    }

    suspend fun deleteTrackerPerson(personUid: String) {
        customTrackerDao.deleteTrackerPerson(personUid)
    }

    suspend fun addTrackerSettlement(settlement: TrackerSettlementEntity) {
        customTrackerDao.upsertTrackerSettlement(settlement)
    }

    suspend fun addTrackerAttachment(attachment: TrackerAttachmentEntity) {
        customTrackerDao.upsertTrackerAttachment(attachment)
    }

    suspend fun deleteTrackerAttachment(attachmentUid: String) {
        customTrackerDao.deleteTrackerAttachment(attachmentUid)
    }

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
