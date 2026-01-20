package com.countingstar.domain

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
