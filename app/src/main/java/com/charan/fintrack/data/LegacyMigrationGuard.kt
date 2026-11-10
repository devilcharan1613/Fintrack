package com.charan.fintrack.data

/** Prevent unreadable legacy WebView saves from being imported as an empty seed database. */
internal object LegacyMigrationGuard {
    fun requireReadable(recoveryRequired: Boolean) {
        require(!recoveryRequired) {
            "Saved finance data could not be read. The legacy recovery copy was preserved and native migration was stopped. Restore a valid backup before retrying."
        }
    }
}
