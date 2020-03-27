package com.jorkoh.polyrhythmtrainer.ui

data class PolyrhythmSettings(
    var BPM: Int = 80,
    var xNumberOfBeats: Int = 3,
    var yNumberOfBeats: Int = 4
)

enum class RhythmLine {
    X, Y
}

const val MAX_BPM = 200
const val MIN_BPM = 30
fun Int.isValidBPM() = this in MIN_BPM..MAX_BPM

const val MIN_NUMBER_OF_BEATS = 1
const val MAX_NUMBER_OF_BEATS = 14
fun Int.isValidNumberOfBeats() = this in MIN_NUMBER_OF_BEATS..MAX_NUMBER_OF_BEATS
