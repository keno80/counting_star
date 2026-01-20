package com.countingstar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: String,
    val ledgerId: String,
    val name: String,
    val type: String,
    val currency: String,
    val initialBalance: Long,
    val currentBalance: Long,
    val isActive: Boolean,
    val creditBillingDay: Int? = null,
    val creditRepaymentDay: Int? = null,
    val creditLimit: Long? = null,
)
