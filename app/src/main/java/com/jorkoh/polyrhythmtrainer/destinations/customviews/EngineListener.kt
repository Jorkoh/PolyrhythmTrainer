package com.jorkoh.polyrhythmtrainer.destinations.customviews

import com.jorkoh.polyrhythmtrainer.destinations.RhythmLine

interface EngineListener {
    // tapTiming is a fraction of the total rhythm length
    fun onTapResult(tapResultNative: Int, tapTiming: Double, rhythmLineNative: Int)

    enum class TapResult(val nativeValue: Int) {
        Error(0),
        Ignored(1),
        Early(2),
        Late(3),
        Success(4);

        companion object {
            fun fromNativeValue(nativeValue: Int) = values().first { it.nativeValue == nativeValue }
        }
    }
}

