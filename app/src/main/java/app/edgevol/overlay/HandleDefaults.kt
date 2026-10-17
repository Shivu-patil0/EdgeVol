package app.edgevol.overlay

/** The single home of the handle constants. Nothing else hard-codes these values. */
internal object HandleDefaults {
    const val POSITION_MIN_PERCENT: Float = 10f
    const val POSITION_MAX_PERCENT: Float = 90f

    const val HEIGHT_DP: Float = 72f

    /** Width of the touch window. Must stay >= [PILL_WIDTH_ACTIVE_DP]. */
    const val TOUCH_WIDTH_DP: Float = 24f

    /** Idle width of the visible pill. */
    const val PILL_WIDTH_DP: Float = 4f

    /** Width of the visible pill while the user holds and drags it. */
    const val PILL_WIDTH_ACTIVE_DP: Float = 12f

    const val WIDEN_DURATION_MS: Long = 120L
    const val NARROW_DURATION_MS: Long = 160L

    /** Idle opacity of the visible pill, 0..1. */
    const val OPACITY: Float = 0.35f

    /** Moving further than this (per axis) before the hold fires turns the touch into a non-tap. */
    const val TAP_MAX_TRAVEL_DP: Float = 48f

    const val LONG_PRESS_MS: Long = 500L

    /**
     * How long after our own panel show a tap is treated as a "close" tap. MIUI auto-hides the panel
     * after ~2.0 s, stock Android after ~3 s; MIUI/AOSP dismiss the panel on any touch outside it,
     * including our ACTION_DOWN, so a tap within this window is a "close" tap. 1800 ms is below the
     * shortest known timeout, so we rarely swallow a tap when the panel is already gone; a wrong
     * guess costs one extra tap (swallowed) or the old flicker (reopen), never a failure.
     */
    const val PANEL_ASSUMED_VISIBLE_MS: Long = 1800L

    /** Indigo #4F46E5. */
    const val COLOR_INDIGO: Int = 0xFF4F46E5.toInt()

    const val WINDOW_TITLE: String = "EdgeVolHandle"
}
