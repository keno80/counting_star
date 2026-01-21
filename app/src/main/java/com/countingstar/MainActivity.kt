package com.countingstar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.countingstar.ui.CountingStarApp
import com.countingstar.ui.theme.countingStarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            countingStarTheme {
                CountingStarApp()
            }
        }
    }
}
