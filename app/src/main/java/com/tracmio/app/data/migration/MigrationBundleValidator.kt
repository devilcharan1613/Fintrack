package com.tracmio.app.data.migration

import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

/**
 * Validates a decrypted Tracmio migration envelope before any importer can stage its data.
 * This class is deliberately read-only: it never opens or mutates the live Room database.
 */
object MigrationBundleValidator {
    private const val ENVELOPE_FORMAT = "tracmio-migration-envelope"
    private const val PAYLOAD_FORMAT = "tracmio-migration-bundle"
    private const val FORMAT_VERSION = 1
    private const val MAX_ATTACHMENT_BYTES_EACH = 8 * 1024 * 1024
    private const val MAX_ATTACHMENT_BYTES_TOTAL = 24 * 1024 * 1024

    data class ValidationResult(
        val payload: JSONObject,
        val attachmentCount: Int,
        val attachmentWarningCount: Int,
        val attachmentExportComplete: Boolean
    )

    /**
     * Throws IllegalArgumentException for malformed, unsupported, or integrity-failed bundles.
     * The digest is checked before attachment contents are decoded or made available to an importer.
     */
    fun validate(decryptedEnvelopeJson: String): ValidationResult {
        val envelope = try {
            JSONObject(decryptedEnvelopeJson)
        } catch (error: Exception) {
            throw IllegalArgumentException("Migration envelope is not valid JSON", error)
        }

        require(envelope.optString("format") == ENVELOPE_FORMAT) {
            "Unsupported migration envelope format"
        }
        require(envelope.optInt("formatVersion", -1) == FORMAT_VERSION) {
            "Unsupported migration envelope version"
        }

        val payload = envelope.optJSONObject("payload")
            ?: throw IllegalArgumentException("Migration payload is missing")
        require(payload.optString("format") == PAYLOAD_FORMAT) {
            "Unsupported migration payload format"
        }
        require(payload.optInt("formatVersion", -1) == FORMAT_VERSION) {
            "Unsupported migration payload version"
        }
        require(payload.optJSONObject("legacyWebData") != null) {
            "Legacy WebView data is missing"
        }

        val room = payload.optJSONObject("roomDatabase")
            ?: throw IllegalArgumentException("Room database snapshot is missing")
        require(room.optJSONArray("tables") != null) {
            "Room table snapshot is missing"
        }

        val expectedHex = envelope.optString("sha256", "")
        require(expectedHex.matches(Regex("[0-9a-fA-F]{64}"))) {
            "Migration payload checksum is missing or malformed"
        }
        val actualDigest = MessageDigest.getInstance("SHA-256")
            .digest(MigrationJsonCanonicalizer.canonicalize(payload).toByteArray(Charsets.UTF_8))
        val expectedDigest = hexToBytes(expectedHex)
        require(MessageDigest.isEqual(expectedDigest, actualDigest)) {
            "Migration payload checksum mismatch. The recovery file may be corrupted or modified."
        }

        val attachments = room.optJSONArray("attachmentFiles") ?: org.json.JSONArray()
        val warnings = room.optJSONArray("attachmentWarnings") ?: org.json.JSONArray()
        var totalBytes = 0L

        for (index in 0 until attachments.length()) {
            val attachment = attachments.optJSONObject(index)
                ?: throw IllegalArgumentException("Attachment entry $index is malformed")
            val encoded = attachment.optString("contentBase64", "")
            val bytes = try {
                Base64.getDecoder().decode(encoded)
            } catch (error: Exception) {
                throw IllegalArgumentException("Attachment entry $index has invalid Base64 data", error)
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_ATTACHMENT_BYTES_EACH) {
                "Attachment entry $index has an invalid size"
            }
            totalBytes += bytes.size
            require(totalBytes <= MAX_ATTACHMENT_BYTES_TOTAL) {
                "Attachment data exceeds the 24 MB validation limit"
            }
            require(attachment.optLong("sizeBytes", -1L) == bytes.size.toLong()) {
                "Attachment entry $index size does not match its metadata"
            }
            val expectedAttachmentDigest = attachment.optString("sha256", "")
            require(expectedAttachmentDigest.matches(Regex("[0-9a-fA-F]{64}"))) {
                "Attachment entry $index checksum is missing or malformed"
            }
            val actualAttachmentDigest = MessageDigest.getInstance("SHA-256").digest(bytes)
            require(MessageDigest.isEqual(hexToBytes(expectedAttachmentDigest), actualAttachmentDigest)) {
                "Attachment entry $index checksum mismatch"
            }
        }

        return ValidationResult(
            payload = payload,
            attachmentCount = attachments.length(),
            attachmentWarningCount = warnings.length(),
            attachmentExportComplete = room.optBoolean("attachmentExportComplete", warnings.length() == 0)
        )
    }

    private fun hexToBytes(value: String): ByteArray {
        return ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}
