package com.ps.redmine.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colors.error
                )
                Text(Strings["error_dialog_title"])
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main error message
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.body1
                )

                // Technical details section
                if (!technicalDetails.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Button to show/hide technical details
                    TextButton(
                        onClick = { showTechnicalDetails = !showTechnicalDetails },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (showTechnicalDetails) Strings["hide_details"] else Strings["show_details"]
                        )
                    }

                    // Technical details content
                    if (showTechnicalDetails) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colors.surface.copy(alpha = 0.7f),
                            elevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = technicalDetails,
                                    style = MaterialTheme.typography.caption
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
