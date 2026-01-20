package com.countingstar.domain

data class Ledger(
    val id: String,
    val name: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
