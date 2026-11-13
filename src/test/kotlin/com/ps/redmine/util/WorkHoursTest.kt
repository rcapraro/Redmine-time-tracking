package com.ps.redmine.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkHoursTest {

    @Test
    fun `an entry that fits in the day is kept whole`() {
        assertEquals(2.5f, WorkHours.hoursKept(2.5f, otherHours = 5f, dailyHours = 7.5f))
    }

    @Test
    fun `an entry overflowing the day keeps only what is left of it`() {
        assertEquals(2.5f, WorkHours.hoursKept(4f, otherHours = 5f, dailyHours = 7.5f))
    }

    @Test
    fun `nothing is kept on a day already full or over`() {
        assertEquals(0f, WorkHours.hoursKept(3.75f, otherHours = 8f, dailyHours = 7.5f))
    }
}
