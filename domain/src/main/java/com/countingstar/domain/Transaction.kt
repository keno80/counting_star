package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

data class Transaction(
    val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amount: Long, // In cents, absolute value
    val currency: String,
    val occurredAt: Long,
    val note: String,
    val accountId: String? = null,
    val categoryId: String? = null,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val tagIds: List<String> = emptyList(),
    val merchantId: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
}

interface TransactionRepository {
    fun observeTransactionsByLedger(ledgerId: String): Flow<List<Transaction>>

    suspend fun getTransactionById(id: String): Transaction?

    suspend fun upsert(transaction: Transaction)

    suspend fun update(transaction: Transaction)

    suspend fun deleteById(id: String)

    fun observeTransactionsByFilters(
        ledgerId: String,
        startTime: Long? = null,
        endTime: Long? = null,
        minAmount: Long? = null,
        maxAmount: Long? = null,
        accountIds: List<String>? = null,
        categoryId: String? = null,
        tagId: String? = null,
        merchantId: String? = null,
        keyword: String? = null,
    ): Flow<List<Transaction>>
}
