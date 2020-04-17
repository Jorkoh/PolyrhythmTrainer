package com.jorkoh.polyrhythmtrainer.repositories

import androidx.core.content.edit
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow

enum class RhythmLine(val nativeValue: Int) {
    X(1),
    Y(0);

    companion object {
        fun fromNativeValue(nativeValue: Int) =
            values().first { it.nativeValue == nativeValue }
    }
}

interface PolyrhythmSettingsRepository {
    fun getBPM(): Flow<Int>
    fun getNumberOfBeats(rhythmLine: RhythmLine): Flow<Int>
    fun changeBPM(newBPM: Int)
    fun changeNumberOfBeats(isIncrease: Boolean, rhythmLine: RhythmLine)
}

class PolyrhythmSettingsRepositoryImplementation(private val preferences: FlowSharedPreferences) :
    PolyrhythmSettingsRepository {

    companion object {
        const val BPM_KEY = "BPM"
        const val MAX_BPM = 300
        const val MIN_BPM = 30
        const val DEFAULT_BPM = 80

        const val X_NUMBER_OF_BEATS_KEY = "X_NUMBER_OF_BEATS"
        const val Y_NUMBER_OF_BEATS_KEY = "Y_NUMBER_OF_BEATS"
        const val MIN_NUMBER_OF_BEATS = 1
        const val MAX_NUMBER_OF_BEATS = 14
        const val DEFAULT_X_NUMBER_OF_BEATS = 3
        const val DEFAULT_Y_NUMBER_OF_BEATS = 4
    }

    private val bpmPref = preferences.getInt(BPM_KEY, DEFAULT_BPM)
    private val xNumberOfBeatsPref = preferences.getInt(X_NUMBER_OF_BEATS_KEY, DEFAULT_X_NUMBER_OF_BEATS)
    private val yNumberOfBeatsPref = preferences.getInt(Y_NUMBER_OF_BEATS_KEY, DEFAULT_Y_NUMBER_OF_BEATS)

    private fun Int.isValidBPM() = this in MIN_BPM..MAX_BPM

    private fun Int.isValidNumberOfBeats() = this in MIN_NUMBER_OF_BEATS..MAX_NUMBER_OF_BEATS

    override fun getBPM(): Flow<Int> = bpmPref.asFlow()

    override fun getNumberOfBeats(rhythmLine: RhythmLine): Flow<Int> =
        when (rhythmLine) {
            RhythmLine.X -> xNumberOfBeatsPref.asFlow()
            RhythmLine.Y -> yNumberOfBeatsPref.asFlow()
        }

    override fun changeBPM(newBPM: Int) {
        if (newBPM.isValidBPM()) {
            preferences.getInt(BPM_KEY, DEFAULT_BPM).set(newBPM)
        }
    }

    override fun changeNumberOfBeats(isIncrease: Boolean, rhythmLine: RhythmLine) {
        val increment = if (isIncrease) {
            1
        } else {
            -1
        }

        val newNumberOfBeats = when (rhythmLine) {
            RhythmLine.X -> xNumberOfBeatsPref.get() + increment
            RhythmLine.Y -> yNumberOfBeatsPref.get() + increment
        }

        if(newNumberOfBeats.isValidNumberOfBeats()){
            when (rhythmLine) {
                RhythmLine.X -> xNumberOfBeatsPref.set(newNumberOfBeats)
                RhythmLine.Y -> yNumberOfBeatsPref.set(newNumberOfBeats)
            }
        }
    }
}