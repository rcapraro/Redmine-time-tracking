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
import com.ps.redmine.util.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.context.startKoin
import org.koin.compose.koinInject
import java.time.YearMonth

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
                errorMessage = "Error loading time entries: ${e.message}"
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
                        "New Entry",
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
                                        Text("←")
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
                                        Text("→")
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
                                        Text("Today (Alt+T)")
                                    }
                                }
                                Text(
                                    text = "Alt+← Previous | Alt+→ Next",
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
                                    text = "Total Hours:",
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
                                onDelete = { entry -> deleteTimeEntry(entry) },
                                redmineClient = redmineClient
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
                                    val savedEntry = if (updatedEntry.id == null) {
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
                                        "Time entry created successfully"
                                    else
                                        "Time entry updated successfully"
                                    scaffoldState.snackbarHostState.showSnackbar(message)
                                } catch (e: Exception) {
                                    println("[DEBUG_LOG] Error in parent scope: ${e.message}")
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        "Operation might have succeeded, but there was an error: ${e.message}"
                                    )
                                }
                            }
                        },
                        onCancel = { selectedTimeEntry = null },
                        showMessage = { message -> 
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(message)
                            }
                        }
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
    onDelete: (TimeEntry) -> Unit,
    redmineClient: RedmineClient
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
                            text = "%.1f h".format(entries.sumOf { it.hours.toDouble() }),
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
                            onDelete = { onDelete(entry) },
                            redmineClient = redmineClient
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
    onDelete: () -> Unit,
    redmineClient: RedmineClient
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
                        text = "%.1f h".format(timeEntry.hours),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                }
                Column {
                    Text(
                        text = timeEntry.project?.name ?: "No Project",
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
    onCancel: () -> Unit = {},
    showMessage: suspend (String) -> Unit
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
            e.printStackTrace()
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
                e.printStackTrace()
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
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmation = false
                        onCancel()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text("Continue Editing")
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
                text = if (timeEntry == null) "Add Time Entry" else "Edit Time Entry",
                style = MaterialTheme.typography.h6
            )
            TextButton(
                onClick = { handleCancel() },
                enabled = !isLoading && !isSaving
            ) {
                Text("Cancel (Esc)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date picker
        DatePicker(
            selectedDate = date,
            onDateSelected = { date = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Today shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { date = today },
                enabled = !isLoading && !isSaving
            ) {
                Text("Set to Today")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Project dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedProject?.name ?: "",
                    onValueChange = {},
                    label = { Text("Project") },
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
                                Text(if (showProjectDropdown) "▲" else "▼")
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
                    text = "No projects available",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hours
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = hours,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        hours = input.take(5) // Limit to 5 characters (e.g., "12.50")
                    }
                },
                label = { Text("Hours") },
                modifier = Modifier.fillMaxWidth(),
                isError = hours.isNotEmpty() && (hours.toFloatOrNull() == null || hours.toFloat() <= 0f),
                singleLine = true,
                enabled = !isLoading,
                trailingIcon = {
                    if (hours.isNotEmpty()) {
                        IconButton(
                            onClick = { hours = "" },
                            enabled = !isLoading
                        ) {
                            Text("✕")
                        }
                    }
                }
            )
            if (hours.isNotEmpty() && hours.toFloatOrNull() == null) {
                Text(
                    text = "Please enter a valid number",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            } else if (hours.toFloatOrNull()?.let { it <= 0f } == true) {
                Text(
                    text = "Hours must be greater than 0",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Issue dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedIssue?.let { "#${it.id} - ${it.subject}" } ?: "",
                    onValueChange = {},
                    label = { Text("Issue") },
                    placeholder = { Text("Select an issue") },
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
                                Text(if (showIssueDropdown) "▲" else "▼")
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
                        text = "Select a project first to see its issues",
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                issues.isEmpty() && selectedIssue != null -> {
                    Text(
                        text = "Currently showing issue #${selectedIssue!!.id}",
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
                    label = { Text("Activity") },
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
                                Text(if (showActivityDropdown) "▲" else "▼")
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
                    text = "No activities available",
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
                label = { Text("Comments") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isLoading,
                trailingIcon = if (comments.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { comments = "" },
                            enabled = !isLoading
                        ) {
                            Text("✕")
                        }
                    }
                } else null
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${comments.length}/255",
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
                Text(if (timeEntry?.id == null) "Add Entry (⌘S)" else "Update Entry (⌘S)")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keyboard shortcuts help
        Text(
            text = "Keyboard shortcuts: ⌘S - Save, Esc - Cancel",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

fun main() = application {
    startKoin {
        properties(mapOf(
            "redmine.uri" to (System.getenv("REDMINE_URL") ?: "http://localhost:3000"),
            "redmine.username" to (System.getenv("REDMINE_USERNAME") ?: ""),
            "redmine.password" to (System.getenv("REDMINE_PASSWORD") ?: "")
        ))
        modules(appModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Redmine Time Tracking",
        onKeyEvent = { KeyShortcutManager.handleKeyEvent(it) },
        state = rememberWindowState(width = 1000.dp, height = 900.dp)
    ) {
        val redmineClient = koinInject<RedmineClient>()
        App(redmineClient)
    }
}
