package com.charan.tracmio.ui.nativeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.charan.tracmio.data.MoneyPaise
import com.charan.tracmio.data.NativeLedgerCalculator
import com.charan.tracmio.data.PersonLedgerSummary
import com.charan.tracmio.data.local.AccountEntity
import com.charan.tracmio.data.local.BudgetEntity
import com.charan.tracmio.data.local.CategoryEntity
import com.charan.tracmio.data.local.GoalContributionEntity
import com.charan.tracmio.data.local.GoalEntity
import com.charan.tracmio.data.local.LoanEntity
import com.charan.tracmio.data.local.LoanPaymentEntity
import com.charan.tracmio.data.local.PersonEntity
import com.charan.tracmio.data.local.TransactionEntity
import com.charan.tracmio.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NativeTracmioUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val goalContributions: List<GoalContributionEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanPayments: List<LoanPaymentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val statusMessage: String? = null
) {
    val totalBalance: Double
        get() = NativeLedgerCalculator.totalBalance(accounts, transactions)

    val totalIncome: Double
        get() = NativeLedgerCalculator.totalIncome(transactions)

    val totalExpense: Double
        get() = NativeLedgerCalculator.totalExpense(transactions)

    val personSummaries: List<PersonLedgerSummary>
        get() = NativeLedgerCalculator.personSummaries(persons, transactions)
}

class NativeTracmioViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val statusMessage = MutableStateFlow<String?>(null)

    private val primaryFlow = combine(
        repository.accountList,
        repository.transactions,
        repository.budgets,
        repository.goals,
        repository.goalContributions
    ) { accounts, transactions, budgets, goals, goalContribs ->
        NativeTracmioUiState(
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            goalContributions = goalContribs
        )
    }

    private val secondaryFlow = combine(
        repository.loans,
        repository.loanPaymentsAll,
        repository.categories,
        repository.persons,
        statusMessage
    ) { loans, loanPayments, categories, persons, message ->
        NativeTracmioUiState(
            loans = loans,
            loanPayments = loanPayments,
            categories = categories,
            persons = persons,
            statusMessage = message
        )
    }

    val uiState: StateFlow<NativeTracmioUiState> = combine(primaryFlow, secondaryFlow) { p, s ->
        p.copy(
            loans = s.loans,
            loanPayments = s.loanPayments,
            categories = s.categories,
            persons = s.persons,
            statusMessage = s.statusMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NativeTracmioUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }

    fun addAccount(name: String, type: String, balance: Double, currency: String = "INR") {
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = name.trim(),
                    accountType = type.trim().ifBlank { "Bank" },
                    balance = balance,
                    balancePaise = MoneyPaise.toPaise(balance),
                    currency = currency
                )
            )
            statusMessage.value = "Account saved"
        }
    }

    fun addTransaction(
        accountId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String = "",
        paymentMethod: String = "Cash",
        upiApp: String = ""
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        type = type.uppercase(),
                        amount = amount,
                        amountPaise = MoneyPaise.toPaise(amount),
                        category = category.trim().ifBlank { "General" },
                        note = note.trim(),
                        paymentMethod = paymentMethod,
                        upiApp = upiApp
                    )
                )
            }.onSuccess {
                statusMessage.value = "Transaction saved"
            }.onFailure {
                statusMessage.value = it.message
            }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    companion object {
        fun factory(repository: FinanceRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NativeTracmioViewModel(repository) as T
                }
            }
    }
}
