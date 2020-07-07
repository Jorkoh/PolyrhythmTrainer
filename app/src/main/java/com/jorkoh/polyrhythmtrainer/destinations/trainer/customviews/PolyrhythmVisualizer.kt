package com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnRepeat
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews.EngineListener.TapResult
import com.jorkoh.polyrhythmtrainer.repositories.Mode
import com.jorkoh.polyrhythmtrainer.repositories.RhythmLine
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.DEFAULT_BPM
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.DEFAULT_X_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.DEFAULT_Y_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.MEASURES_TO_PASS_IMPOSSIBLE

data class TapResultWithTimingLineAndMeasure(val tapResult: TapResult, val tapTiming: Double, val rhythmLine: RhythmLine, val measure: Int)

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

    var parentTrainer: TrainerView? = null

    // The current state of the visualizer
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
                actionOnStatusChange?.invoke(field)
            }
        }

    var bpm = DEFAULT_BPM
        set(value) {
            if (field != value) {
                polyrhythmLengthInMs = yNumberOfBeats * 60000 / value
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
                polyrhythmLengthInMs = value * 60000 / bpm
                yRhythmSubdivisions = calculateRhythmLineSubdivisions(value)
                field = value
                stop()
            }
        }

    var mode = Mode()
        set(value) {
            if (field != value) {
                field = value
                resizeRhythmRectangle(width, height)
                animator.repeatCount = calculateRepeatCount(value)
                stop()
            }
        }

    // Recalculated when changing BPM or y number of beats
    private var polyrhythmLengthInMs = yNumberOfBeats * 60000 / bpm
        set(value) {
            field = value
            animator.duration = calculateAnimatorDuration(value)
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
    private var playerInputTimings = mutableListOf<TapResultWithTimingLineAndMeasure>()
        set(value) {
            field = value
            invalidate()
        }

    // The number of player mistakes on the current attempt
    private var actionOnMistakeCountChange: ((Int) -> Unit)? = null
    private var mistakeCount = 0
        set(value) {
            if (field != value) {
                field = value
                attemptResultSuccess = calculateAttemptResultSuccess(value, mode)
                actionOnMistakeCountChange?.invoke(field)
            }
        }

    // The current measure in play
    private var actionOnCurrentMeasureChange: ((Int) -> Unit)? = null
    private var currentMeasure = 0
        set(value) {
            if (field != value) {
                field = value
                actionOnCurrentMeasureChange?.invoke(field)
            }
        }

    private var actionOnExerciseEnd: ((Boolean, Int) -> Unit)? = null
    private var attemptResultSuccess = true
        set(value) {
            field = value
            if (!value) {
                currentStatus = Status.AFTER_PLAY

                // The exercise failed, unless it was impossible mode and he completed the needed measures
                actionOnExerciseEnd?.invoke(
                    mode.playerMeasures == -1 && currentMeasure - mode.engineMeasures >= MEASURES_TO_PASS_IMPOSSIBLE,
                    currentMeasure
                )
            }
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

    // Inner rectangle where the actual rhythm is displayed
    private val rhythmRectangle = RectF()

    // Outer rectangle including error windows at the start and end of the measure
    private val rhythmWithErrorWindowsRectangle = RectF()

    // Paints
    private val xPaint = Paint()
    private val yPaint = Paint()
    private val neutralPaint = Paint()
    private val successPaint = Paint()
    private val errorPaint = Paint()
    private val progressPaint = Paint()

    // Animation
    private var animationProgress = 0f
    private var playerPhase = mode.engineMeasures == 0
    private var animator = ValueAnimator.ofInt(0, 1).apply {
        duration = calculateAnimatorDuration(polyrhythmLengthInMs)
        addUpdateListener { valueAnimator ->
            interpolator = LinearInterpolator()
            repeatCount = calculateRepeatCount(mode)
            animationProgress = valueAnimator.animatedFraction
            invalidate()
        }
        doOnRepeat {
            // If the user is playing count missed beats of the previous measure as mistakes
            if (playerPhase) {
                addMissedBeatsAsMistakes()
                // If the mistakes of the previous measure caused the failure of the attempt return early
                if (currentStatus != Status.PLAYING) {
                    return@doOnRepeat
                }
            }

            currentMeasure += 1
            // If it's not infinite engine measures (metronome) player phase is enabled when
            // the number of repeats have surpassed the designated engine measures
            if (mode.engineMeasures >= 0) {
                playerPhase = currentMeasure > mode.engineMeasures
            }
        }
        doOnEnd {
            // If the user is playing count missed beats by the end of the measure as mistakes
            if (playerPhase) {
                addMissedBeatsAsMistakes()
            }
            animationProgress = 0f
            currentStatus = Status.AFTER_PLAY
            if (attemptResultSuccess) {
                actionOnExerciseEnd?.invoke(true, currentMeasure)
            }
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

    // Keep the animator speed constant independently of the user's animator duration setting
    private fun calculateAnimatorDuration(lengthInMs: Int) : Long {
        val animScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return (lengthInMs/animScale).toLong()
    }

    private fun calculateRhythmLineSubdivisions(numberOfBeats: Int) =
        List(numberOfBeats) { index -> index / numberOfBeats.toFloat() }

    private fun calculateRepeatCount(mode: Mode): Int =
        if (mode.engineMeasures < 0 || mode.playerMeasures < 0) {
            ValueAnimator.INFINITE
        } else {
            (mode.engineMeasures + mode.playerMeasures) - 1
        }

    // The attempt is successful when the user has no mistakes or when the number of missed inputs
    // is less than (xNumberOfBeats + yNumberOfBeats) / 2 in a mode that allows for mistakes
    private fun calculateAttemptResultSuccess(mistakeCount: Int, mode: Mode) =
        mistakeCount == 0 || (mode.allowSomeMistakes && mistakeCount < (xNumberOfBeats + yNumberOfBeats) / 2)

    // Add to the mistake count the difference between the expected amount of beats and the amount of player inputs
    private fun addMissedBeatsAsMistakes() {
        mistakeCount += xNumberOfBeats + yNumberOfBeats - playerInputTimings.filter {
            it.measure == currentMeasure - mode.engineMeasures
        }.size
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw neutral lines, horizontal and end
        drawNeutralLines(canvas)

        // If the user input is not currently being evaluated or the mode dictates
        // that the beat lines are always present draw the rhythm as a visual guide
        if (!playerPhase || mode.showBeatLines) {
            // Draw x lines on the top
            drawXLines(canvas)
            // Draw y lines on the bottom
            drawYLines(canvas)
        }

        // If the user is actively playing or is reviewing his play draw his tap results
        if (playerPhase || currentStatus == Status.AFTER_PLAY) {
            drawPlayerInputTimingLines(canvas)
        }

        // If not reviewing the attempt draw the progress line
        if (currentStatus != Status.AFTER_PLAY) {
            drawAnimationProgress(canvas)
        }
    }

    private fun drawNeutralLines(canvas: Canvas) {
        val paint = if (currentStatus != Status.AFTER_PLAY) {
            // Unless in the review phase use the neutral paint
            neutralPaint
        } else {
            // If it's the review phase color depending on the attempt result
            if (attemptResultSuccess) {
                successPaint
            } else {
                errorPaint
            }
        }

        // Start of beat
        canvas.drawLine(
            rhythmRectangle.left,
            rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter / 2,
            rhythmRectangle.left,
            rhythmRectangle.centerY() + rhythmLinesSeparationFromCenter / 2,
            paint
        )
        // Horizontal guide
        canvas.drawLine(
            rhythmWithErrorWindowsRectangle.left,
            rhythmWithErrorWindowsRectangle.centerY(),
            rhythmWithErrorWindowsRectangle.right,
            rhythmWithErrorWindowsRectangle.centerY(),
            paint
        )
        // End of beat
        canvas.drawLine(
            rhythmRectangle.right,
            rhythmRectangle.top + (rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter) / 2,
            rhythmRectangle.right,
            rhythmRectangle.bottom - (rhythmRectangle.centerY() - rhythmLinesSeparationFromCenter) / 2,
            paint
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
        for (result in playerInputTimings.filter { it.measure == currentMeasure - mode.engineMeasures }) {
            canvas.drawLine(
                (rhythmRectangle.left + rhythmRectangle.width() * result.tapTiming).toFloat(),
                if (result.rhythmLine == RhythmLine.X) {
                    rhythmRectangle.centerY() - tapResultSeparationFromCenterStart
                } else {
                    rhythmRectangle.centerY() + tapResultSeparationFromCenterStart
                },
                (rhythmRectangle.left + rhythmRectangle.width() * result.tapTiming).toFloat(),
                if (result.rhythmLine == RhythmLine.X) {
                    rhythmRectangle.centerY() - tapResultSeparationFromCenterEnd
                } else {
                    rhythmRectangle.centerY() + tapResultSeparationFromCenterEnd
                },
                when (result.tapResult) {
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

    private fun getAdjustedHeight(width: Int) = (width * 0.5).toInt()

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, getAdjustedHeight(width), oldWidth, oldHeight)

        resizeRhythmWithErrorWindowsRectangle(width, height)
        resizeRhythmRectangle(width, height)
    }

    // The outer rectangle uses all the available space
    private fun resizeRhythmWithErrorWindowsRectangle(width: Int, height: Int) {
        val adjustedHeight = getAdjustedHeight(width)
        rhythmWithErrorWindowsRectangle.set(
            paddingLeft + horizontalInternalPadding,
            paddingTop.toFloat() + (height - adjustedHeight) / 2,
            width - (paddingRight + horizontalInternalPadding),
            height - (paddingBottom).toFloat() - (height - adjustedHeight) / 2
        )
    }

    // The inner rectangle leaves error windows at the start and end
    private fun resizeRhythmRectangle(width: Int, height: Int) {
        val adjustedHeight = getAdjustedHeight(width)
        val errorWindowSize = mode.successWindow * width
        rhythmRectangle.set(
            paddingLeft + horizontalInternalPadding + errorWindowSize,
            paddingTop.toFloat() + (height - adjustedHeight) / 2,
            width - (paddingRight + horizontalInternalPadding + errorWindowSize),
            height - (paddingBottom).toFloat() - (height - adjustedHeight) / 2
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
        currentMeasure = 1
        animator.start()
    }

    // Not directly used, called from status change
    private fun stopRhythm() {
        playerPhase = false
        animator.cancel()
        nativeStopRhythm()
    }

    // Not directly used, called from status change
    private fun resetAnimation() {
        animationProgress = 0f
        currentMeasure = 0
        playerPhase = mode.engineMeasures == 0
        playerInputTimings.clear()
        mistakeCount = 0
        invalidate()
    }

    fun doOnStatusChange(action: (status: Status) -> Unit) {
        actionOnStatusChange = action
    }

    fun doOnMistakeCountChange(action: (mistakeCount: Int) -> Unit) {
        actionOnMistakeCountChange = action
    }

    fun doOnCurrentMeasureChange(action: (currentMeasure: Int) -> Unit) {
        actionOnCurrentMeasureChange = action
    }

    fun doOnExerciseEnd(action: (success: Boolean, lastPlayedMeasure: Int) -> Unit) {
        actionOnExerciseEnd = action
    }

    // Called from JNI
    override fun onTapResult(tapResultNative: Int, tapTiming: Double, rhythmLineNative: Int, measure: Int) {
        val tapResult = TapResult.fromNativeValue(tapResultNative)
        val rhythmLine = RhythmLine.fromNativeValue(rhythmLineNative)
        // Save the timing to be painted
        if (tapResult in TapResult.Early..TapResult.Success) {
            playerInputTimings.add(TapResultWithTimingLineAndMeasure(tapResult, tapTiming, rhythmLine, measure))
            // Increase the mistake count if needed
            if (tapResult != TapResult.Success) {
                mistakeCount += 1
            }
        }
    }

    private external fun nativeStartRhythm()
    private external fun nativeStopRhythm()
}