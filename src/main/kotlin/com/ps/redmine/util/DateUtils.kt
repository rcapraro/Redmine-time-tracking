package com.ps.redmine.util

import kotlinx.datetime.*

val today: LocalDate
    get() = run {
        val instant = kotlin.time.Instant.fromEpochMilliseconds(java.lang.System.currentTimeMillis())
        instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
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
    return countWorkingDays(firstDay, lastDay)
}

/**
 * Counts business days (ISO 1..5) in [start, end], optionally excluding ISO weekdays the user
 * has marked as non-working. Both endpoints inclusive.
 */
fun countWorkingDays(start: LocalDate, end: LocalDate, excludedIsoDays: Set<Int> = emptySet()): Int {
    if (end < start) return 0
    var count = 0
    var cursor = start
    while (cursor <= end) {
        val iso = cursor.dayOfWeek.isoDayNumber
        if (iso in 1..5 && !excludedIsoDays.contains(iso)) count++
        cursor = cursor.plus(1, DateTimeUnit.DAY)
    }
    return count
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

    return buildList {
        var currentDate = firstDay
        while (currentDate <= lastDay) {
            val weekStart = currentDate.minus((currentDate.dayOfWeek.isoDayNumber - 1), DateTimeUnit.DAY)
            val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)

            var workingDays = 0
            var dayInWeek = weekStart
            while (dayInWeek <= weekEnd) {
                if (dayInWeek in firstDay..lastDay && dayInWeek.dayOfWeek.isoDayNumber in 1..5) {
                    workingDays++
                }
                dayInWeek = dayInWeek.plus(1, DateTimeUnit.DAY)
            }

            if (weekEnd >= firstDay && weekStart <= lastDay) {
                add(WeekInfo(weekStart, weekEnd, workingDays))
            }

            currentDate = weekEnd.plus(1, DateTimeUnit.DAY)
        }
    }
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

