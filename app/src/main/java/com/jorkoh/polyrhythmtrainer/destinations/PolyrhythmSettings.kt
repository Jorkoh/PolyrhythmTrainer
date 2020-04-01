package com.jorkoh.polyrhythmtrainer.destinations

import com.jorkoh.polyrhythmtrainer.destinations.customviews.EngineListener

data class PolyrhythmSettings(
    var BPM: Int = 80,
    var xNumberOfBeats: Int = 3,
    var yNumberOfBeats: Int = 4
)

enum class RhythmLine(val nativeValue: Int) {
    X(1),
    Y(0);

    companion object {
        fun fromNativeValue(nativeValue: Int) =
            values().first { it.nativeValue == nativeValue }
    }
}

const val MAX_BPM = 300
const val MIN_BPM = 30
fun Int.isValidBPM() = this in MIN_BPM..MAX_BPM

const val MIN_NUMBER_OF_BEATS = 1
const val MAX_NUMBER_OF_BEATS = 14
fun Int.isValidNumberOfBeats() = this in MIN_NUMBER_OF_BEATS..MAX_NUMBER_OF_BEATS
