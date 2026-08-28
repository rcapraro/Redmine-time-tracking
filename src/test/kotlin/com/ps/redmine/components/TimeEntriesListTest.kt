package com.ps.redmine.components

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TimeEntriesListTest {

    private val activity = Activity(1, "Development")
    private val project = Project(1, "Test Project")
    private val issue = Issue(1, "Test Issue")

    private var nextId = 1

    private fun entriesByDate(vararg daysWithCount: Pair<LocalDate, Int>): Map<LocalDate, List<TimeEntry>> =
        daysWithCount.associate { (date, count) ->
            date to List(count) { TimeEntry(nextId++, date, 1f, activity, project, issue) }
        }.toSortedMap(compareByDescending { it })

    @Test
    fun `last item is the last row of the week's first day`() {
        // Week of 2025-07-07 (Mon) .. 2025-07-11 (Fri), descending order.
        // 11th: header 0, entry 1 | 10th: header 2, entries 3-4 | 9th: header 5, entry 6
        // 8th: header 7, entry 8 | 7th: header 9, entries 10-12
        val entries = entriesByDate(
            LocalDate(2025, 7, 11) to 1,
            LocalDate(2025, 7, 10) to 2,
            LocalDate(2025, 7, 9) to 1,
            LocalDate(2025, 7, 8) to 1,
            LocalDate(2025, 7, 7) to 3,
        )

        val anchors = weekAnchors(entries, LocalDate(2025, 7, 7), LocalDate(2025, 7, 7))!!

        assertEquals(12, anchors.lastItem)
    }

    @Test
    fun `selected day anchors on its own header and spans down to the block's end`() {
        val entries = entriesByDate(
            LocalDate(2025, 7, 11) to 1,
            LocalDate(2025, 7, 10) to 2,
            LocalDate(2025, 7, 9) to 1,
            LocalDate(2025, 7, 8) to 1,
            LocalDate(2025, 7, 7) to 3,
        )

        val anchors = weekAnchors(entries, LocalDate(2025, 7, 7), LocalDate(2025, 7, 9))!!

        assertEquals(5, anchors.selectedDayHeader)
        assertEquals(3, anchors.headerCount)
        assertEquals(5, anchors.entryCount)
    }

    @Test
    fun `a selected day without entries anchors on the next day below`() {
        // Thursday has no entry: the closest row to keep in view is Wednesday's header.
        val entries = entriesByDate(
            LocalDate(2025, 7, 11) to 1,
            LocalDate(2025, 7, 9) to 1,
            LocalDate(2025, 7, 7) to 1,
        )

        val anchors = weekAnchors(entries, LocalDate(2025, 7, 7), LocalDate(2025, 7, 10))!!

        assertEquals(2, anchors.selectedDayHeader)
    }

    @Test
    fun `no anchor when every day of the block is newer than the selected day`() {
        val entries = entriesByDate(
            LocalDate(2025, 7, 11) to 1,
            LocalDate(2025, 7, 10) to 1,
        )

        val anchors = weekAnchors(entries, LocalDate(2025, 7, 7), LocalDate(2025, 7, 7))!!

        assertNull(anchors.selectedDayHeader)
        assertEquals(3, anchors.lastItem)
    }

    @Test
    fun `ignores days outside the week`() {
        // 14th: header 0, entry 1 | 11th: header 2, entry 3 | 7th: header 4, entry 5
        val entries = entriesByDate(
            LocalDate(2025, 7, 14) to 1,
            LocalDate(2025, 7, 11) to 1,
            LocalDate(2025, 7, 7) to 1,
            LocalDate(2025, 7, 4) to 1,
        )

        val anchors = weekAnchors(entries, LocalDate(2025, 7, 7), LocalDate(2025, 7, 7))!!

        assertEquals(5, anchors.lastItem)
        assertEquals(4, anchors.selectedDayHeader)
    }

    @Test
    fun `handles a week partially covered by the month`() {
        // August 2025 starts on a Friday: the week of 2025-07-28 only has the 1st.
        // 4th: header 0, entry 1 | 1st: header 2, entries 3-4
        val entries = entriesByDate(
            LocalDate(2025, 8, 4) to 1,
            LocalDate(2025, 8, 1) to 2,
        )

        val anchors = weekAnchors(entries, LocalDate(2025, 7, 28), LocalDate(2025, 8, 1))!!

        assertEquals(4, anchors.lastItem)
        assertEquals(2, anchors.selectedDayHeader)
    }

    @Test
    fun `returns null when the week has no entry`() {
        val entries = entriesByDate(
            LocalDate(2025, 7, 14) to 1,
            LocalDate(2025, 7, 4) to 1,
        )

        assertNull(weekAnchors(entries, LocalDate(2025, 7, 7), LocalDate(2025, 7, 7)))
    }
}
