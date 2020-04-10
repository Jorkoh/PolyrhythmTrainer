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
    fun getPadSound(position: PadPosition): Flow<Sound>
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
            Sound(1, "Tom 1", "tom1.wav", assignedToLeft = true, assignedToRight = false),
            Sound(2, "Tom 2", "tom2.wav", assignedToLeft = false, assignedToRight = false),
            Sound(3, "Shaker 1", "shaker1.wav", assignedToLeft = false, assignedToRight = true),
            Sound(4, "Shaker 2", "shaker2.wav", assignedToLeft = false, assignedToRight = false),
            Sound(5, "Stick", "stick.wav", assignedToLeft = false, assignedToRight = false),
            Sound(6, "Rimshot", "rimshot.wav", assignedToLeft = false, assignedToRight = false),
            Sound(7, "Clap", "clap.wav", assignedToLeft = false, assignedToRight = false),
            Sound(8, "Can", "can.wav", assignedToLeft = false, assignedToRight = false),
            Sound(9, "Bongo", "bongo.wav", assignedToLeft = false, assignedToRight = false)
        )
    }

    val sounds = defaultSounds.toMutableList()

    override fun getSounds() =
        flow {
            // Observe the flows for both left and right pads
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

    // TODO this is a bit sketchy, I'm not using the sound from the mutable list since
    //  its not clear if the assignedTo will be properly changed
    override fun getPadSound(position: PadPosition): Flow<Sound> =
        when (position) {
            PadPosition.Left -> preferences.getInt(LEFT_PAD_SOUND_ID, LEFT_PAD_DEFAULT_SOUND).asFlow()
                .transform { soundId ->
                    defaultSounds.first { it.soundId == soundId }.apply {
                        emit(Sound(soundId, displayName, resourceName, assignedToLeft = true, assignedToRight = false))
                    }
                }
            PadPosition.Right -> preferences.getInt(RIGHT_PAD_SOUND_ID, RIGHT_PAD_DEFAULT_SOUND).asFlow()
                .transform { soundId ->
                    defaultSounds.first { it.soundId == soundId }.apply {
                        emit(Sound(soundId, displayName, resourceName, assignedToLeft = false, assignedToRight = true))
                    }
                }
        }

    private fun getPadSoundId(position: PadPosition) =
        when (position) {
            PadPosition.Left -> preferences.getInt(LEFT_PAD_SOUND_ID, LEFT_PAD_DEFAULT_SOUND).asFlow()
                .transform { emit(Pair(PadPosition.Left, it)) }
            PadPosition.Right -> preferences.getInt(RIGHT_PAD_SOUND_ID, RIGHT_PAD_DEFAULT_SOUND).asFlow()
                .transform { emit(Pair(PadPosition.Right, it)) }
        }

    override fun changePadSoundId(newId: Int, position: PadPosition) {
        if (newId in defaultSounds.map { it.soundId }) {
            when (position) {
                PadPosition.Left -> preferences.sharedPreferences.edit {
                    putInt(LEFT_PAD_SOUND_ID, newId)
                }
                PadPosition.Right -> preferences.sharedPreferences.edit {
                    putInt(RIGHT_PAD_SOUND_ID, newId)
                }
            }
        }
    }
}