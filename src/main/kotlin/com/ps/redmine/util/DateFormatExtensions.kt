package com.ps.redmine.util

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

fun YearMonth.format(): String {
    return "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"
}