package com.countingstar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GreetingEntity::class,
        LedgerEntity::class,
        AccountEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun greetingDao(): GreetingDao

    abstract fun ledgerDao(): LedgerDao

    abstract fun accountDao(): AccountDao
}
