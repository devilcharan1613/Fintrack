package com.charan.fintrack.data

import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyMigrationGuardTest {
    @Test
    fun blocksMigrationWhenLegacyStorageWasUnreadable() {
        val result = runCatching { LegacyMigrationGuard.requireReadable(recoveryRequired = true) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("recovery copy was preserved") == true)
    }

    @Test
    fun allowsMigrationWhenLegacyStorageLoadedSuccessfully() {
        LegacyMigrationGuard.requireReadable(recoveryRequired = false)
    }
}
