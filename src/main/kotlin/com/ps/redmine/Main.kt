package com.ps.redmine

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ps.redmine.api.RedmineClient
import com.ps.redmine.components.ConfigurationDialog
import com.ps.redmine.components.DatePicker
import com.ps.redmine.components.TimeEntriesList
import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.di.appModule
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.KeyShortcut
import com.ps.redmine.util.KeyShortcutManager
import com.ps.redmine.util.format
import com.ps.redmine.util.today
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
    var deletingEntryId by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
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
            timeEntry.id?.let { id ->
                deletingEntryId = id
                try {
                    redmineClient.deleteTimeEntry(id)
                    scaffoldState.snackbarHostState.showSnackbar(Strings["time_entry_deleted"])
                } catch (e: Exception) {
                    scaffoldState.snackbarHostState.showSnackbar(
                        Strings["operation_error"].format(e.message)
                    )
                } finally {
                    // Always refresh the list and clear selection
                    loadTimeEntries(currentMonth)
                    selectedTimeEntry = null
                    deletingEntryId = null
                }
            }
        }
    }

    LaunchedEffect(currentMonth) {
        loadTimeEntries(currentMonth)
    }

    // Handle keyboard shortcuts
    DisposableEffect(Unit) {
        val callback: (KeyShortcut) -> Unit = { shortcut ->
            when (shortcut) {
                // Navigation shortcuts
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
                // Save/Cancel shortcuts - only handle if TimeEntryDetail is open
                KeyShortcut.Save,
                KeyShortcut.Cancel -> {
                } // Let TimeEntryDetail handle these
            }
        }
        KeyShortcutManager.setShortcutCallback(callback)

        onDispose {
            KeyShortcutManager.removeShortcutCallback(callback)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scaffoldState.snackbarHostState.showSnackbar(
                message = it,
                actionLabel = Strings["dismiss"]
            )
            errorMessage = null
        }
    }

    MaterialTheme {
        if (showConfigDialog) {
            ConfigurationDialog(
                redmineClient = redmineClient,
                onDismiss = { showConfigDialog = false },
                onConfigSaved = {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(Strings["configuration_saved"])
                        // Reload data with new configuration
                        loadTimeEntries(currentMonth)
                    }
                }
            )
        }
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(Strings["window_title"]) },
                    actions = {
                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = Strings["settings"]
                            )
                        }
                    }
                )
            },
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
                                            if (!isLoading) {
                                                currentMonth = currentMonth.minusMonths(1)
                                                selectedTimeEntry = null
                                            }
                                        },
                                        modifier = Modifier.alpha(if (isLoading) 0.6f else 1f)
                                    ) {
                                        Text(Strings["nav_previous"])
                                    }
                                    Text(
                                        text = currentMonth.format(),
                                        style = MaterialTheme.typography.h6
                                    )
                                    IconButton(
                                        onClick = {
                                            if (!isLoading) {
                                                currentMonth = currentMonth.plusMonths(1)
                                                selectedTimeEntry = null
                                            }
                                        },
                                        modifier = Modifier.alpha(if (isLoading) 0.6f else 1f)
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
                                            if (!isLoading) {
                                                currentMonth = YearMonth.now()
                                                selectedTimeEntry = null
                                            }
                                        },
                                        modifier = Modifier.alpha(if (isLoading) 0.6f else 1f)
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

                        if (isLoading && deletingEntryId == null) {
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
                                deletingEntryId = deletingEntryId
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

// TimeEntriesList and TimeEntryItem moved to com.ps.redmine.components.TimeEntriesList

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
    var hours by remember(timeEntry) {
        mutableStateOf(timeEntry?.hours?.toString() ?: "")
    }
    var comments by remember(timeEntry) {
        mutableStateOf(timeEntry?.comments ?: "")
    }
    var selectedProject by remember {
        mutableStateOf<Project?>(null)
    }
    var selectedActivity by remember {
        mutableStateOf<Activity?>(null)
    }
    var selectedIssue by remember {
        mutableStateOf<Issue?>(timeEntry?.issue)
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
        val hasProject = selectedProject != null
        val hasActivity = selectedActivity != null
        val hasHours = hours.isNotEmpty()
        val validHours = hours.toFloatOrNull() != null
        val nonZeroHours = hours.toFloatOrNull() != 0f
        val hasIssue = selectedIssue != null

        hasProject && hasActivity && hasHours && validHours && nonZeroHours && hasIssue
    }

    val hasChanges = remember(hours, comments, selectedProject, selectedActivity, date, selectedIssue) {
        if (timeEntry == null) {
            hours.isNotEmpty() || comments.isNotEmpty() || selectedProject != null || selectedActivity != null || selectedIssue != null
        } else {
            hours != timeEntry.hours.toString() ||
                    comments != timeEntry.comments ||
                    selectedProject?.id != timeEntry.project.id ||
                    selectedActivity != timeEntry.activity ||
                    date != timeEntry.date ||
                    selectedIssue?.id != timeEntry.issue.id
        }
    }

    fun saveEntry() {
        if (!isValid || isSaving) {
            return
        }

        selectedProject?.let { project ->
            selectedActivity?.let { activity ->
                scope.launch {
                    isSaving = true
                    try {
                        val timeEntryToSave = TimeEntry(
                            id = timeEntry?.id,
                            date = date,
                            hours = hours.toFloatOrNull() ?: 0f,
                            project = project,
                            issue = selectedIssue ?: Issue(0, ""),
                            activity = activity,
                            comments = comments
                        )
                        onSave(timeEntryToSave)
                    } catch (e: Exception) {
                        println("Error saving time entry: ${e.message}")
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
            // Reset all fields before canceling
            date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            hours = ""
            comments = ""
            selectedProject = null
            selectedActivity = null
            selectedIssue = null
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
            println("Error loading data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Load issues when project changes
    LaunchedEffect(selectedProject) {
        val project = selectedProject
        if (project != null) {
            isLoadingIssues = true
            try {
                issues = redmineClient.getIssues(project.id)
            } catch (e: Exception) {
                println("Error loading issues: ${e.message}")
                issues = emptyList()
            } finally {
                isLoadingIssues = false
            }
        } else {
            issues = emptyList()
            selectedIssue = null
        }
    }

    // Update selections when timeEntry changes or lists are loaded
    LaunchedEffect(timeEntry) {
        if (timeEntry != null && (selectedProject == null || timeEntry.id != null)) {
            if (selectedProject == null) {
                selectedProject = projects.find { it.id == timeEntry.project.id }
                selectedActivity = activities.find { it.id == timeEntry.activity.id }
                selectedIssue = timeEntry.issue
                hours = timeEntry.hours.toString()
                comments = timeEntry.comments
            }
        }
    }


    val keyboardHandler = Modifier.onPreviewKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown && event.key == Key.S && event.isMetaPressed -> {
                if (isValid && !isLoading && !isSaving) {
                    saveEntry()
                    true
                } else false
            }

            event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                handleCancel()
                true
            }

            else -> false
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
                        // Reset all fields before canceling
                        date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                        hours = ""
                        comments = ""
                        selectedProject = null
                        selectedActivity = null
                        selectedIssue = null
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

    val shortcutHandler = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        when {
            event.key == Key.S && event.isMetaPressed && isValid && !isLoading && !isSaving -> {
                saveEntry()
                true
            }

            event.key == Key.Escape -> {
                handleCancel()
                true
            }

            else -> false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(shortcutHandler)
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
            Button(
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
                Text(Strings["today"])
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
                                selectedProject = project
                                selectedIssue = null  // Reset only issue selection
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
                    text = Strings["no_projects"],
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hours
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { input: String ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            val newValue = input.take(4) // Limit to 4 characters (e.g., "7.50")
                            val floatValue = newValue.toFloatOrNull()
                            if (floatValue == null || floatValue <= 7.5f) {
                                hours = newValue
                            }
                        }
                    },
                    modifier = Modifier.width(200.dp).then(shortcutHandler),
                    label = { Text(Strings["hours_label"]) },
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
                    Text(Strings["full_day"])
                }
            }

            if (hours.isNotEmpty() && hours.toFloatOrNull() == null) {
                Text(
                    text = Strings["invalid_number"],
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            } else if (hours.isNotEmpty() && hours.toFloat() <= 0f) {
                Text(
                    text = Strings["hours_must_be_positive"],
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            } else if (hours.isNotEmpty() && hours.toFloat() > 7.5f) {
                Text(
                    text = Strings["hours_max_value"],
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
                            Strings["loading_issues_for_project"].format(project.name)
                        else
                            Strings["loading_issues"],
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
                            Strings["no_issues_for_project"].format(project.name)
                        else
                            Strings["no_issues_available"],
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
                onValueChange = { newValue: String ->
                    if (newValue.length <= 255) {
                        comments = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth().then(shortcutHandler),
                label = { Text(Strings["comments_label"]) },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keyboard shortcuts help
            Text(
                text = Strings["keyboard_shortcuts"],
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Button(
                onClick = { saveEntry() },
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
                    Text(if (timeEntry?.id == null) Strings["add_entry"] else Strings["update_entry"])
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun main() {
    // Set default locale to French
    Locale.setDefault(Locale.FRENCH)

    Strings

    application {
        startKoin {
            val config = ConfigurationManager.loadConfig()
            properties(
                mapOf(
                    "redmine.uri" to config.redmineUri,
                    "redmine.username" to config.username,
                    "redmine.password" to config.password
                )
            )
            modules(appModule)
        }

        val redmineClient = koinInject<RedmineClient>()

        Window(
            onCloseRequest = ::exitApplication,
            title = Strings["window_title"],
            onKeyEvent = KeyShortcutManager::handleKeyEvent,
            state = rememberWindowState(width = 1070.dp, height = 870.dp)
        ) {
            App(redmineClient)
        }
    }
}
