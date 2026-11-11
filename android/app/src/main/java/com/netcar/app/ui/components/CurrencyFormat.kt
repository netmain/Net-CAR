package com.netcar.app.ui.components

import java.util.Locale

/**
 * Formats a monetary amount with a readable space between the currency
 * symbol and the value (e.g. "DA 2000.00" instead of "DA2000.00").
 */
fun formatMoney(symbol: String, amount: Double, decimals: Int = 2): String =
    formatMoney(symbol, String.format(Locale.US, "%.${decimals}f", amount))

fun formatMoney(symbol: String, amount: String): String {
    val trimmedSymbol = symbol.trim()
    val trimmedAmount = amount.trim()
    return when {
        trimmedSymbol.isEmpty() -> trimmedAmount
        trimmedAmount.isEmpty() -> trimmedSymbol
        else -> "$trimmedSymbol $trimmedAmount"
    }
}
