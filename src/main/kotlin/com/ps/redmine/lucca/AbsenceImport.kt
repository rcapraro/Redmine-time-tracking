package com.ps.redmine.lucca

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.util.WorkHours
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.math.abs

const val HALF_DAY_HOURS = 3.75f
const val FULL_DAY_HOURS = 2 * HALF_DAY_HOURS

val AbsencePeriod.hours: Float
    get() = when (this) {
        AbsencePeriod.FULL_DAY -> FULL_DAY_HOURS
        AbsencePeriod.MORNING, AbsencePeriod.AFTERNOON -> HALF_DAY_HOURS
    }

/** The Redmine project, issue and activity that absences of one Lucca leave type are logged against. */
@Serializable
data class AbsenceTarget(
    val projectId: Int,
    val projectName: String,
    val issueId: Int,
    val issueSubject: String,
    val activityId: Int,
    val activityName: String,
) {
    val project get() = Project(projectId, projectName)
    val issue get() = Issue(issueId, issueSubject)
    val activity get() = Activity(activityId, activityName)
}

sealed interface AbsenceImportStatus {
    data object ToAdd : AbsenceImportStatus
    data object AlreadyEntered : AbsenceImportStatus

    /**
     * Adding the absence could push the day past a full day: [otherHours] are already logged on other issues and
     * [otherAbsenceHours] belong to other absences of that day not imported yet.
     */
    data class Overbooked(val otherHours: Float, val otherAbsenceHours: Float) : AbsenceImportStatus

    /**
     * The issue already has one entry that day, with other hours; importing replaces its hours.
     * [exceedsDay] tells whether the day would then go past a full day.
     */
    data class DifferentHours(val existing: TimeEntry, val exceedsDay: Boolean) : AbsenceImportStatus

    /** Several entries already exist on the issue that day; only a person can tell which one is right. */
    data class Ambiguous(val existingHours: Float) : AbsenceImportStatus

    data object Unmapped : AbsenceImportStatus
}

/** One Redmine entry to create (or fix) for one day and one target issue. */
data class AbsenceImportItem(
    val date: LocalDate,
    val slots: List<AbsenceSlot>,
    val target: AbsenceTarget?,
    val hours: Float,
    val status: AbsenceImportStatus,
) {
    val key: String = "$date|" +
            (target?.let { "issue:${it.issueId}/activity:${it.activityId}" } ?: "type:${slots.first().accountName}")

    val isSelectable: Boolean
        get() = status is AbsenceImportStatus.ToAdd ||
                status is AbsenceImportStatus.Overbooked ||
                status is AbsenceImportStatus.DifferentHours
}

/**
 * Compares the Lucca absences with the Redmine entries already logged and decides, per day and target issue,
 * what importing would do. Nothing here writes: existing entries are only ever changed by an explicit choice.
 */
fun planAbsenceImport(
    days: List<AbsenceDay>,
    entries: List<TimeEntry>,
    targets: Map<String, AbsenceTarget>,
): List<AbsenceImportItem> = days.flatMap { day ->
    val dayEntries = entries.filter { it.date == day.date }
    val (mapped, unmapped) = day.slots.partition { it.accountName in targets }
    // Grouped by ids only: two leave types mapped to the same issue and activity at different times may carry
    // different stored names, yet they must become one entry.
    val groups = mapped.groupBy { targets.getValue(it.accountName).let { t -> t.issueId to t.activityId } }
        .values.associateBy { slots -> targets.getValue(slots.first().accountName) }
    // Telling two targets on the same issue apart takes the activity; otherwise the issue alone identifies an
    // absence, whatever activity it was logged with.
    val sharedIssues = groups.keys.groupBy { it.issueId }.filterValues { it.size > 1 }.keys
    fun AbsenceTarget.owns(entry: TimeEntry) =
        entry.issue.id == issueId && (issueId !in sharedIssues || entry.activity.id == activityId)

    val hoursByTarget = groups.mapValues { (_, slots) -> slots.sumOf { it.period.hours.toDouble() }.toFloat() }
    val pendingHoursByTarget = hoursByTarget.filterKeys { target -> dayEntries.none { target.owns(it) } }

    val mappedItems = groups.map { (target, slots) ->
        val hours = hoursByTarget.getValue(target)
        val owned = dayEntries.filter { target.owns(it) }
        val ownedHours = owned.sumOf { it.hours.toDouble() }.toFloat()
        val otherHours = dayEntries.filterNot { target.owns(it) }.sumOf { it.hours.toDouble() }.toFloat()
        val otherPending = pendingHoursByTarget.filterKeys { it != target }.values.sum()
        val exceedsDay = otherHours + otherPending + hours > FULL_DAY_HOURS + WorkHours.HOURS_TOLERANCE
        val status = when {
            owned.isNotEmpty() && sameHours(ownedHours, hours) -> AbsenceImportStatus.AlreadyEntered
            owned.size == 1 -> AbsenceImportStatus.DifferentHours(owned.single(), exceedsDay)
            owned.size > 1 -> AbsenceImportStatus.Ambiguous(ownedHours)
            exceedsDay -> AbsenceImportStatus.Overbooked(otherHours, otherPending)
            else -> AbsenceImportStatus.ToAdd
        }
        AbsenceImportItem(day.date, slots, target, hours, status)
    }
    val unmappedItems = unmapped.map { slot ->
        AbsenceImportItem(day.date, listOf(slot), null, slot.period.hours, AbsenceImportStatus.Unmapped)
    }
    mappedItems + unmappedItems
}

/** Absences waiting for approval in Lucca may still be refused, so they are never pre-selected. */
fun AbsenceImportItem.isSelectedByDefault(): Boolean =
    status == AbsenceImportStatus.ToAdd && slots.all { it.isConfirmed }

/**
 * Carries the user's choices over a new plan: an item whose status did not change keeps its checkbox, while a new
 * item or one whose status changed falls back to its default, so a choice never silently applies to a new situation.
 */
fun reconcileSelection(
    items: List<AbsenceImportItem>,
    previousStatuses: Map<String, AbsenceImportStatus>,
    previousSelection: Set<String>,
): Set<String> = items
    .filter { item ->
        if (previousStatuses[item.key] == item.status) item.key in previousSelection else item.isSelectedByDefault()
    }
    .map { it.key }
    .toSet()

/**
 * Days that would go past a full day if exactly [selected] were imported, counting what is already logged, the
 * hours each selected replacement removes and the hours each selected item adds.
 */
fun daysExceedingFullDay(selected: List<AbsenceImportItem>, entries: List<TimeEntry>): Set<LocalDate> =
    selected.groupBy { it.date }.filter { (date, items) ->
        val logged = entries.filter { it.date == date }.sumOf { it.hours.toDouble() }
        val replaced = items.sumOf { (it.status as? AbsenceImportStatus.DifferentHours)?.existing?.hours?.toDouble() ?: 0.0 }
        val added = items.sumOf { it.hours.toDouble() }
        logged - replaced + added > FULL_DAY_HOURS + WorkHours.HOURS_TOLERANCE
    }.keys

private fun sameHours(a: Float, b: Float) = abs(a - b) <= WorkHours.HOURS_TOLERANCE
