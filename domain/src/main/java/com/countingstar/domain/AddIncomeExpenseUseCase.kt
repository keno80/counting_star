package com.countingstar.domain

import java.util.UUID
import javax.inject.Inject

data class AddIncomeExpenseParams(
    val ledgerId: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val occurredAt: Long,
    val note: String,
    val accountId: String,
    val categoryId: String? = null,
    val tagIds: List<String> = emptyList(),
    val merchantId: String? = null,
    val id: String? = null,
)

class AddIncomeExpenseUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(params: AddIncomeExpenseParams): Transaction {
            require(params.amount > 0)
            require(params.ledgerId.isNotBlank())
            require(params.currency.isNotBlank())
            require(params.accountId.isNotBlank())
            require(
                params.type == TransactionType.INCOME ||
                    params.type == TransactionType.EXPENSE,
            )

            val transaction =
                Transaction(
                    id = params.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    ledgerId = params.ledgerId,
                    type = params.type,
                    amount = params.amount,
                    currency = params.currency,
                    occurredAt = params.occurredAt,
                    note = params.note,
                    accountId = params.accountId,
                    categoryId = params.categoryId,
                    fromAccountId = null,
                    toAccountId = null,
                    tagIds = params.tagIds,
                    merchantId = params.merchantId,
                    isDeleted = false,
                    deletedAt = null,
                )
            val balanceDelta =
                when (params.type) {
                    TransactionType.INCOME -> params.amount
                    TransactionType.EXPENSE -> -params.amount
                    TransactionType.TRANSFER -> 0L
                }
            if (balanceDelta != 0L) {
                val account =
                    requireNotNull(accountRepository.getAccountById(params.accountId)) {
                        "Account not found"
                    }
                accountRepository.updateBalance(
                    params.accountId,
                    account.currentBalance + balanceDelta,
                )
            }
            transactionRepository.upsert(transaction)
            return transaction
        }
    }
