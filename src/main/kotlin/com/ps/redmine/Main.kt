package com.ps.redmine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ps.redmine.api.RedmineClient
import com.ps.redmine.components.DatePicker
import com.ps.redmine.di.appModule
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.time.YearMonth
import java.util.*

@Composable
fun App(redmineClient: RedmineClient) {
    var selectedTimeEntry by remember { mutableStateOf<TimeEntry?>(null) }
    var timeEntries by remember { mutableStateOf<List<TimeEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val totalHours = remember(timeEntries) { timeEntries.sumOf { it.hours.toDouble() } }

    fun loadTimeEntries(yearMonth: YearMonth) {
        scope.launch {
            isLoading = true
            try {
                timeEntries = redmineClient.getTimeEntriesForMonth(
                    yearMonth.year,
                    yearMonth.monthValue
                )
            } catch (e: Exception) {
                errorMessage = Strings["error_loading_entries"].format(e.message)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteTimeEntry(timeEntry: TimeEntry) {
        scope.launch {
            try {
                timeEntry.id?.let { id ->
                    println("[DEBUG_LOG] Attempting to delete time entry #$id")
                    try {
                        redmineClient.deleteTimeEntry(id)
                        println("[DEBUG_LOG] Delete operation completed successfully")
                        scaffoldState.snackbarHostState.showSnackbar("Time entry deleted successfully")
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] Error during delete operation: ${e.message}")
                        scaffoldState.snackbarHostState.showSnackbar(
                            "Operation might have succeeded, but there was an error: ${e.message}"
                        )
                    } finally {
                        // Always refresh the list and clear selection
                        println("[DEBUG_LOG] Refreshing time entries list")
                        loadTimeEntries(currentMonth)
                        selectedTimeEntry = null
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG_LOG] Unexpected error in delete operation: ${e.message}")
                scaffoldState.snackbarHostState.showSnackbar("An unexpected error occurred: ${e.message}")
            }
        }
    }

    LaunchedEffect(currentMonth) {
        loadTimeEntries(currentMonth)
    }

    LaunchedEffect(Unit) {
        KeyShortcutManager.keyShortcuts.collect { shortcut ->
            when (shortcut) {
                KeyShortcut.PreviousMonth -> {
                    selectedTimeEntry = null
                    currentMonth = currentMonth.minusMonths(1)
                }

                KeyShortcut.NextMonth -> {
                    selectedTimeEntry = null
                    currentMonth = currentMonth.plusMonths(1)
                }

                KeyShortcut.CurrentMonth -> {
                    selectedTimeEntry = null
                    currentMonth = YearMonth.now()
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scaffoldState.snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss"
            )
            errorMessage = null
        }
    }

    MaterialTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FloatingActionButton(
                        onClick = {
                            selectedTimeEntry = TimeEntry(
                                id = null,
                                date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                                hours = 0f,
                                project = Project(0, ""),
                                activity = Activity(0, ""),
                                issue = Issue(0, ""),
                                comments = ""
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add new time entry")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        Strings["new_entry"],
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // Left panel - Time entries list
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Month navigation and total hours
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            currentMonth = currentMonth.minusMonths(1)
                                            selectedTimeEntry = null
                                        }
                                    ) {
                                        Text(Strings["nav_previous"])
                                    }
                                    Text(
                                        text = currentMonth.format(),
                                        style = MaterialTheme.typography.h6
                                    )
                                    IconButton(
                                        onClick = {
                                            currentMonth = currentMonth.plusMonths(1)
                                            selectedTimeEntry = null
                                        }
                                    ) {
                                        Text(Strings["nav_next"])
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            currentMonth = YearMonth.now()
                                            selectedTimeEntry = null
                                        }
                                    ) {
                                        Text(Strings["today_shortcut"])
                                    }
                                }
                                Text(
                                    text = Strings["nav_help"],
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings["total_hours"],
                                    style = MaterialTheme.typography.subtitle1
                                )
                                Text(
                                    text = "%.1f".format(totalHours),
                                    style = MaterialTheme.typography.h6,
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }

                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            TimeEntriesList(
                                timeEntries = timeEntries,
                                selectedTimeEntry = selectedTimeEntry,
                                onTimeEntrySelected = { selectedTimeEntry = it },
                                onDelete = { entry -> deleteTimeEntry(entry) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right panel - Time entry details
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    TimeEntryDetail(
                        timeEntry = selectedTimeEntry,
                        redmineClient = redmineClient,
                        onSave = { updatedEntry ->
                            scope.launch {
                                try {
                                    println("[DEBUG_LOG] Saving entry in parent scope")
                                    if (updatedEntry.id == null) {
                                        redmineClient.createTimeEntry(updatedEntry)
                                    } else {
                                        redmineClient.updateTimeEntry(updatedEntry)
                                    }
                                    // Refresh the list
                                    timeEntries = redmineClient.getTimeEntriesForMonth(
                                        currentMonth.year,
                                        currentMonth.monthValue
                                    )
                                    selectedTimeEntry = null

                                    val message = if (updatedEntry.id == null)
                                        Strings["entry_created"]
                                    else
                                        Strings["entry_updated"]
                                    scaffoldState.snackbarHostState.showSnackbar(message)
                                } catch (e: Exception) {
                                    println("[DEBUG_LOG] Error in parent scope: ${e.message}")
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        Strings["operation_error"].format(e.message)
                                    )
                                }
                            }
                        },
                        onCancel = { selectedTimeEntry = null }
                    )
                }
            }
        }
    }
}

@Composable
fun TimeEntriesList(
    timeEntries: List<TimeEntry>,
    selectedTimeEntry: TimeEntry?,
    onTimeEntrySelected: (TimeEntry) -> Unit,
    onDelete: (TimeEntry) -> Unit
) {
    val groupedEntries = remember(timeEntries) {
        timeEntries
            .sortedByDescending { it.date }
            .groupBy { it.date }
    }

    LazyColumn {
        groupedEntries.forEach { (date, entries) ->
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateFormatter.formatFull(date),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = Strings["hours_format"].format(entries.sumOf { it.hours.toDouble() }),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    entries.forEach { entry ->
                        TimeEntryItem(
                            timeEntry = entry,
                            isSelected = entry == selectedTimeEntry,
                            onClick = { onTimeEntrySelected(entry) },
                            onDelete = { onDelete(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeEntryItem(
    timeEntry: TimeEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        elevation = if (isSelected) 4.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateFormatter.formatShort(timeEntry.date),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = Strings["hours_format"].format(timeEntry.hours),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                }
                Column {
                    Text(
                        text = timeEntry.project.name,
                        style = MaterialTheme.typography.body1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeEntry.activity.name,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = "• #${timeEntry.issue.id} ${timeEntry.issue.subject}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.secondary
                        )
                        if (timeEntry.comments.isNotEmpty()) {
                            Text(
                                text = "• ${timeEntry.comments}",
                                style = MaterialTheme.typography.caption,
                                maxLines = 1,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete time entry",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

@Composable
fun TimeEntryDetail(
    timeEntry: TimeEntry?,
    redmineClient: RedmineClient,
    onSave: (TimeEntry) -> Unit,
    onCancel: () -> Unit = {}
) {
    var date by remember(timeEntry) {
        mutableStateOf(
            timeEntry?.date ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        ).also { println("[DEBUG_LOG] Initial date: ${it.value}") }
    }
    var hours by remember(timeEntry) {
        mutableStateOf(timeEntry?.hours?.toString() ?: "").also { println("[DEBUG_LOG] Initial hours: ${it.value}") }
    }
    var comments by remember(timeEntry) {
        mutableStateOf(timeEntry?.comments ?: "").also { println("[DEBUG_LOG] Initial comments: ${it.value}") }
    }
    var selectedProject by remember {
        mutableStateOf<Project?>(null).also { println("[DEBUG_LOG] Initial project: ${it.value?.name}") }
    }
    var selectedActivity by remember {
        mutableStateOf<Activity?>(null).also { println("[DEBUG_LOG] Initial activity: ${it.value?.name}") }
    }
    var selectedIssue by remember {
        mutableStateOf<Issue?>(timeEntry?.issue).also { println("[DEBUG_LOG] Initial issue: ${it.value?.subject}") }
    }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingIssues by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showActivityDropdown by remember { mutableStateOf(false) }
    var showIssueDropdown by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val isValid = remember(selectedProject, selectedActivity, hours, selectedIssue) {
        selectedProject != null &&
                selectedActivity != null &&
                hours.isNotEmpty() &&
                hours.toFloatOrNull() != null &&
                hours.toFloatOrNull() != 0f &&
                selectedIssue != null
    }

    val hasChanges = remember(hours, comments, selectedProject, selectedActivity, date, selectedIssue) {
        val changed = if (timeEntry == null) {
            hours.isNotEmpty() || comments.isNotEmpty() || selectedProject != null || selectedActivity != null || selectedIssue != null
        } else {
            hours != timeEntry.hours.toString() ||
                    comments != timeEntry.comments ||
                    selectedProject?.id != timeEntry.project.id ||
                    selectedActivity != timeEntry.activity ||
                    date != timeEntry.date ||
                    selectedIssue?.id != timeEntry.issue.id
        }
        println("[DEBUG_LOG] Form changes detected: $changed")
        println("[DEBUG_LOG] Hours: $hours, Comments: $comments")
        println("[DEBUG_LOG] Project: ${selectedProject?.name}, Activity: ${selectedActivity?.name}")
        println("[DEBUG_LOG] Issue: ${selectedIssue?.subject}")
        changed
    }

    fun saveEntry() {
        if (!isValid || isSaving) return
        selectedProject?.let { project ->
            selectedActivity?.let { activity ->
                scope.launch {
                    isSaving = true
                    try {
                        println("[DEBUG_LOG] Saving time entry (id: ${timeEntry?.id})")
                        onSave(
                            TimeEntry(
                                id = timeEntry?.id,
                                date = date,
                                hours = hours.toFloatOrNull() ?: 0f,
                                project = project,
                                issue = selectedIssue ?: Issue(0, ""),
                                activity = activity,
                                comments = comments
                            )
                        )
                        println("[DEBUG_LOG] Save operation completed successfully")
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] Error in save operation UI handler: ${e.message}")
                    } finally {
                        isSaving = false
                    }
                }
            }
        }
    }

    fun handleCancel() {
        if (hasChanges) {
            showCancelConfirmation = true
        } else {
            onCancel()
        }
    }

    // Load projects and activities
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            println("[DEBUG_LOG] Loading initial data")
            projects = redmineClient.getProjects()
            activities = redmineClient.getActivities()
            println("[DEBUG_LOG] Loaded ${projects.size} projects and ${activities.size} activities")
        } catch (e: Exception) {
            println("[DEBUG_LOG] Error loading data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Load issues when project changes
    LaunchedEffect(selectedProject) {
        val project = selectedProject
        println("[DEBUG_LOG] Project changed: ${project?.name}")
        println("[DEBUG_LOG] Current state - Hours: $hours, Comments: $comments")
        println("[DEBUG_LOG] Current state - Activity: ${selectedActivity?.name}")

        if (project != null) {
            isLoadingIssues = true
            try {
                println("[DEBUG_LOG] Loading issues for project: ${project.id} (${project.name})")
                issues = redmineClient.getIssues(project.id)
                println("[DEBUG_LOG] Loaded ${issues.size} issues")
                issues.forEach { println("[DEBUG_LOG] Issue: #${it.id} - ${it.subject}") }
            } catch (e: Exception) {
                println("[DEBUG_LOG] Error loading issues: ${e.message}")
                issues = emptyList()
            } finally {
                isLoadingIssues = false
            }
        } else {
            issues = emptyList()
            selectedIssue = null
        }

        println("[DEBUG_LOG] After project change - Hours: $hours, Comments: $comments")
        println("[DEBUG_LOG] After project change - Activity: ${selectedActivity?.name}")
    }

    // Update selections when timeEntry changes or lists are loaded
    LaunchedEffect(timeEntry) {
        println("[DEBUG_LOG] Time entry changed: ${timeEntry?.id}")
        println("[DEBUG_LOG] Current state before update:")
        println("[DEBUG_LOG] - Hours: $hours")
        println("[DEBUG_LOG] - Comments: $comments")
        println("[DEBUG_LOG] - Project: ${selectedProject?.name}")
        println("[DEBUG_LOG] - Activity: ${selectedActivity?.name}")
        println("[DEBUG_LOG] - Issue: ${selectedIssue?.subject}")

        // Only update on initial load or when editing an entry
        if (timeEntry != null && (selectedProject == null || timeEntry.id != null)) {
            println("[DEBUG_LOG] Updating from time entry ${if (timeEntry.id == null) "(new)" else "#${timeEntry.id}"}")

            // Preserve existing values if they exist
            if (selectedProject == null) {
                selectedProject = projects.find { it.id == timeEntry.project.id }
                selectedActivity = activities.find { it.id == timeEntry.activity.id }
                selectedIssue = timeEntry.issue
                hours = timeEntry.hours.toString()
                comments = timeEntry.comments
            }

            println("[DEBUG_LOG] State after update:")
            println("[DEBUG_LOG] - Hours: $hours")
            println("[DEBUG_LOG] - Comments: $comments")
            println("[DEBUG_LOG] - Project: ${selectedProject?.name}")
            println("[DEBUG_LOG] - Activity: ${selectedActivity?.name}")
            println("[DEBUG_LOG] - Issue: ${selectedIssue?.subject}")
        }
    }


    // Handle keyboard shortcuts
    LaunchedEffect(Unit) {
        KeyShortcutManager.keyShortcuts.collect { shortcut ->
            when (shortcut) {
                KeyShortcut.Save -> if (isValid && !isSaving) saveEntry()
                KeyShortcut.Cancel -> handleCancel()
                KeyShortcut.PreviousMonth,
                KeyShortcut.NextMonth,
                KeyShortcut.CurrentMonth -> {
                } // Handled by parent
            }
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text(Strings["discard_changes_title"]) },
            text = { Text(Strings["discard_changes_message"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmation = false
                        onCancel()
                    }
                ) {
                    Text(Strings["discard"])
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(Strings["continue_editing"])
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (timeEntry == null) Strings["add_time_entry"] else Strings["edit_time_entry"],
                style = MaterialTheme.typography.h6
            )
            TextButton(
                onClick = { handleCancel() },
                enabled = !isLoading && !isSaving
            ) {
                Text(Strings["cancel_shortcut"])
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date picker and Today button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DatePicker(
                selectedDate = date,
                onDateSelected = { date = it },
                modifier = Modifier.width(200.dp)
            )

            TextButton(
                onClick = { date = today },
                enabled = !isLoading && !isSaving
            ) {
                Text("Aujourd'hui")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Project dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedProject?.name ?: "",
                    onValueChange = {},
                    label = { Text(Strings["project_label"]) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = !isLoading,
                    isError = !isLoading && projects.isNotEmpty() && selectedProject == null,
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { showProjectDropdown = true },
                                enabled = projects.isNotEmpty()
                            ) {
                                Text(if (showProjectDropdown) Strings["dropdown_up"] else Strings["dropdown_down"])
                            }
                        }
                    }
                )
                DropdownMenu(
                    expanded = showProjectDropdown && !isLoading,
                    onDismissRequest = { showProjectDropdown = false }
                ) {
                    projects.forEach { project ->
                        DropdownMenuItem(
                            onClick = {
                                println("[DEBUG_LOG] Project selection changed:")
                                println("[DEBUG_LOG] - From: ${selectedProject?.name}")
                                println("[DEBUG_LOG] - To: ${project.name}")
                                println("[DEBUG_LOG] Current state before change:")
                                println("[DEBUG_LOG] - Hours: $hours")
                                println("[DEBUG_LOG] - Comments: $comments")
                                println("[DEBUG_LOG] - Activity: ${selectedActivity?.name}")
                                println("[DEBUG_LOG] - Issue: ${selectedIssue?.subject}")

                                selectedProject = project
                                selectedIssue = null  // Reset only issue selection
                                showProjectDropdown = false

                                println("[DEBUG_LOG] State after project change:")
                                println("[DEBUG_LOG] - Hours: $hours")
                                println("[DEBUG_LOG] - Comments: $comments")
                                println("[DEBUG_LOG] - Activity: ${selectedActivity?.name}")
                                println("[DEBUG_LOG] - Issue: ${selectedIssue?.subject}")
                            }
                        ) {
                            Text(
                                text = project.name,
                                color = if (project == selectedProject)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
            if (!isLoading && projects.isEmpty()) {
                Text(
                    text = Strings["no_projects"],
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hours
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hours,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        val newValue = input.take(4) // Limit to 4 characters (e.g., "7.50")
                        val floatValue = newValue.toFloatOrNull()
                        if (floatValue == null || floatValue <= 7.5f) {
                            hours = newValue
                        }
                    }
                },
                label = { Text(Strings["hours_label"]) },
                modifier = Modifier.width(200.dp),
                isError = hours.isNotEmpty() && (hours.toFloatOrNull() == null || hours.toFloat() <= 0f || hours.toFloat() > 7.5f),
                singleLine = true,
                enabled = !isLoading,
                trailingIcon = {
                    if (hours.isNotEmpty()) {
                        IconButton(
                            onClick = { hours = "" },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = Strings["clear_button"],
                                modifier = Modifier.semantics {
                                    contentDescription = Strings["clear_button_description"]
                                }
                            )
                        }
                    }
                }
            )

            TextButton(
                onClick = { hours = "7.5" },
                enabled = !isLoading
            ) {
                Text("Journée complète")
            }
            Column {
                if (hours.isNotEmpty() && hours.toFloatOrNull() == null) {
                    Text(
                        text = Strings["invalid_number"],
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                } else if (hours.isNotEmpty() && hours.toFloat() <= 0f) {
                    Text(
                        text = Strings["hours_must_be_positive"],
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                } else if (hours.isNotEmpty() && hours.toFloat() > 7.5f) {
                    Text(
                        text = Strings["hours_max_value"],
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Issue dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedIssue?.let { "#${it.id} - ${it.subject}" } ?: "",
                    onValueChange = {},
                    label = { Text(Strings["issue_label"]) },
                    placeholder = { Text(Strings["select_issue_placeholder"]) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = !isLoading,
                    isError = !isLoading && issues.isNotEmpty() && selectedIssue == null,
                    trailingIcon = {
                        if (isLoadingIssues) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { showIssueDropdown = true },
                                enabled = !isLoading && issues.isNotEmpty()
                            ) {
                                Text(if (showIssueDropdown) Strings["dropdown_up"] else Strings["dropdown_down"])
                            }
                        }
                    }
                )
                DropdownMenu(
                    expanded = showIssueDropdown && !isLoading,
                    onDismissRequest = { showIssueDropdown = false }
                ) {
                    issues.forEach { issue ->
                        DropdownMenuItem(
                            onClick = {
                                selectedIssue = issue
                                showIssueDropdown = false
                            }
                        ) {
                            Text(
                                text = "#${issue.id} - ${issue.subject}",
                                color = if (issue == selectedIssue)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
            when {
                isLoadingIssues -> {
                    val project = selectedProject
                    Text(
                        text = if (project != null)
                            "Loading open issues for project ${project.name}..."
                        else
                            "Loading issues...",
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                selectedProject == null -> {
                    Text(
                        text = Strings["select_project_first"],
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                issues.isEmpty() && selectedIssue != null -> {
                    Text(
                        text = Strings["showing_issue"].format(selectedIssue!!.id),
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                issues.isEmpty() -> {
                    val project = selectedProject
                    Text(
                        text = if (project != null)
                            "No open issues found in project ${project.name}"
                        else
                            "No issues available",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Activity dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedActivity?.name ?: "",
                    onValueChange = {},
                    label = { Text(Strings["activity_label"]) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = !isLoading,
                    isError = !isLoading && activities.isNotEmpty() && selectedActivity == null,
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { showActivityDropdown = true },
                                enabled = activities.isNotEmpty()
                            ) {
                                Text(if (showActivityDropdown) Strings["dropdown_up"] else Strings["dropdown_down"])
                            }
                        }
                    }
                )
                DropdownMenu(
                    expanded = showActivityDropdown && !isLoading,
                    onDismissRequest = { showActivityDropdown = false }
                ) {
                    activities.forEach { activity ->
                        DropdownMenuItem(
                            onClick = {
                                selectedActivity = activity
                                showActivityDropdown = false
                            }
                        ) {
                            Text(
                                text = activity.name,
                                color = if (activity == selectedActivity)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
            if (!isLoading && activities.isEmpty()) {
                Text(
                    text = Strings["no_activities"],
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Comments
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = comments,
                onValueChange = { newValue ->
                    if (newValue.length <= 255) {
                        comments = newValue
                    }
                },
                label = { Text(Strings["comments_label"]) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isLoading,
                trailingIcon = if (comments.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { comments = "" },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = Strings["clear_button"],
                                modifier = Modifier.semantics {
                                    contentDescription = Strings["clear_button_description"]
                                }
                            )
                        }
                    }
                } else null
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = Strings["char_count"].format(comments.length),
                    style = MaterialTheme.typography.caption,
                    color = if (comments.length > 240) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(
                        alpha = 0.6f
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isSaving && selectedProject != null && selectedActivity != null && selectedIssue != null) {
                    onSave(
                        TimeEntry(
                            id = timeEntry?.id,
                            date = date,
                            hours = hours.toFloatOrNull() ?: 0f,
                            project = selectedProject!!,
                            activity = selectedActivity!!,
                            issue = selectedIssue!!,
                            comments = comments
                        )
                    )
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = isValid && !isLoading && !isSaving
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
                println("[DEBUG_LOG] TimeEntry id: ${timeEntry?.id}")
                Text(if (timeEntry?.id == null) Strings["add_entry"] else Strings["update_entry"])
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keyboard shortcuts help
        Text(
            text = Strings["keyboard_shortcuts"],
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

fun main() {
    // Set default locale to French
    Locale.setDefault(Locale.FRENCH)
    println("[DEBUG_LOG] Default locale set to: ${Locale.getDefault()}")

    // Initialize Strings with French as default
    println("[DEBUG_LOG] Initializing Strings object")
    Strings

    application {
        startKoin {
            properties(
                mapOf(
                    "redmine.uri" to (System.getenv("REDMINE_URL") ?: "https://redmine-restreint.packsolutions.local"),
                    "redmine.username" to (System.getenv("REDMINE_USERNAME") ?: ""),
                    "redmine.password" to (System.getenv("REDMINE_PASSWORD") ?: "")
                )
            )
            modules(appModule)
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = Strings["window_title"],
            onKeyEvent = { KeyShortcutManager.handleKeyEvent(it) },
            state = rememberWindowState(width = 1050.dp, height = 850.dp)
        ) {
            val redmineClient = koinInject<RedmineClient>()
            App(redmineClient)
        }
    }
}
