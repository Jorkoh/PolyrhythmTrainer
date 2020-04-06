package com.jorkoh.polyrhythmtrainer.repositories

import androidx.core.content.edit
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import com.jorkoh.polyrhythmtrainer.destinations.sounds.Sound
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

interface SoundsRepository {
    fun getSounds(): Flow<List<Sound>>
    fun getPadSoundId(position: PadPosition): Flow<Pair<PadPosition, Int>>
    fun changePadSoundId(newId: Int, position: PadPosition)
}

@FlowPreview
@ExperimentalCoroutinesApi
class SoundsRepositoryImplementation(private val preferences: FlowSharedPreferences) : SoundsRepository {

    companion object {
        const val LEFT_PAD_SOUND_ID = "LEFT_PAD_SOUND_ID"
        const val RIGHT_PAD_SOUND_ID = "RIGHT_PAD_SOUND_ID"
        const val LEFT_PAD_DEFAULT_SOUND = 1
        const val RIGHT_PAD_DEFAULT_SOUND = 2

        val defaultSounds = listOf(
            Sound(1, "Sound 1", "sound1.mp4", assignedToLeft = true, assignedToRight = false),
            Sound(2, "Sound 2", "sound2.mp4", assignedToLeft = false, assignedToRight = true),
            Sound(3, "Sound 3", "sound3.mp4", assignedToLeft = false, assignedToRight = false),
            Sound(4, "Sound 4", "sound4.mp4", assignedToLeft = false, assignedToRight = false),
            Sound(5, "Sound 5", "sound5.mp4", assignedToLeft = false, assignedToRight = false),
            Sound(6, "Sound 6", "sound6.mp4", assignedToLeft = false, assignedToRight = false),
            Sound(7, "Sound 7", "sound7.mp4", assignedToLeft = false, assignedToRight = false),
            Sound(8, "Sound 8", "sound8.mp4", assignedToLeft = false, assignedToRight = false)
        )
    }

    override fun getSounds() =
        flow {
            val sounds = defaultSounds.toMutableList()

            flowOf(getPadSoundId(PadPosition.Left), getPadSoundId(PadPosition.Right))
                .flattenMerge()
                .collect { padPositionAndSoundId ->
                    val padPosition = padPositionAndSoundId.first
                    val soundId = padPositionAndSoundId.second

                    // Unassign the previous one if it existed and assign the position to the sound
                    when (padPosition) {
                        PadPosition.Left -> sounds.forEach { it.assignedToLeft = it.soundId == soundId }
                        PadPosition.Right -> sounds.forEach { it.assignedToRight = it.soundId == soundId }
                    }
                    // Emit a copy of the sounds
                    emit(ArrayList(sounds.map { it.copy() }))
                }
        }

    override fun getPadSoundId(position: PadPosition) =
        when (position) {
            PadPosition.Left -> preferences.getInt(LEFT_PAD_SOUND_ID, LEFT_PAD_DEFAULT_SOUND).asFlow()
                .transform { emit(Pair(PadPosition.Left, it)) }
            PadPosition.Right -> preferences.getInt(RIGHT_PAD_SOUND_ID, RIGHT_PAD_DEFAULT_SOUND).asFlow()
                .transform { emit(Pair(PadPosition.Right, it)) }
        }

    override fun changePadSoundId(newId: Int, position: PadPosition) {
        if (newId in defaultSounds.map { it.soundId }) {
            when (position) {
                PadPosition.Left -> preferences.sharedPreferences.edit(commit = true) {
                    putInt(LEFT_PAD_SOUND_ID, newId)
                }
                PadPosition.Right -> preferences.sharedPreferences.edit(commit = true) {
                    putInt(RIGHT_PAD_SOUND_ID, newId)
                }
            }
        }
    }
}