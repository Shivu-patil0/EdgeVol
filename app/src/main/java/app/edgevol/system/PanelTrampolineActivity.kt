package app.edgevol.system

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import app.edgevol.audio.SystemVolume

/**
 * Invisible, finishes at once. Launched by the Quick Settings tile through startActivityAndCollapse,
 * which is what closes the shade; the panel is shown once the shade is out of the way.
 */
class PanelTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val volume = SystemVolume(applicationContext)
        Handler(Looper.getMainLooper()).postDelayed({ volume.showPanel() }, SHADE_COLLAPSE_DELAY_MS)
        finish()
        overridePendingTransition(0, 0)
    }

    private companion object {
        // The shade's collapse animation; a panel requested earlier can end up hidden behind it.
        const val SHADE_COLLAPSE_DELAY_MS = 350L
    }
}
