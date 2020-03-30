package com.jorkoh.polyrhythmtrainer.destinations.customviews

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnRepeat
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.PolyrhythmSettings
import com.jorkoh.polyrhythmtrainer.destinations.customviews.EngineListener.TapResult

typealias TapResultWithTiming = Pair<TapResult, Double>

class PolyrhythmVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), EngineListener {

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

    // Function invoked on tap result
    private var actionOnTapResult: ((TapResult) -> Unit)? = null

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
    private var polyrhythmLengthMS = polyrhythmSettings.yNumberOfBeats * 60000 / polyrhythmSettings.BPM
        set(value) {
            field = value
            animator.duration = value.toLong()
        }

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

    // The tap timings of the current attempt as fractions of the total rhythm line
    // TODO this also needs info about what rhythm line it belongs to
    private var playerInputTimings = mutableListOf<TapResultWithTiming>()
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
            currentStatus = Status.PAUSED
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

        // If the user input is not currently being evaluated draw the rhythm
        // TODO this will change depending on difficulty (?)
        if (!playerPhase) {
            // Draw x lines on the top
            drawXLines(canvas)
            // Draw y lines on the bottom
            drawYLines(canvas)
        } else {
            drawPlayerInputTimingLines(canvas)
        }

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

    private fun drawPlayerInputTimingLines(canvas: Canvas) {
        for (resultWithTiming in playerInputTimings) {
            // TODO use proper paints
            canvas.drawLine(
                (usableRectF.left + usableRectF.width() * resultWithTiming.second).toFloat(),
                usableRectF.centerY() + rhythmLinesSeparation / 4,
                (usableRectF.left + usableRectF.width() * resultWithTiming.second).toFloat(),
                usableRectF.centerY() + 3 * rhythmLinesSeparation / 4,
                when (resultWithTiming.first) {
                    TapResult.Early -> playingPaint
                    TapResult.Late -> playingPaint
                    else -> pausedPaint
                }
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
        nativeStartRhythm()
        playerInputTimings.clear()
        animator.start()
    }

    private fun stop() {
        animator.cancel()
        playerPhase = false
        nativeStopRhythm()
    }

    fun doOnStatusChange(action: (newStatus: Status) -> Unit) {
        actionOnStatusChange = action
    }

    // Setting tap listener
    fun doOnTapResult(action: (result: TapResult) -> Unit) {
        this.actionOnTapResult = action
    }

    override fun onTapResult(tapResultNative: Int, tapTiming: Double) {
        val tapResult = TapResult.fromNativeValue(tapResultNative)
        // Call the listener if added
        actionOnTapResult?.invoke(tapResult)
        // Save the timing to be painted
        if (tapResult in TapResult.Early..TapResult.Success) {
            playerInputTimings.add(TapResultWithTiming(tapResult, tapTiming))
        }
    }

    override fun onMeasureFinish() {
        Log.d("TESTING", "onMeasureFinish")
    }

    // TODO This start and stop work just fine, the problem is that it has to
    // be between start and stop for the pads to sound this means that we will
    // have to start and stop from resume and just calculate relative to int64 start of rhythm?
    private external fun nativeStartRhythm()
    private external fun nativeStopRhythm()
}