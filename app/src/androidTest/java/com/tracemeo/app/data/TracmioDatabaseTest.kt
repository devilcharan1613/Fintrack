package com.tracemeo.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TracmioDatabase
import com.tracemeo.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TracmioDatabaseTest {
    private lateinit var database: TracmioDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TracmioDatabase::class.java).build()
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
            TransactionEntity(
                accountId = accountId,
                type = "EXPENSE",
                amount = 12.0,
                category = "Coffee",
                paymentMethod = "UPI",
                upiApp = "GPay"
            )
        )

        assertEquals(1, database.accountDao().getAccountById(accountId)?.id)
        assertEquals(1, database.financeDao().transactions().first().size)
    }
}
