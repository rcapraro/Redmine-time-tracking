package com.ps.redmine.config

data class Config(
    val redmineUri: String = "",
    val apiKey: String = "",
    val isDarkTheme: Boolean = false,
    val language: String = "fr", // Default to French
    val nonWorkingIsoDays: Set<Int> = emptySet() // Allowed: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri
)

object ConfigurationManager {
    private const val PREFERENCES_NODE = "com/ps/redmine"
    private const val KEY_REDMINE_URI = "redmine.uri"
    private const val KEY_API_KEY = "redmine.apiKey"
    private const val KEY_DARK_THEME = "redmine.darkTheme"
    private const val KEY_LANGUAGE = "redmine.language"
    private const val KEY_NON_WORKING_ISO_DAYS = "redmine.nonWorkingIsoDays"
    private const val KEY_NON_WORKING_ISO_DAY = "redmine.nonWorkingIsoDay" // legacy
    private const val KEY_WEEKLY_HOURS = "redmine.weeklyHours" // legacy

    private val preferences = java.util.prefs.Preferences.userRoot().node(PREFERENCES_NODE)

    fun loadConfig(): Config = Config(
        redmineUri = preferences.get(
            KEY_REDMINE_URI,
            System.getenv("REDMINE_URL") ?: "https://redmine.local/"
        ),
        apiKey = preferences.get(KEY_API_KEY, System.getenv("REDMINE_API_KEY") ?: ""),
        isDarkTheme = preferences.getBoolean(KEY_DARK_THEME, false),
        language = preferences.get(KEY_LANGUAGE, "fr"), // Default to French if not set
        nonWorkingIsoDays = run {
            // New CSV-based set
            val csv = preferences.get(KEY_NON_WORKING_ISO_DAYS, "")
            val parsed = csv.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..5 }.toSet()
            if (parsed.isNotEmpty()) return@run parsed
            // Fallback for legacy single day (previously supported 1..3)
            val legacyDay = preferences.getInt(KEY_NON_WORKING_ISO_DAY, -1).takeIf { it in 1..5 }
            legacyDay?.let { setOf(it) } ?: emptySet()
        }
    )

    fun saveConfig(config: Config) {
        preferences.put(KEY_REDMINE_URI, config.redmineUri)
        preferences.put(KEY_API_KEY, config.apiKey)
        preferences.putBoolean(KEY_DARK_THEME, config.isDarkTheme)
        preferences.put(KEY_LANGUAGE, config.language)
        // Store as CSV of ints (Mon..Fri -> 1..5), enforce maximum of 4 persisted selections
        val limited = config.nonWorkingIsoDays.filter { it in 1..5 }.sorted().take(4)
        val csv = limited.joinToString(",")
        preferences.put(KEY_NON_WORKING_ISO_DAYS, csv)
        preferences.flush()
    }
}
