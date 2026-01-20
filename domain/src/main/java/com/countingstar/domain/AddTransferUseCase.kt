package com.countingstar.domain

import java.util.UUID
import javax.inject.Inject

data class AddTransferParams(
    val ledgerId: String,
    val amount: Long,
    val currency: String,
    val occurredAt: Long,
    val note: String,
    val fromAccountId: String,
    val toAccountId: String,
    val id: String? = null,
)

class AddTransferUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(params: AddTransferParams): Transaction {
            require(params.amount > 0)
            require(params.ledgerId.isNotBlank())
            require(params.currency.isNotBlank())
            require(params.fromAccountId.isNotBlank())
            require(params.toAccountId.isNotBlank())
            require(params.fromAccountId != params.toAccountId)

            val transaction =
                Transaction(
                    id = params.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    ledgerId = params.ledgerId,
                    type = TransactionType.TRANSFER,
                    amount = params.amount,
                    currency = params.currency,
                    occurredAt = params.occurredAt,
                    note = params.note,
                    accountId = null,
                    categoryId = null,
                    fromAccountId = params.fromAccountId,
                    toAccountId = params.toAccountId,
                    tagIds = emptyList(),
                    merchantId = null,
                    isDeleted = false,
                    deletedAt = null,
                )
            val fromAccount =
                requireNotNull(accountRepository.getAccountById(params.fromAccountId)) {
                    "Account not found"
                }
            val toAccount =
                requireNotNull(accountRepository.getAccountById(params.toAccountId)) {
                    "Account not found"
                }
            accountRepository.updateBalance(
                params.fromAccountId,
                fromAccount.currentBalance - params.amount,
            )
            accountRepository.updateBalance(
                params.toAccountId,
                toAccount.currentBalance + params.amount,
            )
            transactionRepository.upsert(transaction)
            return transaction
        }
    }
