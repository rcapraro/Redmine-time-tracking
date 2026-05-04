package com.ps.redmine

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ps.redmine.api.KtorRedmineClient
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.components.*
import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.di.appModule
import com.ps.redmine.model.Activity
import com.ps.redmine.model.Issue
import com.ps.redmine.model.Project
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.resources.Strings
import com.ps.redmine.ui.RedmineTimeTheme
import com.ps.redmine.update.UpdateManager
import com.ps.redmine.util.*
import com.ps.redmine.util.KeyShortcut
import com.ps.redmine_time.generated.resources.Res
import com.ps.redmine_time.generated.resources.app_icon
import kotlinx.coroutines.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

/**
 * Validates that a string is a valid hours input (digits, optional . or , decimal, max 1 fractional digit).
 * Hoisted out of the onValueChange lambda so it isn't recompiled on every keystroke.
 */
private val HOURS_INPUT_REGEX = Regex("^\\d*([\\.,]\\d{0,1})?$")

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
            e is KtorRedmineClient.RedmineApiException && e.statusCode != 422
}

/**
 * Routes an exception to the appropriate UI surface:
 * - 422 validation errors → toast (short, inline-actionable).
 * - Connection / unknown errors → modal ErrorDialog with stacktrace.
 */
fun handleException(
    e: Exception,
    notifier: Notifier,
    errorDialogMessage: (String) -> Unit,
    errorDialogDetails: (String) -> Unit,
    showErrorDialog: (Boolean) -> Unit
) {
    val isValidationError = e is KtorRedmineClient.RedmineApiException && e.statusCode == 422
    if (isValidationError) {
        notifier.error(e.message ?: Strings["error_dialog_message"])
        return
    }

    val userMessage = if (isConnectionError(e)) {
        Strings["error_api_unreachable"]
    } else {
        Strings["error_dialog_message"]
    }
    errorDialogMessage(userMessage)
    errorDialogDetails("${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}")
    showErrorDialog(true)
}

@Composable
fun App(
    redmineClient: RedmineClientInterface,
) {
    val updateManager: UpdateManager = koinInject()

    var selectedTimeEntry by remember { mutableStateOf<TimeEntry?>(null) }
    var timeEntries by remember { mutableStateOf<List<TimeEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) } // For data loading
    var isGlobalLoading by remember { mutableStateOf(false) } // For global loading (config changes)
    var deletingEntryId by remember { mutableStateOf<Int?>(null) }
    var selectedEntryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showBulkEditDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var configVersion by remember { mutableStateOf(0) }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }
    var errorDialogDetails by remember { mutableStateOf<String?>(null) }

    // Update state
    val updateState by updateManager.updateState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = rememberNotifier(snackbarHostState)

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

    // Start update checks when app launches
    LaunchedEffect(Unit) {
        updateManager.startPeriodicUpdateChecks()
        updateManager.checkForUpdates()
    }

    // Display name of the authenticated Redmine user, resolved from the API key.
    // Reloaded when the configuration changes (configVersion bumps).
    var userDisplayName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(configVersion) {
        userDisplayName = redmineClient.getCurrentUser()?.displayName
    }

    // Wall-clock state ticking once per second so the date/time/week shown in the
    // top bar stay current without the user having to refresh anything.
    var clockNow by remember { mutableStateOf(nowLocalDateTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            clockNow = nowLocalDateTime()
        }
    }

    // Non-working days (Mon–Fri) from configuration
    var nonWorkingIsoDays by remember { mutableStateOf(ConfigurationManager.loadConfig().nonWorkingIsoDays) }

    // Cleanup update manager when app is disposed
    DisposableEffect(Unit) {
        onDispose {
            updateManager.cleanup()
        }
    }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val totalHours = remember(timeEntries) { timeEntries.sumOf { it.hours.toDouble() } }

    // Working-day arithmetic for the current month — lifted so onSave can detect a completion transition.
    val effectiveDays = remember(currentMonth, nonWorkingIsoDays) {
        val firstK = kotlinx.datetime.LocalDate(currentMonth.year, currentMonth.monthValue, 1)
        val lastK = kotlinx.datetime.LocalDate(
            currentMonth.year,
            currentMonth.monthValue,
            lengthOfMonth(currentMonth.year, currentMonth.monthValue)
        )
        countWorkingDays(firstK, lastK, nonWorkingIsoDays)
    }
    val expectedHours = effectiveDays * WorkHours.configuredDailyHours()
    val isCompleted = expectedHours > 0 && totalHours >= expectedHours

    // Confetti trigger key — incrementing fires a fresh burst.
    var confettiTrigger by remember { mutableStateOf(0) }

    // Selected calendar date used to initialize the date field when creating a new entry
    var selectedDate by remember { mutableStateOf(com.ps.redmine.util.today) }

    // Compute the earliest non-complete working day in the given month
    fun computeFirstIncompleteDate(
        yearMonth: YearMonth,
        entries: List<TimeEntry>,
        excludedIsoDays: Set<Int>
    ): LocalDate {
        val firstDay = yearMonth.atDay(1)
        val lastDay = if (yearMonth.monthValue == 12) {
            kotlinx.datetime.LocalDate(yearMonth.year + 1, 1, 1).minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        } else {
            kotlinx.datetime.LocalDate(yearMonth.year, yearMonth.monthValue + 1, 1)
                .minus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }

        var d = firstDay
        var firstWorkingDay: LocalDate? = null
        while (d <= lastDay) {
            val iso = d.dayOfWeek.isoDayNumber
            if (iso in 1..5 && !excludedIsoDays.contains(iso)) {
                if (firstWorkingDay == null) firstWorkingDay = d
                val totalForDay = entries
                    .asSequence()
                    .filter { it.date == d }
                    .sumOf { it.hours.toDouble() }
                    .toFloat()
                if (totalForDay < WorkHours.configuredDailyHours()) {
                    return d
                }
            }
            d = d.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }
        return firstWorkingDay ?: today
    }

    fun loadTimeEntries(yearMonth: YearMonth) {
        scope.launch {
            isLoading = true
            try {
                timeEntries = redmineClient.getTimeEntriesForMonth(
                    yearMonth.year,
                    yearMonth.monthValue
                )
                // After loading, set selectedDate to first non-complete day of the month
                selectedDate = computeFirstIncompleteDate(yearMonth, timeEntries, nonWorkingIsoDays)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(
                    e,
                    notifier,
                    { errorDialogMessage = it },
                    { errorDialogDetails = it },
                    { showErrorDialog = it }
                )
                // Clear the time entries list when an error occurs
                timeEntries = emptyList()
                // In case of error, keep selectedDate unchanged
            } finally {
                isLoading = false
                // If this was triggered by a config change, also update isGlobalLoading
                if (isGlobalLoading) {
                    isGlobalLoading = false
                }
            }
        }
    }

    fun resolveDuplicateDates(timeEntry: TimeEntry, target: DuplicateTarget): List<LocalDate> {
        fun isWorkingDay(d: LocalDate): Boolean {
            val iso = d.dayOfWeek.isoDayNumber
            return iso in 1..5 && iso !in nonWorkingIsoDays
        }
        return when (target) {
            DuplicateTarget.SameDay -> listOf(timeEntry.date)
            DuplicateTarget.NextDay -> {
                var d = timeEntry.date.plus(1, DateTimeUnit.DAY)
                while (!isWorkingDay(d)) d = d.plus(1, DateTimeUnit.DAY)
                listOf(d)
            }

            is DuplicateTarget.Range -> buildList {
                var d = target.from
                while (d <= target.to) {
                    if (isWorkingDay(d)) add(d)
                    d = d.plus(1, DateTimeUnit.DAY)
                }
            }
        }
    }

    fun duplicateTimeEntry(timeEntry: TimeEntry, target: DuplicateTarget) {
        val dates = resolveDuplicateDates(timeEntry, target)
        if (dates.isEmpty()) {
            notifier.warning(Strings["duplicate_range_no_working_days"])
            return
        }
        scope.launch {
            try {
                val previousTotal = totalHours
                coroutineScope {
                    dates.map { date ->
                        async { redmineClient.createTimeEntry(timeEntry.copy(id = null, date = date)) }
                    }.awaitAll()
                }
                timeEntries = redmineClient.getTimeEntriesForMonth(
                    currentMonth.year,
                    currentMonth.monthValue
                )
                val message = if (dates.size == 1) {
                    Strings["entry_created"]
                } else {
                    Strings["duplicate_entries_created"].format(dates.size)
                }
                notifier.success(message)

                val newTotal = timeEntries.sumOf { it.hours.toDouble() }
                if (expectedHours > 0 && previousTotal < expectedHours && newTotal >= expectedHours) {
                    confettiTrigger++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(
                    e,
                    notifier,
                    { errorDialogMessage = it },
                    { errorDialogDetails = it },
                    { showErrorDialog = it }
                )
            }
        }
    }

    fun deleteTimeEntry(timeEntry: TimeEntry) {
        scope.launch {
            timeEntry.id?.let { id ->
                deletingEntryId = id
                try {
                    redmineClient.deleteTimeEntry(id)
                    notifier.success(Strings["time_entry_deleted"])
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleException(
                        e,
                        notifier,
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

    fun bulkDeleteSelected() {
        val ids = selectedEntryIds.toList()
        if (ids.isEmpty()) return
        // Optimistic UI: remove rows + clear selection immediately so the user
        // sees the result while the network calls are still in flight.
        timeEntries = timeEntries.filter { it.id !in ids }
        selectedEntryIds = emptySet()
        selectedTimeEntry = null
        scope.launch {
            try {
                coroutineScope {
                    ids.map { id -> async { redmineClient.deleteTimeEntry(id) } }.awaitAll()
                }
                notifier.success(Strings["bulk_entries_deleted"].format(ids.size))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(
                    e,
                    notifier,
                    { errorDialogMessage = it },
                    { errorDialogDetails = it },
                    { showErrorDialog = it }
                )
            } finally {
                loadTimeEntries(currentMonth)
            }
        }
    }

    fun bulkUpdateSelected(
        newProject: Project?,
        newActivity: Activity?,
        newIssue: Issue?,
        newHours: Float?,
        newComments: String?,
    ) {
        val targets = timeEntries.filter { it.id != null && it.id in selectedEntryIds }
        if (targets.isEmpty()) return
        selectedEntryIds = emptySet()
        scope.launch {
            try {
                val previousTotal = totalHours
                coroutineScope {
                    targets.map { entry ->
                        async {
                            val updated = entry.copy(
                                project = newProject ?: entry.project,
                                activity = newActivity ?: entry.activity,
                                issue = newIssue ?: entry.issue,
                                hours = newHours ?: entry.hours,
                                comments = newComments ?: entry.comments,
                            )
                            redmineClient.updateTimeEntry(updated)
                        }
                    }.awaitAll()
                }
                timeEntries = redmineClient.getTimeEntriesForMonth(
                    currentMonth.year,
                    currentMonth.monthValue
                )
                notifier.success(Strings["bulk_entries_updated"].format(targets.size))

                val newTotal = timeEntries.sumOf { it.hours.toDouble() }
                if (expectedHours > 0 && previousTotal < expectedHours && newTotal >= expectedHours) {
                    confettiTrigger++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(
                    e,
                    notifier,
                    { errorDialogMessage = it },
                    { errorDialogDetails = it },
                    { showErrorDialog = it }
                )
            }
        }
    }

    LaunchedEffect(currentMonth) {
        selectedEntryIds = emptySet()
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

    // Surface update-check failures once per error-transition (avoids spamming on periodic checks).
    LaunchedEffect(updateState.error) {
        updateState.error?.let {
            notifier.warning(Strings["update_check_failed"])
            updateManager.clearError()
        }
    }

    // Load configuration to get theme preference
    val config = remember { ConfigurationManager.loadConfig() }
    var isDarkTheme by remember { mutableStateOf(config.isDarkTheme) }

    RedmineTimeTheme(darkTheme = isDarkTheme) {
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
                        notifier.success(Strings["configuration_saved"])
                        // Reload configuration to get updated preferences
                        val newConfig = ConfigurationManager.loadConfig()
                        isDarkTheme = newConfig.isDarkTheme
                        nonWorkingIsoDays = newConfig.nonWorkingIsoDays
                        // Update the language state to trigger recomposition
                        currentLanguage = newConfig.language
                        // No-op: removed onLanguageChanged behavior

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
                },
                onError = { notifier.error(Strings["error_saving_config"]) }
            )
        }

        // Update dialog
        if (updateState.showUpdateDialog) {
            UpdateDialog(
                updateInfo = updateState.availableUpdate,
                onDismiss = {
                    updateManager.dismissUpdateDialog()
                }
            )
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(8.dp)
                ) { data -> TypedSnackbar(data) }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                // Top bar — user identity + live clock on the left, update/settings on the right.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        userDisplayName = userDisplayName,
                        clockNow = clockNow,
                        locale = currentLocale,
                        languageKey = currentLanguage,
                    )
                    key(currentLanguage) {
                        ActionPill(
                            hasUpdate = updateState.availableUpdate != null,
                            onUpdateClick = {
                                if (updateState.availableUpdate != null) {
                                    updateManager.showUpdateDialog()
                                }
                            },
                            onSettingsClick = { showConfigDialog = true },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp).padding(bottom = 4.dp)
                ) {
                    // Weekly progress bars on the far left
                    // Use key parameter to force recomposition when language or non-working days change
                    key(currentLanguage, nonWorkingIsoDays) {
                        WeeklyProgressBars(
                            timeEntries = timeEntries,
                            currentMonth = currentMonth,
                            excludedIsoDays = nonWorkingIsoDays,
                            modifier = Modifier.padding(2.dp).focusProperties { canFocus = false },
                            onWeekClick = { weekInfo ->
                                selectedDate = weekInfo.startDate
                            }
                        )
                    }

                    // Left panel - Time entries list (wrapped in card)
                    Surface(
                        modifier = Modifier.weight(1.5f).fillMaxHeight().padding(4.dp)
                            .focusProperties { canFocus = false },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp)) {
                            // Month navigation and total hours
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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
                                            modifier = Modifier
                                                .size(32.dp)
                                                .alpha(if (isLoading || isGlobalLoading) 0.6f else 1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = Strings["nav_previous"],
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        AnimatedContent(
                                            targetState = currentMonth,
                                            transitionSpec = {
                                                val forward = targetState > initialState
                                                val direction = if (forward) 1 else -1
                                                (slideInHorizontally(tween(280)) { it * direction } + fadeIn(tween(280))) togetherWith
                                                        (slideOutHorizontally(tween(280)) { -it * direction } + fadeOut(
                                                            tween(280)
                                                        ))
                                            },
                                            label = "monthLabel",
                                        ) { month ->
                                            Text(
                                                text = "${
                                                    LocaleNames.monthName(
                                                        month.monthValue,
                                                        currentLocale,
                                                        full = true
                                                    )
                                                } ${month.year}",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (!isLoading && !isGlobalLoading) {
                                                    currentMonth = currentMonth.plusMonths(1)
                                                    selectedTimeEntry = null
                                                }
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .alpha(if (isLoading || isGlobalLoading) 0.6f else 1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = Strings["nav_next"],
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    // Use key parameter to force recomposition when language changes
                                    key(currentLanguage) {
                                        TextButton(
                                            onClick = {
                                                if (!isLoading && !isGlobalLoading) {
                                                    currentMonth = YearMonth.now()
                                                    selectedTimeEntry = null
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .heightIn(min = 24.dp)
                                                .alpha(if (isLoading || isGlobalLoading) 0.6f else 1f)
                                        ) {
                                            Text(
                                                text = Strings["today_shortcut"],
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                    }
                                    // Use key parameter to force recomposition when language changes
                                    key(currentLanguage) {
                                        Text(
                                            text = Strings["nav_help"],
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
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
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = Strings["total_hours_format"].format(totalHours),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Monthly progress indicator
                                val completionPercentage = if (expectedHours > 0) {
                                    (totalHours / expectedHours * 100).coerceAtMost(100.0)
                                } else {
                                    0.0
                                }

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
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = if (isCompleted) {
                                                Strings["month_completed"]
                                            } else {
                                                Strings["completion_percentage"].format(completionPercentage)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { (completionPercentage / 100).toFloat() },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).height(6.dp),
                                        color = if (isCompleted) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        drawStopIndicator = {},
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${Strings["working_days"]} $effectiveDays",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${Strings["expected_hours"]} ${
                                                Strings["total_hours_format"].format(
                                                    expectedHours
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (!isCompleted && expectedHours > totalHours) {
                                        val remainingHours = expectedHours - totalHours
                                        Text(
                                            text = Strings["hours_remaining"].format(remainingHours),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            BulkActionBar(
                                count = selectedEntryIds.size,
                                onClear = { selectedEntryIds = emptySet() },
                                onEdit = { showBulkEditDialog = true },
                                onDelete = { showBulkDeleteConfirm = true },
                            )

                            Crossfade(
                                targetState = isLoading && !isGlobalLoading && deletingEntryId == null,
                                animationSpec = tween(220),
                                label = "loadingCrossfade",
                            ) { showLoading ->
                                if (showLoading) {
                                    TimeEntriesListSkeleton(modifier = Modifier.fillMaxSize())
                                } else {
                                    key(currentLanguage) {
                                        TimeEntriesList(
                                            timeEntries = timeEntries,
                                            selectedTimeEntry = selectedTimeEntry,
                                            onTimeEntrySelected = { selectedTimeEntry = it },
                                            onDelete = { entry -> deleteTimeEntry(entry) },
                                            onDuplicate = { entry, target -> duplicateTimeEntry(entry, target) },
                                            selectedEntryIds = selectedEntryIds,
                                            onToggleSelect = { entry ->
                                                entry.id?.let { id ->
                                                    selectedEntryIds = if (id in selectedEntryIds) {
                                                        selectedEntryIds - id
                                                    } else {
                                                        selectedEntryIds + id
                                                    }
                                                }
                                            },
                                            deletingEntryId = deletingEntryId,
                                            locale = currentLocale
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right panel - Time entry details (wrapped in card)
                    Surface(
                        modifier = Modifier.weight(1.3f).fillMaxHeight().padding(4.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        // Use key parameter to force recomposition when language changes
                        key(currentLanguage) {
                            // Manage focus reset to Date field after save/cancel
                            var focusRequestKey by remember { mutableStateOf(0) }

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
                                            val previousTotal = totalHours
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
                                            notifier.success(message)

                                            // Fire confetti when this save flips the month from incomplete to complete
                                            val newTotal = timeEntries.sumOf { it.hours.toDouble() }
                                            if (expectedHours > 0 && previousTotal < expectedHours && newTotal >= expectedHours) {
                                                confettiTrigger++
                                            }

                                            // Trigger focus back to Date field for next entry
                                            focusRequestKey++
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            handleException(
                                                e,
                                                notifier,
                                                { errorDialogMessage = it },
                                                { errorDialogDetails = it },
                                                { showErrorDialog = it }
                                            )
                                        }
                                    }
                                },
                                onCancel = {
                                    selectedTimeEntry = null
                                    // Trigger focus back to Date field when cancelling entry
                                    focusRequestKey++
                                },
                                locale = currentLocale,
                                configVersion = configVersion,
                                isGlobalLoading = isGlobalLoading,
                                focusRequestKey = focusRequestKey,
                                initialDate = selectedDate,
                                onError = { notifier.error(it) }
                            )
                        }
                    }
                }
            }

            // Show loading overlay when reloading after configuration changes
            if (isGlobalLoading) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .semantics { contentDescription = "Global Loading Indicator" },
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 4.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp,
                                )
                                Text(
                                    text = Strings["loading"],
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
            }

            // Festive overlay when the month is fully filled in.
            key(currentLanguage) {
                ConfettiOverlay(
                    triggerKey = confettiTrigger,
                    title = Strings["month_completed_celebration_title"],
                    subtitle = Strings["month_completed_celebration_subtitle"],
                    modifier = Modifier.padding(innerPadding),
                )
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

        if (showBulkDeleteConfirm) {
            ConfirmDialog(
                title = Strings["bulk_delete_title"],
                message = Strings["bulk_delete_message"].format(selectedEntryIds.size),
                confirmLabel = Strings["confirm_delete_yes"],
                dismissLabel = Strings["confirm_delete_no"],
                destructive = true,
                onConfirm = {
                    showBulkDeleteConfirm = false
                    bulkDeleteSelected()
                },
                onDismiss = { showBulkDeleteConfirm = false },
            )
        }

        if (showBulkEditDialog) {
            val selectedEntries = remember(timeEntries, selectedEntryIds) {
                timeEntries.filter { it.id != null && it.id in selectedEntryIds }
            }
            BulkEditDialog(
                selectedEntries = selectedEntries,
                redmineClient = redmineClient,
                onDismiss = { showBulkEditDialog = false },
                onApply = { newProject, newActivity, newIssue, newHours, newComments ->
                    showBulkEditDialog = false
                    bulkUpdateSelected(newProject, newActivity, newIssue, newHours, newComments)
                },
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
    isGlobalLoading: Boolean = false,
    focusRequestKey: Int = 0,
    initialDate: kotlinx.datetime.LocalDate,
    onError: (String) -> Unit = {}
) {
    var date by remember(timeEntry, initialDate) {
        mutableStateOf(
            timeEntry?.date ?: initialDate
        )
    }
    // Keep date in sync with selected initialDate when creating a new entry
    LaunchedEffect(initialDate, timeEntry) {
        if (timeEntry == null) {
            date = initialDate
        }
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
            val normalizedHours = hours.replace(',', '.')
            val validHours = normalizedHours.toFloatOrNull() != null
            val nonZeroHours = normalizedHours.toFloatOrNull() != 0f
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
                            val parsedHours = hours.replace(',', '.').toFloatOrNull() ?: 0f
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
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // onSave handles its own exceptions; this is a defensive
                            // fallback for unexpected synchronous errors
                            onError(e.message ?: Strings["error_dialog_message"])
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
            date = initialDate
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
            // Get projects that have open issues only
            projects = redmineClient.getProjectsWithOpenIssues()
            // Activities will be loaded when a project is selected
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(Strings["error_loading_projects"])
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(Strings["error_loading_issues_activities"])
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
                        date = initialDate
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
            event.isSaveShortcut() && isValid && !isLoading && !isSaving && !isGlobalLoading -> {
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

    // Make the right pane scrollable with a visible scrollbar so the bottom action button is always reachable on small heights
    val scrollState = rememberScrollState()

    // Ensure the Date field is the first to receive focus in the right pane
    val dateFocusRequester = remember { FocusRequester() }
    LaunchedEffect(timeEntry, isGlobalLoading, focusRequestKey) {
        if (!isGlobalLoading) {
            // Defer to give Compose time to lay out the node tree, then request focus
            dateFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(scrollState)
                .then(shortcutHandler)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (timeEntry == null) Strings["add_time_entry"] else Strings["edit_time_entry"],
                    style = MaterialTheme.typography.titleLarge
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
                    modifier = Modifier.width(200.dp).heightIn(min = 48.dp).focusRequester(dateFocusRequester),
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
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
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
                            if (input.isEmpty() || input.matches(HOURS_INPUT_REGEX)) {
                                val normalized = input.replace(',', '.')
                                val floatValue = normalized.toFloatOrNull()
                                val maxHours = WorkHours.configuredDailyHours()
                                if (floatValue == null || floatValue <= maxHours) {
                                    hours = input
                                }
                            }
                        },
                        modifier = Modifier.width(200.dp).heightIn(min = 48.dp).then(shortcutHandler),
                        label = { Text(Strings["hours_label"]) },
                        isError = hours.isNotEmpty() && run {
                            val normalized = hours.replace(',', '.')
                            (normalized.toFloatOrNull() == null || normalized.toFloat() <= 0f || normalized.toFloat() > WorkHours.configuredDailyHours())
                        },
                        singleLine = true,
                        enabled = !isLoading && !isGlobalLoading,
                        trailingIcon = {
                            if (hours.isNotEmpty()) {
                                IconButton(
                                    onClick = { hours = "" },
                                    enabled = !isLoading && !isGlobalLoading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = Strings["clear_button_description"],
                                    )
                                }
                            }
                        }
                    )

                    TextButton(
                        onClick = { hours = Strings["total_hours_format"].format(WorkHours.configuredDailyHours()) },
                        enabled = !isLoading && !isGlobalLoading
                    ) {
                        Text(Strings["full_day"])
                    }
                }

                run {
                    val normalized = hours.replace(',', '.')
                    if (hours.isNotEmpty() && normalized.toFloatOrNull() == null) {
                        Text(
                            text = Strings["invalid_number"],
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else if (hours.isNotEmpty() && normalized.toFloat() <= 0f) {
                        Text(
                            text = Strings["hours_must_be_positive"],
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    } else if (hours.isNotEmpty() && normalized.toFloat() > WorkHours.configuredDailyHours()) {
                        val maxHours = WorkHours.configuredDailyHours()
                        Text(
                            text = Strings["hours_max_value"].format(maxHours),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
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
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    selectedProject == null -> {
                        Text(
                            text = Strings["select_project_first"],
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    issues.isEmpty() -> {
                        // Snapshot state values once so the branch body sees a consistent view
                        val currentIssue = selectedIssue
                        val project = selectedProject
                        val text = when {
                            currentIssue != null -> Strings["showing_issue"].format(currentIssue.id)
                            project != null -> Strings["no_issues_for_project"].format(project.name)
                            else -> Strings["no_issues_available"]
                        }
                        Text(
                            text = text,
                            color = if (currentIssue != null) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
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
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
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
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = Strings["clear_button_description"],
                                )
                            }
                        }
                    } else null
                )

                if (!isLoading && comments.isEmpty()) {
                    Text(
                        text = Strings["comments_required"],
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = Strings["char_count"].format(comments.length),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (comments.length > 240) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
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
            icon = painterResource(Res.drawable.app_icon),
            onKeyEvent = KeyShortcutManager::handleKeyEvent,
            state = rememberWindowState(width = 1100.dp, height = 900.dp),
        ) {
            App(redmineClient)
        }
    }
}

@Composable
private fun StatusPill(
    userDisplayName: String?,
    clockNow: LocalDateTime,
    locale: Locale,
    languageKey: String,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            userDisplayName?.let { name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UserAvatar(name)
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                StatusDivider()
            }
            key(languageKey) {
                InfoChip(
                    icon = Icons.Outlined.CalendarToday,
                    text = "${
                        LocaleNames.weekdayName(
                            clockNow.date.dayOfWeek.isoDayNumber,
                            locale,
                            full = false
                        )
                    } ${clockNow.date.day} ${
                        LocaleNames.monthName(
                            clockNow.date.month.number,
                            locale,
                            full = false
                        )
                    }",
                )
            }
            StatusDivider()
            InfoChip(
                icon = Icons.Outlined.Schedule,
                text = "%02d:%02d".format(clockNow.hour, clockNow.minute),
            )
            StatusDivider()
            key(languageKey) {
                InfoChip(
                    icon = Icons.Outlined.DateRange,
                    text = Strings["week_label"].format(isoWeekNumber(clockNow.date)),
                )
            }
        }
    }
}

@Composable
private fun StatusDivider() {
    VerticalDivider(
        modifier = Modifier.height(18.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ActionPill(
    hasUpdate: Boolean,
    onUpdateClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (hasUpdate) {
                ActionChip(
                    icon = Icons.Default.Download,
                    text = Strings["update"],
                    contentDescription = Strings["update_available_title"],
                    onClick = onUpdateClick,
                    badged = true,
                )
                StatusDivider()
            }
            ActionChip(
                icon = Icons.Default.Settings,
                text = Strings["settings"],
                contentDescription = Strings["settings"],
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    badged: Boolean = false,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (badged) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UserAvatar(name: String) {
    val initials = remember(name) { computeInitials(name) }
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun computeInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
