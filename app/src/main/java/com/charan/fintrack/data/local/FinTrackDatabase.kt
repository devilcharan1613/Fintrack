package com.charan.fintrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        LoanEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
        LoanPaymentEntity::class,
        GoalContributionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FinTrackDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinTrackDatabase? = null

        fun getDatabase(context: Context): FinTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinTrackDatabase::class.java,
                    "fintrack_database"
                ).addMigrations(*ALL_MIGRATIONS).build()

                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL, transferAccountId INTEGER, transferId TEXT)")
                db.execSQL("CREATE TABLE IF NOT EXISTS budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, category TEXT NOT NULL, limitAmount REAL NOT NULL, month TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, targetAmount REAL NOT NULL, savedAmount REAL NOT NULL, deadline INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS loans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, principal REAL NOT NULL, paidAmount REAL NOT NULL, monthlyPayment REAL NOT NULL, interestRate REAL NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN personId INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, parentId INTEGER, icon TEXT NOT NULL DEFAULT '', isSystem INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS persons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE TABLE IF NOT EXISTS loan_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanId INTEGER NOT NULL, amount REAL NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')")
            }
        }

        /**
         * V3 -> V4 adds exact paise columns and stable UUIDs without removing or
         * rewriting any legacy rupee column. Room runs migrations transactionally;
         * any invalid/overflowing legacy amount aborts the migration rather than
         * silently truncating a user's balance.
         */
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                addColumn(db, "accounts", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "accounts", "openingBalancePaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "transactions", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "transactions", "amountPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "budgets", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "budgets", "limitAmountPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "goals", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "goals", "targetAmountPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "goals", "savedAmountPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "loans", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "loans", "principalPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "loans", "paidAmountPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "loans", "monthlyPaymentPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "loan_payments", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "loan_payments", "amountPaise", "INTEGER NOT NULL DEFAULT 0")
                addColumn(db, "categories", "stableUid", "TEXT NOT NULL DEFAULT ''")
                addColumn(db, "persons", "stableUid", "TEXT NOT NULL DEFAULT ''")

                backfillSignedMoney(db, "accounts", "balance", "openingBalancePaise")
                backfillMoney(db, "transactions", "amount", "amountPaise")
                backfillMoney(db, "budgets", "limitAmount", "limitAmountPaise")
                backfillMoney(db, "goals", "targetAmount", "targetAmountPaise")
                backfillMoney(db, "goals", "savedAmount", "savedAmountPaise")
                backfillMoney(db, "loans", "principal", "principalPaise")
                backfillMoney(db, "loans", "paidAmount", "paidAmountPaise")
                backfillMoney(db, "loans", "monthlyPayment", "monthlyPaymentPaise")
                backfillMoney(db, "loan_payments", "amount", "amountPaise")

                listOf("accounts", "transactions", "budgets", "goals", "loans",
                    "loan_payments", "categories", "persons").forEach { table ->
                    backfillStableUids(db, table)
                }

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_stableUid ON accounts(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_stableUid ON transactions(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_stableUid ON budgets(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_stableUid ON goals(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_loans_stableUid ON loans(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_loan_payments_stableUid ON loan_payments(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_stableUid ON categories(stableUid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_persons_stableUid ON persons(stableUid)")
            }
        }

        private fun addColumn(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            column: String,
            definition: String
        ) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }

        private fun backfillStableUids(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String
        ) {
            db.query("SELECT id FROM $table WHERE stableUid = ''").use { cursor ->
                val ids = mutableListOf<Long>()
                while (cursor.moveToNext()) ids += cursor.getLong(0)
                ids.forEach { id ->
                    db.execSQL(
                        "UPDATE $table SET stableUid = ? WHERE id = ?",
                        arrayOf(java.util.UUID.randomUUID().toString(), id)
                    )
                }
            }
        }

        private fun backfillMoney(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            legacyColumn: String,
            paiseColumn: String
        ) {
            db.query("SELECT id, $legacyColumn FROM $table").use { cursor ->
                val rows = mutableListOf<Pair<Long, Double>>()
                while (cursor.moveToNext()) rows += cursor.getLong(0) to cursor.getDouble(1)
                rows.forEach { (id, amount) ->
                    val paise = com.charan.fintrack.data.MoneyPaise.fromLegacyRupees(amount).paise
                    db.execSQL("UPDATE $table SET $paiseColumn = ? WHERE id = ?", arrayOf(paise, id))
                }
            }
        }

        private fun backfillSignedMoney(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            legacyColumn: String,
            paiseColumn: String
        ) {
            db.query("SELECT id, $legacyColumn FROM $table").use { cursor ->
                val rows = mutableListOf<Pair<Long, Double>>()
                while (cursor.moveToNext()) rows += cursor.getLong(0) to cursor.getDouble(1)
                rows.forEach { (id, amount) ->
                    val paise = com.charan.fintrack.data.MoneyPaise.fromLegacySignedRupees(amount).paise
                    db.execSQL(
                        "UPDATE $table SET $paiseColumn = ? WHERE id = ?",
                        arrayOf(paise, id)
                    )
                }
            }
        }


        /** V4 -> V5 preserves additional legacy fields and savings contribution history. */
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimitPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN upiApp TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE budgets ADD COLUMN period TEXT NOT NULL DEFAULT 'Monthly'")
                db.execSQL("ALTER TABLE categories ADD COLUMN subcategoriesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS goal_contributions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "goalId INTEGER NOT NULL, amount REAL NOT NULL, timestamp INTEGER NOT NULL, " +
                        "note TEXT NOT NULL DEFAULT '', stableUid TEXT NOT NULL DEFAULT '', " +
                        "amountPaise INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goal_contributions_stableUid ON goal_contributions(stableUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId_timestamp ON goal_contributions(goalId, timestamp)")
            }
        }

        internal val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

    }
}
