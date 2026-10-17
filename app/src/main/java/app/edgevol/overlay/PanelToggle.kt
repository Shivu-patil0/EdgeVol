package app.edgevol.overlay

/**
 * Decides whether a tap opens the system volume panel or is swallowed as a "close" tap. It is a
 * debounce: a tap that starts within [assumedVisibleMs] after our last show does nothing, because
 * the panel is assumed to still be up and re-requesting it would only restart its timeout (MIUI
 * flickers). The panel hides itself ~2.0 s after being shown on MIUI (stock Android ~3 s). Whether
 * a touch over our handle closes it is OEM-dependent and verified only as "no" on MIUI 12.5 (the
 * panel window sits below our overlay); a wrong guess costs one extra tap. Pure Kotlin: [clock] is
 * injected (the view passes `SystemClock::elapsedRealtime`).
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
