package com.tracemeo.app.data.local

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        PersonEntity::class,
        CategoryEntity::class,
        CustomTrackerEntity::class,
        CustomTrackerEntryEntity::class,
        TrackerFolderEntity::class,
        SubscriptionEntity::class,
        AcademicEventEntity::class,
        ReminderItemEntity::class,
        KhataEntryEntity::class,
        WishlistCooldownEntity::class,
        CashbackRewardEntity::class,
        PricePerUseEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class TracmioDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun financeDao(): FinanceDao
    abstract fun customTrackerDao(): CustomTrackerDao
    abstract fun featureDao(): FeatureDao

    companion object {
        @Volatile
        private var INSTANCE: TracmioDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureCoreSchemaTables(db)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureCoreSchemaTables(db)
            }
        }

        private fun ensureCoreSchemaTables(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS khata_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    shopOrFriend TEXT NOT NULL,
                    itemDescription TEXT NOT NULL,
                    amount REAL NOT NULL,
                    isSettled INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS wishlist_cooldowns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    itemName TEXT NOT NULL,
                    price REAL NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    status TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cashback_rewards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sourceApp TEXT NOT NULL,
                    amount REAL NOT NULL,
                    note TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS price_per_use_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    itemName TEXT NOT NULL,
                    purchasePrice REAL NOT NULL,
                    usageCount INTEGER NOT NULL,
                    targetUses INTEGER NOT NULL,
                    createdAtMillis INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_trackers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    trackerType TEXT NOT NULL,
                    targetValue REAL NOT NULL,
                    currentValue REAL NOT NULL,
                    unit TEXT NOT NULL,
                    icon TEXT NOT NULL,
                    colorHex TEXT NOT NULL,
                    folderId INTEGER NOT NULL,
                    isPinned INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tracker_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    trackerId INTEGER NOT NULL,
                    deltaValue REAL NOT NULL,
                    note TEXT NOT NULL,
                    attachmentPath TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tracker_folders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    folderName TEXT NOT NULL,
                    icon TEXT NOT NULL,
                    colorHex TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
            """.trimIndent())
        }

        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val securePrefs = runCatching {
                val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                    .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                    .build()
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    context,
                    "tracmio_db_key_secure",
                    masterKey,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.getOrElse {
                context.getSharedPreferences("tracmio_db_key_store", Context.MODE_PRIVATE)
            }

            val existingSecure = securePrefs.getString("db_passphrase_b64", null)
            if (!existingSecure.isNullOrBlank()) {
                return Base64.decode(existingSecure, Base64.NO_WRAP)
            }

            // One-time non-destructive migration from legacy unencrypted prefs so existing vaults remain readable
            val legacyPrefs = context.getSharedPreferences("tracmio_db_key_store", Context.MODE_PRIVATE)
            val legacyPass = legacyPrefs.getString("db_passphrase_b64", null)
            if (!legacyPass.isNullOrBlank()) {
                securePrefs.edit().putString("db_passphrase_b64", legacyPass).apply()
                if (securePrefs !== legacyPrefs) {
                    legacyPrefs.edit().remove("db_passphrase_b64").apply()
                }
                return Base64.decode(legacyPass, Base64.NO_WRAP)
            }

            val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
            securePrefs.edit().putString("db_passphrase_b64", Base64.encodeToString(raw, Base64.NO_WRAP)).apply()
            return raw
        }

        fun getInstance(context: Context): TracmioDatabase {
            return INSTANCE ?: synchronized(this) {
                val appCtx = context.applicationContext
                val builder = Room.databaseBuilder(
                    appCtx,
                    TracmioDatabase::class.java,
                    "tracmio_database"
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10)

                val sqlCipherLoaded = runCatching {
                    System.loadLibrary("sqlcipher")
                    true
                }.getOrDefault(false)

                if (sqlCipherLoaded) {
                    val factory = SupportOpenHelperFactory(getOrCreatePassphrase(appCtx))
                    builder.openHelperFactory(factory)
                }

                builder.build().also { INSTANCE = it }
            }
        }
    }
}
