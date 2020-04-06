package com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews

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

    companion object {
        private const val DEFAULT_PAD_POSITION = -1
        private const val DEFAULT_PAD_COLOR = Color.BLACK
        private const val DEFAULT_PAD_RIPPLE_COLOR = Color.GRAY
    }

    // Styled attributes
    private var padPosition = DEFAULT_PAD_POSITION
    private var padColor = DEFAULT_PAD_COLOR
    private var padRippleColor = DEFAULT_PAD_RIPPLE_COLOR

    // Drawing stuff
    private val padPaint = Paint()
    private val animationPaint = Paint()
    private val drawRectF = RectF()

    // Animation
    private var animationProgress = 0f
    private var animator = ValueAnimator.ofInt(0, 1).apply {
        duration = 800
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
    private var animationEpicenterX = 0f
    private var animationEpicenterY = 0f
    private val animationStrokeWidthDecayFactor = resources.displayMetrics.density * 20
    private val animationRadiusStart = resources.displayMetrics.density * 12
    private val animationRadiusGrowthFactor = resources.displayMetrics.density * 100

    init {
        if (attrs != null) {
            setupAttributes(attrs)
        }
        setupPaints()
        clipToOutline = true
    }

    private fun setupAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.PadView,
            0, 0
        )
        padPosition = typedArray.getIntOrThrow(R.styleable.PadView_padPosition)
        padColor = typedArray.getColor(R.styleable.PadView_padColor, DEFAULT_PAD_COLOR)
        padRippleColor =
            typedArray.getColor(R.styleable.PadView_padRippleColor, DEFAULT_PAD_RIPPLE_COLOR)

        typedArray.recycle()
    }

    private fun setupPaints() {
        padPaint.color = padColor
        padPaint.isAntiAlias = true
        padPaint.style = Paint.Style.FILL

        animationPaint.color = padRippleColor
        animationPaint.isAntiAlias = true
        animationPaint.style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the pad itself
        canvas.drawRect(drawRectF, padPaint)
        // If the animation is in progress draw the ripple
        if (animationProgress != 0f) {
            animationPaint.alpha = (255 * (1 - animationProgress)).toInt()
            animationPaint.strokeWidth = animationStrokeWidthDecayFactor * (1 - animationProgress)
            canvas.drawCircle(
                animationEpicenterX,
                animationEpicenterY,
                animationRadiusStart + animationProgress * animationRadiusGrowthFactor,
                animationPaint
            )
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
        val cornerRadius = minOf(w / 8.0f, h / 8.0f)
        // Clip corners and add elevation shadows
        outlineProvider = CustomOutline(w, h, cornerRadius)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Signal the native game process
                nativeOnPadTouch(padPosition, event.eventTime)
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
        animator.cancel()
        animationEpicenterX = x
        animationEpicenterY = y
        animator.start()
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

    private external fun nativeOnPadTouch(padPosition: Int, timeSinceBoot: Long)
}