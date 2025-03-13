package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object DateFormatter {
    private fun getFormatter(style: FormatStyle): DateTimeFormatter {
        return DateTimeFormatter.ofLocalizedDate(style).withLocale(Locale.getDefault())
    }

    fun formatShort(date: LocalDate): String {
        return date.toJava().format(getFormatter(FormatStyle.SHORT))
    }

    fun formatFull(date: LocalDate): String {
        return date.toJava().format(getFormatter(FormatStyle.FULL))
    }
}
