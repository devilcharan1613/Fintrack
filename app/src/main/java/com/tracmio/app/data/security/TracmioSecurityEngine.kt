package com.tracmio.app.data.security

import android.util.Base64
import com.tracmio.app.data.local.TracmioDatabase
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
    private const val ITERATIONS = 310_000
    private const val MIN_ACCEPTED_ITERATIONS = 100_000
    private const val MAX_ACCEPTED_ITERATIONS = 1_000_000
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

    /**
     * Encrypts a backup with AES-256-GCM and PBKDF2-HMAC-SHA256.
     * The caller transfers ownership of the password array; it is cleared before return.
     */
    fun encryptBackup(plaintextJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        var derivedKey: ByteArray? = null
        var plaintextBytes: ByteArray? = null
        val keySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(keySpec).encoded
            derivedKey = keyBytes
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            plaintextBytes = plaintextJson.toByteArray(StandardCharsets.UTF_8)
            val cipherBytes = cipher.doFinal(plaintextBytes)

            return JSONObject().apply {
                put("app", "Tracmio")
                put("format", BACKUP_ENCRYPTION_FORMAT)
                put("version", 1)
                put("kdf", PBKDF2_ALGORITHM)
                put("iterations", ITERATIONS)
                put("salt", encodeBase64(salt))
                put("iv", encodeBase64(iv))
                put("cipherText", encodeBase64(cipherBytes))
                put("createdAt", System.currentTimeMillis())
            }.toString()
        } finally {
            keySpec.clearPassword()
            password.fill('\u0000')
            derivedKey?.fill(0)
            plaintextBytes?.fill(0)
            salt.fill(0)
            iv.fill(0)
        }
    }

    /**
     * Decrypts current backups and legacy V1 envelopes. KDF work factors are bounded
     * before derivation to prevent malformed files from requesting excessive CPU time.
     * The caller transfers ownership of the password array; it is cleared before return.
     */
    fun decryptBackup(encryptedEnvelopeJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        var derivedKey: ByteArray? = null
        var decryptedBytes: ByteArray? = null
        var salt: ByteArray? = null
        var iv: ByteArray? = null
        var cipherBytes: ByteArray? = null
        try {
            val root = JSONObject(encryptedEnvelopeJson)
            val format = root.optString("format", "")
            require(format == BACKUP_ENCRYPTION_FORMAT) {
                "Unsupported backup format: $format. Expected $BACKUP_ENCRYPTION_FORMAT"
            }
            val version = root.optInt("version", 1)
            require(version == 1) { "Unsupported encrypted backup version: $version" }

            val iterations = root.optInt("iterations", 100_000)
            require(iterations in MIN_ACCEPTED_ITERATIONS..MAX_ACCEPTED_ITERATIONS) {
                "Backup encryption work factor is outside the supported safe range"
            }
            val decodedSalt = decodeBase64(root.getString("salt"))
            val decodedIv = decodeBase64(root.getString("iv"))
            val decodedCipherBytes = decodeBase64(root.getString("cipherText"))
            salt = decodedSalt
            iv = decodedIv
            cipherBytes = decodedCipherBytes
            require(decodedSalt.size == SALT_LENGTH_BYTES) { "Invalid backup salt length" }
            require(decodedIv.size == IV_LENGTH_BYTES) { "Invalid backup IV length" }
            require(decodedCipherBytes.size >= GCM_TAG_LENGTH_BITS / 8) { "Encrypted backup is truncated" }

            val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
            try {
                val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                    .generateSecret(keySpec).encoded
                derivedKey = keyBytes
            } finally {
                keySpec.clearPassword()
            }

            val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(derivedKey ?: throw IllegalStateException("Backup key derivation failed"), "AES"),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, decodedIv)
            )
            decryptedBytes = try {
                cipher.doFinal(decodedCipherBytes)
            } catch (e: Exception) {
                throw IllegalArgumentException("Decryption failed: invalid password or corrupted data", e)
            }
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } finally {
            password.fill('\u0000')
            derivedKey?.fill(0)
            decryptedBytes?.fill(0)
            salt?.fill(0)
            iv?.fill(0)
            cipherBytes?.fill(0)
        }
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
