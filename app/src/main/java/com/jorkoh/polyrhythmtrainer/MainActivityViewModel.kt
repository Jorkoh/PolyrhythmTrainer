package com.jorkoh.polyrhythmtrainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepository

class MainActivityViewModel(private val soundsRepository: SoundsRepository) : ViewModel(){

    val sounds = soundsRepository.getSounds().asLiveData()
}