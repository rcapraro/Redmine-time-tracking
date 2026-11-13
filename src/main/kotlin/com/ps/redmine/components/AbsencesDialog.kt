package com.ps.redmine.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.lucca.*
import com.ps.redmine.model.TimeEntry
import com.ps.redmine.model.User
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.WorkHours
import com.ps.redmine.util.atDay
import com.ps.redmine.util.lengthOfMonth
import com.ps.redmine.util.monthValue
import com.ps.redmine.util.plusMonths
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

private sealed interface AbsencesState {
    data object Loading : AbsencesState
    data class SignedOut(val sessionExpired: Boolean) : AbsencesState
    data object SigningIn : AbsencesState
    data class Hidden(val message: String) : AbsencesState

    // ownerName is set when the absences belong to the impersonated user rather than to the signed-in one.
    data class Loaded(
        val months: List<MonthAbsences>,
        val entries: List<TimeEntry>?,
        val ownerName: String? = null,
    ) : AbsencesState
    data class Failed(val message: String) : AbsencesState
}

private data class MonthAbsences(val month: YearMonth, val days: List<AbsenceDay>)

private data class ImportOutcome(val added: Int, val updated: Int, val deleted: Int, val failures: List<String>)

@Composable
fun AbsencesDialog(
    luccaClient: LuccaClient,
    redmineClient: RedmineClientInterface,
    month: YearMonth,
    locale: Locale,
    impersonatedUser: User?,
    onTimeEntriesChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    var state by remember { mutableStateOf<AbsencesState>(AbsencesState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }
    var targets by remember { mutableStateOf(AbsenceTargetStore.load()) }
    var editingType by remember { mutableStateOf<String?>(null) }
    var confirmingConflicts by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<ImportOutcome?>(null) }
    var targetsSaveFailed by remember { mutableStateOf(false) }
    val dailyHours = remember { WorkHours.configuredDailyHours() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(month, reloadKey, impersonatedUser) {
        state = AbsencesState.Loading
        state = loadAbsences(luccaClient, redmineClient, month, impersonatedUser)
    }

    val loaded = state as? AbsencesState.Loaded
    val canImport = loaded?.entries != null
    val plans = remember(loaded, targets, canImport) {
        val comparable = if (canImport) loaded.entries.orEmpty() else emptyList()
        val entriesByMonth = comparable.groupBy { it.date.yearMonth }
        loaded?.months.orEmpty().map { m ->
            m.month to planAbsenceImport(m.days, entriesByMonth[m.month].orEmpty(), targets, dailyHours)
        }
    }
    val allItems = plans.flatMap { it.second }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previousStatuses by remember { mutableStateOf<Map<String, AbsenceImportStatus>>(emptyMap()) }
    LaunchedEffect(plans) {
        selected = reconcileSelection(allItems, previousStatuses, selected)
        previousStatuses = allItems.associate { it.key to it.status }
    }
    val selectedItems = allItems.filter { it.isSelectable && it.key in selected }
    val replacedCount = selectedItems.count { it.status is AbsenceImportStatus.DifferentHours }
    val (selectedDeletions, selectedWrites) = selectedItems.partition { it.isDeletion }
    val deletedCount = selectedDeletions.size
    val exceedingDays = daysExceedingFullDay(selectedItems, loaded?.entries.orEmpty(), dailyHours)

    fun apply() {
        applying = true
        scope.launch {
            outcome = applyAbsenceImport(redmineClient, selectedItems)
            applying = false
            onTimeEntriesChanged()
            reloadKey++
        }
    }

    if (state == AbsencesState.SigningIn) {
        LuccaLoginWindow(
            luccaClient = luccaClient,
            onLoggedIn = { session ->
                scope.launch {
                    state = try {
                        withContext(Dispatchers.IO) { LuccaSessionStore.save(session) }
                        reloadKey++
                        AbsencesState.Loading
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AbsencesState.Failed("${Strings["lucca_keychain_error"]}\n${e.message.orEmpty()}")
                    }
                }
            },
            onDismiss = { state = AbsencesState.SignedOut(sessionExpired = false) },
            onBrowserUnavailable = { e ->
                state = AbsencesState.Failed("${Strings["lucca_browser_unavailable"]}\n${e.message.orEmpty()}")
            },
        )
    }

    editingType?.let { type ->
        AbsenceMappingDialog(
            leaveType = type,
            redmineClient = redmineClient,
            initial = targets[type],
            onSave = { target ->
                val updated = targets + (type to target)
                targetsSaveFailed = !AbsenceTargetStore.save(updated)
                targets = updated
                editingType = null
            },
            onDismiss = { editingType = null },
        )
    }

    if (confirmingConflicts) {
        ConfirmDialog(
            title = Strings["absence_confirm_title"],
            message = buildList {
                if (replacedCount > 0) add(Strings["absence_confirm_replaced"].format(replacedCount))
                if (deletedCount > 0) add(Strings["absence_confirm_deleted"].format(deletedCount))
                if (exceedingDays.isNotEmpty()) {
                    add(Strings["absence_confirm_overbooked"].format(formatHours(dailyHours, locale), exceedingDays.size))
                }
            }.joinToString("\n"),
            confirmLabel = Strings["absence_confirm_action"],
            dismissLabel = Strings["cancel"],
            onConfirm = {
                confirmingConflicts = false
                apply()
            },
            onDismiss = { confirmingConflicts = false },
            icon = Icons.Outlined.Warning,
        )
    }

    Dialog(
        onDismissRequest = { if (!applying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.width(880.dp).padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column {
                DialogHeader(
                    subtitle = if (loaded != null) {
                        Strings["absence_dialog_subtitle"].format(
                            monthLabel(loaded.months.first().month, locale),
                            monthLabel(loaded.months.last().month, locale),
                        )
                    } else {
                        Strings["absence_dialog_subtitle_short"]
                    },
                    showLogout = state is AbsencesState.Loaded || state is AbsencesState.Failed,
                    logoutEnabled = !applying,
                    onLogout = {
                        scope.launch {
                            state = if (clearSession()) AbsencesState.SignedOut(sessionExpired = false)
                            else AbsencesState.Failed(Strings["lucca_keychain_error"])
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Box(modifier = Modifier.fillMaxWidth().height(600.dp)) {
                    when (val current = state) {
                        AbsencesState.Loading -> CenteredState {
                            CircularProgressIndicator()
                            StateText(Strings["absence_loading"])
                        }

                        AbsencesState.SigningIn -> CenteredState {
                            CircularProgressIndicator()
                            StateText(Strings["lucca_login_in_progress"])
                        }

                        is AbsencesState.SignedOut -> CenteredState {
                            Icon(
                                Icons.AutoMirrored.Outlined.Login,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(Strings["lucca_signed_out_title"], style = MaterialTheme.typography.titleMedium)
                            StateText(
                                if (current.sessionExpired) Strings["lucca_session_expired"]
                                else Strings["lucca_login_prompt"]
                            )
                            Button(onClick = { state = AbsencesState.SigningIn }) { Text(Strings["lucca_login"]) }
                        }

                        is AbsencesState.Hidden -> CenteredState {
                            Icon(
                                Icons.Outlined.PersonOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(Strings["absence_hidden_title"], style = MaterialTheme.typography.titleMedium)
                            StateText(current.message)
                        }

                        is AbsencesState.Failed -> Box(Modifier.padding(24.dp)) {
                            Banner(Icons.Outlined.ErrorOutline, current.message, Tone.Error)
                        }

                        is AbsencesState.Loaded -> {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                current.ownerName?.let { name ->
                                    Banner(Icons.Outlined.SupervisorAccount, Strings["absence_for_user"].format(name), Tone.Neutral)
                                }
                                outcome?.let { OutcomeBanner(it) }
                                if (targetsSaveFailed) {
                                    Banner(Icons.Outlined.ErrorOutline, Strings["absence_targets_save_error"], Tone.Error)
                                }
                                if (current.entries == null) {
                                    Banner(Icons.Outlined.CloudOff, Strings["absence_import_redmine_unavailable"], Tone.Warning)
                                }
                                val leaveTypes = current.months
                                    .flatMap { m -> m.days.flatMap { d -> d.slots.map { it.accountName } } }
                                    .distinct()
                                    .sorted()
                                if (canImport && leaveTypes.isNotEmpty()) {
                                    LeaveTypeTargets(leaveTypes, targets, onEdit = { editingType = it })
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SectionTitle(Strings["absence_months_title"])
                                    plans.forEach { (planMonth, items) ->
                                        AbsenceMonthSection(
                                            month = planMonth,
                                            items = items,
                                            canImport = canImport,
                                            selected = selected,
                                            onSelectedChange = { selected = it },
                                            locale = locale,
                                            dailyHours = dailyHours,
                                        )
                                    }
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(scrollState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val showImport = canImport && allItems.any { it.isSelectable }
                    if (showImport) {
                        Text(
                            text = selectionSummary(selectedWrites, selectedDeletions, locale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (showImport) {
                        TextButton(onClick = onDismiss, enabled = !applying) { Text(Strings["close"]) }
                        Button(
                            enabled = selectedItems.isNotEmpty() && !applying,
                            onClick = {
                                if (replacedCount > 0 || deletedCount > 0 || exceedingDays.isNotEmpty()) {
                                    confirmingConflicts = true
                                } else {
                                    apply()
                                }
                            },
                        ) {
                            if (applying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalContentColor.current,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(Strings["absence_import_running"])
                            } else {
                                Icon(Icons.Outlined.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(Strings["absence_import_action"])
                            }
                        }
                    } else {
                        Button(onClick = onDismiss) { Text(Strings["close"]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(subtitle: String, showLogout: Boolean, logoutEnabled: Boolean, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.BeachAccess,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(Strings["absence_dialog_title"], style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showLogout) {
            TextButton(onClick = onLogout, enabled = logoutEnabled) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(Strings["lucca_logout"])
            }
        }
    }
}

@Composable
private fun CenteredState(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun StateText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 420.dp),
    )
}

private suspend fun loadAbsences(
    luccaClient: LuccaClient,
    redmineClient: RedmineClientInterface,
    month: YearMonth,
    impersonatedUser: User?,
): AbsencesState = try {
    val session = withContext(Dispatchers.IO) { LuccaSessionStore.load() }
    val ownerId = when {
        session == null -> null
        impersonatedUser == null -> session.userId
        else -> findLuccaPerson(impersonatedUser, luccaClient.planningPeople(session.cookies))?.id
    }
    when {
        session == null -> AbsencesState.SignedOut(sessionExpired = false)
        ownerId == null -> AbsencesState.Hidden(Strings["absence_not_in_planning"].format(impersonatedUser?.displayName))
        else -> coroutineScope {
            val months = listOf(month, month.plusMonths(1))
            val entries = async { redmineEntries(redmineClient, months) }
            val from = months.first().atDay(1)
            val to = months.last().let { it.atDay(it.lengthOfMonth()) }
            val days = groupAbsencesByDay(luccaClient.leaves(session.cookies, ownerId, from, to))
            AbsencesState.Loaded(
                months = months.map { m ->
                    MonthAbsences(m, days.filter { it.date.yearMonth == m })
                },
                entries = entries.await(),
                ownerName = impersonatedUser?.displayName,
            )
        }
    }
} catch (e: CancellationException) {
    throw e
} catch (e: LuccaApiException) {
    if (impersonatedUser != null && e.statusCode == HttpStatusCode.Forbidden.value) {
        AbsencesState.Hidden(Strings["absence_not_allowed"].format(impersonatedUser.displayName))
    } else {
        AbsencesState.Failed("${Strings["lucca_error"]}\n${e.message.orEmpty()}")
    }
} catch (_: LuccaSessionExpiredException) {
    clearSession()
    AbsencesState.SignedOut(sessionExpired = true)
} catch (e: LuccaKeychainException) {
    AbsencesState.Failed("${Strings["lucca_keychain_error"]}\n${e.message.orEmpty()}")
} catch (e: Exception) {
    AbsencesState.Failed("${Strings["lucca_error"]}\n${e.message.orEmpty()}")
}

// Null when Redmine cannot be reached.
private suspend fun redmineEntries(redmineClient: RedmineClientInterface, months: List<YearMonth>): List<TimeEntry>? =
    try {
        coroutineScope {
            months.map { async { redmineClient.getTimeEntriesForMonth(it.year, it.monthValue) } }.awaitAll().flatten()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

private suspend fun applyAbsenceImport(
    redmineClient: RedmineClientInterface,
    items: List<AbsenceImportItem>,
): ImportOutcome {
    var added = 0
    var updated = 0
    var deleted = 0
    val failures = mutableListOf<String>()
    val failedDeletionDays = mutableSetOf<LocalDate>()
    // Deleting first keeps a day from briefly holding both a cancelled absence and its replacement, which Redmine
    // would truncate; a day whose deletion failed gets no new hours for the same reason.
    items.sortedBy { !it.isDeletion }.forEach { item ->
        val target = item.target ?: return@forEach
        if (!item.isDeletion && item.date in failedDeletionDays) {
            failures += "${item.date} : ${Strings["absence_import_skipped_after_delete"]}"
            return@forEach
        }
        try {
            when (val status = item.status) {
                is AbsenceImportStatus.Cancelled -> {
                    redmineClient.deleteTimeEntry(checkNotNull(status.existing.id))
                    deleted++
                }

                is AbsenceImportStatus.DifferentHours -> {
                    redmineClient.updateTimeEntry(status.existing.copy(hours = item.hours))
                    updated++
                }

                else -> {
                    redmineClient.createTimeEntry(
                        TimeEntry(
                            date = item.date,
                            hours = item.hours,
                            activity = target.activity,
                            project = target.project,
                            issue = target.issue,
                            comments = item.slots.joinToString(", ") {
                                it.accountName + ABSENCE_COMMENT_SEPARATOR + periodLabel(it.period)
                            },
                        )
                    )
                    added++
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failures += "${item.date} : ${e.message.orEmpty()}"
            if (item.isDeletion) failedDeletionDays += item.date
        }
    }
    return ImportOutcome(added, updated, deleted, failures)
}

private fun selectionSummary(
    writes: List<AbsenceImportItem>,
    deletions: List<AbsenceImportItem>,
    locale: Locale,
): String {
    fun hoursOf(items: List<AbsenceImportItem>) = formatHours(items.sumOf { it.hours.toDouble() }.toFloat(), locale)
    val parts = buildList {
        if (writes.isNotEmpty()) add(Strings["absence_selection_summary"].format(writes.size, hoursOf(writes)))
        if (deletions.isNotEmpty()) add(Strings["absence_selection_deletions"].format(deletions.size, hoursOf(deletions)))
    }
    return parts.joinToString(" · ").ifEmpty { Strings["absence_selection_empty"] }
}

private suspend fun clearSession(): Boolean = try {
    withContext(Dispatchers.IO) { LuccaSessionStore.clear() }
    JcefRuntime.clearCookies()
    true
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    false
}

@Composable
private fun OutcomeBanner(outcome: ImportOutcome) {
    val hasFailures = outcome.failures.isNotEmpty()
    Banner(
        icon = if (hasFailures) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
        text = Strings["absence_import_done"].format(outcome.added, outcome.updated, outcome.deleted) +
                if (hasFailures) " · " + Strings["absence_import_failed"].format(outcome.failures.size) else "",
        tone = if (hasFailures) Tone.Error else Tone.Primary,
        details = outcome.failures,
    )
}
