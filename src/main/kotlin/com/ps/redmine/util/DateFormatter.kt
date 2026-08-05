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

    fun formatShortWithWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        "${weekdayLabel(date, locale)} ${formatShort(date, locale)}"

    fun formatFull(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val dayName = weekdayLabel(date, locale)
        val monthName = LocaleNames.monthName(date.month.number, locale, full = true)
        return when (locale.language.lowercase()) {
            "en" -> "$dayName, $monthName ${date.day}, ${date.year}"
            else -> "$dayName ${date.day} $monthName ${date.year}"
        }
    }

    /**
     * Localized full weekday name, capitalized for use at the start of a label —
     * [LocaleNames] stores the French names lowercase.
     */
    private fun weekdayLabel(date: LocalDate, locale: Locale): String =
        LocaleNames.weekdayName(date.dayOfWeek.isoDayNumber, locale, full = true)
            .replaceFirstChar { it.titlecase(locale) }
}
