package app.edgevol.audio

import android.content.Context
import android.media.AudioManager

/**
 * The only way EdgeVol touches volume: it asks the system to show its own panel, without changing
 * the volume. No custom volume UI, no stream selection.
 */
class SystemVolume internal constructor(
    private val adjust: (direction: Int, streamType: Int, flags: Int) -> Unit,
) {
    constructor(context: Context) : this(audioManagerAdjust(context))

    /** Shows the system volume panel without changing the volume. */
    fun showPanel() {
        adjust(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)
    }
}

// Captures only the AudioManager, never the Context.
private fun audioManagerAdjust(context: Context): (Int, Int, Int) -> Unit {
    val audioManager = context.getSystemService(AudioManager::class.java)
    return { direction, streamType, flags ->
        audioManager.adjustSuggestedStreamVolume(direction, streamType, flags)
    }
}
