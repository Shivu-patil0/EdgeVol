package app.edgevol.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.edgevol.R
import app.edgevol.audio.SystemVolume
import kotlin.math.roundToInt

/**
 * The edge handle: a plain [View] that draws a thin pill on the screen edge and turns touches into
 * volume steps, a panel tap, or a reposition drag. Gesture maths lives in [HandleGestures].
 *
 * [onRepositionBy] receives incremental vertical movement in pixels while repositioning.
 */
internal class EdgeHandleView(
    context: Context,
    private val volume: SystemVolume,
    private val side: HandleSide,
    private val onRepositionBy: (dyPx: Float) -> Unit,
) : View(context) {
    private val density = context.resources.displayMetrics.density
    private val gestures = HandleGestures(
        stepPx = HandleDefaults.STEP_DP * density,
        slopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
    )
    private val pillWidthPx = HandleDefaults.PILL_WIDTH_DP * density
    private val pillRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = HandleDefaults.COLOR_INDIGO }
    private val exclusionRect = Rect()

    private var trackedPointerId = INVALID_POINTER_ID
    private var active = false

    private val longPressRunnable = Runnable { handle(gestures.onLongPress()) }

    init {
        contentDescription = context.getString(R.string.handle_content_description)
        isClickable = true
        isSoundEffectsEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        paint.alpha = if (active) 255 else (HandleDefaults.OPACITY * 255f).roundToInt()
        val w = width.toFloat()
        val left = if (side == HandleSide.Right) w - pillWidthPx else 0f
        pillRect.set(left, 0f, left + pillWidthPx, height.toFloat())
        val radius = pillWidthPx / 2f
        canvas.drawRoundRect(pillRect, radius, radius, paint)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateGestureExclusion()
    }

    /** Registers the whole view as a system-gesture exclusion zone (API 29+). */
    fun updateGestureExclusion() {
        if (Build.VERSION.SDK_INT >= 29) {
            exclusionRect.set(0, 0, width, height)
            systemGestureExclusionRects = listOf(Rect(exclusionRect))
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        cancelGesture()
        super.onDetachedFromWindow()
    }

    override fun performClick(): Boolean {
        super.performClick()
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        Log.d(TAG, "showPanel")
        volume.showPanel()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trackedPointerId = event.getPointerId(0)
                setActive(true)
                handle(gestures.onDown(rawX(event, 0), rawY(event, 0)))
                removeCallbacks(longPressRunnable)
                postDelayed(longPressRunnable, HandleDefaults.LONG_PRESS_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(trackedPointerId)
                if (index >= 0) handle(gestures.onMove(rawX(event, index), rawY(event, index)))
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // Other pointers are ignored; if the tracked pointer lifts, the gesture ends.
                if (event.getPointerId(event.actionIndex) == trackedPointerId) finishGesture()
            }
            MotionEvent.ACTION_UP -> finishGesture()
            MotionEvent.ACTION_CANCEL -> cancelGesture()
        }
        return true
    }

    private fun finishGesture() {
        removeCallbacks(longPressRunnable)
        trackedPointerId = INVALID_POINTER_ID
        val action = gestures.onUp()
        setActive(false)
        handle(action)
    }

    private fun cancelGesture() {
        removeCallbacks(longPressRunnable)
        trackedPointerId = INVALID_POINTER_ID
        gestures.onCancel()
        setActive(false)
    }

    private fun setActive(value: Boolean) {
        if (active != value) {
            active = value
            invalidate()
        }
    }

    private fun handle(action: HandleAction) {
        when (action) {
            HandleAction.None -> Unit
            HandleAction.ShowPanel -> performClick()
            is HandleAction.Step -> performSteps(action)
            HandleAction.BeginReposition -> {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                setActive(true)
            }
            is HandleAction.Reposition -> onRepositionBy(action.dyPx)
        }
    }

    private fun performSteps(action: HandleAction.Step) {
        var refused = false
        for (i in 0 until action.count) {
            Log.d(TAG, if (action.up) "step up" else "step down")
            if (!volume.step(action.up)) {
                Log.d(TAG, "step refused")
                refused = true
                break
            }
        }
        performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 34) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            },
        )
        if (refused) {
            Log.d(TAG, "showPanel")
            volume.showPanel()
        }
    }

    // Raw screen coordinates: the window moves under the finger while repositioning.
    // MotionEvent.getRawX(int) is API 29; before that derive it from pointer 0's raw position.
    private fun rawX(event: MotionEvent, index: Int): Float =
        if (Build.VERSION.SDK_INT >= 29) {
            event.getRawX(index)
        } else {
            event.rawX + (event.getX(index) - event.getX(0))
        }

    private fun rawY(event: MotionEvent, index: Int): Float =
        if (Build.VERSION.SDK_INT >= 29) {
            event.getRawY(index)
        } else {
            event.rawY + (event.getY(index) - event.getY(0))
        }

    private companion object {
        const val TAG = "EdgeVol"
        const val INVALID_POINTER_ID = -1
    }
}
