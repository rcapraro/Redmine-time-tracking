package com.ps.redmine.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings
import com.ps.redmine.ui.LocalIsDarkTheme
import com.ps.redmine.ui.WarningAccentDark
import com.ps.redmine.ui.WarningAccentLight
import com.ps.redmine.ui.WarningOnAccentDark
import com.ps.redmine.ui.WarningOnAccentLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class NotificationKind { Success, Error, Warning, Info }

/**
 * Centralizes user-facing notifications shown via the Material [SnackbarHostState].
 *
 * - Encodes the [NotificationKind] in [SnackbarData.visuals.actionLabel] so [TypedSnackbar] can style it.
 * - Cancels any in-flight snackbar when a new one arrives (no queue, no stacking).
 * - Drops a duplicate of the most recent message if it arrives within the dedup window.
 */
class Notifier(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
    private val dedupWindowMs: Long = 750L,
) {
    private var currentJob: Job? = null
    private var lastMessage: String? = null
    private var lastTimestampMs: Long = 0L

    fun success(message: String) = show(NotificationKind.Success, message)
    fun error(message: String) = show(NotificationKind.Error, message)
    fun warning(message: String) = show(NotificationKind.Warning, message)
    fun info(message: String) = show(NotificationKind.Info, message)

    private fun show(kind: NotificationKind, message: String) {
        val now = System.currentTimeMillis()
        if (lastMessage == message && now - lastTimestampMs < dedupWindowMs) return
        lastMessage = message
        lastTimestampMs = now

        currentJob?.cancel()
        currentJob = scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(
                message = message,
                actionLabel = kind.name,
                duration = when (kind) {
                    NotificationKind.Success, NotificationKind.Info -> SnackbarDuration.Short
                    NotificationKind.Warning, NotificationKind.Error -> SnackbarDuration.Long
                }
            )
        }
    }
}

@Composable
fun rememberNotifier(hostState: SnackbarHostState): Notifier {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { Notifier(hostState, scope) }
}

/**
 * Renders a [Snackbar] styled according to the [NotificationKind] encoded in
 * [SnackbarData.visuals.actionLabel] by [Notifier]. Falls back to [NotificationKind.Info] if absent or unknown.
 */
@Composable
fun TypedSnackbar(data: SnackbarData) {
    val kind = runCatching { NotificationKind.valueOf(data.visuals.actionLabel ?: "") }
        .getOrDefault(NotificationKind.Info)
    val isDark = LocalIsDarkTheme.current
    val warningBg = if (isDark) WarningAccentDark else WarningAccentLight
    val warningFg = if (isDark) WarningOnAccentDark else WarningOnAccentLight
    val background = when (kind) {
        NotificationKind.Success -> MaterialTheme.colorScheme.primary
        NotificationKind.Error -> MaterialTheme.colorScheme.error
        NotificationKind.Warning -> warningBg
        NotificationKind.Info -> MaterialTheme.colorScheme.inverseSurface
    }
    val foreground = when (kind) {
        NotificationKind.Success -> MaterialTheme.colorScheme.onPrimary
        NotificationKind.Error -> MaterialTheme.colorScheme.onError
        NotificationKind.Warning -> warningFg
        NotificationKind.Info -> MaterialTheme.colorScheme.inverseOnSurface
    }
    val icon = when (kind) {
        NotificationKind.Success -> Icons.Filled.CheckCircle
        NotificationKind.Error -> Icons.Filled.Error
        NotificationKind.Warning -> Icons.Filled.Warning
        NotificationKind.Info -> Icons.Filled.Info
    }

    Snackbar(
        containerColor = background,
        contentColor = foreground,
        actionContentColor = foreground,
        dismissActionContentColor = foreground,
        action = {
            IconButton(
                onClick = { data.dismiss() },
                modifier = Modifier.semantics { contentDescription = Strings["close"] }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = foreground
                )
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = foreground
            )
        }
    }
}
