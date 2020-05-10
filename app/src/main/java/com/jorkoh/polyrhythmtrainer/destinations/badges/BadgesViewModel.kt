package com.jorkoh.polyrhythmtrainer.destinations.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.jorkoh.polyrhythmtrainer.db.Badge
import com.jorkoh.polyrhythmtrainer.repositories.BadgesRepository
import com.jorkoh.polyrhythmtrainer.repositories.ModeIds
import kotlinx.coroutines.flow.transformLatest

class BadgesViewModel(private val badgesRepository: BadgesRepository) : ViewModel() {

    val badgesGroupedByPolyrhythm = badgesRepository.getAllBadges()
        .transformLatest { badges ->
            emit(badges.groupBy { badge -> badge.xBeats * 100 + badge.yBeats }.map { (polyrhythm, badges) ->
                BadgesGroupedByPolyrhythm(
                    polyrhythm / 100,
                    polyrhythm % 100,
                    badges,
                    badges.any { it.modeId == ModeIds.IMPOSSIBLE.id })
            })
        }.asLiveData()
}

data class BadgesGroupedByPolyrhythm(
    val xBeats: Int,
    val yBeats: Int,
    val badges: List<Badge>,
    val hasImpossibleMode: Boolean
)