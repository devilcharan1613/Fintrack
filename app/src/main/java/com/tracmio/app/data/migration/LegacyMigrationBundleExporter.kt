package com.tracmio.app.data.migration

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Base64
import java.io.File
import androidx.room.RoomDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Read-only recovery exporter. It never writes to, clears, or migrates the source database.
 * The full SQLite table snapshot is included so future import code can map every Room entity
 * and relationship without silently omitting tables unknown to this exporter.
 */
object LegacyMigrationBundleExporter {
    private const val MAX_LEGACY_PAYLOAD_CHARS = 20_000_000
    private const val MAX_BUNDLE_BYTES = 50 * 1024 * 1024
    private const val MAX_ATTACHMENT_BYTES_TOTAL = 24 * 1024 * 1024
    private const val MAX_ATTACHMENT_BYTES_EACH = 8 * 1024 * 1024

    fun create(
        database: RoomDatabase,
        legacyPayloadJson: String,
        sourcePackage: String,
        appFilesDir: File,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        require(legacyPayloadJson.isNotBlank()) { "Legacy app data is empty" }
        require(legacyPayloadJson.length <= MAX_LEGACY_PAYLOAD_CHARS) { "Legacy app data exceeds the safe export limit" }
        val legacyPayload = runCatching { JSONObject(legacyPayloadJson) }
            .getOrElse { throw IllegalArgumentException("Legacy app data is not valid JSON") }

        val sqlite = database.openHelper.readableDatabase
        val tables = readTables(sqlite)
        val attachmentResult = readAttachmentFiles(tables, appFilesDir)
        val userVersion = sqlite.query("PRAGMA user_version").use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        val roomSnapshot = JSONObject().apply {
            put("userVersion", userVersion)
            put("tables", tables)
            put("attachmentFiles", attachmentResult.files)
            put("attachmentWarnings", attachmentResult.warnings)
            put("attachmentExportComplete", attachmentResult.warnings.length() == 0)
        }

        val payload = JSONObject().apply {
            put("format", "tracmio-migration-bundle")
            put("formatVersion", 1)
            put("app", "Tracmio")
            put("sourcePackage", sourcePackage)
            put("exportedAt", exportedAt)
            put("legacyWebData", legacyPayload)
            put("roomDatabase", roomSnapshot)
        }

        val canonicalPayload = MigrationJsonCanonicalizer.canonicalize(payload)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonicalPayload.toByteArray(Charsets.UTF_8))
        val bundle = JSONObject().apply {
            put("format", "tracmio-migration-envelope")
            put("formatVersion", 1)
            put("sha256", digest.joinToString("") { "%02x".format(it) })
            put("payload", payload)
        }.toString()

        require(bundle.toByteArray(Charsets.UTF_8).size <= MAX_BUNDLE_BYTES) {
            "Migration bundle exceeds 50 MB. Export attachments separately and retry."
        }
        return bundle
    }

    private fun readTables(database: SupportSQLiteDatabase): JSONArray {
        val result = JSONArray()
        val names = mutableListOf<String>()
        database.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0) ?: continue
                if (name.matches(Regex("[A-Za-z0-9_]+"))) names += name
            }
        }

        names.forEach { table ->
            val rows = JSONArray()
            val quotedTable = "`$table`"
            database.query("SELECT * FROM $quotedTable").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (index in 0 until cursor.columnCount) {
                        row.put(cursor.getColumnName(index), cursorValue(cursor, index))
                    }
                    rows.put(row)
                }
            }
            result.put(JSONObject().apply {
                put("name", table)
                put("rows", rows)
            })
        }
        return result
    }

    /**
     * Copies only files inside this app's tracker_attachments directory. Absolute paths
     * from database rows are treated as untrusted and are never followed outside that folder.
     */
    private data class AttachmentExportResult(
        val files: JSONArray,
        val warnings: JSONArray
    )

    /**
     * Copies only files inside this app's tracker_attachments directory. Absolute paths
     * from database rows are treated as untrusted and are never followed outside that folder.
     * Every skipped row is reported so a bundle can never silently claim complete attachment
     * recovery when a referenced file was missing or unsafe.
     */
    private fun readAttachmentFiles(tables: JSONArray, appFilesDir: File): AttachmentExportResult {
        val result = JSONArray()
        val warnings = JSONArray()
        val attachmentRoot = File(appFilesDir, "tracker_attachments").canonicalFile
        var totalBytes = 0L

        fun warn(row: JSONObject, reason: String) {
            warnings.put(JSONObject().apply {
                put("attachmentUid", row.optString("attachmentUid", ""))
                put("fileName", row.optString("fileName", ""))
                put("reason", reason)
            })
        }

        for (tableIndex in 0 until tables.length()) {
            val table = tables.optJSONObject(tableIndex) ?: continue
            if (table.optString("name") != "tracker_attachments") continue
            val rows = table.optJSONArray("rows") ?: continue

            for (rowIndex in 0 until rows.length()) {
                val row = rows.optJSONObject(rowIndex) ?: continue
                val rawPath = row.optString("localPath", "")
                if (rawPath.isBlank()) {
                    val contentUri = row.optString("contentUri", "")
                    warn(row, if (contentUri.isNotBlank()) "CONTENT_URI_NOT_EMBEDDED" else "EMPTY_LOCAL_PATH")
                    continue
                }

                val candidate = runCatching { File(rawPath).canonicalFile }.getOrNull()
                if (candidate == null) {
                    warn(row, "INVALID_LOCAL_PATH")
                    continue
                }

                val rootPrefix = attachmentRoot.path + File.separator
                if (!candidate.path.startsWith(rootPrefix)) {
                    warn(row, "PATH_OUTSIDE_ATTACHMENT_DIRECTORY")
                    continue
                }
                if (!candidate.isFile) {
                    warn(row, "FILE_MISSING")
                    continue
                }

                val length = candidate.length()
                if (length <= 0L) {
                    warn(row, "EMPTY_FILE")
                    continue
                }
                if (length > MAX_ATTACHMENT_BYTES_EACH) {
                    warn(row, "FILE_EXCEEDS_8_MB_LIMIT")
                    continue
                }
                if (totalBytes + length > MAX_ATTACHMENT_BYTES_TOTAL) {
                    warn(row, "TOTAL_ATTACHMENT_EXPORT_LIMIT_REACHED")
                    continue
                }

                val bytes = try {
                    candidate.readBytes()
                } catch (_: Exception) {
                    warn(row, "FILE_READ_FAILED")
                    continue
                }
                val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                result.put(JSONObject().apply {
                    put("attachmentUid", row.optString("attachmentUid", ""))
                    put("fileName", row.optString("fileName", candidate.name))
                    put("storedFileName", candidate.name)
                    put("mimeType", row.optString("mimeType", "application/octet-stream"))
                    put("sourceContentUri", row.optString("contentUri", ""))
                    put("recordedSizeBytes", row.optLong("fileSizeBytes", 0L))
                    put("sizeBytes", bytes.size)
                    put("sha256", digest.joinToString("") { "%02x".format(it) })
                    put("contentBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                })
                totalBytes += bytes.size
            }
        }
        return AttachmentExportResult(result, warnings)
    }

    private fun cursorValue(cursor: Cursor, index: Int): Any {
        return when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
            Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
            Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP)
            else -> JSONObject.NULL
        }
    }
}
