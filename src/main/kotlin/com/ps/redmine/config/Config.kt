package com.ps.redmine.config

data class Config(
    val redmineUri: String = "",
    val username: String = "",
    val password: String = "",
    val isDarkTheme: Boolean = false,
    val language: String = "fr" // Default to French
)

object ConfigurationManager {
    private const val PREFERENCES_NODE = "com/ps/redmine"
    private const val KEY_REDMINE_URI = "redmine.uri"
    private const val KEY_USERNAME = "redmine.username"
    private const val KEY_PASSWORD = "redmine.password"
    private const val KEY_DARK_THEME = "redmine.darkTheme"
    private const val KEY_LANGUAGE = "redmine.language"

    private val preferences = java.util.prefs.Preferences.userRoot().node(PREFERENCES_NODE)

    fun loadConfig(): Config = Config(
        redmineUri = preferences.get(
            KEY_REDMINE_URI,
            System.getenv("REDMINE_URL") ?: "https://redmine.local/"
        ),
        username = preferences.get(KEY_USERNAME, System.getenv("REDMINE_USERNAME") ?: ""),
        password = preferences.get(KEY_PASSWORD, System.getenv("REDMINE_PASSWORD") ?: ""),
        isDarkTheme = preferences.getBoolean(KEY_DARK_THEME, false),
        language = preferences.get(KEY_LANGUAGE, "fr") // Default to French if not set
    )

    fun saveConfig(config: Config) {
        preferences.put(KEY_REDMINE_URI, config.redmineUri)
        preferences.put(KEY_USERNAME, config.username)
        preferences.put(KEY_PASSWORD, config.password)
        preferences.putBoolean(KEY_DARK_THEME, config.isDarkTheme)
        preferences.put(KEY_LANGUAGE, config.language)
        preferences.flush()
    }
}
