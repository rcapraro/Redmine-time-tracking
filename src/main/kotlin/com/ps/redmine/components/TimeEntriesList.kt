package com.ps.redmine.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import com.ps.redmine.util.ElevationTokens
import com.ps.redmine.util.WorkHours
import kotlinx.datetime.LocalDate
import java.util.*

@Composable
fun TimeEntriesList(
    timeEntries: List<TimeEntry>,
    selectedTimeEntry: TimeEntry?,
    onTimeEntrySelected: (TimeEntry) -> Unit,
    onDelete: (TimeEntry) -> Unit,
    deletingEntryId: Int? = null,
    locale: Locale = Locale.getDefault()
) {
    // Group entries by date and sort dates in descending order
    val entriesByDate = remember(timeEntries) {
        timeEntries.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }

    // Calculate total hours per day and check if < 7.5
    val dailyTotals = remember(entriesByDate) {
        entriesByDate.mapValues { (_, entries) ->
            entries.sumOf { it.hours.toDouble() }.toFloat()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()  // Allow the box to fill the available height
    ) {
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            entriesByDate.forEach { (date, entries) ->
                // Display date header with total hours
                item {
                    DateHeader(
                        date = date,
                        totalHours = dailyTotals[date] ?: 0f,
                        locale = locale
                    )
                }

                // Display entries for this date
                items(entries.size) { index ->
                    val entry = entries[index]
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

        // Add a visible scrollbar
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(listState)
        )
    }
}

@Composable
fun DateHeader(
    date: LocalDate,
    totalHours: Float,
    locale: Locale = Locale.getDefault()
) {
    val missingHours =
        if (totalHours < WorkHours.DAILY_STANDARD_HOURS) WorkHours.DAILY_STANDARD_HOURS - totalHours else 0f
    val excessHours =
        if (totalHours > WorkHours.DAILY_STANDARD_HOURS) totalHours - WorkHours.DAILY_STANDARD_HOURS else 0f
    val isPerfectHours = totalHours == WorkHours.DAILY_STANDARD_HOURS

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = ElevationTokens.Medium,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Date and total hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateFormatter.formatShort(date, locale),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )

                Surface(
                    color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = Strings["hours_format"].format(totalHours),
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Display appropriate message based on hours
            when {
                isPerfectHours -> {
                    // Display checkbox for perfect hours (7.5)
                    Surface(
                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = Strings["perfect_hours"],
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                missingHours > 0 -> {
                    // Display warning for missing hours
                    Surface(
                        color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "⚠️ " + Strings["missing_hours"].format(missingHours),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                excessHours > 0 -> {
                    // Display warning for excess hours
                    Surface(
                        color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "⚠️ " + Strings["excess_hours"].format(excessHours),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(enabled = !isLoading) { onClick() }
            .alpha(if (isLoading) 0.6f else 1f),
        elevation = if (isSelected) ElevationTokens.High else ElevationTokens.Low,
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
            // Bullet point to indicate this entry is under a day
            Text(
                text = "•",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // First row: Hours, Project
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours with more prominence
                    Surface(
                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = Strings["hours_format"].format(timeEntry.hours),
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Project name
                    Text(
                        text = timeEntry.project.name,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Second row: Activity, Issue, Comments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Activity with more prominence
                    Surface(
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = timeEntry.activity.name,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Issue
                    Text(
                        text = Strings["issue_item_format"].format(timeEntry.issue.id, timeEntry.issue.subject),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.secondary
                    )

                }
            }

            // Delete button
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
