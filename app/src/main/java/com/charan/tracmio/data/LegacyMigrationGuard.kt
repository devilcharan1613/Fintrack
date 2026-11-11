package com.charan.tracmio.data

object LegacyMigrationGuard {
    fun shouldImportLegacyJson(
        alreadyMigrated: Boolean,
        existingAccountCount: Int,
        existingTransactionCount: Int,
        legacyJson: String?
    ): Boolean {
        if (alreadyMigrated) return false
        if (existingAccountCount > 0 || existingTransactionCount > 0) return false
        if (legacyJson.isNullOrBlank()) return false
        val trimmed = legacyJson.trim()
        return trimmed.startsWith("{") && (trimmed.contains("\"accounts\"") || trimmed.contains("\"transactions\""))
    }
}
