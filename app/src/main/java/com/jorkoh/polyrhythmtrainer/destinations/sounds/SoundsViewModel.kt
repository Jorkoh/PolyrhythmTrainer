package com.jorkoh.polyrhythmtrainer.destinations.sounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepository

class SoundsViewModel(private val soundsRepository: SoundsRepository) : ViewModel() {

    val sounds = soundsRepository.getSounds().asLiveData()
    val leftPadSound = soundsRepository.getPadSound(PadPosition.Left).asLiveData()
    val rightPadSound = soundsRepository.getPadSound(PadPosition.Right).asLiveData()

    fun changeSelectedSound(newSoundId: Int, position: PadPosition) {
        soundsRepository.changePadSoundId(newSoundId, position)
    }
}