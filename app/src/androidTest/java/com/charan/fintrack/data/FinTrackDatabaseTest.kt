package com.charan.fintrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charan.fintrack.data.local.AccountEntity
import com.charan.fintrack.data.local.FinTrackDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinTrackDatabaseTest {
    private lateinit var database: FinTrackDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun accountAndTransactionArePersisted() = runBlocking {
        val accountId = database.accountDao().insertAccount(
            AccountEntity(name = "Cash", accountType = "Wallet", balance = 50.0)
        )
        database.financeDao().insertTransaction(
            com.charan.fintrack.data.local.TransactionEntity(
                accountId = accountId,
                type = "EXPENSE",
                amount = 12.0,
                category = "Coffee"
            )
        )

        assertEquals(1L, database.accountDao().getAccountById(accountId)?.id)
        assertEquals(1, database.financeDao().transactions().first().size)
    }
}
