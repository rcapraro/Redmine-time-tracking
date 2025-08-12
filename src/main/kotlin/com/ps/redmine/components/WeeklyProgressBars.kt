package com.ps.redmine.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.WeekInfo
import com.ps.redmine.util.WorkHours
import com.ps.redmine.util.getWeeksInMonth
import com.ps.redmine.util.toJava
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.YearMonth
import java.time.temporal.IsoFields
import kotlin.time.Clock

/**
 * Data class representing weekly progress information.
 */
data class WeeklyProgress(
    val weekInfo: WeekInfo,
    val actualHours: Float,
    val expectedHours: Float,
    val progressPercentage: Float,
    val isNonWorkedWeek: Boolean
)

/**
 * Calculates weekly progress for all weeks in a month.
 */
fun calculateWeeklyProgress(
    timeEntries: List<TimeEntry>,
    yearMonth: YearMonth,
    excludedIsoDays: Set<Int> = emptySet() // Allowed: 1=Mon,2=Tue,3=Wed
): List<WeeklyProgress> {
    val weeks = getWeeksInMonth(yearMonth.year, yearMonth.monthValue)

    return weeks.map { weekInfo ->
        // Calculate actual hours for this week
        val actualHours = timeEntries
            .filter { entry ->
                entry.date >= weekInfo.startDate && entry.date <= weekInfo.endDate
            }
            .sumOf { it.hours.toDouble() }
            .toFloat()

        // Calculate expected hours using weekly hours proportionally to effective working days, possibly excluding a fixed non-working weekday
        val firstDayOfMonth = java.time.LocalDate.of(yearMonth.year, yearMonth.monthValue, 1)
        val lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth())
        // Clamp the week range to the current month
        val rawStart = weekInfo.startDate.toJava()
        val rawEnd = weekInfo.endDate.toJava()
        val rangeStart = if (rawStart.isBefore(firstDayOfMonth)) firstDayOfMonth else rawStart
        val rangeEnd = if (rawEnd.isAfter(lastDayOfMonth)) lastDayOfMonth else rawEnd

        var effectiveWorkingDaysInWeek = 0
        var dateCursor = rangeStart
        var clampedDaysCount = 0
        while (!dateCursor.isAfter(rangeEnd)) {
            clampedDaysCount++
            val iso = dateCursor.dayOfWeek.value // 1=Mon..7=Sun
            if (iso in 1..5 && (excludedIsoDays.isEmpty() || !excludedIsoDays.contains(iso))) {
                effectiveWorkingDaysInWeek++
            }
            dateCursor = dateCursor.plusDays(1)
        }
        val expectedHours = effectiveWorkingDaysInWeek * WorkHours.DAILY_STANDARD_HOURS

        // Determine if this is a non-worked week within the current month (only non-working days fall inside the month)
        val isNonWorkedWeek = clampedDaysCount > 0 && effectiveWorkingDaysInWeek == 0

        // Calculate progress percentage
        val progressPercentage = if (expectedHours > 0) {
            (actualHours / expectedHours * 100).coerceAtMost(100f)
        } else {
            if (isNonWorkedWeek) 100f else 0f
        }

        WeeklyProgress(
            weekInfo = weekInfo,
            actualHours = actualHours,
            expectedHours = expectedHours,
            progressPercentage = progressPercentage,
            isNonWorkedWeek = isNonWorkedWeek
        )
    }
}

/**
 * Composable that displays vertical progress bars for each week of the month.
 */
@Composable
fun WeeklyProgressBars(
    timeEntries: List<TimeEntry>,
    currentMonth: YearMonth,
    excludedIsoDays: Set<Int> = emptySet(),
    modifier: Modifier = Modifier
) {
    val weeklyProgress = remember(timeEntries, currentMonth, excludedIsoDays) {
        calculateWeeklyProgress(timeEntries, currentMonth, excludedIsoDays).reversed()
    }

    // Determine today's date and whether the displayed month is the current calendar month
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val isCurrentMonth = currentMonth == YearMonth.now()

    Column(
        modifier = modifier.width(44.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weeklyProgress.forEach { progress ->
            val isCurrentWeek =
                isCurrentMonth && today >= progress.weekInfo.startDate && today <= progress.weekInfo.endDate
            WeekProgressBar(
                progress = progress,
                isCurrentWeek = isCurrentWeek,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual vertical progress bar for a single week.
 */
@Composable
private fun WeekProgressBar(
    progress: WeeklyProgress,
    isCurrentWeek: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    val backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    val isCompleted = progress.actualHours >= progress.expectedHours
    val currentWeekStrokeColor = MaterialTheme.colors.onSurface.copy(alpha = 0.35f)
    val nonWorkedMarkerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.45f)

    // Calculate the real ISO week number from the week's start date
    val isoWeekNumber = progress.weekInfo.startDate.toJava().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Week number label with internationalization
        Text(
            text = Strings["week_label"].format(isoWeekNumber),
            fontSize = 10.sp, // Increased font size for better readability
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Vertical progress bar
        Canvas(
            modifier = Modifier
                .width(16.dp)
                .weight(1f)
                .padding(horizontal = 1.dp)
        ) {
            val barWidth = size.width
            val barHeight = size.height
            val cornerRadius = CornerRadius(4.dp.toPx())

            // Draw background
            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )

            // Subtle indicator for current week: outline stroke
            if (isCurrentWeek) {
                drawRoundRect(
                    color = currentWeekStrokeColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Draw progress
            if (progress.progressPercentage > 0) {
                val progressHeight = (barHeight * (progress.progressPercentage / 100f)).coerceAtMost(barHeight)
                val progressColor = if (isCompleted) secondaryColor else primaryColor

                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(0f, barHeight - progressHeight),
                    size = Size(barWidth, progressHeight),
                    cornerRadius = cornerRadius
                )

                // Special marker for non-worked week: draw a subtle cross overlay
                if (progress.isNonWorkedWeek) {
                    val stroke = 1.5f
                    drawLine(
                        color = nonWorkedMarkerColor,
                        start = Offset(0f, 0f),
                        end = Offset(barWidth, barHeight),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = nonWorkedMarkerColor,
                        start = Offset(barWidth, 0f),
                        end = Offset(0f, barHeight),
                        strokeWidth = stroke
                    )
                }
            }
        }

        // Progress percentage text
        Text(
            text = "${progress.progressPercentage.toInt()}%" + if (progress.isNonWorkedWeek) "•" else "",
            fontSize = 9.sp, // Increased font size for better readability
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}