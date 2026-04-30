package com.ps.redmine.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings

/**
 * A dialog that displays an error message in a user-friendly way.
 * It shows a human-readable message by default and provides a button to show/hide technical details.
 *
 * @param errorMessage The human-readable error message to display
 * @param technicalDetails The technical details (e.g., stacktrace) to show when requested
 * @param onDismiss Callback to be invoked when the dialog is dismissed
 */
@Composable
fun ErrorDialog(
    errorMessage: String,
    technicalDetails: String? = null,
    onDismiss: () -> Unit
) {
    var showTechnicalDetails by remember { mutableStateOf(false) }
    val detailsScroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(Strings["error_dialog_title"])
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!technicalDetails.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showTechnicalDetails = !showTechnicalDetails },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (showTechnicalDetails) Strings["hide_details"] else Strings["show_details"]
                        )
                    }

                    if (showTechnicalDetails) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(detailsScroll)
                            ) {
                                Text(
                                    text = technicalDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(Strings["close"])
            }
        }
    )
}
