package com.jorkoh.polyrhythmtrainer.destinations.badges

import androidx.lifecycle.ViewModel
import com.jorkoh.polyrhythmtrainer.db.Badge
import com.jorkoh.polyrhythmtrainer.repositories.BadgesRepository
import kotlinx.coroutines.flow.transformLatest

class BadgesViewModel(val badgesRepository: BadgesRepository) : ViewModel() {
    val badges = badgesRepository.getAllBadges()
        .transformLatest<List<Badge>, Map<String, List<Badge>>> { badges ->
            badges.groupBy { badge -> badge.xBeats.toString() + ":" + badge.yBeats.toString() }
        }
}