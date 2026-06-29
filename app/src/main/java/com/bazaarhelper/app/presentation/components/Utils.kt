package com.bazaarhelper.app.presentation.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bazaarhelper.app.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Double.formatCurrency(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0
    return "${formatter.format(this)} ${stringResource(R.string.currency_symbol)}"
}

fun LocalDate.formatArabic(): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return this.format(formatter)
}

@Composable
fun monthName(month: Int): String {
    return stringResource(getMonthResource(month))
}

fun getMonthName(context: Context, month: Int): String {
    return context.getString(getMonthResource(month))
}

private fun getMonthResource(month: Int): Int {
    return when (month) {
        1 -> R.string.january
        2 -> R.string.february
        3 -> R.string.march
        4 -> R.string.april
        5 -> R.string.may
        6 -> R.string.june
        7 -> R.string.july
        8 -> R.string.august
        9 -> R.string.september
        10 -> R.string.october
        11 -> R.string.november
        12 -> R.string.december
        else -> R.string.january
    }
}

fun String.normalizeDigits(): String {
    var result = this
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    for (i in 0..9) {
        result = result.replace(arabicDigits[i], i.toString()[0])
    }
    return result
}
