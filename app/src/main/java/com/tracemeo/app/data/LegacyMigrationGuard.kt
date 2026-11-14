package com.tracemeo.app.data

import android.content.Context
import com.tracemeo.app.data.local.AccountEntity
import com.tracemeo.app.data.local.TransactionEntity
import com.tracemeo.app.data.repository.FinanceRepository
import org.json.JSONObject
import java.io.File

object LegacyMigrationGuard {
    private const val MIGRATION_PREFS = "tracmio_migration_guard"
    private const val KEY_MIGRATED_V16_13 = "migrated_v16_13"

    suspend fun runOneTimeLegacyMigrationIfNeeded(
        context: Context,
        repository: FinanceRepository
    ): Boolean {
        val prefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED_V16_13, false)) return false

        val legacyDbFile = context.getDatabasePath("fintrack_database")
        if (legacyDbFile.exists()) {
            // Preserve legacy database file and mark safe migration complete
            val backupCopy = File(context.filesDir, "fintrack_database_legacy.bak")
            runCatching { legacyDbFile.copyTo(backupCopy, overwrite = true) }
        }

        prefs.edit().putBoolean(KEY_MIGRATED_V16_13, true).apply()
        return true
    }

    fun parseLegacyJsonSnapshot(json: String): Pair<List<AccountEntity>, List<TransactionEntity>> {
        return runCatching {
            val obj = JSONObject(json)
            val accounts = mutableListOf<AccountEntity>()
            val transactions = mutableListOf<TransactionEntity>()
            val accArr = obj.optJSONArray("accounts")
            if (accArr != null) {
                for (i in 0 until accArr.length()) {
                    val a = accArr.getJSONObject(i)
                    val bal = a.optDouble("balance", 0.0)
                    accounts.add(
                        AccountEntity(
                            id = a.optLong("id", (i + 1).toLong()),
                            name = a.optString("name", "Account ${i + 1}"),
                            accountType = a.optString("type", "Bank"),
                            balance = bal,
                            balancePaise = MoneyPaise.toPaise(bal)
                        )
                    )
                }
            }
            val txArr = obj.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val t = txArr.getJSONObject(i)
                    val amt = t.optDouble("amount", 0.0)
                    transactions.add(
                        TransactionEntity(
                            accountId = t.optLong("accountId", 1L),
                            type = t.optString("type", "EXPENSE"),
                            amount = amt,
                            amountPaise = MoneyPaise.toPaise(amt),
                            category = t.optString("category", "Other"),
                            note = t.optString("note", "")
                        )
                    )
                }
            }
            accounts to transactions
        }.getOrDefault(emptyList<AccountEntity>() to emptyList())
    }
}
