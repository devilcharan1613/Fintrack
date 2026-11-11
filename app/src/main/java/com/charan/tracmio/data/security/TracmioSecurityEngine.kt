package com.charan.tracmio.data.security

import android.util.Base64
import com.charan.tracmio.data.local.TracmioDatabase
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object TracmioSecurityEngine {

    const val BACKUP_ENCRYPTION_FORMAT = "TRACMIO_ENCRYPTED_BACKUP_V1"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATIONS = 100_000
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

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
    )

    fun encryptBackup(plaintextJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val keySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKey = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val plaintextBytes = plaintextJson.toByteArray(StandardCharsets.UTF_8)
        val cipherBytes = cipher.doFinal(plaintextBytes)

        val saltBase64 = encodeBase64(salt)
        val ivBase64 = encodeBase64(iv)
        val cipherBase64 = encodeBase64(cipherBytes)

        val envelope = JSONObject().apply {
            put("app", "Tracmio")
            put("format", BACKUP_ENCRYPTION_FORMAT)
            put("version", 1)
            put("kdf", PBKDF2_ALGORITHM)
            put("iterations", ITERATIONS)
            put("salt", saltBase64)
            put("iv", ivBase64)
            put("cipherText", cipherBase64)
            put("createdAt", System.currentTimeMillis())
        }
        return envelope.toString()
    }

    fun decryptBackup(encryptedEnvelopeJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val root = JSONObject(encryptedEnvelopeJson)
        val format = root.optString("format", "")
        if (format != BACKUP_ENCRYPTION_FORMAT) {
            throw IllegalArgumentException("Unsupported backup format: $format. Expected $BACKUP_ENCRYPTION_FORMAT")
        }

        val iterations = root.optInt("iterations", ITERATIONS)
        val salt = decodeBase64(root.getString("salt"))
        val iv = decodeBase64(root.getString("iv"))
        val cipherBytes = decodeBase64(root.getString("cipherText"))

        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val secretKey = SecretKeySpec(keyFactory.generateSecret(keySpec).encoded, "AES")

        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = try {
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            throw IllegalArgumentException("Decryption failed: invalid password or corrupted data", e)
        }
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (_: Throwable) {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }
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
