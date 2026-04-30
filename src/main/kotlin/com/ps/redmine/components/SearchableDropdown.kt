package com.ps.redmine.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.ps.redmine.resources.Strings

/**
 * A searchable dropdown component that limits the number of visible items and provides search functionality.
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

    val filteredItems = remember(items, searchText) {
        if (searchText.isEmpty()) items
        else items.filter { itemText(it).contains(searchText, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
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
                            searchText = ""
                        },
                        enabled = enabled && items.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = if (expanded) Modifier.rotate(180f) else Modifier,
                        )
                    }
                }
            }
        )

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = {
                expanded = false
                searchText = ""
            },
            modifier = Modifier
                .width(400.dp)
                .heightIn(max = 500.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .width(380.dp)
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

            LaunchedEffect(expanded) {
                if (expanded) {
                    focusRequester.requestFocus()
                }
            }

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.width(380.dp).padding(8.dp).heightIn(min = 48.dp)) {
                    Text(
                        text = if (searchText.isEmpty()) noItemsText else Strings["no_search_results"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .height(340.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems) { item ->
                                val isSelected = item == selectedItem
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
                                    modifier = Modifier.heightIn(min = 48.dp),
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
}
