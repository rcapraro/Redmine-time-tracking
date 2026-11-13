package com.ps.redmine.lucca

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LuccaModelsTest {

    private val nov3 = LocalDate(2026, 11, 3)
    private val nov4 = LocalDate(2026, 11, 4)

    @Test
    fun `morning and afternoon of the same account make a full day`() {
        val days = groupAbsencesByDay(
            listOf(
                LuccaLeave(nov3, isAM = false, accountName = "CP", isConfirmed = true),
                LuccaLeave(nov3, isAM = true, accountName = "CP", isConfirmed = true),
            )
        )

        assertEquals(listOf(AbsenceDay(nov3, listOf(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)))), days)
    }

    @Test
    fun `half days of different accounts stay separate and days are sorted`() {
        val days = groupAbsencesByDay(
            listOf(
                LuccaLeave(nov4, isAM = true, accountName = "CP", isConfirmed = true),
                LuccaLeave(nov3, isAM = false, accountName = "RTT", isConfirmed = false),
                LuccaLeave(nov3, isAM = true, accountName = "CP", isConfirmed = true),
            )
        )

        assertEquals(
            listOf(
                AbsenceDay(
                    nov3,
                    listOf(
                        AbsenceSlot("CP", AbsencePeriod.MORNING, true),
                        AbsenceSlot("RTT", AbsencePeriod.AFTERNOON, false),
                    ),
                ),
                AbsenceDay(nov4, listOf(AbsenceSlot("CP", AbsencePeriod.MORNING, true))),
            ),
            days,
        )
    }

    @Test
    fun `a full day is pending when either half is pending`() {
        val days = groupAbsencesByDay(
            listOf(
                LuccaLeave(nov3, isAM = true, accountName = "CP", isConfirmed = true),
                LuccaLeave(nov3, isAM = false, accountName = "CP", isConfirmed = false),
            )
        )

        assertEquals(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, false), days.single().slots.single())
    }
}
