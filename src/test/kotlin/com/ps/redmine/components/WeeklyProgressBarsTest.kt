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

    private val testActivity = Activity(1, "Development")
    private val testProject = Project(1, "Test Project")
    private val testIssue = Issue(1, "Test Issue")

    @Test
    fun `test calculateWeeklyProgress with empty time entries`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
        val timeEntries = emptyList<TimeEntry>()

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth)

        assertTrue(weeklyProgress.isNotEmpty(), "Should have weeks for January 2024")
        weeklyProgress.forEach { progress ->
            assertEquals(0f, progress.actualHours, "Actual hours should be 0 for empty entries")
            assertEquals(
                progress.weekInfo.workingDays * 7.5f,
                progress.expectedHours,
                "Expected hours should be working days * 7.5"
            )
            assertEquals(0f, progress.progressPercentage, "Progress percentage should be 0 for empty entries")
        }
    }

    @Test
    fun `test calculateWeeklyProgress with time entries`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
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

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth)

        assertTrue(weeklyProgress.isNotEmpty(), "Should have weeks for January 2024")

        // First week should have some progress
        val firstWeek = weeklyProgress.first()
        assertTrue(firstWeek.actualHours > 0f, "First week should have actual hours")
        assertEquals(15f, firstWeek.actualHours, "First week should have 15 hours (7.5 + 7.5)")

        // Find the week containing January 8th (second week)
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

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth)
        val firstWeek = weeklyProgress.first()

        // First week of January 2024 should have 5 working days (Jan 1 is Monday)
        assertEquals(37.5f, firstWeek.expectedHours, "First week should expect 37.5 hours (5 working days * 7.5)")
        assertEquals(37.5f, firstWeek.actualHours, "First week should have 37.5 actual hours")
        assertEquals(100f, firstWeek.progressPercentage, "Progress should be 100%")
    }

    @Test
    fun `test calculateWeeklyProgress with over 100 percent progress`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
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

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth)
        val firstWeek = weeklyProgress.first()

        assertEquals(50f, firstWeek.actualHours, "Should have 50 actual hours")
        assertEquals(100f, firstWeek.progressPercentage, "Progress should be capped at 100%")
    }

    @Test
    fun `test calculateWeeklyProgress excludes weekends`() {
        val yearMonth = YearMonth.of(2024, 1) // January 2024
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

        val weeklyProgress = calculateWeeklyProgress(timeEntries, yearMonth)
        val firstWeek = weeklyProgress.first()

        // Weekend hours should still be counted in actual hours
        assertEquals(15f, firstWeek.actualHours, "Weekend hours should be included in actual hours")
        // But expected hours should only count working days
        assertTrue(firstWeek.expectedHours > 0f, "Expected hours should be based on working days only")
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
}