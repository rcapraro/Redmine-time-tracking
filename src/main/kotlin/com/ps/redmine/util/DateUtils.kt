package com.ps.redmine.util

import kotlinx.datetime.*
import kotlin.time.Duration.Companion.milliseconds

val today: LocalDate
    get() = run {
        // Compute current date in UTC using only Kotlin stdlib + kotlinx-datetime
        val epochDays = System.currentTimeMillis().milliseconds.inWholeDays.toInt()
        LocalDate.fromEpochDays(epochDays)
    }

private val WEEKEND_ISO_DAYS: Set<Int> = setOf(6, 7) // Saturday=6, Sunday=7

private fun lastDayOfMonth(year: Int, month: Int): LocalDate {
    val firstOfNext = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return firstOfNext.minus(1, DateTimeUnit.DAY)
}

/**
 * Returns the length of the given month (1..12) in the given year.
 */
fun lengthOfMonth(year: Int, month: Int): Int {
    require(month in 1..12) { "month must be 1..12" }
    val lastOfThis = lastDayOfMonth(year, month)
    return lastOfThis.day
}

/**
 * Returns the next business day (skipping weekends).
 */
fun LocalDate.nextBusinessDay(): LocalDate {
    var nextDay = this.plus(1, DateTimeUnit.DAY)
    // Skip Saturday (6) and Sunday (7)
    while (nextDay.dayOfWeek.isoDayNumber in WEEKEND_ISO_DAYS) {
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
    while (prevDay.dayOfWeek.isoDayNumber in WEEKEND_ISO_DAYS) {
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
    val lastDay = lastDayOfMonth(year, month)

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
    val lastDay = lastDayOfMonth(year, month)

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
            if (dayInWeek in firstDay..lastDay && dayInWeek.dayOfWeek.isoDayNumber in 1..5) {
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

/**
 * Calculates the ISO-8601 week number for the given date.
 * Algorithm: use Thursday-based week definition.
 */
fun isoWeekNumber(date: LocalDate): Int {
    val dayIso = date.dayOfWeek.isoDayNumber // 1=Mon..7=Sun
    // Thursday for this week
    val thursday = date.plus(4 - dayIso, DateTimeUnit.DAY)
    // First week of week-based-year contains Jan 4
    val firstWeekThursday = LocalDate(thursday.year, 1, 4)
    val firstWeekStart = firstWeekThursday.minus(firstWeekThursday.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val thisWeekStart = thursday.minus(thursday.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    val daysBetween = firstWeekStart.daysUntil(thisWeekStart)
    return (daysBetween / 7) + 1
}

