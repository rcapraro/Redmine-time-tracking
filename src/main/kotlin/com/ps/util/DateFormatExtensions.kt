package com.ps.util

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

fun YearMonth.format(): String {
    return "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"
}