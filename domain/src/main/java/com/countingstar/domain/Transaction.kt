package com.countingstar.domain

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
