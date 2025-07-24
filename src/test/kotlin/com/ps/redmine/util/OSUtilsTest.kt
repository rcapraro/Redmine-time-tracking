package com.ps.redmine.util

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OSUtilsTest {

    @Test
    fun `test formatShortcut with modifier key`() {
        val result = OSUtils.formatShortcut("S", useModifier = true)

        // Should contain either ⌘S (macOS) or Ctrl+S (Windows/Linux)
        assertTrue(
            result == "⌘S" || result == "Ctrl+S",
            "Expected ⌘S or Ctrl+S, but got: $result"
        )
    }

    @Test
    fun `test formatShortcut with alt key`() {
        val result = OSUtils.formatShortcut("T", useAlt = true)

        // Should contain either ⌥T (macOS) or Alt+T (Windows/Linux)
        assertTrue(
            result == "⌥T" || result == "Alt+T",
            "Expected ⌥T or Alt+T, but got: $result"
        )
    }

    @Test
    fun `test getCurrentOS returns valid OS`() {
        val os = OSUtils.getCurrentOS()
        assertTrue(
            os in listOf(
                OperatingSystem.MACOS, OperatingSystem.WINDOWS,
                OperatingSystem.LINUX, OperatingSystem.UNKNOWN
            )
        )
    }
}