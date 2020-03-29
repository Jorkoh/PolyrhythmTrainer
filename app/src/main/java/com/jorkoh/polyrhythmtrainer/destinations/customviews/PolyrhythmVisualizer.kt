package com.jorkoh.polyrhythmtrainer.destinations.customviews

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.PolyrhythmSettings

class PolyrhythmVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_NEUTRAL_COLOR = Color.BLACK
        private const val DEFAULT_PLAYING_COLOR = Color.RED
        private const val DEFAULT_PAUSED_COLOR = Color.GREEN
        private const val DEFAULT_X_RHYTHM_COLOR = Color.BLACK
        private const val DEFAULT_Y_RHYTHM_COLOR = Color.BLACK
    }

    // Status stuff
    enum class Status {
        PLAYING,
        PAUSED
    }

    private var actionOnStatusChange: ((Status) -> Unit)? = null
    private var currentStatus: Status = Status.PAUSED
        set(value) {
            if (field != value) {
                field = value
                when (value) {
                    Status.PLAYING -> start()
                    Status.PAUSED -> stop()
                }
                actionOnStatusChange?.invoke(value)
            }
        }

    var polyrhythmSettings = PolyrhythmSettings()
        set(value) {
            if (field.BPM != value.BPM) {
                polyrhythmLengthMS = value.yNumberOfBeats * 60000 / value.BPM
                stop()
            }
            if (field.xNumberOfBeats != value.xNumberOfBeats) {
                xRhythmSubdivisions = calculateRhythmLineSubdivisons(value.xNumberOfBeats)
                stop()
            }
            if (field.yNumberOfBeats != value.yNumberOfBeats) {
                yRhythmSubdivisions = calculateRhythmLineSubdivisons(value.yNumberOfBeats)
                polyrhythmLengthMS = value.yNumberOfBeats * 60000 / value.BPM
                stop()
            }
            field = value
        }

    // Recalculated when changing BPM or y number of beats
    private var polyrhythmLengthMS =
        polyrhythmSettings.yNumberOfBeats * 60000 / polyrhythmSettings.BPM
        set(value) {
            field = value
            animator.duration = value.toLong()
        }

    // Recalculated when changing BPM, causes redraw
    private var xRhythmSubdivisions =
        calculateRhythmLineSubdivisons(polyrhythmSettings.xNumberOfBeats)
        set(value) {
            field = value
            invalidate()
        }
    private var yRhythmSubdivisions =
        calculateRhythmLineSubdivisons(polyrhythmSettings.yNumberOfBeats)
        set(value) {
            field = value
            invalidate()
        }

    // Styled attributes
    private var neutralColor = DEFAULT_NEUTRAL_COLOR
    private var playingColor = DEFAULT_PLAYING_COLOR
    private var pausedColor = DEFAULT_PAUSED_COLOR
    private var xColor = DEFAULT_X_RHYTHM_COLOR
    private var yColor = DEFAULT_Y_RHYTHM_COLOR

    // Drawing stuff
    private val horizontalInternalPadding = resources.displayMetrics.density * 10
    private val rhythmLinesSeparation = resources.displayMetrics.density * 30
    private val usableRectF = RectF()
    private val xPaint = Paint()
    private val yPaint = Paint()
    private val neutralPaint = Paint()
    private val pausedPaint = Paint()
    private val playingPaint = Paint()

    // Animation
    private var animationProgress = 0f
    private var animator = ValueAnimator.ofInt(0, 1).apply {
        duration = polyrhythmLengthMS.toLong()
        addUpdateListener { valueAnimator ->
            interpolator = LinearInterpolator()
            animationProgress = valueAnimator.animatedFraction
            invalidate()
        }
        doOnEnd {
            animationProgress = 0f
            invalidate()
        }
    }

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
        neutralColor = typedArray.getColor(R.styleable.RhythmVisualizer_neutralColor, DEFAULT_NEUTRAL_COLOR)
        playingColor = typedArray.getColor(R.styleable.RhythmVisualizer_playingColor, DEFAULT_PLAYING_COLOR)
        pausedColor = typedArray.getColor(R.styleable.RhythmVisualizer_pausedColor, DEFAULT_PAUSED_COLOR)
        xColor = typedArray.getColor(R.styleable.RhythmVisualizer_xRhythmColor, DEFAULT_X_RHYTHM_COLOR)
        yColor = typedArray.getColor(R.styleable.RhythmVisualizer_yRhythmColor, DEFAULT_Y_RHYTHM_COLOR)

        typedArray.recycle()
    }

    private fun setupPaints() {
        xPaint.color = xColor
        xPaint.isAntiAlias = true
        xPaint.strokeWidth = resources.displayMetrics.density * 8

        yPaint.color = yColor
        yPaint.isAntiAlias = true
        yPaint.strokeWidth = resources.displayMetrics.density * 8

        neutralPaint.color = neutralColor
        neutralPaint.isAntiAlias = true
        neutralPaint.strokeWidth = resources.displayMetrics.density * 5

        playingPaint.color = playingColor
        playingPaint.isAntiAlias = true
        playingPaint.strokeWidth = resources.displayMetrics.density * 5

        pausedPaint.color = pausedColor
        pausedPaint.isAntiAlias = true
        pausedPaint.strokeWidth = resources.displayMetrics.density * 5
    }

    private fun calculateRhythmLineSubdivisons(numberOfBeats: Int) =
        List(numberOfBeats) { index -> index / numberOfBeats.toFloat() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw neutral lines, horizontal and end
        drawNeutralLines(canvas)
        // Draw x lines on the top
        drawXLines(canvas)
        // Draw y lines on the bottom
        drawYLines(canvas)
        // Draw the progress line
        drawAnimationProgress(canvas)
    }

    private fun drawNeutralLines(canvas: Canvas) {
        // Start of beat
        canvas.drawLine(
            usableRectF.left,
            usableRectF.centerY() - rhythmLinesSeparation / 2,
            usableRectF.left,
            usableRectF.centerY() + rhythmLinesSeparation / 2,
            neutralPaint
        )
        // Horizontal guide
        canvas.drawLine(
            usableRectF.left,
            usableRectF.centerY(),
            usableRectF.right,
            usableRectF.centerY(),
            neutralPaint
        )
        // End of beat
        canvas.drawLine(
            usableRectF.right,
            usableRectF.top + (usableRectF.height() / 2 - rhythmLinesSeparation) / 2,
            usableRectF.right,
            usableRectF.bottom - (usableRectF.height() / 2 - rhythmLinesSeparation) / 2,
            neutralPaint
        )
    }

    private fun drawXLines(canvas: Canvas) {
        for (subdivision in xRhythmSubdivisions) {
            canvas.drawLine(
                usableRectF.left + usableRectF.width() * subdivision,
                usableRectF.centerY() - rhythmLinesSeparation,
                usableRectF.left + usableRectF.width() * subdivision,
                usableRectF.top,
                xPaint
            )
        }
    }

    private fun drawYLines(canvas: Canvas) {
        for (subdivision in yRhythmSubdivisions) {
            canvas.drawLine(
                usableRectF.left + usableRectF.width() * subdivision,
                usableRectF.centerY() + rhythmLinesSeparation,
                usableRectF.left + usableRectF.width() * subdivision,
                usableRectF.bottom,
                yPaint
            )
        }
    }

    private fun drawAnimationProgress(canvas: Canvas) {
        canvas.drawLine(
            usableRectF.left + usableRectF.width() * animationProgress,
            usableRectF.centerY() - rhythmLinesSeparation / 2,
            usableRectF.left + usableRectF.width() * animationProgress,
            usableRectF.centerY() + rhythmLinesSeparation / 2,
            when (currentStatus) {
                Status.PLAYING -> playingPaint
                Status.PAUSED -> pausedPaint
            }
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Size of the pad itself
        usableRectF.set(
            paddingLeft + horizontalInternalPadding,
            paddingTop.toFloat(),
            w - (paddingRight + horizontalInternalPadding),
            (h - paddingBottom).toFloat()
        )
    }


    fun changeStatus() {
        currentStatus = when (currentStatus) {
            Status.PLAYING -> Status.PAUSED
            Status.PAUSED -> Status.PLAYING
        }
    }

    private fun start() {
        nativeStart()
        animator.start()
    }

    private fun stop() {
        animator.cancel()
        nativeStop()
    }

    fun doOnStatusChange(action: (newStatus: Status) -> Unit) {
        actionOnStatusChange = action
    }

    private external fun nativeStart()
    private external fun nativeStop()
}