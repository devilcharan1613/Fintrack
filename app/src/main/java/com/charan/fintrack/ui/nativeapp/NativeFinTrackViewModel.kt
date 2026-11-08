package com.charan.fintrack.ui.nativeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charan.fintrack.data.MoneyPaise
import com.charan.fintrack.data.local.*
import com.charan.fintrack.data.repository.NativeFinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NativeFinTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NativeFinanceRepository(FinTrackDatabase.getDatabase(application))

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
        viewModelScope, SharingStarted.WhileSubscribed(5_000), com.charan.fintrack.data.repository.NativeDashboardTotals()
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

    fun clearMessage() { _message.value = null }

    fun addAccount(name: String, type: String, openingRupees: String) = perform {
        repository.addAccount(name, type, MoneyPaise.fromSignedRupees(openingRupees))
    }

    fun archiveAccount(id: Long) = perform { repository.archiveAccount(id) }

    fun addTransaction(
        type: String, amountRupees: String, accountId: Long, category: String,
        note: String, destinationAccountId: Long? = null
    ) = perform {
        val amount = MoneyPaise.fromRupees(amountRupees)
        if (type.equals("TRANSFER", true)) {
            val target = destinationAccountId ?: throw IllegalArgumentException("Choose a destination account")
            repository.addTransfer(accountId, target, amount, note)
        } else {
            repository.saveTransaction(accountId, type, amount, category, note)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) = perform {
        repository.deleteTransaction(transaction)
    }

    fun addPerson(name: String, phone: String, notes: String) = perform {
        repository.addPerson(name, phone, notes)
    }

    fun addCategory(name: String, type: String) = perform {
        repository.addCategory(name, type)
    }

    fun addBudget(category: String, limitRupees: String, month: String) = perform {
        repository.addBudget(category, MoneyPaise.fromRupees(limitRupees), month)
    }

    fun addGoal(name: String, targetRupees: String) = perform {
        repository.addGoal(name, MoneyPaise.fromRupees(targetRupees))
    }

    fun contribute(goalId: Long, amountRupees: String) = perform {
        repository.addGoalContribution(goalId, MoneyPaise.fromRupees(amountRupees))
    }

    fun addLoan(name: String, principalRupees: String, monthlyRupees: String, interestRate: String) = perform {
        repository.addLoan(
            name,
            MoneyPaise.fromRupees(principalRupees),
            MoneyPaise.fromRupees(monthlyRupees.ifBlank { "0" }),
            interestRate.toDoubleOrNull() ?: 0.0
        )
    }

    fun repay(loanId: Long, amountRupees: String, note: String) = perform {
        repository.addLoanRepayment(loanId, MoneyPaise.fromRupees(amountRupees), note)
    }

    fun deleteBudget(item: BudgetEntity) = perform { repository.deleteBudget(item) }
    fun deleteGoal(item: GoalEntity) = perform { repository.deleteGoal(item) }
    fun deleteLoan(item: LoanEntity) = perform { repository.deleteLoan(item) }
    fun deleteCategory(item: CategoryEntity) = perform { repository.deleteCategory(item) }
    fun deletePerson(item: PersonEntity) = perform { repository.deletePerson(item) }
}
