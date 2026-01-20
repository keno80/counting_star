package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GreetingDao {
    @Query("SELECT * FROM greeting WHERE id = 0")
    fun observeGreeting(): Flow<GreetingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GreetingEntity)
}
