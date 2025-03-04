package com.ps.redmine.util

import androidx.compose.ui.input.key.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

enum class KeyShortcut {
    Save,
    Cancel,
    PreviousMonth,
    NextMonth,
    CurrentMonth
}

object KeyShortcutManager {
    private val _keyShortcuts = Channel<KeyShortcut>(Channel.BUFFERED)
    val keyShortcuts = _keyShortcuts.receiveAsFlow()

    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false

        when {
            // Save
            (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) && keyEvent.key == Key.S -> {
                _keyShortcuts.trySend(KeyShortcut.Save)
                return true
            }
            // Cancel
            keyEvent.key == Key.Escape -> {
                _keyShortcuts.trySend(KeyShortcut.Cancel)
                return true
            }
            // Month navigation
            (keyEvent.isAltPressed) && keyEvent.key == Key.DirectionLeft -> {
                _keyShortcuts.trySend(KeyShortcut.PreviousMonth)
                return true
            }

            (keyEvent.isAltPressed) && keyEvent.key == Key.DirectionRight -> {
                _keyShortcuts.trySend(KeyShortcut.NextMonth)
                return true
            }

            (keyEvent.isAltPressed) && keyEvent.key == Key.T -> {
                _keyShortcuts.trySend(KeyShortcut.CurrentMonth)
                return true
            }
        }
        return false
    }
}
