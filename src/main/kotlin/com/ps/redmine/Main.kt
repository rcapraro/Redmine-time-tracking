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
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.components.*
import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.di.appModule
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.*
import com.ps.redmine.util.KeyShortcut
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.time.YearMonth
import java.util.*
import kotlin.time.Clock

/**
 * Checks if an exception is a connection error.
 *
 * @param e The exception to check
 * @return true if the exception is a connection error, false otherwise
 */
fun isConnectionError(e: Exception): Boolean {
    return e is java.net.ConnectException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException ||
            e is java.io.IOException ||
            e.cause is java.net.ConnectException ||
            e.cause is java.net.SocketTimeoutException ||
            e.cause is java.net.UnknownHostException ||
            e.cause is java.io.IOException
}

/**
 * Handles an exception by showing an error dialog with appropriate message.
 *
 * @param e The exception to handle
 * @param errorDialogMessage Reference to the error dialog message state
 * @param errorDialogDetails Reference to the error dialog details state
 * @param showErrorDialog Reference to the show error dialog state
 */
fun handleException(
    e: Exception,
    errorDialogMessage: (String) -> Unit,
    errorDialogDetails: (String) -> Unit,
    showErrorDialog: (Boolean) -> Unit
) {
    e.printStackTrace()

    // Check if it's a connection error
    val isConnectionError = isConnectionError(e)

    // Check if it's a Redmine API error with status 422 (Unprocessable Entity)
    val isValidationError = e is com.ps.redmine.api.KtorRedmineClient.RedmineApiException &&
            e.statusCode == 422

    // For RedmineApiException, get additional details
    if (e is com.ps.redmine.api.KtorRedmineClient.RedmineApiException) {
        // Try to parse the error response for more details
        if (e.statusCode == 422) {
            val errorBody = e.responseBody

            // Check for specific error patterns
            val containsActivityError = errorBody.contains("Activité")
            val containsProjectError = errorBody.contains("Projet")
            val containsIssueError = errorBody.contains("Demande")
        }
    }

    // Set appropriate error message
    val userMessage = when {
        isConnectionError -> Strings["error_api_unreachable"]
        isValidationError -> {
            // For validation errors, provide a more specific message
            val errorMsg = e.message ?: ""
            if (errorMsg.contains("Activité") || errorMsg.contains("Projet") || errorMsg.contains("Demande")) {
                // This is the specific error we're trying to fix
                "Please ensure all required fields (Activity, Project, and Issue) are correctly selected."
            } else {
                // Other validation errors
                "The time entry could not be saved due to validation errors: ${e.message}"
            }
        }

        else -> Strings["error_dialog_message"]
    }

    errorDialogMessage(userMessage)

    // Store technical details
    val technicalDetails = "${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}"
    errorDialogDetails(technicalDetails)

    // Show error dialog
    showErrorDialog(true)
}

@Composable
fun App(redmineClient: RedmineClientInterface) {
    var selectedTimeEntry by remember { mutableStateOf<TimeEntry?>(null) }
    var timeEntries by remember { mutableStateOf<List<TimeEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) } // For data loading
    var isGlobalLoading by remember { mutableStateOf(false) } // For global loading (config changes)
    var deletingEntryId by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var configVersion by remember { mutableStateOf(0) }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }
    var errorDialogDetails by remember { mutableStateOf<String?>(null) }

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
                // Handle the exception
                handleException(
                    e,
                    { errorDialogMessage = it },
                    { errorDialogDetails = it },
                    { showErrorDialog = it }
                )
                // Clear the time entries list when an error occurs
                timeEntries = emptyList()
            } finally {
                isLoading = false
                // If this was triggered by a config change, also update isGlobalLoading
                if (isGlobalLoading) {
                    isGlobalLoading = false
                }
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
                    // Handle the exception
                    handleException(
                        e,
                        { errorDialogMessage = it },
                        { errorDialogDetails = it },
                        { showErrorDialog = it }
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
                onConfigSaved = { redmineConfigChanged, languageChanged, themeChanged ->
                    // Set isGlobalLoading to true immediately to show the loading overlay for any change
                    if (redmineConfigChanged || languageChanged || themeChanged) {
                        isGlobalLoading = true
                    }
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(Strings["configuration_saved"])
                        // Reload configuration to get updated preferences
                        val newConfig = ConfigurationManager.loadConfig()
                        isDarkTheme = newConfig.isDarkTheme
                        // Update the language state to trigger recomposition
                        currentLanguage = newConfig.language

                        // Only reload data and increment configVersion if Redmine configuration changed
                        if (redmineConfigChanged) {
                            // Increment configVersion to trigger reloading of dropdowns
                            configVersion++
                            // Reload data with new configuration
                            loadTimeEntries(currentMonth)
                        } else {
                            // For language or theme changes, we don't need to reload data
                            // Just set isGlobalLoading back to false after a short delay to allow UI to update
                            kotlinx.coroutines.delay(300)
                            isGlobalLoading = false
                        }
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
                modifier = Modifier.fillMaxSize().padding(4.dp)
            ) {
                // Left panel - Time entries list (wrapped in card)
                Surface(
                    modifier = Modifier.weight(1.5f).fillMaxHeight().padding(4.dp),
                    elevation = ElevationTokens.Medium,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
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
                                            if (!isLoading && !isGlobalLoading) {
                                                currentMonth = currentMonth.minusMonths(1)
                                                selectedTimeEntry = null
                                            }
                                        },
                                        modifier = Modifier.alpha(if (isLoading || isGlobalLoading) 0.6f else 1f)
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
                                            if (!isLoading && !isGlobalLoading) {
                                                currentMonth = currentMonth.plusMonths(1)
                                                selectedTimeEntry = null
                                            }
                                        },
                                        modifier = Modifier.alpha(if (isLoading || isGlobalLoading) 0.6f else 1f)
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
                                                if (!isLoading && !isGlobalLoading) {
                                                    currentMonth = YearMonth.now()
                                                    selectedTimeEntry = null
                                                }
                                            },
                                            modifier = Modifier.alpha(if (isLoading || isGlobalLoading) 0.6f else 1f)
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
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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

                            // Monthly progress indicator
                            val workingDays = remember(currentMonth) {
                                getWorkingDaysInMonth(currentMonth.year, currentMonth.monthValue)
                            }
                            val expectedHours = workingDays * 7.5
                            val completionPercentage = if (expectedHours > 0) {
                                (totalHours / expectedHours * 100).coerceAtMost(100.0)
                            } else {
                                0.0
                            }
                            val isCompleted = totalHours >= expectedHours

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Strings["monthly_progress"],
                                        style = MaterialTheme.typography.subtitle2
                                    )
                                    Text(
                                        text = if (isCompleted) {
                                            Strings["month_completed"]
                                        } else {
                                            Strings["completion_percentage"].format(completionPercentage)
                                        },
                                        style = MaterialTheme.typography.body2,
                                        color = if (isCompleted) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = (completionPercentage / 100).toFloat(),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    color = if (isCompleted) MaterialTheme.colors.secondary else MaterialTheme.colors.primary,
                                    backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${Strings["working_days"]} $workingDays",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "${Strings["expected_hours"]} ${
                                            Strings["total_hours_format"].format(
                                                expectedHours
                                            )
                                        }",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                if (!isCompleted && expectedHours > totalHours) {
                                    val remainingHours = expectedHours - totalHours
                                    Text(
                                        text = Strings["hours_remaining"].format(remainingHours),
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isLoading && !isGlobalLoading && deletingEntryId == null) {
                            // Only show the time entries list loading indicator when not showing the global loading indicator
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

                // Right panel - Time entry details (wrapped in card)
                Surface(
                    modifier = Modifier.weight(1.3f).fillMaxHeight().padding(4.dp),
                    elevation = ElevationTokens.Medium,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.surface
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
                                        // Handle the exception
                                        handleException(
                                            e,
                                            { errorDialogMessage = it },
                                            { errorDialogDetails = it },
                                            { showErrorDialog = it }
                                        )
                                    }
                                }
                            },
                            onCancel = { selectedTimeEntry = null },
                            locale = currentLocale,
                            configVersion = configVersion,
                            isGlobalLoading = isGlobalLoading
                        )
                    }
                }
            }

            // Show loading overlay when reloading after configuration changes
            if (isGlobalLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().semantics { contentDescription = "Global Loading Indicator" },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background.copy(alpha = 0.7f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = Strings["loading"],
                                style = MaterialTheme.typography.subtitle1
                            )
                        }
                    }
                }
            }
        }

        // Show error dialog when needed
        if (showErrorDialog) {
            ErrorDialog(
                errorMessage = errorDialogMessage,
                technicalDetails = errorDialogDetails,
                onDismiss = { showErrorDialog = false }
            )
        }
    }
}

// TimeEntriesList and TimeEntryItem moved to com.ps.redmine.components.TimeEntriesList

@Composable
fun TimeEntryDetail(
    timeEntry: TimeEntry?,
    redmineClient: RedmineClientInterface,
    onSave: (TimeEntry) -> Unit,
    onCancel: () -> Unit = {},
    locale: Locale = Locale.getDefault(),
    configVersion: Int = 0,
    isGlobalLoading: Boolean = false
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

    // Check if the form is valid and not in a loading state
    val isValid =
        remember(selectedProject, selectedActivity, hours, selectedIssue, comments, isLoading, isGlobalLoading) {
            val hasProject = selectedProject != null
            val hasActivity = selectedActivity != null
            val hasHours = hours.isNotEmpty()
            val validHours = hours.toFloatOrNull() != null
            val nonZeroHours = hours.toFloatOrNull() != 0f
            val hasIssue = selectedIssue != null
            val hasComments = comments.isNotEmpty()
            val notLoading = !isLoading && !isGlobalLoading

            hasProject && hasActivity && hasHours && validHours && nonZeroHours && hasIssue && hasComments && notLoading
        }

    val hasChanges = remember(hours, comments, selectedProject, selectedActivity, date, selectedIssue) {
        if (timeEntry == null) {
            hours.isNotEmpty() || comments.isNotEmpty() || selectedProject != null || selectedActivity != null || selectedIssue != null
        } else {
            hours != timeEntry.hours.toString() ||
                    comments != (timeEntry.comments ?: "") ||
                    selectedProject?.id != timeEntry.project.id ||
                    selectedActivity != timeEntry.activity ||
                    date != timeEntry.date ||
                    selectedIssue?.id != timeEntry.issue.id
        }
    }

    fun saveEntry() {
        if (!isValid || isSaving || isGlobalLoading) {
            return
        }

        selectedProject?.let { project ->
            selectedActivity?.let { activity ->
                selectedIssue?.let { issue ->
                    scope.launch {
                        isSaving = true
                        try {
                            val parsedHours = hours.toFloatOrNull() ?: 0f
                            val timeEntryToSave = TimeEntry(
                                id = timeEntry?.id,
                                date = date,
                                hours = parsedHours,
                                project = project,
                                issue = issue,
                                activity = activity,
                                comments = comments
                            )
                            onSave(timeEntryToSave)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isSaving = false
                        }
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

    // Load projects
    // Use redmineClient and configVersion as dependencies to reload when configuration changes
    LaunchedEffect(redmineClient, configVersion) {
        isLoading = true
        try {
            // Get projects in a single call
            projects = redmineClient.getProjectsWithActivities()
            // Activities will be loaded when a project is selected
        } catch (e: Exception) {
            println("Error loading data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Load activities and issues when project changes
    LaunchedEffect(selectedProject) {
        val project = selectedProject
        if (project != null) {
            isLoadingIssues = true
            try {
                // Load activities for the selected project
                activities = redmineClient.getActivitiesForProject(project.id)

                // Load issues for the selected project
                issues = redmineClient.getIssues(project.id)
            } catch (e: Exception) {
                println("Error loading issues or activities: ${e.message}")
                issues = emptyList()
                activities = emptyList()
            } finally {
                isLoadingIssues = false
            }
        } else {
            issues = emptyList()
            activities = emptyList()
            selectedIssue = null
            selectedActivity = null
        }
    }

    // Update selections when timeEntry changes or lists are loaded
    LaunchedEffect(timeEntry, projects) {
        if (timeEntry != null && projects.isNotEmpty()) {
            // Always update all fields when a time entry is selected
            selectedProject = projects.find { it.id == timeEntry.project.id }
            selectedIssue = timeEntry.issue
            // Note: hours and comments are already handled by remember(timeEntry) above
        } else if (timeEntry == null) {
            // Clear selections when no time entry is selected
            selectedProject = null
            selectedIssue = null
            selectedActivity = null
        }
    }

    // Update activity selection when activities list changes or timeEntry changes
    LaunchedEffect(activities, timeEntry) {
        if (timeEntry != null && activities.isNotEmpty()) {
            selectedActivity = activities.find { it.id == timeEntry.activity.id }
        } else if (timeEntry == null) {
            selectedActivity = null
        }
    }


    val keyboardHandler = Modifier.onPreviewKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown && event.key == Key.S && event.isMetaPressed -> {
                if (isValid && !isLoading && !isSaving && !isGlobalLoading) {
                    saveEntry()
                    true
                } else false
            }

            event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                if (!isGlobalLoading) {
                    handleCancel()
                    true
                } else false
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
            event.key == Key.S && event.isMetaPressed && isValid && !isLoading && !isSaving && !isGlobalLoading -> {
                saveEntry()
                true
            }

            event.key == Key.Escape -> {
                if (!isGlobalLoading) {
                    handleCancel()
                    true
                } else false
            }

            else -> false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
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
                enabled = !isLoading && !isSaving && !isGlobalLoading
            ) {
                Text(Strings["cancel_shortcut"])
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date picker and Today button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DatePicker(
                selectedDate = date,
                onDateSelected = { date = it },
                modifier = Modifier.width(200.dp).heightIn(min = 48.dp),
                locale = locale
            )

            TextButton(
                onClick = { date = today },
                enabled = !isLoading && !isSaving && !isGlobalLoading
            ) {
                Text(Strings["today"])
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Project dropdown
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
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
                enabled = !isLoading && !isGlobalLoading,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Hours
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
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
                    modifier = Modifier.width(200.dp).heightIn(min = 48.dp).then(shortcutHandler),
                    label = { Text(Strings["hours_label"]) },
                    isError = hours.isNotEmpty() && (hours.toFloatOrNull() == null || hours.toFloat() <= 0f || hours.toFloat() > 7.5f),
                    singleLine = true,
                    enabled = !isLoading && !isGlobalLoading,
                    trailingIcon = {
                        if (hours.isNotEmpty()) {
                            IconButton(
                                onClick = { hours = "" },
                                enabled = !isLoading && !isGlobalLoading
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
                    enabled = !isLoading && !isGlobalLoading
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

        Spacer(modifier = Modifier.height(6.dp))

        // Issue dropdown
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
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
                enabled = !isLoading && !isGlobalLoading,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Activity dropdown
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            SearchableDropdown(
                items = activities,
                selectedItem = selectedActivity,
                onItemSelected = { activity: Activity ->
                    selectedActivity = activity
                },
                itemText = { activity: Activity -> activity.name },
                label = { Text(Strings["activity_label"]) },
                isError = !isLoading && activities.isNotEmpty() && selectedActivity == null,
                enabled = !isLoading && !isGlobalLoading,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Comments
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)) {
            OutlinedTextField(
                value = comments,
                onValueChange = { newValue: String ->
                    if (newValue.length <= 255) {
                        comments = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).then(shortcutHandler),
                label = { Text(Strings["comments_label"]) },
                minLines = 5,
                enabled = !isLoading && !isGlobalLoading,
                isError = !isLoading && comments.isEmpty(),
                trailingIcon = if (comments.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { comments = "" },
                            enabled = !isLoading && !isGlobalLoading
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

            if (!isLoading && comments.isEmpty()) {
                Text(
                    text = Strings["comments_required"],
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
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

        Spacer(modifier = Modifier.height(10.dp))

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
                enabled = isValid && !isLoading && !isSaving && !isGlobalLoading
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
                    "redmine.apiKey" to config.apiKey
                )
            )
            modules(appModule)
        }

        val redmineClient = koinInject<RedmineClientInterface>()

        Window(
            onCloseRequest = {
                // Close the RedmineClient to release resources
                redmineClient.close()
                exitApplication()
            },
            title = Strings["window_title"],
            onKeyEvent = KeyShortcutManager::handleKeyEvent,
            state = rememberWindowState(width = 1100.dp, height = 900.dp),
        ) {
            App(redmineClient)
        }
    }
}
