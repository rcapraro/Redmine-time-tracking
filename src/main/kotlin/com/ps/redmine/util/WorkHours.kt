package com.ps.redmine.util

import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.model.Hours

/**
 * Centralized hours configuration used across the application.
 */
object WorkHours {
    /** Legacy default number of working hours in a day (kept for backward compatibility). */
    val DAILY_STANDARD_HOURS: Hours = Hours.of(7.5)

    /**
     * Returns the currently configured daily hours from persisted configuration.
     * Falls back to the legacy default if unavailable.
     */
    fun configuredDailyHours(): Hours = try {
        ConfigurationManager.loadConfig().dailyHours
    } catch (_: Exception) {
        DAILY_STANDARD_HOURS
    }

    /**
     * Hours Redmine keeps of an entry of [hours] on a day already holding [otherHours]: the server silently
     * truncates entries so that a day never goes past [dailyHours], and still answers the write with the hours sent.
     */
    fun hoursKept(hours: Hours, otherHours: Hours, dailyHours: Hours): Hours =
        minOf(hours, (dailyHours - otherHours).coerceAtLeast(Hours.ZERO))
}
