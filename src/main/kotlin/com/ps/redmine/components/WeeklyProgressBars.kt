package com.ps.redmine.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.*
import kotlinx.datetime.YearMonth

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

        val effectiveWorkingDaysInWeek = countWorkingDays(rangeStartK, rangeEndK, excludedIsoDays)
        val expectedHours = effectiveWorkingDaysInWeek * WorkHours.configuredDailyHours()

        val isNonWorkedWeek = rangeStartK <= rangeEndK && effectiveWorkingDaysInWeek == 0

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
            TooltipArea(
                tooltip = { WeekTooltip(progress, isCurrentWeek) },
                modifier = Modifier.weight(1f),
                delayMillis = 500
            ) {
                WeekProgressBar(
                    progress = progress,
                    isCurrentWeek = isCurrentWeek,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = onWeekClick != null) { onWeekClick?.invoke(progress.weekInfo) }
                )
            }
        }
    }
}

/** Tooltip card shown when hovering a week bar. Explains the bar + selection/non-worked markers. */
@Composable
private fun WeekTooltip(progress: WeeklyProgress, isCurrentWeek: Boolean) {
    val isoWeek = isoWeekNumber(progress.weekInfo.startDate)
    val startDate = DateFormatter.formatShort(progress.weekInfo.startDate)
    val endDate = DateFormatter.formatShort(progress.weekInfo.endDate)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = Strings["week_tooltip_dates"].format(isoWeek, startDate, endDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = Strings["week_tooltip_hours"].format(progress.actualHours, progress.expectedHours),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isCurrentWeek) {
                Text(
                    text = Strings["week_tooltip_current"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (progress.isNonWorkedWeek) {
                Text(
                    text = Strings["week_tooltip_non_worked"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Individual vertical progress bar for a single week. */
@Composable
private fun WeekProgressBar(
    progress: WeeklyProgress,
    isCurrentWeek: Boolean,
    modifier: Modifier = Modifier
) {
    // In-progress weeks get primary (Violet); complete weeks switch to secondary (Emerald)
    // so a glance at the bar column tells which weeks are done vs still owed.
    val isComplete = progress.progressPercentage >= 100f
    val fillColor = if (isComplete) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    // High-contrast neutral (near-black in light theme, near-white in dark) — guarantees
    // the current-week marker stays visible over any fill (Violet primary, Emerald
    // secondary, gray background) and reads as "selected" through being the only bar with
    // a border, not through hue. The non-worked-week cross uses the same token so both
    // status markers share a consistent emphasis level.
    val currentWeekStrokeColor = MaterialTheme.colorScheme.onSurface
    val nonWorkedMarkerColor = MaterialTheme.colorScheme.onSurface

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
            style = MaterialTheme.typography.labelSmall,
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

            if (animatedPercent > 0) {
                val progressHeight = (barHeight * (animatedPercent / 100f)).coerceAtMost(barHeight)

                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(0f, barHeight - progressHeight),
                    size = Size(barWidth, progressHeight),
                    cornerRadius = cornerRadius
                )

                if (progress.isNonWorkedWeek) {
                    val stroke = 2.dp.toPx()
                    val inset = 3.dp.toPx()
                    drawLine(
                        color = nonWorkedMarkerColor,
                        start = Offset(inset, inset),
                        end = Offset(barWidth - inset, barHeight - inset),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = nonWorkedMarkerColor,
                        start = Offset(barWidth - inset, inset),
                        end = Offset(inset, barHeight - inset),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (isCurrentWeek) {
                drawRoundRect(
                    color = currentWeekStrokeColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        Text(
            text = "${progress.progressPercentage.toInt()}%" + if (progress.isNonWorkedWeek) "•" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}
