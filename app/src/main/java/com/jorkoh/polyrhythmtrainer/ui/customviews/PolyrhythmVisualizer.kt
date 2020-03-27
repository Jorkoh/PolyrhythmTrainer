package com.jorkoh.polyrhythmtrainer.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.ui.PolyrhythmSettings

class PolyrhythmVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_X_RHYTHM_COLOR = Color.BLACK
        private const val DEFAULT_Y_RHYTHM_COLOR = Color.BLACK
    }

    // TODO view size of stuff should depend on dp
    var polyrhythmSettings = PolyrhythmSettings()
        set(value) {
            if (field.BPM != value.BPM) {
                beatLengthMS = 60000 / value.BPM
            }
            if (field.xNumberOfBeats != value.xNumberOfBeats) {
                xRhythmSubdivisions = calculateRhythmLineSubdivisons(value.xNumberOfBeats)
            }
            if (field.yNumberOfBeats != value.yNumberOfBeats) {
                yRhythmSubdivisions = calculateRhythmLineSubdivisons(value.yNumberOfBeats)
            }
            field = value
        }

    // Recalculated when changing BPM
    private var beatLengthMS = 60000 / polyrhythmSettings.BPM

    // Recalculated when changing BPM, causes redraw
    private var xRhythmSubdivisions = calculateRhythmLineSubdivisons(polyrhythmSettings.xNumberOfBeats)
        set(value) {
            field = value
            invalidate()
        }
    private var yRhythmSubdivisions = calculateRhythmLineSubdivisons(polyrhythmSettings.yNumberOfBeats)
        set(value) {
            field = value
            invalidate()
        }

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
        setupPaints()

    }

    private fun setupAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.RhythmVisualizer,
            0, 0
        )
        xColor = typedArray.getColor(R.styleable.RhythmVisualizer_xColor, DEFAULT_X_RHYTHM_COLOR)
        yColor = typedArray.getColor(R.styleable.RhythmVisualizer_yColor, DEFAULT_Y_RHYTHM_COLOR)

        typedArray.recycle()
    }

    private fun setupPaints(){
        xPaint.color = xColor
        xPaint.isAntiAlias = true
        xPaint.strokeWidth = 30f

        yPaint.color = yColor
        yPaint.isAntiAlias = true
        yPaint.strokeWidth = 30f

        neutralPaint.color = Color.GRAY
        neutralPaint.isAntiAlias = true
        neutralPaint.strokeWidth = 15f
    }

    private fun calculateRhythmLineSubdivisons(numberOfBeats: Int) =
        List(numberOfBeats) { index -> index / numberOfBeats.toFloat() }

    // TODO add proper padding usage
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw neutral lines, horizontal and end
        canvas.drawLine(
            0f,
            height.toFloat() / 2,
            width.toFloat(),
            height.toFloat() / 2,
            neutralPaint
        )
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), neutralPaint)

        // Draw x lines on the top
        for (subdivision in xRhythmSubdivisions) {
            canvas.drawLine(
                width * subdivision,
                height.toFloat() / 2,
                width * subdivision,
                0f,
                xPaint
            )
        }

        // Draw y lines on the bottom
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