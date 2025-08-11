package com.ps.redmine.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable data classes for Redmine API responses
 */

@Serializable
data class RedmineTimeEntriesResponse(
    @SerialName("time_entries") val timeEntries: List<RedmineTimeEntry> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0,
    val offset: Int = 0,
    val limit: Int = 25
)

@Serializable
data class RedmineTimeEntryResponse(
    @SerialName("time_entry") val timeEntry: RedmineTimeEntry
)

@Serializable
data class RedmineTimeEntry(
    val id: Int? = null,
    @SerialName("spent_on") val spentOn: String = "",
    val hours: Float = 0f,
    val activity: RedmineActivity = RedmineActivity(),
    val project: RedmineProject = RedmineProject(),
    val issue: RedmineIssue = RedmineIssue(),
    val comments: String? = null
)

@Serializable
data class RedmineActivity(
    val id: Int = -1,
    val name: String = ""
)

@Serializable
data class RedmineProject(
    val id: Int = -1,
    val name: String = ""
)

@Serializable
data class RedmineIssue(
    val id: Int = -1,
    val subject: String = ""
)

@Serializable
data class RedmineProjectsResponse(
    val projects: List<RedmineProject> = emptyList()
)

@Serializable
data class RedmineIssuesResponse(
    val issues: List<RedmineIssue> = emptyList()
)

@Serializable
data class RedmineIssueResponse(
    val issue: RedmineIssue
)

@Serializable
data class RedmineProjectWithActivities(
    val id: Int = -1,
    val name: String = "",
    @SerialName("time_entry_activities") val timeEntryActivities: List<RedmineActivity> = emptyList()
)

@Serializable
data class RedmineProjectsWithActivitiesResponse(
    val projects: List<RedmineProjectWithActivities> = emptyList()
)

/**
 * Extension functions to convert between API models and domain models
 */

fun RedmineTimeEntry.toDomainModel(issueCache: Map<Int, Issue> = emptyMap()): TimeEntry {
    // Parse the date from "YYYY-MM-DD" format
    val dateParts = spentOn.split("-").map { it.toInt() }
    val date = if (dateParts.size == 3) {
        LocalDate(dateParts[0], dateParts[1], dateParts[2])
    } else {
        LocalDate(1970, 1, 1) // Fallback date
    }

    // Use cached issue if available, otherwise fall back to the API response issue
    val domainIssue = if (issue.id > 0 && issueCache.containsKey(issue.id)) {
        issueCache[issue.id]!!
    } else {
        issue.toDomainModel()
    }

    return TimeEntry(
        id = id,
        date = date,
        hours = hours,
        activity = activity.toDomainModel(),
        project = project.toDomainModel(),
        issue = domainIssue,
        comments = comments
    )
}

fun RedmineActivity.toDomainModel(): Activity = Activity(id, name)
fun RedmineProject.toDomainModel(): Project = Project(id, name)
fun RedmineIssue.toDomainModel(): Issue = Issue(id, subject)

/**
 * This class is used specifically for API requests to create or update time entries.
 * It uses the field names expected by the Redmine API.
 */
@Serializable
data class RedmineTimeEntryRequest(
    val id: Int? = null,
    @SerialName("spent_on") val spentOn: String = "",
    val hours: Float = 0f,
    @SerialName("activity_id") val activityId: Int = -1,
    @SerialName("project_id") val projectId: Int = -1,
    @SerialName("issue_id") val issueId: Int = -1,
    val comments: String? = null
)

fun TimeEntry.toApiModel(): RedmineTimeEntry {
    val apiModel = RedmineTimeEntry(
        id = id,
        spentOn = date.toString(),
        hours = hours,
        activity = activity.toApiModel(),
        project = project.toApiModel(),
        issue = issue.toApiModel(),
        comments = comments
    )

    return apiModel
}

/**
 * Convert a TimeEntry to a RedmineTimeEntryRequest for API requests.
 * This uses the field names expected by the Redmine API.
 */
fun TimeEntry.toApiRequest(): RedmineTimeEntryRequest {
    val apiRequest = RedmineTimeEntryRequest(
        id = id,
        spentOn = date.toString(),
        hours = hours,
        activityId = activity.id,
        projectId = project.id,
        issueId = issue.id,
        comments = comments
    )

    return apiRequest
}

fun Activity.toApiModel(): RedmineActivity = RedmineActivity(id, name)
fun Project.toApiModel(): RedmineProject = RedmineProject(id, name)
fun Issue.toApiModel(): RedmineIssue = RedmineIssue(id, subject)


@Serializable
data class RedmineAccountResponse(
    val user: RedmineUser
)

@Serializable
data class RedmineUser(
    val id: Int = -1,
    val login: String = "",
    val admin: Boolean = false,
    val firstname: String = "",
    val lastname: String = "",
    val mail: String = "",
    @SerialName("created_on") val createdOn: String? = null,
    @SerialName("last_login_on") val lastLoginOn: String? = null,
    @SerialName("api_key") val apiKey: String? = null,
    @SerialName("custom_fields") val customFields: List<RedmineCustomField> = emptyList()
)

@Serializable
data class RedmineCustomField(
    val id: Int,
    val name: String = "",
    val value: String? = null
)
