package com.jorkoh.polyrhythmtrainer.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Badge::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "PolyrhythmTrainerDB"
    }

    abstract fun badgesDao(): BadgesDao
}