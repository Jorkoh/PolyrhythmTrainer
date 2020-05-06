package com.jorkoh.polyrhythmtrainer.destinations.trainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.jorkoh.polyrhythmtrainer.db.Badge
import com.jorkoh.polyrhythmtrainer.repositories.BadgesRepository
import com.jorkoh.polyrhythmtrainer.repositories.RhythmLine
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepository
import kotlinx.coroutines.launch

class TrainerViewModel(
    private val trainerSettingsRepository: TrainerSettingsRepository,
    private val badgesRepository: BadgesRepository
) : ViewModel() {
    // TODO listen for new badges, maybe update badge icon when there are new ones
    val bpm = trainerSettingsRepository.getBPM().asLiveData()
    val xNumberOfBeats = trainerSettingsRepository.getNumberOfBeats(RhythmLine.X).asLiveData()
    val yNumberOfBeats = trainerSettingsRepository.getNumberOfBeats(RhythmLine.Y).asLiveData()
    val mode = trainerSettingsRepository.getMode().asLiveData()
    val modes = trainerSettingsRepository.getModes()

    fun changeBPM(newBpm: Int) {
        trainerSettingsRepository.changeBPM(newBpm)
    }

    fun changeNumberOfBeats(isIncrease: Boolean, line: RhythmLine) {
        trainerSettingsRepository.changeNumberOfBeats(isIncrease, line)
    }

    fun changeMode(newModeId: Int) {
        trainerSettingsRepository.changeMode(newModeId)
    }

    fun addBadge(badge: Badge) {
        viewModelScope.launch {
            badgesRepository.addBadge(badge)
        }
    }
}