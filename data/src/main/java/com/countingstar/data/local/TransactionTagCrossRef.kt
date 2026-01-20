package com.countingstar.data.local

import androidx.room.Entity

@Entity(
    tableName = "transaction_tag",
    primaryKeys = ["transactionId", "tagId"],
)
data class TransactionTagCrossRef(
    val transactionId: String,
    val tagId: String,
)
