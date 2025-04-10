package com.ps.redmine.config

data class Config(
    val redmineUri: String = "",
    val apiKey: String = "",
    val isDarkTheme: Boolean = false,
    val language: String = "fr" // Default to French
)

object ConfigurationManager {
    private const val PREFERENCES_NODE = "com/ps/redmine"
    private const val KEY_REDMINE_URI = "redmine.uri"
    private const val KEY_API_KEY = "redmine.apiKey"
    private const val KEY_DARK_THEME = "redmine.darkTheme"
    private const val KEY_LANGUAGE = "redmine.language"

    private val preferences = java.util.prefs.Preferences.userRoot().node(PREFERENCES_NODE)

    fun loadConfig(): Config = Config(
        redmineUri = preferences.get(
            KEY_REDMINE_URI,
            System.getenv("REDMINE_URL") ?: "https://redmine.local/"
        ),
        apiKey = preferences.get(KEY_API_KEY, System.getenv("REDMINE_API_KEY") ?: ""),
        isDarkTheme = preferences.getBoolean(KEY_DARK_THEME, false),
        language = preferences.get(KEY_LANGUAGE, "fr") // Default to French if not set
    )

    fun saveConfig(config: Config) {
        preferences.put(KEY_REDMINE_URI, config.redmineUri)
        preferences.put(KEY_API_KEY, config.apiKey)
        preferences.putBoolean(KEY_DARK_THEME, config.isDarkTheme)
        preferences.put(KEY_LANGUAGE, config.language)
        preferences.flush()
    }
}
