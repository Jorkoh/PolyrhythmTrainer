package com.jorkoh.polyrhythmtrainer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getIntOrThrow
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.jorkoh.polyrhythmtrainer.R

class PadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // TODO pad positions should be some kind of enum, not hardcoded values
        private const val DEFAULT_PAD_POSITION = -1
        private const val DEFAULT_PAD_COLOR = Color.WHITE
        private const val DEFAULT_PAD_RIPPLE_COLOR = Color.GRAY
    }

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
    private var currentAnimationValue = 0
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

        if (currentAnimationValue != 0) {
            animationPaint.alpha = (255 * (1 - currentAnimationValue / 100f)).toInt()
            animationPaint.strokeWidth = 40f * (1 - currentAnimationValue / 100f)
            canvas.drawCircle(animationEpicenterX, animationEpicenterY, 20f + currentAnimationValue * 4f, animationPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        drawRectF.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (w - paddingRight).toFloat(),
            (h - paddingBottom).toFloat()
        )

        // Rounded corner radius size depends on the size of the pad
        cornerRadius = minOf(w / 8.0f, h / 8.0f)
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
        animator?.cancel()

        animationEpicenterX = x
        animationEpicenterY = y
        animator = ValueAnimator.ofInt(0, 100).apply {
            duration = 750
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { valueAnimator ->
                val newAnimationValue = valueAnimator.animatedValue as Int
                if (currentAnimationValue != newAnimationValue) {
                    currentAnimationValue = newAnimationValue
                    invalidate()
                }
            }
            doOnEnd {
                currentAnimationValue = -1
                invalidate()
            }
        }
        animator?.start()
    }

    private external fun nativeOnPadTouch(padPosition: Int, timeSinceBoot: Long)
}