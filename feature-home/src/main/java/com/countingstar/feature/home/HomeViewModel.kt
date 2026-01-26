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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
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
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val minAmountInput: String = "",
    val maxAmountInput: String = "",
    val selectedAccountIds: List<String> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
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
        private val searchQuery = MutableStateFlow("")
        private val startDate = MutableStateFlow<Long?>(null)
        private val endDate = MutableStateFlow<Long?>(null)
        private val minAmountInput = MutableStateFlow("")
        private val maxAmountInput = MutableStateFlow("")
        private val selectedAccountIds = MutableStateFlow<List<String>>(emptyList())
        private var refreshJob: Job? = null

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
                    val amountRangeFlow =
                        combine(
                            minAmountInput,
                            maxAmountInput,
                        ) { minInput, maxInput ->
                            val minCents = amountInputToCents(minInput)
                            val maxCents = amountInputToCents(maxInput)
                            normalizeAmountRange(
                                minCents,
                                maxCents,
                            )
                        }
                    combine(
                        searchQuery
                            .map { it.trim() }
                            .map { it.takeIf(String::isNotEmpty) }
                            .distinctUntilChanged(),
                        startDate,
                        endDate,
                        amountRangeFlow,
                        selectedAccountIds,
                    ) { keyword, start, end, amountRange, accounts ->
                        TransactionFilter(
                            keyword = keyword,
                            startTime = start,
                            endTime = end,
                            minAmount = amountRange.minAmount,
                            maxAmount = amountRange.maxAmount,
                            accountIds = accounts.takeIf { it.isNotEmpty() },
                        )
                    }.distinctUntilChanged()
                        .flatMapLatest { filter ->
                            queryTransactionsUseCase(
                                TransactionQueryParams(
                                    ledgerId = ledgerId,
                                    startTime = filter.startTime,
                                    endTime = filter.endTime,
                                    minAmount = filter.minAmount,
                                    maxAmount = filter.maxAmount,
                                    accountIds = filter.accountIds,
                                    keyword = filter.keyword,
                                    sortField = TransactionSortField.OCCURRED_AT,
                                    sortDirection = SortDirection.DESC,
                                ),
                            )
                        }.collectLatest { transactions ->
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

        fun refresh() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    _uiState.update { current -> current.copy(isRefreshing = true) }
                    delay(500)
                    _uiState.update { current -> current.copy(isRefreshing = false) }
                }
        }

        fun updateSearchQuery(query: String) {
            _uiState.update { current -> current.copy(searchQuery = query) }
            searchQuery.value = query
        }

        fun updateStartDate(start: Long) {
            val currentEnd = endDate.value
            val normalizedEnd =
                if (currentEnd != null && currentEnd < start) {
                    start
                } else {
                    currentEnd
                }
            _uiState.update { current ->
                current.copy(startDate = start, endDate = normalizedEnd)
            }
            startDate.value = start
            if (normalizedEnd != currentEnd) {
                endDate.value = normalizedEnd
            }
        }

        fun updateEndDate(end: Long) {
            val currentStart = startDate.value
            val normalizedStart =
                if (currentStart != null && end < currentStart) {
                    end
                } else {
                    currentStart
                }
            _uiState.update { current ->
                current.copy(startDate = normalizedStart, endDate = end)
            }
            endDate.value = end
            if (normalizedStart != currentStart) {
                startDate.value = normalizedStart
            }
        }

        fun clearDateRange() {
            _uiState.update { current ->
                current.copy(startDate = null, endDate = null)
            }
            startDate.value = null
            endDate.value = null
        }

        fun updateMinAmountInput(input: String) {
            _uiState.update { current -> current.copy(minAmountInput = input) }
            minAmountInput.value = input
        }

        fun updateMaxAmountInput(input: String) {
            _uiState.update { current -> current.copy(maxAmountInput = input) }
            maxAmountInput.value = input
        }

        fun clearAmountRange() {
            _uiState.update { current ->
                current.copy(minAmountInput = "", maxAmountInput = "")
            }
            minAmountInput.value = ""
            maxAmountInput.value = ""
        }

        fun toggleAccountSelection(accountId: String) {
            val current = selectedAccountIds.value
            val updated =
                if (current.contains(accountId)) {
                    current.filterNot { it == accountId }
                } else {
                    current + accountId
                }
            _uiState.update { state -> state.copy(selectedAccountIds = updated) }
            selectedAccountIds.value = updated
        }

        fun clearAccountSelection() {
            _uiState.update { state -> state.copy(selectedAccountIds = emptyList()) }
            selectedAccountIds.value = emptyList()
        }
    }

private data class TimeRange(
    val start: Long,
    val end: Long,
)

private data class TransactionFilter(
    val keyword: String?,
    val startTime: Long?,
    val endTime: Long?,
    val minAmount: Long?,
    val maxAmount: Long?,
    val accountIds: List<String>?,
)

private data class AmountRange(
    val minAmount: Long?,
    val maxAmount: Long?,
)

private fun normalizeAmountRange(
    minAmount: Long?,
    maxAmount: Long?,
): AmountRange =
    if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
        AmountRange(minAmount = maxAmount, maxAmount = minAmount)
    } else {
        AmountRange(minAmount = minAmount, maxAmount = maxAmount)
    }

private fun amountInputToCents(input: String): Long? {
    val value = input.toBigDecimalOrNull() ?: return null
    return value
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .toLong()
}

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
