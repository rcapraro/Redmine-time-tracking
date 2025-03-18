package com.ps.redmine

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ps.redmine.api.RedmineClient
import com.ps.redmine.components.ConfigurationDialog
import com.ps.redmine.components.DatePicker
import com.ps.redmine.components.SearchableDropdown
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

    // Add a state variable to track the current language
    var currentLanguage by remember { mutableStateOf(ConfigurationManager.loadConfig().language) }

    // Create a state variable for the current locale based on the language
    val currentLocale = remember(currentLanguage) {
        when (currentLanguage.lowercase()) {
            "en" -> Locale.ENGLISH
            else -> Locale.FRENCH
        }
    }

    // Update Strings object when language changes
    LaunchedEffect(currentLanguage) {
        Strings.updateLanguage(currentLanguage)
        // Update locale
        Locale.setDefault(currentLocale)
    }

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
                        message = Strings["operation_error"].format(e.message),
                        duration = SnackbarDuration.Long
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
                actionLabel = Strings["dismiss"],
                duration = SnackbarDuration.Long
            )
            errorMessage = null
        }
    }

    // Custom typography with harmonized font sizes
    val customTypography = Typography(
        h6 = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp // Reduced from default 20sp
        ),
        subtitle1 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp // Harmonized
        ),
        subtitle2 = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp // Harmonized
        ),
        body1 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp // Harmonized
        ),
        body2 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp // Harmonized
        ),
        caption = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp // Harmonized
        )
    )

    // Load configuration to get theme preference
    val config = remember { ConfigurationManager.loadConfig() }
    var isDarkTheme by remember { mutableStateOf(config.isDarkTheme) }

    // Define light and dark color schemes
    val lightColorScheme = lightColors(
        secondary = Color(0xFF00897B) // Darker teal color for better visibility
    )

    val darkColorScheme = darkColors(
        primary = Color(0xFF90CAF9), // Light blue
        primaryVariant = Color(0xFF64B5F6), // Lighter blue
        secondary = Color(0xFF80CBC4), // Light teal
        background = Color(0xFF121212), // Dark background
        surface = Color(0xFF1E1E1E), // Dark surface
        onPrimary = Color(0xFF000000), // Black text on primary
        onSecondary = Color(0xFF000000), // Black text on secondary
        onBackground = Color(0xFFFFFFFF), // White text on background
        onSurface = Color(0xFFFFFFFF) // White text on surface
    )

    // Use the appropriate color scheme based on the theme preference
    val colorScheme = if (isDarkTheme) darkColorScheme else lightColorScheme

    MaterialTheme(
        typography = customTypography,
        colors = colorScheme
    ) {
        if (showConfigDialog) {
            ConfigurationDialog(
                redmineClient = redmineClient,
                onDismiss = { showConfigDialog = false },
                onConfigSaved = {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(Strings["configuration_saved"])
                        // Reload configuration to get updated preferences
                        val newConfig = ConfigurationManager.loadConfig()
                        isDarkTheme = newConfig.isDarkTheme
                        // Update the language state to trigger recomposition
                        currentLanguage = newConfig.language
                        // Reload data with new configuration
                        loadTimeEntries(currentMonth)
                    }
                }
            )
        }
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                // Use key parameter to force recomposition when language changes
                key(currentLanguage) {
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
                }
            },
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(8.dp)
            ) {
                // Left panel - Time entries list
                Box(
                    modifier = Modifier.weight(1.5f).fillMaxHeight()
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
                                        // Use key parameter to force recomposition when language changes
                                        key(currentLanguage) {
                                            Text(Strings["nav_previous"])
                                        }
                                    }
                                    Text(
                                        text = currentMonth.format(currentLocale),
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
                                        // Use key parameter to force recomposition when language changes
                                        key(currentLanguage) {
                                            Text(Strings["nav_next"])
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Use key parameter to force recomposition when language changes
                                    key(currentLanguage) {
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
                                }
                                // Use key parameter to force recomposition when language changes
                                key(currentLanguage) {
                                    Text(
                                        text = Strings["nav_help"],
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
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
                                    text = Strings["total_hours_format"].format(totalHours),
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
                            // Use key parameter to force recomposition when language changes
                            key(currentLanguage) {
                                TimeEntriesList(
                                    timeEntries = timeEntries,
                                    selectedTimeEntry = selectedTimeEntry,
                                    onTimeEntrySelected = { selectedTimeEntry = it },
                                    onDelete = { entry -> deleteTimeEntry(entry) },
                                    deletingEntryId = deletingEntryId,
                                    locale = currentLocale
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right panel - Time entry details
                Box(
                    modifier = Modifier.weight(1.3f).fillMaxHeight()
                ) {
                    // Use key parameter to force recomposition when language changes
                    key(currentLanguage) {
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
                                            message = Strings["operation_error"].format(e.message),
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            },
                            onCancel = { selectedTimeEntry = null },
                            locale = currentLocale
                        )
                    }
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
    onCancel: () -> Unit = {},
    locale: Locale = Locale.getDefault()
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
        mutableStateOf(timeEntry?.issue)
    }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingIssues by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
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
            title = { Text(Strings["discard_changes_title"], modifier = Modifier.heightIn(min = 24.dp)) },
            text = { Text(Strings["discard_changes_message"], modifier = Modifier.heightIn(min = 24.dp)) },
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
                modifier = Modifier.width(200.dp).heightIn(min = 56.dp),
                locale = locale
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
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            SearchableDropdown(
                items = projects,
                selectedItem = selectedProject,
                onItemSelected = { project: Project ->
                    selectedProject = project
                    selectedIssue = null  // Reset only issue selection
                },
                itemText = { project: Project -> project.name },
                label = { Text(Strings["project_label"]) },
                isError = !isLoading && projects.isNotEmpty() && selectedProject == null,
                enabled = !isLoading,
                isLoading = isLoading,
                noItemsText = Strings["no_projects"]
            )
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
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
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
                    modifier = Modifier.width(200.dp).heightIn(min = 56.dp).then(shortcutHandler),
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
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            SearchableDropdown(
                items = issues,
                selectedItem = selectedIssue,
                onItemSelected = { issue: Issue ->
                    selectedIssue = issue
                },
                itemText = { issue: Issue -> "#${issue.id} - ${issue.subject}" },
                label = { Text(Strings["issue_label"]) },
                placeholder = { Text(Strings["select_issue_placeholder"]) },
                isError = !isLoading && issues.isNotEmpty() && selectedIssue == null,
                enabled = !isLoading,
                isLoading = isLoadingIssues,
                noItemsText = Strings["no_issues_available"]
            )
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
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            SearchableDropdown(
                items = activities,
                selectedItem = selectedActivity,
                onItemSelected = { activity: Activity ->
                    selectedActivity = activity
                },
                itemText = { activity: Activity -> activity.name },
                label = { Text(Strings["activity_label"]) },
                isError = !isLoading && activities.isNotEmpty() && selectedActivity == null,
                enabled = !isLoading,
                isLoading = isLoading,
                noItemsText = Strings["no_activities"]
            )
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
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
            OutlinedTextField(
                value = comments,
                onValueChange = { newValue: String ->
                    if (newValue.length <= 255) {
                        comments = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).then(shortcutHandler),
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
    // Load configuration
    val config = ConfigurationManager.loadConfig()

    // Set default locale based on language configuration
    val locale = when (config.language.lowercase()) {
        "en" -> Locale.ENGLISH
        else -> Locale.FRENCH
    }
    Locale.setDefault(locale)

    // Update Strings with the configured language
    Strings.updateLanguage(config.language)

    application {
        startKoin {
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
            state = rememberWindowState(width = 1100.dp, height = 900.dp)
        ) {
            App(redmineClient)
        }
    }
}
