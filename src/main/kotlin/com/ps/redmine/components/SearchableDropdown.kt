package com.ps.redmine.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.ElevationTokens

/**
 * A searchable dropdown component that limits the number of visible items and provides search functionality.
 *
 * @param T The type of items in the dropdown
 * @param items The list of items to display in the dropdown
 * @param selectedItem The currently selected item
 * @param onItemSelected Callback when an item is selected
 * @param itemText Function to extract the display text from an item
 * @param label The label for the dropdown field
 * @param placeholder Optional placeholder text
 * @param isError Whether the field is in an error state
 * @param enabled Whether the dropdown is enabled
 * @param isLoading Whether the dropdown is in a loading state
 * @param noItemsText Text to display when there are no items
 * @param readOnly Whether the field is read-only
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
    noItemsText: String = Strings["no_items"],
    readOnly: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Filter items based on search text
    val filteredItems = remember(items, searchText) {
        if (searchText.isEmpty()) {
            items
        } else {
            items.filter {
                itemText(it).contains(searchText, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)  // Use fixed height instead of intrinsic measurement
    ) {
        OutlinedTextField(
            value = selectedItem?.let { itemText(it) } ?: "",
            onValueChange = {},
            label = label,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            readOnly = readOnly,
            enabled = enabled,
            isError = isError,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            expanded = true
                            searchText = ""  // Clear search when opening dropdown
                            // Focus will be requested in LaunchedEffect
                        },
                        enabled = enabled && items.isNotEmpty()
                    ) {
                        Text(if (expanded) Strings["dropdown_up"] else Strings["dropdown_down"])
                    }
                }
            }
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = {
                expanded = false
                searchText = ""  // Clear search when closing dropdown
            },
            modifier = Modifier
                .width(400.dp)  // Use fixed width instead of percentage of parent
                .heightIn(max = 500.dp)  // Add explicit height constraint
        ) {
            // Search field
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .width(380.dp)  // Use fixed width instead of fillMaxWidth
                    .padding(6.dp)
                    .heightIn(min = 48.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(Strings["search"]) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }
            )

            // Focus the search field when dropdown opens
            // This ensures immediate focus when clicking on the dropdown
            LaunchedEffect(expanded) {
                if (expanded) {
                    focusRequester.requestFocus()
                }
            }

            // Show filtered items in a scrollable list with max height
            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.width(380.dp).padding(8.dp).heightIn(min = 48.dp)) {
                    Text(
                        text = if (searchText.isEmpty()) noItemsText else Strings["no_search_results"],
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Add a background to make the scrollable area more visible
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .height(340.dp),
                    color = MaterialTheme.colors.surface,
                    elevation = ElevationTokens.Low,
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems) { item ->
                                DropdownMenuItem(
                                    onClick = {
                                        onItemSelected(item)
                                        expanded = false
                                        searchText = ""  // Clear search after selection
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = itemText(item),
                                        color = if (item == selectedItem)
                                            MaterialTheme.colors.primary
                                        else
                                            MaterialTheme.colors.onSurface
                                    )
                                }
                            }
                        }

                        // Add a visible scrollbar
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
}
