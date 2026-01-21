package com.countingstar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.countingstar.feature.home.HomeDestination
import com.countingstar.feature.home.homeRoute
import com.countingstar.navigation.TopLevelDestination
import java.math.BigDecimal

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
            AddTransactionScreen()
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

private enum class RecordType(
    val label: String,
) {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("转账"),
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun AddTransactionScreen() {
    var selectedType by remember { mutableStateOf(RecordType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedFromAccountId by remember { mutableStateOf<String?>(null) }
    var selectedToAccountId by remember { mutableStateOf<String?>(null) }
    val accounts =
        remember {
            listOf(
                AccountItem(id = "cash", name = "现金", balance = 0L),
                AccountItem(id = "card", name = "银行卡", balance = 120_00L),
            )
        }
    val expenseCategories =
        remember {
            listOf(
                CategoryItem(id = "food", name = "餐饮"),
                CategoryItem(id = "transport", name = "交通"),
            )
        }
    val incomeCategories =
        remember {
            listOf(
                CategoryItem(id = "salary", name = "工资"),
                CategoryItem(id = "bonus", name = "奖金"),
            )
        }
    val categories =
        if (selectedType == RecordType.INCOME) {
            incomeCategories
        } else {
            expenseCategories
        }
    val amountValue = amount.toBigDecimalOrNull()
    val amountError =
        when {
            amount.isBlank() -> "金额必填"
            amountValue == null -> "金额格式错误"
            amountValue <= BigDecimal.ZERO -> "金额需大于0"
            else -> null
        }
    val accountError =
        if (selectedType == RecordType.TRANSFER || !selectedAccountId.isNullOrBlank()) {
            null
        } else {
            "账户必填"
        }
    val fromAccountError =
        if (selectedType != RecordType.TRANSFER || !selectedFromAccountId.isNullOrBlank()) {
            null
        } else {
            "转出账户必填"
        }
    val toAccountError =
        if (selectedType != RecordType.TRANSFER) {
            null
        } else if (selectedToAccountId.isNullOrBlank()) {
            "转入账户必填"
        } else if (selectedToAccountId == selectedFromAccountId) {
            "转入账户需不同"
        } else {
            null
        }
    val categoryError =
        if (selectedType == RecordType.TRANSFER || !selectedCategoryId.isNullOrBlank()) {
            null
        } else {
            "分类必填"
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
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
                    selected = selectedType == type,
                    onClick = {
                        selectedType = type
                        selectedCategoryId = null
                        if (type == RecordType.TRANSFER) {
                            selectedAccountId = null
                        } else {
                            selectedFromAccountId = null
                            selectedToAccountId = null
                        }
                    },
                    label = { Text(type.label) },
                )
            }
        }
        AmountInput(
            amount = amount,
            onAmountChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            isError = amountError != null,
            errorMessage = amountError,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "日期时间",
                style = MaterialTheme.typography.bodyMedium,
            )
            DateTimePicker(
                timestamp = selectedTimestamp,
                onDateTimeSelected = { selectedTimestamp = it },
            )
        }
        if (selectedType != RecordType.TRANSFER) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountSelector(
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onAccountSelected = { selectedAccountId = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = { selectedCategoryId = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                        accounts = accounts,
                        selectedAccountId = selectedFromAccountId,
                        onAccountSelected = { selectedFromAccountId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "转出账户",
                    )
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
                        accounts = accounts,
                        selectedAccountId = selectedToAccountId,
                        onAccountSelected = { selectedToAccountId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "转入账户",
                    )
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
    }
}
