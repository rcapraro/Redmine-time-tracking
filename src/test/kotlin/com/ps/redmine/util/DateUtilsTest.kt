package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DateUtilsTest {

    @Test
    fun `test working days calculation for January 2024`() {
        // January 2024: 1st is Monday, 31st is Wednesday
        // Should have 23 working days (31 days - 8 weekend days)
        val workingDays = getWorkingDaysInMonth(2024, 1)
        assertEquals(23, workingDays)
        println("[DEBUG_LOG] January 2024 working days: $workingDays")
    }

    @Test
    fun `test working days calculation for February 2024 (leap year)`() {
        // February 2024: 1st is Thursday, 29th is Thursday (leap year)
        // Should have 21 working days
        val workingDays = getWorkingDaysInMonth(2024, 2)
        assertEquals(21, workingDays)
        println("[DEBUG_LOG] February 2024 working days: $workingDays")
    }

    @Test
    fun `test working days calculation for February 2023 (non-leap year)`() {
        // February 2023: 1st is Wednesday, 28th is Tuesday
        // Should have 20 working days
        val workingDays = getWorkingDaysInMonth(2023, 2)
        assertEquals(20, workingDays)
        println("[DEBUG_LOG] February 2023 working days: $workingDays")
    }

    @Test
    fun `test working days calculation for December 2024`() {
        // December 2024: 1st is Sunday, 31st is Tuesday
        // Should have 22 working days
        val workingDays = getWorkingDaysInMonth(2024, 12)
        assertEquals(22, workingDays)
        println("[DEBUG_LOG] December 2024 working days: $workingDays")
    }

    @Test
    fun `test working days calculation for current month`() {
        // Test with current month to ensure it doesn't crash
        val currentMonth = kotlinx.datetime.YearMonth.now()
        val workingDays = getWorkingDaysInMonth(currentMonth.year, currentMonth.monthValue)
        assertTrue(workingDays > 0, "Working days should be positive")
        assertTrue(workingDays <= 31, "Working days should not exceed 31")
        println("[DEBUG_LOG] Current month (${currentMonth.year}-${currentMonth.monthValue}) working days: $workingDays")
    }

    @Test
    fun `isoWeekNumber for January 1 belonging to previous year week 53`() {
        // 2021-01-01 is a Friday in ISO week 53 of 2020
        assertEquals(53, isoWeekNumber(LocalDate(2021, 1, 1)))
    }

    @Test
    fun `isoWeekNumber for late December belonging to next year week 1`() {
        // 2024-12-30 is a Monday in ISO week 1 of 2025
        assertEquals(1, isoWeekNumber(LocalDate(2024, 12, 30)))
    }

    @Test
    fun `isoWeekNumber for first ISO week of the year`() {
        // 2024-01-01 is a Monday — ISO week 1 of 2024
        assertEquals(1, isoWeekNumber(LocalDate(2024, 1, 1)))
    }

    @Test
    fun `isoWeekNumber boundary at year split`() {
        // 2023-01-02 is a Monday in ISO week 1 of 2023; 2023-01-01 (Sunday) is week 52 of 2022
        assertEquals(52, isoWeekNumber(LocalDate(2023, 1, 1)))
        assertEquals(1, isoWeekNumber(LocalDate(2023, 1, 2)))
    }
}