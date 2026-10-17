package app.edgevol.overlay

import app.edgevol.data.HandleSide
import app.edgevol.overlay.HandleGestures.State
import org.junit.Assert.assertEquals
import org.junit.Test

/** HandleGestures with slop = 8f and tapMaxTravel = 48f (per axis from DOWN). */
class HandleGesturesTest {

    private val slop = 8f
    private val tapMax = 48f

    private fun gestures() = HandleGestures(slopPx = slop, tapMaxTravelPx = tapMax)

    private fun HandleGestures.pressAndHold(x: Float = 100f, y: Float = 200f): HandleGestures {
        onDown(x, y)
        assertEquals(HandleAction.BeginReposition, onLongPress())
        return this
    }

    @Test
    fun `new gesture state machine starts idle`() {
        assertEquals(State.Idle, gestures().state)
    }

    @Test
    fun `tap - down then up shows the panel and ends idle`() {
        val g = gestures()
        assertEquals(HandleAction.None, g.onDown(100f, 200f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp(100f, 200f))
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `tap - move within slop then up still shows the panel`() {
        val g = gestures()
        g.onDown(100f, 200f)
        assertEquals(HandleAction.None, g.onMove(104f, 203f))
        assertEquals(HandleAction.None, g.onMove(97f, 196f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp(97f, 196f))
    }

    @Test
    fun `tap - move beyond slop but within tap travel then up still shows the panel`() {
        val g = gestures()
        g.onDown(100f, 200f)
        assertEquals(HandleAction.None, g.onMove(120f, 230f))
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp(120f, 230f))
    }

    @Test
    fun `tap travel limit is exclusive - exactly tapMaxTravel stays Pressed`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(100f + tapMax, 200f)
        assertEquals(State.Pressed, g.state)
        assertEquals(HandleAction.ShowPanel, g.onUp(100f + tapMax, 200f))
    }

    @Test
    fun `moving more than tapMaxTravel on x before the hold ignores the touch`() {
        val g = gestures()
        g.onDown(100f, 200f)
        assertEquals(HandleAction.None, g.onMove(100f + tapMax + 1f, 200f))
        assertEquals(State.Ignored, g.state)
        assertEquals(HandleAction.None, g.onUp(150f, 200f))
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `moving more than tapMaxTravel on y before the hold ignores the touch`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(100f, 200f - tapMax - 1f)
        assertEquals(State.Ignored, g.state)
        assertEquals(HandleAction.None, g.onUp(100f, 100f))
    }

    @Test
    fun `ignored touch emits nothing on further moves and long press`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(100f, 400f)
        assertEquals(HandleAction.None, g.onMove(100f, 420f))
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Ignored, g.state)
    }

    @Test
    fun `long press in place begins repositioning`() {
        val g = gestures()
        g.onDown(100f, 200f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
        assertEquals(State.Repositioning, g.state)
    }

    @Test
    fun `long press after a small wobble within slop still begins repositioning`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(105f, 195f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    @Test
    fun `moving beyond slop on either axis disables the long press`() {
        val gx = gestures()
        gx.onDown(100f, 200f)
        gx.onMove(100f + slop + 1f, 200f)
        assertEquals(HandleAction.None, gx.onLongPress())
        assertEquals(State.Pressed, gx.state)
        assertEquals(HandleAction.ShowPanel, gx.onUp(109f, 200f))

        val gy = gestures()
        gy.onDown(100f, 200f)
        gy.onMove(100f, 200f - slop - 1f)
        assertEquals(HandleAction.None, gy.onLongPress())
        assertEquals(State.Pressed, gy.state)
    }

    @Test
    fun `slop is exclusive - exactly slop still allows the long press`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(100f + slop, 200f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
    }

    @Test
    fun `slop exceeded stays exceeded after returning to the down point`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(130f, 200f)
        g.onMove(100f, 200f)
        assertEquals(HandleAction.None, g.onLongPress())
    }

    @Test
    fun `long press is ignored when idle`() {
        val g = gestures()
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `long press is ignored when already repositioning`() {
        val g = gestures().pressAndHold()
        assertEquals(HandleAction.None, g.onLongPress())
        assertEquals(State.Repositioning, g.state)
    }

    @Test
    fun `repositioning emits incremental deltas from the previous event`() {
        val g = gestures().pressAndHold(100f, 200f)
        assertEquals(HandleAction.Reposition(5f, -3f), g.onMove(105f, 197f))
        assertEquals(HandleAction.Reposition(10f, 20f), g.onMove(115f, 217f))
        assertEquals(HandleAction.Reposition(-15f, 0f), g.onMove(100f, 217f))
    }

    @Test
    fun `first reposition delta is relative to the last pre-hold position`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(103f, 202f)
        g.onLongPress()
        assertEquals(HandleAction.Reposition(2f, 1f), g.onMove(105f, 203f))
    }

    @Test
    fun `repositioning move with no change emits nothing`() {
        val g = gestures().pressAndHold(100f, 200f)
        assertEquals(HandleAction.None, g.onMove(100f, 200f))
        g.onMove(110f, 210f)
        assertEquals(HandleAction.None, g.onMove(110f, 210f))
        assertEquals(State.Repositioning, g.state)
    }

    @Test
    fun `repositioning is not limited by tap travel`() {
        val g = gestures().pressAndHold(100f, 200f)
        assertEquals(HandleAction.Reposition(0f, 500f), g.onMove(100f, 700f))
        assertEquals(State.Repositioning, g.state)
    }

    @Test
    fun `hold and release in place ends reposition with moved false`() {
        val g = gestures().pressAndHold(100f, 200f)
        assertEquals(HandleAction.EndReposition(100f, 200f, false), g.onUp(100f, 200f))
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `hold with jitter within slop ends with moved false`() {
        val g = gestures().pressAndHold(100f, 200f)
        g.onMove(104f, 203f)
        g.onMove(98f, 197f)
        assertEquals(HandleAction.EndReposition(98f, 197f, false), g.onUp(98f, 197f))
    }

    @Test
    fun `distance exactly slop from the begin point is not moved, beyond is moved`() {
        val atSlop = gestures().pressAndHold(100f, 200f)
        atSlop.onMove(100f + slop, 200f)
        assertEquals(HandleAction.EndReposition(108f, 200f, false), atSlop.onUp(108f, 200f))

        val beyond = gestures().pressAndHold(100f, 200f)
        beyond.onMove(100f + slop + 1f, 200f)
        assertEquals(HandleAction.EndReposition(109f, 200f, true), beyond.onUp(109f, 200f))
    }

    @Test
    fun `moved uses euclidean distance, not per axis`() {
        // 6 on each axis is within slop per axis, but sqrt(72) = 8.49 > 8.
        val g = gestures().pressAndHold(100f, 200f)
        g.onMove(106f, 206f)
        assertEquals(HandleAction.EndReposition(106f, 206f, true), g.onUp(106f, 206f))
    }

    @Test
    fun `moved stays true after returning to the begin point`() {
        val g = gestures().pressAndHold(100f, 200f)
        g.onMove(160f, 200f)
        g.onMove(100f, 200f)
        assertEquals(HandleAction.EndReposition(100f, 200f, true), g.onUp(100f, 200f))
    }

    @Test
    fun `end reposition reports the raw finger position given to onUp`() {
        val g = gestures().pressAndHold(100f, 200f)
        g.onMove(300f, 500f)
        assertEquals(HandleAction.EndReposition(320f, 510f, true), g.onUp(320f, 510f))
    }

    @Test
    fun `a tap never repositions`() {
        val g = gestures()
        val actions = listOf(
            g.onDown(100f, 200f),
            g.onMove(102f, 201f),
            g.onUp(102f, 201f),
        )
        assertEquals(
            listOf(HandleAction.None, HandleAction.None, HandleAction.ShowPanel),
            actions,
        )
    }

    @Test
    fun `cancel while pressed goes idle with no action`() {
        val g = gestures()
        g.onDown(100f, 200f)
        assertEquals(HandleAction.None, g.onCancel())
        assertEquals(State.Idle, g.state)
        assertEquals(HandleAction.None, g.onUp(100f, 200f))
    }

    @Test
    fun `cancel while repositioning goes idle with no action`() {
        val g = gestures().pressAndHold()
        g.onMove(150f, 250f)
        assertEquals(HandleAction.None, g.onCancel())
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `cancel while ignored goes idle with no action`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(300f, 200f)
        assertEquals(HandleAction.None, g.onCancel())
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `move and up while idle do nothing`() {
        val g = gestures()
        assertEquals(HandleAction.None, g.onMove(1f, 1f))
        assertEquals(HandleAction.None, g.onUp(1f, 1f))
        assertEquals(State.Idle, g.state)
    }

    @Test
    fun `a new down after a reposition resets slop and moved`() {
        val g = gestures().pressAndHold(100f, 200f)
        g.onMove(300f, 200f)
        g.onUp(300f, 200f)

        g.onDown(50f, 60f)
        assertEquals(HandleAction.BeginReposition, g.onLongPress())
        assertEquals(HandleAction.EndReposition(50f, 60f, false), g.onUp(50f, 60f))
    }

    @Test
    fun `a new down after an ignored touch is a fresh tap`() {
        val g = gestures()
        g.onDown(100f, 200f)
        g.onMove(100f, 400f)
        g.onUp(100f, 400f)

        g.onDown(10f, 10f)
        assertEquals(HandleAction.ShowPanel, g.onUp(10f, 10f))
    }

    // HandleSnap

    @Test
    fun `sideFor left of centre is LEFT`() {
        assertEquals(HandleSide.LEFT, HandleSnap.sideFor(0f, 1080))
        assertEquals(HandleSide.LEFT, HandleSnap.sideFor(539.9f, 1080))
    }

    @Test
    fun `sideFor exactly half and beyond is RIGHT`() {
        assertEquals(HandleSide.RIGHT, HandleSnap.sideFor(540f, 1080))
        assertEquals(HandleSide.RIGHT, HandleSnap.sideFor(1080f, 1080))
    }

    @Test
    fun `sideFor odd width uses the true half`() {
        assertEquals(HandleSide.LEFT, HandleSnap.sideFor(540f, 1081))
        assertEquals(HandleSide.RIGHT, HandleSnap.sideFor(540.5f, 1081))
    }

    @Test
    fun `positionPct is the rounded percent of the height`() {
        assertEquals(50, HandleSnap.positionPct(1000f, 2000))
        assertEquals(40, HandleSnap.positionPct(800f, 2000))
        assertEquals(33, HandleSnap.positionPct(660f, 2000))
        assertEquals(34, HandleSnap.positionPct(675f, 2000))
    }

    @Test
    fun `positionPct is coerced to 10 through 90`() {
        assertEquals(10, HandleSnap.positionPct(0f, 2000))
        assertEquals(10, HandleSnap.positionPct(-500f, 2000))
        assertEquals(10, HandleSnap.positionPct(199f, 2000))
        assertEquals(10, HandleSnap.positionPct(200f, 2000))
        assertEquals(90, HandleSnap.positionPct(1800f, 2000))
        assertEquals(90, HandleSnap.positionPct(2000f, 2000))
        assertEquals(90, HandleSnap.positionPct(5000f, 2000))
    }

    @Test
    fun `positionPct falls back to the default 40 for a non-positive height`() {
        assertEquals(40, HandleSnap.positionPct(500f, 0))
        assertEquals(40, HandleSnap.positionPct(500f, -1))
    }

    // Rotation: POCO X2 is 1080 x 2400 px; the 72 dp handle is 198 px tall.

    @Test
    fun `orientedSize swaps a stale portrait reading when the screen is landscape`() {
        assertEquals(ScreenSize(2400, 1080), HandleSnap.orientedSize(1080, 2400, landscape = true))
        assertEquals(ScreenSize(2400, 1080), HandleSnap.orientedSize(2400, 1080, landscape = true))
    }

    @Test
    fun `orientedSize swaps a stale landscape reading when the screen is portrait`() {
        assertEquals(ScreenSize(1080, 2400), HandleSnap.orientedSize(2400, 1080, landscape = false))
        assertEquals(ScreenSize(1080, 2400), HandleSnap.orientedSize(1080, 2400, landscape = false))
    }

    @Test
    fun `orientedSize keeps the reading when the orientation is unknown`() {
        assertEquals(ScreenSize(1500, 1400), HandleSnap.orientedSize(1500, 1400, landscape = null))
    }

    @Test
    fun `the same percent sits at the same relative height in portrait and landscape`() {
        for (pct in listOf(10f, 25f, 40f, 75f, 90f)) {
            val portraitCentre = HandleSnap.windowTopPx(pct, 2400, 198) + 99
            val landscapeCentre = HandleSnap.windowTopPx(pct, 1080, 198) + 99
            assertEquals(pct, portraitCentre / 2400f * 100f, 0.5f)
            assertEquals(pct, landscapeCentre / 1080f * 100f, 0.5f)
        }
    }

    @Test
    fun `a handle at the top of a portrait screen stays at the top in landscape`() {
        assertEquals(141, HandleSnap.windowTopPx(10f, 2400, 198))
        assertEquals(9, HandleSnap.windowTopPx(10f, 1080, 198))
    }

    @Test
    fun `windowTopPx keeps the whole handle on screen when it is taller than the percent margin`() {
        assertEquals(0, HandleSnap.windowTopPx(10f, 300, 198))
        assertEquals(102, HandleSnap.windowTopPx(90f, 300, 198))
    }

    @Test
    fun `windowTopPx is zero for a non-positive height`() {
        assertEquals(0, HandleSnap.windowTopPx(40f, 0, 198))
    }
}
