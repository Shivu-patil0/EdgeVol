package app.edgevol.audio

import android.content.Context
import android.media.AudioManager

/**
 * The only way EdgeVol touches volume: it asks the system to show its own panel or to step the
 * stream the system considers active. No custom volume UI, no stream selection.
 */
class SystemVolume internal constructor(
    private val adjust: (direction: Int, streamType: Int, flags: Int) -> Unit,
) {
    constructor(context: Context) : this(audioManagerAdjust(context))

    /** Shows the system volume panel without changing the volume. */
    fun showPanel() {
        adjust(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Raises or lowers the volume by one system step and shows the system panel.
     * Returns false if the system refused the step with a [SecurityException]
     * (e.g. a step that would toggle Do Not Disturb); the caller should then call [showPanel].
     */
    fun step(up: Boolean): Boolean {
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        return try {
            adjust(direction, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

// Captures only the AudioManager, never the Context.
private fun audioManagerAdjust(context: Context): (Int, Int, Int) -> Unit {
    val audioManager = context.getSystemService(AudioManager::class.java)
    return { direction, streamType, flags ->
        audioManager.adjustSuggestedStreamVolume(direction, streamType, flags)
    }
}
