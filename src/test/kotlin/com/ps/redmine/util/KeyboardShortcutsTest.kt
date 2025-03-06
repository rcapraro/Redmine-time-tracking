package com.ps.redmine.util

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KeyboardShortcutsTest {
    private class TestKeyEvent(
        override val key: Key,
        override val isMetaPressed: Boolean = false,
        override val isAltPressed: Boolean = false,
        override val isCtrlPressed: Boolean = false,
        override val type: KeyEventType = KeyEventType.KeyDown
    ) : IKeyEventInfo

    @BeforeEach
    fun setup() {
        // Clear any existing callbacks
        KeyShortcutManager.clearCallbacks()
    }

    @Test
    fun `test command S triggers save shortcut`() {
        var shortcutReceived: KeyShortcut? = null
        KeyShortcutManager.setShortcutCallback { shortcut ->
            shortcutReceived = shortcut
        }

        val keyEvent = TestKeyEvent(Key.S, isMetaPressed = true)
        val handled = KeyShortcutManager.handleKeyEventInfo(keyEvent)

        assertTrue(handled, "Key event should be handled")
        assertEquals(KeyShortcut.Save, shortcutReceived, "Save shortcut should be triggered")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test command S emits save shortcut`() = runTest {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(500.milliseconds) {
                // Set up collector
                val collector = async { KeyShortcutManager.keyShortcuts.first() }

                // Simulate Command + S
                val keyEvent = TestKeyEvent(Key.S, isMetaPressed = true)
                KeyShortcutManager.handleKeyEventInfo(keyEvent)

                // Verify the emitted shortcut
                val result = collector.await()
                assertEquals(KeyShortcut.Save, result, "Save shortcut should be emitted")
            }
        }
    }

    @Test
    fun `test ctrl S triggers save shortcut`() {
        var shortcutReceived: KeyShortcut? = null
        KeyShortcutManager.setShortcutCallback { shortcut ->
            shortcutReceived = shortcut
        }

        val keyEvent = TestKeyEvent(Key.S, isCtrlPressed = true)
        val handled = KeyShortcutManager.handleKeyEventInfo(keyEvent)

        assertTrue(handled, "Key event should be handled")
        assertEquals(KeyShortcut.Save, shortcutReceived, "Save shortcut should be triggered")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test multiple collectors receive same shortcut`() = runTest {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(500.milliseconds) {
                coroutineScope {
                    // Set up collectors first
                    val collector1 = async { KeyShortcutManager.keyShortcuts.first() }
                    val collector2 = async { KeyShortcutManager.keyShortcuts.first() }

                    // Then emit shortcut
                    KeyShortcutManager.emitShortcutForTest(KeyShortcut.Save)

                    // Wait for both collectors and verify results
                    val collector1Result = collector1.await()
                    val collector2Result = collector2.await()

                    // Both collectors should receive the same event
                    assertEquals(KeyShortcut.Save, collector1Result, "Collector 1 should receive Save shortcut")
                    assertEquals(KeyShortcut.Save, collector2Result, "Collector 2 should receive Save shortcut")
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test shortcut emission with multiple collectors`() = runTest {
        val shortcuts = listOf(
            KeyShortcut.PreviousMonth,
            KeyShortcut.NextMonth,
            KeyShortcut.CurrentMonth,
            KeyShortcut.Save,
            KeyShortcut.Cancel
        )

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(1.seconds) {
                shortcuts.forEach { shortcut ->
                    coroutineScope {
                        // Set up collectors first
                        val collector1 = async { KeyShortcutManager.keyShortcuts.first() }
                        val collector2 = async { KeyShortcutManager.keyShortcuts.first() }

                        // Then emit shortcut
                        KeyShortcutManager.emitShortcutForTest(shortcut)

                        // Wait for both collectors and verify results
                        val collector1Result = collector1.await()
                        val collector2Result = collector2.await()

                        // Verify both collectors received the same shortcut
                        assertEquals(shortcut, collector1Result, "Collector 1 should receive $shortcut")
                        assertEquals(shortcut, collector2Result, "Collector 2 should receive $shortcut")
                    }
                }
            }
        }
    }
}
