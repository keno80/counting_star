package com.countingstar.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.countingstar.core.ui.EmptyState
import com.countingstar.core.ui.LocalSnackbarHostState
import com.countingstar.core.ui.component.AccountItem
import com.countingstar.core.ui.component.AccountSelector
import com.countingstar.core.ui.component.AmountInput
import com.countingstar.core.ui.component.CategoryItem
import com.countingstar.core.ui.component.CategorySelector
import com.countingstar.core.ui.component.DateTimePicker
import com.countingstar.core.ui.component.ListHeader
import com.countingstar.core.ui.component.formatAmount
import com.countingstar.core.ui.component.formatDate
import com.countingstar.core.ui.component.formatTime
import com.countingstar.domain.Account
import com.countingstar.domain.AccountRepository
import com.countingstar.domain.AddIncomeExpenseParams
import com.countingstar.domain.AddIncomeExpenseUseCase
import com.countingstar.domain.AddTransferParams
import com.countingstar.domain.AddTransferUseCase
import com.countingstar.domain.Category
import com.countingstar.domain.CategoryRepository
import com.countingstar.domain.CategoryType
import com.countingstar.domain.InitializeDefaultDataUseCase
import com.countingstar.domain.PreferenceRepository
import com.countingstar.domain.QueryTransactionsUseCase
import com.countingstar.domain.SortDirection
import com.countingstar.domain.Transaction
import com.countingstar.domain.TransactionQueryParams
import com.countingstar.domain.TransactionRepository
import com.countingstar.domain.TransactionSortField
import com.countingstar.domain.TransactionType
import com.countingstar.feature.home.HomeDestination
import com.countingstar.feature.home.HomeViewModel
import com.countingstar.feature.home.homeRoute
import com.countingstar.navigation.TopLevelDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

private const val FILTER_ROUTE = "transaction-filter"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun CountingStarApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showHomeTopBar = currentRoute == HomeDestination.ROUTE
    val showFilterTopBar = currentRoute == FILTER_ROUTE

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                when {
                    showHomeTopBar -> {
                        TopAppBar(
                            title = { Text("首页") },
                            actions = {
                                IconButton(
                                    onClick = { navController.navigate(FILTER_ROUTE) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "筛选",
                                    )
                                }
                            },
                        )
                    }
                    showFilterTopBar -> {
                        TopAppBar(
                            title = { Text("筛选") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "返回",
                                    )
                                }
                            },
                        )
                    }
                }
            },
            bottomBar = {
                AppBottomBar(navController = navController)
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelDestinations = TopLevelDestination.entries
    val isTopLevel = topLevelDestinations.any { it.route == currentDestination?.route }

    if (isTopLevel) {
        NavigationBar {
            topLevelDestinations.forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                NavigationBarItem(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val addTransactionRoute = "add-transaction"
    NavHost(
        navController = navController,
        startDestination = HomeDestination.ROUTE,
        modifier = modifier,
    ) {
        homeRoute(
            onAddTransaction = {
                navController.navigate(addTransactionRoute)
            },
            onStatisticsClick = {
                navController.navigate(TopLevelDestination.STATISTICS.route)
            },
        )

        composable(addTransactionRoute) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(FILTER_ROUTE) {
            val parentEntry =
                remember(it) {
                    navController.getBackStackEntry(HomeDestination.ROUTE)
                }
            val homeViewModel: HomeViewModel = hiltViewModel(parentEntry)
            FilterScreen(viewModel = homeViewModel)
        }

        composable(TopLevelDestination.STATISTICS.route) {
            EmptyState(message = "统计功能开发中")
        }
        composable(TopLevelDestination.SETTINGS.route) {
            EmptyState(message = "设置功能开发中")
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun FilterScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    val startDate = uiState.startDate
    val endDate = uiState.endDate
    val minAmountInput = uiState.minAmountInput
    val maxAmountInput = uiState.maxAmountInput

    if (datePickerTarget != null) {
        val initialDate = startDate ?: endDate ?: System.currentTimeMillis()
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = initialDate,
            )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            when (datePickerTarget) {
                                DatePickerTarget.START ->
                                    viewModel.updateStartDate(startOfDayMillis(selected))
                                DatePickerTarget.END ->
                                    viewModel.updateEndDate(endOfDayMillis(selected))
                                null -> Unit
                            }
                        }
                        datePickerTarget = null
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { datePickerTarget = null },
                ) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "日期范围",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { datePickerTarget = DatePickerTarget.START },
            ) {
                Text(text = startDate?.let { formatDate(it) } ?: "开始日期")
            }
            OutlinedButton(
                onClick = { datePickerTarget = DatePickerTarget.END },
            ) {
                Text(text = endDate?.let { formatDate(it) } ?: "结束日期")
            }
        }
        if (startDate != null || endDate != null) {
            TextButton(onClick = viewModel::clearDateRange) {
                Text("清空日期")
            }
        }
        Text(
            text = "金额区间",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AmountInput(
                amount = minAmountInput,
                onAmountChange = viewModel::updateMinAmountInput,
                modifier = Modifier.weight(1f),
                label = "最小金额",
            )
            AmountInput(
                amount = maxAmountInput,
                onAmountChange = viewModel::updateMaxAmountInput,
                modifier = Modifier.weight(1f),
                label = "最大金额",
            )
        }
        if (minAmountInput.isNotBlank() || maxAmountInput.isNotBlank()) {
            TextButton(onClick = viewModel::clearAmountRange) {
                Text("清空金额")
            }
        }
        Text(
            text = "关键词搜索",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "搜索备注/商家/标签") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                )
            },
            trailingIcon =
                if (uiState.searchQuery.isNotBlank()) {
                    {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery("") },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清空",
                            )
                        }
                    }
                } else {
                    null
                },
        )
    }
}

private enum class DatePickerTarget {
    START,
    END,
}

private fun startOfDayMillis(dateMillis: Long): Long {
    val zoneId = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
    return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

private fun endOfDayMillis(dateMillis: Long): Long {
    val zoneId = ZoneId.systemDefault()
    val date =
        Instant
            .ofEpochMilli(dateMillis)
            .atZone(zoneId)
            .toLocalDate()
    return date
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli() - 1
}

enum class RecordType(
    val label: String,
) {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("转账"),
}

data class AddTransactionUiState(
    val selectedType: RecordType = RecordType.EXPENSE,
    val amount: String = "",
    val note: String = "",
    val merchant: String = "",
    val selectedTimestamp: Long = System.currentTimeMillis(),
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedFromAccountId: String? = null,
    val selectedToAccountId: String? = null,
    val accounts: List<AccountItem> = emptyList(),
    val expenseCategories: List<CategoryItem> = emptyList(),
    val incomeCategories: List<CategoryItem> = emptyList(),
    val templates: List<QuickTemplate> = emptyList(),
    val commonExpenseCategoryIds: List<String> = emptyList(),
    val commonIncomeCategoryIds: List<String> = emptyList(),
    val quickAmounts: List<String> = listOf("10", "20", "50", "100", "200", "500"),
    val amountError: String? = null,
    val accountError: String? = null,
    val categoryError: String? = null,
    val fromAccountError: String? = null,
    val toAccountError: String? = null,
    val isSaveEnabled: Boolean = false,
)

private fun buildAddTransactionUiState(state: AddTransactionUiState): AddTransactionUiState {
    val amountValue = state.amount.toBigDecimalOrNull()
    val amountError =
        when {
            state.amount.isBlank() -> "金额必填"
            amountValue == null -> "金额格式错误"
            amountValue <= BigDecimal.ZERO -> "金额需大于0"
            else -> null
        }
    val accountError =
        if (state.selectedType == RecordType.TRANSFER || !state.selectedAccountId.isNullOrBlank()) {
            null
        } else {
            "账户必填"
        }
    val fromAccountError =
        if (state.selectedType != RecordType.TRANSFER || !state.selectedFromAccountId.isNullOrBlank()) {
            null
        } else {
            "转出账户必填"
        }
    val toAccountError =
        if (state.selectedType != RecordType.TRANSFER) {
            null
        } else if (state.selectedToAccountId.isNullOrBlank()) {
            "转入账户必填"
        } else if (state.selectedToAccountId == state.selectedFromAccountId) {
            "转入账户需不同"
        } else {
            null
        }
    val categoryError =
        if (state.selectedType == RecordType.TRANSFER || !state.selectedCategoryId.isNullOrBlank()) {
            null
        } else {
            "分类必填"
        }
    val isSaveEnabled =
        when (state.selectedType) {
            RecordType.TRANSFER ->
                amountError == null && fromAccountError == null && toAccountError == null
            RecordType.EXPENSE,
            RecordType.INCOME,
            -> amountError == null && accountError == null && categoryError == null
        }
    return state.copy(
        amountError = amountError,
        accountError = accountError,
        categoryError = categoryError,
        fromAccountError = fromAccountError,
        toAccountError = toAccountError,
        isSaveEnabled = isSaveEnabled,
    )
}

sealed interface AddTransactionUiEvent {
    data class TypeSelected(
        val type: RecordType,
    ) : AddTransactionUiEvent

    data class AmountChanged(
        val amount: String,
    ) : AddTransactionUiEvent

    data class DateTimeChanged(
        val timestamp: Long,
    ) : AddTransactionUiEvent

    data class NoteChanged(
        val note: String,
    ) : AddTransactionUiEvent

    data class MerchantChanged(
        val merchant: String,
    ) : AddTransactionUiEvent

    data class AccountSelected(
        val accountId: String?,
    ) : AddTransactionUiEvent

    data class CategorySelected(
        val categoryId: String?,
    ) : AddTransactionUiEvent

    data class FromAccountSelected(
        val accountId: String?,
    ) : AddTransactionUiEvent

    data class ToAccountSelected(
        val accountId: String?,
    ) : AddTransactionUiEvent

    data class QuickAmountSelected(
        val amount: String,
    ) : AddTransactionUiEvent

    data class TemplateSelected(
        val template: QuickTemplate,
    ) : AddTransactionUiEvent

    object CopyPreviousClicked : AddTransactionUiEvent

    object SaveClicked : AddTransactionUiEvent
}

sealed interface AddTransactionUiEffect {
    object SaveSuccess : AddTransactionUiEffect

    data class SaveFailed(
        val message: String,
    ) : AddTransactionUiEffect

    data class ShowMessage(
        val message: String,
    ) : AddTransactionUiEffect
}

private fun amountToCents(amount: String): Long? {
    val value = amount.toBigDecimalOrNull() ?: return null
    return value
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .toLong()
}

private fun centsToAmountString(amount: Long): String =
    BigDecimal(amount)
        .movePointLeft(2)
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()

private fun recordTypeFromTransaction(type: TransactionType): RecordType =
    when (type) {
        TransactionType.INCOME -> RecordType.INCOME
        TransactionType.EXPENSE -> RecordType.EXPENSE
        TransactionType.TRANSFER -> RecordType.TRANSFER
    }

data class QuickTemplate(
    val id: String,
    val type: RecordType,
    val amountCents: Long,
    val accountId: String? = null,
    val categoryId: String? = null,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
)

private fun Transaction.toTemplate(): QuickTemplate =
    QuickTemplate(
        id = id,
        type = recordTypeFromTransaction(type),
        amountCents = amount,
        accountId = accountId,
        categoryId = categoryId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
    )

private fun templateLabel(
    template: QuickTemplate,
    accounts: List<AccountItem>,
    expenseCategories: List<CategoryItem>,
    incomeCategories: List<CategoryItem>,
): String {
    val amount = centsToAmountString(template.amountCents)
    val accountName = { id: String? ->
        accounts.firstOrNull { it.id == id }?.name ?: "未选择"
    }
    val categoryName = { id: String?, categories: List<CategoryItem> ->
        categories.firstOrNull { it.id == id }?.name ?: "未选择"
    }
    return when (template.type) {
        RecordType.TRANSFER ->
            "转账 · ${accountName(template.fromAccountId)} → ${accountName(template.toAccountId)} · $amount"
        RecordType.EXPENSE ->
            "支出 · ${categoryName(template.categoryId, expenseCategories)} · ${accountName(template.accountId)} · $amount"
        RecordType.INCOME ->
            "收入 · ${categoryName(template.categoryId, incomeCategories)} · ${accountName(template.accountId)} · $amount"
    }
}

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val accountMap: Map<String, Account> = emptyMap(),
    val categoryMap: Map<String, Category> = emptyMap(),
)

private fun transactionTypeLabel(type: TransactionType): String =
    when (type) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
        TransactionType.TRANSFER -> "转账"
    }

private fun transactionAmountText(transaction: Transaction): String {
    val amount = formatAmount(transaction.amount)
    return when (transaction.type) {
        TransactionType.INCOME -> "+$amount"
        TransactionType.EXPENSE -> "-$amount"
        TransactionType.TRANSFER -> amount
    }
}

private fun transactionDirectionText(
    transaction: Transaction,
    accountMap: Map<String, Account>,
): String {
    val accountName = { id: String? ->
        id?.let { accountMap[it]?.name } ?: "未选择"
    }
    return when (transaction.type) {
        TransactionType.TRANSFER ->
            "${accountName(transaction.fromAccountId)} → ${accountName(transaction.toAccountId)}"
        TransactionType.INCOME,
        TransactionType.EXPENSE,
        -> accountName(transaction.accountId)
    }
}

private fun transactionCategoryText(
    transaction: Transaction,
    categoryMap: Map<String, Category>,
): String = transaction.categoryId?.let { categoryMap[it]?.name } ?: "未选择"

private fun noteSummary(
    note: String,
    limit: Int = 20,
): String {
    val content = note.trim()
    return if (content.length > limit) {
        "${content.take(limit)}…"
    } else {
        content
    }
}

@HiltViewModel
class TransactionListViewModel
    @Inject
    constructor(
        private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
        private val queryTransactionsUseCase: QueryTransactionsUseCase,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TransactionListUiState())
        val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val ledgerId = initializeDefaultDataUseCase().ledgerId
                launch {
                    accountRepository.observeAccountsByLedger(ledgerId).collectLatest { accounts ->
                        val map = accounts.associateBy { it.id }
                        _uiState.update { current -> current.copy(accountMap = map) }
                    }
                }
                launch {
                    combine(
                        categoryRepository.observeCategories(ledgerId, CategoryType.INCOME),
                        categoryRepository.observeCategories(ledgerId, CategoryType.EXPENSE),
                    ) { incomeCategories, expenseCategories ->
                        (incomeCategories + expenseCategories).associateBy { it.id }
                    }.collectLatest { categoryMap ->
                        _uiState.update { current -> current.copy(categoryMap = categoryMap) }
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
            }
        }
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.transactions
    val accountMap = uiState.accountMap
    val categoryMap = uiState.categoryMap

    if (transactions.isEmpty()) {
        EmptyState(
            message = "暂无流水，点击下方去记一笔",
            actionLabel = "记一笔",
            onActionClick = onAddTransaction,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(transactions, key = { _, transaction -> transaction.id }) { index, transaction ->
            val currentDate = formatDate(transaction.occurredAt)
            val previousDate =
                transactions.getOrNull(index - 1)?.let { formatDate(it.occurredAt) }
            if (currentDate != previousDate) {
                ListHeader(title = currentDate)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = transactionTypeLabel(transaction.type))
                    Text(text = transactionAmountText(transaction))
                }
                Text(
                    text =
                        if (transaction.type == TransactionType.TRANSFER) {
                            "转账 · ${transactionDirectionText(transaction, accountMap)}"
                        } else {
                            "${transactionCategoryText(transaction, categoryMap)} · ${transactionDirectionText(transaction, accountMap)}"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val note = transaction.note
                Text(
                    text =
                        if (note.isNotBlank()) {
                            "${formatTime(transaction.occurredAt)} · ${noteSummary(note)}"
                        } else {
                            formatTime(transaction.occurredAt)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@HiltViewModel
class AddTransactionViewModel
    @Inject
    constructor(
        private val addIncomeExpenseUseCase: AddIncomeExpenseUseCase,
        private val addTransferUseCase: AddTransferUseCase,
        private val preferenceRepository: PreferenceRepository,
        private val initializeDefaultDataUseCase: InitializeDefaultDataUseCase,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                buildAddTransactionUiState(
                    AddTransactionUiState(),
                ),
            )
        val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()
        private val _effect = MutableSharedFlow<AddTransactionUiEffect>()
        val effect = _effect.asSharedFlow()
        private var latestTransaction: Transaction? = null

        init {
            viewModelScope.launch {
                val result = initializeDefaultDataUseCase()
                val ledgerId = result.ledgerId
                launch {
                    accountRepository.observeAccountsByLedger(ledgerId).collectLatest { accounts ->
                        val items = accounts.map { it.toItem() }
                        val accountIds = accounts.map { it.id }.toSet()
                        updateState { current ->
                            current.copy(
                                accounts = items,
                                selectedAccountId =
                                    current.selectedAccountId?.takeIf { accountIds.contains(it) },
                                selectedFromAccountId =
                                    current.selectedFromAccountId?.takeIf { accountIds.contains(it) },
                                selectedToAccountId =
                                    current.selectedToAccountId?.takeIf { accountIds.contains(it) },
                            )
                        }
                    }
                }
                launch {
                    categoryRepository
                        .observeCategories(ledgerId, CategoryType.EXPENSE)
                        .collectLatest { categories ->
                            val items = categories.map { it.toItem() }
                            val categoryIds = categories.map { it.id }.toSet()
                            updateState { current ->
                                current.copy(
                                    expenseCategories = items,
                                    selectedCategoryId =
                                        if (current.selectedType == RecordType.EXPENSE) {
                                            current.selectedCategoryId?.takeIf {
                                                categoryIds.contains(it)
                                            }
                                        } else {
                                            current.selectedCategoryId
                                        },
                                )
                            }
                        }
                }
                launch {
                    categoryRepository
                        .observeCategories(ledgerId, CategoryType.INCOME)
                        .collectLatest { categories ->
                            val items = categories.map { it.toItem() }
                            val categoryIds = categories.map { it.id }.toSet()
                            updateState { current ->
                                current.copy(
                                    incomeCategories = items,
                                    selectedCategoryId =
                                        if (current.selectedType == RecordType.INCOME) {
                                            current.selectedCategoryId?.takeIf {
                                                categoryIds.contains(it)
                                            }
                                        } else {
                                            current.selectedCategoryId
                                        },
                                )
                            }
                        }
                }
                launch {
                    transactionRepository
                        .observeTransactionsByLedger(ledgerId)
                        .collectLatest { transactions ->
                            latestTransaction = transactions.firstOrNull()
                            val templates =
                                transactions
                                    .map { it.toTemplate() }
                                    .distinctBy {
                                        "${it.type}-${it.amountCents}-${it.accountId}-${it.categoryId}-${it.fromAccountId}-${it.toAccountId}"
                                    }.take(5)
                            val commonExpenseCategoryIds =
                                transactions
                                    .asSequence()
                                    .filter { it.type == TransactionType.EXPENSE }
                                    .mapNotNull { it.categoryId }
                                    .groupingBy { it }
                                    .eachCount()
                                    .toList()
                                    .sortedByDescending { it.second }
                                    .map { it.first }
                                    .take(6)
                            val commonIncomeCategoryIds =
                                transactions
                                    .asSequence()
                                    .filter { it.type == TransactionType.INCOME }
                                    .mapNotNull { it.categoryId }
                                    .groupingBy { it }
                                    .eachCount()
                                    .toList()
                                    .sortedByDescending { it.second }
                                    .map { it.first }
                                    .take(6)
                            updateState { current ->
                                current.copy(
                                    templates = templates,
                                    commonExpenseCategoryIds = commonExpenseCategoryIds,
                                    commonIncomeCategoryIds = commonIncomeCategoryIds,
                                )
                            }
                        }
                }
            }
        }

        fun onEvent(event: AddTransactionUiEvent) {
            when (event) {
                is AddTransactionUiEvent.TypeSelected -> {
                    updateState { current ->
                        if (event.type == RecordType.TRANSFER) {
                            current.copy(
                                selectedType = event.type,
                                selectedCategoryId = null,
                                selectedAccountId = null,
                            )
                        } else {
                            current.copy(
                                selectedType = event.type,
                                selectedCategoryId = null,
                                selectedFromAccountId = null,
                                selectedToAccountId = null,
                            )
                        }
                    }
                }
                is AddTransactionUiEvent.AmountChanged -> {
                    updateState { current -> current.copy(amount = event.amount) }
                }
                is AddTransactionUiEvent.DateTimeChanged -> {
                    updateState { current -> current.copy(selectedTimestamp = event.timestamp) }
                }
                is AddTransactionUiEvent.NoteChanged -> {
                    updateState { current -> current.copy(note = event.note) }
                }
                is AddTransactionUiEvent.MerchantChanged -> {
                    updateState { current -> current.copy(merchant = event.merchant) }
                }
                is AddTransactionUiEvent.AccountSelected -> {
                    updateState { current -> current.copy(selectedAccountId = event.accountId) }
                }
                is AddTransactionUiEvent.CategorySelected -> {
                    updateState { current -> current.copy(selectedCategoryId = event.categoryId) }
                }
                is AddTransactionUiEvent.FromAccountSelected -> {
                    updateState { current -> current.copy(selectedFromAccountId = event.accountId) }
                }
                is AddTransactionUiEvent.ToAccountSelected -> {
                    updateState { current -> current.copy(selectedToAccountId = event.accountId) }
                }
                is AddTransactionUiEvent.QuickAmountSelected -> {
                    updateState { current -> current.copy(amount = event.amount) }
                }
                is AddTransactionUiEvent.TemplateSelected -> {
                    updateState { current ->
                        val template = event.template
                        if (template.type == RecordType.TRANSFER) {
                            current.copy(
                                selectedType = template.type,
                                amount = centsToAmountString(template.amountCents),
                                selectedAccountId = null,
                                selectedCategoryId = null,
                                selectedFromAccountId = template.fromAccountId,
                                selectedToAccountId = template.toAccountId,
                            )
                        } else {
                            current.copy(
                                selectedType = template.type,
                                amount = centsToAmountString(template.amountCents),
                                selectedAccountId = template.accountId,
                                selectedCategoryId = template.categoryId,
                                selectedFromAccountId = null,
                                selectedToAccountId = null,
                            )
                        }
                    }
                }
                AddTransactionUiEvent.CopyPreviousClicked -> {
                    viewModelScope.launch {
                        val transaction = latestTransaction
                        if (transaction == null) {
                            _effect.emit(AddTransactionUiEffect.ShowMessage("暂无上一笔"))
                            return@launch
                        }
                        updateState { current ->
                            val recordType = recordTypeFromTransaction(transaction.type)
                            current.copy(
                                selectedType = recordType,
                                amount = centsToAmountString(transaction.amount),
                                note = transaction.note,
                                selectedTimestamp = transaction.occurredAt,
                                selectedAccountId =
                                    if (recordType == RecordType.TRANSFER) {
                                        null
                                    } else {
                                        transaction.accountId
                                    },
                                selectedCategoryId =
                                    if (recordType == RecordType.TRANSFER) {
                                        null
                                    } else {
                                        transaction.categoryId
                                    },
                                selectedFromAccountId =
                                    if (recordType == RecordType.TRANSFER) {
                                        transaction.fromAccountId
                                    } else {
                                        null
                                    },
                                selectedToAccountId =
                                    if (recordType == RecordType.TRANSFER) {
                                        transaction.toAccountId
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                }
                AddTransactionUiEvent.SaveClicked -> {
                    viewModelScope.launch {
                        val current = _uiState.value
                        if (!current.isSaveEnabled) {
                            return@launch
                        }
                        val result =
                            runCatching {
                                val ledgerId =
                                    requireNotNull(preferenceRepository.getDefaultLedgerId())
                                val amountCents = requireNotNull(amountToCents(current.amount))
                                when (current.selectedType) {
                                    RecordType.INCOME -> {
                                        val accountId = requireNotNull(current.selectedAccountId)
                                        val categoryId = requireNotNull(current.selectedCategoryId)
                                        addIncomeExpenseUseCase(
                                            AddIncomeExpenseParams(
                                                ledgerId = ledgerId,
                                                type = TransactionType.INCOME,
                                                amount = amountCents,
                                                currency = "CNY",
                                                occurredAt = current.selectedTimestamp,
                                                note = current.note,
                                                accountId = accountId,
                                                categoryId = categoryId,
                                            ),
                                        )
                                    }
                                    RecordType.EXPENSE -> {
                                        val accountId = requireNotNull(current.selectedAccountId)
                                        val categoryId = requireNotNull(current.selectedCategoryId)
                                        addIncomeExpenseUseCase(
                                            AddIncomeExpenseParams(
                                                ledgerId = ledgerId,
                                                type = TransactionType.EXPENSE,
                                                amount = amountCents,
                                                currency = "CNY",
                                                occurredAt = current.selectedTimestamp,
                                                note = current.note,
                                                accountId = accountId,
                                                categoryId = categoryId,
                                            ),
                                        )
                                    }
                                    RecordType.TRANSFER -> {
                                        val fromAccountId =
                                            requireNotNull(current.selectedFromAccountId)
                                        val toAccountId = requireNotNull(current.selectedToAccountId)
                                        addTransferUseCase(
                                            AddTransferParams(
                                                ledgerId = ledgerId,
                                                amount = amountCents,
                                                currency = "CNY",
                                                occurredAt = current.selectedTimestamp,
                                                note = current.note,
                                                fromAccountId = fromAccountId,
                                                toAccountId = toAccountId,
                                            ),
                                        )
                                    }
                                }
                            }
                        if (result.isSuccess) {
                            _effect.emit(AddTransactionUiEffect.SaveSuccess)
                        } else {
                            _effect.emit(AddTransactionUiEffect.SaveFailed("保存失败，请重试"))
                        }
                    }
                }
            }
        }

        private fun updateState(reducer: (AddTransactionUiState) -> AddTransactionUiState) {
            _uiState.update { current ->
                buildAddTransactionUiState(reducer(current))
            }
        }
    }

private fun Account.toItem(): AccountItem =
    AccountItem(
        id = id,
        name = name,
        balance = currentBalance,
    )

private fun Category.toItem(): CategoryItem =
    CategoryItem(
        id = id,
        name = name,
        parentId = parentId,
    )

@Suppress("ktlint:standard:function-naming")
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val categories =
        if (uiState.selectedType == RecordType.INCOME) {
            uiState.incomeCategories
        } else {
            uiState.expenseCategories
        }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                AddTransactionUiEffect.SaveSuccess -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("保存成功")
                    }
                    onNavigateBack()
                }
                is AddTransactionUiEffect.SaveFailed -> {
                    coroutineScope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = effect.message,
                                actionLabel = "重试",
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onEvent(AddTransactionUiEvent.SaveClicked)
                        }
                    }
                }
                is AddTransactionUiEffect.ShowMessage -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "类型",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecordType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.selectedType == type,
                    onClick = {
                        viewModel.onEvent(AddTransactionUiEvent.TypeSelected(type))
                    },
                    label = { Text(type.label) },
                )
            }
        }
        OutlinedButton(
            onClick = { viewModel.onEvent(AddTransactionUiEvent.CopyPreviousClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "复制上一笔")
        }
        if (uiState.templates.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "常用模板",
                    style = MaterialTheme.typography.bodyMedium,
                )
                uiState.templates.forEach { template ->
                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(AddTransactionUiEvent.TemplateSelected(template))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text =
                                templateLabel(
                                    template,
                                    uiState.accounts,
                                    uiState.expenseCategories,
                                    uiState.incomeCategories,
                                ),
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "快捷金额",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.quickAmounts.forEach { amount ->
                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(AddTransactionUiEvent.QuickAmountSelected(amount))
                        },
                    ) {
                        Text(text = amount)
                    }
                }
            }
        }
        if (uiState.selectedType != RecordType.TRANSFER) {
            val commonCategories =
                if (uiState.selectedType == RecordType.INCOME) {
                    uiState.incomeCategories.filter {
                        uiState.commonIncomeCategoryIds.contains(it.id)
                    }
                } else {
                    uiState.expenseCategories.filter {
                        uiState.commonExpenseCategoryIds.contains(it.id)
                    }
                }
            if (commonCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "常用分类",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        commonCategories.forEach { category ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.onEvent(
                                        AddTransactionUiEvent.CategorySelected(category.id),
                                    )
                                },
                            ) {
                                Text(text = category.name)
                            }
                        }
                    }
                }
            }
        }
        AmountInput(
            amount = uiState.amount,
            onAmountChange = { viewModel.onEvent(AddTransactionUiEvent.AmountChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.amountError != null,
            errorMessage = uiState.amountError,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "日期时间",
                style = MaterialTheme.typography.bodyMedium,
            )
            DateTimePicker(
                timestamp = uiState.selectedTimestamp,
                onDateTimeSelected = { viewModel.onEvent(AddTransactionUiEvent.DateTimeChanged(it)) },
            )
        }
        OutlinedTextField(
            value = uiState.note,
            onValueChange = { viewModel.onEvent(AddTransactionUiEvent.NoteChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注") },
            maxLines = 3,
        )
        OutlinedTextField(
            value = uiState.merchant,
            onValueChange = { viewModel.onEvent(AddTransactionUiEvent.MerchantChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("商家") },
            singleLine = true,
        )
        if (uiState.selectedType != RecordType.TRANSFER) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountSelector(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        onAccountSelected = {
                            viewModel.onEvent(AddTransactionUiEvent.AccountSelected(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val accountError = uiState.accountError
                    if (accountError != null) {
                        Text(
                            text = accountError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CategorySelector(
                        categories = categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelected = {
                            viewModel.onEvent(AddTransactionUiEvent.CategorySelected(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val categoryError = uiState.categoryError
                    if (categoryError != null) {
                        Text(
                            text = categoryError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountSelector(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedFromAccountId,
                        onAccountSelected = {
                            viewModel.onEvent(AddTransactionUiEvent.FromAccountSelected(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "转出账户",
                    )
                    val fromAccountError = uiState.fromAccountError
                    if (fromAccountError != null) {
                        Text(
                            text = fromAccountError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountSelector(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedToAccountId,
                        onAccountSelected = {
                            viewModel.onEvent(AddTransactionUiEvent.ToAccountSelected(it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = "转入账户",
                    )
                    val toAccountError = uiState.toAccountError
                    if (toAccountError != null) {
                        Text(
                            text = toAccountError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                viewModel.onEvent(AddTransactionUiEvent.SaveClicked)
            },
            enabled = uiState.isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "保存")
        }
    }
}
