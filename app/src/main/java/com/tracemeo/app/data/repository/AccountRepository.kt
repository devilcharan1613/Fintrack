package com.tracemeo.app.data.repository

import com.tracemeo.app.data.local.AccountDao
import com.tracemeo.app.data.local.AccountEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    fun getTotalBalance(): Flow<Double> = accountDao.getComputedTotalBalance()
    suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
    suspend fun archiveAccount(accountId: Long, archived: Boolean) {
        if (archived) accountDao.archiveAccount(accountId)
    }
}
