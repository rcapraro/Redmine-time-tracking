package com.ps.redmine.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings

private val MenuWidth = 400.dp

/**
 * Searchable dropdown — type to filter, arrow keys to navigate, Enter to select, Esc to close.
 */
@Composable
fun <T> SearchableDropdown(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemText: (T) -> String,
    label: @Composable () -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    noItemsText: String = Strings["no_items"]
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var highlightedIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    val filteredItems = remember(items, searchText) {
        if (searchText.isEmpty()) items
        else items.filter { itemText(it).contains(searchText, ignoreCase = true) }
    }

    val displayedValue = if (expanded) searchText else selectedItem?.let(itemText).orEmpty()

    LaunchedEffect(enabled) {
        if (!enabled) {
            expanded = false
            searchText = ""
        }
    }

    LaunchedEffect(filteredItems, expanded) {
        if (expanded) {
            highlightedIndex = highlightedIndex.coerceIn(0, (filteredItems.size - 1).coerceAtLeast(0))
        } else {
            highlightedIndex = 0
        }
    }

    LaunchedEffect(highlightedIndex, expanded) {
        if (!expanded || highlightedIndex !in filteredItems.indices) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        val first = visible.firstOrNull()?.index
        val last = visible.lastOrNull()?.index
        if (first == null || last == null || highlightedIndex < first || highlightedIndex > last) {
            listState.animateScrollToItem(highlightedIndex)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { newExpanded ->
            if (enabled && items.isNotEmpty()) {
                expanded = newExpanded
                if (!newExpanded) searchText = ""
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayedValue,
            onValueChange = { newValue ->
                if (enabled) {
                    searchText = newValue
                    if (!expanded && items.isNotEmpty()) expanded = true
                }
            },
            modifier = Modifier
                .onPreviewKeyEvent { event ->
                    if (!expanded) return@onPreviewKeyEvent false
                    val isDown = event.type == KeyEventType.KeyDown
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            if (isDown) {
                                val item = filteredItems.getOrNull(highlightedIndex)
                                if (item != null) {
                                    onItemSelected(item)
                                    expanded = false
                                    searchText = ""
                                }
                            }
                            true
                        }

                        Key.DirectionDown -> {
                            if (isDown && filteredItems.isNotEmpty()) {
                                highlightedIndex =
                                    (highlightedIndex + 1).coerceAtMost(filteredItems.size - 1)
                            }
                            true
                        }

                        Key.DirectionUp -> {
                            if (isDown && filteredItems.isNotEmpty()) {
                                highlightedIndex = (highlightedIndex - 1).coerceAtLeast(0)
                            }
                            true
                        }

                        Key.Escape -> {
                            if (isDown) {
                                expanded = false
                                searchText = ""
                            }
                            true
                        }

                        else -> false
                    }
                }
                // Positioning still works (AnchorElement is unconditional); we disable only the
                // click/key/semantics block so we own those interactions ourselves. a11y note:
                // screen-reader users expand via the trailing arrow (SecondaryEditable, Role.Button).
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = false)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .onFocusChanged { state ->
                    if (state.isFocused && enabled && items.isNotEmpty() && !expanded) {
                        expanded = true
                        searchText = ""
                    }
                },
            label = label,
            placeholder = placeholder,
            singleLine = true,
            enabled = enabled,
            isError = isError,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                        modifier = Modifier.menuAnchor(
                            ExposedDropdownMenuAnchorType.SecondaryEditable,
                            enabled = enabled && items.isNotEmpty()
                        )
                    )
                }
            }
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = {
                expanded = false
                searchText = ""
            },
            matchAnchorWidth = false,
            modifier = Modifier.width(MenuWidth)
        ) {
            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.width(MenuWidth).padding(8.dp).heightIn(min = 48.dp)) {
                    Text(
                        text = if (searchText.isEmpty()) noItemsText else Strings["no_search_results"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Fixed width short-circuits DropdownMenuContent's IntrinsicSize.Max query so it
                // never tries to intrinsically measure the LazyColumn (a SubcomposeLayout).
                Box(modifier = Modifier.width(MenuWidth).height(340.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredItems) { index, item ->
                            val isSelected = item == selectedItem
                            val isHighlighted = index == highlightedIndex
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = itemText(item),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onItemSelected(item)
                                    expanded = false
                                    searchText = ""
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .background(
                                        if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant
                                        else Color.Transparent
                                    ),
                            )
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(listState)
                    )
                }
            }
        }
    }
}
