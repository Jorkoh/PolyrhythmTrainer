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

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.trainer_view, this)
        trainer_polyrhythm_visualizer.parentTrainer = this
    }

    private var actionOnStatusChange: ((PolyrhythmVisualizer.Status) -> Unit)? = null

    var bpm = TrainerSettingsRepositoryImplementation.DEFAULT_BPM
        set(value) {
            trainer_polyrhythm_visualizer.bpm = value
            field = value
        }

    var xNumberOfBeats = TrainerSettingsRepositoryImplementation.DEFAULT_X_NUMBER_OF_BEATS
        set(value) {
            trainer_polyrhythm_visualizer.xNumberOfBeats = value
            field = value
            setupMistakeIcons()
        }

    var yNumberOfBeats = TrainerSettingsRepositoryImplementation.DEFAULT_Y_NUMBER_OF_BEATS
        set(value) {
            trainer_polyrhythm_visualizer.yNumberOfBeats = value
            field = value
            setupMistakeIcons()
        }

    var mode = Mode()
        set(value) {
            trainer_polyrhythm_visualizer.mode = value
            field = value
            setupMistakeIcons()
        }

    private var mistakeCount = 0
        set(value) {
            if (field != value) {
                field = value
                colorMistakeIcons()
            }
        }

    fun stop() {
        trainer_polyrhythm_visualizer.stop()
    }

    fun advanceToNextState() {
        trainer_polyrhythm_visualizer.advanceToNextState()
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

    private fun setupMistakeIcons() {
        val allowedMistakesCount = when {
            mode.engineMeasures == -1 -> 0
            !mode.allowSomeMistakes -> 1
            else -> (xNumberOfBeats + yNumberOfBeats) / 2
        }

        trainer_view_mistakes_layout.removeAllViews()
        repeat(allowedMistakesCount) { _ ->
            View.inflate(context, R.layout.mistake_icon, trainer_view_mistakes_layout)
        }
        colorMistakeIcons()
    }

    private fun colorMistakeIcons() {
        repeat(trainer_view_mistakes_layout.childCount) { index ->
            val mistakeView = trainer_view_mistakes_layout.getChildAt(index) as ImageView
            if (mistakeCount >= index + 1) {
                mistakeView.alpha = 1f
                mistakeView.setColorFilter(ContextCompat.getColor(context, R.color.errorColor))
            } else {
                mistakeView.alpha = 0.3f
                mistakeView.clearColorFilter()
            }
        }
    }
}