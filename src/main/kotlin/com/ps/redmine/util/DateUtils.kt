package com.ps.redmine.util

import kotlinx.datetime.*
import kotlin.time.Clock
import java.time.LocalDate as JavaLocalDate
import java.time.YearMonth as JavaYearMonth

val today: LocalDate
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.toJava(): JavaLocalDate = JavaLocalDate.of(year, month.number, day)

fun JavaLocalDate.toKotlin(): LocalDate = LocalDate(year, monthValue, dayOfMonth)

fun LocalDate.toJavaYearMonth(): JavaYearMonth = JavaYearMonth.of(year, month.number)

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

/**
 * Calculates the number of working days (Monday to Friday) in a given month.
 */
fun getWorkingDaysInMonth(year: Int, month: Int): Int {
    val firstDay = LocalDate(year, month, 1)
    // Get the last day of the month by going to the first day of next month and subtracting 1 day
    val lastDay = if (month == 12) {
        LocalDate(year + 1, 1, 1).minus(1, DateTimeUnit.DAY)
    } else {
        LocalDate(year, month + 1, 1).minus(1, DateTimeUnit.DAY)
    }

    var workingDays = 0
    var currentDay = firstDay

    while (currentDay <= lastDay) {
        // Monday = 1, Tuesday = 2, ..., Friday = 5, Saturday = 6, Sunday = 7
        if (currentDay.dayOfWeek.isoDayNumber in 1..5) {
            workingDays++
        }
        currentDay = currentDay.plus(1, DateTimeUnit.DAY)
    }

    return workingDays
}
