package com.charan.tracmio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        LoanEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
        LoanPaymentEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class TracmioDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun financeDao(): FinanceDao

    companion object {
        const val DATABASE_NAME = "tracmio_database"
        private const val LEGACY_DATABASE_NAME = "fintrack_database"

        @Volatile
        private var INSTANCE: TracmioDatabase? = null

        fun getDatabase(context: Context): TracmioDatabase {
            return INSTANCE ?: synchronized(this) {
                migrateLegacyDatabaseFileIfNeeded(context.applicationContext)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TracmioDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                ).build()

                INSTANCE = instance
                instance
            }
        }

        private fun migrateLegacyDatabaseFileIfNeeded(context: Context) {
            val newDb = context.getDatabasePath(DATABASE_NAME)
            val oldDb = context.getDatabasePath(LEGACY_DATABASE_NAME)
            if (!newDb.exists() && oldDb.exists()) {
                runCatching {
                    newDb.parentFile?.mkdirs()
                    oldDb.copyTo(newDb, overwrite = false)
                    listOf("-wal", "-shm").forEach { suffix ->
                        val oldAux = context.getDatabasePath(LEGACY_DATABASE_NAME + suffix)
                        val newAux = context.getDatabasePath(DATABASE_NAME + suffix)
                        if (oldAux.exists()) {
                            oldAux.copyTo(newAux, overwrite = true)
                        }
                    }
                }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL, transferAccountId INTEGER, transferId TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, category TEXT NOT NULL, limitAmount REAL NOT NULL, month TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, targetAmount REAL NOT NULL, savedAmount REAL NOT NULL, deadline INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS loans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, principal REAL NOT NULL, paidAmount REAL NOT NULL, monthlyPayment REAL NOT NULL, interestRate REAL NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN personId INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, parentId INTEGER, icon TEXT NOT NULL DEFAULT '', isSystem INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS persons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE TABLE IF NOT EXISTS loan_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanId INTEGER NOT NULL, amount REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Cash'")
                db.execSQL("ALTER TABLE loans ADD COLUMN dueDay INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE loan_payments ADD COLUMN accountId INTEGER")
                db.execSQL("ALTER TABLE loan_payments ADD COLUMN transactionId INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS goal_contributions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goalId INTEGER NOT NULL, accountId INTEGER, transactionId INTEGER, amount REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN balancePaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE accounts SET stableUid = 'account-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE accounts SET balancePaise = CAST(ROUND(balance * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE transactions ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN amountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE transactions SET stableUid = 'txn-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE transactions SET amountPaise = CAST(ROUND(amount * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE budgets ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE budgets ADD COLUMN limitAmountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE budgets SET stableUid = 'budget-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE budgets SET limitAmountPaise = CAST(ROUND(limitAmount * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE goals ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goals ADD COLUMN targetAmountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN savedAmountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE goals SET stableUid = 'goal-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE goals SET targetAmountPaise = CAST(ROUND(targetAmount * 100.0) AS INTEGER), savedAmountPaise = CAST(ROUND(savedAmount * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE goal_contributions ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goal_contributions ADD COLUMN amountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE goal_contributions SET stableUid = 'goal-contrib-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE goal_contributions SET amountPaise = CAST(ROUND(amount * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE loans ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loans ADD COLUMN principalPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loans ADD COLUMN paidAmountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loans ADD COLUMN monthlyPaymentPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE loans SET stableUid = 'loan-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE loans SET principalPaise = CAST(ROUND(principal * 100.0) AS INTEGER), paidAmountPaise = CAST(ROUND(paidAmount * 100.0) AS INTEGER), monthlyPaymentPaise = CAST(ROUND(monthlyPayment * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE loan_payments ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loan_payments ADD COLUMN amountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE loan_payments SET stableUid = 'loan-pay-' || id WHERE stableUid = ''")
                db.execSQL("UPDATE loan_payments SET amountPaise = CAST(ROUND(amount * 100.0) AS INTEGER)")

                db.execSQL("ALTER TABLE categories ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE categories SET stableUid = 'cat-' || id WHERE stableUid = ''")

                db.execSQL("ALTER TABLE persons ADD COLUMN stableUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE persons SET stableUid = 'person-' || id WHERE stableUid = ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN upiApp TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
