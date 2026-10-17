package app.edgevol.overlay

import app.edgevol.data.HandleSide
import app.edgevol.data.Prefs
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the handle view should do in response to a gesture event. */
internal sealed interface HandleAction {
    data object None : HandleAction

    data object ShowPanel : HandleAction

    data object BeginReposition : HandleAction

    /** Incremental raw-coordinate movement since the previous event; never both 0. */
    data class Reposition(val dxPx: Float, val dyPx: Float) : HandleAction

    /** End of a drag at the finger's raw position. [moved] is false for hold-and-release in place. */
    data class EndReposition(val rawX: Float, val rawY: Float, val moved: Boolean) : HandleAction
}

/**
 * Pure gesture state machine for the edge handle (no Android types, so it is JVM-testable).
 *
 * The view passes raw screen coordinates (`getRawX/Y`), because the window itself moves under the
 * finger while repositioning. A quick touch is a tap ([HandleAction.ShowPanel] on release); holding
 * still for the long-press time begins a reposition drag.
 */
internal class HandleGestures(private val slopPx: Float, private val tapMaxTravelPx: Float) {
    enum class State { Idle, Pressed, Ignored, Repositioning }

    var state: State = State.Idle
        private set

    private var downX = 0f
    private var downY = 0f
    private var beginX = 0f
    private var beginY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var slopExceeded = false
    private var moved = false

    fun onDown(x: Float, y: Float): HandleAction {
        downX = x
        downY = y
        lastX = x
        lastY = y
        slopExceeded = false
        moved = false
        state = State.Pressed
        return HandleAction.None
    }

    fun onMove(x: Float, y: Float): HandleAction {
        when (state) {
            State.Idle, State.Ignored -> return HandleAction.None
            State.Pressed -> {
                lastX = x
                lastY = y
                // Both thresholds are per axis, measured from the DOWN point.
                val dx = abs(x - downX)
                val dy = abs(y - downY)
                if (dx > slopPx || dy > slopPx) slopExceeded = true
                if (dx > tapMaxTravelPx || dy > tapMaxTravelPx) state = State.Ignored
                return HandleAction.None
            }
            State.Repositioning -> {
                val dx = x - lastX
                val dy = y - lastY
                lastX = x
                lastY = y
                // Euclidean distance from where the drag began.
                val fromBeginX = x - beginX
                val fromBeginY = y - beginY
                if (fromBeginX * fromBeginX + fromBeginY * fromBeginY > slopPx * slopPx) moved = true
                return if (dx == 0f && dy == 0f) HandleAction.None else HandleAction.Reposition(dx, dy)
            }
        }
    }

    /** Fires after the hold time: begins a drag only if the finger stayed within slop. */
    fun onLongPress(): HandleAction {
        if (state != State.Pressed || slopExceeded) return HandleAction.None
        state = State.Repositioning
        beginX = lastX
        beginY = lastY
        return HandleAction.BeginReposition
    }

    fun onUp(x: Float, y: Float): HandleAction {
        val action = when (state) {
            State.Pressed -> HandleAction.ShowPanel
            State.Repositioning -> HandleAction.EndReposition(x, y, moved)
            State.Idle, State.Ignored -> HandleAction.None
        }
        state = State.Idle
        return action
    }

    fun onCancel(): HandleAction {
        state = State.Idle
        return HandleAction.None
    }
}

/** Maps where a drag ended to the persisted side and vertical position. Pure Kotlin. */
internal object HandleSnap {
    private val DEFAULT_POSITION_PCT: Int = Prefs().positionPct

    /** The half of the screen the finger is in; exactly half goes to the right. */
    fun sideFor(fingerRawX: Float, screenWidthPx: Int): HandleSide =
        if (fingerRawX < screenWidthPx / 2f) HandleSide.LEFT else HandleSide.RIGHT

    /** Handle centre as a percent of the screen height, clamped to the allowed range. */
    fun positionPct(centreYPx: Float, screenHeightPx: Int): Int {
        if (screenHeightPx <= 0) return DEFAULT_POSITION_PCT
        val min = HandleDefaults.POSITION_MIN_PERCENT.toInt()
        val max = HandleDefaults.POSITION_MAX_PERCENT.toInt()
        return (centreYPx / screenHeightPx * 100f).roundToInt().coerceIn(min, max)
    }
}
