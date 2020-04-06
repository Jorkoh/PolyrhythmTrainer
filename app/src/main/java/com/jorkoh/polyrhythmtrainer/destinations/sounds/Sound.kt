package com.jorkoh.polyrhythmtrainer.destinations.sounds

import com.jorkoh.polyrhythmtrainer.destinations.PadPosition

data class Sound(
    val soundId: Int,
    val displayName: String,
    val resourceName: String,
    var assignedToLeft: Boolean,
    var assignedToRight: Boolean
)