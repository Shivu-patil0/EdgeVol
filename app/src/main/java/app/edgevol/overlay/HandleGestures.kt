package app.edgevol.overlay

import kotlin.math.abs
import kotlin.math.floor

/** What the handle view should do in response to a gesture event. */
internal sealed interface HandleAction {
    data object None : HandleAction

    data object ShowPanel : HandleAction

    /** Step the volume [count] times (`count >= 1`); [up] is true for an upward swipe. */
    data class Step(val up: Boolean, val count: Int) : HandleAction

    data object BeginReposition : HandleAction

    /** Incremental vertical movement since the previous event; never 0. */
    data class Reposition(val dyPx: Float) : HandleAction
}

/**
 * Pure gesture state machine for the edge handle (no Android types, so it is JVM-testable).
 *
 * The view passes raw screen coordinates (`getRawX/Y`), because the window itself moves under the
 * finger while repositioning. Up is a decreasing y.
 */
internal class HandleGestures(private val stepPx: Float, private val slopPx: Float) {
    enum class State { Idle, Pressed, Stepping, Repositioning }

    var state: State = State.Idle
        private set

    private var downX = 0f
    private var downY = 0f
    private var anchorY = 0f
    private var lastY = 0f
    private var slopExceeded = false

    init {
        require(stepPx > 0f) { "stepPx must be positive" }
    }

    fun onDown(x: Float, y: Float): HandleAction {
        downX = x
        downY = y
        anchorY = y
        lastY = y
        slopExceeded = false
        state = State.Pressed
        return HandleAction.None
    }

    fun onMove(x: Float, y: Float): HandleAction {
        when (state) {
            State.Idle -> return HandleAction.None
            State.Repositioning -> {
                val dy = y - lastY
                lastY = y
                return if (dy == 0f) HandleAction.None else HandleAction.Reposition(dy)
            }
            State.Pressed, State.Stepping -> {
                lastY = y
                // Slop is measured from the DOWN point, per axis. x is ignored once Stepping.
                if (state == State.Pressed && (abs(x - downX) > slopPx || abs(y - downY) > slopPx)) {
                    slopExceeded = true
                }
                val dy = y - anchorY
                val distance = abs(dy)
                if (distance < stepPx) return HandleAction.None
                val count = floor(distance / stepPx).toInt()
                val up = dy < 0f
                anchorY += (if (up) -1f else 1f) * count * stepPx
                state = State.Stepping
                return HandleAction.Step(up = up, count = count)
            }
        }
    }

    fun onLongPress(): HandleAction {
        if (state != State.Pressed || slopExceeded) return HandleAction.None
        state = State.Repositioning
        return HandleAction.BeginReposition
    }

    fun onUp(): HandleAction {
        val action = if (state == State.Pressed) HandleAction.ShowPanel else HandleAction.None
        state = State.Idle
        return action
    }

    fun onCancel(): HandleAction {
        state = State.Idle
        return HandleAction.None
    }
}
