package com.jorkoh.polyrhythmtrainer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jorkoh.polyrhythmtrainer.R

class PadView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object{
        private const val DEFAULT_PAD_COLOR = Color.WHITE
        private const val DEFAULT_PAD_RIPPLE_COLOR = Color.GRAY
    }

    private var padColor = DEFAULT_PAD_COLOR
    private var padRippleColor = DEFAULT_PAD_RIPPLE_COLOR

    private val paint = Paint()
    private val drawRectF = RectF()

    init {
        setupAttributes(attrs)
    }

    private fun setupAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.PadView,
            0, 0
        )
        padColor = typedArray.getColor(R.styleable.PadView_padColor, DEFAULT_PAD_COLOR)
        padRippleColor = typedArray.getColor(R.styleable.PadView_padRippleColor, DEFAULT_PAD_RIPPLE_COLOR)

        paint.color = padColor

        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = minOf(drawRectF.width() / 8.0f, drawRectF.height() / 8.0f)
        canvas.drawRoundRect(drawRectF, cornerRadius, cornerRadius, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        drawRectF.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )

        setMeasuredDimension(width, height)
    }
}