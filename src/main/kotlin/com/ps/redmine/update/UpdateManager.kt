package com.ps.redmine.update

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/**
 * Manages the application update process including checking for updates
 * and coordinating the update UI.
 */
class UpdateManager(
    private val updateService: UpdateService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val preferences = Preferences.userRoot().node("com/ps/redmine/update")

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var updateCheckJob: Job? = null

    private companion object {
        const val MILLIS_PER_HOUR: Long = 3_600_000L
    }

    /**
     * Starts periodic update checks if enabled in configuration.
     */
    fun startPeriodicUpdateChecks() {
        if (!isUpdateCheckEnabled()) return

        updateCheckJob?.cancel()
        updateCheckJob = scope.launch {
            while (isActive) {
                checkForUpdates()
                delay(getUpdateCheckInterval())
            }
        }
    }

    /**
     * Stops periodic update checks.
     */
    fun stopPeriodicUpdateChecks() {
        updateCheckJob?.cancel()
        updateCheckJob = null
    }

    /**
     * Manually checks for updates.
     */
    suspend fun checkForUpdates() {
        if (_updateState.value.isChecking) return

        _updateState.value = _updateState.value.copy(isChecking = true)

        try {
            val updateInfo = updateService.checkForUpdates()

            _updateState.value = _updateState.value.copy(
                isChecking = false,
                availableUpdate = updateInfo,
                showUpdateDialog = false
            )

        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                isChecking = false,
                error = e.message
            )
        }
    }

    /**
     * Shows the update dialog if an update is available.
     */
    fun showUpdateDialog() {
        if (_updateState.value.availableUpdate != null) {
            _updateState.value = _updateState.value.copy(showUpdateDialog = true)
        }
    }

    /**
     * Dismisses the update dialog.
     */
    fun dismissUpdateDialog() {
        _updateState.value = _updateState.value.copy(showUpdateDialog = false)
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _updateState.value = _updateState.value.copy(error = null)
    }

    /**
     * Checks if update checking is enabled.
     */
    private fun isUpdateCheckEnabled(): Boolean {
        return preferences.getBoolean("auto_update_enabled", true)
    }

    /**
     * Gets the update check interval in milliseconds.
     */
    private fun getUpdateCheckInterval(): Long {
        val hours = preferences.getInt("update_check_interval_hours", 24)
        return hours * MILLIS_PER_HOUR // Convert to milliseconds
    }


    // Legacy "last update check" persistence removed

    /**
     * Cleans up resources.
     */
    fun cleanup() {
        scope.cancel()
        updateService.close()
    }
}

/**
 * Represents the current state of the update system.
 */
data class UpdateState(
    val isChecking: Boolean = false,
    val availableUpdate: UpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val error: String? = null
)
