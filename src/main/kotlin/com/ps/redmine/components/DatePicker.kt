package com.ps.redmine.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.*
import kotlinx.datetime.LocalDate
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

@Composable
fun DatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault()
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(selectedDate.toJavaYearMonth()) }
    val currentToday by remember { mutableStateOf(today) }

    Column(modifier = modifier.heightIn(min = 56.dp)) {
        OutlinedTextField(
            value = DateFormatter.formatShort(selectedDate, locale),
            onValueChange = {},
            label = { Text(Strings["date_label"]) },
            modifier = Modifier.fillMaxWidth(0.5f).heightIn(min = 56.dp),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDialog = true }) {
                    Text("📅")
                }
            }
        )

        // Add buttons for previous/next business day
        Row(
            modifier = Modifier.fillMaxWidth(0.5f).padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-1/+1 " + Strings["day_label"],
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp)
            )
            // Previous day button
            OutlinedButton(
                onClick = { onDateSelected(selectedDate.previousBusinessDay()) },
                modifier = Modifier.heightIn(min = 28.dp).widthIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("←", style = MaterialTheme.typography.caption)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Next day button
            OutlinedButton(
                onClick = { onDateSelected(selectedDate.nextBusinessDay()) },
                modifier = Modifier.heightIn(min = 28.dp).widthIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("→", style = MaterialTheme.typography.caption)
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                modifier = Modifier.widthIn(min = 450.dp, max = 450.dp).heightIn(min = 450.dp, max = 550.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                currentYearMonth = currentYearMonth.minusMonths(1)
                            },
                            modifier = Modifier.onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                    currentYearMonth = currentYearMonth.minusMonths(1)
                                    true
                                } else false
                            }
                        ) {
                            // Use remember with locale as key to force recomposition when locale changes
                            val navPreviousText = remember(locale) { Strings["nav_previous"] }
                            Text(navPreviousText)
                        }
                        Text(
                            text = "${
                                currentYearMonth.month.getDisplayName(
                                    TextStyle.FULL,
                                    locale
                                )
                            } ${currentYearMonth.year}",
                            style = MaterialTheme.typography.h6
                        )
                        IconButton(
                            onClick = {
                                currentYearMonth = currentYearMonth.plusMonths(1)
                            },
                            modifier = Modifier.onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                                    currentYearMonth = currentYearMonth.plusMonths(1)
                                    true
                                } else false
                            }
                        ) {
                            // Use remember with locale as key to force recomposition when locale changes
                            val navNextText = remember(locale) { Strings["nav_next"] }
                            Text(navNextText)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp)) {
                        // Days of week header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in DayOfWeek.entries) {
                                Text(
                                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }

                        // Calendar grid
                        val firstDayOfMonth = currentYearMonth.atDay(1)
                        val daysInMonth = currentYearMonth.lengthOfMonth()
                        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
                        val weeks = (daysInMonth + firstDayOfWeek - 1 + 6) / 7

                        for (week in 0 until weeks) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (dayOfWeek in 1..7) {
                                    val day = week * 7 + dayOfWeek - firstDayOfWeek + 1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f) // Keep it square
                                            .padding(2.dp)
                                    ) {
                                        if (day in 1..daysInMonth) {
                                            val javaDate = currentYearMonth.atDay(day)
                                            val kotlinDate = javaDate.toKotlin()
                                            val isSelected = kotlinDate == selectedDate
                                            val isToday = kotlinDate == currentToday

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        onDateSelected(kotlinDate)
                                                        showDialog = false
                                                    },
                                                color = when {
                                                    isSelected -> MaterialTheme.colors.primary
                                                    isToday -> MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                                    else -> MaterialTheme.colors.surface
                                                },
                                                elevation = if (isSelected) 4.dp else 0.dp,
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Text(
                                                        text = day.toString(),
                                                        style = MaterialTheme.typography.body2,
                                                        color = when {
                                                            isSelected -> MaterialTheme.colors.onPrimary
                                                            isToday -> MaterialTheme.colors.primary
                                                            else -> MaterialTheme.colors.onSurface
                                                        },
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextButton(
                            onClick = {
                                val todayDate = today
                                onDateSelected(todayDate)
                                currentYearMonth = todayDate.toJavaYearMonth()
                                showDialog = false
                            }
                        ) {
                            Text(Strings["today"])
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = { showDialog = false }) {
                            Text(Strings["close"])
                        }
                    }
                }
            )
        }
    }
}
