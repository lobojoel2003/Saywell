package io.github.lobojoel2003.saywell.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.MAX_COLUMNS
import io.github.lobojoel2003.saywell.core.model.MAX_ROWS

/** Name and grid size of a board, plus making it home or deleting it. */
@Composable
fun BoardSettingsDialog(
    board: Board,
    isHome: Boolean,
    onSave: (name: String, rows: Int, columns: Int) -> Unit,
    onMakeHome: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(board.name) }
    var rows by remember { mutableIntStateOf(board.rows) }
    var columns by remember { mutableIntStateOf(board.columns) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.board_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.board_name)) }, singleLine = true)
                Stepper(stringResource(R.string.rows), rows, 1..MAX_ROWS) { rows = it }
                Stepper(stringResource(R.string.columns), columns, 1..MAX_COLUMNS) { columns = it }
                if (!isHome) {
                    TextButton(onClick = onMakeHome) { Text(stringResource(R.string.make_home)) }
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.delete_board), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, rows, columns) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_board_title, board.name)) },
            text = { Text(stringResource(R.string.delete_board_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - 1).coerceIn(range)) }, enabled = value > range.first) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.fewer, label))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { onChange((value + 1).coerceIn(range)) }, enabled = value < range.last) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.more_of, label))
        }
    }
}

/** Every board, to jump to one or start a new one. */
@Composable
fun BoardListDialog(
    boards: BoardSet,
    onOpen: (String) -> Unit,
    onCreate: (name: String, rows: Int, columns: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var rows by remember { mutableIntStateOf(4) }
    var columns by remember { mutableIntStateOf(5) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (creating) R.string.new_board else R.string.all_boards)) },
        text = {
            if (creating) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.board_name)) }, singleLine = true)
                    Stepper(stringResource(R.string.rows), rows, 1..MAX_ROWS) { rows = it }
                    Stepper(stringResource(R.string.columns), columns, 1..MAX_COLUMNS) { columns = it }
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(boards.boards, key = { it.id }) { board ->
                        ListItem(
                            headlineContent = { Text(board.name) },
                            supportingContent = { Text(pluralStringResource(R.plurals.button_count, board.buttons.size, board.buttons.size)) },
                            leadingContent = if (board.id == boards.rootBoardId) ({ Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.home_board)) }) else null,
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(board.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = { onCreate(name, rows, columns) }) { Text(stringResource(R.string.create)) }
            } else {
                TextButton(onClick = { creating = true }) { Text(stringResource(R.string.new_board)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Sentences spoken recently; tap one to say it again. */
@Composable
fun HistoryDialog(recent: List<String>, onSpeak: (String) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history)) },
        text = {
            if (recent.isEmpty()) {
                Text(stringResource(R.string.history_empty))
            } else {
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(recent) { sentence ->
                        ListItem(
                            headlineContent = { Text(sentence, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = stringResource(R.string.speak)) { onSpeak(sentence) }.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = { if (recent.isNotEmpty()) TextButton(onClick = onClear) { Text(stringResource(R.string.clear_history)) } },
    )
}
