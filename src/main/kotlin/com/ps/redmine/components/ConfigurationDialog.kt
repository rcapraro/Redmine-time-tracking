package com.ps.redmine.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ps.redmine.api.RedmineClient
import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.resources.Strings

@Composable
fun ConfigurationDialog(
    redmineClient: RedmineClient,
    onDismiss: () -> Unit,
    onConfigSaved: () -> Unit
) {
    var config by remember { mutableStateOf(ConfigurationManager.loadConfig()) }
    var showError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings["configuration_title"]) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = config.redmineUri,
                    onValueChange = { config = config.copy(redmineUri = it) },
                    label = { Text(Strings["redmine_uri"]) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = config.username,
                    onValueChange = { config = config.copy(username = it) },
                    label = { Text(Strings["username"]) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = config.password,
                        onValueChange = { config = config.copy(password = it) },
                        label = { Text(Strings["password"]) },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    TextButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(if (passwordVisible) Strings["hide_password"] else Strings["show_password"])
                    }
                }

                if (showError) {
                    Text(
                        text = Strings["configuration_error"],
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (config.redmineUri.isNotBlank() && config.username.isNotBlank() && config.password.isNotBlank()) {
                        // Save to persistent storage
                        ConfigurationManager.saveConfig(config)
                        // Update RedmineClient
                        redmineClient.updateConfiguration(config.redmineUri, config.username, config.password)
                        // Notify parent to reload data
                        onConfigSaved()
                        onDismiss()
                    } else {
                        showError = true
                    }
                }
            ) {
                Text(Strings["save"])
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["cancel"])
            }
        }
    )
}
