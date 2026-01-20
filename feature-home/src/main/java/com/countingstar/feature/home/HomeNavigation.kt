package com.countingstar.feature.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object HomeDestination {
    const val route = "home"
}

fun NavGraphBuilder.homeRoute() {
    composable(route = HomeDestination.route) {
        HomeRoute()
    }
}

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    HomeScreen(
        greeting = viewModel.greeting,
    )
}
