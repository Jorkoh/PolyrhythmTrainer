package com.jorkoh.polyrhythmtrainer.destinations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrainerViewModel : ViewModel() {
    private val polyrhythmSettings =
        MutableLiveData(PolyrhythmSettings())

    fun getPolyrhythmSettings(): LiveData<PolyrhythmSettings> = polyrhythmSettings

    fun changeBPM(newBPM: Int) {
        if (newBPM.isValidBPM()) {
            polyrhythmSettings.mutate {
                value?.BPM = newBPM
            }
        }
    }

    fun changeNumberOfBeats(isIncrease: Boolean, line: RhythmLine) {
        val increment = if (isIncrease) {
            1
        } else {
            -1
        }

        val newNumberOfBeats = when (line) {
            RhythmLine.X -> polyrhythmSettings.value?.xNumberOfBeats?.plus(increment)
            RhythmLine.Y -> polyrhythmSettings.value?.yNumberOfBeats?.plus(increment)
        }

        if (newNumberOfBeats != null && newNumberOfBeats.isValidNumberOfBeats()) {
            when (line) {
                RhythmLine.X -> polyrhythmSettings.mutate {
                    value?.xNumberOfBeats = newNumberOfBeats
                }
                RhythmLine.Y -> polyrhythmSettings.mutate {
                    value?.yNumberOfBeats = newNumberOfBeats
                }
            }
        }
    }
}