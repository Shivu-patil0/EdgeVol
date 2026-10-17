package app.edgevol.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

// Stub: the real behaviour (handle overlay, volume steps) arrives in M2.
class VolumeAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
