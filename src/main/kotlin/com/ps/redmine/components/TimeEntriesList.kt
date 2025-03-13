package com.ps.redmine.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.DateFormatter

@Composable
fun TimeEntriesList(
    timeEntries: List<TimeEntry>,
    selectedTimeEntry: TimeEntry?,
    onTimeEntrySelected: (TimeEntry) -> Unit,
    onDelete: (TimeEntry) -> Unit,
    deletingEntryId: Int? = null
) {
    val groupedEntries = remember(timeEntries) {
        timeEntries
            .sortedByDescending { it.date }
            .groupBy { it.date }
    }

    LazyColumn {
        groupedEntries.forEach { (date, entries) ->
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateFormatter.formatFull(date),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = Strings["hours_format"].format(entries.sumOf { it.hours.toDouble() }),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    entries.forEach { entry ->
                        TimeEntryItem(
                            timeEntry = entry,
                            isSelected = entry == selectedTimeEntry,
                            onClick = { onTimeEntrySelected(entry) },
                            onDelete = { onDelete(entry) },
                            isLoading = entry.id == deletingEntryId
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeEntryItem(
    timeEntry: TimeEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = !isLoading) { onClick() }
            .alpha(if (isLoading) 0.6f else 1f),
        elevation = if (isSelected) 4.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormatter.formatShort(timeEntry.date),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = Strings["hours_format"].format(timeEntry.hours),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                }
                Column {
                    Text(
                        text = timeEntry.project.name,
                        style = MaterialTheme.typography.body1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeEntry.activity.name,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = Strings["issue_item_format"].format(timeEntry.issue.id, timeEntry.issue.subject),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary
                        )
                        if (timeEntry.comments.isNotEmpty()) {
                            Text(
                                text = Strings["comment_item_format"].format(timeEntry.comments),
                                style = MaterialTheme.typography.caption,
                                maxLines = 1,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = { if (!isLoading) onDelete() },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .alpha(if (isLoading) 0.6f else 1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colors.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = Strings["delete_time_entry"],
                        tint = MaterialTheme.colors.error
                    )
                }
            }
        }
    }
}
