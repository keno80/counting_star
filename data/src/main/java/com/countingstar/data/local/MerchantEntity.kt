package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val alias: String? = null,
)
