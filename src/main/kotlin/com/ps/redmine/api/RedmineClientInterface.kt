package com.ps.redmine.api

import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
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
     * Gets the user's theoretical weekly working hours from Redmine (custom field id 27).
     * Returns null if not available.
     */
    suspend fun getUserWeeklyHours(): Float?

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
