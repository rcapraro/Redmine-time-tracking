package com.ps.redmine.util

/**
 * Operating System types supported by the application
 */
enum class OperatingSystem {
    MACOS,
    WINDOWS,
    LINUX,
    UNKNOWN
}

/**
 * Utility object for operating system detection and OS-specific functionality
 */
object OSUtils {

    /**
     * Detects the current operating system
     */
    fun getCurrentOS(): OperatingSystem {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MACOS
            osName.contains("win") -> OperatingSystem.WINDOWS
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OperatingSystem.LINUX
            else -> OperatingSystem.UNKNOWN
        }
    }

    /**
     * Returns the appropriate modifier key symbol for the current OS
     */
    fun getModifierKeySymbol(): String {
        return when (getCurrentOS()) {
            OperatingSystem.MACOS -> "⌘"
            OperatingSystem.WINDOWS -> "Ctrl"
            OperatingSystem.LINUX -> "Ctrl"
            OperatingSystem.UNKNOWN -> "Ctrl" // Default to Ctrl for unknown systems
        }
    }

    /**
     * Returns the appropriate Alt key symbol for the current OS
     */
    fun getAltKeySymbol(): String {
        return when (getCurrentOS()) {
            OperatingSystem.MACOS -> "⌥"
            OperatingSystem.WINDOWS -> "Alt"
            OperatingSystem.LINUX -> "Alt"
            OperatingSystem.UNKNOWN -> "Alt" // Default to Alt for unknown systems
        }
    }

    /**
     * Formats a keyboard shortcut with the appropriate symbols for the current OS
     * @param key The key letter (e.g., "S", "T")
     * @param useModifier Whether to use the modifier key (Command/Ctrl)
     * @param useAlt Whether to use the Alt key
     * @return Formatted shortcut string (e.g., "⌘S", "Ctrl+S", "Alt+T")
     */
    fun formatShortcut(key: String, useModifier: Boolean = false, useAlt: Boolean = false): String {
        val parts = mutableListOf<String>()

        if (useModifier) {
            parts.add(getModifierKeySymbol())
        }

        if (useAlt) {
            parts.add(getAltKeySymbol())
        }

        parts.add(key)

        return when (getCurrentOS()) {
            OperatingSystem.MACOS -> parts.joinToString("")
            else -> parts.joinToString("+")
        }
    }
}