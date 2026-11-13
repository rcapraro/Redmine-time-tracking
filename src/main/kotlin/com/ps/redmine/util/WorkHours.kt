package com.ps.redmine.util

import com.ps.redmine.config.ConfigurationManager

/**
 * Centralized hours configuration used across the application.
 */
object WorkHours {
    /** Legacy default number of working hours in a day (kept for backward compatibility). */
    const val DAILY_STANDARD_HOURS: Float = 7.5f

    /**
     * Tolerance for comparing accumulated hour sums against a target. Daily totals are summed
     * from individual entries, so exact equality would fail on Float rounding noise. Well below
     * the single decimal the UI displays.
     */
    const val HOURS_TOLERANCE: Float = 0.01f

    /**
     * Returns the currently configured daily hours from persisted configuration.
     * Falls back to the legacy default if unavailable.
     */
    fun configuredDailyHours(): Float = try {
        ConfigurationManager.loadConfig().dailyHours
    } catch (_: Exception) {
        DAILY_STANDARD_HOURS
    }

    /**
     * Hours Redmine keeps of an entry of [hours] on a day already holding [otherHours]: the server silently
     * truncates entries so that a day never goes past [dailyHours], and still answers the write with the hours sent.
     */
    fun hoursKept(hours: Float, otherHours: Float, dailyHours: Float): Float =
        minOf(hours, (dailyHours - otherHours).coerceAtLeast(0f))
}
