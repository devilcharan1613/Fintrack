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
        LoanPaymentEntity::class,
        CustomTrackerEntity::class,
        TrackerTransactionCrossRefEntity::class,
        TrackerBudgetEntity::class,
        TrackerPersonEntity::class,
        TrackerSettlementEntity::class,
        TrackerAttachmentEntity::class,
        TrackerCustomFieldEntity::class,
        TrackerCustomValueEntity::class,
        TrackerRecurringRuleEntity::class,
        TrackerAlertSettingEntity::class,
        TrackerActivityLogEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class TracmioDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun financeDao(): FinanceDao
    abstract fun customTrackerDao(): CustomTrackerDao

    companion object {
        const val DATABASE_NAME = "tracmio_database"

        @Volatile
        private var INSTANCE: TracmioDatabase? = null

        fun getDatabase(context: Context): TracmioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TracmioDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                ).build()

                INSTANCE = instance
                instance
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_trackers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stableUid` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `trackerType` TEXT NOT NULL,
                        `customTypeName` TEXT NOT NULL DEFAULT '',
                        `description` TEXT NOT NULL DEFAULT '',
                        `icon` TEXT NOT NULL DEFAULT '🎯',
                        `themeColor` TEXT NOT NULL DEFAULT '#00f5d4',
                        `startDate` TEXT NOT NULL DEFAULT '',
                        `endDate` TEXT NOT NULL DEFAULT '',
                        `budgetAmount` REAL NOT NULL DEFAULT 0.0,
                        `budgetMode` TEXT NOT NULL DEFAULT 'NONE',
                        `currency` TEXT NOT NULL DEFAULT '₹',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_trackers_stableUid` ON `custom_trackers` (`stableUid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_trackers_status` ON `custom_trackers` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_trackers_isArchived` ON `custom_trackers` (`isArchived`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `transactionUid` TEXT NOT NULL,
                        `isRefund` INTEGER NOT NULL DEFAULT 0,
                        `paidByPersonUid` TEXT NOT NULL DEFAULT '',
                        `splitMode` TEXT NOT NULL DEFAULT 'EQUAL',
                        `splitSharesJson` TEXT NOT NULL DEFAULT '{}',
                        `linkedAt` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_transactions_trackerUid_transactionUid` ON `tracker_transactions` (`trackerUid`, `transactionUid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_transactions_transactionUid` ON `tracker_transactions` (`transactionUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `budgetMode` TEXT NOT NULL DEFAULT 'FIXED',
                        `plannedAmount` REAL NOT NULL DEFAULT 0.0,
                        `adjustmentAmount` REAL NOT NULL DEFAULT 0.0,
                        `startDate` TEXT NOT NULL DEFAULT '',
                        `endDate` TEXT NOT NULL DEFAULT '',
                        `categoryLimitsJson` TEXT NOT NULL DEFAULT '{}',
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_budgets_trackerUid` ON `tracker_budgets` (`trackerUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_people` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personUid` TEXT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `avatarColor` TEXT NOT NULL DEFAULT '#00f5d4',
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_people_trackerUid` ON `tracker_people` (`trackerUid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_people_personUid` ON `tracker_people` (`personUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_settlements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `settlementUid` TEXT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `fromPersonUid` TEXT NOT NULL,
                        `toPersonUid` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `isFullSettlement` INTEGER NOT NULL DEFAULT 0,
                        `date` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_settlements_trackerUid` ON `tracker_settlements` (`trackerUid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_settlements_settlementUid` ON `tracker_settlements` (`settlementUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_attachments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `attachmentUid` TEXT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `transactionUid` TEXT NOT NULL DEFAULT '',
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `contentUri` TEXT NOT NULL DEFAULT '',
                        `fileSizeBytes` INTEGER NOT NULL DEFAULT 0,
                        `addedAt` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_attachments_trackerUid` ON `tracker_attachments` (`trackerUid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_attachments_attachmentUid` ON `tracker_attachments` (`attachmentUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_custom_fields` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fieldUid` TEXT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `fieldName` TEXT NOT NULL,
                        `fieldType` TEXT NOT NULL,
                        `optionsJson` TEXT NOT NULL DEFAULT '[]',
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_custom_fields_trackerUid` ON `tracker_custom_fields` (`trackerUid`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_custom_fields_fieldUid` ON `tracker_custom_fields` (`fieldUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_custom_values` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `fieldUid` TEXT NOT NULL,
                        `fieldValue` TEXT NOT NULL,
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_custom_values_trackerUid_fieldUid` ON `tracker_custom_values` (`trackerUid`, `fieldUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_recurring_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL DEFAULT 'NONE',
                        `customIntervalDays` INTEGER NOT NULL DEFAULT 30,
                        `startDate` TEXT NOT NULL DEFAULT '',
                        `endDate` TEXT NOT NULL DEFAULT '',
                        `nextOccurrenceDate` TEXT NOT NULL DEFAULT '',
                        `reminderDaysBefore` INTEGER NOT NULL DEFAULT 1,
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_recurring_rules_trackerUid` ON `tracker_recurring_rules` (`trackerUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_alert_settings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `alert50` INTEGER NOT NULL DEFAULT 1,
                        `alert75` INTEGER NOT NULL DEFAULT 1,
                        `alert90` INTEGER NOT NULL DEFAULT 1,
                        `alert100` INTEGER NOT NULL DEFAULT 1,
                        `customPercent` INTEGER NOT NULL DEFAULT 0,
                        `firedThresholdsJson` TEXT NOT NULL DEFAULT '[]',
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracker_alert_settings_trackerUid` ON `tracker_alert_settings` (`trackerUid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tracker_activity_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `logUid` TEXT NOT NULL,
                        `trackerUid` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`trackerUid`) REFERENCES `custom_trackers`(`stableUid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_activity_logs_trackerUid_timestamp` ON `tracker_activity_logs` (`trackerUid`, `timestamp`)")
            }
        }
    }
}
