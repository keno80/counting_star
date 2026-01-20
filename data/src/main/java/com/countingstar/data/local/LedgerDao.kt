package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger ORDER BY createdAt DESC")
    fun observeLedgers(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledger WHERE id = :id LIMIT 1")
    suspend fun getLedgerById(id: String): LedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ledger: LedgerEntity)

    @Update
    suspend fun update(ledger: LedgerEntity)

    @Query("DELETE FROM ledger WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM ledger WHERE isDefault = 1 LIMIT 1")
    fun observeDefaultLedger(): Flow<LedgerEntity?>

    @Query("UPDATE ledger SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE ledger SET isDefault = 1 WHERE id = :ledgerId")
    suspend fun setDefaultLedger(ledgerId: String)
}
