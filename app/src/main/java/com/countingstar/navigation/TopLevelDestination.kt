package com.countingstar.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.countingstar.feature.home.HomeDestination

enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
) {
    HOME(HomeDestination.ROUTE, Icons.Default.Home, "首页"),
    STATISTICS("statistics", Icons.Default.Info, "统计"),
    SETTINGS("settings", Icons.Default.Settings, "设置"),
}
