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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
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
            Log.d(TAG, "prefs loaded: side=${prefs.side} pct=${prefs.positionPct} size=${prefs.size} opacity=${prefs.opacityPct}")
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "prefs save failed: ${e.javaClass.simpleName}")
                    }
                }
            }
            overlay = controller
            isRunning = controller.apply(prefs)
            // Live updates from the setup screen. A snap's own save comes back here too; apply() is idempotent.
            try {
                repository.prefs.distinctUntilChanged().collect { latest ->
                    Log.d(TAG, "prefs applied: side=${latest.side} pct=${latest.positionPct} size=${latest.size} opacity=${latest.opacityPct}")
                    isRunning = controller.apply(latest)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "prefs watch failed: ${e.javaClass.simpleName}")
            }
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
        overlay?.relayout(newConfig)
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
         * Process-wide: true once the service is connected and its handle is shown (or deliberately
         * hidden because the user chose tile-only), until [onUnbind]/[onDestroy].
         */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
