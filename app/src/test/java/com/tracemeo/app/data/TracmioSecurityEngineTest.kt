package com.tracmio.app.data

import com.tracmio.app.data.security.AppLockManager
import com.tracmio.app.data.security.TracmioSecurityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
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
    fun decryptRejectsWeakIterationCountOrUnsupportedCipher() {
        val encrypted = TracmioSecurityEngine.encryptBackup("""{"accounts":[]}""", "StrongPassword#1".toCharArray())
        val weakIterations = org.json.JSONObject(encrypted).put("iterations", 1000).toString()
        assertTrue(runCatching { TracmioSecurityEngine.decryptBackup(weakIterations, "StrongPassword#1".toCharArray()) }.isFailure)
        val badCipher = org.json.JSONObject(encrypted).put("cipher", "AES/ECB/PKCS5Padding").toString()
        assertTrue(runCatching { TracmioSecurityEngine.decryptBackup(badCipher, "StrongPassword#1".toCharArray()) }.isFailure)
    }

    @Test
    fun appLockPinAndMandatoryRecoveryRoundtripSucceeds() {
        val ctx = RuntimeEnvironment.getApplication()
        val mgr = AppLockManager(ctx)
        val key = mgr.enableLock(
            pin = "2468",
            biometricEnabled = true,
            securityQuestion = "City of birth?",
            securityAnswer = "Bengaluru"
        )
        assertTrue(key.startsWith("TRAC-"))
        assertTrue(mgr.getState().isEnabled)
        assertTrue(mgr.verifyPin("2468"))
        assertFalse(mgr.verifyPin("9999"))

        // Reset PIN with security answer
        assertTrue(mgr.verifyRecoveryAndReset("bengaluru", "135790"))
        assertEquals(6, mgr.getState().pinLength)
        assertTrue(mgr.verifyPin("135790"))

        // Reset with recovery key
        assertTrue(mgr.verifyRecoveryAndReset(key, null))
        assertFalse(mgr.getState().isEnabled)
    }

    @Test
    fun sanitizeInputStripsControlCharsAndScripts() {
        val cleaned = TracmioSecurityEngine.sanitizeInput("  <script>alert(1)</script>Coffee \u0000#trip  ", 100)
        assertFalse(cleaned.contains("<script>"))
        assertFalse(cleaned.contains("\u0000"))
        assertTrue(cleaned.contains("Coffee #trip"))
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
    fun pureNativeComposeSecurityManifestAndZeroUiMemeAuditPasses() {
        val manifestCandidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertTrue("AndroidManifest.xml must exist", manifestFile != null)
        val manifest = manifestFile!!.readText()
        assertFalse("Zero INTERNET permission required", manifest.contains("android.permission.INTERNET"))
        assertFalse("Zero ACCESS_NETWORK_STATE permission required", manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse("Zero READ_MEDIA_IMAGES permission allowed", manifest.contains("READ_MEDIA_IMAGES"))
        assertFalse("Zero READ_MEDIA_VISUAL_USER_SELECTED permission allowed", manifest.contains("READ_MEDIA_VISUAL_USER_SELECTED"))
        assertFalse("Zero READ_EXTERNAL_STORAGE permission allowed", manifest.contains("READ_EXTERNAL_STORAGE"))
        assertTrue("EventReminderReceiver must be registered", manifest.contains(".EventReminderReceiver"))
        assertTrue("DailyReminderReceiver must be registered", manifest.contains(".DailyReminderReceiver"))

        val mainActivityCandidates = listOf(
            File("src/main/java/com/tracemeo/app/MainActivity.kt"),
            File("app/src/main/java/com/tracemeo/app/MainActivity.kt")
        )
        val mainActivityFile = mainActivityCandidates.firstOrNull { it.exists() }
        assertTrue("MainActivity.kt must exist", mainActivityFile != null)
        val mainActivity = mainActivityFile!!.readText()
        assertTrue("MainActivity must extend FragmentActivity", mainActivity.contains("FragmentActivity"))
        assertFalse("WebView must be completely removed", mainActivity.contains("android.webkit.WebView"))
        assertFalse("Insecure file:// assets must not exist", mainActivity.contains("file:///android_asset"))

        val gradleFile = listOf(File("build.gradle.kts"), File("app/build.gradle.kts")).firstOrNull { it.exists() }
        assertTrue("app/build.gradle.kts must exist", gradleFile != null)
        val gradleSrc = gradleFile!!.readText()
        assertTrue("applicationId must be com.tracmio.app", gradleSrc.contains("""applicationId = "com.tracmio.app""""))
        assertTrue("versionName must be 16.13", gradleSrc.contains("""versionName = "16.13""""))

        val uiDir = listOf(
            File("src/main/java/com/tracemeo/app/ui/nativeapp"),
            File("app/src/main/java/com/tracemeo/app/ui/nativeapp")
        ).firstOrNull { it.exists() }
        assertTrue("Compose UI directory must exist", uiDir != null)
        val uiFiles = uiDir!!.listFiles()?.filter { it.extension == "kt" }.orEmpty()
        assertTrue(uiFiles.isNotEmpty())
        uiFiles.forEach { file ->
            val src = file.readText()
            assertFalse("UI must never display any meme toggle or option (${file.name})", src.contains("meme", ignoreCase = true))
        }
    }
}
