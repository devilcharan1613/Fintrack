package com.charan.fintrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.charan.fintrack.data.local.FinTrackDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinTrackDatabaseMigrationTest {
    @Test
    fun versionThreeDataMigratesToPaiseWithoutLosingLedgerValues() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "fintrack-v3-migration-test.db"
        context.deleteDatabase(databaseName)
        val legacy = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        legacy.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, accountType TEXT NOT NULL, balance REAL NOT NULL, currency TEXT NOT NULL, institution TEXT NOT NULL, accountNumber TEXT NOT NULL, notes TEXT NOT NULL, isArchived INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        legacy.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, timestamp INTEGER NOT NULL, transferAccountId INTEGER, transferId TEXT, subcategory TEXT NOT NULL, personId INTEGER)")
        legacy.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, category TEXT NOT NULL, limitAmount REAL NOT NULL, month TEXT NOT NULL)")
        legacy.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, targetAmount REAL NOT NULL, savedAmount REAL NOT NULL, deadline INTEGER)")
        legacy.execSQL("CREATE TABLE loans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, principal REAL NOT NULL, paidAmount REAL NOT NULL, monthlyPayment REAL NOT NULL, interestRate REAL NOT NULL)")
        legacy.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, parentId INTEGER, icon TEXT NOT NULL, isSystem INTEGER NOT NULL)")
        legacy.execSQL("CREATE TABLE persons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, notes TEXT NOT NULL)")
        legacy.execSQL("CREATE TABLE loan_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanId INTEGER NOT NULL, amount REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL)")
        legacy.execSQL("INSERT INTO accounts VALUES (1, 'Cash', 'Wallet', 1000.00, 'INR', '', '', '', 0, 1000, 1000)")
        legacy.execSQL("INSERT INTO transactions VALUES (1, 1, 'INCOME', 25.55, 'Gift', '', 2000, NULL, NULL, '', NULL)")
        legacy.execSQL("INSERT INTO budgets VALUES (1, 'Food', 300.75, '2026-11')")
        legacy.execSQL("INSERT INTO goals VALUES (1, 'Trip', 5000.00, 125.05, NULL)")
        legacy.execSQL("INSERT INTO loans VALUES (1, 'Phone', 10000.00, 1200.10, 800.00, 0.0)")
        legacy.execSQL("INSERT INTO loan_payments VALUES (1, 1, 1200.10, 3000, '')")
        legacy.version = 3
        legacy.close()

        val migrated = Room.databaseBuilder(context, FinTrackDatabase::class.java, databaseName)
            .addMigrations(*FinTrackDatabase.ALL_MIGRATIONS)
            .build()
        try {
            val account = migrated.accountDao().getAccountById(1L)!!
            val txn = migrated.financeDao().allTransactions().single()
            val budget = migrated.financeDao().allBudgets().single()
            val goal = migrated.financeDao().allGoals().single()
            val loan = migrated.financeDao().allLoans().single()
            val repayment = migrated.financeDao().allLoanPayments().single()

            assertEquals(100_000L, account.openingBalancePaise)
            assertEquals(2_555L, txn.amountPaise)
            assertEquals(30_075L, budget.limitAmountPaise)
            assertEquals(500_000L, goal.targetAmountPaise)
            assertEquals(12_505L, goal.savedAmountPaise)
            assertEquals(1_000_000L, loan.principalPaise)
            assertEquals(120_010L, loan.paidAmountPaise)
            assertEquals(80_000L, loan.monthlyPaymentPaise)
            assertEquals(120_010L, repayment.amountPaise)
            assertFalse(account.stableUid.isBlank())
            assertFalse(txn.stableUid.isBlank())
            assertTrue(migrated.financeDao().allGoalContributions().isEmpty())
            assertEquals(0, migrated.openHelper.writableDatabase.version - 5)
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }
}
