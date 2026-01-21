package com.countingstar.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun homeScreen(
    greeting: StateFlow<String>,
    onAddTransaction: () -> Unit,
) {
    val greetingState: State<String> = greeting.collectAsState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = greetingState.value,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddTransaction) {
            Text(text = "记一笔")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun homeScreenPreview() {
    homeScreen(
        greeting = kotlinx.coroutines.flow.MutableStateFlow("Counting Star"),
        onAddTransaction = {},
    )
}
