package app.edgevol.data

enum class HandleSide { LEFT, RIGHT }

/** [positionPct] is the vertical centre of the handle in percent of the screen height (10..90). */
data class Prefs(
    val side: HandleSide = HandleSide.RIGHT,
    val positionPct: Int = 40,
)
