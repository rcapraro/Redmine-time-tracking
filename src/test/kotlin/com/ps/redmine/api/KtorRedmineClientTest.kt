package com.ps.redmine.api

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KtorRedmineClientTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `getUserWeeklyHours returns value and caches`() = runTest {
        var callCount = 0
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    callCount++
                    assertTrue(req.url.toString().contains("/my/account.json"))
                    assertEquals(HttpMethod.Get, req.method)
                    val body = json.encodeToString(
                        com.ps.redmine.model.RedmineAccountResponse(
                            user = com.ps.redmine.model.RedmineUser(
                                id = 1,
                                login = "john",
                                customFields = listOf(
                                    com.ps.redmine.model.RedmineCustomField(
                                        id = 27,
                                        name = "Weekly Hours",
                                        value = "37.5"
                                    )
                                )
                            )
                        )
                    )
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = io.ktor.http.headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                    )
                }
            }
            defaultRequest {
                header("X-Redmine-API-Key", "test-key")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        }

        KtorRedmineClient("http://localhost", "test-key", client).use { api ->
            val first = api.getUserWeeklyHours()
            val second = api.getUserWeeklyHours()
            assertEquals(37.5f, first)
            assertEquals(37.5f, second)
            assertEquals(1, callCount, "Should use cached value on second call")
        }
    }

    @Test
    fun `getTimeEntriesForMonth handles pagination and maps domain`() = runTest {
        // Prepare 150 time entries split across 2 pages (100 + 50)
        fun makeEntry(id: Int, day: Int) = com.ps.redmine.model.RedmineTimeEntry(
            id = id,
            spentOn = "2024-01-%02d".format(day),
            hours = 7.5f,
            activity = com.ps.redmine.model.RedmineActivity(1, "Dev"),
            project = com.ps.redmine.model.RedmineProject(1, "Proj"),
            issue = com.ps.redmine.model.RedmineIssue(100, "Issue 100"),
            comments = "c$id"
        )

        val page1 = com.ps.redmine.model.RedmineTimeEntriesResponse(
            timeEntries = (1..100).map { makeEntry(it, (it % 28) + 1) },
            totalCount = 150,
            offset = 0,
            limit = 100
        )
        val page2 = com.ps.redmine.model.RedmineTimeEntriesResponse(
            timeEntries = (101..150).map { makeEntry(it, (it % 28) + 1) },
            totalCount = 150,
            offset = 100,
            limit = 100
        )

        // Projects with activities response required by client before mapping
        val projectsWithActivities = com.ps.redmine.model.RedmineProjectsWithActivitiesResponse(
            projects = listOf(
                com.ps.redmine.model.RedmineProjectWithActivities(
                    id = 1,
                    name = "Proj",
                    timeEntryActivities = listOf(com.ps.redmine.model.RedmineActivity(1, "Dev"))
                )
            )
        )

        // Issue details response
        val issueResponse = com.ps.redmine.model.RedmineIssueResponse(
            issue = com.ps.redmine.model.RedmineIssue(100, "Issue 100")
        )

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    val url = req.url
                    val path = url.encodedPath
                    val params = url.parameters
                    when {
                        path.endsWith("/time_entries.json") && params["offset"] == "0" -> {
                            respond(
                                content = json.encodeToString(page1),
                                status = HttpStatusCode.OK,
                                headers = io.ktor.http.headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                            )
                        }

                        path.endsWith("/time_entries.json") && params["offset"] == "100" -> {
                            respond(
                                content = json.encodeToString(page2),
                                status = HttpStatusCode.OK,
                                headers = io.ktor.http.headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                            )
                        }

                        path.endsWith("/projects.json") -> {
                            respond(
                                content = json.encodeToString(projectsWithActivities),
                                status = HttpStatusCode.OK,
                                headers = io.ktor.http.headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                            )
                        }

                        path.endsWith("/issues/100.json") -> {
                            respond(
                                content = json.encodeToString(issueResponse),
                                status = HttpStatusCode.OK,
                                headers = io.ktor.http.headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                            )
                        }

                        else -> error("Unhandled path: ${'$'}path?${'$'}{params.buildString()}")
                    }
                }
            }
            defaultRequest {
                header("X-Redmine-API-Key", "test-key")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        }

        val api = KtorRedmineClient("http://localhost", "test-key", client)
        val entries = api.getTimeEntriesForMonth(2024, 1)
        assertEquals(150, entries.size)
        // Check a few mapped fields
        val sample = entries.first()
        assertEquals(7.5f, sample.hours)
        assertEquals(1, sample.activity.id)
        assertEquals("Dev", sample.activity.name)
        assertEquals(1, sample.project.id)
        assertEquals("Proj", sample.project.name)
        assertEquals(100, sample.issue.id)
        assertEquals("Issue 100", sample.issue.subject)
    }

    @Test
    fun `createTimeEntry success`() = runTest {
        val createdResponse = com.ps.redmine.model.RedmineTimeEntryResponse(
            timeEntry = com.ps.redmine.model.RedmineTimeEntry(
                id = 10,
                spentOn = "2024-01-10",
                hours = 8.0f,
                activity = com.ps.redmine.model.RedmineActivity(1, "Dev"),
                project = com.ps.redmine.model.RedmineProject(1, "Proj"),
                issue = com.ps.redmine.model.RedmineIssue(200, "Issue 200"),
                comments = "work"
            )
        )
        val issueResponse = com.ps.redmine.model.RedmineIssueResponse(
            issue = com.ps.redmine.model.RedmineIssue(200, "Issue 200")
        )

        var sawApiKey = false
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    if (req.method == HttpMethod.Post && req.url.encodedPath.endsWith("/time_entries.json")) {
                        sawApiKey = req.headers["X-Redmine-API-Key"] == "test-key"
                        respond(
                            content = json.encodeToString(createdResponse),
                            status = HttpStatusCode.OK,
                            headers = io.ktor.http.headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                        )
                    } else if (req.method == HttpMethod.Get && req.url.encodedPath.endsWith("/issues/200.json")) {
                        respond(
                            content = json.encodeToString(issueResponse),
                            status = HttpStatusCode.OK,
                            headers = io.ktor.http.headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString()
                            )
                        )
                    } else error("Unhandled request: ${'$'}{req.method} ${'$'}{req.url}")
                }
            }
            defaultRequest {
                header("X-Redmine-API-Key", "test-key")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        }

        val api = KtorRedmineClient("http://localhost", "test-key", client)
        val timeEntry = TimeEntry(
            id = null,
            date = kotlinx.datetime.LocalDate(2024, 1, 10),
            hours = 8.0f,
            activity = Activity(1, "Dev"),
            project = Project(1, "Proj"),
            issue = Issue(200, "Issue 200"),
            comments = "work"
        )

        val created = api.createTimeEntry(timeEntry)
        assertEquals(10, created.id)
        assertTrue(sawApiKey, "API key header should be present")
    }

    @Test
    fun `error 422 surfaces as validation error`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { req ->
                    if (req.method == HttpMethod.Post && req.url.encodedPath.endsWith("/time_entries.json")) {
                        respond(
                            content = "{\"errors\":[\"Invalid\"]}",
                            status = HttpStatusCode.UnprocessableEntity,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        )
                    } else error("Unhandled request: ${'$'}{req.method} ${'$'}{req.url}")
                }
            }
            defaultRequest {
                header("X-Redmine-API-Key", "test-key")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        }

        val api = KtorRedmineClient("http://localhost", "test-key", client)
        val timeEntry = TimeEntry(
            id = null,
            date = kotlinx.datetime.LocalDate(2024, 1, 10),
            hours = 0.0f,
            activity = Activity(1, "Dev"),
            project = Project(1, "Proj"),
            issue = Issue(0, ""),
            comments = null
        )

        val ex = try {
            api.createTimeEntry(timeEntry)
            fail("Expected RedmineApiException to be thrown")
        } catch (e: KtorRedmineClient.RedmineApiException) {
            e
        }
        assertEquals(422, ex.statusCode)
        assertTrue(ex.message!!.contains("Validation error"))
    }
}
