package com.ps.redmine.lucca

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AbsenceImportTest {

    private val day = LocalDate(2026, 11, 3)
    private val paidLeave = AbsenceTarget(1, "Interne", 100, "Congés payés", 9, "Absence")
    private val rtt = AbsenceTarget(1, "Interne", 101, "RTT", 9, "Absence")
    private val targets = mapOf("CP" to paidLeave, "RTT" to rtt)

    private fun absence(vararg slots: AbsenceSlot) = listOf(AbsenceDay(day, slots.toList()))

    private fun entry(issueId: Int, hours: Float, id: Int = issueId, activityId: Int = 9, comments: String? = null) =
        TimeEntry(id, day, hours, Activity(activityId, "Absence"), Project(1, "Interne"), Issue(issueId, "x"), comments)

    private fun imported(type: String, issueId: Int, hours: Float, id: Int = issueId, activityId: Int = 9) =
        entry(issueId, hours, id, activityId, comments = "$type${ABSENCE_COMMENT_SEPARATOR}Journée")

    private fun planSingle(days: List<AbsenceDay>, entries: List<TimeEntry> = emptyList()) =
        planAbsenceImport(days, entries, targets).single()

    @Test
    fun `a half day is 3,75 h whether morning or afternoon`() {
        assertEquals(3.75f, planSingle(absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true))).hours)
        assertEquals(3.75f, planSingle(absence(AbsenceSlot("CP", AbsencePeriod.AFTERNOON, true))).hours)
        assertEquals(7.5f, planSingle(absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true))).hours)
    }

    @Test
    fun `free day is proposed and selectable`() {
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)))

        assertEquals(AbsenceImportStatus.ToAdd, item.status)
        assertEquals(paidLeave, item.target)
        assertTrue(item.isSelectable)
    }

    @Test
    fun `absence already logged with the same hours is not proposed again`() {
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true)), listOf(entry(100, 3.75f)))

        assertEquals(AbsenceImportStatus.AlreadyEntered, item.status)
        assertFalse(item.isSelectable)
    }

    @Test
    fun `absence logged with other hours is a replaceable conflict`() {
        val existing = entry(100, 7.5f)
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true)), listOf(existing))

        assertEquals(AbsenceImportStatus.DifferentHours(existing, exceedsDay = false), item.status)
        assertTrue(item.isSelectable)
    }

    @Test
    fun `replacing an absence flags a day that would then exceed a full day`() {
        val existing = entry(100, 3.75f)
        val item = planSingle(
            absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)),
            listOf(existing, entry(500, 4f)),
        )

        assertEquals(AbsenceImportStatus.DifferentHours(existing, exceedsDay = true), item.status)
    }

    @Test
    fun `several entries on the absence issue cannot be resolved automatically`() {
        val item = planSingle(
            absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)),
            listOf(entry(100, 2f, id = 1), entry(100, 3f, id = 2)),
        )

        assertEquals(AbsenceImportStatus.Ambiguous(5f), item.status)
        assertFalse(item.isSelectable)
    }

    @Test
    fun `half-day absence next to a half day of work fits`() {
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.AFTERNOON, true)), listOf(entry(500, 3.75f)))

        assertEquals(AbsenceImportStatus.ToAdd, item.status)
    }

    @Test
    fun `absence that would exceed a full day is an overbooking conflict`() {
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)), listOf(entry(500, 4f)))

        assertEquals(AbsenceImportStatus.Overbooked(otherHours = 4f, otherAbsenceHours = 0f), item.status)
        assertTrue(item.isSelectable)
    }

    @Test
    fun `morning and afternoon of different types fill the day together without conflict`() {
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true), AbsenceSlot("RTT", AbsencePeriod.AFTERNOON, true)),
            emptyList(),
            targets,
        )

        assertEquals(listOf(AbsenceImportStatus.ToAdd, AbsenceImportStatus.ToAdd), items.map { it.status })
        assertEquals(listOf(3.75f, 3.75f), items.map { it.hours })
    }

    @Test
    fun `types sharing an issue but not an activity stay separate entries`() {
        val paidLeaveActivity = paidLeave.copy(activityId = 10, activityName = "Congés")
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true), AbsenceSlot("RTT", AbsencePeriod.AFTERNOON, true)),
            listOf(entry(100, 3.75f, activityId = 10)),
            mapOf("CP" to paidLeaveActivity, "RTT" to paidLeave),
        )

        assertEquals(listOf(10, 9), items.map { it.target?.activityId })
        assertEquals(listOf(AbsenceImportStatus.AlreadyEntered, AbsenceImportStatus.ToAdd), items.map { it.status })
        assertEquals(2, items.map { it.key }.distinct().size)
    }

    @Test
    fun `types sharing an issue become a single entry`() {
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true), AbsenceSlot("RTT", AbsencePeriod.AFTERNOON, true)),
            emptyList(),
            mapOf("CP" to paidLeave, "RTT" to paidLeave),
        )

        assertEquals(7.5f, items.single().hours)
        assertEquals(2, items.single().slots.size)
    }

    @Test
    fun `types mapped to the same issue and activity under different stored names become a single entry`() {
        val renamed = paidLeave.copy(issueSubject = "Congés payés 2026")
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true), AbsenceSlot("RTT", AbsencePeriod.AFTERNOON, true)),
            emptyList(),
            mapOf("CP" to paidLeave, "RTT" to renamed),
        )

        assertEquals(7.5f, items.single().hours)
        assertEquals(AbsenceImportStatus.ToAdd, items.single().status)
    }

    @Test
    fun `overbooking counts the other absences of the day not imported yet`() {
        val sickLeave = AbsenceTarget(1, "Interne", 102, "Maladie", 9, "Absence")
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true), AbsenceSlot("Maladie", AbsencePeriod.MORNING, false)),
            emptyList(),
            targets + ("Maladie" to sickLeave),
        )

        assertEquals(
            AbsenceImportStatus.Overbooked(otherHours = 0f, otherAbsenceHours = 3.75f),
            items.first { it.target == paidLeave }.status,
        )
    }

    @Test
    fun `a day only exceeds a full day through what is actually selected`() {
        val sickLeave = AbsenceTarget(1, "Interne", 102, "Maladie", 9, "Absence")
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true), AbsenceSlot("Maladie", AbsencePeriod.MORNING, true)),
            emptyList(),
            targets + ("Maladie" to sickLeave),
        )

        assertEquals(emptySet<LocalDate>(), daysExceedingFullDay(items.filter { it.target == paidLeave }, emptyList()))
        assertEquals(setOf(day), daysExceedingFullDay(items, emptyList()))
    }

    @Test
    fun `a replacement only counts its new hours towards the day`() {
        val existing = entry(100, 7.5f)
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true)), listOf(existing, entry(500, 3.75f)))

        assertEquals(emptySet<LocalDate>(), daysExceedingFullDay(listOf(item), listOf(existing, entry(500, 3.75f))))
    }

    @Test
    fun `an absence entry no longer backed by Lucca is offered for deletion, unticked`() {
        val existing = imported("CP", 100, 7.5f)
        val item = planSingle(emptyList(), listOf(existing, entry(500, 2f)))

        assertEquals(AbsenceImportStatus.Cancelled(existing), item.status)
        assertEquals(paidLeave, item.target)
        assertTrue(item.isSelectable)
        assertFalse(item.isSelectedByDefault())
    }

    @Test
    fun `each cancelled entry of a day is its own item`() {
        val items = planAbsenceImport(emptyList(), listOf(imported("CP", 100, 3.75f, id = 1), imported("CP", 100, 3.75f, id = 2)), targets)

        assertEquals(2, items.map { it.key }.distinct().size)
    }

    @Test
    fun `a leave whose type changed is not pre-selected while the old entry still fills the day`() {
        val oldRtt = imported("RTT", 101, 7.5f)
        val items = planAbsenceImport(absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)), listOf(oldRtt), targets)

        assertEquals(
            listOf(AbsenceImportStatus.Overbooked(7.5f, 0f, cancelledHours = 7.5f), AbsenceImportStatus.Cancelled(oldRtt)),
            items.map { it.status },
        )
        assertFalse(items.first().isSelectedByDefault())
        assertEquals(emptySet<LocalDate>(), daysExceedingFullDay(items, listOf(oldRtt)))
        assertEquals(setOf(day), daysExceedingFullDay(items.take(1), listOf(oldRtt)))
    }

    @Test
    fun `a leave whose type changed on a shared issue is told apart by its activity`() {
        val paidLeaveActivity = paidLeave.copy(activityId = 10, activityName = "Congés")
        val oldRtt = imported("RTT", 100, 7.5f, activityId = 9)
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)),
            listOf(oldRtt),
            mapOf("CP" to paidLeaveActivity, "RTT" to paidLeave),
        )

        assertEquals(
            listOf(AbsenceImportStatus.Overbooked(7.5f, 0f, cancelledHours = 7.5f), AbsenceImportStatus.Cancelled(oldRtt)),
            items.map { it.status },
        )
    }

    @Test
    fun `nothing is taken as cancelled on a day with an unmapped leave`() {
        val items = planAbsenceImport(
            absence(AbsenceSlot("CP 2026-2027", AbsencePeriod.FULL_DAY, true)),
            listOf(imported("CP", 100, 7.5f)),
            targets,
        )

        assertEquals(listOf(AbsenceImportStatus.Unmapped), items.map { it.status })
    }

    @Test
    fun `an entry on an absence issue that the import did not create is never offered for deletion`() {
        val publicHoliday = entry(100, 7.5f, comments = "Jour férié")

        assertEquals(emptyList<AbsenceImportItem>(), planAbsenceImport(emptyList(), listOf(publicHoliday), targets))
    }

    @Test
    fun `an entry on a shared issue under an unmapped activity still counts for the only absence of the day on it`() {
        val paidLeaveActivity = paidLeave.copy(activityId = 10, activityName = "Congés")
        val item = planAbsenceImport(
            absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true)),
            listOf(entry(100, 3.75f, activityId = 12)),
            mapOf("CP" to paidLeaveActivity, "RTT" to paidLeave),
        ).single()

        assertEquals(AbsenceImportStatus.AlreadyEntered, item.status)
    }

    @Test
    fun `the day is capped at the configured daily hours`() {
        val days = absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true))
        val logged = listOf(entry(500, 4.25f))

        assertEquals(AbsenceImportStatus.ToAdd, planAbsenceImport(days, logged, targets, dailyHours = 8f).single().status)
        assertTrue(planAbsenceImport(days, logged, targets).single().status is AbsenceImportStatus.Overbooked)
    }

    @Test
    fun `a cancelled entry on a shared issue under an unmapped activity is attributed by its comment`() {
        val paidLeaveActivity = paidLeave.copy(activityId = 10, activityName = "Congés")
        val existing = imported("RTT", 100, 7.5f, activityId = 11)
        val item = planAbsenceImport(
            emptyList(),
            listOf(existing),
            mapOf("CP" to paidLeaveActivity, "RTT" to paidLeave),
        ).single()

        assertEquals(AbsenceImportStatus.Cancelled(existing), item.status)
        assertEquals(paidLeave, item.target)
    }

    @Test
    fun `an absence overflowing the day tells how many hours Redmine will keep`() {
        val overbooked = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)), listOf(entry(500, 5f)))
        val replaced = planSingle(
            absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true)),
            listOf(entry(100, 3.75f), entry(500, 4f)),
        )
        val fitting = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.MORNING, true)), listOf(entry(500, 3.75f)))

        assertEquals(2.5f, overbooked.keptHours)
        assertEquals(3.5f, replaced.keptHours)
        assertEquals(3.75f, fitting.keptHours)
    }

    @Test
    fun `unmapped type waits for a target`() {
        val item = planAbsenceImport(absence(AbsenceSlot("Maladie", AbsencePeriod.FULL_DAY, true)), emptyList(), targets)
            .single()

        assertEquals(AbsenceImportStatus.Unmapped, item.status)
        assertFalse(item.isSelectable)
    }

    @Test
    fun `pending absences are never selected by default`() {
        val item = planSingle(absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, isConfirmed = false)))

        assertEquals(AbsenceImportStatus.ToAdd, item.status)
        assertFalse(item.isSelectedByDefault())
    }

    @Test
    fun `reconciling keeps the user's choice on unchanged items and defaults the others`() {
        val days = listOf(
            AbsenceDay(day, listOf(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true))),
            AbsenceDay(day.plus(1, DateTimeUnit.DAY), listOf(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true))),
            AbsenceDay(day.plus(2, DateTimeUnit.DAY), listOf(AbsenceSlot("Maladie", AbsencePeriod.FULL_DAY, true))),
        )
        val before = planAbsenceImport(days, emptyList(), targets)
        val unchecked = before[0].key
        val selection = before.filter { it.isSelectedByDefault() }.map { it.key }.toSet() - unchecked

        val after = planAbsenceImport(days, emptyList(), targets + ("Maladie" to rtt))
        val reconciled = reconcileSelection(after, before.associate { it.key to it.status }, selection)

        assertEquals(setOf(after[1].key, after[2].key), reconciled)
    }

    @Test
    fun `a selected item whose status changed falls back to its default`() {
        val days = absence(AbsenceSlot("CP", AbsencePeriod.FULL_DAY, true))
        val before = planAbsenceImport(days, emptyList(), targets)
        val after = planAbsenceImport(days, listOf(entry(500, 4f)), targets)

        val reconciled = reconcileSelection(after, before.associate { it.key to it.status }, setOf(before.single().key))

        assertTrue(reconciled.isEmpty())
    }
}
