package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

data class Merchant(
    val id: String,
    val ledgerId: String,
    val name: String,
    val alias: String? = null,
)

interface MerchantRepository {
    fun observeMerchantsByLedger(ledgerId: String): Flow<List<Merchant>>

    suspend fun getMerchantById(id: String): Merchant?

    suspend fun upsert(merchant: Merchant)

    suspend fun update(merchant: Merchant)

    suspend fun deleteById(id: String)

    fun observeMerchantsByKeyword(
        ledgerId: String,
        keyword: String,
    ): Flow<List<Merchant>>
}
