package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_split")
data class TransactionSplitEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val ledgerId: String,
    val categoryId: String? = null,
    val amount: Long,
    val note: String = "",
)
