package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateFormatter {
    private val shortFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    private val fullFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

    fun formatShort(date: LocalDate): String {
        return date.toJava().format(shortFormatter)
    }

    fun formatFull(date: LocalDate): String {
        return date.toJava().format(fullFormatter)
    }
}