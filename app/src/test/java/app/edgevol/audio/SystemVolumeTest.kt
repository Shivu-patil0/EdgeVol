package app.edgevol.audio

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SystemVolumeTest {

    private data class Call(val direction: Int, val streamType: Int, val flags: Int)

    private val calls = mutableListOf<Call>()

    private fun volume(onAdjust: (Int, Int, Int) -> Unit = { _, _, _ -> }) =
        SystemVolume { direction, streamType, flags ->
            calls += Call(direction, streamType, flags)
            onAdjust(direction, streamType, flags)
        }

    @Test
    fun `showPanel adjusts exactly once with ADJUST_SAME and default stream and FLAG_SHOW_UI`() {
        volume().showPanel()

        assertEquals(
            listOf(Call(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)),
            calls,
        )
    }

    @Test
    fun `step up raises with default stream and FLAG_SHOW_UI and returns true`() {
        val result = volume().step(true)

        assertTrue(result)
        assertEquals(
            listOf(Call(AudioManager.ADJUST_RAISE, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)),
            calls,
        )
    }

    @Test
    fun `step down lowers with default stream and FLAG_SHOW_UI and returns true`() {
        val result = volume().step(false)

        assertTrue(result)
        assertEquals(
            listOf(Call(AudioManager.ADJUST_LOWER, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)),
            calls,
        )
    }

    @Test
    fun `step returns false and does not rethrow on SecurityException`() {
        val v = volume { _, _, _ -> throw SecurityException("DND") }

        assertFalse(v.step(true))
        assertFalse(v.step(false))
        assertEquals(2, calls.size)
    }

    @Test
    fun `step propagates IllegalStateException`() {
        val boom = IllegalStateException("boom")
        val v = volume { _, _, _ -> throw boom }

        try {
            v.step(true)
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertSame(boom, e)
        }
    }

    @Test
    fun `AudioManager constants have the documented values`() {
        assertEquals(1, AudioManager.ADJUST_RAISE)
        assertEquals(-1, AudioManager.ADJUST_LOWER)
        assertEquals(0, AudioManager.ADJUST_SAME)
        assertEquals(Int.MIN_VALUE, AudioManager.USE_DEFAULT_STREAM_TYPE)
        assertEquals(1, AudioManager.FLAG_SHOW_UI)
    }
}
