package com.countingstar.core.ui.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun DateTimePicker(
    timestamp: Long,
    onDateTimeSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Date Picker Logic
    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = timestamp,
            )
        val confirmEnabled =
            remember {
                derivedStateOf { datePickerState.selectedDateMillis != null }
            }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        // Combine selected date with existing time or default time
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            // Here we should ideally launch time picker or just update date
                            // For simplicity, let's update date part only, preserving time?
                            // Or better: update date, then maybe show time picker?
                            // Let's just update date part of the timestamp
                            val newTimestamp = updateDatePart(timestamp, selectedDate)
                            onDateTimeSelected(newTimestamp)
                        }
                    },
                    enabled = confirmEnabled.value,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                ) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // UI Trigger - simplified text button for now
    TextButton(onClick = { showDatePicker = true }, modifier = modifier) {
        Text(text = formatDateTime(timestamp))
    }
}

private fun updateDatePart(
    originalTimestamp: Long,
    newDateMillis: Long,
): Long {
    // Simple implementation: use Calendar (since java.time is desugared)
    val original = Calendar.getInstance().apply { timeInMillis = originalTimestamp }
    val newDate = Calendar.getInstance().apply { timeInMillis = newDateMillis }

    original.set(Calendar.YEAR, newDate.get(Calendar.YEAR))
    original.set(Calendar.MONTH, newDate.get(Calendar.MONTH))
    original.set(Calendar.DAY_OF_MONTH, newDate.get(Calendar.DAY_OF_MONTH))

    return original.timeInMillis
}

fun formatDateTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
