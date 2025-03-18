package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object DateFormatter {
    private fun getFormatter(style: FormatStyle, locale: Locale = Locale.getDefault()): DateTimeFormatter {
        return DateTimeFormatter.ofLocalizedDate(style).withLocale(locale)
    }

    fun formatShort(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.toJava().format(getFormatter(FormatStyle.SHORT, locale))
    }

    fun formatFull(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.toJava().format(getFormatter(FormatStyle.FULL, locale))
    }
}
