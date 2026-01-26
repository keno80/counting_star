package com.countingstar.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `transaction` WHERE ledgerId = :ledgerId ORDER BY occurredAt DESC")
    fun observeTransactionsByLedger(ledgerId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transaction` WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM `transaction` WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT DISTINCT t.* FROM `transaction` t " +
            "LEFT JOIN transaction_tag tt ON t.id = tt.transactionId " +
            "LEFT JOIN tag tg ON tg.id = tt.tagId AND tg.ledgerId = t.ledgerId " +
            "LEFT JOIN merchant m ON t.merchantId = m.id AND m.ledgerId = t.ledgerId " +
            "WHERE t.ledgerId = :ledgerId " +
            "AND (:startTime IS NULL OR t.occurredAt >= :startTime) " +
            "AND (:endTime IS NULL OR t.occurredAt <= :endTime) " +
            "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
            "AND (:hasAccountIds = 0 OR t.accountId IN (:accountIds) " +
            "OR t.fromAccountId IN (:accountIds) OR t.toAccountId IN (:accountIds)) " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId " +
            "OR t.categoryId IN (SELECT id FROM category WHERE parentId = :categoryId AND ledgerId = :ledgerId)) " +
            "AND (:merchantId IS NULL OR t.merchantId = :merchantId) " +
            "AND (:tagId IS NULL OR tt.tagId = :tagId) " +
            "AND (:keyword IS NULL OR t.note LIKE '%' || :keyword || '%' " +
            "OR m.name LIKE '%' || :keyword || '%' OR m.alias LIKE '%' || :keyword || '%'" +
            " OR tg.name LIKE '%' || :keyword || '%') " +
            "ORDER BY t.occurredAt DESC",
    )
    fun observeTransactionsByFilters(
        ledgerId: String,
        startTime: Long? = null,
        endTime: Long? = null,
        minAmount: Long? = null,
        maxAmount: Long? = null,
        accountIds: List<String>? = null,
        hasAccountIds: Int = if (accountIds.isNullOrEmpty()) 0 else 1,
        categoryId: String? = null,
        tagId: String? = null,
        merchantId: String? = null,
        keyword: String? = null,
    ): Flow<List<TransactionEntity>>

    @Query(
        "SELECT DISTINCT t.* FROM `transaction` t " +
            "LEFT JOIN transaction_tag tt ON t.id = tt.transactionId " +
            "LEFT JOIN tag tg ON tg.id = tt.tagId AND tg.ledgerId = t.ledgerId " +
            "LEFT JOIN merchant m ON t.merchantId = m.id AND m.ledgerId = t.ledgerId " +
            "WHERE t.ledgerId = :ledgerId " +
            "AND (:startTime IS NULL OR t.occurredAt >= :startTime) " +
            "AND (:endTime IS NULL OR t.occurredAt <= :endTime) " +
            "AND (:minAmount IS NULL OR t.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR t.amount <= :maxAmount) " +
            "AND (:hasAccountIds = 0 OR t.accountId IN (:accountIds) " +
            "OR t.fromAccountId IN (:accountIds) OR t.toAccountId IN (:accountIds)) " +
            "AND (:categoryId IS NULL OR t.categoryId = :categoryId " +
            "OR t.categoryId IN (SELECT id FROM category WHERE parentId = :categoryId AND ledgerId = :ledgerId)) " +
            "AND (:merchantId IS NULL OR t.merchantId = :merchantId) " +
            "AND (:tagId IS NULL OR tt.tagId = :tagId) " +
            "AND (:keyword IS NULL OR t.note LIKE '%' || :keyword || '%' " +
            "OR m.name LIKE '%' || :keyword || '%' OR m.alias LIKE '%' || :keyword || '%'" +
            " OR tg.name LIKE '%' || :keyword || '%') " +
            "ORDER BY t.occurredAt DESC",
    )
    fun pagingTransactionsByFilters(
        ledgerId: String,
        startTime: Long? = null,
        endTime: Long? = null,
        minAmount: Long? = null,
        maxAmount: Long? = null,
        accountIds: List<String>? = null,
        hasAccountIds: Int = if (accountIds.isNullOrEmpty()) 0 else 1,
        categoryId: String? = null,
        tagId: String? = null,
        merchantId: String? = null,
        keyword: String? = null,
    ): PagingSource<Int, TransactionEntity>
}
