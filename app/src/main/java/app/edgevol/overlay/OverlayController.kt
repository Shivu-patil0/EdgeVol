package app.edgevol.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import app.edgevol.audio.SystemVolume
import app.edgevol.data.HandleSide
import app.edgevol.data.HandleSize
import app.edgevol.data.Prefs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Owns the one and only EdgeVol window: the edge handle. All methods must be called on the main
 * thread (accessibility service callbacks already are). [context] must be the accessibility service
 * itself: its [WindowManager] is what permits `TYPE_ACCESSIBILITY_OVERLAY`.
 *
 * [onPlacementChanged] is called only with the snapped placement after a completed drag.
 */
internal class OverlayController(
    private val context: Context,
    private val volume: SystemVolume,
    initial: Prefs,
    private val onPlacementChanged: (Prefs) -> Unit,
) {
    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    private var side: HandleSide = initial.side
    private var positionPercent: Float = initial.positionPct.toFloat()
    private var size: HandleSize = initial.size
    private var opacityPct: Int = initial.opacityPct
    private var landscape: Boolean? = isLandscape(context.resources.configuration)

    private var view: EdgeHandleView? = null
    private var params: WindowManager.LayoutParams? = null

    // Unclamped window top-left while dragging, so the window re-syncs with the finger after
    // an excursion past the screen edge.
    private var dragging = false
    private var dragX = 0f
    private var dragY = 0f

    private val repositionListener = object : RepositionListener {
        override fun onBegin() = beginReposition()

        override fun onMove(dxPx: Float, dyPx: Float) = repositionBy(dxPx, dyPx)

        override fun onEnd(rawX: Float, rawY: Float, moved: Boolean) = endReposition(rawX, moved)

        override fun onCancel() = cancelReposition()
    }

    /**
     * Applies saved settings (start-up, or a change on the setup screen): shows, updates or removes
     * the handle. Returns false only if the handle should be visible but could not be added.
     */
    fun apply(prefs: Prefs): Boolean {
        side = prefs.side
        positionPercent = prefs.positionPct.toFloat()
        size = prefs.size
        opacityPct = prefs.opacityPct
        if (prefs.handleHidden) {
            removeHandle()
            return true
        }
        val handleView = view ?: return showHandle()
        // Mid-drag the finger owns the window; the drag's own snap saves the final placement.
        if (dragging) return true
        handleView.setIdleOpacity(opacityPct / 100f)
        applyPlacement()
        return true
    }

    /** Adds the handle window. Idempotent: an existing handle is removed first. Returns true if added. */
    fun showHandle(): Boolean {
        removeHandle()
        val handleView = EdgeHandleView(context, volume, side, opacityPct / 100f, repositionListener)
        val layoutParams = WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            title = HandleDefaults.WINDOW_TITLE
        }
        applyGeometry(layoutParams)
        try {
            windowManager.addView(handleView, layoutParams)
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "handle add failed: ${e.javaClass.simpleName}")
            return false
        } catch (e: WindowManager.InvalidDisplayException) {
            Log.w(TAG, "handle add failed: ${e.javaClass.simpleName}")
            return false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "handle add failed: ${e.javaClass.simpleName}")
            return false
        }
        view = handleView
        params = layoutParams
        Log.d(TAG, "handle added: side=$side pct=${positionPercent.roundToInt()} size=$size opacity=$opacityPct")
        return true
    }

    /** Removes the handle window if there is one. Never throws. */
    fun removeHandle() {
        val handleView = view ?: return
        view = null
        params = null
        dragging = false
        try {
            windowManager.removeViewImmediate(handleView)
            Log.d(TAG, "handle removed")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "handle remove skipped: not attached")
        }
    }

    /** Recomputes size and position for [newConfig] (e.g. after rotation). */
    fun relayout(newConfig: Configuration) {
        landscape = isLandscape(newConfig)
        dragging = false
        applyPlacement()
    }

    private fun beginReposition() {
        val layoutParams = params ?: return
        val screen = screenSize()
        if (screen.widthPx <= 0 || screen.heightPx <= 0) return
        // Switch to absolute TOP|LEFT coordinates at the handle's current top-left.
        dragX = if (side == HandleSide.RIGHT) (screen.widthPx - layoutParams.width).toFloat() else 0f
        dragY = layoutParams.y.toFloat()
        dragging = true
        layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        layoutParams.x = dragX.roundToInt()
        updateLayout(layoutParams)
    }

    private fun repositionBy(dxPx: Float, dyPx: Float) {
        if (!dragging) return
        val layoutParams = params ?: return
        val screen = screenSize()
        dragX += dxPx
        dragY += dyPx
        layoutParams.x = dragX.roundToInt().coerceIn(0, max(0, screen.widthPx - layoutParams.width))
        layoutParams.y = dragY.roundToInt().coerceIn(0, max(0, screen.heightPx - layoutParams.height))
        updateLayout(layoutParams)
    }

    private fun endReposition(fingerRawX: Float, moved: Boolean) {
        if (!dragging) return
        val layoutParams = params ?: return
        dragging = false
        if (!moved) {
            applyPlacement()
            return
        }
        val screen = screenSize()
        side = HandleSnap.sideFor(fingerRawX, screen.widthPx)
        val pct = HandleSnap.positionPct(layoutParams.y + layoutParams.height / 2f, screen.heightPx)
        positionPercent = pct.toFloat()
        applyPlacement()
        Log.d(TAG, "snap: side=$side pct=$pct")
        onPlacementChanged(Prefs(side = side, positionPct = pct))
    }

    private fun cancelReposition() {
        if (!dragging) return
        dragging = false
        applyPlacement()
    }

    /** Applies stored side and percent to the window and tells the view. */
    private fun applyPlacement() {
        val handleView = view ?: return
        val layoutParams = params ?: return
        applyGeometry(layoutParams)
        if (!updateLayout(layoutParams)) return
        handleView.setSide(side)
        handleView.updateGestureExclusion()
    }

    private fun updateLayout(layoutParams: WindowManager.LayoutParams): Boolean {
        val handleView = view ?: return false
        return try {
            windowManager.updateViewLayout(handleView, layoutParams)
            true
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "handle relayout skipped: not attached")
            false
        }
    }

    private fun applyGeometry(layoutParams: WindowManager.LayoutParams) {
        val density = context.resources.displayMetrics.density
        val widthPx = (HandleDefaults.TOUCH_WIDTH_DP * density).roundToInt()
        val heightPx = (size.heightDp * density).roundToInt()
        layoutParams.width = widthPx
        layoutParams.height = heightPx
        layoutParams.gravity = Gravity.TOP or (if (side == HandleSide.RIGHT) Gravity.RIGHT else Gravity.LEFT)
        layoutParams.x = 0
        // positionPercent is never rewritten here, so rotating back and forth returns to the same spot.
        layoutParams.y = HandleSnap.windowTopPx(positionPercent, screenSize().heightPx, heightPx)
    }

    private fun screenSize(): ScreenSize {
        val raw = if (Build.VERSION.SDK_INT >= 30) {
            windowManager.currentWindowMetrics.bounds.let { ScreenSize(it.width(), it.height()) }
        } else {
            realMetricsPreR().let { ScreenSize(it.widthPixels, it.heightPixels) }
        }
        return HandleSnap.orientedSize(raw.widthPx, raw.heightPx, landscape)
    }

    @Suppress("DEPRECATION")
    private fun realMetricsPreR(): DisplayMetrics {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private companion object {
        const val TAG = "EdgeVol"

        fun isLandscape(config: Configuration): Boolean? = when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> true
            Configuration.ORIENTATION_PORTRAIT -> false
            else -> null
        }
    }
}
