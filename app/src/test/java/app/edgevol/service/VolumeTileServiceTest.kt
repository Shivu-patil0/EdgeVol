package app.edgevol.service

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.edgevol.MainActivity
import app.edgevol.R
import app.edgevol.data.PrefsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The real tile and DataStore. The Android 14+ PendingIntent path to setup is checked by hand (not shadowed). */
@RunWith(AndroidJUnit4::class)
class VolumeTileServiceTest {
    private val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val repository = PrefsRepository(app)

    private fun stored(): Boolean = runBlocking { repository.prefs.first().handleEnabled }

    private fun store(enabled: Boolean) {
        runBlocking { repository.update { it.copy(handleEnabled = enabled) } }
    }

    private fun setAccessibilityEnabled(enabled: Boolean) {
        val value = if (enabled) ComponentName(app, VolumeAccessibilityService::class.java).flattenToString() else ""
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, value)
    }

    // Saves finish on DataStore's own thread and resume on the main looper.
    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("timed out waiting for $what")
    }

    // A new Tile starts ACTIVE on API 26 but INACTIVE on 30+; start from neither so rendering is observable.
    private fun listeningTile(): VolumeTileService =
        Robolectric.setupService(VolumeTileService::class.java).apply {
            qsTile.state = Tile.STATE_UNAVAILABLE
            onStartListening()
        }

    private fun assertRendered(tile: Tile, on: Boolean) {
        assertEquals(if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE, tile.state)
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals(app.getString(if (on) R.string.tile_state_on else R.string.tile_state_off), tile.subtitle)
        }
    }

    @Test
    @Config(sdk = [26, 30, 35])
    fun `tile shows the stored state and a tap turns the handle off`() {
        setAccessibilityEnabled(true)
        store(true)
        val service = listeningTile()
        waitFor("tile On") { service.qsTile.state == Tile.STATE_ACTIVE }
        assertRendered(service.qsTile, on = true)

        service.onClick()

        assertRendered(service.qsTile, on = false)
        waitFor("handle off saved") { !stored() }
        assertNull(shadowOf(app).nextStartedActivity)
        service.onStopListening()
    }

    @Test
    @Config(sdk = [26, 30, 35])
    fun `with the service on, a tap turns the handle back on without opening the app`() {
        setAccessibilityEnabled(true)
        store(false)
        val service = listeningTile()
        waitFor("tile Off") { service.qsTile.state == Tile.STATE_INACTIVE }

        service.onClick()

        assertRendered(service.qsTile, on = true)
        waitFor("handle on saved") { stored() }
        assertNull(shadowOf(app).nextStartedActivity)
        service.onStopListening()
    }

    @Test
    @Config(sdk = [26, 30])
    fun `turning on without the accessibility service saves it and opens setup`() {
        setAccessibilityEnabled(false)
        store(false)
        val service = listeningTile()
        waitFor("tile Off") { service.qsTile.state == Tile.STATE_INACTIVE }

        service.onClick()

        waitFor("handle on saved") { stored() }
        val started = shadowOf(app).nextStartedActivity
        assertEquals(ComponentName(app, MainActivity::class.java), started.component)
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        service.onStopListening()
    }
}
