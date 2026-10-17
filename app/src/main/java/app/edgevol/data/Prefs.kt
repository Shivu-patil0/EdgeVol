package app.edgevol.data

enum class HandleSide { LEFT, RIGHT }

/** Height of the handle: S 56 dp, M 72 dp, L 120 dp. */
enum class HandleSize(val heightDp: Int) { S(56), M(72), L(120) }

/**
 * [positionPct] is the vertical centre of the handle in percent of the screen height (10..90).
 * [opacityPct] is the idle opacity of the handle (10..80).
 * [handleEnabled] is the "Show handle" switch, also flipped by the Quick Settings tile.
 */
data class Prefs(
    val side: HandleSide = HandleSide.RIGHT,
    val positionPct: Int = 40,
    val size: HandleSize = HandleSize.M,
    val opacityPct: Int = 35,
    val handleEnabled: Boolean = true,
) {
    val handleHidden: Boolean get() = !handleEnabled

    companion object {
        const val POSITION_MIN_PCT = 10
        const val POSITION_MAX_PCT = 90
        const val OPACITY_MIN_PCT = 10
        const val OPACITY_MAX_PCT = 80
    }
}
