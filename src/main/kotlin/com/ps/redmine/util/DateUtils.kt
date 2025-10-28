package com.ps.redmine.util

import kotlinx.datetime.*
import java.time.LocalDate as JavaLocalDate
import java.time.YearMonth as JavaYearMonth

val today: LocalDate
    get() = java.time.LocalDate.now().toKotlin()

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

/**
 * Data class representing a week with its start date, end date, and working days count.
 */
data class WeekInfo(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val workingDays: Int
)

/**
 * Gets all weeks in a given month with their working days count.
 * A week starts on Monday and ends on Sunday.
 */
fun getWeeksInMonth(year: Int, month: Int): List<WeekInfo> {
    val firstDay = LocalDate(year, month, 1)
    val lastDay = if (month == 12) {
        LocalDate(year + 1, 1, 1).minus(1, DateTimeUnit.DAY)
    } else {
        LocalDate(year, month + 1, 1).minus(1, DateTimeUnit.DAY)
    }

    val weeks = mutableListOf<WeekInfo>()
    var currentDate = firstDay

    while (currentDate <= lastDay) {
        // Find the start of the week (Monday)
        val weekStart = currentDate.minus((currentDate.dayOfWeek.isoDayNumber - 1), DateTimeUnit.DAY)

        // Find the end of the week (Sunday)
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)

        // Calculate working days in this week that fall within the month
        var workingDays = 0
        var dayInWeek = weekStart

        while (dayInWeek <= weekEnd) {
            // Only count days that are within the month and are working days (Monday-Friday)
            if (dayInWeek >= firstDay && dayInWeek <= lastDay && dayInWeek.dayOfWeek.isoDayNumber in 1..5) {
                workingDays++
            }
            dayInWeek = dayInWeek.plus(1, DateTimeUnit.DAY)
        }

        // Only add weeks that have at least one day in the current month
        if (weekEnd >= firstDay && weekStart <= lastDay) {
            weeks.add(WeekInfo(weekStart, weekEnd, workingDays))
        }

        // Move to the next week
        currentDate = weekEnd.plus(1, DateTimeUnit.DAY)
    }

    return weeks
}

