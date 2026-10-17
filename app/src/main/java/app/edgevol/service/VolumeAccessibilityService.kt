package app.edgevol.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import app.edgevol.audio.SystemVolume
import app.edgevol.data.Prefs
import app.edgevol.data.PrefsRepository
import app.edgevol.overlay.OverlayController
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the edge handle on screen while the service is enabled. It reads no events and no screen
 * content (`canRetrieveWindowContent="false"`); it exists to be allowed to add the overlay window.
 */
class VolumeAccessibilityService : AccessibilityService() {
    private var overlay: OverlayController? = null
    private var scope: CoroutineScope? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        teardown()
        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = serviceScope
        serviceScope.launch {
            val repository = PrefsRepository(applicationContext)
            val prefs = loadPrefs(repository)
            // A cancelled load (service unbound meanwhile) must never add a handle.
            ensureActive()
            Log.d(TAG, "prefs loaded: side=${prefs.side} pct=${prefs.positionPct}")
            val controller = OverlayController(
                context = this@VolumeAccessibilityService,
                volume = SystemVolume(this@VolumeAccessibilityService),
                initial = prefs,
            ) { placement ->
                serviceScope.launch {
                    try {
                        // NonCancellable: unbinding right after a snap must not lose the placement.
                        withContext(NonCancellable) {
                            repository.update { it.copy(side = placement.side, positionPct = placement.positionPct) }
                        }
                        Log.d(TAG, "prefs saved: side=${placement.side} pct=${placement.positionPct}")
                    } catch (e: IOException) {
                        Log.w(TAG, "prefs save failed: ${e.javaClass.simpleName}")
                    }
                }
            }
            overlay = controller
            if (controller.showHandle()) isRunning = true
        }
    }

    // Never blocks the main thread and never fails: defaults if loading fails or times out.
    private suspend fun loadPrefs(repository: PrefsRepository): Prefs =
        try {
            withTimeoutOrNull(PREFS_LOAD_TIMEOUT_MS) { repository.prefs.first() } ?: Prefs()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "prefs load failed: ${e.javaClass.simpleName}")
            Prefs()
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.relayout()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    // Cancelling the scope first stops a still-pending prefs load from adding a handle afterwards.
    private fun teardown() {
        isRunning = false
        scope?.cancel()
        scope = null
        overlay?.removeHandle()
        overlay = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "EdgeVol"
        private const val PREFS_LOAD_TIMEOUT_MS = 1000L

        /**
         * Process-wide: true once the handle window has been added after [onServiceConnected], until
         * [onUnbind]/[onDestroy].
         */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
