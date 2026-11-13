package com.ps.redmine.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ps.redmine.lucca.*
import com.ps.redmine.resources.Strings
import com.ps.redmine.ui.*
import com.ps.redmine.util.LocaleNames
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import java.text.NumberFormat
import java.util.*

private val CheckColumn = 40.dp
private val DateColumn = 104.dp
private val StatusColumn = 168.dp
private val HoursColumn = 56.dp
private val RowPadding = PaddingValues(horizontal = 12.dp)

enum class Tone { Neutral, Primary, Pending, Warning, Error }

@Composable
fun Tone.containerColor(): Color = when (this) {
    Tone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHighest
    Tone.Primary -> MaterialTheme.colorScheme.primaryContainer
    Tone.Pending -> MaterialTheme.colorScheme.tertiaryContainer
    Tone.Warning -> if (LocalIsDarkTheme.current) WarningContainerDark else WarningContainerLight
    Tone.Error -> MaterialTheme.colorScheme.errorContainer
}

@Composable
fun Tone.contentColor(): Color = when (this) {
    Tone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    Tone.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
    Tone.Pending -> MaterialTheme.colorScheme.onTertiaryContainer
    Tone.Warning -> if (LocalIsDarkTheme.current) OnWarningContainerDark else OnWarningContainerLight
    Tone.Error -> MaterialTheme.colorScheme.onErrorContainer
}

fun formatHours(hours: Float, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(hours) + " h"

fun periodLabel(period: AbsencePeriod): String = when (period) {
    AbsencePeriod.FULL_DAY -> Strings["absence_full_day"]
    AbsencePeriod.MORNING -> Strings["absence_morning"]
    AbsencePeriod.AFTERNOON -> Strings["absence_afternoon"]
}

fun monthLabel(month: YearMonth, locale: Locale): String =
    "${LocaleNames.monthName(month.month.number, locale).replaceFirstChar { it.titlecase(locale) }} ${month.year}"

@Composable
fun Banner(icon: ImageVector, text: String, tone: Tone, details: List<String> = emptyList()) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tone.containerColor(),
        contentColor = tone.contentColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun TableCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
fun LeaveTypeTargets(
    leaveTypes: List<String>,
    targets: Map<String, AbsenceTarget>,
    onEdit: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(Strings["absence_types_title"])
        TableCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(RowPadding).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColumnHeader(Strings["absence_col_type"], Modifier.weight(0.3f))
                ColumnHeader(Strings["absence_col_issue"], Modifier.weight(0.45f))
                ColumnHeader(Strings["absence_col_activity"], Modifier.weight(0.25f))
                Spacer(Modifier.width(112.dp))
            }
            leaveTypes.forEach { type ->
                RowDivider()
                LeaveTypeRow(type, targets[type], onEdit = { onEdit(type) })
            }
        }
    }
}

@Composable
private fun LeaveTypeRow(type: String, target: AbsenceTarget?, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            type,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(0.3f).padding(end = 8.dp),
        )
        if (target == null) {
            Row(
                modifier = Modifier.weight(0.7f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.LinkOff,
                    contentDescription = null,
                    tint = Tone.Warning.contentColor(),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    Strings["absence_type_unmapped"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = Tone.Warning.contentColor(),
                )
            }
        } else {
            Column(modifier = Modifier.weight(0.45f).padding(end = 8.dp)) {
                Text(
                    "#${target.issueId} ${target.issueSubject}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    target.projectName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                target.activityName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.25f).padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(modifier = Modifier.width(112.dp), contentAlignment = Alignment.CenterEnd) {
            if (target == null) {
                OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Text(Strings["absence_type_map"])
                }
            } else {
                TextButton(onClick = onEdit) { Text(Strings["absence_type_edit"]) }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
}

@Composable
fun AbsenceMonthSection(
    month: YearMonth,
    items: List<AbsenceImportItem>,
    canImport: Boolean,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    locale: Locale,
) {
    TableCard {
        MonthHeader(month, items, canImport, selected, onSelectedChange, locale)
        if (items.isEmpty()) {
            RowDivider()
            Text(
                Strings["lucca_no_absences"],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
            return@TableCard
        }
        RowDivider()
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(RowPadding)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canImport) Spacer(Modifier.width(CheckColumn))
            ColumnHeader(Strings["absence_col_date"], Modifier.width(DateColumn))
            ColumnHeader(Strings["absence_col_absence"], Modifier.weight(1f))
            if (canImport) ColumnHeader(Strings["absence_col_status"], Modifier.width(StatusColumn))
            ColumnHeader(Strings["absence_col_hours"], Modifier.width(HoursColumn), TextAlign.End)
        }
        items.forEach { item ->
            RowDivider()
            AbsenceImportRow(
                item = item,
                canImport = canImport,
                checked = item.key in selected,
                onCheckedChange = { on -> onSelectedChange(if (on) selected + item.key else selected - item.key) },
                locale = locale,
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    items: List<AbsenceImportItem>,
    canImport: Boolean,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    locale: Locale,
) {
    val bulkKeys = items.filter { it.isSelectedByDefault() }.map { it.key }.toSet()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canImport) {
            val chosen = bulkKeys.count { it in selected }
            val state = when (chosen) {
                0 -> ToggleableState.Off
                bulkKeys.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
            val toggleAll = if (bulkKeys.isNotEmpty()) {
                Modifier.triStateToggleable(state = state, role = Role.Checkbox) {
                    onSelectedChange(if (chosen == bulkKeys.size) selected - bulkKeys else selected + bulkKeys)
                }
            } else {
                Modifier
            }
            // A null onClick keeps the box at its visual size, in line with the row checkboxes, instead of
            // centering it in a 48 dp touch target wider than the column.
            Box(toggleAll.width(CheckColumn), contentAlignment = Alignment.CenterStart) {
                if (bulkKeys.isNotEmpty()) TriStateCheckbox(state = state, onClick = null)
            }
        }
        Text(
            monthLabel(month, locale),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
        )
        if (canImport) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val toAdd = items.count { it.status == AbsenceImportStatus.ToAdd }
                val entered = items.count { it.status == AbsenceImportStatus.AlreadyEntered }
                val toReview = items.count {
                    it.status != AbsenceImportStatus.ToAdd && it.status != AbsenceImportStatus.AlreadyEntered
                }
                if (toAdd > 0) CountChip(Strings["absence_count_to_add"].format(toAdd), Tone.Primary)
                if (entered > 0) CountChip(Strings["absence_count_entered"].format(entered), Tone.Neutral)
                if (toReview > 0) CountChip(Strings["absence_count_to_review"].format(toReview), Tone.Warning)
            }
        }
    }
}

@Composable
private fun CountChip(text: String, tone: Tone) {
    Surface(shape = MaterialTheme.shapes.small, color = tone.containerColor(), contentColor = tone.contentColor()) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private data class StatusPresentation(
    val badge: String,
    val tone: Tone,
    val icon: ImageVector,
    val detail: String? = null,
)

private fun AbsenceImportItem.presentation(locale: Locale): StatusPresentation = when (val s = status) {
    AbsenceImportStatus.ToAdd ->
        if (slots.all { it.isConfirmed }) {
            StatusPresentation(Strings["absence_badge_to_add"], Tone.Primary, Icons.Outlined.AddCircleOutline)
        } else {
            StatusPresentation(
                Strings["absence_badge_pending"], Tone.Pending, Icons.Outlined.HourglassEmpty,
                Strings["absence_status_pending"],
            )
        }

    AbsenceImportStatus.AlreadyEntered ->
        StatusPresentation(Strings["absence_badge_entered"], Tone.Neutral, Icons.Filled.CheckCircle)

    is AbsenceImportStatus.Overbooked -> StatusPresentation(
        Strings["absence_badge_overbooked"], Tone.Warning, Icons.Outlined.WarningAmber,
        if (s.otherAbsenceHours > 0f) {
            Strings["absence_status_overbooked_with_absences"]
                .format(formatHours(s.otherHours, locale), formatHours(s.otherAbsenceHours, locale))
        } else {
            Strings["absence_status_overbooked"].format(formatHours(s.otherHours, locale))
        },
    )

    is AbsenceImportStatus.DifferentHours -> {
        val detail = Strings["absence_status_different"]
            .format(formatHours(s.existing.hours, locale), formatHours(hours, locale))
        StatusPresentation(
            Strings["absence_badge_different"], Tone.Warning, Icons.Outlined.SwapHoriz,
            if (s.exceedsDay) {
                "$detail ${Strings["absence_status_exceeds_day"].format(formatHours(FULL_DAY_HOURS, locale))}"
            } else {
                detail
            },
        )
    }

    is AbsenceImportStatus.Ambiguous -> StatusPresentation(
        Strings["absence_badge_ambiguous"], Tone.Error, Icons.Outlined.ErrorOutline,
        Strings["absence_status_ambiguous"].format(formatHours(s.existingHours, locale)),
    )

    AbsenceImportStatus.Unmapped -> StatusPresentation(
        Strings["absence_badge_unmapped"], Tone.Warning, Icons.Outlined.LinkOff,
        Strings["absence_status_unmapped"].format(slots.first().accountName),
    )
}

@Composable
private fun AbsenceImportRow(
    item: AbsenceImportItem,
    canImport: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    locale: Locale,
) {
    val selectable = canImport && item.isSelectable
    val presentation = item.presentation(locale)
    val dimmed = canImport && item.status == AbsenceImportStatus.AlreadyEntered
    val textColor = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    val rowModifier = if (selectable) {
        Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
    } else {
        Modifier
    }
    Row(
        modifier = rowModifier.fillMaxWidth().heightIn(min = 52.dp).padding(RowPadding).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canImport) {
            Box(Modifier.width(CheckColumn), contentAlignment = Alignment.CenterStart) {
                if (selectable) Checkbox(checked = checked, onCheckedChange = null)
            }
        }
        Text(
            shortDate(item.date, locale),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier = Modifier.width(DateColumn),
        )
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.slots.joinToString(" + ") { "${it.accountName} · ${periodLabel(it.period)}" },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (canImport) {
                presentation.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (canImport) {
            Box(Modifier.width(StatusColumn), contentAlignment = Alignment.CenterStart) {
                StatusBadge(presentation)
            }
        }
        Text(
            formatHours(item.hours, locale),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(HoursColumn),
        )
    }
}

@Composable
private fun StatusBadge(presentation: StatusPresentation) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = presentation.tone.containerColor(),
        contentColor = presentation.tone.contentColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(presentation.icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(presentation.badge, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

private fun shortDate(date: LocalDate, locale: Locale): String =
    "${LocaleNames.weekdayName(date.dayOfWeek.isoDayNumber, locale, full = false)} ${date.day} " +
            LocaleNames.monthName(date.month.number, locale, full = false)
