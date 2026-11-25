package com.ps.redmine.util

import com.ps.redmine.model.Hours
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkHoursTest {

    private val day = Hours.of(7.5)

    @Test
    fun `an entry that fits in the day is kept whole`() {
        assertEquals(Hours.of(2.5), WorkHours.hoursKept(Hours.of(2.5), otherHours = Hours.of(5.0), dailyHours = day))
    }

    @Test
    fun `an entry overflowing the day keeps only what is left of it`() {
        assertEquals(Hours.of(2.5), WorkHours.hoursKept(Hours.of(4.0), otherHours = Hours.of(5.0), dailyHours = day))
    }

    @Test
    fun `nothing is kept on a day already full or over`() {
        assertEquals(Hours.ZERO, WorkHours.hoursKept(Hours.of(3.75), otherHours = Hours.of(8.0), dailyHours = day))
    }
}
