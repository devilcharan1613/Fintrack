package com.tracmio.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracmio.app.data.MoneyPaise
import com.tracmio.app.data.local.AccountEntity
import com.tracmio.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val repository: AccountRepository
) : ViewModel() {

    val accounts = repository.getAllAccounts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val totalBalance = repository.getTotalBalance()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

    fun addAccount(
        name: String,
        accountType: String,
        balance: Double,
        institution: String,
        currency: String = "INR"
    ) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name,
                    accountType = accountType,
                    balance = balance,
                    balancePaise = MoneyPaise.toPaise(balance),
                    institution = institution,
                    currency = currency
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun archiveAccount(id: Long) {
        viewModelScope.launch {
            repository.archiveAccount(id)
        }
    }
}
