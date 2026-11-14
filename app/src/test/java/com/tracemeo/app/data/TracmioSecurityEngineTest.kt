package com.tracemeo.app.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TracmioSecurityEngineTest {
    @Test
    fun backupEncryptionAndDecryptionRoundtripWorks() {
        val originalJson = """{"app":"Tracmio","versionName":"16.13","balance":25000}"""
        val password = "StrongVaultPassword!1613".toCharArray()
        val encrypted = TracmioSecurityEngine.encryptBackup(originalJson, password)

        val envelope = org.json.JSONObject(encrypted)
        assertEquals("AES/GCM/NoPadding", envelope.getString("cipher"))
        assertEquals("PBKDF2WithHmacSHA256", envelope.getString("kdf"))
        assertTrue(envelope.getInt("iterations") >= 100_000)
        assertNotEquals(originalJson, encrypted)

        val decrypted = TracmioSecurityEngine.decryptBackup(encrypted, password)
        assertEquals(originalJson, decrypted)
    }

    @Test
    fun pinHashingProducesDeterministicSha256Hex() {
        val hash1 = TracmioSecurityEngine.hashPin("1234")
        val hash2 = TracmioSecurityEngine.hashPin("1234")
        val hashOther = TracmioSecurityEngine.hashPin("5678")

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashOther)
        assertNotEquals("1234", hash1)
    }

    @Test
    fun sanitizeInputStripsScriptsAndControlChars() {
        val dirty = "  <script>alert('xss')</script>Coffee \u0007 #chai  "
        val clean = TracmioSecurityEngine.sanitizeInput(dirty, 50)
        assertEquals("Coffee  #chai", clean)
    }
}
