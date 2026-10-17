package app.edgevol.overlay

/**
 * Decides whether a tap opens the system volume panel or only closes it. The system dismisses the
 * panel on our own touch-down, so a tap shortly after we showed it is a "close" tap and must not
 * re-show it. Pure Kotlin: [clock] is injected (the view passes `SystemClock::elapsedRealtime`).
 */
internal class PanelToggle(
    private val clock: () -> Long,
    private val assumedVisibleMs: Long = HandleDefaults.PANEL_ASSUMED_VISIBLE_MS,
) {
    private var shownAt: Long? = null
    private var closingTap = false

    fun onTouchDown() {
        val shown = shownAt
        closingTap = shown != null && clock() - shown < assumedVisibleMs
        if (closingTap) shownAt = null
    }

    /** Returns true if the caller should show the panel, false if this tap only closed it. */
    fun onTap(): Boolean {
        if (closingTap) {
            closingTap = false
            return false
        }
        shownAt = clock()
        return true
    }

    /** Records a panel show that did not come through a touch (accessibility click). */
    fun markShown() {
        shownAt = clock()
    }
}
