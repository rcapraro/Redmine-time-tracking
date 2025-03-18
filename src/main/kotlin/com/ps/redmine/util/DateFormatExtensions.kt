package com.ps.redmine.util

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

fun YearMonth.format(locale: Locale = Locale.getDefault()): String {
    return "${month.getDisplayName(TextStyle.FULL, locale)} $year"
}
