package app.edgevol.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStatusTest {

    @Test
    fun `restricted settings help is not shown on API 26`() {
        assertFalse(shouldShowRestrictedSettingsHelp(26))
    }

    @Test
    fun `restricted settings help is not shown on API 32`() {
        assertFalse(shouldShowRestrictedSettingsHelp(32))
    }

    @Test
    fun `restricted settings help is shown on API 33`() {
        assertTrue(shouldShowRestrictedSettingsHelp(33))
    }

    @Test
    fun `restricted settings help is shown on API 34`() {
        assertTrue(shouldShowRestrictedSettingsHelp(34))
    }

    @Test
    fun `restricted settings help is shown on API 36`() {
        assertTrue(shouldShowRestrictedSettingsHelp(36))
    }

    @Test
    fun `SetupStatus defaults showRestrictedSettingsHelp to false`() {
        assertFalse(SetupStatus(true).showRestrictedSettingsHelp)
        assertFalse(SetupStatus(false).showRestrictedSettingsHelp)
    }
}
