package com.countingstar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.countingstar.domain.TransactionType
import com.countingstar.feature.home.HomeDestination
import com.countingstar.feature.home.homeRoute
import com.countingstar.navigation.TopLevelDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("ktlint:standard:function-naming")
@Composable
fun CountingStarApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
        )

        composable(addTransactionRoute) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(TopLevelDestination.TRANSACTIONS.route) {
            EmptyState(message = "流水列表功能开发中")
        }
        composable(TopLevelDestination.STATISTICS.route) {
            EmptyState(message = "统计功能开发中")
        }
        composable(TopLevelDestination.SETTINGS.route) {
            EmptyState(message = "设置功能开发中")
        }
    }
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

    object SaveClicked : AddTransactionUiEvent
}

sealed interface AddTransactionUiEffect {
    object SaveSuccess : AddTransactionUiEffect
}

private fun amountToCents(amount: String): Long? {
    val value = amount.toBigDecimalOrNull() ?: return null
    return value
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .toLong()
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
                AddTransactionUiEvent.SaveClicked -> {
                    viewModelScope.launch {
                        val current = _uiState.value
                        if (!current.isSaveEnabled) {
                            return@launch
                        }
                        val ledgerId = preferenceRepository.getDefaultLedgerId() ?: return@launch
                        val amountCents = amountToCents(current.amount) ?: return@launch
                        when (current.selectedType) {
                            RecordType.INCOME -> {
                                val accountId = current.selectedAccountId ?: return@launch
                                val categoryId = current.selectedCategoryId ?: return@launch
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
                                val accountId = current.selectedAccountId ?: return@launch
                                val categoryId = current.selectedCategoryId ?: return@launch
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
                                val fromAccountId = current.selectedFromAccountId ?: return@launch
                                val toAccountId = current.selectedToAccountId ?: return@launch
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
                        _effect.emit(AddTransactionUiEffect.SaveSuccess)
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
