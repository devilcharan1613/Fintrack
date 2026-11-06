package com.charan.fintrack.data.repository

import androidx.room.withTransaction
import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.BudgetEntity
import com.charan.fintrack.data.local.CategoryEntity
import com.charan.fintrack.data.local.FinTrackDatabase
import com.charan.fintrack.data.local.GoalEntity
import com.charan.fintrack.data.local.LoanEntity
import com.charan.fintrack.data.local.LoanPaymentEntity
import com.charan.fintrack.data.local.PersonEntity
import com.charan.fintrack.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class FinanceRepository(private val database: FinTrackDatabase) {
    private val accounts = database.accountDao()
    private val finance = database.financeDao()

    val accountList: Flow<List<AccountEntity>> = accounts.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = finance.transactions()
    val budgets: Flow<List<BudgetEntity>> = finance.budgets()
    val goals: Flow<List<GoalEntity>> = finance.goals()
    val loans: Flow<List<LoanEntity>> = finance.loans()
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

    suspend fun addAccount(account: AccountEntity) = accounts.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accounts.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
    suspend fun archiveAccount(account: AccountEntity) = accounts.archiveAccount(account.id)

    suspend fun saveTransaction(transaction: TransactionEntity) = finance.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.amount > 0) { "Transaction amount must be positive" }
        database.withTransaction {
            if (transaction.transferId != null) finance.deleteTransfer(transaction.transferId)
            finance.insertTransaction(transaction)
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

    suspend fun addTransfer(from: Long, to: Long, amount: Double, note: String) {
        require(from != to) { "Transfer accounts must be different" }
        require(amount > 0) { "Transfer amount must be positive" }
        val transferId = UUID.randomUUID().toString()
        database.withTransaction {
            finance.insertTransaction(
                TransactionEntity(
                    accountId = from,
                    type = "TRANSFER_OUT",
                    amount = amount,
                    category = "Transfer",
                    note = note,
                    transferAccountId = to,
                    transferId = transferId
                )
            )
            finance.insertTransaction(
                TransactionEntity(
                    accountId = to,
                    type = "TRANSFER_IN",
                    amount = amount,
                    category = "Transfer",
                    note = note,
                    transferAccountId = from,
                    transferId = transferId
                )
            )
        }
    }

    suspend fun addBudget(budget: BudgetEntity) = finance.insertBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = finance.deleteBudget(budget)
    suspend fun addGoal(goal: GoalEntity) = finance.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = finance.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = finance.deleteGoal(goal)
    suspend fun addLoan(loan: LoanEntity) = finance.insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = finance.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = finance.deleteLoan(loan)
    suspend fun addLoanPayment(payment: LoanPaymentEntity) = finance.insertLoanPayment(payment)
    fun loanPayments(loanId: Long): Flow<List<LoanPaymentEntity>> = finance.loanPayments(loanId)

    suspend fun addCategory(category: CategoryEntity) = finance.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = finance.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = finance.deleteCategory(category)
    suspend fun addPerson(person: PersonEntity) = finance.insertPerson(person)
    suspend fun updatePerson(person: PersonEntity) = finance.updatePerson(person)
    suspend fun deletePerson(person: PersonEntity) = finance.deletePerson(person)

    suspend fun exportJson(): String {
        val accountRows = accountList.first()
        val transactionRows = transactions.first()
        val budgetRows = budgets.first()
        val goalRows = goals.first()
        val loanRows = loans.first()
        val categoryRows = categories.first()
        val personRows = persons.first()
        fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        return buildString {
            append("{\"version\":2,\"accounts\":[")
            append(accountRows.joinToString(",") { "{\"id\":${it.id},\"name\":${quote(it.name)},\"type\":${quote(it.accountType)},\"balance\":${it.balance},\"currency\":${quote(it.currency)}}"} )
            append("],\"transactions\":[")
            append(transactionRows.joinToString(",") { "{\"accountId\":${it.accountId},\"type\":${quote(it.type)},\"amount\":${it.amount},\"category\":${quote(it.category)},\"subcategory\":${quote(it.subcategory)},\"note\":${quote(it.note)},\"timestamp\":${it.timestamp},\"transferAccountId\":${it.transferAccountId ?: "null"},\"transferId\":${it.transferId?.let { value -> quote(value) } ?: "null"}}"} )
            append("],\"budgets\":[")
            append(budgetRows.joinToString(",") { "{\"category\":${quote(it.category)},\"limit\":${it.limitAmount},\"month\":${quote(it.month)}}"} )
            append("],\"goals\":[")
            append(goalRows.joinToString(",") { "{\"name\":${quote(it.name)},\"target\":${it.targetAmount},\"saved\":${it.savedAmount}}"} )
            append("],\"loans\":[")
            append(loanRows.joinToString(",") { "{\"name\":${quote(it.name)},\"principal\":${it.principal},\"paid\":${it.paidAmount},\"monthly\":${it.monthlyPayment},\"interest\":${it.interestRate}}"} )
            append("],\"categories\":[")
            append(categoryRows.joinToString(",") { "{\"name\":${quote(it.name)},\"type\":${quote(it.type)},\"parentId\":${it.parentId ?: "null"},\"icon\":${quote(it.icon)},\"system\":${it.isSystem}}"} )
            append("],\"persons\":[")
            append(personRows.joinToString(",") { "{\"name\":${quote(it.name)},\"phone\":${quote(it.phone)},\"notes\":${quote(it.notes)}}"} )
            append("]}")
        }
    }

    suspend fun exportCsv(): String {
        val rows = transactions.first()
        return buildString {
            appendLine("id,accountId,type,amount,category,note,timestamp")
            rows.forEach {
                appendLine(listOf(it.id, it.accountId, it.type, it.amount, it.category, it.note, it.timestamp)
                    .joinToString(",") { value -> "\"${value.toString().replace("\"", "\"\"")}\"" })
            }
        }
    }

    suspend fun restoreJson(json: String) {
            fun section(name: String): String {
                val start = json.indexOf("\"$name\":[")
                if (start < 0) return ""
                val contentStart = start + name.length + 4
                val end = json.indexOf("]", contentStart)
                return if (end < 0) "" else json.substring(contentStart, end)
            }
            fun objects(value: String): List<String> = Regex("""\{[^{}]*\}""").findAll(value).map { it.value }.toList()
            fun text(obj: String, key: String): String =
                Regex(""""$key":"((?:\\.|[^"])*)"""").find(obj)?.groupValues?.get(1)
                    ?.replace("\\\"", "\"")?.replace("\\\\", "\\") ?: ""
            fun number(obj: String, key: String): Double =
                Regex(""""$key":(-?[0-9]+(?:\.[0-9]+)?)""").find(obj)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val accountIds = mutableMapOf<Long, Long>()
            database.withTransaction {
                objects(section("accounts")).forEach { obj ->
                    val oldId = number(obj, "id").toLong()
                    val newId = accounts.insertAccount(
                        AccountEntity(
                            name = text(obj, "name"),
                            accountType = text(obj, "type"),
                            balance = number(obj, "balance"),
                            currency = text(obj, "currency").ifBlank { "INR" }
                        )
                    )
                    accountIds[oldId] = newId
                }
                objects(section("transactions")).forEach { obj ->
                    accountIds[number(obj, "accountId").toLong()]?.let { accountId ->
                        finance.insertTransaction(
                            TransactionEntity(
                                accountId = accountId,
                                type = text(obj, "type"),
                                amount = number(obj, "amount"),
                                category = text(obj, "category"),
                                note = text(obj, "note"),
                                subcategory = text(obj, "subcategory"),
                                timestamp = number(obj, "timestamp").toLong().takeIf { it > 0 } ?: System.currentTimeMillis(),
                                transferAccountId = number(obj, "transferAccountId").toLong().takeIf { it > 0 },
                                transferId = text(obj, "transferId").ifBlank { null }
                            )
                        )
                    }
                }
                objects(section("budgets")).forEach { obj ->
                    finance.insertBudget(
                        BudgetEntity(
                            category = text(obj, "category"),
                            limitAmount = number(obj, "limit"),
                            month = text(obj, "month")
                        )
                    )
                }
                objects(section("goals")).forEach { obj ->
                    finance.insertGoal(GoalEntity(name = text(obj, "name"), targetAmount = number(obj, "target"), savedAmount = number(obj, "saved")))
                }
                objects(section("loans")).forEach { obj ->
                    finance.insertLoan(LoanEntity(name = text(obj, "name"), principal = number(obj, "principal"), paidAmount = number(obj, "paid"), monthlyPayment = number(obj, "monthly"), interestRate = number(obj, "interest")))
                }
                objects(section("categories")).forEach { obj ->
                    finance.insertCategory(CategoryEntity(name = text(obj, "name"), type = text(obj, "type"), icon = text(obj, "icon"), isSystem = text(obj, "system") == "true"))
                }
                objects(section("persons")).forEach { obj ->
                    finance.insertPerson(PersonEntity(name = text(obj, "name"), phone = text(obj, "phone"), notes = text(obj, "notes")))
                }
            }
        }
    }
}
