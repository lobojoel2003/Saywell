package io.github.lobojoel2003.saywell.ui.edit

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalResources
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.core.model.BoardAction
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button as BoardButton
import io.github.lobojoel2003.saywell.core.model.Colors
import io.github.lobojoel2003.saywell.core.model.Ids
import io.github.lobojoel2003.saywell.ui.components.SymbolImage
import kotlinx.coroutines.launch

private enum class Kind { WORD, FOLDER, ACTION }

private val actionChoices = listOf(
    BoardAction.Clear.code to R.string.action_clear,
    BoardAction.Backspace.code to R.string.action_backspace,
    BoardAction.Speak.code to R.string.action_speak,
    BoardAction.Home.code to R.string.action_home,
    BoardAction.Back.code to R.string.action_back,
    BoardAction.Space.code to R.string.action_space,
    "+" to R.string.action_append,
)

/**
 * Edits one button: its words, picture, colour, and what it does (say a word, open a board, or run
 * an action such as clearing the message).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonEditorDialog(
    initial: BoardButton?,
    boards: BoardSet,
    currentBoardId: String,
    onSave: (BoardButton) -> Unit,
    onDelete: (() -> Unit)?,
    onCreateBoard: (String) -> String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val images = context.container.images
    val scope = rememberCoroutineScope()

    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var spoken by remember { mutableStateOf(initial?.vocalization.orEmpty()) }
    var image by remember { mutableStateOf(initial?.image) }
    var color by remember { mutableStateOf(initial?.backgroundColor ?: Colors.LITTLE_WORDS) }
    var hidden by remember { mutableStateOf(initial?.hidden ?: false) }
    var kind by remember {
        mutableStateOf(
            when {
                initial?.linkedBoardId != null -> Kind.FOLDER
                initial?.actions?.isNotEmpty() == true -> Kind.ACTION
                else -> Kind.WORD
            },
        )
    }
    var linked by remember { mutableStateOf(initial?.linkedBoardId) }
    val initialAction = initial?.actions?.firstOrNull()
    var action by remember { mutableStateOf(if (initialAction?.startsWith("+") == true) "+" else initialAction ?: BoardAction.Clear.code) }
    var ending by remember { mutableStateOf(initialAction?.takeIf { it.startsWith("+") }?.substring(1) ?: "s") }
    var newBoardName by remember { mutableStateOf("") }
    var pickingSymbol by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { images.importPhoto(context.contentResolver, uri) }
                    .onSuccess { image = it }
                    .onFailure { problem = resources.getString(R.string.photo_failed) }
            }
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) scope.launch { image = images.saveBitmap(bitmap) }
    }

    val canSave = label.isNotBlank() || image != null
    fun save() {
        var target = linked
        if (kind == Kind.FOLDER && target == null && newBoardName.isNotBlank()) target = onCreateBoard(newBoardName.trim())
        if (kind == Kind.FOLDER && target == null) {
            problem = resources.getString(R.string.choose_board_first)
            return
        }
        onSave(
            BoardButton(
                id = initial?.id ?: Ids.new(),
                label = label.trim(),
                vocalization = spoken.trim().ifEmpty { null }?.takeIf { kind == Kind.WORD },
                image = image,
                backgroundColor = color,
                borderColor = initial?.borderColor,
                linkedBoardId = target.takeIf { kind == Kind.FOLDER },
                actions = if (kind == Kind.ACTION) listOf(if (action == "+") "+$ending" else action) else emptyList(),
                hidden = hidden,
            ),
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(if (initial == null) R.string.new_button else R.string.edit_button),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.button_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (kind == Kind.WORD) {
                        OutlinedTextField(
                            value = spoken,
                            onValueChange = { spoken = it },
                            label = { Text(stringResource(R.string.button_spoken)) },
                            supportingText = { Text(stringResource(R.string.button_spoken_help)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Picture
                    Text(stringResource(R.string.picture), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .background(Color(Colors.toArgb(color) ?: 0xFFFFFFFF), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val current = image
                            if (current != null) {
                                SymbolImage(current, Modifier.size(80.dp))
                            } else {
                                Text(stringResource(R.string.no_picture), style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickingSymbol = true }) {
                                Icon(Icons.Filled.EmojiEmotions, contentDescription = null)
                                Text(" " + stringResource(R.string.symbol))
                            }
                            OutlinedButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                                Text(" " + stringResource(R.string.photo))
                            }
                            OutlinedButton(onClick = {
                                try {
                                    takePhoto.launch(null)
                                } catch (e: ActivityNotFoundException) {
                                    problem = resources.getString(R.string.no_camera)
                                }
                            }) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Text(" " + stringResource(R.string.camera))
                            }
                            if (image != null) {
                                TextButton(onClick = { image = null }) { Text(stringResource(R.string.remove_picture)) }
                            }
                        }
                    }

                    // Colour
                    Text(stringResource(R.string.colour), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Colors.palette.forEachIndexed { i, swatch ->
                            val selected = Colors.normalize(swatch) == Colors.normalize(color)
                            val name = stringResource(R.string.colour_n, i + 1)
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(Color(Colors.toArgb(swatch)!!), CircleShape)
                                    .border(BorderStroke(if (selected) 4.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), CircleShape)
                                    .semantics { contentDescription = name }
                                    .selectable(selected = selected, role = Role.RadioButton) { color = swatch },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black)
                            }
                        }
                    }

                    // What it does
                    HorizontalDivider()
                    Text(stringResource(R.string.button_does), style = MaterialTheme.typography.titleMedium)
                    Choice(stringResource(R.string.kind_word), kind == Kind.WORD) { kind = Kind.WORD }
                    Choice(stringResource(R.string.kind_folder), kind == Kind.FOLDER) { kind = Kind.FOLDER }
                    Choice(stringResource(R.string.kind_action), kind == Kind.ACTION) { kind = Kind.ACTION }

                    if (kind == Kind.FOLDER) {
                        Text(stringResource(R.string.open_which_board), style = MaterialTheme.typography.titleSmall)
                        boards.boards.filter { it.id != currentBoardId }.forEach { b ->
                            Choice(b.name, linked == b.id) {
                                linked = b.id
                                if (label.isBlank()) label = b.name
                            }
                        }
                        Choice(stringResource(R.string.new_board_choice), linked == null) { linked = null }
                        if (linked == null) {
                            OutlinedTextField(
                                value = newBoardName,
                                onValueChange = { newBoardName = it },
                                label = { Text(stringResource(R.string.new_board_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (kind == Kind.ACTION) {
                        actionChoices.forEach { (code, name) -> Choice(stringResource(name), action == code) { action = code } }
                        if (action == "+") {
                            OutlinedTextField(
                                value = ending,
                                onValueChange = { ending = it.trim() },
                                label = { Text(stringResource(R.string.ending)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.hide_button), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.hide_button_help), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = hidden, onCheckedChange = { hidden = it })
                    }
                    problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Text(" " + stringResource(R.string.delete))
                        }
                    }
                    Box(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = ::save, enabled = canSave) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }

    if (pickingSymbol) {
        SymbolPickerDialog(
            initialQuery = label,
            onPick = {
                image = it
                pickingSymbol = false
            },
            onDismiss = { pickingSymbol = false },
        )
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}
