package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val type: String,
    val amount: Long,
    val currency: String,
    val occurredAt: Long,
    val note: String,
    val accountId: String? = null,
    val categoryId: String? = null,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val merchantId: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
)
