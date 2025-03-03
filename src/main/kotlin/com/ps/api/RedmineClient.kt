package com.ps.api

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
import com.ps.model.Activity
import com.ps.model.Project
import com.ps.model.TimeEntry as AppTimeEntry
import com.ps.util.toJava
import com.ps.util.toKotlin
import com.taskadapter.redmineapi.internal.ResultsWrapper
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
        val redmineTimeEntry = redmineManager.timeEntryManager.getTimeEntry(0)
        val javaDate = Date.from(timeEntry.date.toJava().atStartOfDay(ZoneId.of("UTC")).toInstant())

        redmineTimeEntry.projectId = timeEntry.project.id
        redmineTimeEntry.activityId = timeEntry.activity.id
        redmineTimeEntry.spentOn = javaDate
        redmineTimeEntry.hours = timeEntry.hours
        redmineTimeEntry.comment = timeEntry.comments

        convertToAppTimeEntry(redmineManager.timeEntryManager.createTimeEntry(redmineTimeEntry))
    }

    suspend fun updateTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry = withContext(Dispatchers.IO) {
        require(timeEntry.id != null) { "Time entry ID cannot be null for update operation" }

        val redmineTimeEntry = redmineManager.timeEntryManager.getTimeEntry(timeEntry.id)
        val javaDate = Date.from(timeEntry.date.toJava().atStartOfDay(ZoneId.of("UTC")).toInstant())

        redmineTimeEntry.spentOn = javaDate
        redmineTimeEntry.hours = timeEntry.hours
        redmineTimeEntry.activityId = timeEntry.activity.id
        redmineTimeEntry.projectId = timeEntry.project.id
        redmineTimeEntry.comment = timeEntry.comments

        redmineManager.timeEntryManager.update(redmineTimeEntry)
        convertToAppTimeEntry(redmineManager.timeEntryManager.getTimeEntry(timeEntry.id))
    }

    suspend fun getActivities(): List<Activity> = withContext(Dispatchers.IO) {
        val activities = redmineManager.timeEntryManager.timeEntryActivities
        (activities as? List<*>)?.filterIsInstance<TimeEntryActivity>()?.map { activity -> 
            Activity(activity.id, activity.name)
        } ?: emptyList()
    }

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        val projects = redmineManager.projectManager.projects
        (projects as? List<*>)?.filterIsInstance<RedmineProject>()?.map { project -> 
            Project(project.id, project.name)
        } ?: emptyList()
    }

    private fun convertToAppTimeEntry(entry: TimeEntry): AppTimeEntry {
        val date = entry.spentOn.toInstant()
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
            .toKotlin()

        return AppTimeEntry(
            id = entry.id,
            date = date,
            hours = entry.hours,
            activity = Activity(entry.activityId, ""),  // Activity name will be fetched separately if needed
            project = Project(entry.projectId, ""),     // Project name will be fetched separately if needed
            comments = entry.comment ?: ""
        )
    }
}
