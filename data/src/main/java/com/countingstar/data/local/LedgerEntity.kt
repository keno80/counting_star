package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDefault: Boolean = false,
)
