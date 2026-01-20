package com.countingstar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.countingstar.feature.home.HomeDestination
import com.countingstar.feature.home.homeRoute
import com.countingstar.ui.theme.CountingStarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CountingStarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CountingStarNavHost()
                }
            }
        }
    }
}

@Composable
private fun CountingStarNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
    ) {
        homeRoute()
    }
}
