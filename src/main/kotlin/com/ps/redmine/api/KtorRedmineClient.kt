package com.ps.redmine.api

import com.ps.redmine.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.cancellation.CancellationException
import com.ps.redmine.model.TimeEntry as AppTimeEntry

/**
 * Implementation of RedmineClientInterface that uses Ktor Client for HTTP requests.
 * This is a more Kotlin-idiomatic approach compared to the Java HttpClient used in DirectRedmineClient.
 */
class KtorRedmineClient(
    initialUri: String,
    initialApiKey: String,
    private val httpClientOverride: HttpClient? = null
) : RedmineClientInterface {

    @Volatile
    private var cachedWeeklyHours: Float? = null

    /**
     * Immutable snapshot of the mutable state that can be swapped atomically
     * by [updateConfiguration]. Each request reads [state] once, into a local
     * val, so that uri/apiKey/httpClient are always read as a consistent group.
     */
    private data class ClientState(
        val uri: String,
        val apiKey: String,
        val httpClient: HttpClient
    )

    @Volatile
    private var state: ClientState

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    /**
     * Enum representing different types of API errors
     */
    enum class ErrorType {
        CONFIGURATION_ERROR,    // 401, 403 - Authentication/authorization issues
        CONNECTION_ERROR,       // Network-related issues
        VALIDATION_ERROR,       // 422 - Validation failures
        SERVER_ERROR,           // 500, 502, 503, etc. - Server-side errors
        NOT_FOUND_ERROR,        // 404 - Resource not found
        RATE_LIMIT_ERROR,       // 429 - Too many requests
        UNKNOWN_ERROR           // Any other error
    }

    /**
     * Determines the error type based on the HTTP status code
     */
    private fun determineErrorType(statusCode: Int): ErrorType = when (statusCode) {
        401, 403 -> ErrorType.CONFIGURATION_ERROR
        404 -> ErrorType.NOT_FOUND_ERROR
        422 -> ErrorType.VALIDATION_ERROR
        429 -> ErrorType.RATE_LIMIT_ERROR
        in 500..599 -> ErrorType.SERVER_ERROR
        else -> ErrorType.UNKNOWN_ERROR
    }

    /**
     * Generates a user-friendly error message based on the status code, response body, and error type
     */
    private fun generateErrorMessage(statusCode: Int, responseBody: String, errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.CONFIGURATION_ERROR -> "Configuration error: Please check your Redmine URL and API key."
            ErrorType.CONNECTION_ERROR -> "Connection error: Unable to connect to Redmine server."
            ErrorType.VALIDATION_ERROR ->
                "Validation error: The time entry could not be saved due to validation errors: $responseBody"

            ErrorType.SERVER_ERROR -> "Server error: The Redmine server encountered an error. Please try again later."
            ErrorType.NOT_FOUND_ERROR -> "Not found error: The requested resource was not found."
            ErrorType.RATE_LIMIT_ERROR -> "Rate limit error: Too many requests. Please try again later."
            ErrorType.UNKNOWN_ERROR -> "Unknown error: An unexpected error occurred (Status code: $statusCode)."
        }
    }

    /**
     * Creates a RedmineApiException with an appropriate error message based on the HTTP status code and response body
     */
    private fun createApiException(statusCode: Int, responseBody: String): RedmineApiException {
        val errorType = determineErrorType(statusCode)
        val errorMessage = generateErrorMessage(statusCode, responseBody, errorType)
        return RedmineApiException(statusCode, responseBody, errorMessage)
    }

    /**
     * Creates a RedmineApiException for connection errors
     */
    private fun createConnectionException(e: Exception): RedmineApiException {
        return RedmineApiException(
            statusCode = 0,
            responseBody = e.message ?: "",
            message = "Connection error: Unable to connect to Redmine server."
        )
    }

    private suspend fun mapAndThrow(e: Exception, defaultMessage: String): Nothing {
        when (e) {
            is RedmineApiException -> throw e
            is ConnectException,
            is SocketTimeoutException,
            is UnknownHostException -> throw createConnectionException(e)

            is HttpRequestTimeoutException -> throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Request timeout: Unable to connect to Redmine server."
            )

            is ResponseException -> {
                val status = e.response.status.value
                val body = try {
                    e.response.bodyAsText()
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    e.message ?: ""
                }
                throw createApiException(statusCode = status, responseBody = body)
            }

            else -> throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "$defaultMessage: ${e.message}"
            )
        }
    }

    // Caches — concurrent because the API methods are called from multiple coroutines
    private val projectCache = ConcurrentHashMap<Int, Project>()

    // Cache for activities by project (inner map also concurrent — read outside cacheMutex)
    private val projectActivitiesCache = ConcurrentHashMap<Int, ConcurrentHashMap<Int, Activity>>()

    // TTL-based cache for issues by project
    private data class CachedValue<T>(val value: T, val timestampMs: Long)

    private val projectIssuesCache = ConcurrentHashMap<Int, CachedValue<List<Issue>>>()

    private val issueCache = ConcurrentHashMap<Int, Issue>()

    // Cache timing
    private val PROJECTS_TTL_MS = 10 * 60 * 1000L // 10 minutes
    private val ISSUES_TTL_MS = 5 * 60 * 1000L // 5 minutes
    private val cacheMutex = kotlinx.coroutines.sync.Mutex()

    @Volatile
    private var projectsFetchedAtMs: Long = 0L

    private fun <T> isFresh(cached: CachedValue<T>?, ttlMs: Long, now: Long = System.currentTimeMillis()): Boolean {
        return cached != null && (now - cached.timestampMs) < ttlMs
    }

    init {
        val client = httpClientOverride ?: createHttpClient(initialApiKey)
        state = ClientState(initialUri, initialApiKey, client)
    }

    private fun createHttpClient(apiKey: String): HttpClient {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // Create Ktor HttpClient
        return HttpClient(CIO) {
            // Configure timeout
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }

            // Configure default request — apiKey is captured from the parameter so
            // each client carries its own immutable key
            defaultRequest {
                header("X-Redmine-API-Key", apiKey)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }

            // Configure engine to accept self-signed certificates
            engine {
                https {
                    // Use our custom SSLContext that trusts all certificates
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                }
            }
        }
    }

    override fun close() {
        if (httpClientOverride == null) {
            try {
                state.httpClient.close()
            } catch (_: Exception) {
                // Ignore close failures
            }
        }
    }

    override fun updateConfiguration(newUri: String, newApiKey: String) {
        // Clear caches first — readers seeing the new state should not see stale data
        clearCaches()

        if (httpClientOverride == null) {
            // Build a fully-formed new client, then swap atomically. In-flight
            // requests holding a reference to the old client continue against
            // the old server until they complete, then the old client is closed.
            val old = state
            val newClient = createHttpClient(newApiKey)
            state = ClientState(newUri, newApiKey, newClient)
            try {
                old.httpClient.close()
            } catch (_: Exception) {
                // Ignore close failures on the old client
            }
        } else {
            // Test path: keep the override but update uri/apiKey
            state = state.copy(uri = newUri, apiKey = newApiKey)
        }
    }

    private fun clearCaches() {
        projectCache.clear()
        projectActivitiesCache.clear()
        projectIssuesCache.clear()
        issueCache.clear()
        cachedWeeklyHours = null
        projectsFetchedAtMs = 0L
    }

    /**
     * Custom exception class for REST API errors
     */
    class RedmineApiException(
        val statusCode: Int,
        val responseBody: String,
        message: String = "Redmine API error: $statusCode - $responseBody"
    ) : Exception(message)

    override suspend fun getUserWeeklyHours(): Float? {
        // Return cached value if available
        cachedWeeklyHours?.let { return it }
        return try {
            val account = getAndParse<RedmineAccountResponse>("/my/account.json")
            val value = account.user.customFields.firstOrNull { it.id == 27 }?.value
            val weekly = value?.toFloatOrNull()
            if (weekly != null && weekly > 0f) {
                cachedWeeklyHours = weekly
            }
            weekly
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Optional information — don't propagate, but log so the failure isn't silent
            System.err.println("Warning: Failed to load weekly hours from Redmine: ${e.message}")
            null
        }
    }

    /**
     * Helper method to make a GET request to the Redmine API
     */
    private suspend inline fun <reified T> getAndParse(endpoint: String): T = withContext(Dispatchers.IO) {
        val current = state
        try {
            val response = current.httpClient.get("${current.uri}$endpoint")

            if (!response.status.isSuccess()) {
                throw createApiException(
                    statusCode = response.status.value,
                    responseBody = response.bodyAsText()
                )
            }

            // Parse the response body using kotlinx.serialization
            val responseText = response.bodyAsText()
            json.decodeFromString<T>(responseText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapAndThrow(e, "Error retrieving data from Redmine")
        }
    }

    /**
     * Helper method to make a POST request to the Redmine API
     */
    private suspend inline fun <reified R> postAndParse(endpoint: String, body: String): R =
        withContext(Dispatchers.IO) {
            val current = state
            try {
                val response = current.httpClient.post("${current.uri}$endpoint") {
                    setBody(body)
                }

                if (!response.status.isSuccess()) {
                    throw createApiException(
                        statusCode = response.status.value,
                        responseBody = response.bodyAsText()
                    )
                }

                // Parse the response body using kotlinx.serialization
                val responseText = response.bodyAsText()
                json.decodeFromString<R>(responseText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mapAndThrow(e, "Error saving data to Redmine")
            }
        }

    /**
     * Helper method to make a PUT request to the Redmine API
     */
    private suspend inline fun <reified R> putAndParse(endpoint: String, body: String): R =
        withContext(Dispatchers.IO) {
            val current = state
            try {
                val response = current.httpClient.put("${current.uri}$endpoint") {
                    setBody(body)
                }

                if (!response.status.isSuccess()) {
                    throw createApiException(
                        statusCode = response.status.value,
                        responseBody = response.bodyAsText()
                    )
                }

                // Parse the response body using kotlinx.serialization
                val responseText = response.bodyAsText()

                // Handle empty responses when Unit is expected
                if (responseText.isEmpty() && R::class == Unit::class) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext Unit as R
                }

                json.decodeFromString<R>(responseText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mapAndThrow(e, "Error updating data in Redmine")
            }
        }

    /**
     * Helper method to make a DELETE request to the Redmine API
     */
    private suspend fun delete(endpoint: String): Unit = withContext(Dispatchers.IO) {
        val current = state
        try {
            val response = current.httpClient.delete("${current.uri}$endpoint")

            if (!response.status.isSuccess()) {
                throw createApiException(
                    statusCode = response.status.value,
                    responseBody = response.bodyAsText()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapAndThrow(e, "Error deleting data from Redmine")
        }
    }

    /**
     * Fetches all pages of projects (with activities included) using pagination
     * and parallel page fetches.
     */
    private suspend fun fetchAllProjectsWithActivities(): List<RedmineProjectWithActivities> {
        val limit = 100

        val firstPageEndpoint = "/projects.json?include=time_entry_activities&limit=${limit}&offset=0"
        val firstPage = getAndParse<RedmineProjectsWithActivitiesResponse>(firstPageEndpoint)

        val all = mutableListOf<RedmineProjectWithActivities>()
        all.addAll(firstPage.projects)

        val totalCount = firstPage.totalCount
        if (totalCount > limit) {
            val remainingOffsets = mutableListOf<Int>()
            var offset = limit
            while (offset < totalCount) {
                remainingOffsets.add(offset)
                offset += limit
            }

            val remainingResponses = coroutineScope {
                remainingOffsets.map { pageOffset ->
                    async {
                        val endpoint =
                            "/projects.json?include=time_entry_activities&limit=${limit}&offset=${pageOffset}"
                        getAndParse<RedmineProjectsWithActivitiesResponse>(endpoint)
                    }
                }.awaitAll()
            }

            remainingResponses.forEach { all.addAll(it.projects) }
        }

        return all
    }

    /**
     * Fetches issues by their IDs in batches, using Redmine's `issue_id=1,2,3` filter.
     * Returns a map keyed by issue ID. Missing IDs are not present in the result.
     */
    private suspend fun fetchIssuesByIds(issueIds: Collection<Int>): Map<Int, Issue> {
        if (issueIds.isEmpty()) return emptyMap()

        val batchSize = 100
        val batches = issueIds.toList().chunked(batchSize)

        val responses = coroutineScope {
            batches.map { batch ->
                async {
                    val ids = batch.joinToString(",")
                    // status_id=* ensures both open and closed issues are returned
                    val endpoint = "/issues.json?issue_id=${ids}&status_id=*&limit=${batchSize}"
                    getAndParse<RedmineIssuesResponse>(endpoint)
                }
            }.awaitAll()
        }

        val result = mutableMapOf<Int, Issue>()
        for (response in responses) {
            for (apiIssue in response.issues) {
                if (apiIssue.id > 0 && apiIssue.subject.isNotEmpty()) {
                    result[apiIssue.id] = Issue(apiIssue.id, apiIssue.subject)
                }
            }
        }
        return result
    }

    /**
     * Fetches all time entries for a date range using pagination.
     * Uses parallel coroutines to fetch multiple pages simultaneously for better performance.
     */
    private suspend fun fetchAllTimeEntriesWithPagination(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<RedmineTimeEntry> {
        val limit = 100 // Use a reasonable page size

        // First, fetch the first page to get the total count
        val firstPageEndpoint = "/time_entries.json?from=${startDate}&to=${endDate}&user_id=me&limit=${limit}&offset=0"
        val firstPageResponse = getAndParse<RedmineTimeEntriesResponse>(firstPageEndpoint)

        val allTimeEntries = mutableListOf<RedmineTimeEntry>()
        allTimeEntries.addAll(firstPageResponse.timeEntries)

        // If there are more pages, fetch them in parallel
        val totalCount = firstPageResponse.totalCount
        if (totalCount > limit) {
            val remainingPages = mutableListOf<Int>()
            var offset = limit
            while (offset < totalCount) {
                remainingPages.add(offset)
                offset += limit
            }

            // Fetch remaining pages in parallel using coroutines
            val remainingResponses = coroutineScope {
                remainingPages.map { pageOffset ->
                    async {
                        val endpoint =
                            "/time_entries.json?from=${startDate}&to=${endDate}&user_id=me&limit=${limit}&offset=${pageOffset}"
                        getAndParse<RedmineTimeEntriesResponse>(endpoint)
                    }
                }.awaitAll()
            }

            // Add all entries from remaining pages
            remainingResponses.forEach { response ->
                allTimeEntries.addAll(response.timeEntries)
            }
        }

        return allTimeEntries
    }

    override suspend fun getTimeEntriesForMonth(year: Int, month: Int): List<AppTimeEntry> =
        withContext(Dispatchers.IO) {
            // Calculate date range for the month
            val startDate = LocalDate(year, month, 1)
            val endDate = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

            try {
                // Fetch all pages of time entries using pagination
                val allTimeEntries = fetchAllTimeEntriesWithPagination(startDate, endDate)

                // Pre-load activities and projects
                getProjectsWithActivities()

                // Collect unique issue IDs that aren't already cached
                val missingIssueIds = allTimeEntries
                    .mapNotNull { it.issue.id.takeIf { id -> id > 0 } }
                    .distinct()
                    .filter { !issueCache.containsKey(it) }

                if (missingIssueIds.isNotEmpty()) {
                    // Batch-fetch them in parallel chunks rather than one GET per issue
                    val fetched = try {
                        fetchIssuesByIds(missingIssueIds)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    for (issueId in missingIssueIds) {
                        val issue = fetched[issueId]
                        issueCache[issueId] = issue ?: Issue(issueId, "Unknown Issue")
                    }
                }

                // Convert and return the time entries
                allTimeEntries.mapNotNull { timeEntry ->
                    try {
                        timeEntry.toDomainModel(issueCache)
                    } catch (e: Exception) {
                        // Log the error but continue processing other time entries
                        // This allows us to return partial results even if some entries fail to convert
                        System.err.println("Warning: Skipping malformed time entry due to conversion error: ${e.message}")
                        null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Rethrow RedmineApiException as it already has a descriptive message
                if (e is RedmineApiException) {
                    throw e
                }

                // For other exceptions, create a more descriptive error
                throw RedmineApiException(
                    statusCode = 0,
                    responseBody = e.message ?: "",
                    message = "Error fetching time entries for ${year}-${month}: ${e.message}"
                )
            }
        }

    override suspend fun createTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry = withContext(Dispatchers.IO) {
        try {
            // Build the request payload using the typed envelope
            val requestJson = json.encodeToString(
                RedmineTimeEntryRequestEnvelope.serializer(),
                RedmineTimeEntryRequestEnvelope(timeEntry.toApiRequest())
            )

            // Make the API request
            val response = postAndParse<RedmineTimeEntryResponse>("/time_entries.json", requestJson)

            // Ensure the issue is cached if it has a valid ID
            val timeEntryResponse = response.timeEntry
            if (timeEntryResponse.issue.id > 0 && !issueCache.containsKey(timeEntryResponse.issue.id)) {
                try {
                    val issueEndpoint = "/issues/${timeEntryResponse.issue.id}.json"
                    val issueResponse = getAndParse<RedmineIssueResponse>(issueEndpoint)
                    val issue = issueResponse.issue
                    issueCache[issue.id] = Issue(issue.id, issue.subject)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    issueCache[timeEntryResponse.issue.id] = Issue(timeEntryResponse.issue.id, "Unknown Issue")
                }
            }

            // Convert and return the time entry
            timeEntryResponse.toDomainModel(issueCache)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Rethrow RedmineApiException as it already has a descriptive message
            if (e is RedmineApiException) {
                throw e
            }

            // For other exceptions, create a more descriptive error
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error creating time entry: ${e.message}"
            )
        }
    }

    override suspend fun updateTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry = withContext(Dispatchers.IO) {
        require(timeEntry.id != null) { "Time entry ID cannot be null for update operation" }

        try {
            // Build the request payload using the typed envelope
            val requestJson = json.encodeToString(
                RedmineTimeEntryRequestEnvelope.serializer(),
                RedmineTimeEntryRequestEnvelope(timeEntry.toApiRequest())
            )

            // Redmine's PUT /time_entries/{id}.json returns 200 with empty body, so we
            // can't decode the updated resource from the response. The input timeEntry
            // is what was sent and accepted, so we return it as-is rather than paying
            // for a follow-up GET round-trip.
            val endpoint = "/time_entries/${timeEntry.id}.json"
            putAndParse<Unit>(endpoint, requestJson)

            // Make sure the issue is cached for downstream consumers
            if (timeEntry.issue.id > 0) {
                issueCache.putIfAbsent(timeEntry.issue.id, timeEntry.issue)
            }

            timeEntry
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Rethrow RedmineApiException as it already has a descriptive message
            if (e is RedmineApiException) {
                throw e
            }

            // For other exceptions, create a more descriptive error
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error updating time entry (ID: ${timeEntry.id}): ${e.message}"
            )
        }
    }

    override suspend fun deleteTimeEntry(timeEntryId: Int) = withContext(Dispatchers.IO) {
        try {
            // Make the API request
            delete("/time_entries/$timeEntryId.json")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Rethrow RedmineApiException as it already has a descriptive message
            if (e is RedmineApiException) {
                throw e
            }

            // For other exceptions, create a more descriptive error
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error deleting time entry (ID: $timeEntryId): ${e.message}"
            )
        }
    }

    override suspend fun getActivities(): List<Activity> = withContext(Dispatchers.IO) {
        // This method is now deprecated. It returns an empty list as per the interface documentation.
        // Projects should use getActivitiesForProject(projectId) instead.
        emptyList()
    }

    override suspend fun getActivitiesForProject(projectId: Int): List<Activity> = withContext(Dispatchers.IO) {
        // Return cached activities for this project if available
        projectActivitiesCache[projectId]?.values?.toList()?.let { return@withContext it }

        // If activities for this project aren't cached yet, call getProjectsWithActivities() 
        // which will load both projects and activities in a single API call
        getProjectsWithActivities()

        // Now activities should be cached, return them for this project
        projectActivitiesCache[projectId]?.values?.toList() ?: emptyList()
    }

    override suspend fun getProjectsWithActivities(): List<Project> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (projectCache.isNotEmpty() && projectActivitiesCache.isNotEmpty() &&
            (now - projectsFetchedAtMs) < PROJECTS_TTL_MS
        ) {
            return@withContext projectCache.values.toList()
        }

        cacheMutex.lock()
        try {
            // Re-check inside lock to avoid duplicate reloads
            val nowLocked = System.currentTimeMillis()
            if (projectCache.isNotEmpty() && projectActivitiesCache.isNotEmpty() &&
                (nowLocked - projectsFetchedAtMs) < PROJECTS_TTL_MS
            ) {
                return@withContext projectCache.values.toList()
            }

            // Fetch all pages of projects in parallel
            val allApiProjects = fetchAllProjectsWithActivities()

            // Process all projects and their activities
            projectCache.clear()
            projectActivitiesCache.clear()
            val parsedProjects = mutableListOf<Project>()

            for (projectWithActivities in allApiProjects) {
                val id = projectWithActivities.id
                val name = projectWithActivities.name
                if (id <= 0 || name.isEmpty()) continue

                val project = Project(id, name)
                projectCache[id] = project
                parsedProjects.add(project)

                val timeEntryActivities = projectWithActivities.timeEntryActivities
                if (timeEntryActivities.isNotEmpty()) {
                    val projectActivities = projectActivitiesCache.getOrPut(id) { ConcurrentHashMap() }
                    for (activityApi in timeEntryActivities) {
                        val activityId = activityApi.id
                        val activityName = activityApi.name
                        if (activityId <= 0 || activityName.isEmpty()) continue
                        projectActivities[activityId] = Activity(activityId, activityName)
                    }
                }
            }

            projectsFetchedAtMs = System.currentTimeMillis()
            parsedProjects
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is RedmineApiException) throw e
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error fetching projects and activities: ${e.message}"
            )
        } finally {
            cacheMutex.unlock()
        }
    }

    override suspend fun getProjectsWithOpenIssues(): List<Project> = withContext(Dispatchers.IO) {
        try {
            val allProjects = getProjectsWithActivities()
            val now = System.currentTimeMillis()

            // Limit concurrency to avoid overwhelming the server
            val semaphore = kotlinx.coroutines.sync.Semaphore(permits = 4)
            coroutineScope {
                allProjects.map { project ->
                    async {
                        val cached = projectIssuesCache[project.id]
                        if (isFresh(cached, ISSUES_TTL_MS, now)) return@async

                        semaphore.acquire()
                        try {
                            val endpoint =
                                "/issues.json?project_id=${project.id}&status_id=open&limit=100&sort=updated_on:desc"
                            val response = getAndParse<RedmineIssuesResponse>(endpoint)
                            val issues = response.issues
                                .filter { it.id > 0 && it.subject.isNotEmpty() }
                                .map { Issue(it.id, it.subject) }
                            projectIssuesCache[project.id] = CachedValue(issues, System.currentTimeMillis())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            projectIssuesCache[project.id] = CachedValue(emptyList(), System.currentTimeMillis())
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll()
            }

            allProjects.filter { project ->
                projectIssuesCache[project.id]?.value?.isNotEmpty() == true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is RedmineApiException) throw e
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error fetching projects with open issues: ${e.message}"
            )
        }
    }

    override suspend fun getIssues(projectId: Int): List<Issue> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = projectIssuesCache[projectId]
        if (isFresh(cached, ISSUES_TTL_MS, now)) {
            return@withContext cached!!.value
        }
        return@withContext try {
            val endpoint = "/issues.json?project_id=${projectId}&status_id=open&limit=100&sort=updated_on:desc"
            val response = getAndParse<RedmineIssuesResponse>(endpoint)
            val issues = response.issues
                .filter { it.id > 0 && it.subject.isNotEmpty() }
                .map { Issue(it.id, it.subject) }
            projectIssuesCache[projectId] = CachedValue(issues, System.currentTimeMillis())
            issues
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            val empty = emptyList<Issue>()
            projectIssuesCache[projectId] = CachedValue(empty, System.currentTimeMillis())
            empty
        }
    }
}
