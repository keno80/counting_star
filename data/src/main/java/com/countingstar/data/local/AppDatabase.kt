package com.countingstar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GreetingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun greetingDao(): GreetingDao
}
