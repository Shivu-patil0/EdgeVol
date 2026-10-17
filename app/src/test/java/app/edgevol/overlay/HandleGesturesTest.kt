package app.edgevol.overlay

import app.edgevol.overlay.HandleGestures.State
import org.junit.Assert.assertEquals
import org.junit.Test

/** HandleGestures with step = 24f and slop = 8f. Up is a decreasing y. */
class HandleGesturesTest {

    private val step = 24f
    private val slop = 8f

    private fun gestures() = HandleGestures(stepPx = step, slopPx = slop)

    // 1
    @Test
    fun `scenario 01 down then up shows the panel and ends idle`() {
        val g = gestures()
        assertEquals(HandleAction.None, g.onDown(0f, 100f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp())
        assertEquals(State.Idle, g.state)
    }

    // 2
    @Test
    fun `scenario 02 move within slop then up shows the panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(3f, 104f))
        assertEquals(HandleAction.None, g.onMove(-2f, 97f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp())
    }

    // 3
    @Test
    fun `scenario 03 horizontal move over slop but under step then up shows the panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(-40f, 100f)) // outward swipe, no vertical step
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp())
    }

    @Test
    fun `scenario 03 vertical move over slop but under step then up shows the panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(0f, 100f - 20f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp())
    }

    // 4
    @Test
    fun `scenario 04 moving up one step emits Step up count 1`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.Step(up = true, count = 1), g.onMove(0f, 100f - 24f))
        assertEquals(State.Stepping, g.state)
    }

    // 5
    @Test
    fun `scenario 05 moving down one step emits Step down count 1`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.Step(up = false, count = 1), g.onMove(0f, 100f + 24f))
        assertEquals(State.Stepping, g.state)
    }

    // 6
    @Test
    fun `scenario 06 move of 23 is None and release shows the panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(0f, 100f - 23f))
        assertEquals(HandleAction.ShowPanel, g.onUp())
    }

    // 7
    @Test
    fun `scenario 07 single large move emits count 3 and remainder carries to next step`() {
        val g = gestures()
        g.onDown(0f, 0f)
        assertEquals(HandleAction.Step(up = false, count = 3), g.onMove(0f, 75f))
        assertEquals(HandleAction.None, g.onMove(0f, 84f)) // 12 past anchor (72)
        assertEquals(HandleAction.Step(up = false, count = 1), g.onMove(0f, 96f)) // 24 past anchor
    }

    // 8
    @Test
    fun `scenario 08 reversal needs a full step from the anchor`() {
        val g = gestures()
        g.onDown(0f, 0f)
        assertEquals(HandleAction.Step(up = false, count = 1), g.onMove(0f, 30f)) // anchor 24
        assertEquals(HandleAction.None, g.onMove(0f, 20f))
        assertEquals(HandleAction.None, g.onMove(0f, 5f))
        assertEquals(HandleAction.Step(up = true, count = 1), g.onMove(0f, 0f)) // 24 above anchor
    }

    // 9
    @Test
    fun `scenario 09 release after stepping does not show the panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 70f)
        assertEquals(HandleAction.None, g.onUp())
        assertEquals(State.Idle, g.state)
    }

    // 10
    @Test
    fun `scenario 10 x changes while stepping have no effect`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.Step(up = true, count = 1), g.onMove(0f, 76f))
        assertEquals(HandleAction.None, g.onMove(500f, 76f))
        assertEquals(HandleAction.None, g.onMove(-500f, 70f))
        assertEquals(HandleAction.Step(up = true, count = 1), g.onMove(300f, 52f))
        assertEquals(State.Stepping, g.state)
    }

    // 11
    @Test
    fun `scenario 11 long press in Pressed begins reposition and moves report incremental dy`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
        assertEquals(State.Repositioning, g.state)
        assertEquals(HandleAction.Reposition(10f), g.onMove(0f, 110f))
        assertEquals(HandleAction.Reposition(5f), g.onMove(0f, 115f))
        assertEquals(HandleAction.Reposition(-3f), g.onMove(0f, 112f))
        assertEquals(HandleAction.None, g.onUp())
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `scenario 11 repositioning never steps even when total movement exceeds a step`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onLongPress()
        assertEquals(HandleAction.Reposition(30f), g.onMove(0f, 130f))
        assertEquals(HandleAction.Reposition(60f), g.onMove(200f, 190f))
        assertEquals(HandleAction.Reposition(-100f), g.onMove(0f, 90f))
        assertEquals(State.Repositioning, g.state)
        assertEquals(HandleAction.None, g.onUp())
    }

    // 12
    @Test
    fun `scenario 12 long press after slop exceeded is ignored and release shows the panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(20f, 100f))
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp())
    }

    // 13
    @Test
    fun `scenario 13 long press while stepping is ignored`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 70f)
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Stepping, g.state)
    }

    // 14
    @Test
    fun `scenario 14 stale long press in Idle does nothing`() {
        val g = gestures()
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Idle, g.state)

        g.onDown(0f, 0f)
        g.onUp()
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Idle, g.state)
    }

    // 15
    @Test
    fun `scenario 15 cancel in Pressed returns None goes Idle and later up is None`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onCancel())
        assertEquals(State.Idle, g.state)
        assertEquals(HandleAction.None, g.onUp())
    }

    @Test
    fun `scenario 15 cancel in Stepping returns None goes Idle and later up is None`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 70f)
        assertEquals(State.Stepping, g.state)
        assertEquals(HandleAction.None, g.onCancel())
        assertEquals(State.Idle, g.state)
        assertEquals(HandleAction.None, g.onUp())
    }

    @Test
    fun `scenario 15 cancel in Repositioning returns None goes Idle and later up is None`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onLongPress()
        assertEquals(State.Repositioning, g.state)
        assertEquals(HandleAction.None, g.onCancel())
        assertEquals(State.Idle, g.state)
        assertEquals(HandleAction.None, g.onUp())
    }

    // 16
    @Test
    fun `scenario 16 move and up before any down return None`() {
        val g = gestures()
        assertEquals(HandleAction.None, g.onMove(0f, 100f))
        assertEquals(HandleAction.None, g.onMove(0f, 300f))
        assertEquals(HandleAction.None, g.onUp())
        assertEquals(State.Idle, g.state)
    }

    // 17
    @Test
    fun `scenario 17 second down without up resets the anchor`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(0f, 80f))
        assertEquals(HandleAction.None, g.onDown(0f, 200f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.Step(up = true, count = 1), g.onMove(0f, 176f))
    }

    @Test
    fun `scenario 17 second down without up resets stepping state and slop`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(40f, 60f) // slop exceeded and Stepping
        assertEquals(State.Stepping, g.state)
        g.onDown(0f, 100f)
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    // 18
    @Test
    fun `scenario 18 reposition released with no movement shows no panel`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onLongPress()
        assertEquals(HandleAction.None, g.onUp())
        assertEquals(State.Idle, g.state)
    }

    // 19
    @Test
    fun `scenario 19 slop boundary x of exactly 8 is not exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(8f, 100f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary x of 8_01 is exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(8.01f, 100f)
        assertEquals(HandleAction.None, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary negative x of exactly 8 is not exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(-8f, 100f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary negative x of 8_01 is exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(-8.01f, 100f)
        assertEquals(HandleAction.None, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary y of exactly 8 is not exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 108f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary y of 8_01 is exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 108.01f)
        assertEquals(HandleAction.None, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary negative y of exactly 8 is not exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 92f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop boundary negative y of 8_01 is exceeded`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(0f, 91.99f)
        assertEquals(HandleAction.None, g.onLongPress())
    }

    @Test
    fun `scenario 19 slop is measured from the down point not the previous move`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onMove(5f, 100f)
        g.onMove(10f, 100f) // 5 from previous, 10 from down
        assertEquals(HandleAction.None, g.onLongPress())
    }

    // 20
    @Test
    fun `scenario 20 zero dy move is None`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.None, g.onMove(0f, 100f))
        assertEquals(State.Pressed, g.state)
    }

    @Test
    fun `scenario 20 zero dy move while repositioning is None`() {
        val g = gestures()
        g.onDown(0f, 100f)
        g.onLongPress()
        assertEquals(HandleAction.Reposition(4f), g.onMove(0f, 104f))
        assertEquals(HandleAction.None, g.onMove(50f, 104f))
        assertEquals(State.Repositioning, g.state)
    }

    // 21
    @Test
    fun `scenario 21 five steps in one move emit Step up count 5`() {
        val g = gestures()
        g.onDown(0f, 500f)
        assertEquals(HandleAction.Step(up = true, count = 5), g.onMove(0f, 500f - 5 * 24f))
    }

    // 22
    @Test
    fun `scenario 22 two gestures in a row are independent`() {
        val g = gestures()
        g.onDown(0f, 100f)
        assertEquals(HandleAction.Step(up = true, count = 1), g.onMove(30f, 70f))
        assertEquals(HandleAction.None, g.onUp())

        g.onDown(0f, 300f)
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.None, g.onMove(0f, 290f))
        assertEquals(HandleAction.ShowPanel, g.onUp())

        g.onDown(0f, 300f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
        assertEquals(HandleAction.Reposition(7f), g.onMove(0f, 307f))
        assertEquals(HandleAction.None, g.onUp())

        g.onDown(0f, 300f)
        assertEquals(HandleAction.Step(up = false, count = 1), g.onMove(0f, 324f))
        assertEquals(HandleAction.None, g.onUp())
        assertEquals(State.Idle, g.state)
    }
}
