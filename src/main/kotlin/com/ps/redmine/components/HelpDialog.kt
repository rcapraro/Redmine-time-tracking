package com.ps.redmine.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.OSUtils

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Card(
            modifier = Modifier
                .width(680.dp)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = Strings["help_title"],
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HelpSection(title = Strings["help_overview_title"]) {
                        Text(
                            text = Strings["help_overview_body"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    HelpSection(title = Strings["help_entries_title"]) {
                        ActionRow(Icons.Filled.Edit, Strings["help_action_edit"])
                        ActionRow(Icons.Filled.AddCircleOutline, Strings["help_action_new"])
                        ActionRow(Icons.Filled.ContentCopy, Strings["help_action_duplicate"])
                        ActionRow(
                            icon = Icons.Filled.Delete,
                            text = Strings["help_action_delete"],
                            iconTint = MaterialTheme.colorScheme.error,
                        )
                    }

                    HelpSection(title = Strings["help_bulk_title"]) {
                        ActionRow(Icons.Filled.Checklist, Strings["help_bulk_select"])
                        ActionRow(Icons.Filled.Edit, Strings["help_bulk_edit_action"])
                        ActionRow(
                            icon = Icons.Filled.Delete,
                            text = Strings["help_bulk_delete_action"],
                            iconTint = MaterialTheme.colorScheme.error,
                        )
                    }

                    HelpSection(title = Strings["help_navigation_title"]) {
                        Text(
                            text = Strings["help_navigation_body"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    HelpShortcutsSection()

                    HelpSection(title = Strings["help_impersonation_title"]) {
                        ActionRow(
                            icon = Icons.Filled.SwitchAccount,
                            text = Strings["help_impersonation_body"],
                            iconTint = MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    HelpSection(title = Strings["help_settings_title"]) {
                        ActionRow(Icons.Filled.Settings, Strings["help_settings_body"])
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onDismiss) {
                        Text(Strings["close"])
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HelpShortcutsSection() {
    val shortcuts = listOf(
        OSUtils.formatShortcut("S", useModifier = true) to Strings["help_shortcut_save"],
        "Esc" to Strings["help_shortcut_cancel"],
        "Alt+←" to Strings["help_shortcut_prev_month"],
        "Alt+→" to Strings["help_shortcut_next_month"],
        "Alt+T" to Strings["help_shortcut_today"],
    )
    HelpSection(title = Strings["help_shortcuts_title"]) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.small,
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shortcuts.forEach { (keys, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = keys,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(120.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
