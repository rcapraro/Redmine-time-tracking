package com.ps.redmine.util

/**
 * Centralized constants for standard working hours used across the application.
 *
 * Having a single source of truth avoids magic numbers scattered in the codebase
 * and simplifies future changes (e.g., if the standard day length changes).
 */
object WorkHours {
    /** Standard number of working hours in a day. */
    const val DAILY_STANDARD_HOURS: Float = 7.5f
}
