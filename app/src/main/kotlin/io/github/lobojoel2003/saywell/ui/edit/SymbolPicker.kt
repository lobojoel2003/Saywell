package io.github.lobojoel2003.saywell.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.core.model.ImageRef
import io.github.lobojoel2003.saywell.core.symbols.SymbolEntry
import io.github.lobojoel2003.saywell.ui.components.OpenMojiImage
import kotlinx.coroutines.delay

/** Search the bundled OpenMoji symbols by word, or browse them by group. */
@Composable
fun SymbolPickerDialog(initialQuery: String, onPick: (ImageRef) -> Unit, onDismiss: () -> Unit) {
    val library = LocalContext.current.container.symbols
    var query by remember { mutableStateOf(initialQuery) }
    var group by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf(emptyList<String>()) }
    var results by remember { mutableStateOf(emptyList<SymbolEntry>()) }

    LaunchedEffect(query, group) {
        delay(150)
        val search = library.get()
        groups = search.groups
        results = when {
            query.isNotBlank() -> library.search(query)
            group != null -> search.inGroup(group!!)
            else -> search.inGroup(search.groups.firstOrNull().orEmpty())
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.choose_symbol), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text(stringResource(R.string.search_symbols)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (query.isBlank()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groups) { g ->
                            FilterChip(
                                selected = (group ?: groups.firstOrNull()) == g,
                                onClick = { group = g },
                                label = { Text(groupName(g)) },
                            )
                        }
                    }
                }
                if (results.isEmpty() && query.isNotBlank()) {
                    Text(stringResource(R.string.no_symbols_found), modifier = Modifier.padding(16.dp))
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(92.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(results, key = { it.code }) { entry ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClickLabel = entry.name) { onPick(ImageRef.openMoji(entry.code)) }
                                .padding(4.dp),
                        ) {
                            OpenMojiImage(entry.code, Modifier.size(64.dp))
                            Text(
                                entry.name,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Text(stringResource(R.string.openmoji_credit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun groupName(group: String): String = when (group) {
    "smileys-emotion" -> stringResource(R.string.group_feelings)
    "people-body" -> stringResource(R.string.group_people)
    "animals-nature" -> stringResource(R.string.group_nature)
    "food-drink" -> stringResource(R.string.group_food)
    "travel-places" -> stringResource(R.string.group_places)
    "activities" -> stringResource(R.string.group_activities)
    "objects" -> stringResource(R.string.group_things)
    "symbols" -> stringResource(R.string.group_symbols)
    "flags" -> stringResource(R.string.group_flags)
    "extras-openmoji" -> stringResource(R.string.group_extras)
    "extras-unicode" -> stringResource(R.string.group_more_symbols)
    else -> group
}
