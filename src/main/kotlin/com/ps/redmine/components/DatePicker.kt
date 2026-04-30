package com.ps.redmine.components

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import java.util.*

@Composable
fun DatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault()
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(YearMonth(selectedDate.year, selectedDate.month)) }
    val currentToday by remember { mutableStateOf(today) }

    Column(modifier = modifier.heightIn(min = 56.dp)) {
        OutlinedTextField(
            value = DateFormatter.formatShort(selectedDate, locale),
            onValueChange = {},
            label = { Text(Strings["date_label"]) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = Strings["date_label"],
                    )
                }
            }
        )

        // Previous/next business day buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-1/+1 " + Strings["day_label"],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            OutlinedButton(
                onClick = { onDateSelected(selectedDate.previousBusinessDay()) },
                modifier = Modifier.height(28.dp).width(40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = Strings["previous_day"],
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedButton(
                onClick = { onDateSelected(selectedDate.nextBusinessDay()) },
                modifier = Modifier.height(28.dp).width(40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = Strings["next_day"],
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                            modifier = Modifier.onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                    currentYearMonth = currentYearMonth.minusMonths(1)
                                    true
                                } else false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = Strings["nav_previous"],
                            )
                        }
                        Text(
                            text = "${
                                LocaleNames.monthName(
                                    currentYearMonth.monthValue,
                                    locale,
                                    full = true
                                )
                            } ${currentYearMonth.year}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(
                            onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                            modifier = Modifier.onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                                    currentYearMonth = currentYearMonth.plusMonths(1)
                                    true
                                } else false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = Strings["nav_next"],
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.heightIn(min = 200.dp, max = 220.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in DayOfWeek.entries) {
                                Text(
                                    text = LocaleNames.weekdayName(dayOfWeek.isoDayNumber, locale, full = false),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        val firstDayOfMonth = currentYearMonth.atDay(1)
                        val daysInMonth = currentYearMonth.lengthOfMonth()
                        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber
                        val weeks = (daysInMonth + firstDayOfWeek - 1 + 6) / 7

                        for (week in 0 until weeks) {
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (dayOfWeek in 1..7) {
                                    val day = week * 7 + dayOfWeek - firstDayOfWeek + 1
                                    Box(modifier = Modifier.size(30.dp).padding(2.dp)) {
                                        if (day in 1..daysInMonth) {
                                            val kotlinDate = currentYearMonth.atDay(day)
                                            val isSelected = kotlinDate == selectedDate
                                            val isToday = kotlinDate == currentToday

                                            TooltipArea(
                                                tooltip = {
                                                    CalendarDayTooltip(
                                                        date = kotlinDate,
                                                        isSelected = isSelected,
                                                        isToday = isToday,
                                                        locale = locale,
                                                    )
                                                },
                                                modifier = Modifier.fillMaxSize(),
                                                delayMillis = 500
                                            ) {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable {
                                                            onDateSelected(kotlinDate)
                                                            showDialog = false
                                                        },
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.tertiary
                                                        else -> MaterialTheme.colorScheme.surface
                                                    },
                                                    shape = MaterialTheme.shapes.small,
                                                    tonalElevation = if (isSelected) 3.dp else 0.dp,
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Text(
                                                            text = day.toString(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = when {
                                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                                isToday -> MaterialTheme.colorScheme.onTertiary
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val todayDate = today
                            onDateSelected(todayDate)
                            currentYearMonth = YearMonth(todayDate.year, todayDate.month)
                            showDialog = false
                        }
                    ) {
                        Text(Strings["today"])
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(Strings["close"])
                    }
                }
            )
        }
    }
}

/** Tooltip card shown when hovering a calendar day cell. Names today and the selected date. */
@Composable
private fun CalendarDayTooltip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    locale: Locale,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = DateFormatter.formatFull(date, locale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isSelected) {
                Text(
                    text = Strings["calendar_tooltip_selected"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isToday) {
                Text(
                    text = Strings["calendar_tooltip_today"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
