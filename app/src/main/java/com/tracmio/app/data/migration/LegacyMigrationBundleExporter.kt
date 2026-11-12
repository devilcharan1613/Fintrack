package com.tracmio.app.data.migration

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Base64
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

    fun create(
        database: RoomDatabase,
        legacyPayloadJson: String,
        sourcePackage: String,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        require(legacyPayloadJson.isNotBlank()) { "Legacy app data is empty" }
        require(legacyPayloadJson.length <= MAX_LEGACY_PAYLOAD_CHARS) { "Legacy app data exceeds the safe export limit" }
        val legacyPayload = runCatching { JSONObject(legacyPayloadJson) }
            .getOrElse { throw IllegalArgumentException("Legacy app data is not valid JSON") }

        val sqlite = database.openHelper.readableDatabase
        val tables = readTables(sqlite)
        val userVersion = sqlite.query("PRAGMA user_version").use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        val roomSnapshot = JSONObject().apply {
            put("userVersion", userVersion)
            put("tables", tables)
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

        val canonicalPayload = payload.toString()
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
