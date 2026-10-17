package app.edgevol.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.edgevol.R
import app.edgevol.audio.SystemVolume
import app.edgevol.data.HandleSide
import kotlin.math.roundToInt

/** Receives the hold-and-drag reposition gesture; coordinates are raw screen pixels. */
internal interface RepositionListener {
    fun onBegin()

    fun onMove(dxPx: Float, dyPx: Float)

    /** [moved] is false when the user held and released without dragging. */
    fun onEnd(rawX: Float, rawY: Float, moved: Boolean)

    /** The gesture was cancelled by the system; restore the pre-drag placement. */
    fun onCancel()
}

/**
 * The edge handle: a plain [View] that draws a thin pill on the screen edge. A tap toggles the
 * system volume panel; holding then dragging repositions the handle. Gesture maths lives in
 * [HandleGestures].
 */
internal class EdgeHandleView(
    context: Context,
    private val volume: SystemVolume,
    private var side: HandleSide,
    private val repositionListener: RepositionListener,
) : View(context) {
    private val density = context.resources.displayMetrics.density
    private val gestures = HandleGestures(
        slopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
        tapMaxTravelPx = HandleDefaults.TAP_MAX_TRAVEL_DP * density,
    )
    private val toggle = PanelToggle(SystemClock::elapsedRealtime)
    private val pillRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = HandleDefaults.COLOR_INDIGO }
    private val exclusionRect = Rect()

    private var trackedPointerId = INVALID_POINTER_ID

    // True from touch-down to release: the pill is at full opacity.
    private var held = false

    // 0 = idle pill, 1 = widened pill; animated between the two.
    private var widenFraction = 0f
    private var widenAnimator: ValueAnimator? = null

    private val longPressRunnable = Runnable { handle(gestures.onLongPress()) }

    init {
        contentDescription = context.getString(R.string.handle_content_description)
        isClickable = true
        isSoundEffectsEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    /** Changes the physical edge the pill hugs. */
    fun setSide(value: HandleSide) {
        if (side == value) return
        side = value
        invalidate()
        updateGestureExclusion()
    }

    override fun onDraw(canvas: Canvas) {
        val idleAlpha = HandleDefaults.OPACITY * 255f
        paint.alpha = if (held) 255 else (idleAlpha + (255f - idleAlpha) * widenFraction).roundToInt()
        val pillWidthPx = lerp(HandleDefaults.PILL_WIDTH_DP, HandleDefaults.PILL_WIDTH_ACTIVE_DP) * density
        // Hugs the edge when idle and drifts to the window centre as it widens (finger is not at the edge).
        val edgeLeft = if (side == HandleSide.RIGHT) width - pillWidthPx else 0f
        val centreLeft = (width - pillWidthPx) / 2f
        val left = edgeLeft + (centreLeft - edgeLeft) * widenFraction
        pillRect.set(left, 0f, left + pillWidthPx, height.toFloat())
        val radius = pillWidthPx / 2f
        canvas.drawRoundRect(pillRect, radius, radius, paint)
    }

    private fun lerp(from: Float, to: Float): Float = from + (to - from) * widenFraction

    private fun animateWiden(target: Float, durationMs: Long) {
        widenAnimator?.cancel()
        widenAnimator = null
        if (widenFraction == target) return
        if (!ValueAnimator.areAnimatorsEnabled()) {
            widenFraction = target
            invalidate()
            return
        }
        widenAnimator = ValueAnimator.ofFloat(widenFraction, target).apply {
            duration = durationMs
            addUpdateListener {
                widenFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
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
        widenAnimator?.cancel()
        widenAnimator = null
        widenFraction = 0f
        held = false
        trackedPointerId = INVALID_POINTER_ID
        gestures.onCancel()
        super.onDetachedFromWindow()
    }

    // Also the TalkBack / accessibility ACTION_CLICK path: no touch events, so it always shows.
    override fun performClick(): Boolean {
        super.performClick()
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        Log.d(TAG, "tap: panel shown")
        volume.showPanel()
        toggle.markShown()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trackedPointerId = event.getPointerId(0)
                toggle.onTouchDown()
                setHeld(true)
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
                val index = event.actionIndex
                if (event.getPointerId(index) == trackedPointerId) {
                    finishGesture(rawX(event, index), rawY(event, index))
                }
            }
            MotionEvent.ACTION_UP -> finishGesture(rawX(event, 0), rawY(event, 0))
            MotionEvent.ACTION_CANCEL -> cancelGesture()
        }
        return true
    }

    private fun finishGesture(rawX: Float, rawY: Float) {
        removeCallbacks(longPressRunnable)
        trackedPointerId = INVALID_POINTER_ID
        val action = gestures.onUp(rawX, rawY)
        setHeld(false)
        animateWiden(target = 0f, durationMs = HandleDefaults.NARROW_DURATION_MS)
        handle(action)
    }

    private fun cancelGesture() {
        removeCallbacks(longPressRunnable)
        trackedPointerId = INVALID_POINTER_ID
        val wasRepositioning = gestures.state == HandleGestures.State.Repositioning
        gestures.onCancel()
        setHeld(false)
        animateWiden(target = 0f, durationMs = HandleDefaults.NARROW_DURATION_MS)
        if (wasRepositioning) repositionListener.onCancel()
    }

    private fun setHeld(value: Boolean) {
        if (held != value) {
            held = value
            invalidate()
        }
    }

    private fun handle(action: HandleAction) {
        when (action) {
            HandleAction.None -> Unit
            HandleAction.ShowPanel ->
                if (toggle.onTap()) {
                    performClick()
                } else {
                    Log.d(TAG, "tap: close (not re-shown)")
                }
            HandleAction.BeginReposition -> {
                Log.d(TAG, "reposition begin")
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                animateWiden(target = 1f, durationMs = HandleDefaults.WIDEN_DURATION_MS)
                repositionListener.onBegin()
            }
            is HandleAction.Reposition -> repositionListener.onMove(action.dxPx, action.dyPx)
            is HandleAction.EndReposition ->
                repositionListener.onEnd(action.rawX, action.rawY, action.moved)
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
