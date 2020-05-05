package com.jorkoh.polyrhythmtrainer.repositories

import com.jorkoh.polyrhythmtrainer.db.Badge
import com.jorkoh.polyrhythmtrainer.db.BadgesDao
import kotlinx.coroutines.flow.Flow

interface BadgesRepository {
    fun getAllBadges(): Flow<List<Badge>>
    suspend fun addBadge(badge: Badge)
    suspend fun resetBadges()
}

class BadgesRepositoryImplementation(private val badgesDao: BadgesDao) : BadgesRepository {

    override fun getAllBadges(): Flow<List<Badge>> = badgesDao.getAllBadges()

    override suspend fun addBadge(badge: Badge) {
        badgesDao.insertBadge(badge)
    }

    override suspend fun resetBadges() {
        badgesDao.deleteBadges()
    }
}