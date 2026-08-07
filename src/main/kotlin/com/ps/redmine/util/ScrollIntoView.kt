package com.ps.redmine.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Keeps the row at [targetIndex] visible in [listState], scrolling only when it sits outside the
 * viewport. A null [targetIndex] means "nothing is highlighted" and never scrolls.
 *
 * [targetIndex] is an index into the `LazyColumn`, not into the caller's data list — a list with
 * header or divider items must offset it before calling.
 *
 * Shared by the keyboard-navigable dropdowns (`SearchableDropdown` and the user switcher in
 * `Main.kt`), which previously carried near-identical copies of this effect and drifted apart.
 */
@Composable
fun ScrollIntoViewEffect(
    listState: LazyListState,
    enabled: Boolean,
    targetIndex: Int?,
) {
    LaunchedEffect(listState, enabled, targetIndex) {
        if (!enabled || targetIndex == null) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        // Not laid out yet (the menu just opened) — the list starts at the top, nothing to correct.
        if (visible.isEmpty()) return@LaunchedEffect
        if (targetIndex < visible.first().index || targetIndex > visible.last().index) {
            listState.animateScrollToItem(targetIndex)
        }
    }
}
