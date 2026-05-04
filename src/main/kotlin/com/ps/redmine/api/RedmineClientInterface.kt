package com.ps.redmine.api

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.User
import java.io.Closeable
import com.ps.redmine.model.TimeEntry as AppTimeEntry

/**
 * Interface for Redmine client implementations.
 * Defines the contract for interacting with the Redmine API.
 */
interface RedmineClientInterface : Closeable {
    /**
     * Updates the client configuration with new URI and API key.
     */
    fun updateConfiguration(newUri: String, newApiKey: String)

    /**
     * Sets (or clears, with null) the user to act on behalf of via the
     * `X-Redmine-Switch-User` header. Requires the configured API key
     * to belong to a Redmine administrator.
     *
     * @param login the target user's Redmine login, or null to stop impersonating.
     */
    fun setImpersonation(login: String?)

    /**
     * Lists all active users. Requires admin privileges in Redmine.
     */
    suspend fun listUsers(): List<User>

    /**
     * Gets the user's theoretical weekly working hours from Redmine (custom field id 27).
     * Returns null if not available.
     */
    suspend fun getUserWeeklyHours(): Float?

    /**
     * Gets the currently authenticated user (resolved from the configured API key
     * via Redmine's `/my/account.json`). Returns null if the call fails.
     */
    suspend fun getCurrentUser(): User?

    /**
     * Gets time entries for a specific month.
     */
    suspend fun getTimeEntriesForMonth(year: Int, month: Int): List<AppTimeEntry>

    /**
     * Creates a new time entry.
     */
    suspend fun createTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry

    /**
     * Updates an existing time entry.
     */
    suspend fun updateTimeEntry(timeEntry: AppTimeEntry): AppTimeEntry

    /**
     * Deletes a time entry.
     */
    suspend fun deleteTimeEntry(timeEntryId: Int)

    /**
     * Gets all available activities.
     *
     * Note: This method is deprecated. Use getActivitiesForProject(projectId) instead.
     * When no project is selected, this will return an empty list.
     */
    suspend fun getActivities(): List<Activity>

    /**
     * Gets activities for a specific project.
     *
     * @param projectId The ID of the project to get activities for
     * @return List of activities available for the specified project
     */
    suspend fun getActivitiesForProject(projectId: Int): List<Activity>

    /**
     * Gets all projects with their activities.
     */
    suspend fun getProjectsWithActivities(): List<Project>

    /**
     * Gets projects that have open issues, with their activities.
     * This method filters out projects that don't have any open tickets.
     */
    suspend fun getProjectsWithOpenIssues(): List<Project>

    /**
     * Gets issues for a specific project.
     */
    suspend fun getIssues(projectId: Int): List<Issue>
}
