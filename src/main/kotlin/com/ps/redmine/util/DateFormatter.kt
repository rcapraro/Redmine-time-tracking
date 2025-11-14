package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import java.util.*

object DateFormatter {
    fun formatShort(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return when (locale.language.lowercase()) {
            "en" -> String.format("%02d/%02d/%02d", date.month.number, date.day, date.year % 100)
            else -> String.format("%02d/%02d/%04d", date.day, date.month.number, date.year)
        }
    }

    fun formatFull(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val dayName = LocaleNames.weekdayName(date.dayOfWeek.isoDayNumber, locale, full = true)
        val monthName = LocaleNames.monthName(date.monthNumber, locale, full = true)
        return when (locale.language.lowercase()) {
            "en" -> "$dayName, $monthName ${date.day}, ${date.year}"
            else -> "$dayName ${date.day} $monthName ${date.year}"
        }
    }
}
