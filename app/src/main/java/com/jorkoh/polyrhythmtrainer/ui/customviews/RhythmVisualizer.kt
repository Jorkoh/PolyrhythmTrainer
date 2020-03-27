package com.jorkoh.polyrhythmtrainer.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.jorkoh.polyrhythmtrainer.R

class RhythmVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_BPM = 80
        private const val DEFAULT_X_RHYTHM = 3
        private const val DEFAULT_Y_RHYTHM = 4
        private const val DEFAULT_X_RHYTHM_COLOR = Color.BLACK
        private const val DEFAULT_Y_RHYTHM_COLOR = Color.BLACK
    }

    // TODO view size of stuff should depend on dp
    //TODO check what happens if setters fail? throw exception?
    //TODO BPM shouldn't cause a redraw on the lines but the timings do change (?)
    enum class RhythmLines {
        X, Y
    }

    var BPM = DEFAULT_BPM
        set(value) {
            if (field != value && value > 0 && value <= 140) {
                field = value
                beatLengthMS = 60000 / value
            }
        }

    // Recalculated when changing BPM
    private var beatLengthMS = 750
        set(value) {
            field = value
            calculateRhythm()
        }


    var xRhythm = 1
        set(value) {
            if (field != value && value > 0 && value < 15) {
                field = value
                calculateRhythm(RhythmLines.X)
            }
        }
    var yRhythm = 1
        set(value) {
            if (field != value && value > 0 && value < 15) {
                field = value
                calculateRhythm(RhythmLines.Y)
            }
        }


    private var xRhythmSubdivisions = listOf(0f)
    private var yRhythmSubdivisions = listOf(0f)

    // Styled attributes
    private var xColor = DEFAULT_X_RHYTHM_COLOR
    private var yColor = DEFAULT_Y_RHYTHM_COLOR

    // Reused
    private val xPaint = Paint()
    private val yPaint = Paint()
    private val neutralPaint = Paint()

    init {
        if (attrs != null) {
            setupAttributes(attrs)
        }

        xPaint.color = xColor
        xPaint.isAntiAlias = true
        xPaint.strokeWidth = 30f

        yPaint.color = yColor
        yPaint.isAntiAlias = true
        yPaint.strokeWidth = 30f

        neutralPaint.color = Color.BLACK
        neutralPaint.isAntiAlias = true
        neutralPaint.strokeWidth = 20f
        neutralPaint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 20f)
    }

    private fun setupAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.RhythmVisualizer,
            0, 0
        )
        BPM = typedArray.getInt(R.styleable.RhythmVisualizer_bpm, DEFAULT_BPM)
        xRhythm = typedArray.getInt(R.styleable.RhythmVisualizer_xRhythm, DEFAULT_X_RHYTHM)
        yRhythm = typedArray.getInt(R.styleable.RhythmVisualizer_yRhythm, DEFAULT_Y_RHYTHM)
        xColor = typedArray.getColor(R.styleable.RhythmVisualizer_xColor, DEFAULT_X_RHYTHM_COLOR)
        yColor = typedArray.getColor(R.styleable.RhythmVisualizer_yColor, DEFAULT_Y_RHYTHM_COLOR)

        typedArray.recycle()
    }

    private fun calculateRhythm(line: RhythmLines? = null) {
        when (line) {
            RhythmLines.X -> {
                xRhythmSubdivisions = List(xRhythm) { index -> index / xRhythm.toFloat() }
            }
            RhythmLines.Y -> {
                yRhythmSubdivisions = List(yRhythm) { index -> index / yRhythm.toFloat() }
            }
            null -> {
                // TODO clean this up
                xRhythmSubdivisions = List(xRhythm) { index -> index / yRhythm.toFloat() }
                yRhythmSubdivisions = List(yRhythm) { index -> index / yRhythm.toFloat() }
            }
        }
    }

    // TODO add proper padding usage
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawLine(0f, height.toFloat() / 2, width.toFloat(), height.toFloat() / 2, neutralPaint)
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), neutralPaint)

        for (subdivision in xRhythmSubdivisions) {
            canvas.drawLine(
                width * subdivision,
                height.toFloat() / 2,
                width * subdivision,
                0f,
                xPaint
            )
        }

        for (subdivision in yRhythmSubdivisions) {
            canvas.drawLine(
                width * subdivision,
                height.toFloat() / 2,
                width * subdivision,
                height.toFloat(),
                yPaint
            )
        }
    }
}