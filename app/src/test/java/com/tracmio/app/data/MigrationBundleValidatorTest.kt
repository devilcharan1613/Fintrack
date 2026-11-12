package com.tracmio.app.data

import com.tracmio.app.data.migration.MigrationBundleValidator
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class MigrationBundleValidatorTest {

    @Test
    fun validatesCompleteBundleAndAttachmentChecksums() {
        val fileBytes = "attachment-content".toByteArray()
        val payload = validPayload(fileBytes)
        val envelope = envelopeFor(payload)

        val result = MigrationBundleValidator.validate(envelope.toString())

        assertEquals(1, result.attachmentCount)
        assertEquals(0, result.attachmentWarningCount)
        assertTrue(result.attachmentExportComplete)
    }

    @Test
    fun reportsPartialAttachmentRecoveryWithoutRejectingValidBundle() {
        val payload = validPayload()
        val room = payload.getJSONObject("roomDatabase")
        room.put("attachmentWarnings", JSONArray().put(JSONObject().put("reason", "FILE_MISSING")))
        room.put("attachmentExportComplete", false)

        val result = MigrationBundleValidator.validate(envelopeFor(payload).toString())

        assertEquals(1, result.attachmentWarningCount)
        assertFalse(result.attachmentExportComplete)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPayloadChangedAfterChecksumWasCreated() {
        val payload = validPayload()
        val envelope = envelopeFor(payload)
        envelope.getJSONObject("payload").put("sourcePackage", "tampered.package")

        MigrationBundleValidator.validate(envelope.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAttachmentBytesWithIncorrectChecksum() {
        val payload = validPayload("original".toByteArray())
        val attachment = payload.getJSONObject("roomDatabase")
            .getJSONArray("attachmentFiles").getJSONObject(0)
        attachment.put("contentBase64", Base64.getEncoder().encodeToString("modified".toByteArray()))

        // Recompute the envelope digest to isolate the attachment-level checksum check.
        MigrationBundleValidator.validate(envelopeFor(payload).toString())
    }

    private fun validPayload(attachmentBytes: ByteArray? = null): JSONObject {
        val attachments = JSONArray()
        if (attachmentBytes != null) {
            val digest = sha256(attachmentBytes)
            attachments.put(JSONObject().apply {
                put("attachmentUid", "attachment-1")
                put("fileName", "receipt.jpg")
                put("mimeType", "image/jpeg")
                put("sizeBytes", attachmentBytes.size)
                put("sha256", digest)
                put("contentBase64", Base64.getEncoder().encodeToString(attachmentBytes))
            })
        }
        return JSONObject().apply {
            put("format", "tracmio-migration-bundle")
            put("formatVersion", 1)
            put("legacyWebData", JSONObject().put("localStorage", JSONObject()))
            put("roomDatabase", JSONObject().apply {
                put("tables", JSONArray())
                put("attachmentFiles", attachments)
                put("attachmentWarnings", JSONArray())
                put("attachmentExportComplete", true)
            })
        }
    }

    private fun envelopeFor(payload: JSONObject): JSONObject {
        return JSONObject().apply {
            put("format", "tracmio-migration-envelope")
            put("formatVersion", 1)
            put("sha256", sha256(com.tracmio.app.data.migration.MigrationJsonCanonicalizer.canonicalize(payload).toByteArray(Charsets.UTF_8)))
            put("payload", payload)
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
