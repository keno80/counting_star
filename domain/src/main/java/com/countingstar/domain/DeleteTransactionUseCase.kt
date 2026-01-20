package com.countingstar.domain

import javax.inject.Inject

data class DeleteTransactionParams(
    val id: String,
)

class DeleteTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(params: DeleteTransactionParams) {
            require(params.id.isNotBlank())
            val transaction =
                requireNotNull(transactionRepository.getTransactionById(params.id)) {
                    "Transaction not found"
                }
            rollbackBalance(transaction)
            transactionRepository.deleteById(params.id)
        }

        private suspend fun rollbackBalance(transaction: Transaction) {
            val deltas =
                when (transaction.type) {
                    TransactionType.INCOME ->
                        mapOf(
                            requireNotNull(transaction.accountId) to -transaction.amount,
                        )
                    TransactionType.EXPENSE ->
                        mapOf(
                            requireNotNull(transaction.accountId) to transaction.amount,
                        )
                    TransactionType.TRANSFER ->
                        mapOf(
                            requireNotNull(transaction.fromAccountId) to transaction.amount,
                            requireNotNull(transaction.toAccountId) to -transaction.amount,
                        )
                }
            for ((accountId, delta) in deltas) {
                if (delta == 0L) {
                    continue
                }
                val account =
                    requireNotNull(accountRepository.getAccountById(accountId)) {
                        "Account not found"
                    }
                accountRepository.updateBalance(accountId, account.currentBalance + delta)
            }
        }
    }
