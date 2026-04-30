package com.ps.redmine.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.DateFormatter
import com.ps.redmine.util.LocaleNames
import com.ps.redmine.util.atDay
import com.ps.redmine.util.lengthOfMonth
import com.ps.redmine.util.minusMonths
import com.ps.redmine.util.monthValue
import com.ps.redmine.util.nextBusinessDay
import com.ps.redmine.util.plusMonths
import com.ps.redmine.util.previousBusinessDay
import com.ps.redmine.util.today
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import java.util.Locale

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
                style = MaterialTheme.typography.labelSmall,
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
                            style = MaterialTheme.typography.titleMedium
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
                                    style = MaterialTheme.typography.labelSmall,
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

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        onDateSelected(kotlinDate)
                                                        showDialog = false
                                                    },
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.tertiaryContainer
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
                                                            isToday -> MaterialTheme.colorScheme.onTertiaryContainer
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
