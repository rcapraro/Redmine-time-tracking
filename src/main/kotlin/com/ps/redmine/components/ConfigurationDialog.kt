package com.ps.redmine.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.resources.Strings
import java.util.*

// Helper function to get the application version
private fun getAppVersion(): String {
    return try {
        // Try to load the Version class dynamically
        val versionClass = Class.forName("com.ps.redmine.Version")
        val versionField = versionClass.getDeclaredField("VERSION")
        versionField.get(null) as String
    } catch (e: Exception) {
        // If the Version class is not available, return a default version
        "dev"
    }
}

@Composable
fun ConfigurationDialog(
    redmineClient: RedmineClientInterface,
    onDismiss: () -> Unit,
    onConfigSaved: (redmineConfigChanged: Boolean, languageChanged: Boolean, themeChanged: Boolean) -> Unit
) {
    var config by remember { mutableStateOf(ConfigurationManager.loadConfig()) }
    var showError by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var showApiKeyHelp by remember { mutableStateOf(false) }

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


                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = config.apiKey,
                            onValueChange = { config = config.copy(apiKey = it) },
                            label = { Text(Strings["api_key"]) },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            singleLine = true,
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )
                        TextButton(
                            onClick = { apiKeyVisible = !apiKeyVisible },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(if (apiKeyVisible) Strings["hide_api_key"] else Strings["show_api_key"])
                        }
                    }

                    // API Key help link
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showApiKeyHelp = true },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = Strings["api_key_help"],
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }

                // Theme switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings["dark_theme"],
                        style = MaterialTheme.typography.body1
                    )
                    Switch(
                        checked = config.isDarkTheme,
                        onCheckedChange = { config = config.copy(isDarkTheme = it) }
                    )
                }

                // Language selection
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings["language"],
                        style = MaterialTheme.typography.body1
                    )

                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf("fr", "en")
                    val languageLabels = mapOf(
                        "fr" to Strings["language_fr"],
                        "en" to Strings["language_en"]
                    )

                    Box {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(languageLabels[config.language] ?: config.language)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { language ->
                                DropdownMenuItem(
                                    onClick = {
                                        config = config.copy(language = language)
                                        expanded = false
                                    }
                                ) {
                                    Text(languageLabels[language] ?: language)
                                }
                            }
                        }
                    }
                }

                if (showError) {
                    Text(
                        text = Strings["configuration_error"],
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }

                // Display version at the bottom
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Strings["version"]}: ${getAppVersion()}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (config.redmineUri.isNotBlank() && config.apiKey.isNotBlank()) {
                        // Check what has changed
                        val oldConfig = ConfigurationManager.loadConfig()
                        val languageChanged = oldConfig.language != config.language
                        val themeChanged = oldConfig.isDarkTheme != config.isDarkTheme
                        val redmineConfigChanged = oldConfig.redmineUri != config.redmineUri ||
                                oldConfig.apiKey != config.apiKey

                        // Save to persistent storage
                        ConfigurationManager.saveConfig(config)

                        // Update RedmineClient if Redmine configuration changed
                        if (redmineConfigChanged) {
                            redmineClient.updateConfiguration(config.redmineUri, config.apiKey)
                        }

                        // Update language if changed
                        if (languageChanged) {
                            // Update Strings with the new language
                            Strings.updateLanguage(config.language)

                            // Update locale
                            val locale = when (config.language.lowercase()) {
                                "en" -> Locale.ENGLISH
                                else -> Locale.FRENCH
                            }
                            Locale.setDefault(locale)
                        }

                        // Notify parent about all changes
                        onConfigSaved(redmineConfigChanged, languageChanged, themeChanged)
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

    // API Key help dialog
    if (showApiKeyHelp) {
        AlertDialog(
            onDismissRequest = { showApiKeyHelp = false },
            title = { Text(Strings["api_key_help"]) },
            text = {
                Text(
                    text = Strings["api_key_help_content"],
                    style = MaterialTheme.typography.body1
                )
            },
            confirmButton = {
                TextButton(onClick = { showApiKeyHelp = false }) {
                    Text(Strings["close"])
                }
            }
        )
    }
}
