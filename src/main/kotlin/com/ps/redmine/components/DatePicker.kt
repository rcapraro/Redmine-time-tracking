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
import com.ps.redmine.util.DateFormatter
import com.ps.redmine.util.toJavaYearMonth
import com.ps.redmine.util.toKotlin
import com.ps.redmine.util.today
import kotlinx.datetime.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun DatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(selectedDate.toJavaYearMonth()) }
    val currentToday by remember { mutableStateOf(today) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = DateFormatter.formatShort(selectedDate),
            onValueChange = {},
            label = { Text(Strings["date_label"]) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDialog = true }) {
                    Text("📅")
                }
            }
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
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
                            Text(Strings["nav_previous"])
                        }
                        Text(
                            text = "${
                                currentYearMonth.month.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.getDefault()
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
                            Text(Strings["nav_next"])
                        }
                    }
                },
                text = {
                    Column {
                        // Days of week header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in java.time.DayOfWeek.values()) {
                                Text(
                                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
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
                                        modifier = Modifier.size(36.dp).padding(2.dp)
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
                },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
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
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showDialog = false }) {
                            Text(Strings["close"])
                        }
                    }
                }
            )
        }
    }
}
