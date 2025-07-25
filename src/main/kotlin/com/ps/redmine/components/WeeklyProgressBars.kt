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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.WeekInfo
import com.ps.redmine.util.getWeeksInMonth
import com.ps.redmine.util.toJava
import java.time.YearMonth
import java.time.temporal.IsoFields

/**
 * Data class representing weekly progress information.
 */
data class WeeklyProgress(
    val weekInfo: WeekInfo,
    val actualHours: Float,
    val expectedHours: Float,
    val progressPercentage: Float
)

/**
 * Calculates weekly progress for all weeks in a month.
 */
fun calculateWeeklyProgress(timeEntries: List<TimeEntry>, yearMonth: YearMonth): List<WeeklyProgress> {
    val weeks = getWeeksInMonth(yearMonth.year, yearMonth.monthValue)

    return weeks.map { weekInfo ->
        // Calculate actual hours for this week
        val actualHours = timeEntries
            .filter { entry ->
                entry.date >= weekInfo.startDate && entry.date <= weekInfo.endDate
            }
            .sumOf { it.hours.toDouble() }
            .toFloat()

        // Calculate expected hours (7.5 hours per working day)
        val expectedHours = weekInfo.workingDays * 7.5f

        // Calculate progress percentage
        val progressPercentage = if (expectedHours > 0) {
            (actualHours / expectedHours * 100).coerceAtMost(100f)
        } else {
            0f
        }

        WeeklyProgress(
            weekInfo = weekInfo,
            actualHours = actualHours,
            expectedHours = expectedHours,
            progressPercentage = progressPercentage
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
    modifier: Modifier = Modifier
) {
    val weeklyProgress = remember(timeEntries, currentMonth) {
        calculateWeeklyProgress(timeEntries, currentMonth).reversed()
    }

    Column(
        modifier = modifier.width(44.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weeklyProgress.forEach { progress ->
            WeekProgressBar(
                progress = progress,
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
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    val backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    val isCompleted = progress.actualHours >= progress.expectedHours

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
            }
        }

        // Progress percentage text
        Text(
            text = "${progress.progressPercentage.toInt()}%",
            fontSize = 9.sp, // Increased font size for better readability
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}