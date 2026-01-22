package com.countingstar.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object HomeDestination {
    const val ROUTE = "home"
}

fun NavGraphBuilder.homeRoute(
    onAddTransaction: () -> Unit,
    onTransfer: () -> Unit,
    onFilter: () -> Unit,
    onStatisticsClick: () -> Unit,
) {
    composable(route = HomeDestination.ROUTE) {
        homeRouteContent(
            onAddTransaction = onAddTransaction,
            onTransfer = onTransfer,
            onFilter = onFilter,
            onStatisticsClick = onStatisticsClick,
        )
    }
}

@Composable
fun homeRouteContent(
    onAddTransaction: () -> Unit,
    onTransfer: () -> Unit,
    onFilter: () -> Unit,
    onStatisticsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    homeScreen(
        uiState = uiState,
        onAddTransaction = onAddTransaction,
        onTransfer = onTransfer,
        onFilter = onFilter,
        onStatisticsClick = onStatisticsClick,
    )
}
