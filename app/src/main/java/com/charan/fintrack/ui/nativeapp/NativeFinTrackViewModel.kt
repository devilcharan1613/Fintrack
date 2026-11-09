package com.charan.fintrack.ui.nativeapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charan.fintrack.XlsxExporter
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.backup.NativeBackupService
import com.charan.fintrack.data.local.*
import com.charan.fintrack.data.repository.NativeFinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class NativeFinTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FinTrackDatabase.getDatabase(application)
    private val repository = NativeFinanceRepository(database)
    private val backupService = NativeBackupService(database)

    val accounts: StateFlow<List<AccountEntity>> =
        repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val transactions: StateFlow<List<TransactionEntity>> =
        repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories: StateFlow<List<CategoryEntity>> =
        repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val people: StateFlow<List<PersonEntity>> =
        repository.people.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val budgets: StateFlow<List<BudgetEntity>> =
        repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals: StateFlow<List<GoalEntity>> =
        repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val loans: StateFlow<List<LoanEntity>> =
        repository.loans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dashboard = repository.dashboard.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        com.charan.fintrack.data.repository.NativeDashboardTotals()
    )

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private fun perform(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _message.value = "Saved successfully"
            } catch (error: Exception) {
                _message.value = error.message ?: "Unable to save this change"
            }
        }
    }

    private fun reportFailure(error: Exception) {
        _message.value = error.message ?: "Operation failed"
    }

    fun clearMessage() { _message.value = null }

    fun addAccount(
        name: String,
        type: String,
        openingRupees: String,
        creditLimitRupees: String = "0",
        institution: String = "",
        accountNumber: String = "",
        notes: String = ""
    ) = perform {
        repository.addAccount(
            name,
            type,
            MoneyPaise.fromSignedRupees(openingRupees),
            MoneyPaise.fromRupees(creditLimitRupees.ifBlank { "0" }),
            institution,
            accountNumber,
            notes
        )
    }

    fun updateAccount(
        account: AccountEntity,
        name: String,
        type: String,
        openingRupees: String,
        creditLimitRupees: String,
        institution: String = account.institution,
        accountNumber: String = account.accountNumber,
        notes: String = account.notes
    ) = perform {
        val opening = MoneyPaise.fromSignedRupees(openingRupees)
        val credit = MoneyPaise.fromRupees(creditLimitRupees.ifBlank { "0" })
        repository.updateAccount(
            account.copy(
                name = name.trim(), accountType = type.trim(),
                balance = opening.toRupeesDecimal().toDouble(),
                openingBalancePaise = opening.paise,
                creditLimit = credit.toRupeesDecimal().toDouble(),
                creditLimitPaise = credit.paise,
                institution = institution.trim(),
                accountNumber = accountNumber.trim(),
                notes = notes.trim()
            )
        )
    }

    fun archiveAccount(id: Long) = perform { repository.archiveAccount(id) }

    fun addTransaction(
        type: String,
        amountRupees: String,
        accountId: Long,
        category: String,
        note: String,
        destinationAccountId: Long? = null,
        personId: Long? = null,
        paymentMethod: String = "",
        upiApp: String = "",
        date: String = "",
        time: String = "",
        subcategory: String = ""
    ) = perform {
        val amount = MoneyPaise.fromRupees(amountRupees)
        val timestamp = parseDateTime(date, time)
        if (type.equals("TRANSFER", true)) {
            val target = destinationAccountId ?: throw IllegalArgumentException("Choose a destination account")
            repository.addTransfer(accountId, target, amount, note, timestamp)
        } else {
            repository.saveTransaction(
                accountId = accountId,
                type = type,
                amount = amount,
                category = category.ifBlank { if (type.equals("BORROW", true)) "Borrowed" else if (type.equals("LEND", true)) "Lent" else "Other" },
                note = note,
                timestamp = timestamp,
                personId = personId,
                paymentMethod = paymentMethod,
                upiApp = upiApp,
                subcategory = subcategory
            )
        }
    }

    fun updateTransaction(
        original: TransactionEntity,
        type: String,
        amountRupees: String,
        accountId: Long,
        category: String,
        note: String,
        destinationAccountId: Long?,
        personId: Long?,
        paymentMethod: String,
        upiApp: String,
        date: String,
        time: String,
        subcategory: String = ""
    ) = perform {
        val amount = MoneyPaise.fromRupees(amountRupees)
        val timestamp = parseDateTime(date, time)
        repository.replaceTransaction(
            original.copy(
                accountId = accountId,
                type = type,
                amount = amount.toRupeesDecimal().toDouble(),
                amountPaise = amount.paise,
                category = if (type.equals("TRANSFER", true)) "Transfer" else category,
                note = note,
                timestamp = timestamp,
                personId = personId,
                paymentMethod = paymentMethod,
                upiApp = upiApp,
                subcategory = subcategory.trim()
            ),
            destinationAccountId
        )
    }

    fun deleteTransaction(transaction: TransactionEntity) = perform {
        repository.deleteTransaction(transaction)
    }

    fun addPerson(name: String, phone: String, notes: String) = perform {
        repository.addPerson(name, phone, notes)
    }

    fun updatePerson(person: PersonEntity, name: String, phone: String, notes: String) = perform {
        repository.updatePerson(person.copy(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
    }

    fun addCategory(name: String, type: String, icon: String, subcategoriesText: String) = perform {
        val subcategories = subcategoriesText.lines().map { it.trim() }.filter { it.isNotBlank() }
        repository.addCategory(name, type, icon, subcategories)
    }

    fun updateCategory(category: CategoryEntity, name: String, type: String, icon: String, subcategoriesText: String) = perform {
        val subcategories = subcategoriesText.lines().map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
        repository.updateCategory(
            category.copy(
                name = name.trim(), type = type.uppercase(), icon = icon,
                subcategoriesJson = org.json.JSONArray(subcategories).toString()
            )
        )
    }

    fun resetAllData() = perform { repository.resetAllFinanceData() }

    fun addBudget(category: String, limitRupees: String, month: String, period: String) = perform {
        repository.addBudget(category, MoneyPaise.fromRupees(limitRupees), month, period)
    }

    fun updateBudget(item: BudgetEntity, category: String, limitRupees: String, month: String, period: String) = perform {
        val limit = MoneyPaise.fromRupees(limitRupees)
        repository.updateBudget(
            item.copy(
                category = category.trim(), limitAmount = limit.toRupeesDecimal().toDouble(),
                limitAmountPaise = limit.paise, month = month, period = period
            )
        )
    }

    fun addGoal(name: String, targetRupees: String) = perform {
        repository.addGoal(name, MoneyPaise.fromRupees(targetRupees))
    }

    fun updateGoal(goal: GoalEntity, name: String, targetRupees: String) = perform {
        val target = MoneyPaise.fromRupees(targetRupees)
        require(goal.savedAmountPaise <= target.paise) { "Target cannot be lower than the amount already saved" }
        repository.updateGoal(
            goal.copy(name = name.trim(), targetAmount = target.toRupeesDecimal().toDouble(), targetAmountPaise = target.paise)
        )
    }

    fun contribute(goalId: Long, amountRupees: String, note: String = "") = perform {
        repository.addGoalContribution(goalId, MoneyPaise.fromRupees(amountRupees), note)
    }

    fun addLoan(name: String, principalRupees: String, monthlyRupees: String, interestRate: String) = perform {
        val rate = interestRate.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid interest rate")
        require(rate >= 0.0 && rate.isFinite()) { "Interest rate cannot be negative" }
        repository.addLoan(
            name,
            MoneyPaise.fromRupees(principalRupees),
            MoneyPaise.fromRupees(monthlyRupees.ifBlank { "0" }),
            rate
        )
    }

    fun repay(loanId: Long, amountRupees: String, note: String) = perform {
        repository.addLoanRepayment(loanId, MoneyPaise.fromRupees(amountRupees), note)
    }

    fun updateLoan(loan: LoanEntity, name: String, principalRupees: String, monthlyRupees: String, interestRate: String) = perform {
        val principal = MoneyPaise.fromRupees(principalRupees)
        val monthly = MoneyPaise.fromRupees(monthlyRupees.ifBlank { "0" })
        require(principal.paise >= loan.paidAmountPaise) { "Principal cannot be lower than amounts already repaid" }
        val rate = interestRate.toDoubleOrNull() ?: throw IllegalArgumentException("Enter a valid interest rate")
        require(rate >= 0.0 && rate.isFinite()) { "Interest rate cannot be negative" }
        repository.updateLoan(
            loan.copy(
                name = name.trim(),
                principal = principal.toRupeesDecimal().toDouble(),
                principalPaise = principal.paise,
                monthlyPayment = monthly.toRupeesDecimal().toDouble(),
                monthlyPaymentPaise = monthly.paise,
                interestRate = rate
            )
        )
    }

    fun goalContributions(goalId: Long): Flow<List<GoalContributionEntity>> =
        repository.goalContributions(goalId)

    fun loanPayments(loanId: Long): Flow<List<LoanPaymentEntity>> =
        repository.loanPayments(loanId)

    fun deleteBudget(item: BudgetEntity) = perform { repository.deleteBudget(item) }
    fun deleteGoal(item: GoalEntity) = perform { repository.deleteGoal(item) }
    fun deleteLoan(item: LoanEntity) = perform { repository.deleteLoan(item) }
    fun deleteCategory(item: CategoryEntity) = perform { repository.deleteCategory(item) }
    fun deletePerson(item: PersonEntity) = perform { repository.deletePerson(item) }

    fun exportBackup(uri: Uri) = exportFile(uri) { backupService.exportJson().toByteArray(Charsets.UTF_8) }
    fun exportCsv(uri: Uri) = exportFile(uri) { ("﻿" + backupService.exportCsv()).toByteArray(Charsets.UTF_8) }
    fun exportXlsx(uri: Uri) = exportFile(uri) { XlsxExporter.create(backupService.exportJson()) }
    fun exportPdf(uri: Uri) = exportFile(uri) {
        NativeStatementPdf.create(database.accountDao().allAccounts(), database.financeDao().allTransactions())
    }

    private fun exportFile(uri: Uri, produce: suspend () -> ByteArray) {
        viewModelScope.launch {
            try {
                val bytes = produce()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: throw IllegalStateException("Unable to open the selected destination")
                _message.value = "Export saved successfully"
            } catch (error: Exception) {
                reportFailure(error)
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input, Charsets.UTF_8).buffered().use { it.readText() }
                } ?: throw IllegalStateException("Unable to open the selected backup")
                val result = backupService.mergeJson(json)
                _message.value = "Restore complete: ${result.totalAdded} added, ${result.duplicatesSkipped} already present"
            } catch (error: Exception) {
                reportFailure(error)
            }
        }
    }

    private fun parseDateTime(date: String, time: String): Long {
        if (date.isBlank()) return System.currentTimeMillis()
        val input = date + " " + time.ifBlank { "12:00" }
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
            .parse(input)?.time ?: throw IllegalArgumentException("Invalid transaction date or time")
    }
}
