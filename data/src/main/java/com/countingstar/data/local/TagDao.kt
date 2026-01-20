package com.countingstar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tag WHERE ledgerId = :ledgerId ORDER BY name ASC")
    fun observeTagsByLedger(ledgerId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT tag.* FROM tag " +
            "INNER JOIN transaction_tag ON tag.id = transaction_tag.tagId " +
            "WHERE transaction_tag.transactionId = :transactionId " +
            "ORDER BY tag.name ASC",
    )
    fun observeTagsByTransaction(transactionId: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactionTags(crossRefs: List<TransactionTagCrossRef>)

    @Query("DELETE FROM transaction_tag WHERE transactionId = :transactionId")
    suspend fun deleteTransactionTagsByTransaction(transactionId: String)
}
