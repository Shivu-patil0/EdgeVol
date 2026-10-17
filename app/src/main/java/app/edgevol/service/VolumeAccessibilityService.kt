package app.edgevol.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import app.edgevol.audio.SystemVolume
import app.edgevol.overlay.OverlayController

/**
 * Keeps the edge handle on screen while the service is enabled. It reads no events and no screen
 * content (`canRetrieveWindowContent="false"`); it exists to be allowed to add the overlay window.
 */
class VolumeAccessibilityService : AccessibilityService() {
    private var overlay: OverlayController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay?.removeHandle()
        val controller = OverlayController(this, SystemVolume(this))
        overlay = controller
        controller.showHandle()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.relayout()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        overlay?.removeHandle()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        overlay?.removeHandle()
        overlay = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
