package app.edgevol.overlay

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.edgevol.R
import app.edgevol.audio.SystemVolume
import app.edgevol.data.HandleSide
import app.edgevol.data.HandleSize
import app.edgevol.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl

/**
 * The real handle window and view on simulated Android 8.0 (minSdk), 11 (the test phone) and 15.
 * The 360 x 640 px screen at density 1 makes window sizes equal their dp values.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [26, 30, 35], qualifiers = "w360dp-h640dp-port-mdpi")
class OverlayControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val panelCalls = mutableListOf<Triple<Int, Int, Int>>()
    private val saved = mutableListOf<Prefs>()

    private fun controller(initial: Prefs = Prefs()) = OverlayController(
        context = context,
        volume = SystemVolume { direction, stream, flags -> panelCalls += Triple(direction, stream, flags) },
        initial = initial,
        onPlacementChanged = { saved += it },
    )

    private fun handles(): List<EdgeHandleView> =
        Shadow.extract<ShadowWindowManagerImpl>(windowManager).views.filterIsInstance<EdgeHandleView>()

    private fun handle(): EdgeHandleView = handles().single()

    private fun params(): WindowManager.LayoutParams = handle().layoutParams as WindowManager.LayoutParams

    private fun actionIds(view: View): List<Int> =
        view.createAccessibilityNodeInfo().actionList.map { it.id }

    private fun actionLabel(view: View, id: Int): CharSequence? =
        view.createAccessibilityNodeInfo().actionList.firstOrNull { it.id == id }?.label

    @Test
    fun `apply shows exactly one non-focusable accessibility overlay at the saved place`() {
        assertTrue(controller().apply(Prefs()))

        val params = params()
        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertEquals(HandleDefaults.WINDOW_TITLE, params.title)
        assertEquals(Gravity.TOP or Gravity.RIGHT, params.gravity)
        assertEquals(HandleDefaults.TOUCH_WIDTH_DP.toInt(), params.width)
        assertEquals(HandleSize.M.heightDp, params.height)
        assertEquals(HandleSnap.windowTopPx(40f, 640, HandleSize.M.heightDp), params.y)
    }

    @Test
    fun `applying the same prefs again keeps the same single window`() {
        val controller = controller()
        controller.apply(Prefs())
        val first = handle()

        controller.apply(Prefs())

        assertSame(first, handle())
    }

    @Test
    fun `hidden removes the window and showing again adds one`() {
        val controller = controller()
        controller.apply(Prefs())
        val first = handle()

        assertTrue(controller.apply(Prefs(handleEnabled = false)))
        assertFalse(first.isAttachedToWindow)
        assertTrue(handles().isEmpty())

        controller.apply(Prefs())
        assertEquals(1, handles().size)
    }

    @Test
    fun `settings change the window live`() {
        val controller = controller()
        controller.apply(Prefs())

        controller.apply(Prefs(side = HandleSide.LEFT, positionPct = 70, size = HandleSize.L))

        val params = params()
        assertEquals(Gravity.TOP or Gravity.LEFT, params.gravity)
        assertEquals(HandleSize.L.heightDp, params.height)
        assertEquals(HandleSnap.windowTopPx(70f, 640, HandleSize.L.heightDp), params.y)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `rotation keeps the relative height and returns to the same spot`() {
        val controller = controller()
        controller.apply(Prefs(positionPct = 20))
        val portraitY = params().y

        RuntimeEnvironment.setQualifiers("w640dp-h360dp-land")
        controller.relayout(context.resources.configuration.also { assertEquals(Configuration.ORIENTATION_LANDSCAPE, it.orientation) })
        assertEquals(HandleSnap.windowTopPx(20f, 360, HandleSize.M.heightDp), params().y)

        RuntimeEnvironment.setQualifiers("w360dp-h640dp-port")
        controller.relayout(context.resources.configuration)
        assertEquals(portraitY, params().y)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `TalkBack double-tap shows the system volume panel`() {
        controller().apply(Prefs())

        assertTrue(handle().performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))

        assertEquals(
            listOf(Triple(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI)),
            panelCalls,
        )
    }

    @Test
    fun `node info names the click and offers the three moves`() {
        controller().apply(Prefs(side = HandleSide.RIGHT, positionPct = 40))
        val view = handle()

        assertEquals(context.getString(R.string.handle_content_description), view.contentDescription)
        assertEquals(context.getString(R.string.handle_action_show_panel), actionLabel(view, AccessibilityNodeInfo.ACTION_CLICK))
        assertEquals(context.getString(R.string.handle_action_move_left), actionLabel(view, R.id.handle_action_other_side))
        assertEquals(context.getString(R.string.handle_action_move_up), actionLabel(view, R.id.handle_action_up))
        assertEquals(context.getString(R.string.handle_action_move_down), actionLabel(view, R.id.handle_action_down))
    }

    @Test
    fun `move to the other side moves the window, saves, and relabels the action`() {
        controller().apply(Prefs(side = HandleSide.RIGHT, positionPct = 40))
        val view = handle()
        val y = params().y

        assertTrue(view.performAccessibilityAction(R.id.handle_action_other_side, null))

        assertEquals(Gravity.TOP or Gravity.LEFT, params().gravity)
        assertEquals(y, params().y)
        assertEquals(listOf(HandleSide.LEFT to 40), saved.map { it.side to it.positionPct })
        assertEquals(context.getString(R.string.handle_action_move_right), actionLabel(view, R.id.handle_action_other_side))
    }

    @Test
    fun `move up and down step ten points and save each step`() {
        controller().apply(Prefs(positionPct = 40))
        val view = handle()

        assertTrue(view.performAccessibilityAction(R.id.handle_action_up, null))
        assertEquals(HandleSnap.windowTopPx(30f, 640, HandleSize.M.heightDp), params().y)
        assertTrue(view.performAccessibilityAction(R.id.handle_action_down, null))
        assertTrue(view.performAccessibilityAction(R.id.handle_action_down, null))

        assertEquals(listOf(30, 40, 50), saved.map { it.positionPct })
        assertEquals(HandleSnap.windowTopPx(50f, 640, HandleSize.M.heightDp), params().y)
    }

    @Test
    fun `no move up at the top and no move down at the bottom`() {
        val controller = controller()
        controller.apply(Prefs(positionPct = 10))
        val view = handle()
        assertFalse(R.id.handle_action_up in actionIds(view))
        assertTrue(R.id.handle_action_down in actionIds(view))
        val y = params().y

        assertFalse(view.performAccessibilityAction(R.id.handle_action_up, null))
        assertEquals(y, params().y)

        controller.apply(Prefs(positionPct = 90))
        assertFalse(R.id.handle_action_down in actionIds(view))
        assertFalse(view.performAccessibilityAction(R.id.handle_action_down, null))
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `a removed handle ignores moves`() {
        val controller = controller()
        controller.apply(Prefs())
        val view = handle()
        controller.apply(Prefs(handleEnabled = false))

        assertFalse(view.performAccessibilityAction(R.id.handle_action_other_side, null))
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `the custom action ids are distinct app ids`() {
        val ids = listOf(R.id.handle_action_other_side, R.id.handle_action_up, R.id.handle_action_down)
        assertEquals(3, ids.toSet().size)
        ids.forEach { assertNotEquals(AccessibilityNodeInfo.ACTION_CLICK, it) }
        assertNotNull(context.resources.getResourceName(ids.first()))
    }
}
