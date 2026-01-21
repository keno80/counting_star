package com.countingstar.core.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal
import java.text.DecimalFormat

@Suppress("ktlint:standard:function-naming")
@Composable
fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "金额",
    currencySymbol: String = "¥",
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = amount,
        onValueChange = { newValue ->
            if (isValidAmountInput(newValue)) {
                onAmountChange(newValue)
            }
        },
        modifier = modifier,
        label = { Text(label) },
        prefix = { Text(currencySymbol) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = isError,
        supportingText =
            if (errorMessage != null) {
                { Text(errorMessage) }
            } else {
                null
            },
        singleLine = true,
    )
}

private fun isValidAmountInput(input: String): Boolean {
    if (input.isEmpty()) return true
    if (input == ".") return true
    // Allow up to 2 decimal places
    val regex = Regex("^\\d*\\.?\\d{0,2}$")
    return input.matches(regex)
}

fun formatAmount(amount: Long): String {
    val decimal = BigDecimal(amount).movePointLeft(2)
    return DecimalFormat("#,##0.00").format(decimal)
}
