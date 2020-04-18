package com.jorkoh.polyrhythmtrainer.destinations.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepository
import com.jorkoh.polyrhythmtrainer.repositories.RhythmLine

class TrainerViewModel(private val trainerSettingsRepository: TrainerSettingsRepository) : ViewModel() {
    val bpm = trainerSettingsRepository.getBPM().asLiveData()
    val xNumberOfBeats = trainerSettingsRepository.getNumberOfBeats(RhythmLine.X).asLiveData()
    val yNumberOfBeats = trainerSettingsRepository.getNumberOfBeats(RhythmLine.Y).asLiveData()
    val mode = trainerSettingsRepository.getMode().asLiveData()

    fun changeBPM(newBpm: Int) {
        trainerSettingsRepository.changeBPM(newBpm)
    }

    fun changeNumberOfBeats(isIncrease: Boolean, line: RhythmLine) {
        trainerSettingsRepository.changeNumberOfBeats(isIncrease, line)
    }

    fun changeMode(newModeId: Int){
        trainerSettingsRepository.changeMode(newModeId)
    }
}