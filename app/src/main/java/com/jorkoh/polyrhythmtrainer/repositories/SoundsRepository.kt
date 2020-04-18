package com.jorkoh.polyrhythmtrainer.repositories

import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

data class Sound(
    val soundId: Int,
    val displayNameResource: Int,
    val resourceName: String
)

interface SoundsRepository {
    fun getSounds(): List<Sound>
    fun getPadSound(position: PadPosition): Flow<Sound>
    fun changePadSoundId(newId: Int, position: PadPosition)
}

class SoundsRepositoryImplementation(private val preferences: FlowSharedPreferences) : SoundsRepository {

    companion object {
        const val LEFT_PAD_SOUND_ID_KEY = "LEFT_PAD_SOUND_ID"
        const val RIGHT_PAD_SOUND_ID_KEY = "RIGHT_PAD_SOUND_ID"
        const val LEFT_PAD_DEFAULT_SOUND = 1
        const val RIGHT_PAD_DEFAULT_SOUND = 2

        //TODO this display names have to be string resources to support translation
        val sounds = listOf(
            Sound(1, R.string.sound_name_tom_1, "tom1.wav"),
            Sound(2, R.string.sound_name_tom_2, "tom2.wav"),
            Sound(3, R.string.sound_name_shaker_1, "shaker1.wav"),
            Sound(4, R.string.sound_name_shaker_2, "shaker2.wav"),
            Sound(5, R.string.sound_name_stick, "stick.wav"),
            Sound(6, R.string.sound_name_rimshot, "rimshot.wav"),
            Sound(7, R.string.sound_name_clap, "clap.wav"),
            Sound(8, R.string.sound_name_can, "can.wav"),
            Sound(9, R.string.sound_name_bongo, "bongo.wav")
        )
    }

    private val leftPadSoundIdPref = preferences.getInt(LEFT_PAD_SOUND_ID_KEY, LEFT_PAD_DEFAULT_SOUND)
    private val rightPadSoundIdPref = preferences.getInt(RIGHT_PAD_SOUND_ID_KEY, RIGHT_PAD_DEFAULT_SOUND)

    override fun getSounds() = sounds

    override fun getPadSound(position: PadPosition): Flow<Sound> =
        when (position) {
            PadPosition.Left -> leftPadSoundIdPref.asFlow()
                .transform { soundId -> emit(sounds.first { it.soundId == soundId }) }
            PadPosition.Right -> rightPadSoundIdPref.asFlow()
                .transform { soundId -> emit(sounds.first { it.soundId == soundId }) }
        }

    override fun changePadSoundId(newId: Int, position: PadPosition) {
        if (newId.isValidSoundId()) {
            when (position) {
                PadPosition.Left -> leftPadSoundIdPref.set(newId)
                PadPosition.Right -> rightPadSoundIdPref.set(newId)
            }
        }
    }

    private fun Int.isValidSoundId() = this in sounds.map { it.soundId }
}