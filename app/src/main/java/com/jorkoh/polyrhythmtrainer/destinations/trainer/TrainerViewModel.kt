package com.jorkoh.polyrhythmtrainer.destinations.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.jorkoh.polyrhythmtrainer.repositories.PolyrhythmSettingsRepository
import com.jorkoh.polyrhythmtrainer.repositories.RhythmLine

class TrainerViewModel(private val polyrhythmSettingsRepository: PolyrhythmSettingsRepository) : ViewModel() {
    val bpm = polyrhythmSettingsRepository.getBPM().asLiveData()
    val xNumberOfBeats = polyrhythmSettingsRepository.getNumberOfBeats(RhythmLine.X).asLiveData()
    val yNumberOfBeats = polyrhythmSettingsRepository.getNumberOfBeats(RhythmLine.Y).asLiveData()

    fun changeBPM(newBpm: Int) {
        polyrhythmSettingsRepository.changeBPM(newBpm)
    }

    fun changeNumberOfBeats(isIncrease: Boolean, line: RhythmLine) {
        polyrhythmSettingsRepository.changeNumberOfBeats(isIncrease, line)
    }
}