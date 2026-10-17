package app.edgevol.ui.setup

import app.edgevol.system.SetupStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupStateTest {

    @Test
    fun `not enabled and not running is Off`() {
        assertEquals(SetupState.Off, SetupStatus(serviceEnabled = false, serviceRunning = false).toSetupState())
    }

    @Test
    fun `not enabled is Off even when the service is running`() {
        assertEquals(SetupState.Off, SetupStatus(serviceEnabled = false, serviceRunning = true).toSetupState())
    }

    @Test
    fun `enabled but not running is EnabledNotRunning`() {
        assertEquals(
            SetupState.EnabledNotRunning,
            SetupStatus(serviceEnabled = true, serviceRunning = false).toSetupState(),
        )
    }

    @Test
    fun `enabled and running is Running`() {
        assertEquals(SetupState.Running, SetupStatus(serviceEnabled = true, serviceRunning = true).toSetupState())
    }

    @Test
    fun `SetupStatus(true) alone is EnabledNotRunning`() {
        assertEquals(SetupState.EnabledNotRunning, SetupStatus(true).toSetupState())
    }

    @Test
    fun `SetupStatus(false) alone is Off`() {
        assertEquals(SetupState.Off, SetupStatus(false).toSetupState())
    }

    @Test
    fun `restricted settings help does not influence the state`() {
        assertEquals(
            SetupState.Off,
            SetupStatus(serviceEnabled = false, showRestrictedSettingsHelp = true).toSetupState(),
        )
        assertEquals(
            SetupState.Running,
            SetupStatus(serviceEnabled = true, showRestrictedSettingsHelp = true, serviceRunning = true)
                .toSetupState(),
        )
    }
}
