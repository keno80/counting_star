package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

data class Tag(
    val id: String,
    val ledgerId: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
)

interface TagRepository {
    fun observeTagsByLedger(ledgerId: String): Flow<List<Tag>>

    suspend fun getTagById(id: String): Tag?

    suspend fun upsert(tag: Tag)

    suspend fun update(tag: Tag)

    suspend fun deleteById(id: String)

    fun observeTagsByTransaction(transactionId: String): Flow<List<Tag>>

    suspend fun upsertTransactionTags(
        transactionId: String,
        tagIds: List<String>,
    )

    suspend fun deleteTransactionTagsByTransaction(transactionId: String)
}
