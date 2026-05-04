package com.ps.redmine.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.WorkHours
import kotlinx.coroutines.CancellationException

private val HOURS_INPUT_REGEX = Regex("^\\d*([\\.,]\\d{0,1})?$")

@Composable
fun BulkActionBar(
    count: Int,
    onClear: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = count > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Strings["selection_count"].format(count),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(Strings["bulk_edit"])
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(Strings["bulk_delete"])
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = Strings["clear_selection"],
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun BulkEditDialog(
    selectedEntries: List<TimeEntry>,
    redmineClient: RedmineClientInterface,
    onDismiss: () -> Unit,
    onApply: (
        newProject: Project?,
        newActivity: Activity?,
        newIssue: Issue?,
        newHours: Float?,
        newComments: String?,
    ) -> Unit,
) {
    val sharedProjectId: Int? = remember(selectedEntries) {
        val ids = selectedEntries.map { it.project.id }.distinct()
        if (ids.size == 1) ids.first() else null
    }

    var modifyProject by remember { mutableStateOf(false) }
    var modifyActivity by remember { mutableStateOf(false) }
    var modifyIssue by remember { mutableStateOf(false) }
    var modifyHours by remember { mutableStateOf(false) }
    var modifyComments by remember { mutableStateOf(false) }

    var newProject by remember { mutableStateOf<Project?>(null) }
    var newActivity by remember { mutableStateOf<Activity?>(null) }
    var newIssue by remember { mutableStateOf<Issue?>(null) }
    var hoursText by remember { mutableStateOf("") }
    var commentsText by remember { mutableStateOf("") }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoadingProjects by remember { mutableStateOf(false) }
    var isLoadingDeps by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Lazy-load project list the first time the user enables "Modify project"
    LaunchedEffect(modifyProject) {
        if (modifyProject && projects.isEmpty()) {
            isLoadingProjects = true
            try {
                projects = redmineClient.getProjectsWithOpenIssues()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                projects = emptyList()
            } finally {
                isLoadingProjects = false
            }
        }
    }

    // Project context that drives activity/issue dropdowns:
    //   - the newly chosen project when changing project
    //   - otherwise the shared project of the selection (if any)
    val effectiveProjectId: Int? = if (modifyProject) newProject?.id else sharedProjectId

    LaunchedEffect(effectiveProjectId) {
        // Selections from a previous project context don't apply anymore
        newActivity = null
        newIssue = null
        if (effectiveProjectId != null) {
            isLoadingDeps = true
            try {
                activities = redmineClient.getActivitiesForProject(effectiveProjectId)
                issues = redmineClient.getIssues(effectiveProjectId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                activities = emptyList()
                issues = emptyList()
            } finally {
                isLoadingDeps = false
            }
        } else {
            activities = emptyList()
            issues = emptyList()
        }
    }

    val maxHours = WorkHours.configuredDailyHours()
    val parsedHours = hoursText.replace(',', '.').toFloatOrNull()
    val hoursError: String? = when {
        !modifyHours -> null
        hoursText.isEmpty() -> Strings["hours_must_be_positive"]
        parsedHours == null -> Strings["invalid_number"]
        parsedHours <= 0f -> Strings["hours_must_be_positive"]
        parsedHours > maxHours -> Strings["hours_max_value"].format(maxHours)
        else -> null
    }
    val hoursValid = hoursError == null

    val projectValid = !modifyProject || newProject != null
    val activityValid = !modifyActivity || newActivity != null
    val issueValid = !modifyIssue || newIssue != null
    val commentsValid = !modifyComments || commentsText.isNotBlank()

    val anyFieldEnabled = modifyProject || modifyActivity || modifyIssue || modifyHours || modifyComments
    val canApply =
        anyFieldEnabled && projectValid && activityValid && issueValid && hoursValid && commentsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings["bulk_edit_title"]) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = Strings["selection_count"].format(selectedEntries.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                BulkEditFieldRow(
                    label = Strings["bulk_edit_modify_project"],
                    enabled = true,
                    checked = modifyProject,
                    onCheckedChange = { modifyProject = it },
                ) {
                    SearchableDropdown(
                        items = projects,
                        selectedItem = newProject,
                        onItemSelected = { newProject = it },
                        itemText = { it.name },
                        label = { Text(Strings["bulk_edit_modify_project"]) },
                        isLoading = isLoadingProjects,
                        isError = modifyProject && newProject == null,
                    )
                }

                BulkEditFieldRow(
                    label = Strings["bulk_edit_modify_activity"],
                    enabled = effectiveProjectId != null,
                    checked = modifyActivity,
                    onCheckedChange = { modifyActivity = it },
                ) {
                    SearchableDropdown(
                        items = activities,
                        selectedItem = newActivity,
                        onItemSelected = { newActivity = it },
                        itemText = { it.name },
                        label = { Text(Strings["bulk_edit_modify_activity"]) },
                        isLoading = isLoadingDeps,
                        isError = modifyActivity && newActivity == null,
                    )
                }

                BulkEditFieldRow(
                    label = Strings["bulk_edit_modify_issue"],
                    enabled = effectiveProjectId != null,
                    checked = modifyIssue,
                    onCheckedChange = { modifyIssue = it },
                ) {
                    SearchableDropdown(
                        items = issues,
                        selectedItem = newIssue,
                        onItemSelected = { newIssue = it },
                        itemText = { "#${it.id} - ${it.subject}" },
                        label = { Text(Strings["bulk_edit_modify_issue"]) },
                        isLoading = isLoadingDeps,
                        isError = modifyIssue && newIssue == null,
                    )
                }

                if (sharedProjectId == null && !modifyProject) {
                    Text(
                        text = Strings["bulk_edit_mixed_projects"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                BulkEditFieldRow(
                    label = Strings["bulk_edit_modify_hours"],
                    enabled = true,
                    checked = modifyHours,
                    onCheckedChange = { modifyHours = it },
                ) {
                    Column {
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(HOURS_INPUT_REGEX)) {
                                    val candidate = input.replace(',', '.').toFloatOrNull()
                                    if (candidate == null || candidate <= maxHours) {
                                        hoursText = input
                                    }
                                }
                            },
                            label = { Text(Strings["bulk_edit_modify_hours"]) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = modifyHours && !hoursValid,
                        )
                        if (modifyHours && hoursError != null && hoursText.isNotEmpty()) {
                            Text(
                                text = hoursError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                            )
                        }
                    }
                }

                BulkEditFieldRow(
                    label = Strings["bulk_edit_modify_comments"],
                    enabled = true,
                    checked = modifyComments,
                    onCheckedChange = { modifyComments = it },
                ) {
                    OutlinedTextField(
                        value = commentsText,
                        onValueChange = { commentsText = it },
                        label = { Text(Strings["bulk_edit_modify_comments"]) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = modifyComments && !commentsValid,
                    )
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!anyFieldEnabled) {
                        errorMessage = Strings["bulk_no_field_selected"]
                        return@TextButton
                    }
                    onApply(
                        if (modifyProject) newProject else null,
                        if (modifyActivity) newActivity else null,
                        if (modifyIssue) newIssue else null,
                        if (modifyHours) parsedHours else null,
                        if (modifyComments) commentsText else null,
                    )
                },
                enabled = canApply,
            ) {
                Text(Strings["bulk_edit_apply"])
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["confirm_delete_no"])
            }
        },
    )
}

@Composable
private fun BulkEditFieldRow(
    label: String,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = checked && enabled,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
    if (checked && enabled) {
        Box(modifier = Modifier.padding(start = 56.dp)) {
            content()
        }
    }
}
