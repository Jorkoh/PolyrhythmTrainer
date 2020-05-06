package com.jorkoh.polyrhythmtrainer.db

import androidx.room.*
import com.jorkoh.polyrhythmtrainer.repositories.isBetterThan
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgesDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    @Query("SELECT * FROM badges WHERE xBeats = :xBeats AND yBeats = :yBeats AND modeId = :modeId")
    suspend fun getPreviousBadge(xBeats: Int, yBeats: Int, modeId: Int): Badge?

    @Update
    fun updateBadge(badge: Badge)

    @Transaction
    suspend fun insertBadgeIfNeeded(newBadge: Badge) {
        val previousBadge = getPreviousBadge(newBadge.xBeats, newBadge.yBeats, newBadge.modeId)

        if (previousBadge == null) {
            insertBadge(newBadge)
        } else if (newBadge.isBetterThan(previousBadge)) {
            previousBadge.completedMeasures = newBadge.completedMeasures
            previousBadge.bpm = newBadge.bpm
            previousBadge.achievedAt = newBadge.achievedAt
            updateBadge(previousBadge)
        }
    }

    @Query("DELETE FROM badges")
    suspend fun deleteBadges()
}