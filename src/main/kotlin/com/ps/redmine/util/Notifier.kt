package com.ps.redmine.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class NotificationKind { Success, Error, Warning, Info }

/**
 * Centralizes user-facing notifications shown via the Material [SnackbarHostState].
 *
 * - Encodes the [NotificationKind] in [SnackbarData.actionLabel] so [TypedSnackbar] can style it.
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
 * [SnackbarData.actionLabel] by [Notifier]. Falls back to [NotificationKind.Info] if absent or unknown.
 */
@Composable
fun TypedSnackbar(data: SnackbarData) {
    val kind = runCatching { NotificationKind.valueOf(data.actionLabel ?: "") }
        .getOrDefault(NotificationKind.Info)
    val background = when (kind) {
        NotificationKind.Success -> MaterialTheme.colors.primary
        NotificationKind.Error -> MaterialTheme.colors.error
        NotificationKind.Warning -> Color(0xFFF9A825)
        NotificationKind.Info -> MaterialTheme.colors.surface
    }
    val foreground = when (kind) {
        NotificationKind.Success -> MaterialTheme.colors.onPrimary
        NotificationKind.Error -> MaterialTheme.colors.onError
        NotificationKind.Warning -> Color.Black
        NotificationKind.Info -> MaterialTheme.colors.onSurface
    }
    val icon = when (kind) {
        NotificationKind.Success -> Icons.Filled.CheckCircle
        NotificationKind.Error -> Icons.Filled.Error
        NotificationKind.Warning -> Icons.Filled.Warning
        NotificationKind.Info -> Icons.Filled.Info
    }

    Snackbar(
        modifier = Modifier.alpha(0.95f),
        backgroundColor = background,
        contentColor = foreground,
        elevation = ElevationTokens.High,
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
                text = data.message,
                style = MaterialTheme.typography.body2,
                color = foreground
            )
        }
    }
}
