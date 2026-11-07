package com.charan.fintrack.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = 1, updatedAt = :timestamp WHERE id = :accountId")
    suspend fun archiveAccount(
        accountId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT COALESCE(SUM(a.balance), 0) + COALESCE((SELECT SUM(CASE WHEN t.type IN ('INCOME', 'TRANSFER_IN', 'BORROW') THEN t.amount ELSE -t.amount END) FROM transactions t WHERE t.accountId = a.id), 0) FROM accounts a WHERE a.id = :accountId AND a.isArchived = 0")
    fun getBalance(accountId: Long): Flow<Double>

    @Query("SELECT COALESCE((SELECT SUM(a.balance) FROM accounts a WHERE a.isArchived = 0), 0) + COALESCE((SELECT SUM(CASE WHEN t.type IN ('INCOME', 'TRANSFER_IN', 'BORROW') THEN t.amount ELSE -t.amount END) FROM transactions t INNER JOIN accounts a ON a.id = t.accountId WHERE a.isArchived = 0), 0)")
    fun getComputedTotalBalance(): Flow<Double>
}
