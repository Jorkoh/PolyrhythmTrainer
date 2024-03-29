package com.jorkoh.polyrhythmtrainer.repositories

import com.jorkoh.polyrhythmtrainer.db.Badge
import com.jorkoh.polyrhythmtrainer.db.BadgesDao
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.min

interface BadgesRepository {
    fun getAllBadges(): Flow<List<Badge>>
    suspend fun addBadgeIfNeeded(badge: Badge)
    suspend fun resetBadges()
}

class BadgesRepositoryImplementation(private val badgesDao: BadgesDao) : BadgesRepository {

    override fun getAllBadges(): Flow<List<Badge>> = badgesDao.getAllBadges()

    override suspend fun addBadgeIfNeeded(badge: Badge) {
        if (isPolyrhythm(badge.xBeats, badge.yBeats)) {
            badgesDao.insertBadgeIfNewOrImproved(badge)
        }
    }

    override suspend fun resetBadges() {
        badgesDao.deleteBadges()
    }
}

fun Badge.isBetterThan(otherBadge: Badge) = this.completedMeasures > otherBadge.completedMeasures || this.bpm > otherBadge.bpm

fun isPolyrhythm(xBeats : Int, yBeats : Int) = max(xBeats, yBeats) % min(xBeats, yBeats) != 0