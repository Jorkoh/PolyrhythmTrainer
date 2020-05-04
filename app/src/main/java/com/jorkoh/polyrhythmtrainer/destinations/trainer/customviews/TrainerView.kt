package com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.repositories.Mode
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation
import kotlinx.android.synthetic.main.trainer_view.view.*

class TrainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val activeColor = ContextCompat.getColor(context, R.color.errorColor)
    private val inactiveColor = ContextCompat.getColor(context, R.color.primary)

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.trainer_view, this)
        trainer_view_polyrhythm_visualizer.parentTrainer = this
    }

    private var actionOnStatusChange: ((PolyrhythmVisualizer.Status) -> Unit)? = null

    var bpm = TrainerSettingsRepositoryImplementation.DEFAULT_BPM
        set(value) {
            trainer_view_polyrhythm_visualizer.bpm = value
            field = value
        }

    var xNumberOfBeats = TrainerSettingsRepositoryImplementation.DEFAULT_X_NUMBER_OF_BEATS
        set(value) {
            trainer_view_polyrhythm_visualizer.xNumberOfBeats = value
            field = value
            setupMistakeIcons()
        }

    var yNumberOfBeats = TrainerSettingsRepositoryImplementation.DEFAULT_Y_NUMBER_OF_BEATS
        set(value) {
            trainer_view_polyrhythm_visualizer.yNumberOfBeats = value
            field = value
            setupMistakeIcons()
        }

    var mode = Mode()
        set(value) {
            trainer_view_polyrhythm_visualizer.mode = value
            field = value
            setupMistakeIcons()
            setupMeasureViews()
        }

    private var mistakeCount = 0
        set(value) {
            if (field != value) {
                field = value
                colorMistakeIcons()
            }
        }

    private var currentMeasure = 1
        set(value) {
            if (field != value) {
                field = value
                colorMeasureViews()
            }
        }

    fun stop() {
        trainer_view_polyrhythm_visualizer.stop()
    }

    fun advanceToNextState() {
        trainer_view_polyrhythm_visualizer.advanceToNextState()
    }

    fun doOnStatusChange(action: (newStatus: PolyrhythmVisualizer.Status) -> Unit) {
        actionOnStatusChange = action
    }

    fun onStatusChange(currentStatus: PolyrhythmVisualizer.Status) {
        actionOnStatusChange?.invoke(currentStatus)
    }

    fun onMistakeCountChange(newMistakeCount: Int) {
        mistakeCount = newMistakeCount
    }

    fun onCurrentMeasureChange(newCurrentMeasure: Int) {
        currentMeasure = newCurrentMeasure
    }

    private fun setupMistakeIcons() {
        val allowedMistakesCount = when {
            mode.engineMeasures == -1 -> 0
            !mode.allowSomeMistakes -> 1
            else -> (xNumberOfBeats + yNumberOfBeats) / 2
        }

        trainer_view_mistakes_layout.removeAllViews()
        repeat(allowedMistakesCount) {
            View.inflate(context, R.layout.mistake_icon, trainer_view_mistakes_layout)
        }
        colorMistakeIcons()
    }

    private fun colorMistakeIcons() {
        repeat(trainer_view_mistakes_layout.childCount) { index ->
            val mistakeView = trainer_view_mistakes_layout.getChildAt(index) as ImageView
            if (mistakeCount >= index + 1) {
                mistakeView.alpha = 1f
                mistakeView.setColorFilter(activeColor)
                mistakeView.contentDescription = resources.getString(R.string.mistake_consumed)
            } else {
                mistakeView.alpha = 0.3f
                mistakeView.clearColorFilter()
                mistakeView.contentDescription = resources.getString(R.string.mistake_available)
            }
        }
    }

    private fun setupMeasureViews() {
        trainer_view_listen_layout.removeAllViews()
        if (mode.engineMeasures == -1) {
            View.inflate(context, R.layout.infinite_icon, trainer_view_listen_layout)

            trainer_view_separator_view.visibility = View.INVISIBLE
            trainer_view_user_icon.visibility = View.INVISIBLE
        } else {
            trainer_view_separator_view.visibility = View.VISIBLE
            trainer_view_user_icon.visibility = View.VISIBLE

            repeat(mode.engineMeasures) {
                View.inflate(context, R.layout.measure_view, trainer_view_listen_layout)
            }
        }

        trainer_view_user_layout.removeAllViews()
        if (mode.playerMeasures == -1) {
            View.inflate(context, R.layout.infinite_icon, trainer_view_user_layout)
        } else {
            repeat(mode.playerMeasures) {
                View.inflate(context, R.layout.measure_view, trainer_view_user_layout)
            }
        }
        colorMeasureViews()
    }

    // TODO have last bar not stay red at end of exercise
    private fun colorMeasureViews() {
        // Color the listen measures
        if (mode.engineMeasures == -1) {
            // Metronome  mode
            val infiniteListenView = trainer_view_listen_layout.getChildAt(0) as ImageView
            infiniteListenView.alpha = 1f
            infiniteListenView.setColorFilter(activeColor)

            // Since it's metronome mode "enable" the listen icon
            trainer_view_listen_icon.alpha = 1f
        } else {
            repeat(trainer_view_listen_layout.childCount) { index ->
                val measureView = trainer_view_listen_layout.getChildAt(index) as View
                when {
                    currentMeasure == index + 1 -> {
                        // Measure in play
                        measureView.alpha = 1f
                        measureView.setBackgroundColor(activeColor)

                        // If the measure in play is part of the listen measures "enable" the listen icon
                        trainer_view_listen_icon.alpha = 1f
                        trainer_view_user_icon.alpha = 0.3f
                    }
                    currentMeasure > index + 1 -> {
                        // Measures already played
                        measureView.alpha = 1f
                        measureView.setBackgroundColor(inactiveColor)
                    }
                    else -> {
                        // Measures not yet played
                        measureView.alpha = 0.3f
                        measureView.setBackgroundColor(inactiveColor)
                    }
                }
            }
        }

        // Color the player measures
        if (mode.playerMeasures == -1) {
            val infiniteUserView = trainer_view_user_layout.getChildAt(0) as ImageView
            if (currentMeasure > mode.engineMeasures) {
                infiniteUserView.alpha = 1f
                infiniteUserView.setColorFilter(activeColor)

                // If the measure in play is part of the user measures "enable" the user icon
                trainer_view_listen_icon.alpha = 1f
                trainer_view_user_icon.alpha = 1f
            } else {
                infiniteUserView.alpha = 0.3f
                infiniteUserView.clearColorFilter()
            }
        } else {
            repeat(trainer_view_user_layout.childCount) { index ->
                val measureView = trainer_view_user_layout.getChildAt(index) as View
                when {
                    currentMeasure - mode.engineMeasures == index + 1 -> {
                        // Measure in play
                        measureView.alpha = 1f
                        measureView.setBackgroundColor(activeColor)

                        // If the measure in play is part of the user measures "enable" the user icon
                        trainer_view_listen_icon.alpha = 1f
                        trainer_view_user_icon.alpha = 1f
                    }
                    currentMeasure - mode.engineMeasures > index + 1 -> {
                        // Measures already played
                        measureView.alpha = 1f
                        measureView.setBackgroundColor(inactiveColor)
                    }
                    else -> {
                        // Measures not yet played
                        measureView.alpha = 0.3f
                        measureView.setBackgroundColor(inactiveColor)
                    }
                }
            }
        }
    }
}