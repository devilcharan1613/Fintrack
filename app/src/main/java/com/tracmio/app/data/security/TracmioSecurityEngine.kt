package com.tracmio.app.data.security

import com.tracmio.app.data.local.TracmioDatabase
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object TracmioSecurityEngine {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    data class DatabaseHealthReport(
        val isHealthy: Boolean,
        val pragmaIntegrity: String,
        val foreignKeyErrors: Int,
        val accountCount: Int,
        val transactionCount: Int,
        val trackerCount: Int,
        val orphanedTransactions: Int,
        val timestamp: Long = System.currentTimeMillis(),
        val diagnosticSummary: String
    ) {
        fun toJsonString(): String = JSONObject().apply {
            put("isHealthy", isHealthy)
            put("pragmaIntegrity", pragmaIntegrity)
            put("foreignKeyErrors", foreignKeyErrors)
            put("accountCount", accountCount)
            put("transactionCount", transactionCount)
            put("trackerCount", trackerCount)
            put("orphanedTransactions", orphanedTransactions)
            put("timestamp", timestamp)
            put("diagnosticSummary", diagnosticSummary)
        }.toString()
    }

    fun encryptBackup(plainJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Encryption password must not be empty" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))

        val encoder = Base64.getEncoder()
        return JSONObject().apply {
            put("app", "Tracmio")
            put("version", 1)
            put("kdf", PBKDF2_ALGORITHM)
            put("iterations", PBKDF2_ITERATIONS)
            put("cipher", "AES/GCM/NoPadding")
            put("salt", encoder.encodeToString(salt))
            put("iv", encoder.encodeToString(iv))
            put("cipherText", encoder.encodeToString(cipherBytes))
        }.toString()
    }

    fun decryptBackup(envelopeJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Decryption password must not be empty" }
        val obj = JSONObject(envelopeJson)
        val decoder = Base64.getDecoder()
        val salt = decoder.decode(obj.getString("salt"))
        val iv = decoder.decode(obj.getString("iv"))
        val cipherBytes = decoder.decode(obj.getString("cipherText"))
        val iterations = obj.optInt("iterations", PBKDF2_ITERATIONS)

        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val secretKeyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    fun checkDatabaseIntegrity(database: TracmioDatabase): DatabaseHealthReport {
        var pragmaResult = "unknown"
        var fkErrors = 0
        var accounts = 0
        var txns = 0
        var trackers = 0
        var orphans = 0

        database.openHelper.readableDatabase.let { db ->
            db.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    pragmaResult = cursor.getString(0) ?: "ok"
                }
            }
            db.query("PRAGMA foreign_key_check").use { cursor ->
                fkErrors = cursor.count
            }
            db.query("SELECT COUNT(*) FROM accounts").use { cursor ->
                if (cursor.moveToFirst()) accounts = cursor.getInt(0)
            }
            db.query("SELECT COUNT(*) FROM transactions").use { cursor ->
                if (cursor.moveToFirst()) txns = cursor.getInt(0)
            }
            runCatching {
                db.query("SELECT COUNT(*) FROM custom_trackers").use { cursor ->
                    if (cursor.moveToFirst()) trackers = cursor.getInt(0)
                }
            }
            db.query("SELECT COUNT(*) FROM transactions WHERE accountId NOT IN (SELECT id FROM accounts)").use { cursor ->
                if (cursor.moveToFirst()) orphans = cursor.getInt(0)
            }
        }

        val isHealthy = pragmaResult.equals("ok", ignoreCase = true) && fkErrors == 0 && orphans == 0
        val summary = buildString {
            append("SQLite PRAGMA: $pragmaResult. ")
            append("Accounts: $accounts, Transactions: $txns, Trackers: $trackers. ")
            if (fkErrors > 0) append("FK violations: $fkErrors. ")
            if (orphans > 0) append("Orphaned txns: $orphans. ")
            if (isHealthy) append("Database integrity 100% verified.")
            else append("Issues detected in database relations.")
        }

        return DatabaseHealthReport(
            isHealthy = isHealthy,
            pragmaIntegrity = pragmaResult,
            foreignKeyErrors = fkErrors,
            accountCount = accounts,
            transactionCount = txns,
            trackerCount = trackers,
            orphanedTransactions = orphans,
            diagnosticSummary = summary
        )
    }
}
