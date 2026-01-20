package com.countingstar.domain

data class TransactionSplit(
    val id: String,
    val transactionId: String,
    val amount: Long,
    val currency: String,
    val categoryId: String,
    val tagIds: List<String> = emptyList(),
    val note: String,
)
