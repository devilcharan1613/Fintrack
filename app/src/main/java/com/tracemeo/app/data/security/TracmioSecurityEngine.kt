package com.tracemeo.app.data.security

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object TracmioSecurityEngine {
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGO = "AES/GCM/NoPadding"
    private const val MIN_ITERATIONS = 100_000
    private const val DEFAULT_ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128

    data class DatabaseHealthReport(
        val isHealthy: Boolean,
        val pragmaIntegrity: String,
        val foreignKeyErrors: Int,
        val accountCount: Int,
        val transactionCount: Int,
        val trackerCount: Int,
        val orphanedTransactions: Int,
        val diagnosticSummary: String
    )

    fun encrypt(data: String): String = data
    fun decrypt(data: String): String = data

    fun hashPin(pin: String, salt: String = "tracmio_pin_salt"): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun encryptBackup(plainJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }

        val spec = PBEKeySpec(password, salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).encoded
        val keySpec = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        val envelope = JSONObject()
        envelope.put("kdf", PBKDF2_ALGO)
        envelope.put("cipher", CIPHER_ALGO)
        envelope.put("iterations", DEFAULT_ITERATIONS)
        envelope.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
        envelope.put("cipherText", Base64.encodeToString(cipherBytes, Base64.NO_WRAP))
        return envelope.toString()
    }

    fun decryptBackup(encryptedEnvelopeJson: String, password: CharArray): String {
        val obj = JSONObject(encryptedEnvelopeJson)
        val cipherName = obj.optString("cipher", "")
        val iterations = obj.optInt("iterations", 0)
        require(cipherName == CIPHER_ALGO) { "Unsupported cipher: $cipherName" }
        require(iterations >= MIN_ITERATIONS) { "Iteration count too weak: $iterations" }

        val salt = Base64.decode(obj.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(obj.getString("iv"), Base64.NO_WRAP)
        val cipherBytes = Base64.decode(obj.getString("cipherText"), Base64.NO_WRAP)

        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val secretKeyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).encoded
        val keySpec = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun sanitizeInput(raw: String, maxLength: Int = 500): String {
        val noScripts = raw
            .replace(Regex("(?i)<script[^>]*>.*?</script>"), "")
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
            .trim()
        return if (noScripts.length > maxLength) noScripts.take(maxLength) else noScripts
    }
}
