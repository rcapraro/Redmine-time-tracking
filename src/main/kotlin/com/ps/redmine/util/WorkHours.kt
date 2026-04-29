package com.ps.redmine.util

import com.ps.redmine.config.ConfigurationManager

/**
 * Centralized hours configuration used across the application.
 */
object WorkHours {
    /** Legacy default number of working hours in a day (kept for backward compatibility). */
    const val DAILY_STANDARD_HOURS: Float = 7.5f

    /**
     * Returns the currently configured daily hours from persisted configuration.
     * Falls back to the legacy default if unavailable.
     */
    fun configuredDailyHours(): Float = try {
        ConfigurationManager.loadConfig().dailyHours
    } catch (_: Exception) {
        DAILY_STANDARD_HOURS
    }
}
