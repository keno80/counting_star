package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionSplitDao {
    @Query(
        "SELECT * FROM transaction_split " +
            "WHERE transactionId = :transactionId " +
            "ORDER BY id ASC",
    )
    fun observeSplitsByTransaction(transactionId: String): Flow<List<TransactionSplitEntity>>

    @Query("SELECT * FROM transaction_split WHERE id = :id LIMIT 1")
    suspend fun getSplitById(id: String): TransactionSplitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(split: TransactionSplitEntity)

    @Update
    suspend fun update(split: TransactionSplitEntity)

    @Query("DELETE FROM transaction_split WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transaction_split WHERE transactionId = :transactionId")
    suspend fun deleteByTransaction(transactionId: String)

    @Query(
        "SELECT IFNULL(SUM(amount), 0) FROM transaction_split " +
            "WHERE transactionId = :transactionId",
    )
    suspend fun getSplitAmountSum(transactionId: String): Long
}
