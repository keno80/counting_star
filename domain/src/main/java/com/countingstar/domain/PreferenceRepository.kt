package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    fun observeDefaultLedgerId(): Flow<String?>

    suspend fun getDefaultLedgerId(): String?

    suspend fun setDefaultLedgerId(ledgerId: String)

    suspend fun clearDefaultLedgerId()

    fun observeDefaultAccountId(): Flow<String?>

    suspend fun getDefaultAccountId(): String?

    suspend fun setDefaultAccountId(accountId: String)

    suspend fun clearDefaultAccountId()
}
