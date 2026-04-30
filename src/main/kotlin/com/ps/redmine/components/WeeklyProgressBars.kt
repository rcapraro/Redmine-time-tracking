package com.ps.redmine.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ps.redmine.util.atDay
import com.ps.redmine.util.getWeeksInMonth
import com.ps.redmine.util.isoWeekNumber
import com.ps.redmine.util.lengthOfMonth
import com.ps.redmine.util.monthValue
import com.ps.redmine.util.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

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
    excludedIsoDays: Set<Int> = emptySet()
): List<WeeklyProgress> {
    val weeks = getWeeksInMonth(yearMonth.year, yearMonth.monthValue)

    return weeks.map { weekInfo ->
        val actualHours = timeEntries
            .filter { entry -> entry.date >= weekInfo.startDate && entry.date <= weekInfo.endDate }
            .sumOf { it.hours.toDouble() }
            .toFloat()

        val firstDayOfMonthK = yearMonth.atDay(1)
        val lastDayOfMonthK = yearMonth.atDay(yearMonth.lengthOfMonth())
        val rangeStartK = if (weekInfo.startDate < firstDayOfMonthK) firstDayOfMonthK else weekInfo.startDate
        val rangeEndK = if (weekInfo.endDate > lastDayOfMonthK) lastDayOfMonthK else weekInfo.endDate

        var effectiveWorkingDaysInWeek = 0
        var dateCursorK = rangeStartK
        var clampedDaysCount = 0
        while (dateCursorK <= rangeEndK) {
            clampedDaysCount++
            val iso = dateCursorK.dayOfWeek.isoDayNumber
            if (iso in 1..5 && (excludedIsoDays.isEmpty() || !excludedIsoDays.contains(iso))) {
                effectiveWorkingDaysInWeek++
            }
            dateCursorK = dateCursorK.plus(1, DateTimeUnit.DAY)
        }
        val expectedHours = effectiveWorkingDaysInWeek * WorkHours.configuredDailyHours()

        val isNonWorkedWeek = clampedDaysCount > 0 && effectiveWorkingDaysInWeek == 0

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
    modifier: Modifier = Modifier,
    onWeekClick: ((WeekInfo) -> Unit)? = null
) {
    val weeklyProgress = remember(timeEntries, currentMonth, excludedIsoDays) {
        calculateWeeklyProgress(timeEntries, currentMonth, excludedIsoDays).reversed()
    }

    val today = today
    val isCurrentMonth = currentMonth.year == today.year && currentMonth.month == today.month

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
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = onWeekClick != null) { onWeekClick?.invoke(progress.weekInfo) }
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val isCompleted = progress.actualHours >= progress.expectedHours
    val currentWeekStrokeColor = MaterialTheme.colorScheme.outline
    val nonWorkedMarkerColor = MaterialTheme.colorScheme.onSurfaceVariant

    val isoWeekNumber = isoWeekNumber(progress.weekInfo.startDate)

    val animatedPercent by animateFloatAsState(
        targetValue = progress.progressPercentage,
        animationSpec = tween(durationMillis = 450),
        label = "weekProgress",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings["week_label"].format(isoWeekNumber),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Canvas(
            modifier = Modifier
                .width(16.dp)
                .weight(1f)
                .padding(horizontal = 1.dp)
        ) {
            val barWidth = size.width
            val barHeight = size.height
            val cornerRadius = CornerRadius(4.dp.toPx())

            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )

            if (isCurrentWeek) {
                drawRoundRect(
                    color = currentWeekStrokeColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            if (animatedPercent > 0) {
                val progressHeight = (barHeight * (animatedPercent / 100f)).coerceAtMost(barHeight)
                val progressColor = if (isCompleted) secondaryColor else primaryColor

                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(0f, barHeight - progressHeight),
                    size = Size(barWidth, progressHeight),
                    cornerRadius = cornerRadius
                )

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

        Text(
            text = "${progress.progressPercentage.toInt()}%" + if (progress.isNonWorkedWeek) "•" else "",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}
