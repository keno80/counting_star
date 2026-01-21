package com.countingstar.feature.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object HomeDestination {
    const val ROUTE = "home"
}

fun NavGraphBuilder.homeRoute(onAddTransaction: () -> Unit) {
    composable(route = HomeDestination.ROUTE) {
        homeRouteContent(
            onAddTransaction = onAddTransaction,
        )
    }
}

@Composable
fun homeRouteContent(
    onAddTransaction: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    homeScreen(
        greeting = viewModel.greeting,
        onAddTransaction = onAddTransaction,
    )
}
