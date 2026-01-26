package com.countingstar.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ExportCsvParams(
    val ledgerId: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val accountIds: List<String>? = null,
    val categoryId: String? = null,
    val tagId: String? = null,
    val merchantId: String? = null,
    val keyword: String? = null,
)

class ExportCsvUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val tagRepository: TagRepository,
        private val merchantRepository: MerchantRepository,
    ) {
        operator fun invoke(params: ExportCsvParams): Flow<String> {
            val transactionsFlow =
                transactionRepository.observeTransactionsByFilters(
                    ledgerId = params.ledgerId,
                    startTime = params.startTime,
                    endTime = params.endTime,
                    minAmount = params.minAmount,
                    maxAmount = params.maxAmount,
                    accountIds = params.accountIds,
                    categoryId = params.categoryId,
                    tagId = params.tagId,
                    merchantId = params.merchantId,
                    keyword = params.keyword,
                )
            val lookupFlow =
                combine(
                    accountRepository.observeAccountsByLedger(params.ledgerId),
                    categoryRepository.observeCategories(params.ledgerId, CategoryType.INCOME),
                    categoryRepository.observeCategories(params.ledgerId, CategoryType.EXPENSE),
                    tagRepository.observeTagsByLedger(params.ledgerId),
                    merchantRepository.observeMerchantsByLedger(params.ledgerId),
                ) { accounts, incomeCategories, expenseCategories, tags, merchants ->
                    LookupMaps(
                        accountMap = accounts.associateBy { it.id },
                        categoryMap =
                            (incomeCategories + expenseCategories).associateBy { it.id },
                        tagMap = tags.associateBy { it.id },
                        merchantMap = merchants.associateBy { it.id },
                    )
                }
            return combine(transactionsFlow, lookupFlow) { transactions, lookups ->
                buildCsv(
                    transactions = transactions,
                    accountMap = lookups.accountMap,
                    categoryMap = lookups.categoryMap,
                    tagMap = lookups.tagMap,
                    merchantMap = lookups.merchantMap,
                )
            }
        }

        private fun buildCsv(
            transactions: List<Transaction>,
            accountMap: Map<String, Account>,
            categoryMap: Map<String, Category>,
            tagMap: Map<String, Tag>,
            merchantMap: Map<String, Merchant>,
        ): String {
            val builder = StringBuilder()
            builder.append(
                listOf(
                    "id",
                    "type",
                    "amount",
                    "currency",
                    "occurredAt",
                    "account",
                    "category",
                    "tags",
                    "merchant",
                    "note",
                    "fromAccount",
                    "toAccount",
                ).joinToString(","),
            )
            for (transaction in transactions) {
                builder.append('\n')
                builder.append(
                    buildRow(
                        transaction = transaction,
                        accountMap = accountMap,
                        categoryMap = categoryMap,
                        tagMap = tagMap,
                        merchantMap = merchantMap,
                    ).joinToString(","),
                )
            }
            return builder.toString()
        }

        private fun buildRow(
            transaction: Transaction,
            accountMap: Map<String, Account>,
            categoryMap: Map<String, Category>,
            tagMap: Map<String, Tag>,
            merchantMap: Map<String, Merchant>,
        ): List<String> {
            val accountName =
                transaction.accountId?.let { accountMap[it]?.name }
                    ?: ""
            val categoryName =
                transaction.categoryId?.let { categoryMap[it]?.name }
                    ?: ""
            val fromAccountName =
                transaction.fromAccountId?.let { accountMap[it]?.name }
                    ?: ""
            val toAccountName =
                transaction.toAccountId?.let { accountMap[it]?.name }
                    ?: ""
            val tagNames =
                if (transaction.tagIds.isEmpty()) {
                    ""
                } else {
                    transaction.tagIds
                        .map { tagId -> tagMap[tagId]?.name ?: tagId }
                        .joinToString("|")
                }
            val merchantName =
                transaction.merchantId?.let { merchantMap[it]?.name }
                    ?: ""

            return listOf(
                escape(transaction.id),
                escape(transaction.type.name),
                escape(transaction.amount.toString()),
                escape(transaction.currency),
                escape(transaction.occurredAt.toString()),
                escape(accountName),
                escape(categoryName),
                escape(tagNames),
                escape(merchantName),
                escape(transaction.note),
                escape(fromAccountName),
                escape(toAccountName),
            )
        }

        private fun escape(value: String): String {
            val needsEscape =
                value.indexOf(',') >= 0 ||
                    value.indexOf('"') >= 0 ||
                    value.indexOf('\n') >= 0 ||
                    value.indexOf('\r') >= 0
            if (!needsEscape) {
                return value
            }
            val escaped = value.replace("\"", "\"\"")
            return "\"$escaped\""
        }

        private data class LookupMaps(
            val accountMap: Map<String, Account>,
            val categoryMap: Map<String, Category>,
            val tagMap: Map<String, Tag>,
            val merchantMap: Map<String, Merchant>,
        )
    }
