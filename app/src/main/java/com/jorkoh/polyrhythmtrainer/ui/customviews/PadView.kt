package com.jorkoh.polyrhythmtrainer.ui.customviews

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getIntOrThrow
import com.jorkoh.polyrhythmtrainer.R

class PadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // TODO view size of stuff should depend on dp
    companion object {
        // TODO pad positions should be some kind of enum, not hardcoded values
        private const val DEFAULT_PAD_POSITION = -1
        private const val DEFAULT_PAD_COLOR = Color.BLACK
        private const val DEFAULT_PAD_RIPPLE_COLOR = Color.GRAY
    }

    // Function invoked on touch
    private var actionOnTapResult: ((TapResult) -> Unit)? = null

    // Styled attributes
    private var padPosition = DEFAULT_PAD_POSITION
    private var padColor = DEFAULT_PAD_COLOR
    private var padRippleColor = DEFAULT_PAD_RIPPLE_COLOR

    // Reused
    private val padPaint = Paint()
    private val animationPaint = Paint()
    private val drawRectF = RectF()

    // Calculated
    private var cornerRadius = 20f

    // Animation
    private var animator: ValueAnimator? = null
    private var animationProgress = 0f
    private var animationEpicenterX = 0f
    private var animationEpicenterY = 0f

    init {
        if (attrs != null) {
            setupAttributes(attrs)
        }

        padPaint.color = padColor
        padPaint.isAntiAlias = true
        padPaint.style = Paint.Style.FILL

        animationPaint.color = padRippleColor
        animationPaint.isAntiAlias = true
        animationPaint.style = Paint.Style.STROKE
    }

    private fun setupAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.PadView,
            0, 0
        )
        padPosition = typedArray.getIntOrThrow(R.styleable.PadView_padPosition)
        padColor = typedArray.getColor(R.styleable.PadView_padColor, DEFAULT_PAD_COLOR)
        padRippleColor = typedArray.getColor(R.styleable.PadView_padRippleColor, DEFAULT_PAD_RIPPLE_COLOR)

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRoundRect(drawRectF, cornerRadius, cornerRadius, padPaint)

        // If the animation is in progress draw the ripple
        if (animationProgress != 0f) {
            animationPaint.alpha = (255 * (1 - animationProgress)).toInt()
            animationPaint.strokeWidth = 50f * (1 - animationProgress)
            canvas.drawCircle(animationEpicenterX, animationEpicenterY, 30f + animationProgress * 400f, animationPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Size of the pad itself
        drawRectF.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (w - paddingRight).toFloat(),
            (h - paddingBottom).toFloat()
        )
        // Rounded corner radius size depends on the size of the pad
        cornerRadius = minOf(w / 8.0f, h / 8.0f)
        // Shadow and elevation support
        outlineProvider = CustomOutline(w, h, cornerRadius)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Signal the native game process
                val tapResult = nativeOnPadTouch(padPosition, event.eventTime)
                actionOnTapResult?.invoke(TapResult.fromNativeValue(tapResult))

                // Run animation for the custom view
                startTouchAnimation(event.x, event.y)
                // Accessibility reasons
                performClick()
                true
            }
            else -> false
        }
    }

    // Accessibility reasons, should probably add action description here
    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun startTouchAnimation(x: Float, y: Float) {
        animator?.cancel()

        animationEpicenterX = x
        animationEpicenterY = y
        animator = ValueAnimator.ofInt(0, 1).apply {
            duration = 1000
            interpolator = DecelerateInterpolator(1.75f)
            addUpdateListener { valueAnimator ->
                animationProgress = valueAnimator.animatedFraction
                invalidate()
            }
            doOnEnd {
                animationProgress = 0f
                invalidate()
            }
        }
        animator?.start()
    }

    inner class CustomOutline(
        private val width: Int,
        private val height: Int,
        private val cornerRadius: Float
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, width, height, cornerRadius)
        }
    }

    enum class TapResult(val nativeValue: Int) {
        Error(0),
        Early(1),
        Success(2),
        Late(3);

        companion object {
            fun fromNativeValue(nativeValue: Int) = values().first { it.nativeValue == nativeValue }
        }
    }

    fun doOnTapResult(action: (result: TapResult) -> Unit) {
        this.actionOnTapResult = action
    }

    private external fun nativeOnPadTouch(padPosition: Int, timeSinceBoot: Long): Int
}