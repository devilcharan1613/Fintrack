package com.tracmio.app.data.migration

import org.json.JSONArray
import org.json.JSONObject

/**
 * Produces deterministic JSON for integrity hashing. Object keys are sorted recursively;
 * array order is preserved. This avoids relying on JSONObject's map iteration order.
 */
object MigrationJsonCanonicalizer {
    fun canonicalize(value: Any?): String {
        return when {
            value == null || value === JSONObject.NULL -> "null"
            value is JSONObject -> {
                val keys = value.keys().asSequence().toList().sorted()
                keys.joinToString(prefix = "{", postfix = "}", separator = ",") { key ->
                    JSONObject.quote(key) + ":" + canonicalize(value.get(key))
                }
            }
            value is JSONArray -> {
                (0 until value.length()).joinToString(prefix = "[", postfix = "]", separator = ",") { index ->
                    canonicalize(value.get(index))
                }
            }
            value is String -> JSONObject.quote(value)
            value is Number || value is Boolean -> value.toString()
            else -> JSONObject.quote(value.toString())
        }
    }
}
