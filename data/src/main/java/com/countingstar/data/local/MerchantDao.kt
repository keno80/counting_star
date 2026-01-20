package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchant WHERE ledgerId = :ledgerId ORDER BY name ASC")
    fun observeMerchantsByLedger(ledgerId: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchant WHERE id = :id LIMIT 1")
    suspend fun getMerchantById(id: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(merchant: MerchantEntity)

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Query("DELETE FROM merchant WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT * FROM merchant " +
            "WHERE ledgerId = :ledgerId AND (" +
            "name LIKE '%' || :keyword || '%' OR " +
            "alias LIKE '%' || :keyword || '%'" +
            ") " +
            "ORDER BY name ASC",
    )
    fun observeMerchantsByKeyword(
        ledgerId: String,
        keyword: String,
    ): Flow<List<MerchantEntity>>
}
