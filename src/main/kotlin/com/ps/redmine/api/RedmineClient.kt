package com.ps.redmine.api

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.util.toJava
import com.ps.redmine.util.toKotlin
import com.taskadapter.redmineapi.RedmineManagerFactory
import com.taskadapter.redmineapi.bean.TimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import java.time.ZoneId
import java.util.*
import com.ps.redmine.model.TimeEntry as AppTimeEntry

class RedmineClient(
    uri: String,
    username: String,
    password: String
) {
    // Cache for activities and projects
    private var cachedActivities: List<Activity>? = null
    private var cachedProjects: Map<Int, Project>? = null
    private val projectCache = mutableMapOf<Int, Project>()
    private val activityCache = mutableMapOf<Int, Activity>()
    private val issueCache = mutableMapOf<Int, Issue>()
    private val redmineManager = RedmineManagerFactory.createWithUserAuth(
        uri,
        username,
        password,
        HttpClients.custom()
            .setSSLContext(SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy()).build())
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .build()
    )

    suspend fun getTimeEntriesForMonth(year: Int, month: Int): List<AppTimeEntry> = withContext(Dispatchers.IO) {
        println("[DEBUG_LOG] Loading time entries for $year-$month")
        val startTime = System.currentTimeMillis()

        val startDate = LocalDate(year, month, 1)
        val endDate = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        val params = mapOf(
            "from" to startDate.toString(),
            "to" to endDate.toString(),
            "user_id" to "me"
        )

        // Pre-load activities and projects
        getActivities()
        getProjects()

        val entries = redmineManager.timeEntryManager.getTimeEntries(params)

        // Collect unique issue IDs and project IDs
        val uniqueIssueIds = entries.results.orEmpty().map { it.issueId }.distinct()
        entries.results.orEmpty().map { it.projectId }.distinct()

        // Batch load issues
        println("[DEBUG_LOG] Batch loading ${uniqueIssueIds.size} issues")
        uniqueIssueIds.forEach { issueId ->
            if (!issueCache.containsKey(issueId)) {
                try {
                    val issue = redmineManager.issueManager.getIssueById(issueId)
                    issueCache[issueId] = Issue(issue.id, issue.subject)
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Failed to load issue #$issueId: ${e.message}")
                    issueCache[issueId] = Issue(issueId, "Unknown Issue")
                }
            }
        }

        val result = entries.results.orEmpty().map { entry -> convertToAppTimeEntry(entry) }

        val endTime = System.currentTimeMillis()
        println("[DEBUG_LOG] Loaded ${result.size} time entries in ${endTime - startTime}ms")

        result
    }

    suspend fun createTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry = withContext(Dispatchers.IO) {
        val redmineTimeEntry = com.taskadapter.redmineapi.bean.TimeEntryFactory.create(timeEntry.project.id).apply {
            issueId = timeEntry.issue.id
        }

        val javaDate = Date.from(timeEntry.date.toJava().atStartOfDay(ZoneId.systemDefault()).toInstant())

        redmineTimeEntry.activityId = timeEntry.activity.id
        redmineTimeEntry.spentOn = javaDate
        redmineTimeEntry.hours = timeEntry.hours
        redmineTimeEntry.comment = timeEntry.comments

        convertToAppTimeEntry(redmineManager.timeEntryManager.createTimeEntry(redmineTimeEntry))
    }

    suspend fun updateTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry = withContext(Dispatchers.IO) {
        require(timeEntry.id != null) { "Time entry ID cannot be null for update operation" }
        println("[DEBUG_LOG] Updating time entry #${timeEntry.id}")

        try {
            val redmineTimeEntry = redmineManager.timeEntryManager.getTimeEntry(timeEntry.id)
            val javaDate = Date.from(timeEntry.date.toJava().atStartOfDay(ZoneId.systemDefault()).toInstant())

            redmineTimeEntry.spentOn = javaDate
            redmineTimeEntry.hours = timeEntry.hours
            redmineTimeEntry.activityId = timeEntry.activity.id
            redmineTimeEntry.projectId = timeEntry.project.id
            redmineTimeEntry.issueId = timeEntry.issue.id
            redmineTimeEntry.comment = timeEntry.comments

            println("[DEBUG_LOG] Updating time entry with new values")
            redmineManager.timeEntryManager.update(redmineTimeEntry)
            println("[DEBUG_LOG] Time entry updated successfully")

            // Return the updated entry directly instead of fetching it again
            timeEntry
        } catch (e: Exception) {
            println("[DEBUG_LOG] Error updating time entry: ${e.message}")
            timeEntry  // Return original entry if update fails
        }
    }

    suspend fun deleteTimeEntry(timeEntryId: Int) = withContext(Dispatchers.IO) {
        println("[DEBUG_LOG] Deleting time entry #$timeEntryId")
        try {
            redmineManager.timeEntryManager.deleteTimeEntry(timeEntryId)
            println("[DEBUG_LOG] Time entry deleted successfully")
        } catch (e: Exception) {
            println("[DEBUG_LOG] Error deleting time entry: ${e.message}")
            // The entry might still be deleted even if we get an error
            println("[DEBUG_LOG] Note: The entry might have been deleted despite the error")
        }
    }

    suspend fun getActivities(): List<Activity> = withContext(Dispatchers.IO) {
        cachedActivities?.let { return@withContext it }

        val activities = redmineManager.timeEntryManager.timeEntryActivities.map { activity ->
            Activity(activity.id, activity.name).also {
                activityCache[activity.id] = it
            }
        }
        cachedActivities = activities
        activities
    }

    private suspend fun getActivity(activityId: Int): Activity = withContext(Dispatchers.IO) {
        activityCache[activityId]?.let { return@withContext it }

        getActivities() // This will populate the cache
        activityCache[activityId] ?: Activity(activityId, "Unknown Activity")
    }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        cachedProjects?.values?.toList()?.let { return@withContext it }

        val projects = redmineManager.projectManager.projects.map { project ->
            Project(project.id, project.name).also {
                projectCache[project.id] = it
            }
        }
        cachedProjects = projectCache.toMap()
        projects
    }


    suspend fun getIssues(projectId: Int): List<Issue> = withContext(Dispatchers.IO) {
        println("[DEBUG_LOG] Fetching issues for project $projectId...")
        val params = mapOf(
            "project_id" to projectId.toString(),
            "status_id" to "open",
            "limit" to "100",
            "sort" to "updated_on:desc"
        )

        println("[DEBUG_LOG] Query parameters: $params")

        try {
            val issues = redmineManager.issueManager.getIssues(params)
            println("[DEBUG_LOG] Fetched ${issues.results.size} issues from Redmine")
            issues.results.orEmpty().map { issue ->
                println("[DEBUG_LOG] Processing issue #${issue.id}: ${issue.subject} (Project: ${issue.projectId})")
                Issue(issue.id, issue.subject)
            }.also {
                println("[DEBUG_LOG] Returning ${it.size} issues")
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] Error fetching issues: ${e.message}")
            emptyList()
        }
    }

    private fun convertToAppTimeEntry(entry: TimeEntry): AppTimeEntry {
        val date = entry.spentOn.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toKotlin()

        // Use cached data
        val project = projectCache[entry.projectId]
            ?: Project(entry.projectId, "Unknown Project").also { projectCache[entry.projectId] = it }

        val issue = issueCache[entry.issueId]
            ?: Issue(entry.issueId, "Unknown Issue").also { issueCache[entry.issueId] = it }

        val activity = activityCache[entry.activityId]
            ?: Activity(entry.activityId, "Unknown Activity").also { activityCache[entry.activityId] = it }

        return AppTimeEntry(
            id = entry.id,
            date = date,
            hours = entry.hours,
            activity = activity,
            project = project,
            issue = issue,
            comments = entry.comment ?: ""
        )
    }
}
