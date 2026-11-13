package com.ps.redmine.lucca

import kotlinx.serialization.json.Json
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

/** Persists, per Lucca leave type name, the Redmine target its absences are logged against. */
object AbsenceTargetStore {
    private const val KEY = "lucca.absenceTargets"
    private val preferences = Preferences.userRoot().node("com/ps/redmine")

    /** An unreadable value is treated as no mapping yet. */
    fun load(): Map<String, AbsenceTarget> =
        preferences.get(KEY, null)
            ?.let { runCatching { Json.decodeFromString<Map<String, AbsenceTarget>>(it) }.getOrNull() }
            .orEmpty()

    /** Returns false if the targets could not be persisted. */
    fun save(targets: Map<String, AbsenceTarget>): Boolean = try {
        // Throws IllegalArgumentException beyond Preferences.MAX_VALUE_LENGTH.
        preferences.put(KEY, Json.encodeToString(targets))
        preferences.flush()
        true
    } catch (e: IllegalArgumentException) {
        System.err.println("Warning: Absence targets too large to persist: ${e.message}")
        false
    } catch (e: BackingStoreException) {
        System.err.println("Warning: Failed to persist absence targets: ${e.message}")
        false
    }
}
