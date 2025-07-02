package com.ps.redmine.model

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RedmineApiModelsTest {

    @Test
    fun `test toDomainModel uses issue cache when available`() {
        // Create a RedmineTimeEntry with minimal issue information (as would come from API)
        val redmineTimeEntry = RedmineTimeEntry(
            id = 1,
            spentOn = "2024-01-15",
            hours = 8.0f,
            activity = RedmineActivity(1, "Development"),
            project = RedmineProject(1, "Test Project"),
            issue = RedmineIssue(123, ""), // Empty subject as would come from time entries API
            comments = "Test comment"
        )

        // Create an issue cache with full issue information
        val issueCache = mapOf(
            123 to Issue(123, "Fix critical bug in authentication system")
        )

        // Convert to domain model
        val domainTimeEntry = redmineTimeEntry.toDomainModel(issueCache)

        // Verify that the issue subject comes from the cache, not the API response
        assertEquals(123, domainTimeEntry.issue.id)
        assertEquals("Fix critical bug in authentication system", domainTimeEntry.issue.subject)
        assertEquals(LocalDate(2024, 1, 15), domainTimeEntry.date)
        assertEquals(8.0f, domainTimeEntry.hours)
        assertEquals("Test comment", domainTimeEntry.comments)
    }

    @Test
    fun `test toDomainModel falls back to API response when issue not in cache`() {
        // Create a RedmineTimeEntry with issue information
        val redmineTimeEntry = RedmineTimeEntry(
            id = 1,
            spentOn = "2024-01-15",
            hours = 8.0f,
            activity = RedmineActivity(1, "Development"),
            project = RedmineProject(1, "Test Project"),
            issue = RedmineIssue(456, "API Response Subject"),
            comments = "Test comment"
        )

        // Empty issue cache
        val issueCache = emptyMap<Int, Issue>()

        // Convert to domain model
        val domainTimeEntry = redmineTimeEntry.toDomainModel(issueCache)

        // Verify that the issue subject comes from the API response
        assertEquals(456, domainTimeEntry.issue.id)
        assertEquals("API Response Subject", domainTimeEntry.issue.subject)
    }

    @Test
    fun `test toDomainModel without issue cache parameter uses default behavior`() {
        // Create a RedmineTimeEntry with issue information
        val redmineTimeEntry = RedmineTimeEntry(
            id = 1,
            spentOn = "2024-01-15",
            hours = 8.0f,
            activity = RedmineActivity(1, "Development"),
            project = RedmineProject(1, "Test Project"),
            issue = RedmineIssue(789, "Default Behavior Subject"),
            comments = "Test comment"
        )

        // Convert to domain model without providing issue cache
        val domainTimeEntry = redmineTimeEntry.toDomainModel()

        // Verify that the issue subject comes from the API response
        assertEquals(789, domainTimeEntry.issue.id)
        assertEquals("Default Behavior Subject", domainTimeEntry.issue.subject)
    }
}
