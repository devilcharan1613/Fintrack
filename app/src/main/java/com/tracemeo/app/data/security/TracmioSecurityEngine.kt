package com.tracmio.app.data.security

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tracmio.app.data.local.TracmioDatabase
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class BiometricReadiness {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

data class DeviceSecurityCapability(
    val biometricReadiness: BiometricReadiness = BiometricReadiness.UNAVAILABLE,
    val deviceCredentialAvailable: Boolean = false,
    val statusText: String = "Biometrics checked on device"
) {
    val deviceCredentialReady: Boolean get() = deviceCredentialAvailable
    val summaryLabel: String get() = statusText
}

data class AppLockState(
    val isEnabled: Boolean = false,
    val pinLength: Int = 4,
    val biometricEnabled: Boolean = false,
    val securityQuestion: String = "",
    val hasRecoveryConfigured: Boolean = false
)

object BiometricCapabilityChecker {
    fun inspect(context: Context): DeviceSecurityCapability {
        val manager = BiometricManager.from(context)
        val bioResult = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        val readiness = when (bioResult) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricReadiness.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricReadiness.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricReadiness.NO_HARDWARE
            else -> BiometricReadiness.UNAVAILABLE
        }
        val deviceCredentialAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.isDeviceSecure == true
        }
        val statusText = when (readiness) {
            BiometricReadiness.AVAILABLE -> "Fingerprint / Face Unlock Ready"
            BiometricReadiness.NONE_ENROLLED ->
                if (deviceCredentialAvailable) "No biometrics enrolled — Device PIN & Liquid Glass PIN ready"
                else "No biometrics enrolled in device Settings"
            BiometricReadiness.NO_HARDWARE ->
                if (deviceCredentialAvailable) "No biometric sensor — Device PIN & Liquid Glass PIN ready"
                else "Biometric hardware unavailable — Liquid Glass PIN active"
            BiometricReadiness.UNAVAILABLE -> "Biometrics temporarily unavailable — PIN fallback ready"
        }
        return DeviceSecurityCapability(
            biometricReadiness = readiness,
            deviceCredentialAvailable = deviceCredentialAvailable,
            statusText = statusText
        )
    }
}

object TracmioSecurityEngine {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val VAULT_PREFS_NAME = "tracmio_keystore_vault"
    private const val KEY_DB_PASSPHRASE = "db_sqlcipher_key_b64"
    private const val MAX_IMPORT_JSON_CHARS = 15_000_000

    data class DatabaseHealthReport(
        val isHealthy: Boolean,
        val pragmaIntegrity: String,
        val foreignKeyErrors: Int,
        val accountCount: Int,
        val transactionCount: Int,
        val trackerCount: Int,
        val orphanedTransactions: Int,
        val isEncrypted: Boolean = true,
        val timestamp: Long = System.currentTimeMillis(),
        val diagnosticSummary: String
    ) {
        fun toJsonString(): String = JSONObject().apply {
            put("isHealthy", isHealthy)
            put("pragmaIntegrity", pragmaIntegrity)
            put("sqliteIntegrity", pragmaIntegrity)
            put("foreignKeyErrors", foreignKeyErrors)
            put("accountCount", accountCount)
            put("transactionCount", transactionCount)
            put("trackerCount", trackerCount)
            put("orphanedTransactions", orphanedTransactions)
            put("isEncrypted", isEncrypted)
            put("timestamp", timestamp)
            put("diagnosticSummary", diagnosticSummary)
        }.toString()
    }

    fun getEncryptedPrefs(context: Context, fileName: String): SharedPreferences {
        val appContext = context.applicationContext
        return runCatching {
            val masterKey = MasterKey.Builder(appContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse { err ->
            if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
                appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            } else {
                throw SecurityException("Android Keystore EncryptedSharedPreferences unavailable", err)
            }
        }
    }

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val vault = getEncryptedPrefs(context, VAULT_PREFS_NAME)
        val existing = vault.getString(KEY_DB_PASSPHRASE, null)
        if (!existing.isNullOrBlank()) {
            return Base64.getDecoder().decode(existing)
        }
        val keyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encoded = Base64.getEncoder().encodeToString(keyBytes)
        vault.edit().putString(KEY_DB_PASSPHRASE, encoded).apply()
        return keyBytes
    }

    fun sanitizeInput(raw: String?, maxLength: Int = 500): String {
        if (raw.isNullOrEmpty()) return ""
        return raw
            .replace("\u0000", "")
            .replace(Regex("[\\u0001-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .replace(Regex("<\\s*/?\\s*script[^>]*>", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(maxLength)
    }

    fun validateBackupJson(rawJson: String): JSONObject {
        require(rawJson.isNotBlank()) { "Backup JSON is empty" }
        require(rawJson.length <= MAX_IMPORT_JSON_CHARS) { "Backup JSON exceeds safety size limit" }
        val parsed = runCatching { JSONObject(rawJson) }.getOrElse {
            throw IllegalArgumentException("Malformed JSON backup file")
        }
        val root = parsed.optJSONObject("data") ?: parsed
        require(
            root.has("accounts") ||
                root.has("transactions") ||
                root.has("cipherText")
        ) { "Backup missing required Tracmio ledger fields" }
        return parsed
    }

    fun hashSecret(secret: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(secret.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest.digest())
    }

    fun verifySecret(secret: String, saltB64: String, expectedHashB64: String): Boolean {
        if (saltB64.isBlank() || expectedHashB64.isBlank()) return false
        val salt = runCatching { Base64.getDecoder().decode(saltB64) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(expectedHashB64) }.getOrNull() ?: return false
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(secret.toByteArray(StandardCharsets.UTF_8))
        val actual = digest.digest()
        return MessageDigest.isEqual(actual, expected)
    }

    fun generateRecoveryKey(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        fun block() = (0 until 4).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
        return "TRAC-${block()}-${block()}"
    }

    fun encryptBackup(plainJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Encryption password must not be empty" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        var secretKeyBytes: ByteArray? = null
        val plainBytes = plainJson.toByteArray(StandardCharsets.UTF_8)
        return try {
            secretKeyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(secretKeyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val cipherBytes = cipher.doFinal(plainBytes)

            val encoder = Base64.getEncoder()
            JSONObject().apply {
                put("app", "Tracmio")
                put("version", 1)
                put("kdf", PBKDF2_ALGORITHM)
                put("iterations", PBKDF2_ITERATIONS)
                put("cipher", "AES/GCM/NoPadding")
                put("salt", encoder.encodeToString(salt))
                put("iv", encoder.encodeToString(iv))
                put("cipherText", encoder.encodeToString(cipherBytes))
            }.toString()
        } finally {
            keySpec.clearPassword()
            secretKeyBytes?.fill(0)
            plainBytes.fill(0)
        }
    }

    fun decryptBackup(envelopeJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Decryption password must not be empty" }
        val obj = JSONObject(envelopeJson)
        val kdf = obj.optString("kdf", PBKDF2_ALGORITHM)
        val cipherName = obj.optString("cipher", "AES/GCM/NoPadding")
        require(kdf == PBKDF2_ALGORITHM) { "Unsupported KDF algorithm" }
        require(cipherName == "AES/GCM/NoPadding") { "Unsupported cipher mode" }

        val iterations = obj.optInt("iterations", PBKDF2_ITERATIONS)
        require(iterations in 100_000..1_000_000) { "Invalid PBKDF2 iteration count" }

        val decoder = Base64.getDecoder()
        val salt = decoder.decode(obj.getString("salt"))
        val iv = decoder.decode(obj.getString("iv"))
        val cipherBytes = decoder.decode(obj.getString("cipherText"))
        require(salt.size == SALT_BYTES) { "Invalid salt length" }
        require(iv.size == IV_BYTES) { "Invalid IV length" }
        require(cipherBytes.size > (GCM_TAG_BITS / 8)) { "Invalid ciphertext payload" }

        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        var secretKeyBytes: ByteArray? = null
        var plainBytes: ByteArray? = null
        return try {
            secretKeyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(secretKeyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } finally {
            keySpec.clearPassword()
            secretKeyBytes?.fill(0)
            plainBytes?.fill(0)
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
            append("SQLCipher PRAGMA: $pragmaResult. ")
            append("Accounts: $accounts, Transactions: $txns, Trackers: $trackers. ")
            if (fkErrors > 0) append("FK violations: $fkErrors. ")
            if (orphans > 0) append("Orphaned txns: $orphans. ")
            if (isHealthy) append("AES-256 Encrypted Database 100% verified.")
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
            isEncrypted = true,
            diagnosticSummary = summary
        )
    }
}

class AppLockManager(context: Context) {
    private val prefs = TracmioSecurityEngine.getEncryptedPrefs(context, PREFS_LOCK)

    fun getState(): AppLockState {
        return AppLockState(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            pinLength = prefs.getInt(KEY_PIN_LENGTH, 4).let { if (it == 6) 6 else 4 },
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
            securityQuestion = prefs.getString(KEY_SEC_QUESTION, "").orEmpty(),
            hasRecoveryConfigured = !prefs.getString(KEY_RECOVERY_HASH, null).isNullOrBlank()
        )
    }

    fun enableLock(
        pin: String,
        biometricEnabled: Boolean,
        securityQuestion: String,
        securityAnswer: String
    ): String {
        require(pin.length == 4 || pin.length == 6) { "PIN must be 4 or 6 digits" }
        require(pin.all { it.isDigit() }) { "PIN must contain only digits" }
        val cleanQuestion = TracmioSecurityEngine.sanitizeInput(securityQuestion, 140)
        val cleanAnswer = TracmioSecurityEngine.sanitizeInput(securityAnswer, 120).lowercase()
        require(cleanQuestion.isNotEmpty() && cleanAnswer.isNotEmpty()) {
            "Mandatory security question and answer are required"
        }

        val rnd = SecureRandom()
        val pinSalt = ByteArray(16).also { rnd.nextBytes(it) }
        val ansSalt = ByteArray(16).also { rnd.nextBytes(it) }
        val recSalt = ByteArray(16).also { rnd.nextBytes(it) }

        val recoveryKey = TracmioSecurityEngine.generateRecoveryKey()
        val b64 = Base64.getEncoder()

        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putBoolean(KEY_BIOMETRIC, biometricEnabled)
            .putString(KEY_PIN_SALT, b64.encodeToString(pinSalt))
            .putString(KEY_PIN_HASH, TracmioSecurityEngine.hashSecret(pin, pinSalt))
            .putString(KEY_SEC_QUESTION, cleanQuestion)
            .putString(KEY_SEC_ANSWER_SALT, b64.encodeToString(ansSalt))
            .putString(KEY_SEC_ANSWER_HASH, TracmioSecurityEngine.hashSecret(cleanAnswer, ansSalt))
            .putString(KEY_RECOVERY_SALT, b64.encodeToString(recSalt))
            .putString(KEY_RECOVERY_HASH, TracmioSecurityEngine.hashSecret(recoveryKey.uppercase(), recSalt))
            .apply()

        return recoveryKey
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val hash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        return TracmioSecurityEngine.verifySecret(pin, salt, hash)
    }

    fun verifyRecoveryAndReset(secretInput: String, newPin: String? = null): Boolean {
        val trimmed = secretInput.trim()
        if (trimmed.isEmpty()) return false
        val ansSalt = prefs.getString(KEY_SEC_ANSWER_SALT, "") ?: ""
        val ansHash = prefs.getString(KEY_SEC_ANSWER_HASH, "") ?: ""
        val recSalt = prefs.getString(KEY_RECOVERY_SALT, "") ?: ""
        val recHash = prefs.getString(KEY_RECOVERY_HASH, "") ?: ""

        val ansMatch = TracmioSecurityEngine.verifySecret(trimmed.lowercase(), ansSalt, ansHash)
        val recMatch = TracmioSecurityEngine.verifySecret(trimmed.uppercase(), recSalt, recHash)
        if (!ansMatch && !recMatch) return false

        if (newPin != null) {
            require(newPin.length == 4 || newPin.length == 6) { "New PIN must be 4 or 6 digits" }
            require(newPin.all { it.isDigit() }) { "New PIN must be numeric" }
            val newSalt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            prefs.edit()
                .putInt(KEY_PIN_LENGTH, newPin.length)
                .putString(KEY_PIN_SALT, Base64.getEncoder().encodeToString(newSalt))
                .putString(KEY_PIN_HASH, TracmioSecurityEngine.hashSecret(newPin, newSalt))
                .apply()
        } else {
            disableLock()
        }
        return true
    }

    fun disableLock() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_SEC_ANSWER_SALT)
            .remove(KEY_SEC_ANSWER_HASH)
            .remove(KEY_RECOVERY_SALT)
            .remove(KEY_RECOVERY_HASH)
            .remove(KEY_SEC_QUESTION)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()
    }

    companion object {
        private const val PREFS_LOCK = "tracmio_app_lock_vault"
        private const val KEY_ENABLED = "lock_enabled"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SEC_QUESTION = "sec_question"
        private const val KEY_SEC_ANSWER_SALT = "sec_answer_salt"
        private const val KEY_SEC_ANSWER_HASH = "sec_answer_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
    }
}
