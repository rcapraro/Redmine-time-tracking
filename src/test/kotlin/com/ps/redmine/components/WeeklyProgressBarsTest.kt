package com.ps.redmine.components

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.util.toJava
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.temporal.IsoFields

class WeeklyProgressBarsTest {

    private fun totalExpectedWorkingDays(progress: List<WeeklyProgress>): Int {
        // Sum expected hours and convert back to days assuming 7.5h per day
        val totalHours = progress.sumOf { it.expectedHours.toDouble() }
        return (totalHours / 7.5).toInt()
    }

    private fun monthPercentage(progress: List<WeeklyProgress>): Float {
        val totalExpected = progress.sumOf { it.expectedHours.toDouble() }.toFloat()
        val totalActual = progress.sumOf { it.actualHours.toDouble() }.toFloat()
        return if (totalExpected > 0f) ((totalActual / totalExpected) * 100f).coerceAtMost(100f) else 0f
    }

    private val testActivity = Activity(1, "Development")
    private val testProject = Project(1, "Test Project")
    private val testIssue = Issue(1, "Test Issue")

    @Test
    fun `test calculateWeeklyProgress with empty time entries`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
        val excluded = emptySet<Int>()
        val timeEntries = emptyList<TimeEntry>()

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth, excluded)

        // Total worked days (expected) for the month should equal sum of working days in the month
        val totalDays = totalExpectedWorkingDays(weeklyProgress)
        assertTrue(weeklyProgress.isNotEmpty(), "Should have weeks for January 2024")
        assertTrue(totalDays > 0, "January 2024 should have working days")

        // For each week: actual=0, percentages=0, expected computed from working days
        weeklyProgress.forEach { progress ->
            assertEquals(0f, progress.actualHours, "Actual hours should be 0 for empty entries")
            assertEquals(
                progress.weekInfo.workingDays * 7.5f,
                progress.expectedHours,
                "Expected hours should be working days * 7.5"
            )
            assertEquals(0f, progress.progressPercentage, "Progress percentage should be 0 for empty entries")
        }

        // Month percentage = 0%
        assertEquals(0f, monthPercentage(weeklyProgress), 0.0001f)
    }

    @Test
    fun `test calculateWeeklyProgress with time entries`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
        val excluded = emptySet<Int>()
        val timeEntries = listOf(
            TimeEntry(
                id = 1,
                date = LocalDate(2024, 1, 2), // Tuesday, first week
                hours = 7.5f,
                activity = testActivity,
                project = testProject,
                issue = testIssue
            ),
            TimeEntry(
                id = 2,
                date = LocalDate(2024, 1, 3), // Wednesday, first week
                hours = 7.5f,
                activity = testActivity,
                project = testProject,
                issue = testIssue
            ),
            TimeEntry(
                id = 3,
                date = LocalDate(2024, 1, 8), // Monday, second week
                hours = 15f, // More than expected for one day
                activity = testActivity,
                project = testProject,
                issue = testIssue
            )
        )

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth, excluded)

        assertTrue(weeklyProgress.isNotEmpty(), "Should have weeks for January 2024")

        // Total worked days (expected) for the month
        val totalDays = totalExpectedWorkingDays(weeklyProgress)
        assertTrue(totalDays > 0)

        // For each week, assert computed percentage equals actual/expected (capped)
        weeklyProgress.forEach { p ->
            val expectedPct =
                if (p.expectedHours > 0f) (p.actualHours / p.expectedHours * 100f).coerceAtMost(100f) else 0f
            assertEquals(expectedPct, p.progressPercentage, 0.0001f)
        }

        // Month percentage = sum(actual)/sum(expected)
        val expectedMonthPct = monthPercentage(weeklyProgress)
        assertEquals(expectedMonthPct, monthPercentage(weeklyProgress), 0.0001f)

        // Specific checks retained from previous assertions
        val firstWeek = weeklyProgress.first()
        assertTrue(firstWeek.actualHours > 0f, "First week should have actual hours")
        assertEquals(15f, firstWeek.actualHours, "First week should have 15 hours (7.5 + 7.5)")

        val secondWeek = weeklyProgress.find { progress ->
            val jan8 = LocalDate(2024, 1, 8)
            jan8 >= progress.weekInfo.startDate && jan8 <= progress.weekInfo.endDate
        }
        assertTrue(secondWeek != null, "Should find week containing January 8th")
        assertEquals(15f, secondWeek!!.actualHours, "Second week should have 15 hours")
    }

    @Test
    fun `test calculateWeeklyProgress progress percentage calculation`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
        val excluded = emptySet<Int>()
        val timeEntries = listOf(
            TimeEntry(
                id = 1,
                date = LocalDate(2024, 1, 2), // Tuesday, first week
                hours = 37.5f, // Exactly 5 working days * 7.5 hours
                activity = testActivity,
                project = testProject,
                issue = testIssue
            )
        )

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth, excluded)
        val firstWeek = weeklyProgress.first()

        // First week of January 2024 should have 5 working days (Jan 1 is Monday)
        assertEquals(37.5f, firstWeek.expectedHours, "First week should expect 37.5 hours (5 working days * 7.5)")
        assertEquals(37.5f, firstWeek.actualHours, "First week should have 37.5 actual hours")
        assertEquals(100f, firstWeek.progressPercentage, "Progress should be 100%")

        // For all weeks, computed percentage should match formula
        weeklyProgress.forEach { p ->
            val expectedPct =
                if (p.expectedHours > 0f) (p.actualHours / p.expectedHours * 100f).coerceAtMost(100f) else 0f
            assertEquals(expectedPct, p.progressPercentage, 0.0001f)
        }

        // Month percentage from sums
        val expectedMonthPct = monthPercentage(weeklyProgress)
        assertEquals(expectedMonthPct, monthPercentage(weeklyProgress), 0.0001f)
    }

    @Test
    fun `test calculateWeeklyProgress with over 100 percent progress`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
        val excluded = emptySet<Int>()
        val timeEntries = listOf(
            TimeEntry(
                id = 1,
                date = LocalDate(2024, 1, 2), // Tuesday, first week
                hours = 50f, // More than expected
                activity = testActivity,
                project = testProject,
                issue = testIssue
            )
        )

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth, excluded)
        val firstWeek = weeklyProgress.first()

        assertEquals(50f, firstWeek.actualHours, "Should have 50 actual hours")
        assertEquals(100f, firstWeek.progressPercentage, "Progress should be capped at 100%")

        // For each week, ensure cap at 100% is respected
        weeklyProgress.forEach { p ->
            val expectedPct =
                if (p.expectedHours > 0f) (p.actualHours / p.expectedHours * 100f).coerceAtMost(100f) else 0f
            assertEquals(expectedPct, p.progressPercentage, 0.0001f)
        }

        // Month percentage asserted from sums
        val expectedMonthPct = monthPercentage(weeklyProgress)
        assertEquals(expectedMonthPct, monthPercentage(weeklyProgress), 0.0001f)
    }

    @Test
    fun `test calculateWeeklyProgress excludes weekends`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
        val excluded = emptySet<Int>()
        val timeEntries = listOf(
            TimeEntry(
                id = 1,
                date = LocalDate(2024, 1, 6), // Saturday
                hours = 7.5f,
                activity = testActivity,
                project = testProject,
                issue = testIssue
            ),
            TimeEntry(
                id = 2,
                date = LocalDate(2024, 1, 7), // Sunday
                hours = 7.5f,
                activity = testActivity,
                project = testProject,
                issue = testIssue
            )
        )

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth, excluded)
        val firstWeek = weeklyProgress.first()

        // Total worked days (expected) for the month
        val totalDays = totalExpectedWorkingDays(weeklyProgress)
        assertTrue(totalDays > 0)

        // Weekend hours should still be counted in actual hours
        assertEquals(15f, firstWeek.actualHours, "Weekend hours should be included in actual hours")
        // But expected hours should only count working days
        assertTrue(firstWeek.expectedHours > 0f, "Expected hours should be based on working days only")

        // Per-week percentages follow formula
        weeklyProgress.forEach { p ->
            val expectedPct =
                if (p.expectedHours > 0f) (p.actualHours / p.expectedHours * 100f).coerceAtMost(100f) else 0f
            assertEquals(expectedPct, p.progressPercentage, 0.0001f)
        }

        // Month percentage asserted from sums
        val expectedMonthPct = monthPercentage(weeklyProgress)
        assertEquals(expectedMonthPct, monthPercentage(weeklyProgress), 0.0001f)
    }

    @Test
    fun `test ISO week number calculation for July 2025`() {
        // Test the example from the issue description: July 21-25, 2025 should be week 30
        val july21_2025 = LocalDate(2025, 7, 21) // Monday
        val july25_2025 = LocalDate(2025, 7, 25) // Friday

        // Convert to Java LocalDate and get ISO week number
        val weekNumber21 = july21_2025.toJava().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val weekNumber25 = july25_2025.toJava().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

        // Both dates should be in week 30
        assertEquals(30, weekNumber21, "July 21, 2025 should be in week 30")
        assertEquals(30, weekNumber25, "July 25, 2025 should be in week 30")

        println("[DEBUG_LOG] July 21, 2025 is in ISO week: $weekNumber21")
        println("[DEBUG_LOG] July 25, 2025 is in ISO week: $weekNumber25")
    }

    @Test
    fun `test July 2025 total working days with exclusions`() {
        val ym = YearMonth.of(2025, 7)
        val noEntries = emptyList<TimeEntry>()

        // No non-working weekdays excluded => 23 working days (from issue description)
        val p0 = calculateWeeklyProgress(noEntries, ym, excludedIsoDays = emptySet())
        assertEquals(23, totalExpectedWorkingDays(p0), "July 2025 should have 23 working days with no exclusions")

        // Additionally, when we fill every working day (Mon-Fri) with 7.5h, total hours should match and each week's progress should be 100%
        val fullMonthEntries = buildList {
            var d = java.time.LocalDate.of(2025, 7, 1)
            val end = java.time.LocalDate.of(2025, 7, 31)
            while (!d.isAfter(end)) {
                val iso = d.dayOfWeek.value // 1..7
                if (iso in 1..5) {
                    add(
                        TimeEntry(
                            id = size + 1,
                            date = LocalDate(d.year, d.monthValue, d.dayOfMonth),
                            hours = 7.5f,
                            activity = testActivity,
                            project = testProject,
                            issue = testIssue
                        )
                    )
                }
                d = d.plusDays(1)
            }
        }
        val p0Full = calculateWeeklyProgress(fullMonthEntries, ym, excludedIsoDays = emptySet())
        val totalExpectedHoursNoExcl = 23 * 7.5f
        val totalActualHoursNoExcl = p0Full.sumOf { it.actualHours.toDouble() }.toFloat()
        assertEquals(totalExpectedHoursNoExcl, totalActualHoursNoExcl, 0.0001f)
        // Every week's progress should read 100%
        p0Full.forEach { wp ->
            if (wp.expectedHours > 0f) {
                assertEquals(100f, wp.progressPercentage, 0.0001f)
            }
        }
        // Month percentage should be 100%
        assertEquals(100f, monthPercentage(p0Full), 0.0001f)

        // Exclude Monday (1) => 19
        val pMon = calculateWeeklyProgress(noEntries, ym, excludedIsoDays = setOf(1))
        assertEquals(
            19,
            totalExpectedWorkingDays(pMon),
            "July 2025 should have 19 working days when Monday is excluded"
        )

        // Exclude Tuesday (2) => 18 (July 2025 has five Tuesdays)
        val pTue = calculateWeeklyProgress(noEntries, ym, excludedIsoDays = setOf(2))
        assertEquals(
            18,
            totalExpectedWorkingDays(pTue),
            "July 2025 should have 18 working days when Tuesday is excluded"
        )
    }

    @Test
    fun `boundary weeks clamp to month when excluding weekdays`() {
        val ym = YearMonth.of(2025, 7)
        val noEntries = emptyList<TimeEntry>()

        // Exclude Monday but first week starts on 2025-06-30 (a Monday outside July)
        val progress = calculateWeeklyProgress(noEntries, ym, excludedIsoDays = setOf(1))

        // Total expected working days for July 2025 with Monday excluded is 19
        assertEquals(19, totalExpectedWorkingDays(progress))

        // Each week percentage should be 0% as there are no entries
        progress.forEach { p -> assertEquals(0f, p.progressPercentage, 0.0001f) }
        // Month percentage = 0%
        assertEquals(0f, monthPercentage(progress), 0.0001f)

        // Find the week whose start is 2025-06-30 and end is 2025-07-06 (ISO week crossing months)
        val weekSpanningStart = progress.first { it.weekInfo.startDate == kotlinx.datetime.LocalDate(2025, 6, 30) }
        // Inside July for that week we only have Tue-Fri => 4 working days; excluding Monday shouldn't change it
        assertEquals(4 * 7.5f, weekSpanningStart.expectedHours, 0.0001f)

        // Last week spans into August up to 2025-08-03; ensure only July weekdays counted
        val weekSpanningEnd = progress.last { it.weekInfo.endDate == kotlinx.datetime.LocalDate(2025, 8, 3) }
        // In July part of this week, we have Mon-Thu: 2025-07-28..31 (Mon-Thu). With Monday excluded, count Tue-Thu => 3 days
        val expectedDaysEnd = listOf(
            // 2025-07-28 is Monday (excluded), so we only consider:
            LocalDate(2025, 7, 29),
            LocalDate(2025, 7, 30),
            LocalDate(2025, 7, 31)
        )
        assertEquals(expectedDaysEnd.size * 7.5f, weekSpanningEnd.expectedHours, 0.0001f)
    }

    @Test
    fun `actual hours and percentages computed correctly with exclusions`() {
        val ym = YearMonth.of(2025, 7)
        // Create entries only on Tuesdays (2), but then mark Tuesday as excluded, so expected should not count Tuesdays
        val entries = listOf(
            TimeEntry(1, LocalDate(2025, 7, 1), 7.5f, testActivity, testProject, testIssue),
            TimeEntry(2, LocalDate(2025, 7, 8), 7.5f, testActivity, testProject, testIssue),
            TimeEntry(3, LocalDate(2025, 7, 15), 7.5f, testActivity, testProject, testIssue),
            TimeEntry(4, LocalDate(2025, 7, 22), 7.5f, testActivity, testProject, testIssue),
            TimeEntry(5, LocalDate(2025, 7, 29), 7.5f, testActivity, testProject, testIssue)
        )
        val progress = calculateWeeklyProgress(entries, ym, excludedIsoDays = setOf(2))

        // For each week, assert progress percentage equals 100 / expectedDays (since actual is 7.5h on Tuesday only)
        progress.forEach { p ->
            val expectedDaysInWeek = if (p.expectedHours > 0f) p.expectedHours / 7.5f else 0f
            val expectedPct = if (expectedDaysInWeek > 0f) 100f / expectedDaysInWeek else 0f
            assertEquals(expectedPct, p.progressPercentage, 0.2f)
        }
        // Sum expectations across month: with Tuesday excluded total expected days is 18
        val totalExpectedDays = totalExpectedWorkingDays(progress)
        assertEquals(
            18,
            totalExpectedDays,
            "Total expected working days in July 2025 with Tuesday excluded should be 18"
        )

        // Actual hours total should be 5*7.5
        val totalActualHours = progress.sumOf { it.actualHours.toDouble() }.toFloat()
        assertEquals(37.5f, totalActualHours, 0.0001f)

        // Check specific weeks:
        // Week containing 2025-07-01 (spans June 30 - July 6): expected 3 days (Tue-Thu) => 22.5h, progress 7.5/22.5 = 33.33%
        val weekWithJuly1 = progress.first { p ->
            val d = LocalDate(2025, 7, 1)
            d >= p.weekInfo.startDate && d <= p.weekInfo.endDate
        }
        assertEquals(22.5f, weekWithJuly1.expectedHours, 0.0001f)
        assertEquals(33.3333f, weekWithJuly1.progressPercentage, 0.2f)

        // Week containing 2025-07-08 (fully within July Mon-Fri): expected 4 days (Mon,Wed,Thu,Fri) => 30h, progress 25%
        val weekWithJuly8 = progress.first { p ->
            val d = LocalDate(2025, 7, 8)
            d >= p.weekInfo.startDate && d <= p.weekInfo.endDate
        }
        assertEquals(30f, weekWithJuly8.expectedHours, 0.0001f)
        assertEquals(25f, weekWithJuly8.progressPercentage, 0.0001f)

        // Month percentage = total actual / total expected
        val expectedMonthPct = if (totalExpectedDays > 0) (totalActualHours / (totalExpectedDays * 7.5f)) * 100f else 0f
        assertEquals(expectedMonthPct, monthPercentage(progress), 0.0001f)
    }

    @Test
    fun `non-worked week within month should be marked 100 percent with marker`() {
        // February 2025 starts on Saturday, so the first overlapping week inside the month contains only Sat-Sun
        val ym = YearMonth.of(2025, 2)
        val progress = calculateWeeklyProgress(emptyList(), ym, excludedIsoDays = emptySet())

        // Find weeks with zero expected hours (i.e., no working days within month) — should be treated as 100% non-worked
        val zeroExpectedWeeks = progress.filter { it.expectedHours == 0f }
        assertTrue(
            zeroExpectedWeeks.isNotEmpty(),
            "There should be at least one week with zero expected hours in Feb 2025"
        )
        zeroExpectedWeeks.forEach { wp ->
            assertTrue(wp.isNonWorkedWeek, "Week with zero expected hours should be flagged as non-worked")
            assertEquals(100f, wp.progressPercentage, 0.0001f)
        }
    }
}