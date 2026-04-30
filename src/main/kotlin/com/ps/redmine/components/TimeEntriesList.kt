package com.ps.redmine.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.DateFormatter
import com.ps.redmine.util.WorkHours
import kotlinx.datetime.LocalDate
import java.util.Locale

@Composable
fun TimeEntriesList(
    timeEntries: List<TimeEntry>,
    selectedTimeEntry: TimeEntry?,
    onTimeEntrySelected: (TimeEntry) -> Unit,
    onDelete: (TimeEntry) -> Unit,
    deletingEntryId: Int? = null,
    locale: Locale = Locale.getDefault()
) {
    var pendingDelete by remember { mutableStateOf<TimeEntry?>(null) }

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

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
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
                        onClick = { onTimeEntrySelected(entry) },
                        onDelete = { pendingDelete = entry },
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
                style = MaterialTheme.typography.titleMedium,
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
                    style = MaterialTheme.typography.titleMedium,
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
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
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    Text(
                        text = timeEntry.project.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = timeEntry.activity.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = Strings["issue_item_format"].format(timeEntry.issue.id, timeEntry.issue.subject),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
