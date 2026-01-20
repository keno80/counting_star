package com.countingstar.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class StatisticsSummaryParams(
    val ledgerId: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
)

data class CategoryAggregate(
    val categoryId: String,
    val categoryName: String,
    val amount: Long,
)

data class StatisticsSummary(
    val income: Long,
    val expense: Long,
    val balance: Long,
    val incomeByCategory: List<CategoryAggregate>,
    val expenseByCategory: List<CategoryAggregate>,
)

class GetStatisticsSummaryUseCase
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
    ) {
        operator fun invoke(params: StatisticsSummaryParams): Flow<StatisticsSummary> =
            combine(
                transactionRepository.observeTransactionsByFilters(
                    ledgerId = params.ledgerId,
                    startTime = params.startTime,
                    endTime = params.endTime,
                ),
                categoryRepository.observeCategories(
                    ledgerId = params.ledgerId,
                    type = CategoryType.INCOME,
                ),
                categoryRepository.observeCategories(
                    ledgerId = params.ledgerId,
                    type = CategoryType.EXPENSE,
                ),
            ) { transactions, incomeCategories, expenseCategories ->
                val incomeCategoryMap = incomeCategories.associateBy { it.id }
                val expenseCategoryMap = expenseCategories.associateBy { it.id }
                val incomeCategoryTotals = LinkedHashMap<String, Long>()
                val expenseCategoryTotals = LinkedHashMap<String, Long>()
                var incomeTotal = 0L
                var expenseTotal = 0L

                for (transaction in transactions) {
                    when (transaction.type) {
                        TransactionType.INCOME -> {
                            incomeTotal += transaction.amount
                            val categoryId = transaction.categoryId ?: continue
                            val current = incomeCategoryTotals[categoryId] ?: 0L
                            incomeCategoryTotals[categoryId] = current + transaction.amount
                        }
                        TransactionType.EXPENSE -> {
                            expenseTotal += transaction.amount
                            val categoryId = transaction.categoryId ?: continue
                            val current = expenseCategoryTotals[categoryId] ?: 0L
                            expenseCategoryTotals[categoryId] = current + transaction.amount
                        }
                        TransactionType.TRANSFER -> Unit
                    }
                }

                val incomeByCategory =
                    incomeCategoryTotals
                        .map { (categoryId, amount) ->
                            CategoryAggregate(
                                categoryId = categoryId,
                                categoryName = incomeCategoryMap[categoryId]?.name ?: categoryId,
                                amount = amount,
                            )
                        }.sortedByDescending { it.amount }

                val expenseByCategory =
                    expenseCategoryTotals
                        .map { (categoryId, amount) ->
                            CategoryAggregate(
                                categoryId = categoryId,
                                categoryName = expenseCategoryMap[categoryId]?.name ?: categoryId,
                                amount = amount,
                            )
                        }.sortedByDescending { it.amount }

                StatisticsSummary(
                    income = incomeTotal,
                    expense = expenseTotal,
                    balance = incomeTotal - expenseTotal,
                    incomeByCategory = incomeByCategory,
                    expenseByCategory = expenseByCategory,
                )
            }
    }
