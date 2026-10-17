package app.edgevol.audio

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    fun `showPanel never changes the volume`() {
        val v = volume()
        v.showPanel()
        v.showPanel()

        assertEquals(2, calls.size)
        assertEquals(setOf(AudioManager.ADJUST_SAME), calls.map { it.direction }.toSet())
    }

    @Test
    fun `showPanel propagates exceptions from the adjust call`() {
        val boom = IllegalStateException("boom")
        try {
            volume { _, _, _ -> throw boom }.showPanel()
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
