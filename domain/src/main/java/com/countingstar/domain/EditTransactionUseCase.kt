package com.countingstar.domain

import javax.inject.Inject

data class EditTransactionParams(
    val id: String,
    val ledgerId: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val occurredAt: Long,
    val note: String,
    val accountId: String? = null,
    val categoryId: String? = null,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val tagIds: List<String> = emptyList(),
    val merchantId: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
)

class EditTransactionUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(params: EditTransactionParams): Transaction {
            require(params.id.isNotBlank())
            require(params.ledgerId.isNotBlank())
            require(params.currency.isNotBlank())
            require(params.amount > 0)

            val existing =
                requireNotNull(transactionRepository.getTransactionById(params.id)) {
                    "Transaction not found"
                }

            validateTypeFields(params)
            val updated = buildTransaction(params, existing)

            val combined = LinkedHashMap<String, Long>()
            mergeDeltas(combined, balanceDeltas(existing), rollback = true)
            mergeDeltas(combined, balanceDeltas(updated), rollback = false)
            applyBalanceAdjustments(combined)

            transactionRepository.update(updated)
            return updated
        }

        private fun validateTypeFields(params: EditTransactionParams) {
            when (params.type) {
                TransactionType.INCOME,
                TransactionType.EXPENSE,
                -> require(!params.accountId.isNullOrBlank())
                TransactionType.TRANSFER -> {
                    require(!params.fromAccountId.isNullOrBlank())
                    require(!params.toAccountId.isNullOrBlank())
                    require(params.fromAccountId != params.toAccountId)
                }
            }
        }

        private fun buildTransaction(
            params: EditTransactionParams,
            existing: Transaction,
        ): Transaction =
            when (params.type) {
                TransactionType.INCOME,
                TransactionType.EXPENSE,
                ->
                    existing.copy(
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
                        isDeleted = params.isDeleted,
                        deletedAt = params.deletedAt,
                    )
                TransactionType.TRANSFER ->
                    existing.copy(
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
                        isDeleted = params.isDeleted,
                        deletedAt = params.deletedAt,
                    )
            }

        private fun balanceDeltas(transaction: Transaction): Map<String, Long> =
            when (transaction.type) {
                TransactionType.INCOME ->
                    mapOf(
                        requireNotNull(transaction.accountId) to transaction.amount,
                    )
                TransactionType.EXPENSE ->
                    mapOf(
                        requireNotNull(transaction.accountId) to -transaction.amount,
                    )
                TransactionType.TRANSFER ->
                    mapOf(
                        requireNotNull(transaction.fromAccountId) to -transaction.amount,
                        requireNotNull(transaction.toAccountId) to transaction.amount,
                    )
            }

        private fun mergeDeltas(
            combined: MutableMap<String, Long>,
            deltas: Map<String, Long>,
            rollback: Boolean,
        ) {
            for ((accountId, delta) in deltas) {
                val actual = if (rollback) -delta else delta
                val current = combined[accountId] ?: 0L
                combined[accountId] = current + actual
            }
        }

        private suspend fun applyBalanceAdjustments(deltas: Map<String, Long>) {
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
