package com.countingstar.domain

import kotlinx.coroutines.flow.Flow

data class Account(
    val id: String,
    val ledgerId: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalance: Long,
    val currentBalance: Long,
    val isActive: Boolean,
    val creditBillingDay: Int? = null,
    val creditRepaymentDay: Int? = null,
    val creditLimit: Long? = null,
)

enum class AccountType {
    CASH,
    DEBIT_CARD,
    CREDIT_CARD,
    E_WALLET,
    INVESTMENT,
    DEBT,
    OTHER,
}

interface AccountRepository {
    fun observeAccountsByLedger(ledgerId: String): Flow<List<Account>>

    suspend fun getAccountById(id: String): Account?

    suspend fun upsert(account: Account)

    suspend fun update(account: Account)

    suspend fun deleteById(id: String)

    suspend fun setActive(
        id: String,
        isActive: Boolean,
    )

    suspend fun updateBalance(
        id: String,
        balance: Long,
    )
}
