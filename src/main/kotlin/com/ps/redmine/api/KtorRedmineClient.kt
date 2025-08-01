package com.ps.redmine.api

import com.ps.redmine.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.ps.redmine.model.TimeEntry as AppTimeEntry

/**
 * Implementation of RedmineClientInterface that uses Ktor Client for HTTP requests.
 * This is a more Kotlin-idiomatic approach compared to the Java HttpClient used in DirectRedmineClient.
 */
class KtorRedmineClient(
    private var uri: String,
    private var apiKey: String
) : RedmineClientInterface {

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

    // Cache for projects
    private var cachedProjects: Map<Int, Project>? = null
    private val projectCache = mutableMapOf<Int, Project>()

    // Cache for activities by project
    private val projectActivitiesCache = mutableMapOf<Int, MutableMap<Int, Activity>>()

    // Cache for issues by project
    private val projectIssuesCache = mutableMapOf<Int, List<Issue>>()

    private val issueCache = mutableMapOf<Int, Issue>()
    private var httpClient: HttpClient? = null

    init {
        createHttpClient()
    }

    private fun createHttpClient() {
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
        httpClient = HttpClient(CIO) {
            // Configure timeout
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }

            // Configure default request
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
        httpClient?.close()
        httpClient = null
    }

    override fun updateConfiguration(newUri: String, newApiKey: String) {
        uri = newUri
        apiKey = newApiKey

        // Clear caches
        cachedProjects = null
        projectCache.clear()
        projectActivitiesCache.clear()
        projectIssuesCache.clear()
        issueCache.clear()

        // Recreate the HTTP client
        close()
        createHttpClient()
    }

    /**
     * Custom exception class for REST API errors
     */
    class RedmineApiException(
        val statusCode: Int,
        val responseBody: String,
        message: String = "Redmine API error: $statusCode - $responseBody"
    ) : Exception(message)

    /**
     * Helper method to make a GET request to the Redmine API
     */
    private suspend inline fun <reified T> getAndParse(endpoint: String): T = withContext(Dispatchers.IO) {
        try {
            val response = httpClient?.get("$uri$endpoint")
                ?: throw IOException("HTTP client not initialized")

            if (!response.status.isSuccess()) {
                throw createApiException(
                    statusCode = response.status.value,
                    responseBody = response.bodyAsText()
                )
            }

            // Parse the response body using kotlinx.serialization
            val responseText = response.bodyAsText()
            json.decodeFromString<T>(responseText)
        } catch (e: Exception) {
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

                else -> {
                    throw RedmineApiException(
                        statusCode = 0,
                        responseBody = e.message ?: "",
                        message = "Error retrieving data from Redmine: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Helper method to make a POST request to the Redmine API
     */
    private suspend inline fun <reified R> postAndParse(endpoint: String, body: String): R =
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient?.post("$uri$endpoint") {
                    setBody(body)
                } ?: throw IOException("HTTP client not initialized")

                if (!response.status.isSuccess()) {
                    throw createApiException(
                        statusCode = response.status.value,
                        responseBody = response.bodyAsText()
                    )
                }

                // Parse the response body using kotlinx.serialization
                val responseText = response.bodyAsText()
                json.decodeFromString<R>(responseText)
            } catch (e: Exception) {
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

                    else -> {
                        throw RedmineApiException(
                            statusCode = 0,
                            responseBody = e.message ?: "",
                            message = "Error saving data to Redmine: ${e.message}"
                        )
                    }
                }
            }
        }

    /**
     * Helper method to make a PUT request to the Redmine API
     */
    private suspend inline fun <reified R> putAndParse(endpoint: String, body: String): R =
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient?.put("$uri$endpoint") {
                    setBody(body)
                } ?: throw IOException("HTTP client not initialized")

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
            } catch (e: Exception) {
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

                    else -> {
                        throw RedmineApiException(
                            statusCode = 0,
                            responseBody = e.message ?: "",
                            message = "Error updating data in Redmine: ${e.message}"
                        )
                    }
                }
            }
        }

    /**
     * Helper method to make a DELETE request to the Redmine API
     */
    private suspend fun delete(endpoint: String): Unit = withContext(Dispatchers.IO) {
        try {
            val response = httpClient?.delete("$uri$endpoint")
                ?: throw IOException("HTTP client not initialized")

            if (!response.status.isSuccess()) {
                throw createApiException(
                    statusCode = response.status.value,
                    responseBody = response.bodyAsText()
                )
            }
        } catch (e: Exception) {
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

                else -> {
                    throw RedmineApiException(
                        statusCode = 0,
                        responseBody = e.message ?: "",
                        message = "Error deleting data from Redmine: ${e.message}"
                    )
                }
            }
        }
    }

    override suspend fun getTimeEntriesForMonth(year: Int, month: Int): List<AppTimeEntry> =
        withContext(Dispatchers.IO) {
            // Calculate date range for the month
            val startDate = LocalDate(year, month, 1)
            val endDate = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

            // Create endpoint with query parameters
            val endpoint = "/time_entries.json?from=${startDate}&to=${endDate}&user_id=me"

            try {
                // Make the API request and parse the response
                val response = getAndParse<RedmineTimeEntriesResponse>(endpoint)

                // Pre-load activities and projects
                getProjectsWithActivities()

                // Collect unique issue IDs
                val uniqueIssueIds = response.timeEntries
                    .mapNotNull { it.issue.id.takeIf { id -> id > 0 } }
                    .distinct()

                // Batch load issues
                for (issueId in uniqueIssueIds) {
                    if (!issueCache.containsKey(issueId)) {
                        try {
                            val issueEndpoint = "/issues/$issueId.json"
                            val issueResponse = getAndParse<RedmineIssueResponse>(issueEndpoint)
                            val issue = issueResponse.issue

                            if (issue.id > 0) {
                                issueCache[issue.id] = Issue(issue.id, issue.subject)
                            } else {
                                issueCache[issueId] = Issue(issueId, "Unknown Issue")
                            }
                        } catch (_: Exception) {
                            issueCache[issueId] = Issue(issueId, "Unknown Issue")
                        }
                    }
                }

                // Convert and return the time entries
                response.timeEntries.mapNotNull { timeEntry ->
                    try {
                        timeEntry.toDomainModel(issueCache)
                    } catch (e: Exception) {
                        // Log the error but continue processing other time entries
                        // This allows us to return partial results even if some entries fail to convert
                        System.err.println("Warning: Skipping malformed time entry due to conversion error: ${e.message}")
                        null
                    }
                }
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
            // Convert to API request model with the correct field names for the API
            val apiRequest = timeEntry.toApiRequest()

            // Create the request payload
            val timeEntryJson = json.encodeToString(RedmineTimeEntryRequest.serializer(), apiRequest)

            val requestJson = """{"time_entry": $timeEntryJson}"""

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
                } catch (_: Exception) {
                    issueCache[timeEntryResponse.issue.id] = Issue(timeEntryResponse.issue.id, "Unknown Issue")
                }
            }

            // Convert and return the time entry
            timeEntryResponse.toDomainModel(issueCache)
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
            // Convert to API request model with the correct field names for the API
            val apiRequest = timeEntry.toApiRequest()

            // Create the request payload
            val timeEntryJson = json.encodeToString(RedmineTimeEntryRequest.serializer(), apiRequest)

            val requestJson = """{"time_entry": $timeEntryJson}"""

            // Make the API request
            val endpoint = "/time_entries/${timeEntry.id}.json"
            putAndParse<Unit>(endpoint, requestJson)

            // Get the updated time entry
            val getResponse = getAndParse<RedmineTimeEntryResponse>(endpoint)

            // Ensure the issue is cached if it has a valid ID
            val timeEntryResponse = getResponse.timeEntry
            if (timeEntryResponse.issue.id > 0 && !issueCache.containsKey(timeEntryResponse.issue.id)) {
                try {
                    val issueEndpoint = "/issues/${timeEntryResponse.issue.id}.json"
                    val issueResponse = getAndParse<RedmineIssueResponse>(issueEndpoint)
                    val issue = issueResponse.issue
                    issueCache[issue.id] = Issue(issue.id, issue.subject)
                } catch (e: Exception) {
                    issueCache[timeEntryResponse.issue.id] = Issue(timeEntryResponse.issue.id, "Unknown Issue")
                }
            }

            // Convert and return the time entry
            timeEntryResponse.toDomainModel(issueCache)
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
        // Return cached projects if available
        cachedProjects?.values?.toList()?.let {
            // If we have at least some project activities cached, return the projects
            if (projectActivitiesCache.isNotEmpty()) {
                return@withContext it
            }
        }

        try {
            // Get projects with time entry activities included
            val response =
                getAndParse<RedmineProjectsWithActivitiesResponse>("/projects.json?include=time_entry_activities&limit=100")

            // Process all projects and their activities
            val parsedProjects = mutableListOf<Project>()

            for (projectWithActivities in response.projects) {
                val id = projectWithActivities.id
                val name = projectWithActivities.name

                if (id <= 0 || name.isEmpty()) continue

                val project = Project(id, name)
                projectCache[id] = project
                parsedProjects.add(project)

                // Extract time entry activities for this project
                val timeEntryActivities = projectWithActivities.timeEntryActivities
                if (timeEntryActivities.isNotEmpty()) {
                    // Create a map for this project's activities if it doesn't exist
                    val projectActivities = projectActivitiesCache.getOrPut(id) { mutableMapOf() }

                    for (activityApi in timeEntryActivities) {
                        val activityId = activityApi.id
                        val activityName = activityApi.name

                        if (activityId <= 0 || activityName.isEmpty()) continue

                        // Add to project-specific activity cache
                        val activity = Activity(activityId, activityName)
                        projectActivities[activityId] = activity

                    }
                }
            }

            // Cache the projects
            cachedProjects = projectCache.toMap()

            parsedProjects
        } catch (e: Exception) {
            // Rethrow RedmineApiException as it already has a descriptive message
            if (e is RedmineApiException) {
                throw e
            }

            // For other exceptions, create a more descriptive error
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error fetching projects and activities: ${e.message}"
            )
        }
    }

    override suspend fun getProjectsWithOpenIssues(): List<Project> = withContext(Dispatchers.IO) {
        try {
            // First, get all projects with activities
            val allProjects = getProjectsWithActivities()

            // Load issues for all projects upfront and cache them
            for (project in allProjects) {
                try {
                    // Only load issues if not already cached
                    if (!projectIssuesCache.containsKey(project.id)) {
                        // Create endpoint with query parameters
                        val endpoint =
                            "/issues.json?project_id=${project.id}&status_id=open&limit=100&sort=updated_on:desc"

                        // Make the API request
                        val response = getAndParse<RedmineIssuesResponse>(endpoint)

                        // Convert the issues
                        val issues = response.issues
                            .filter { it.id > 0 && it.subject.isNotEmpty() }
                            .map { Issue(it.id, it.subject) }

                        // Cache the issues for this project
                        projectIssuesCache[project.id] = issues
                    }
                } catch (_: Exception) {
                    // If we can't get issues for a project, cache an empty list
                    // This handles cases where the user might not have permission to view issues
                    projectIssuesCache[project.id] = emptyList()
                }
            }

            // Filter projects that have open issues (now all issues are cached)
            val projectsWithOpenIssues = allProjects.filter { project ->
                projectIssuesCache[project.id]?.isNotEmpty() == true
            }

            projectsWithOpenIssues
        } catch (e: Exception) {
            // Rethrow RedmineApiException as it already has a descriptive message
            if (e is RedmineApiException) {
                throw e
            }

            // For other exceptions, create a more descriptive error
            throw RedmineApiException(
                statusCode = 0,
                responseBody = e.message ?: "",
                message = "Error fetching projects with open issues: ${e.message}"
            )
        }
    }

    override suspend fun getIssues(projectId: Int): List<Issue> = withContext(Dispatchers.IO) {
        // Return cached issues for this project
        // Issues should already be cached by getProjectsWithOpenIssues()
        projectIssuesCache[projectId] ?: emptyList()
    }
}
