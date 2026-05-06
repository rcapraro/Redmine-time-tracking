package com.ps.redmine.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.DateFormatter
import com.ps.redmine.util.WorkHours
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import java.util.*

sealed interface DuplicateTarget {
    data object SameDay : DuplicateTarget
    data object NextDay : DuplicateTarget
    data class Range(val from: LocalDate, val to: LocalDate) : DuplicateTarget
}

@Composable
fun TimeEntriesList(
    timeEntries: List<TimeEntry>,
    selectedTimeEntry: TimeEntry?,
    onTimeEntrySelected: (TimeEntry) -> Unit,
    onDelete: (TimeEntry) -> Unit,
    onDuplicate: (TimeEntry, DuplicateTarget) -> Unit,
    selectedEntryIds: Set<Int> = emptySet(),
    onToggleSelect: (TimeEntry) -> Unit = {},
    deletingEntryId: Int? = null,
    locale: Locale = Locale.getDefault(),
    selectedWeekStart: LocalDate? = null,
) {
    var pendingDelete by remember { mutableStateOf<TimeEntry?>(null) }
    var rangeDuplicateEntry by remember { mutableStateOf<TimeEntry?>(null) }

    val entriesByDate = remember(timeEntries) {
        timeEntries.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }

    val dailyTotals = remember(entriesByDate) {
        entriesByDate.mapValues { (_, entries) ->
            entries.sumOf { it.hours.toDouble() }.toFloat()
        }
    }

    if (timeEntries.isEmpty()) {
        EmptyEntriesPlaceholder(modifier = Modifier.fillMaxSize())
        return
    }

    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        val listState = rememberLazyListState()

        LaunchedEffect(selectedWeekStart, entriesByDate) {
            if (selectedWeekStart == null) return@LaunchedEffect
            val weekEnd = selectedWeekStart.plus(6, DateTimeUnit.DAY)
            var index = 0
            for ((date, entries) in entriesByDate) {
                if (date in selectedWeekStart..weekEnd) {
                    listState.animateScrollToItem(index)
                    break
                }
                index += 1 + entries.size
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            entriesByDate.forEach { (date, entries) ->
                item(key = "header-$date") {
                    DateHeader(
                        date = date,
                        totalHours = dailyTotals[date] ?: 0f,
                        locale = locale,
                        modifier = Modifier.animateItem()
                    )
                }

                items(entries, key = { entry -> entry.id ?: entry.hashCode() }) { entry ->
                    TimeEntryItem(
                        timeEntry = entry,
                        isSelected = entry == selectedTimeEntry,
                        isChecked = entry.id != null && entry.id in selectedEntryIds,
                        onCheckedChange = { onToggleSelect(entry) },
                        onClick = { onTimeEntrySelected(entry) },
                        onDelete = { pendingDelete = entry },
                        onDuplicateSameDay = { onDuplicate(entry, DuplicateTarget.SameDay) },
                        onDuplicateNextDay = { onDuplicate(entry, DuplicateTarget.NextDay) },
                        onDuplicateRange = { rangeDuplicateEntry = entry },
                        isLoading = entry.id == deletingEntryId,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(listState)
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = Strings["confirm_delete_title"],
            message = Strings["confirm_delete_message"],
            confirmLabel = Strings["confirm_delete_yes"],
            dismissLabel = Strings["confirm_delete_no"],
            destructive = true,
            onConfirm = {
                pendingDelete = null
                onDelete(entry)
            },
            onDismiss = { pendingDelete = null },
        )
    }

    rangeDuplicateEntry?.let { entry ->
        DuplicateRangeDialog(
            entry = entry,
            locale = locale,
            onConfirm = { from, to ->
                rangeDuplicateEntry = null
                onDuplicate(entry, DuplicateTarget.Range(from, to))
            },
            onDismiss = { rangeDuplicateEntry = null },
        )
    }
}

@Composable
private fun DuplicateRangeDialog(
    entry: TimeEntry,
    locale: Locale,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember(entry) {
        var d = entry.date
        repeat(1) { d = d.nextNonWeekend() }
        d
    }
    var fromDate by remember(entry) { mutableStateOf(initial) }
    var toDate by remember(entry) { mutableStateOf(initial) }

    AlertDialog(
        modifier = Modifier.width(560.dp),
        onDismissRequest = onDismiss,
        title = { Text(Strings["duplicate_range_title"]) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Strings["duplicate_range_from"],
                        style = MaterialTheme.typography.labelMedium,
                    )
                    DatePicker(
                        selectedDate = fromDate,
                        onDateSelected = { fromDate = it },
                        modifier = Modifier.width(200.dp),
                        locale = locale,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(24.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Strings["duplicate_range_to"],
                        style = MaterialTheme.typography.labelMedium,
                    )
                    DatePicker(
                        selectedDate = toDate,
                        onDateSelected = { toDate = it },
                        modifier = Modifier.width(200.dp),
                        locale = locale,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val (a, b) = if (fromDate <= toDate) fromDate to toDate else toDate to fromDate
                    onConfirm(a, b)
                },
                enabled = true,
            ) {
                Text(Strings["duplicate_range_confirm"])
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["confirm_delete_no"])
            }
        },
    )
}

private fun LocalDate.nextNonWeekend(): LocalDate {
    var d = this.plus(1, DateTimeUnit.DAY)
    while (d.dayOfWeek.isoDayNumber !in 1..5) {
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return d
}

@Composable
private fun EmptyEntriesPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = Strings["empty_entries_title"],
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = Strings["empty_entries_subtitle"],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DateHeader(
    date: LocalDate,
    totalHours: Float,
    locale: Locale = Locale.getDefault(),
    modifier: Modifier = Modifier,
) {
    val targetDaily = WorkHours.configuredDailyHours()
    val missingHours = if (totalHours < targetDaily) targetDaily - totalHours else 0f
    val excessHours = if (totalHours > targetDaily) totalHours - targetDaily else 0f
    val isPerfectHours = totalHours == targetDaily

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateFormatter.formatShort(date, locale),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = Strings["hours_format"].format(totalHours),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                isPerfectHours -> StatusRow(
                    icon = Icons.Outlined.CheckCircle,
                    text = Strings["perfect_hours"].format(targetDaily),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                missingHours > 0 -> StatusRow(
                    icon = Icons.Outlined.WarningAmber,
                    text = Strings["missing_hours"].format(missingHours, targetDaily),
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                )

                excessHours > 0 -> StatusRow(
                    icon = Icons.Outlined.WarningAmber,
                    text = Strings["excess_hours"].format(excessHours, targetDaily),
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun EllipsizedHoverText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
) {
    TooltipArea(
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        },
        delayMillis = 500,
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    container: Color,
    onContainer: Color,
) {
    Surface(
        color = container,
        contentColor = onContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = onContainer,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun TimeEntryItem(
    timeEntry: TimeEntry,
    isSelected: Boolean,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicateSameDay: () -> Unit,
    onDuplicateNextDay: () -> Unit,
    onDuplicateRange: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var duplicateMenuExpanded by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(200),
        label = "itemContainer",
    )
    val tonalElevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(200),
        label = "itemElevation",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp)
            .clickable(enabled = !isLoading) { onClick() }
            .alpha(if (isLoading) 0.6f else 1f),
        color = containerColor,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        tonalElevation = tonalElevation,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 4.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { if (!isLoading) onCheckedChange() },
                enabled = !isLoading && timeEntry.id != null,
                modifier = Modifier.size(36.dp),
            )

            Column(
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = Strings["hours_format"].format(timeEntry.hours),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = timeEntry.activity.name,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                EllipsizedHoverText(
                    text = timeEntry.project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                EllipsizedHoverText(
                    text = Strings["issue_item_format"].format(timeEntry.issue.id, timeEntry.issue.subject),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(
                    onClick = { if (!isLoading) duplicateMenuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(start = 4.dp)
                        .alpha(if (isLoading) 0.6f else 1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = Strings["duplicate_time_entry"],
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = duplicateMenuExpanded,
                    onDismissRequest = { duplicateMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings["duplicate_same_day"]) },
                        onClick = {
                            duplicateMenuExpanded = false
                            onDuplicateSameDay()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(Strings["duplicate_next_day"]) },
                        onClick = {
                            duplicateMenuExpanded = false
                            onDuplicateNextDay()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(Strings["duplicate_range"]) },
                        onClick = {
                            duplicateMenuExpanded = false
                            onDuplicateRange()
                        },
                    )
                }
            }

            IconButton(
                onClick = { if (!isLoading) onDelete() },
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 4.dp)
                    .alpha(if (isLoading) 0.6f else 1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = Strings["delete_time_entry"],
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
