package com.ps.redmine.api

import com.taskadapter.redmineapi.RedmineManagerFactory
import com.taskadapter.redmineapi.bean.TimeEntry
import com.taskadapter.redmineapi.bean.TimeEntryActivity
import com.taskadapter.redmineapi.bean.Project as RedmineProject
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry as AppTimeEntry
import com.ps.redmine.util.toJava
import com.ps.redmine.util.toKotlin
import java.time.ZoneId
import java.util.*

class RedmineClient(
    private val uri: String,
    private val username: String,
    private val password: String
) {
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
        val startDate = LocalDate(year, month, 1)
        val endDate = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        val params = mapOf(
            "from" to startDate.toString(),
            "to" to endDate.toString(),
            "user_id" to "me"
        )

        val entries = redmineManager.timeEntryManager.getTimeEntries(params)
        entries.results.orEmpty().map { entry -> convertToAppTimeEntry(entry) }
    }

    suspend fun createTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry = withContext(Dispatchers.IO) {
        val redmineTimeEntry = com.taskadapter.redmineapi.bean.TimeEntryFactory.create(timeEntry.project.id).apply {
            issueId = timeEntry.issue.id
        }

        val javaDate = Date.from(timeEntry.date.toJava().atStartOfDay(ZoneId.of("UTC")).toInstant())

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
            val javaDate = Date.from(timeEntry.date.toJava().atStartOfDay(ZoneId.of("UTC")).toInstant())

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
            e.printStackTrace()
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
            e.printStackTrace()
            // The entry might still be deleted even if we get an error
            println("[DEBUG_LOG] Note: The entry might have been deleted despite the error")
        }
    }

    suspend fun getActivities(): List<Activity> = withContext(Dispatchers.IO) {
        val activities = redmineManager.timeEntryManager.timeEntryActivities
        activities.map { activity -> Activity(activity.id, activity.name) }
    }

    private suspend fun getActivity(activityId: Int): Activity = withContext(Dispatchers.IO) {
        val activities = redmineManager.timeEntryManager.timeEntryActivities
        activities.find { it.id == activityId }?.let { activity ->
            Activity(activity.id, activity.name)
        } ?: Activity(activityId, "Unknown Activity")
    }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        val projects = redmineManager.projectManager.projects
        projects.map { project ->  Project(project.id, project.name) }
    }


    private suspend fun getIssue(issueId: Int): Issue = withContext(Dispatchers.IO) {
        val issue = redmineManager.issueManager.getIssueById(issueId)
        Issue(issue.id, issue.subject)
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
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun convertToAppTimeEntry(entry: TimeEntry): AppTimeEntry {
        val date = entry.spentOn.toInstant()
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
            .toKotlin()

        val project = Project(entry.projectId, redmineManager.projectManager.getProjectById(entry.projectId).name)
        val issue = getIssue(entry.issueId)

        return AppTimeEntry(
            id = entry.id,
            date = date,
            hours = entry.hours,
            activity = getActivity(entry.activityId),
            project = project,
            issue = issue,
            comments = entry.comment ?: ""
        )
    }
}
