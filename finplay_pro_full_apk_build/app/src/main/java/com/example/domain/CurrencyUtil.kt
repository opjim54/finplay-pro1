package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val nameEn: String,
    val nameBn: String
)

object CurrencyUtil {
    val supportedCurrencies = listOf(
        CurrencyInfo("BDT", "৳", "Bangladeshi Taka", "বাংলাদেশি টাকা"),
        CurrencyInfo("USD", "$", "US Dollar", "মার্কিন ডলার"),
        CurrencyInfo("EUR", "€", "Euro", "ইউরো"),
        CurrencyInfo("INR", "₹", "Indian Rupee", "ভারতীয় রুপি"),
        CurrencyInfo("GBP", "£", "British Pound", "ব্রিটিশ পাউন্ড"),
        CurrencyInfo("SAR", "﷼", "Saudi Riyal", "সৌদি রিয়াল"),
        CurrencyInfo("AED", "د.إ", "UAE Dirham", "আমিরাত দিরহাম"),
        CurrencyInfo("MYR", "RM", "Malaysian Ringgit", "মালয়েশিয়ান রিঙ্গিত")
    )

    fun symbolFor(code: String): String =
        supportedCurrencies.firstOrNull { it.code == code }?.symbol ?: "৳"

    fun format(amount: Double, currencyCode: String, showSign: Boolean = false): String {
        val symbol = symbolFor(currencyCode)
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.##", symbols)
        val absVal = abs(amount)
        val formatted = df.format(absVal)
        return when {
            amount < 0 -> "-$symbol$formatted"
            showSign && amount > 0 -> "+$symbol$formatted"
            else -> "$symbol$formatted"
        }
    }

    fun formatCompact(amount: Double, currencyCode: String): String {
        val symbol = symbolFor(currencyCode)
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> "$sign$symbol${String.format(Locale.US, "%.1fM", absVal / 1_000_000.0)}"
            absVal >= 1_000 -> "$sign$symbol${String.format(Locale.US, "%.1fK", absVal / 1_000.0)}"
            else -> format(amount, currencyCode)
        }
    }
}

object DateTimeUtil {
    fun monthKey(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthLabel(monthKey: String): String {
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = parser.parse(monthKey) ?: return monthKey
            SimpleDateFormat("MMM yy", Locale.getDefault()).format(date)
        }.getOrDefault(monthKey)
    }

    fun startOfToday(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(ts: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun startOfWeek(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfMonth(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfYear(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
