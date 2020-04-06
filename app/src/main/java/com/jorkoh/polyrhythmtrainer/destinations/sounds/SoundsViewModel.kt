package com.jorkoh.polyrhythmtrainer.destinations.sounds

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.preference.PreferenceManager
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepository

class SoundsViewModel(private val soundsRepository: SoundsRepository) : ViewModel() {

    val sounds = soundsRepository.getSounds().asLiveData()

    fun changeSelectedSound(newSoundId : Int, position: PadPosition){
        soundsRepository.changePadSoundId(newSoundId, position)
    }
}