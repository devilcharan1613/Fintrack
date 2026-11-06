package com.charan.fintrack.data.repository

import com.charan.fintrack.data.local.AccountDao
import com.charan.fintrack.data.local.AccountEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val accountDao: AccountDao
) {
    fun getAllAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getAllAccounts()
    }

    fun getTotalBalance(): Flow<Double?> {
        return accountDao.getComputedTotalBalance()
    }

    suspend fun getAccountById(id: Long): AccountEntity? {
        return accountDao.getAccountById(id)
    }

    suspend fun insertAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(
            account.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun archiveAccount(id: Long) {
        accountDao.archiveAccount(id)
    }

    fun getBalance(id: Long): Flow<Double> = accountDao.getBalance(id)
}
