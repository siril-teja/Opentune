package com.opentune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    // The salt app/build.gradle.kts uses, and the hash it makes of "correct horse" (checked with Python's hashlib).
    private val salt = "f1a4f60f63de9945938ff4dbe4655af1"
    private val hash = "d9860d9dd2acadb49f73f4a85bc404bd12420514985c1d53833afcd208931dd9"

    @Test fun theAppDerivesWhatTheBuildDid() {
        assertEquals(hash, AppLock.derive("correct horse", salt))
    }

    @Test fun onlyTheSecretUnlocks() {
        assertTrue(AppLock.matches("correct horse", salt, hash))
        assertTrue(AppLock.matches("  correct horse ", salt, hash))
        assertFalse(AppLock.matches("Correct horse", salt, hash))
        assertFalse(AppLock.matches("", salt, hash))
    }
}
