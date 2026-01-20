package com.countingstar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GreetingEntity::class,
        LedgerEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        MerchantEntity::class,
        TransactionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun greetingDao(): GreetingDao

    abstract fun ledgerDao(): LedgerDao

    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    abstract fun merchantDao(): MerchantDao

    abstract fun transactionDao(): TransactionDao
}
