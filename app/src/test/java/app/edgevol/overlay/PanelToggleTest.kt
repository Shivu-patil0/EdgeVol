package app.edgevol.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PanelToggle with a fake clock and a 1000 ms assumed-visible window. */
class PanelToggleTest {

    private val window = 1000L
    private var now = 10_000L
    private val toggle = PanelToggle(clock = { now }, assumedVisibleMs = window)

    /** A complete tap (DOWN then UP) at the current fake time. */
    private fun tap(): Boolean {
        toggle.onTouchDown()
        return toggle.onTap()
    }

    @Test
    fun `default window comes from HandleDefaults`() {
        var t = 0L
        val t1 = PanelToggle(clock = { t })
        assertTrue(t1.run { onTouchDown(); onTap() })
        t = HandleDefaults.PANEL_ASSUMED_VISIBLE_MS - 1
        assertFalse(t1.run { onTouchDown(); onTap() })
    }

    @Test
    fun `first tap shows the panel`() {
        assertTrue(tap())
    }

    @Test
    fun `second tap within the window is swallowed`() {
        assertTrue(tap())
        now += 500
        assertFalse(tap())
    }

    @Test
    fun `third tap shows again because the swallow reset the record`() {
        assertTrue(tap())
        now += 100
        assertFalse(tap())
        now += 100
        assertTrue(tap())
    }

    @Test
    fun `tap after the window shows the panel`() {
        assertTrue(tap())
        now += window + 1
        assertTrue(tap())
    }

    @Test
    fun `elapsed exactly equal to the window is not closing`() {
        assertTrue(tap())
        now += window
        assertTrue(tap())
    }

    @Test
    fun `elapsed one ms under the window is closing`() {
        assertTrue(tap())
        now += window - 1
        assertFalse(tap())
    }

    @Test
    fun `the window is measured at DOWN, not at the tap release`() {
        assertTrue(tap())
        now += window - 1
        toggle.onTouchDown()
        now += 5_000 // slow press; release long after the window ended
        assertFalse(toggle.onTap())
    }

    @Test
    fun `the window is measured from the last show, so a re-show restarts it`() {
        assertTrue(tap())
        now += window + 10
        assertTrue(tap()) // shown again at this time
        now += window - 1
        assertFalse(tap())
    }

    @Test
    fun `markShown followed by a DOWN within the window is swallowed`() {
        toggle.markShown()
        now += 300
        assertFalse(tap())
    }

    @Test
    fun `markShown followed by a DOWN after the window shows`() {
        toggle.markShown()
        now += window
        assertTrue(tap())
    }

    @Test
    fun `markShown restarts the window after a tap show`() {
        assertTrue(tap())
        now += 900
        toggle.markShown()
        now += 900 // 1800 after the tap, 900 after markShown
        assertFalse(tap())
    }

    @Test
    fun `a closing DOWN without a tap does not leak into the next gesture`() {
        assertTrue(tap())
        now += 200
        toggle.onTouchDown() // closing DOWN, then long press or cancel: no onTap
        now += 100
        // Next gesture is a fresh tap well inside the old window; the record was reset by the closing DOWN.
        assertTrue(tap())
    }

    @Test
    fun `a closing DOWN without a tap then a gesture outside any window shows`() {
        assertTrue(tap())
        now += 200
        toggle.onTouchDown()
        now += 5_000
        assertTrue(tap())
    }

    @Test
    fun `a non-closing DOWN without a tap leaves no swallow behind`() {
        toggle.onTouchDown() // e.g. long press: never reaches onTap
        now += 10
        assertTrue(tap())
    }

    @Test
    fun `onTap without a DOWN after a swallowed tap shows`() {
        assertTrue(tap())
        now += 100
        assertFalse(tap())
        assertTrue(toggle.onTap())
    }

    @Test
    fun `two consecutive taps are never both swallowed`() {
        assertTrue(tap())
        val results = (1..6).map {
            now += 50
            tap()
        }
        // show, swallow, show, swallow ... alternating while inside the window
        assertEquals(listOf(false, true, false, true, false, true), results)
    }

    @Test
    fun `a monotonically advancing clock with long gaps never swallows`() {
        assertTrue(tap())
        now += 1_000_000
        assertTrue(tap())
        now += 1_000_000
        assertTrue(tap())
    }
}
