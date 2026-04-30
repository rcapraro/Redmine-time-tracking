package com.ps.redmine.config

import java.util.prefs.BackingStoreException

data class Config(
    val redmineUri: String = "",
    val apiKey: String = "",
    val isDarkTheme: Boolean = false,
    val language: String = "fr", // Default to French
    val nonWorkingIsoDays: Set<Int> = emptySet(), // Allowed: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri
    val dailyHours: Float = 7.5f // Configurable daily hours (6.0 .. 7.5, step 0.5)
)

object ConfigurationManager {
    private const val PREFERENCES_NODE = "com/ps/redmine"
    private const val KEY_REDMINE_URI = "redmine.uri"
    private const val KEY_API_KEY = "redmine.apiKey"
    private const val KEY_DARK_THEME = "redmine.darkTheme"
    private const val KEY_LANGUAGE = "redmine.language"
    private const val KEY_NON_WORKING_ISO_DAYS = "redmine.nonWorkingIsoDays"
    private const val KEY_DAILY_HOURS = "redmine.dailyHours"

    private const val DAILY_HOURS_MIN = 6.0f
    private const val DAILY_HOURS_MAX = 7.5f
    private const val MAX_NON_WORKING_DAYS = 4

    private val preferences = java.util.prefs.Preferences.userRoot().node(PREFERENCES_NODE)

    /** Clamps to [DAILY_HOURS_MIN, DAILY_HOURS_MAX] and rounds to the nearest 0.5. */
    private fun normalizeDailyHours(value: Float): Float {
        val clamped = value.coerceIn(DAILY_HOURS_MIN, DAILY_HOURS_MAX)
        return Math.round(clamped * 2) / 2.0f
    }

    private fun parseNonWorkingDays(csv: String): Set<Int> =
        csv.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..5 }.toSet()

    /**
     * Resolves the dark/light preference. Order of precedence:
     * stored darkTheme pref → REDMINE_DARK_THEME env → false.
     */
    private fun resolveIsDarkTheme(): Boolean {
        preferences.get(KEY_DARK_THEME, null)?.let { return it.toBoolean() }
        System.getenv("REDMINE_DARK_THEME")?.let { return it.toBoolean() }
        return false
    }

    fun loadConfig(): Config = Config(
        redmineUri = preferences.get(
            KEY_REDMINE_URI,
            System.getenv("REDMINE_URL") ?: "https://redmine.local/"
        ),
        apiKey = preferences.get(KEY_API_KEY, System.getenv("REDMINE_API_KEY") ?: ""),
        isDarkTheme = resolveIsDarkTheme(),
        language = preferences.get(KEY_LANGUAGE, "fr"), // Default to French if not set
        nonWorkingIsoDays = parseNonWorkingDays(preferences.get(KEY_NON_WORKING_ISO_DAYS, "")),
        dailyHours = normalizeDailyHours(
            preferences.get(KEY_DAILY_HOURS, null)?.toFloatOrNull() ?: DAILY_HOURS_MAX
        )
    )

    /**
     * Persists the configuration. Returns true on success, false if the
     * preferences store could not be flushed — the caller can then surface
     * a user-visible error rather than crashing.
     */
    fun saveConfig(config: Config): Boolean {
        preferences.put(KEY_REDMINE_URI, config.redmineUri)
        preferences.put(KEY_API_KEY, config.apiKey)
        preferences.putBoolean(KEY_DARK_THEME, config.isDarkTheme)
        preferences.put(KEY_LANGUAGE, config.language)
        // Store as CSV of ints (Mon..Fri -> 1..5), enforce maximum of MAX_NON_WORKING_DAYS persisted selections
        val limited = config.nonWorkingIsoDays.sorted().take(MAX_NON_WORKING_DAYS)
        preferences.put(KEY_NON_WORKING_ISO_DAYS, limited.joinToString(","))
        preferences.put(KEY_DAILY_HOURS, normalizeDailyHours(config.dailyHours).toString())
        return try {
            preferences.flush()
            true
        } catch (e: BackingStoreException) {
            System.err.println("Warning: Failed to persist configuration: ${e.message}")
            false
        }
    }
}
