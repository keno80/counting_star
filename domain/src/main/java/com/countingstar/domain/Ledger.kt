package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

data class Ledger(
    val id: String,
    val name: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

interface LedgerRepository {
    fun observeLedgers(): Flow<List<Ledger>>

    suspend fun getLedgerById(id: String): Ledger?

    suspend fun upsert(ledger: Ledger)

    suspend fun update(ledger: Ledger)

    suspend fun deleteById(id: String)

    fun observeDefaultLedger(): Flow<Ledger?>

    suspend fun clearDefault()

    suspend fun setDefaultLedger(ledgerId: String)
}
