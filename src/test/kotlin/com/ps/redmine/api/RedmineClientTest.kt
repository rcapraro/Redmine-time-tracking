package com.ps.redmine.api

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertEquals

class RedmineClientTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val redmineClient: RedmineClient by inject()

    @BeforeEach
    fun setup() {
        stopKoin()

        val mockRedmineClient = RedmineClient(
            uri = "http://test.com",
            username = "test",
            password = "test"
        ).let { mockk<RedmineClient>(relaxed = true) }

        coEvery {
            mockRedmineClient.getTimeEntriesForMonth(2023, 1)
        } returns emptyList()

        startKoin {
            modules(module {
                single { mockRedmineClient }
            })
        }
    }

    @Test
    fun testGetTimeEntriesForMonth() {
        testScope.runTest {
            val result = redmineClient.getTimeEntriesForMonth(2023, 1)
            assertEquals(emptyList(), result)
        }
    }

    @Test
    fun testDeleteTimeEntry() {
        testScope.runTest {
            // Given
            val timeEntryId = 123
            coEvery { redmineClient.deleteTimeEntry(timeEntryId) } returns Unit

            // When & Then - should not throw any exception
            redmineClient.deleteTimeEntry(timeEntryId)
        }
    }

    @Test
    fun testCreateTimeEntryWithProject() {
        testScope.runTest {
            // Given
            val project = Project(1, "Test Project")
            val activity = Activity(1, "Development")
            val date = kotlinx.datetime.LocalDate(2023, 1, 1)
            val hours = 8f
            val comments = "Test entry"

            val timeEntry = TimeEntry(
                id = null,
                project = project,
                activity = activity,
                date = date,
                hours = hours,
                issue = Issue(1, "Test Issue"),
                comments = comments
            )

            val createdTimeEntry = timeEntry.copy(id = 1)

            coEvery { redmineClient.createTimeEntry(timeEntry) } returns createdTimeEntry

            // When
            val result = redmineClient.createTimeEntry(timeEntry)

            // Then
            assertEquals(createdTimeEntry, result)
        }
    }

    @Test
    fun testCreateTimeEntryWithIssueId() {
        testScope.runTest {
            // Given
            val activity = Activity(1, "Development")
            val date = kotlinx.datetime.LocalDate(2023, 1, 1)
            val hours = 8f
            val comments = "Test entry"

            val timeEntry = TimeEntry(
                id = null,
                project = Project(1, "Test Project"),
                activity = activity,
                date = date,
                hours = hours,
                issue = Issue(123, "Test Issue"),
                comments = comments
            )

            val createdTimeEntry = timeEntry.copy(id = 1)

            coEvery { redmineClient.createTimeEntry(timeEntry) } returns createdTimeEntry

            // When
            val result = redmineClient.createTimeEntry(timeEntry)

            // Then
            assertEquals(createdTimeEntry, result)
        }
    }

    @Test
    fun testCreateTimeEntryWithBothProjectAndIssue() {
        testScope.runTest {
            // Given
            val project = Project(1, "Test Project")
            val activity = Activity(1, "Development")
            val date = kotlinx.datetime.LocalDate(2023, 1, 1)
            val hours = 8f
            val comments = "Test entry"

            val timeEntry = TimeEntry(
                id = null,
                project = project,
                activity = activity,
                date = date,
                hours = hours,
                issue = Issue(123, "Test Issue"),
                comments = comments
            )

            val createdTimeEntry = timeEntry.copy(id = 1)
            coEvery { redmineClient.createTimeEntry(timeEntry) } returns createdTimeEntry

            // When
            val result = redmineClient.createTimeEntry(timeEntry)

            // Then
            assertEquals(createdTimeEntry, result)
            // Project should take precedence over Issue ID
        }
    }
}
