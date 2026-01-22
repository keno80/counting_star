package com.countingstar.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.countingstar.domain.Account
import com.countingstar.domain.AccountRepository
import com.countingstar.domain.Category
import com.countingstar.domain.CategoryRepository
import com.countingstar.domain.CategoryType
import com.countingstar.domain.GetStatisticsSummaryUseCase
import com.countingstar.domain.InitializeDefaultDataUseCase
import com.countingstar.domain.QueryTransactionsUseCase
import com.countingstar.domain.SortDirection
import com.countingstar.domain.StatisticsSummary
import com.countingstar.domain.StatisticsSummaryParams
import com.countingstar.domain.Transaction
import com.countingstar.domain.TransactionQueryParams
import com.countingstar.domain.TransactionSortField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class HomeSummaryUi(
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
)

data class HomeUiState(
    val todaySummary: HomeSummaryUi = HomeSummaryUi(),
    val monthSummary: HomeSummaryUi = HomeSummaryUi(),
    val lastMonthSummary: HomeSummaryUi = HomeSummaryUi(),
    val transactions: List<Transaction> = emptyList(),
    val accountMap: Map<String, Account> = emptyMap(),
    val categoryMap: Map<String, Category> = emptyMap(),
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
        private val queryTransactionsUseCase: QueryTransactionsUseCase,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val getStatisticsSummaryUseCase: GetStatisticsSummaryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val ledgerId = initializeDefaultDataUseCase().ledgerId
                val zoneId = ZoneId.systemDefault()
                val todayRange = todayRange(zoneId)
                val monthRange = monthRange(zoneId)
                val lastMonthRange = previousMonthRange(zoneId)

                launch {
                    accountRepository.observeAccountsByLedger(ledgerId).collectLatest { accounts ->
                        _uiState.update { current ->
                            current.copy(accountMap = accounts.associateBy { it.id })
                        }
                    }
                }
                launch {
                    combine(
                        categoryRepository.observeCategories(ledgerId, CategoryType.INCOME),
                        categoryRepository.observeCategories(ledgerId, CategoryType.EXPENSE),
                    ) { incomeCategories, expenseCategories ->
                        (incomeCategories + expenseCategories).associateBy { it.id }
                    }.collectLatest { categoryMap ->
                        _uiState.update { current ->
                            current.copy(categoryMap = categoryMap)
                        }
                    }
                }
                launch {
                    queryTransactionsUseCase(
                        TransactionQueryParams(
                            ledgerId = ledgerId,
                            sortField = TransactionSortField.OCCURRED_AT,
                            sortDirection = SortDirection.DESC,
                        ),
                    ).collectLatest { transactions ->
                        _uiState.update { current ->
                            current.copy(transactions = transactions)
                        }
                    }
                }
                launch {
                    getStatisticsSummaryUseCase(
                        StatisticsSummaryParams(
                            ledgerId = ledgerId,
                            startTime = todayRange.start,
                            endTime = todayRange.end,
                        ),
                    ).collectLatest { summary ->
                        _uiState.update { current ->
                            current.copy(todaySummary = summary.toUi())
                        }
                    }
                }
                launch {
                    getStatisticsSummaryUseCase(
                        StatisticsSummaryParams(
                            ledgerId = ledgerId,
                            startTime = monthRange.start,
                            endTime = monthRange.end,
                        ),
                    ).collectLatest { summary ->
                        _uiState.update { current ->
                            current.copy(monthSummary = summary.toUi())
                        }
                    }
                }
                launch {
                    getStatisticsSummaryUseCase(
                        StatisticsSummaryParams(
                            ledgerId = ledgerId,
                            startTime = lastMonthRange.start,
                            endTime = lastMonthRange.end,
                        ),
                    ).collectLatest { summary ->
                        _uiState.update { current ->
                            current.copy(lastMonthSummary = summary.toUi())
                        }
                    }
                }
            }
        }
    }

private data class TimeRange(
    val start: Long,
    val end: Long,
)

private fun StatisticsSummary.toUi(): HomeSummaryUi =
    HomeSummaryUi(
        income = income,
        expense = expense,
        balance = balance,
    )

private fun todayRange(zoneId: ZoneId): TimeRange {
    val today = LocalDate.now(zoneId)
    val start =
        today
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    val end =
        today
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1
    return TimeRange(start, end)
}

private fun monthRange(zoneId: ZoneId): TimeRange {
    val yearMonth = YearMonth.now(zoneId)
    val start =
        yearMonth
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    val end =
        yearMonth
            .plusMonths(1)
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1
    return TimeRange(start, end)
}

private fun previousMonthRange(zoneId: ZoneId): TimeRange {
    val yearMonth = YearMonth.now(zoneId).minusMonths(1)
    val start =
        yearMonth
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    val end =
        yearMonth
            .plusMonths(1)
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1
    return TimeRange(start, end)
}
