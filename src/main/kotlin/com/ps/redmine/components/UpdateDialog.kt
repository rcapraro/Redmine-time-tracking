package com.ps.redmine.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ps.redmine.resources.Strings
import com.ps.redmine.update.UpdateInfo
import java.net.URI

/**
 * Dialog component for displaying update notifications and handling update downloads.
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo?,
    onDismiss: () -> Unit
) {
    if (updateInfo != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = Strings["update_available_title"],
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Strings["update_version_format"].format(updateInfo.version),
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Divider()

                    // Release notes
                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Column {
                            Text(
                                text = Strings["update_release_notes"],
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = MaterialTheme.colors.surface,
                                elevation = 2.dp
                            ) {
                                Text(
                                    text = updateInfo.releaseNotes,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .heightIn(max = 200.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }

                    // Links inside content
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (updateInfo.releasePageUrl != null) {
                            TextButton(onClick = { openUrl(updateInfo.releasePageUrl) }) {
                                Text(Strings["update_open_release_page"])
                            }
                        }
                        if (updateInfo.downloadUrl != null) {
                            Button(onClick = { openUrl(updateInfo.downloadUrl) }) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Strings["update_download_for_os"])
                            }
                        }
                    }

                    // Bottom action: only Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(Strings["close"])
                        }
                    }

                    // Warning message if no download URL
                    if (updateInfo.downloadUrl == null) {
                        Card(
                            backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Strings["update_no_download_available"],
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openUrl(url: String) {
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            val desktop = java.awt.Desktop.getDesktop()
            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
    } catch (_: Exception) {
        // Ignore failures silently; user can copy link from release page
    }
}

/**
 * Small update indicator that can be shown in the main UI.
 */
@Composable
fun UpdateIndicator(
    hasUpdate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hasUpdate) {
        IconButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Badge(
                backgroundColor = MaterialTheme.colors.error
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = Strings["update_available_title"],
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}