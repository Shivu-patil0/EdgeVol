package app.edgevol.overlay

/** Which physical screen edge the handle sits on (never START/END). */
internal enum class HandleSide { Left, Right }

/** The single home of the M2 handle defaults. Nothing else hard-codes these values. */
internal object HandleDefaults {
    val SIDE: HandleSide = HandleSide.Right

    /** Vertical position of the handle's centre, as a percentage of the screen height. */
    const val POSITION_PERCENT: Float = 40f
    const val POSITION_MIN_PERCENT: Float = 10f
    const val POSITION_MAX_PERCENT: Float = 90f

    const val HEIGHT_DP: Float = 72f
    const val TOUCH_WIDTH_DP: Float = 24f
    const val PILL_WIDTH_DP: Float = 4f

    /** Idle opacity of the visible pill, 0..1. */
    const val OPACITY: Float = 0.35f

    /** One volume step per this much vertical travel. */
    const val STEP_DP: Float = 24f

    const val LONG_PRESS_MS: Long = 500L

    /** Indigo #4F46E5. */
    const val COLOR_INDIGO: Int = 0xFF4F46E5.toInt()

    const val WINDOW_TITLE: String = "EdgeVolHandle"
}
