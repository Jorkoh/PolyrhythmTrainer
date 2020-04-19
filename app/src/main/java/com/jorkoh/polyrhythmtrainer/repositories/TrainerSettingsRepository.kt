package com.jorkoh.polyrhythmtrainer.repositories

import com.jorkoh.polyrhythmtrainer.R
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

enum class RhythmLine(val nativeValue: Int) {
    X(1),
    Y(0);

    companion object {
        fun fromNativeValue(nativeValue: Int) =
            values().first { it.nativeValue == nativeValue }
    }
}

data class Mode(
    val modeId: Int,
    val displayNameResource: Int,
    val isMetronome: Boolean
)

interface TrainerSettingsRepository {
    fun getBPM(): Flow<Int>
    fun getNumberOfBeats(rhythmLine: RhythmLine): Flow<Int>
    fun getModes(): List<Mode>
    fun getMode(): Flow<Mode>
    fun changeBPM(newBPM: Int)
    fun changeNumberOfBeats(isIncrease: Boolean, rhythmLine: RhythmLine)
    fun changeMode(newModeId: Int)
}

class TrainerSettingsRepositoryImplementation(private val preferences: FlowSharedPreferences) :
    TrainerSettingsRepository {

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

        const val MODE_KEY = "MODE"
        const val DEFAULT_MODE_ID = 1

        // TODO this is a bit messy because it relies on the ids being the same as the ones on modes_array
        private val modes = listOf(
            Mode(1, R.string.mode_metronome, true),
            Mode(2, R.string.mode_easy, false),
            Mode(3, R.string.mode_medium, false),
            Mode(4, R.string.mode_hard, false),
            Mode(5, R.string.mode_impossible, false)
        )
    }

    private val bpmPref = preferences.getInt(BPM_KEY, DEFAULT_BPM)
    private val xNumberOfBeatsPref = preferences.getInt(X_NUMBER_OF_BEATS_KEY, DEFAULT_X_NUMBER_OF_BEATS)
    private val yNumberOfBeatsPref = preferences.getInt(Y_NUMBER_OF_BEATS_KEY, DEFAULT_Y_NUMBER_OF_BEATS)
    private val modeIdPref = preferences.getInt(MODE_KEY, DEFAULT_MODE_ID)

    override fun getBPM(): Flow<Int> = bpmPref.asFlow()

    override fun getNumberOfBeats(rhythmLine: RhythmLine): Flow<Int> =
        when (rhythmLine) {
            RhythmLine.X -> xNumberOfBeatsPref.asFlow()
            RhythmLine.Y -> yNumberOfBeatsPref.asFlow()
        }

    override fun getModes(): List<Mode> = modes

    override fun getMode(): Flow<Mode> =
        modeIdPref.asFlow().transform { modeId -> emit(modes.first { it.modeId == modeId }) }

    override fun changeBPM(newBPM: Int) {
        if (newBPM.isValidBPM()) {
            bpmPref.set(newBPM)
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

        if (newNumberOfBeats.isValidNumberOfBeats()) {
            when (rhythmLine) {
                RhythmLine.X -> xNumberOfBeatsPref.set(newNumberOfBeats)
                RhythmLine.Y -> yNumberOfBeatsPref.set(newNumberOfBeats)
            }
        }
    }

    override fun changeMode(newModeId: Int) {
        if (newModeId.isValidModeId()) {
            modeIdPref.set(newModeId)
        }
    }

    private fun Int.isValidBPM() = this in MIN_BPM..MAX_BPM

    private fun Int.isValidNumberOfBeats() = this in MIN_NUMBER_OF_BEATS..MAX_NUMBER_OF_BEATS

    private fun Int.isValidModeId() = this in modes.map { it.modeId }
}