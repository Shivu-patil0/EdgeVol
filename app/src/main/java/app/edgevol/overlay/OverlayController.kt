package app.edgevol.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import app.edgevol.audio.SystemVolume
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Owns the one and only EdgeVol window: the edge handle. All methods must be called on the main
 * thread (accessibility service callbacks already are). [context] must be the accessibility service
 * itself: its [WindowManager] is what permits `TYPE_ACCESSIBILITY_OVERLAY`.
 */
internal class OverlayController(
    private val context: Context,
    private val volume: SystemVolume,
) {
    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    private val side: HandleSide = HandleDefaults.SIDE

    private var view: EdgeHandleView? = null
    private var params: WindowManager.LayoutParams? = null

    // In memory only (M2): lost when the service restarts.
    private var positionPercent: Float = HandleDefaults.POSITION_PERCENT

    /** Adds the handle window. Idempotent: an existing handle is removed first. */
    fun showHandle() {
        removeHandle()
        val handleView = EdgeHandleView(context, volume, side, ::repositionBy)
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
            gravity = Gravity.TOP or (if (side == HandleSide.Right) Gravity.RIGHT else Gravity.LEFT)
            x = 0
        }
        applyGeometry(layoutParams)
        try {
            windowManager.addView(handleView, layoutParams)
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "handle add failed: ${e.javaClass.simpleName}")
            return
        } catch (e: WindowManager.InvalidDisplayException) {
            Log.w(TAG, "handle add failed: ${e.javaClass.simpleName}")
            return
        } catch (e: IllegalStateException) {
            Log.w(TAG, "handle add failed: ${e.javaClass.simpleName}")
            return
        }
        view = handleView
        params = layoutParams
        Log.d(TAG, "handle added")
    }

    /** Removes the handle window if there is one. Never throws. */
    fun removeHandle() {
        val handleView = view ?: return
        view = null
        params = null
        try {
            windowManager.removeViewImmediate(handleView)
            Log.d(TAG, "handle removed")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "handle remove skipped: not attached")
        }
    }

    /** Recomputes size and position from the current screen (e.g. after rotation). */
    fun relayout() {
        val handleView = view ?: return
        val layoutParams = params ?: return
        applyGeometry(layoutParams)
        try {
            windowManager.updateViewLayout(handleView, layoutParams)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "handle relayout skipped: not attached")
            return
        }
        handleView.updateGestureExclusion()
    }

    private fun repositionBy(dyPx: Float) {
        val height = screenHeightPx()
        if (height <= 0) return
        positionPercent = (positionPercent + dyPx / height * 100f)
            .coerceIn(HandleDefaults.POSITION_MIN_PERCENT, HandleDefaults.POSITION_MAX_PERCENT)
        relayout()
    }

    private fun applyGeometry(layoutParams: WindowManager.LayoutParams) {
        val density = context.resources.displayMetrics.density
        val widthPx = (HandleDefaults.TOUCH_WIDTH_DP * density).roundToInt()
        val heightPx = (HandleDefaults.HEIGHT_DP * density).roundToInt()
        val screenHeight = screenHeightPx()
        layoutParams.width = widthPx
        layoutParams.height = heightPx
        if (screenHeight <= 0) {
            layoutParams.y = 0
            return
        }
        val centre = (positionPercent / 100f * screenHeight).roundToInt()
        // Percent positions the handle's centre. The whole handle must stay on screen AND its centre
        // must stay within the percent range, so the stored percent always matches the real position.
        val minPercentCentre = (HandleDefaults.POSITION_MIN_PERCENT / 100f * screenHeight).roundToInt()
        val maxPercentCentre = (HandleDefaults.POSITION_MAX_PERCENT / 100f * screenHeight).roundToInt()
        val minY = max(0, minPercentCentre - heightPx / 2)
        val maxY = max(minY, minOf(screenHeight - heightPx, maxPercentCentre - heightPx / 2))
        val y = (centre - heightPx / 2).coerceIn(minY, maxY)
        layoutParams.y = y
        // Write the effective position back so stored percent and real position agree.
        positionPercent = ((y + heightPx / 2f) / screenHeight * 100f)
            .coerceIn(HandleDefaults.POSITION_MIN_PERCENT, HandleDefaults.POSITION_MAX_PERCENT)
    }

    private fun screenHeightPx(): Int =
        if (Build.VERSION.SDK_INT >= 30) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            realHeightPreR()
        }

    @Suppress("DEPRECATION")
    private fun realHeightPreR(): Int {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.heightPixels
    }

    private companion object {
        const val TAG = "EdgeVol"
    }
}
