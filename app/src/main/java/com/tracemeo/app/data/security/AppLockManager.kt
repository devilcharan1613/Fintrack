package com.tracemeo.app.data.security

import android.content.Context
import com.tracemeo.app.data.AppLockState
import java.security.SecureRandom

class AppLockManager(context: Context) {
    private val prefs = context.getSharedPreferences("tracmio_applock_prefs", Context.MODE_PRIVATE)

    fun enableLock(
        pin: String,
        biometricEnabled: Boolean,
        securityQuestion: String,
        securityAnswer: String
    ): String {
        val cleanPin = pin.filter { it.isDigit() }
        val recoveryKey = generateRecoveryKey()
        prefs.edit()
            .putBoolean("enabled", true)
            .putString("pin_hash", TracmioSecurityEngine.hashPin(cleanPin))
            .putInt("pin_length", cleanPin.length.coerceIn(4, 6))
            .putBoolean("biometric", biometricEnabled)
            .putString("sec_question", securityQuestion.trim())
            .putString("sec_answer_hash", TracmioSecurityEngine.hashPin(securityAnswer.trim().lowercase()))
            .putString("recovery_key_hash", TracmioSecurityEngine.hashPin(recoveryKey.uppercase()))
            .putString("recovery_key_plain", recoveryKey)
            .apply()
        return recoveryKey
    }

    fun disableLock() {
        prefs.edit().putBoolean("enabled", false).apply()
    }

    fun getState(): AppLockState {
        return AppLockState(
            isEnabled = prefs.getBoolean("enabled", false),
            pin = "",
            pinLength = prefs.getInt("pin_length", 4),
            biometricEnabled = prefs.getBoolean("biometric", false),
            securityQuestion = prefs.getString("sec_question", "") ?: "",
            securityAnswerHash = prefs.getString("sec_answer_hash", "") ?: "",
            recoveryKey = prefs.getString("recovery_key_plain", "") ?: ""
        )
    }

    fun verifyPin(candidatePin: String): Boolean {
        val expected = prefs.getString("pin_hash", "") ?: ""
        return expected.isNotEmpty() && expected == TracmioSecurityEngine.hashPin(candidatePin.trim())
    }

    fun verifyRecoveryAndReset(secretOrRecoveryKey: String, newPin: String?): Boolean {
        val trimmed = secretOrRecoveryKey.trim()
        val answerHash = prefs.getString("sec_answer_hash", "") ?: ""
        val keyHash = prefs.getString("recovery_key_hash", "") ?: ""

        val matchesAnswer = answerHash.isNotEmpty() && answerHash == TracmioSecurityEngine.hashPin(trimmed.lowercase())
        val matchesKey = keyHash.isNotEmpty() && keyHash == TracmioSecurityEngine.hashPin(trimmed.uppercase())
        if (!matchesAnswer && !matchesKey) return false

        if (newPin.isNullOrBlank()) {
            disableLock()
        } else {
            val clean = newPin.filter { it.isDigit() }
            prefs.edit()
                .putBoolean("enabled", true)
                .putString("pin_hash", TracmioSecurityEngine.hashPin(clean))
                .putInt("pin_length", clean.length.coerceIn(4, 6))
                .apply()
        }
        return true
    }

    private fun generateRecoveryKey(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        fun block() = (1..4).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        return "TRAC-${block()}-${block()}"
    }
}
