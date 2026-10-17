package app.edgevol.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import app.edgevol.MainActivity
import app.edgevol.R
import app.edgevol.data.PrefsRepository
import app.edgevol.system.SystemSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile = the "Show handle" switch. The running accessibility service reacts to the
 * saved value like to any other setting. Starting the app process for the tile also makes Android
 * restart a killed accessibility service at once.
 */
class VolumeTileService : TileService() {
    private val repository by lazy { PrefsRepository(applicationContext) }
    private var listening: CoroutineScope? = null

    // Not tied to the tile's listening window: a save must finish even if the shade closes.
    private val saves = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listening = scope
        scope.launch { repository.prefs.collect { render(it.handleEnabled) } }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val turnOn = qsTile?.state != Tile.STATE_ACTIVE
        Log.d(TAG, "tile: handle ${if (turnOn) "on" else "off"}")
        render(turnOn)
        saves.launch {
            try {
                withContext(NonCancellable) { repository.update { it.copy(handleEnabled = turnOn) } }
            } catch (e: Exception) {
                Log.w(TAG, "tile save failed: ${e.javaClass.simpleName}")
            }
        }
        // Without the accessibility service there is no handle to show: take the user to setup.
        if (turnOn && !SystemSettings.status(this).serviceEnabled) openSetup()
    }

    private fun render(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = getString(if (enabled) R.string.tile_state_on else R.string.tile_state_off)
        }
        tile.updateTile()
    }

    // The Intent overload is only used below Android 14, where the PendingIntent overload doesn't exist.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openSetup() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        const val TAG = "EdgeVol"
    }
}
