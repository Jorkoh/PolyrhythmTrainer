package com.jorkoh.polyrhythmtrainer.destinations.sounds

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.recyclerview.widget.RecyclerView
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import com.jorkoh.polyrhythmtrainer.repositories.Sound
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepositoryImplementation.Companion.LEFT_PAD_DEFAULT_SOUND
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepositoryImplementation.Companion.RIGHT_PAD_DEFAULT_SOUND
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.sound_row.*

class SoundAdapter(
    private val switchChanged: (Int, PadPosition) -> Unit
) : RecyclerView.Adapter<SoundAdapter.SoundViewHolder>() {

    inner class SoundViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(sound: Sound, switchChanged: (Int, PadPosition) -> Unit) {
            sound_row_name_text.text = containerView.context.getText(sound.displayNameResource)

            sound_row_enable_for_left_pad_switch.isChecked = sound.soundId == leftPadSoundId
            sound_row_enable_for_right_pad_switch.isChecked = sound.soundId == rightPadSoundId

            sound_row_enable_for_left_pad_switch.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !(view as Checkable).isChecked) {
                    // Ignore the touch if it's already checked
                    switchChanged(sound.soundId, PadPosition.Left)
                }
                true
            }

            sound_row_enable_for_right_pad_switch.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !(view as Checkable).isChecked) {
                    // Ignore the touch if it's already checked
                    switchChanged(sound.soundId, PadPosition.Right)
                }
                true
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    var sounds: List<Sound> = listOf()
    var leftPadSoundId: Int = LEFT_PAD_DEFAULT_SOUND
        set(value) {
            notifyItemChanged(sounds.indexOfFirst { it.soundId == field })
            notifyItemChanged(sounds.indexOfFirst { it.soundId == value })
            field = value
        }
    var rightPadSoundId: Int = RIGHT_PAD_DEFAULT_SOUND
        set(value) {
            notifyItemChanged(sounds.indexOfFirst { it.soundId == field })
            notifyItemChanged(sounds.indexOfFirst { it.soundId == value })
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sound_row, parent, false)
        return SoundViewHolder(view)
    }

    override fun getItemCount() = sounds.size

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val sound = sounds[position]

        holder.bind(sound, switchChanged)
    }

    override fun getItemId(position: Int): Long {
        return sounds[position].soundId.toLong()
    }
}