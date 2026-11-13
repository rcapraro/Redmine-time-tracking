package com.ps.redmine.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.lucca.AbsenceTarget
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.resources.Strings
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun AbsenceMappingDialog(
    leaveType: String,
    redmineClient: RedmineClientInterface,
    initial: AbsenceTarget?,
    onSave: (AbsenceTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var project by remember { mutableStateOf(initial?.project) }
    var issue by remember { mutableStateOf(initial?.issue) }
    var activity by remember { mutableStateOf(initial?.activity) }
    var loadingProjects by remember { mutableStateOf(true) }
    var loadingDetails by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            projects = redmineClient.getProjectsWithOpenIssues()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = Strings["error_loading_projects"]
        } finally {
            loadingProjects = false
        }
    }

    LaunchedEffect(project) {
        val selected = project ?: return@LaunchedEffect
        // Never let the lists of the previously selected project pair with this one.
        issues = emptyList()
        activities = emptyList()
        error = null
        loadingDetails = true
        try {
            activities = redmineClient.getActivitiesForProject(selected.id)
            issues = redmineClient.getIssues(selected.id)
            // A remembered issue may have been closed since; only keep choices that still exist.
            issue = issues.firstOrNull { it.id == issue?.id }
            activity = activities.firstOrNull { it.id == activity?.id }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            issues = emptyList()
            activities = emptyList()
            issue = null
            activity = null
            error = Strings["error_loading_issues_activities"]
        } finally {
            loadingDetails = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings["absence_mapping_title"].format(leaveType)) },
        text = {
            Column(modifier = Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(Strings["absence_mapping_hint"], style = MaterialTheme.typography.bodyMedium)
                SearchableDropdown(
                    items = projects,
                    selectedItem = project,
                    onItemSelected = {
                        project = it
                        issue = null
                        activity = null
                    },
                    itemText = { it.name },
                    label = { Text(Strings["project_label"]) },
                    isLoading = loadingProjects,
                    noItemsText = Strings["no_projects"],
                )
                SearchableDropdown(
                    items = issues,
                    selectedItem = issue,
                    onItemSelected = { issue = it },
                    itemText = { "#${it.id} - ${it.subject}" },
                    label = { Text(Strings["issue_label"]) },
                    enabled = project != null,
                    isLoading = loadingDetails,
                )
                SearchableDropdown(
                    items = activities,
                    selectedItem = activity,
                    onItemSelected = { activity = it },
                    itemText = { it.name },
                    label = { Text(Strings["activity_label"]) },
                    enabled = project != null,
                    isLoading = loadingDetails,
                    noItemsText = Strings["no_activities"],
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            val p = project
            val i = issue
            val a = activity
            Button(
                onClick = { onSave(AbsenceTarget(p!!.id, p.name, i!!.id, i.subject, a!!.id, a.name)) },
                enabled = p != null && i != null && a != null && !loadingDetails,
            ) {
                Text(Strings["save"])
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings["cancel"]) }
        },
    )
}
