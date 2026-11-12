package com.tracmio.app.data

import com.tracmio.app.data.security.TracmioSecurityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TracmioSecurityEngineTest {

    @Test
    fun encryptAndDecryptRoundtripSucceeds() {
        val sampleJson = """{"accounts":[{"id":1,"name":"HDFC","balance":50000.0}],"transactions":[{"id":10,"amount":1200.0,"type":"EXPENSE"}]}"""
        val password = "SuperStrongPassword!2026".toCharArray()

        val encryptedEnvelope = TracmioSecurityEngine.encryptBackup(sampleJson, password)
        assertTrue(encryptedEnvelope.contains("PBKDF2WithHmacSHA256"))
        assertTrue(encryptedEnvelope.contains("cipherText"))
        assertFalse(encryptedEnvelope.contains("HDFC"))

        val decrypted = TracmioSecurityEngine.decryptBackup(encryptedEnvelope, password)
        assertEquals(sampleJson, decrypted)
    }

    @Test
    fun decryptWithWrongPasswordFails() {
        val sampleJson = """{"secret":"financial_ledger_123"}"""
        val encrypted = TracmioSecurityEngine.encryptBackup(sampleJson, "correct-horse-battery".toCharArray())

        val result = runCatching {
            TracmioSecurityEngine.decryptBackup(encrypted, "wrong-password".toCharArray())
        }
        assertTrue("Decryption with wrong password must fail with AEADBadTagException", result.isFailure)
    }

    @Test
    fun databaseHealthReportDataStructureIsValid() {
        val report = TracmioSecurityEngine.DatabaseHealthReport(
            isHealthy = true,
            pragmaIntegrity = "ok",
            foreignKeyErrors = 0,
            accountCount = 5,
            transactionCount = 42,
            trackerCount = 3,
            orphanedTransactions = 0,
            diagnosticSummary = "Database 100% healthy"
        )
        assertTrue(report.isHealthy)
        assertEquals("ok", report.pragmaIntegrity)
        assertEquals(0, report.foreignKeyErrors)
        assertEquals(0, report.orphanedTransactions)
        assertEquals(42, report.transactionCount)
    }

    @Test
    fun suiteUiComponentsAndOfflineGuaranteesArePresent() {
        val htmlCandidates = listOf(
            File("src/main/assets/index.html"),
            File("app/src/main/assets/index.html")
        )
        val htmlFile = htmlCandidates.firstOrNull { it.exists() }
        assertTrue("index.html must exist", htmlFile != null)
        val html = htmlFile!!.readText()

        assertTrue(html.contains("id=\"page-xray\""))
        assertTrue(html.contains("id=\"page-story\""))
        assertTrue(html.contains("id=\"page-subscriptions\""))
        assertTrue(html.contains("id=\"page-spendingdna\""))
        assertTrue(html.contains("id=\"page-permissions\""))
        assertTrue(html.contains("id=\"page-globalsearch\""))
        assertTrue(html.contains("id=\"page-calendar\""))
        assertTrue(html.contains("id=\"page-reminders\""))
        assertTrue(html.contains("id=\"page-timemachine\""))
        assertTrue(html.contains("id=\"page-datacenter\""))
        assertTrue(html.contains("bridge.encryptBackup"))
        assertTrue(html.contains("bridge.decryptBackup"))
        assertTrue(html.contains("bridge.checkDatabaseHealth"))

        val manifestCandidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertTrue("AndroidManifest.xml must exist", manifestFile != null)
        val manifest = manifestFile!!.readText()
        assertFalse("App must have zero INTERNET permissions", manifest.contains("android.permission.INTERNET"))
        assertTrue("EventReminderReceiver must be registered", manifest.contains(".EventReminderReceiver"))
    }
}
