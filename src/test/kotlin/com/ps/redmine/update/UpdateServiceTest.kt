package com.ps.redmine.update

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UpdateServiceTest {

    @Test
    fun `isNewerVersion should return true when latest version is newer`() {
        val updateService = UpdateService()

        // Use reflection to access private method for testing
        val method =
            UpdateService::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        // Test cases where latest is newer
        assertTrue(method.invoke(updateService, "1.0.0", "1.0.1") as Boolean)
        assertTrue(method.invoke(updateService, "1.0.0", "1.1.0") as Boolean)
        assertTrue(method.invoke(updateService, "1.0.0", "2.0.0") as Boolean)
        assertTrue(method.invoke(updateService, "1.2.3", "1.2.4") as Boolean)
        assertTrue(method.invoke(updateService, "1.2.3", "1.3.0") as Boolean)
        assertTrue(method.invoke(updateService, "1.2.3", "2.0.0") as Boolean)
    }

    @Test
    fun `isNewerVersion should return false when latest version is same or older`() {
        val updateService = UpdateService()

        // Use reflection to access private method for testing
        val method =
            UpdateService::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        // Test cases where latest is same or older
        assertFalse(method.invoke(updateService, "1.0.0", "1.0.0") as Boolean)
        assertFalse(method.invoke(updateService, "1.0.1", "1.0.0") as Boolean)
        assertFalse(method.invoke(updateService, "1.1.0", "1.0.0") as Boolean)
        assertFalse(method.invoke(updateService, "2.0.0", "1.0.0") as Boolean)
        assertFalse(method.invoke(updateService, "1.2.4", "1.2.3") as Boolean)
        assertFalse(method.invoke(updateService, "1.3.0", "1.2.3") as Boolean)
        assertFalse(method.invoke(updateService, "2.0.0", "1.2.3") as Boolean)
    }

    @Test
    fun `isNewerVersion should handle different version formats`() {
        val updateService = UpdateService()

        // Use reflection to access private method for testing
        val method =
            UpdateService::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        // Test cases with different formats
        assertTrue(method.invoke(updateService, "1.0", "1.0.1") as Boolean)
        assertTrue(method.invoke(updateService, "1", "1.0.1") as Boolean)
        assertFalse(method.invoke(updateService, "1.0.1", "1.0") as Boolean)
        assertFalse(method.invoke(updateService, "1.0.1", "1") as Boolean)
    }

    @Test
    fun `isNewerVersion should handle invalid version numbers gracefully`() {
        val updateService = UpdateService()

        // Use reflection to access private method for testing
        val method =
            UpdateService::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        // Test cases with invalid version numbers (should treat as 0)
        // "1.0.a" becomes "1.0.0" when 'a' is converted to 0, so they are equal
        assertFalse(method.invoke(updateService, "1.0.0", "1.0.a") as Boolean)
        assertFalse(method.invoke(updateService, "1.0.a", "1.0.0") as Boolean)
        assertFalse(method.invoke(updateService, "1.0.a", "1.0.b") as Boolean)

        // But "1.0.0" should be newer than "1.0.a" when 'a' part is missing
        assertTrue(method.invoke(updateService, "1.0", "1.0.1") as Boolean)
    }

    @Test
    fun `getPlatformAsset should return correct asset for platform`() {
        val updateService = UpdateService()

        // Use reflection to access private method for testing
        val method = UpdateService::class.java.getDeclaredMethod("getPlatformAsset", List::class.java)
        method.isAccessible = true

        val assets = listOf(
            GitHubAsset(
                "RedmineTime-1.0.0.dmg",
                "https://github.com/test/releases/download/v1.0.0/RedmineTime-1.0.0.dmg",
                1000
            ),
            GitHubAsset(
                "RedmineTime-1.0.0.msi",
                "https://github.com/test/releases/download/v1.0.0/RedmineTime-1.0.0.msi",
                2000
            ),
            GitHubAsset(
                "RedmineTime-1.0.0.deb",
                "https://github.com/test/releases/download/v1.0.0/RedmineTime-1.0.0.deb",
                3000
            )
        )

        val result = method.invoke(updateService, assets) as GitHubAsset?

        // Should return an asset based on current platform
        assertNotNull(result)
        assertTrue(result!!.name.contains("RedmineTime-1.0.0"))
        assertTrue(result.size > 0)
    }

    @Test
    fun `getPlatformAsset should return null when no matching asset`() {
        val updateService = UpdateService()

        // Use reflection to access private method for testing
        val method = UpdateService::class.java.getDeclaredMethod("getPlatformAsset", List::class.java)
        method.isAccessible = true

        val assets = listOf(
            GitHubAsset("SomeOtherApp.zip", "https://github.com/test/releases/download/v1.0.0/SomeOtherApp.zip", 1000)
        )

        val result = method.invoke(updateService, assets) as GitHubAsset?

        // Should return null when no matching platform asset found
        assertNull(result)
    }
}