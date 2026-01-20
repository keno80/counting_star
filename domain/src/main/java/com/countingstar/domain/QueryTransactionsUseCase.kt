package com.countingstar.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class TransactionSortField {
    OCCURRED_AT,
    AMOUNT,
}

enum class SortDirection {
    ASC,
    DESC,
}

data class TransactionQueryParams(
    val ledgerId: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val tagId: String? = null,
    val merchantId: String? = null,
    val keyword: String? = null,
    val sortField: TransactionSortField = TransactionSortField.OCCURRED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
)

class QueryTransactionsUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
    ) {
        operator fun invoke(params: TransactionQueryParams): Flow<List<Transaction>> =
            transactionRepository
                .observeTransactionsByFilters(
                    ledgerId = params.ledgerId,
                    startTime = params.startTime,
                    endTime = params.endTime,
                    minAmount = params.minAmount,
                    maxAmount = params.maxAmount,
                    accountId = params.accountId,
                    categoryId = params.categoryId,
                    tagId = params.tagId,
                    merchantId = params.merchantId,
                    keyword = params.keyword,
                ).map { transactions ->
                    sort(transactions, params.sortField, params.sortDirection)
                }

        private fun sort(
            transactions: List<Transaction>,
            field: TransactionSortField,
            direction: SortDirection,
        ): List<Transaction> =
            when (field) {
                TransactionSortField.OCCURRED_AT ->
                    when (direction) {
                        SortDirection.ASC -> transactions.sortedBy { it.occurredAt }
                        SortDirection.DESC -> transactions.sortedByDescending { it.occurredAt }
                    }
                TransactionSortField.AMOUNT ->
                    when (direction) {
                        SortDirection.ASC -> transactions.sortedBy { it.amount }
                        SortDirection.DESC -> transactions.sortedByDescending { it.amount }
                    }
            }
    }
