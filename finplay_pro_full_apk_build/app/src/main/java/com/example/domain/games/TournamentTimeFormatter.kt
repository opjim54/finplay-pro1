package com.example.domain.games

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Shared date/time scheduling & live countdown calculation utilities for
 * Free Fire / Esports Tournaments across Admin Panel and User Arena.
 */
object TournamentTimeFormatter {

    /**
     * Formats a live ticking remaining duration in milliseconds into:
     * - "Xd HH:MM:SS" when >= 24 hours remain
     * - "HH:MM:SS" when < 24 hours remain
     * - "শুরু হচ্ছে!" / "STARTING NOW!" when <= 0
     */
    fun formatCountdown(remainingMs: Long, isBn: Boolean): String {
        if (remainingMs <= 0L) return if (isBn) "শুরু হচ্ছে!" else "STARTING NOW!"
        val totalSec = remainingMs / 1000L
        val days = totalSec / 86_400L
        val hours = (totalSec % 86_400L) / 3_600L
        val mins = (totalSec % 3_600L) / 60L
        val secs = totalSec % 60L
        return if (days > 0L) {
            String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        }
    }

    /**
     * Formats a readable schedule timestamp such as "Nov 20, 09:30 PM".
     */
    fun formatScheduleLabel(epochMillis: Long): String {
        if (epochMillis <= 0L) return "TBD"
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(epochMillis))
    }

    /**
     * Combines a UTC-midnight date selection (as emitted by Material 3 DatePicker)
     * with a local hourOfDay (0..23) and minute (0..59) into an exact local epoch millis.
     */
    fun combineUtcDateWithLocalTime(
        utcDateMillis: Long,
        hourOfDay: Int,
        minute: Int,
        localTimeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcDateMillis
        }
        val localCal = Calendar.getInstance(localTimeZone).apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    /**
     * Computes epoch millis for today at [hourOfDay]:[minute] local time;
     * if that moment has already passed relative to [nowMillis], automatically bumps to tomorrow.
     */
    fun nextOccurrenceOfTime(
        hourOfDay: Int,
        minute: Int,
        addDays: Int = 0,
        nowMillis: Long = System.currentTimeMillis(),
        localTimeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(localTimeZone).apply {
            timeInMillis = nowMillis
            if (addDays != 0) add(Calendar.DAY_OF_YEAR, addDays)
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (addDays == 0 && cal.timeInMillis <= nowMillis + 60_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
