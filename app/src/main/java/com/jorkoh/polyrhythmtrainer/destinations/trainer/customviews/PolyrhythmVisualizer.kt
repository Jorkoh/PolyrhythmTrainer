package com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews

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
import androidx.core.animation.doOnRepeat
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews.EngineListener.TapResult
import com.jorkoh.polyrhythmtrainer.repositories.PolyrhythmSettingsRepositoryImplementation.Companion.DEFAULT_BPM
import com.jorkoh.polyrhythmtrainer.repositories.PolyrhythmSettingsRepositoryImplementation.Companion.DEFAULT_X_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.PolyrhythmSettingsRepositoryImplementation.Companion.DEFAULT_Y_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.RhythmLine

typealias TapResultWithTimingAndLine = Triple<TapResult, Double, RhythmLine>

class PolyrhythmVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), EngineListener {

    companion object {
        private const val DEFAULT_NEUTRAL_COLOR = Color.BLACK
        private const val DEFAULT_SUCCESS_COLOR = Color.GREEN
        private const val DEFAULT_ERROR_COLOR = Color.RED
        private const val DEFAULT_PROGRESS_COLOR = Color.RED
        private const val DEFAULT_X_RHYTHM_COLOR = Color.BLACK
        private const val DEFAULT_Y_RHYTHM_COLOR = Color.BLACK
    }

    // Status stuff
    enum class Status {
        PLAYING,
        BEFORE_PLAY,
        AFTER_PLAY
    }

    private var actionOnStatusChange: ((Status) -> Unit)? = null
    private var currentStatus: Status = Status.BEFORE_PLAY
        set(value) {
            if (field != value) {
                field = value
                when (value) {
                    Status.BEFORE_PLAY -> resetAnimation()
                    Status.PLAYING -> startRhythm()
                    Status.AFTER_PLAY -> stopRhythm()
                }
                actionOnStatusChange?.invoke(value)
            }
        }

    // Function invoked on tap result
    private var actionOnTapResult: ((TapResult) -> Unit)? = null

    var bpm = DEFAULT_BPM
        set(value) {
            if (field != value) {
                polyrhythmLengthMS = yNumberOfBeats * 60000 / value
                field = value
                stop()
            }
        }

    var xNumberOfBeats = DEFAULT_X_NUMBER_OF_BEATS
        set(value) {
            if (field != value) {
                xRhythmSubdivisions = calculateRhythmLineSubdivisions(value)
                field = value
                stop()
            }
        }

    var yNumberOfBeats = DEFAULT_Y_NUMBER_OF_BEATS
        set(value) {
            if (field != value) {
                polyrhythmLengthMS = value * 60000 / bpm
                yRhythmSubdivisions = calculateRhythmLineSubdivisions(value)
                field = value
                stop()
            }
        }

    // Recalculated when changing BPM or y number of beats
    private var polyrhythmLengthMS = yNumberOfBeats * 60000 / bpm
        set(value) {
            field = value
            animator.duration = value.toLong()
        }

    // Recalculated when changing BPM, causes redraw
    private var xRhythmSubdivisions = calculateRhythmLineSubdivisions(xNumberOfBeats)
        set(value) {
            field = value
            invalidate()
        }
    private var yRhythmSubdivisions = calculateRhythmLineSubdivisions(yNumberOfBeats)
        set(value) {
            field = value
            invalidate()
        }

    // The tap timings of the current attempt as fractions of the total rhythm line
    private var playerInputTimings = mutableListOf<TapResultWithTimingAndLine>()
        set(value) {
            field = value
            invalidate()
        }

    // Styled attributes
    private var neutralColor = DEFAULT_NEUTRAL_COLOR
    private var successColor = DEFAULT_SUCCESS_COLOR
    private var errorColor = DEFAULT_ERROR_COLOR
    private var progressColor = DEFAULT_PROGRESS_COLOR
    private var xColor = DEFAULT_X_RHYTHM_COLOR
    private var yColor = DEFAULT_Y_RHYTHM_COLOR

    // Drawing stuff
    private val horizontalInternalPadding = resources.displayMetrics.density * 10
    private val rhythmLinesSeparationFromCenter = resources.displayMetrics.density * 30
    private val tapResultSeparationFromCenterStart = rhythmLinesSeparationFromCenter / 4
    private val tapResultSeparationFromCenterEnd = tapResultSeparationFromCenterStart * 3
    private val rhythmRectangle = RectF()
    private val rhythmWithErrorWindowsRectangle = RectF()
    private val xPaint = Paint()
    private val yPaint = Paint()
    private val neutralPaint = Paint()
    private val successPaint = Paint()
    private val errorPaint = Paint()
    private val progressPaint = Paint()

    // Animation
    private var animationProgress = 0f
    private var playerPhase = false
    private var animator = ValueAnimator.ofInt(0, 1).apply {
        duration = polyrhythmLengthMS.toLong()
        addUpdateListener { valueAnimator ->
            interpolator = LinearInterpolator()
            repeatCount = 1
            animationProgress = valueAnimator.animatedFraction
            invalidate()
        }
        doOnRepeat {
            playerPhase = true
        }
        doOnEnd {
            animationProgress = 0f
            // TODO calling pause here sends signal to native to stop but the signal should come from native
            //  since it's the source of truth in timing matters and we need to wait the window for last event
            currentStatus = Status.AFTER_PLAY
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
        successColor = typedArray.getColor(R.styleable.RhythmVisualizer_successColor, DEFAULT_SUCCESS_COLOR)
        errorColor = typedArray.getColor(R.styleable.RhythmVisualizer_errorColor, DEFAULT_ERROR_COLOR)
        progressColor = typedArray.getColor(R.styleable.RhythmVisualizer_progressColor, DEFAULT_PROGRESS_COLOR)
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

        successPaint.color = successColor
        successPaint.isAntiAlias = true
        successPaint.strokeWidth = resources.displayMetrics.density * 5

        errorPaint.color = errorColor
        errorPaint.isAntiAlias = true
        errorPaint.strokeWidth = resources.displayMetrics.density * 5

        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true
        progressPaint.strokeWidth = resources.displayMetrics.density * 5
    }

    private fun calculateRhythmLineSubdivisions(numberOfBeats: Int) =
        List(numberOfBeats) { index -> index / numberOfBeats.toFloat() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw neutral lines, horizontal and end
        drawNeutralLines(canvas)

        // If the user input is not currently being evaluated draw the rhythm
        if (!playerPhase) {
            // Draw x lines on the top
            drawXLines(canvas)
            // Draw y lines on the bottom
            drawYLines(canvas)
        }

        // If the user is actively playing or is reviewing his play draw his tap results
        if (playerPhase || currentStatus == Status.AFTER_PLAY) {
            drawPlayerInputTimingLines(canvas)
        }

        // Draw the progress line
        if (currentStatus != Status.AFTER_PLAY) {
            drawAnimationProgress(canvas)
        }
    }

    private fun drawNeutralLines(canvas: Canvas) {
        // Start of beat
        canvas.drawLine(
            rhythmRectangle.left,
            rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter / 2,
            rhythmRectangle.left,
            rhythmRectangle.centerY() + rhythmLinesSeparationFromCenter / 2,
            neutralPaint
        )
        // Horizontal guide
        canvas.drawLine(
            rhythmWithErrorWindowsRectangle.left,
            rhythmWithErrorWindowsRectangle.centerY(),
            rhythmWithErrorWindowsRectangle.right,
            rhythmWithErrorWindowsRectangle.centerY(),
            neutralPaint
        )
        // End of beat
        canvas.drawLine(
            rhythmRectangle.right,
            rhythmRectangle.top + (rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter) / 2,
            rhythmRectangle.right,
            rhythmRectangle.bottom - (rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter) / 2,
            neutralPaint
        )
    }

    private fun drawXLines(canvas: Canvas) {
        for (subdivision in xRhythmSubdivisions) {
            canvas.drawLine(
                rhythmRectangle.left + rhythmRectangle.width() * subdivision,
                rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter,
                rhythmRectangle.left + rhythmRectangle.width() * subdivision,
                rhythmRectangle.top,
                xPaint
            )
        }
    }

    private fun drawYLines(canvas: Canvas) {
        for (subdivision in yRhythmSubdivisions) {
            canvas.drawLine(
                rhythmRectangle.left + rhythmRectangle.width() * subdivision,
                rhythmRectangle.centerY() + rhythmLinesSeparationFromCenter,
                rhythmRectangle.left + rhythmRectangle.width() * subdivision,
                rhythmRectangle.bottom,
                yPaint
            )
        }
    }

    private fun drawPlayerInputTimingLines(canvas: Canvas) {
        for (resultWithTimingAndLine in playerInputTimings) {
            canvas.drawLine(
                (rhythmRectangle.left + rhythmRectangle.width() * resultWithTimingAndLine.second).toFloat(),
                if (resultWithTimingAndLine.third == RhythmLine.X) {
                    rhythmRectangle.centerY() - tapResultSeparationFromCenterStart
                } else {
                    rhythmRectangle.centerY() + tapResultSeparationFromCenterStart
                },
                (rhythmRectangle.left + rhythmRectangle.width() * resultWithTimingAndLine.second).toFloat(),
                if (resultWithTimingAndLine.third == RhythmLine.X) {
                    rhythmRectangle.centerY() - tapResultSeparationFromCenterEnd
                } else {
                    rhythmRectangle.centerY() + tapResultSeparationFromCenterEnd
                },
                when (resultWithTimingAndLine.first) {
                    TapResult.Success -> successPaint
                    else -> errorPaint
                }
            )
        }
    }

    private fun drawAnimationProgress(canvas: Canvas) {
        canvas.drawLine(
            rhythmRectangle.left + rhythmRectangle.width() * animationProgress,
            rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter / 2,
            rhythmRectangle.left + rhythmRectangle.width() * animationProgress,
            rhythmRectangle.centerY() + rhythmLinesSeparationFromCenter / 2,
            progressPaint
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        rhythmWithErrorWindowsRectangle.set(
            paddingLeft + horizontalInternalPadding,
            paddingTop.toFloat(),
            w - (paddingRight + horizontalInternalPadding),
            (h - paddingBottom).toFloat()
        )

        val errorWindowSize = 0.04f * w
        rhythmRectangle.set(
            paddingLeft + horizontalInternalPadding + errorWindowSize,
            paddingTop.toFloat(),
            w - (paddingRight + horizontalInternalPadding + errorWindowSize),
            (h - paddingBottom).toFloat()
        )
    }


    fun advanceToNextState() {
        when (currentStatus) {
            Status.BEFORE_PLAY -> currentStatus = Status.PLAYING
            Status.PLAYING -> {
                currentStatus = Status.AFTER_PLAY
                currentStatus = Status.BEFORE_PLAY
            }
            Status.AFTER_PLAY -> {
                currentStatus = Status.BEFORE_PLAY
                currentStatus = Status.PLAYING
            }
        }
    }

    fun stop() {
        currentStatus = Status.AFTER_PLAY
        currentStatus = Status.BEFORE_PLAY
    }

    // Not directly used, called from status change
    private fun startRhythm() {
        nativeStartRhythm()
        animator.start()
    }

    // Not directly used, called from status change
    private fun stopRhythm() {
        animator.cancel()
        playerPhase = false
        nativeStopRhythm()
    }

    // Not directly used, called from status change
    private fun resetAnimation() {
        animationProgress = 0f
        playerInputTimings.clear()
        invalidate()
    }

    fun doOnStatusChange(action: (newStatus: Status) -> Unit) {
        actionOnStatusChange = action
    }

    // Setting tap listener
    fun doOnTapResult(action: (result: TapResult) -> Unit) {
        this.actionOnTapResult = action
    }

    override fun onTapResult(tapResultNative: Int, tapTiming: Double, rhythmLineNative: Int) {
        val tapResult = TapResult.fromNativeValue(tapResultNative)
        val rhythmLine = RhythmLine.fromNativeValue(rhythmLineNative)
        // Call the listener if added
        actionOnTapResult?.invoke(tapResult)
        // Save the timing to be painted
        if (tapResult in TapResult.Early..TapResult.Success) {
            playerInputTimings.add(TapResultWithTimingAndLine(tapResult, tapTiming, rhythmLine))
        }
    }

    private external fun nativeStartRhythm()
    private external fun nativeStopRhythm()
}