package com.charan.tracmio.data

import com.charan.tracmio.data.security.TracmioSecurityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TracmioSecurityEngineTest {

    @Test
    fun encryptAndDecryptRoundtripSucceeds() {
        val originalPayload = """{"accounts":[{"id":"acc-1","name":"Checking","balance":50000.0}],"transactions":[{"id":"tx-1","amount":1200.0,"type":"expense"}]}"""
        val password = "SuperSecretMasterKey#2026".toCharArray()

        val envelope = TracmioSecurityEngine.encryptBackup(originalPayload, password)
        assertNotNull(envelope)
        assertTrue(envelope.contains("app"))
        assertTrue(envelope.contains("Tracmio"))
        assertTrue(envelope.contains("PBKDF2WithHmacSHA256"))
        assertTrue(envelope.contains("cipherText"))
        assertTrue(envelope.contains("salt"))
        assertTrue(envelope.contains("iv"))

        val decrypted = TracmioSecurityEngine.decryptBackup(envelope, password)
        assertEquals(originalPayload, decrypted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun decryptWithWrongPasswordFails() {
        val payload = """{"secret":"financial_data_here"}"""
        val correctPassword = "CorrectPassword123!".toCharArray()
        val wrongPassword = "WrongPassword999!".toCharArray()

        val envelope = TracmioSecurityEngine.encryptBackup(payload, correctPassword)
        TracmioSecurityEngine.decryptBackup(envelope, wrongPassword)
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
        assertNotNull("index.html must exist in assets", htmlFile)
        val html = htmlFile!!.readText()

        // 10 Intelligence & Security Suites
        assertTrue(html.contains("id=\"page-xray\""))
        assertTrue(html.contains("id=\"page-story\""))
        assertTrue(html.contains("id=\"page-globalsearch\""))
        assertTrue(html.contains("id=\"page-spendingdna\""))
        assertTrue(html.contains("id=\"page-calendar\""))
        assertTrue(html.contains("id=\"page-reminders\""))
        assertTrue(html.contains("id=\"page-timemachine\""))
        assertFalse(html.contains("id=\"page-dbhealth\""))
        assertTrue(html.contains("id=\"page-datacenter\""))
        assertTrue(html.contains("id=\"page-permissions\""))

        // Modals
        assertTrue(html.contains("id=\"overlay-event\""))
        assertTrue(html.contains("id=\"overlay-reminder\""))
        assertTrue(html.contains("id=\"overlay-encbackup\""))

        // Privacy & Topbar
        assertTrue(html.contains("id=\"privacyToggleBtn\""))
        assertTrue(html.contains("id=\"topbarSearchBtn\""))
        assertTrue(html.contains("toggleGlobalPrivacy"))
        assertTrue(html.contains("isPrivacyMasked"))

        // Offline verification
        assertFalse("Cannot have Cloud Sync", html.contains("Cloud Sync"))
        assertFalse("Cannot have Install Update APK", html.contains("Install Update from APK"))
    }
}
