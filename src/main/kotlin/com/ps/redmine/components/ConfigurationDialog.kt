package com.ps.redmine.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ps.redmine.api.RedmineClientInterface
import com.ps.redmine.config.ConfigurationManager
import com.ps.redmine.resources.Strings
import java.util.Locale

// Helper function to get the application version
private fun getAppVersion(): String {
    return try {
        val versionClass = Class.forName("com.ps.redmine.Version")
        val versionField = versionClass.getDeclaredField("VERSION")
        versionField.get(null) as String
    } catch (_: Exception) {
        "dev"
    }
}

@Composable
fun ConfigurationDialog(
    redmineClient: RedmineClientInterface,
    onDismiss: () -> Unit,
    onConfigSaved: (redmineConfigChanged: Boolean, languageChanged: Boolean, themeChanged: Boolean) -> Unit,
    onError: (String) -> Unit = {}
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
                    OutlinedTextField(
                        value = config.apiKey,
                        onValueChange = { config = config.copy(apiKey = it) },
                        label = { Text(Strings["api_key"]) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (apiKeyVisible) Strings["hide_api_key"] else Strings["show_api_key"],
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showApiKeyHelp = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = Strings["api_key_help"],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Theme switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings["dark_theme"],
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = config.isDarkTheme,
                        onCheckedChange = { config = config.copy(isDarkTheme = it) }
                    )
                }

                // Language selection
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings["language"],
                        style = MaterialTheme.typography.bodyMedium
                    )

                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf("fr", "en")
                    val languageLabels = mapOf(
                        "fr" to Strings["language_fr"],
                        "en" to Strings["language_en"]
                    )

                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(languageLabels[config.language] ?: config.language)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(languageLabels[language] ?: language) },
                                    onClick = {
                                        config = config.copy(language = language)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Daily hours selection (6 .. 7.5 by 0.5)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings["hours_per_day"],
                        style = MaterialTheme.typography.bodyMedium
                    )
                    var expandedDaily by remember { mutableStateOf(false) }
                    val options = (8 downTo 0).map { it * 0.5f + 5.5f }.filter { it in 6.0f..7.5f }
                    Box {
                        TextButton(onClick = { expandedDaily = true }) {
                            Text(Strings["hours_format"].format(config.dailyHours))
                        }
                        DropdownMenu(expanded = expandedDaily, onDismissRequest = { expandedDaily = false }) {
                            options.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(Strings["hours_format"].format(v)) },
                                    onClick = {
                                        config = config.copy(dailyHours = v)
                                        expandedDaily = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Non-working days selection (Mon–Fri), max 4 days
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = Strings["non_working_days_label"],
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val dayOptions = listOf(
                        1 to Strings["monday"],
                        2 to Strings["tuesday"],
                        3 to Strings["wednesday"],
                        4 to Strings["thursday"],
                        5 to Strings["friday"]
                    )
                    val selectedCount = config.nonWorkingIsoDays.size
                    val limitReached = selectedCount >= 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dayOptions.forEach { (iso, label) ->
                            val selected = remember(config.nonWorkingIsoDays) { config.nonWorkingIsoDays.contains(iso) }
                            val enabled = selected || !limitReached
                            DayChip(
                                label = label,
                                selected = selected,
                                enabled = enabled,
                                onToggle = {
                                    val current = config.nonWorkingIsoDays.toMutableSet()
                                    if (selected) {
                                        current.remove(iso)
                                    } else if (current.size < 4) {
                                        current.add(iso)
                                    }
                                    config = config.copy(nonWorkingIsoDays = current)
                                }
                            )
                        }
                    }
                    val derivedWeekly = config.dailyHours * (5 - config.nonWorkingIsoDays.size)
                    Text(
                        text = Strings["working_weekly_hours"].format(derivedWeekly),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showError) {
                    Text(
                        text = Strings["configuration_error"],
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Strings["version"]}: ${getAppVersion()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (config.redmineUri.isNotBlank() && config.apiKey.isNotBlank()) {
                        val oldConfig = ConfigurationManager.loadConfig()
                        val languageChanged = oldConfig.language != config.language
                        val themeChanged = oldConfig.isDarkTheme != config.isDarkTheme
                        val redmineConfigChanged = oldConfig.redmineUri != config.redmineUri ||
                                oldConfig.apiKey != config.apiKey

                        try {
                            ConfigurationManager.saveConfig(config)
                            if (redmineConfigChanged) {
                                redmineClient.updateConfiguration(config.redmineUri, config.apiKey)
                            }
                            if (languageChanged) {
                                Strings.updateLanguage(config.language)
                                val locale = when (config.language.lowercase()) {
                                    "en" -> Locale.ENGLISH
                                    else -> Locale.FRENCH
                                }
                                Locale.setDefault(locale)
                            }
                            onConfigSaved(redmineConfigChanged, languageChanged, themeChanged)
                            onDismiss()
                        } catch (e: Exception) {
                            showError = true
                            onError(e.message ?: Strings["error_saving_config"])
                        }
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

    if (showApiKeyHelp) {
        AlertDialog(
            onDismissRequest = { showApiKeyHelp = false },
            title = { Text(Strings["api_key_help"]) },
            text = {
                Text(
                    text = Strings["api_key_help_content"],
                    style = MaterialTheme.typography.bodyMedium
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

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    OutlinedButton(
        onClick = onToggle,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
