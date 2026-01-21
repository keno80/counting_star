package com.countingstar.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

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
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            pendingDateMillis = selectedDate
                            showTimePicker = true
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

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val timePickerState =
            rememberTimePickerState(
                initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = calendar.get(Calendar.MINUTE),
                is24Hour = true,
            )
        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
                pendingDateMillis = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val baseDate = pendingDateMillis ?: timestamp
                        val newTimestamp =
                            combineDateAndTime(
                                baseDate,
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                        pendingDateMillis = null
                        showTimePicker = false
                        onDateTimeSelected(newTimestamp)
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        pendingDateMillis = null
                    },
                ) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = { showDatePicker = true }) {
            Text(text = formatDate(timestamp))
        }
        TextButton(
            onClick = {
                pendingDateMillis = null
                showTimePicker = true
            },
        ) {
            Text(text = formatTime(timestamp))
        }
    }
}

private fun combineDateAndTime(
    dateMillis: Long,
    hour: Int,
    minute: Int,
): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun formatDate(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun formatTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter =
        DateTimeFormatter
            .ofPattern("HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
