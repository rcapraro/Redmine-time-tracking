package com.ps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ps.api.RedmineClient
import com.ps.components.DatePicker
import com.ps.model.Activity
import com.ps.model.Project
import com.ps.model.TimeEntry
import com.ps.util.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
                                onTimeEntrySelected = { selectedTimeEntry = it }
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
                                    errorMessage = if (updatedEntry.id == null)
                                        "Time entry created successfully"
                                    else
                                        "Time entry updated successfully"
                                } catch (e: Exception) {
                                    errorMessage = "Error saving time entry: ${e.message}"
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
    onTimeEntrySelected: (TimeEntry) -> Unit
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
                            onClick = { onTimeEntrySelected(entry) }
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
    onClick: () -> Unit
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
        )
    }
    var hours by remember(timeEntry) { mutableStateOf(timeEntry?.hours?.toString() ?: "") }
    var comments by remember(timeEntry) { mutableStateOf(timeEntry?.comments ?: "") }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedActivity by remember { mutableStateOf<Activity?>(null) }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showActivityDropdown by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val isValid = remember(selectedProject, selectedActivity, hours) {
        selectedProject != null &&
                selectedActivity != null &&
                hours.isNotEmpty() &&
                hours.toFloatOrNull() != null &&
                hours.toFloatOrNull() != 0f
    }

    val hasChanges = remember(hours, comments, selectedProject, selectedActivity, date) {
        if (timeEntry == null) {
            hours.isNotEmpty() || comments.isNotEmpty() || selectedProject != null || selectedActivity != null
        } else {
            hours != timeEntry.hours.toString() ||
                    comments != timeEntry.comments ||
                    selectedProject != timeEntry.project ||
                    selectedActivity != timeEntry.activity ||
                    date != timeEntry.date
        }
    }

    fun saveEntry() {
        if (!isValid || isSaving) return
        selectedProject?.let { project ->
            selectedActivity?.let { activity ->
                scope.launch {
                    isSaving = true
                    try {
                        onSave(
                            TimeEntry(
                                id = timeEntry?.id,
                                date = date,
                                hours = hours.toFloatOrNull() ?: 0f,
                                project = project,
                                activity = activity,
                                comments = comments
                            )
                        )
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
            projects = redmineClient.getProjects()
            activities = redmineClient.getActivities()
        } catch (e: Exception) {
            println("Error loading projects and activities: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Update selections when timeEntry changes or lists are loaded
    LaunchedEffect(timeEntry, projects, activities) {
        if (timeEntry != null && projects.isNotEmpty() && activities.isNotEmpty()) {
            selectedProject = projects.find { it.id == timeEntry.project.id }
            selectedActivity = activities.find { it.id == timeEntry.activity.id }
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
                                selectedProject = project
                                showProjectDropdown = false
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
                if (!isSaving) {
                    selectedProject?.let { project ->
                        selectedActivity?.let { activity ->
                            onSave(
                                TimeEntry(
                                    id = timeEntry?.id,
                                    date = date,
                                    hours = hours.toFloatOrNull() ?: 0f,
                                    project = project,
                                    activity = activity,
                                    comments = comments
                                )
                            )
                        }
                    }
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
    val redmineClient = RedmineClient(
        uri = System.getenv("REDMINE_URL") ?: "http://localhost:3000",
        username = System.getenv("REDMINE_USERNAME") ?: "",
        password = System.getenv("REDMINE_PASSWORD") ?: ""
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Redmine Time Tracking",
        onKeyEvent = { KeyShortcutManager.handleKeyEvent(it) }
    ) {
        App(redmineClient)
    }
}
