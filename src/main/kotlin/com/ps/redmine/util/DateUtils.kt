package com.ps.redmine.util

import kotlinx.datetime.*
import java.time.LocalDate as JavaLocalDate
import java.time.YearMonth as JavaYearMonth

val today: LocalDate
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.toJava(): JavaLocalDate = JavaLocalDate.of(year, monthNumber, dayOfMonth)

fun JavaLocalDate.toKotlin(): LocalDate = LocalDate(year, monthValue, dayOfMonth)

fun LocalDate.toJavaYearMonth(): JavaYearMonth = JavaYearMonth.of(year, monthNumber)

/**
 * Returns the next business day (skipping weekends).
 */
fun LocalDate.nextBusinessDay(): LocalDate {
    var nextDay = this.plus(1, DateTimeUnit.DAY)
    // Skip Saturday (6) and Sunday (7)
    while (nextDay.dayOfWeek.isoDayNumber == 6 || nextDay.dayOfWeek.isoDayNumber == 7) {
        nextDay = nextDay.plus(1, DateTimeUnit.DAY)
    }
    return nextDay
}

/**
 * Returns the previous business day (skipping weekends).
 */
fun LocalDate.previousBusinessDay(): LocalDate {
    var prevDay = this.minus(1, DateTimeUnit.DAY)
    // Skip Saturday (6) and Sunday (7)
    while (prevDay.dayOfWeek.isoDayNumber == 6 || prevDay.dayOfWeek.isoDayNumber == 7) {
        prevDay = prevDay.minus(1, DateTimeUnit.DAY)
    }
    return prevDay
}
