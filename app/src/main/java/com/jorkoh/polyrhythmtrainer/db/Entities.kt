package com.jorkoh.polyrhythmtrainer.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

//TODO add "seen" field to showcase badges earned since the badges screen was last open?
// could also be used to color the badges icon on main screen when there are new badges?
@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "badgeId")
    var badgeId: Int,

    @ColumnInfo(name = "xBeats")
    var xBeats: Int,

    @ColumnInfo(name = "yBeats")
    var yBeats: Int,

    @ColumnInfo(name = "bpm")
    var bpm: Int,

    @ColumnInfo(name = "modeId")
    var modeId: Int,

    @ColumnInfo(name = "completedMeasures")
    var completedMeasures: Int,

    @ColumnInfo(name = "achievedAt")
    var achievedAt: Date
)