package com.countingstar.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeScreen(
    greeting: StateFlow<String>,
) {
    val greetingState: State<String> = greeting.collectAsState()
    Text(text = greetingState.value)
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(greeting = kotlinx.coroutines.flow.MutableStateFlow("Counting Star"))
}
