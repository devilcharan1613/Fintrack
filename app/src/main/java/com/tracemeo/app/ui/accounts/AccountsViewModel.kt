package com.tracemeo.app.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TracmioDatabase
import com.tracemeo.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AccountRepository(TracmioDatabase.getInstance(application).accountDao())

    val accounts: StateFlow<List<AccountEntity>> = repo.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = repo.getTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun saveAccount(account: AccountEntity) = viewModelScope.launch { repo.insertAccount(account) }
    fun deleteAccount(account: AccountEntity) = viewModelScope.launch { repo.deleteAccount(account) }
}
