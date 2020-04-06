package com.jorkoh.polyrhythmtrainer.destinations.sounds

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.sound_row.*

class SoundAdapter(
    private val switchChanged: (Int, PadPosition) -> Unit
) : RecyclerView.Adapter<SoundAdapter.SoundViewHolder>() {

    inner class SoundViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        fun bind(sound: Sound, switchChanged: (Int, PadPosition) -> Unit) {
            sound_row_name_text.text = sound.displayName
            sound_row_enable_for_left_pad_switch.isChecked = sound.assignedToLeft
            sound_row_enable_for_right_pad_switch.isChecked = sound.assignedToRight

            sound_row_enable_for_left_pad_switch.setOnTouchListener { v, e ->
                if (e.action == MotionEvent.ACTION_DOWN && !(v as SwitchMaterial).isChecked) {
                    // Ignore the touch if it's already checked
                    switchChanged(sound.soundId, PadPosition.Left)
                    v.performClick()
                }
                true
            }

            sound_row_enable_for_right_pad_switch.setOnTouchListener { v, _ ->
                if (!(v as SwitchMaterial).isChecked) {
                    // Ignore the touch if it's already checked
                    switchChanged(sound.soundId, PadPosition.Right)
                    v.performClick()
                }
                true
            }
        }
    }

    private var sounds: List<Sound> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sound_row, parent, false)
        return SoundViewHolder(view)
    }

    override fun getItemCount() = sounds.size

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val sound = sounds[position]

        holder.bind(sound, switchChanged)
    }

    fun setNewSounds(newSounds: List<Sound>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = sounds.size

            override fun getNewListSize() = newSounds.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return sounds[oldItemPosition].soundId == newSounds[newItemPosition].soundId
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return sounds[oldItemPosition].assignedToLeft == newSounds[newItemPosition].assignedToLeft
                        && sounds[oldItemPosition].assignedToRight == newSounds[newItemPosition].assignedToRight
            }
        })

        sounds = newSounds
        diff.dispatchUpdatesTo(this)
    }
}