package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlin.time.Duration.Companion.milliseconds

// Extension utilities to work with kotlinx.datetime.YearMonth in an idiomatic way

// Month value accessor (1..12) derived from enum ordinal
val YearMonth.monthValue: Int
    get() = this.month.ordinal + 1

fun YearMonth.lengthOfMonth(): Int = lengthOfMonth(this.year, this.monthValue)

fun YearMonth.atDay(day: Int): LocalDate = LocalDate(this.year, this.monthValue, day)

fun YearMonth.plusMonths(months: Int): YearMonth {
    if (months == 0) return this
    val totalMonths = (year * 12 + (monthValue - 1)) + months
    val newYear = floorDivSafe(totalMonths, 12)
    val newMonth = (totalMonths % 12) + 1
    return YearMonth(newYear, newMonth)
}

fun YearMonth.minusMonths(months: Int): YearMonth = plusMonths(-months)

fun YearMonth.Companion.now(): YearMonth {
    val epochDays = System.currentTimeMillis().milliseconds.inWholeDays.toInt()
    val date = LocalDate.fromEpochDays(epochDays)
    return YearMonth(date.year, date.month)
}

// Portable floorDiv for Int (works for negatives too)
private fun floorDivSafe(a: Int, b: Int): Int {
    var q = a / b
    val r = a % b
    if (r != 0 && (a xor b) < 0) q -= 1
    return q
}
