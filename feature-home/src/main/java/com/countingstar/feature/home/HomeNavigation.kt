package com.countingstar.feature.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object HomeDestination {
    const val ROUTE = "home"
}

fun NavGraphBuilder.homeRoute() {
    composable(route = HomeDestination.ROUTE) {
        homeRouteContent()
    }
}

@Composable
fun homeRouteContent(viewModel: HomeViewModel = hiltViewModel()) {
    homeScreen(
        greeting = viewModel.greeting,
    )
}
