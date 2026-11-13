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

/** Separates the leave type from the period in the comment of an imported absence: "RTT — Morning". */
const val ABSENCE_COMMENT_SEPARATOR = " — "

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
     * Adding the absence could push the day past its hours: [otherHours] are already logged on other issues, of
     * which [cancelledHours] belong to cancelled absences offered for deletion, and [otherAbsenceHours] belong to
     * other absences of that day not imported yet.
     */
    data class Overbooked(
        val otherHours: Float,
        val otherAbsenceHours: Float,
        val cancelledHours: Float = 0f,
    ) : AbsenceImportStatus

    /**
     * The issue already has one entry that day, with other hours; importing replaces its hours.
     * [exceedsDay] tells whether the day would then go past its hours.
     */
    data class DifferentHours(val existing: TimeEntry, val exceedsDay: Boolean) : AbsenceImportStatus

    /** Several entries already exist on the issue that day; only a person can tell which one is right. */
    data class Ambiguous(val existingHours: Float) : AbsenceImportStatus

    /**
     * The entry is on an absence issue but Lucca no longer has that absence (cancelled or refused); importing
     * deletes it.
     */
    data class Cancelled(val existing: TimeEntry) : AbsenceImportStatus

    data object Unmapped : AbsenceImportStatus
}

/** One Redmine entry to create (or fix) for one day and one target issue. */
data class AbsenceImportItem(
    val date: LocalDate,
    val slots: List<AbsenceSlot>,
    val target: AbsenceTarget?,
    val hours: Float,
    val status: AbsenceImportStatus,
    val keptHours: Float = hours,
) {
    val key: String = "$date|" + when {
        status is AbsenceImportStatus.Cancelled -> "entry:${status.existing.id}"
        target != null -> "issue:${target.issueId}/activity:${target.activityId}"
        else -> "type:${slots.first().accountName}"
    }

    val isDeletion: Boolean get() = status is AbsenceImportStatus.Cancelled

    val isSelectable: Boolean
        get() = status is AbsenceImportStatus.ToAdd ||
                status is AbsenceImportStatus.Overbooked ||
                status is AbsenceImportStatus.DifferentHours ||
                status is AbsenceImportStatus.Cancelled
}

/**
 * Compares the Lucca absences with the Redmine entries already logged and decides, per day and target issue,
 * what importing would do, given that Redmine keeps no more than [dailyHours] a day. [entries] must cover the same
 * period as [days]: an imported absence entry with no matching Lucca absence is taken as cancelled. Nothing here
 * writes: existing entries are only ever changed by an explicit choice.
 */
fun planAbsenceImport(
    days: List<AbsenceDay>,
    entries: List<TimeEntry>,
    targets: Map<String, AbsenceTarget>,
    dailyHours: Float = FULL_DAY_HOURS,
): List<AbsenceImportItem> {
    val absenceIssues = AbsenceIssues(targets)
    val slotsByDate = days.associate { it.date to it.slots }
    val dates = (slotsByDate.keys + entries.filter { absenceIssues.importedTarget(it) != null }.map { it.date }).sorted()
    return dates.flatMap { date ->
        planDay(date, slotsByDate[date].orEmpty(), entries.filter { it.date == date }, targets, absenceIssues, dailyHours)
    }
}

private class AbsenceIssues(private val targets: Map<String, AbsenceTarget>) {
    private val activitiesByIssue = targets.values.distinctBy { it.issueId to it.activityId }
        .groupBy({ it.issueId }, { it.activityId })

    // Telling two targets on the same issue apart takes the activity; otherwise the issue alone identifies an
    // absence, whatever activity it was logged with.
    fun owns(target: AbsenceTarget, entry: TimeEntry) =
        entry.issue.id == target.issueId &&
                (activitiesByIssue[target.issueId].orEmpty().size <= 1 || entry.activity.id == target.activityId)

    fun hasUnmappedActivity(entry: TimeEntry) =
        activitiesByIssue[entry.issue.id].let { it != null && it.size > 1 && entry.activity.id !in it }

    // Only an entry this import created can stand for a Lucca absence: a public holiday or hours logged by hand on
    // the same issue have no Lucca counterpart and must never be offered for deletion.
    fun importedTarget(entry: TimeEntry): AbsenceTarget? = targets.entries.firstOrNull { (type, target) ->
        target.issueId == entry.issue.id && entry.comments.orEmpty().startsWith(type + ABSENCE_COMMENT_SEPARATOR)
    }?.value
}

private fun planDay(
    date: LocalDate,
    daySlots: List<AbsenceSlot>,
    dayEntries: List<TimeEntry>,
    targets: Map<String, AbsenceTarget>,
    absenceIssues: AbsenceIssues,
    dailyHours: Float,
): List<AbsenceImportItem> {
    val (mapped, unmapped) = daySlots.partition { it.accountName in targets }
    // Grouped by ids only: two leave types mapped to the same issue and activity at different times may carry
    // different stored names, yet they must become one entry.
    val groups = mapped.groupBy { targets.getValue(it.accountName).let { t -> t.issueId to t.activityId } }
        .values.associateBy { slots -> targets.getValue(slots.first().accountName) }
    // An entry on a shared issue under none of its mapped activities can still be told apart when a single
    // absence of the day uses that issue.
    fun AbsenceTarget.owns(entry: TimeEntry) = absenceIssues.owns(this, entry) ||
            (entry.issue.id == issueId && absenceIssues.hasUnmappedActivity(entry) &&
                    groups.keys.count { it.issueId == issueId } == 1)

    // An unmapped leave may be the very absence an entry stands for (a leave account renamed in Lucca, say), so
    // nothing on that day is taken as cancelled.
    val cancelled = if (unmapped.isNotEmpty()) emptyList() else dayEntries.filter { entry ->
        absenceIssues.importedTarget(entry) != null && groups.keys.none { it.owns(entry) }
    }
    val cancelledHours = cancelled.sumOf { it.hours.toDouble() }.toFloat()
    val hoursByTarget = groups.mapValues { (_, slots) -> slots.sumOf { it.period.hours.toDouble() }.toFloat() }
    val pendingHoursByTarget = hoursByTarget.filterKeys { target -> dayEntries.none { target.owns(it) } }

    val mappedItems = groups.map { (target, slots) ->
        val hours = hoursByTarget.getValue(target)
        val owned = dayEntries.filter { target.owns(it) }
        val ownedHours = owned.sumOf { it.hours.toDouble() }.toFloat()
        val otherHours = dayEntries.filterNot { target.owns(it) }.sumOf { it.hours.toDouble() }.toFloat()
        val otherPending = pendingHoursByTarget.filterKeys { it != target }.values.sum()
        val exceedsDay = otherHours + otherPending + hours > dailyHours + WorkHours.HOURS_TOLERANCE
        val status = when {
            owned.isNotEmpty() && sameHours(ownedHours, hours) -> AbsenceImportStatus.AlreadyEntered
            owned.size == 1 -> AbsenceImportStatus.DifferentHours(owned.single(), exceedsDay)
            owned.size > 1 -> AbsenceImportStatus.Ambiguous(ownedHours)
            exceedsDay -> AbsenceImportStatus.Overbooked(otherHours, otherPending, cancelledHours)
            else -> AbsenceImportStatus.ToAdd
        }
        AbsenceImportItem(date, slots, target, hours, status, WorkHours.hoursKept(hours, otherHours, dailyHours))
    }
    val unmappedItems = unmapped.map { slot ->
        AbsenceImportItem(date, listOf(slot), null, slot.period.hours, AbsenceImportStatus.Unmapped)
    }
    val cancelledItems = cancelled.map { entry ->
        AbsenceImportItem(date, emptyList(), absenceIssues.importedTarget(entry), entry.hours, AbsenceImportStatus.Cancelled(entry))
    }
    return mappedItems + unmappedItems + cancelledItems
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
 * Days that would go past [dailyHours] if exactly [selected] were imported, counting what is already logged, the
 * hours each selected replacement or deletion removes and the hours each selected item adds.
 */
fun daysExceedingFullDay(
    selected: List<AbsenceImportItem>,
    entries: List<TimeEntry>,
    dailyHours: Float = FULL_DAY_HOURS,
): Set<LocalDate> =
    selected.groupBy { it.date }.filter { (date, items) ->
        val logged = entries.filter { it.date == date }.sumOf { it.hours.toDouble() }
        val removed = items.sumOf { item ->
            when (val status = item.status) {
                is AbsenceImportStatus.DifferentHours -> status.existing.hours.toDouble()
                is AbsenceImportStatus.Cancelled -> status.existing.hours.toDouble()
                else -> 0.0
            }
        }
        val added = items.filterNot { it.isDeletion }.sumOf { it.hours.toDouble() }
        logged - removed + added > dailyHours + WorkHours.HOURS_TOLERANCE
    }.keys

private fun sameHours(a: Float, b: Float) = abs(a - b) <= WorkHours.HOURS_TOLERANCE
