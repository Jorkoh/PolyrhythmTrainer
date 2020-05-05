package com.jorkoh.polyrhythmtrainer.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgesDao {
    @Query("SELECT * FROM badges")
    fun getAllBadges() : Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    @Query("DELETE FROM badges")
    suspend fun deleteBadges()
}