package com.jorkoh.polyrhythmtrainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepository

class MainActivityViewModel(private val soundsRepository: SoundsRepository) : ViewModel() {

    val leftPadSound = soundsRepository.getPadSound(PadPosition.Left).asLiveData()
    val rightPadSound = soundsRepository.getPadSound(PadPosition.Right).asLiveData()
}