package com.ps.redmine.util

import androidx.compose.ui.input.key.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface IKeyEventInfo {
    val key: Key
    val type: KeyEventType
    val isMetaPressed: Boolean
    val isAltPressed: Boolean
    val isCtrlPressed: Boolean
}

class KeyEventWrapper(event: KeyEvent) : IKeyEventInfo {
    override val key: Key = event.key
    override val type: KeyEventType = event.type
    override val isMetaPressed: Boolean = event.isMetaPressed
    override val isAltPressed: Boolean = event.isAltPressed
    override val isCtrlPressed: Boolean = event.isCtrlPressed
}

enum class KeyShortcut {
    PreviousMonth,
    NextMonth,
    CurrentMonth,
    Save,
    Cancel
}

object KeyShortcutManager {
    private val shortcutCallbacks = mutableListOf<(KeyShortcut) -> Unit>()
    private val _keyShortcuts = MutableSharedFlow<KeyShortcut>(replay = 1, extraBufferCapacity = 10)
    val keyShortcuts: SharedFlow<KeyShortcut> = _keyShortcuts.asSharedFlow()

    fun setShortcutCallback(callback: (KeyShortcut) -> Unit) {
        shortcutCallbacks.add(callback)
    }

    fun removeShortcutCallback(callback: (KeyShortcut) -> Unit) {
        shortcutCallbacks.remove(callback)
    }

    internal fun clearCallbacks() {
        shortcutCallbacks.clear()
    }

    internal suspend fun emitShortcutForTest(shortcut: KeyShortcut) {
        _keyShortcuts.emit(shortcut)
    }

    fun handleKeyEvent(keyEvent: KeyEvent): Boolean = handleKeyEventInfo(KeyEventWrapper(keyEvent))

    internal fun handleKeyEventInfo(keyEvent: IKeyEventInfo): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) {
            return false
        }

        val shortcut = when {
            // Alt + navigation shortcuts
            keyEvent.isAltPressed && !keyEvent.isMetaPressed && !keyEvent.isCtrlPressed -> when (keyEvent.key) {
                Key.DirectionLeft -> KeyShortcut.PreviousMonth
                Key.DirectionRight -> KeyShortcut.NextMonth
                Key.T -> KeyShortcut.CurrentMonth
                else -> null
            }

            // Command/Ctrl + S for Save
            (keyEvent.key == Key.S && keyEvent.isMetaPressed) ||
                    (keyEvent.key == Key.S && keyEvent.isCtrlPressed) -> KeyShortcut.Save

            // Escape for Cancel
            keyEvent.key == Key.Escape -> KeyShortcut.Cancel

            else -> null
        }

        return if (shortcut != null) {
            shortcutCallbacks.forEach { callback ->
                try {
                    callback(shortcut)
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Error in shortcut callback: ${e.message}")
                }
            }
            _keyShortcuts.tryEmit(shortcut)
            true
        } else {
            false
        }
    }
}
