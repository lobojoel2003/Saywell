package io.github.lobojoel2003.saywell.ui.talker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalResources
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.ui.components.HoldIconButton
import io.github.lobojoel2003.saywell.ui.edit.BoardListDialog
import io.github.lobojoel2003.saywell.ui.edit.BoardSettingsDialog
import io.github.lobojoel2003.saywell.ui.edit.ButtonEditorDialog
import io.github.lobojoel2003.saywell.ui.edit.HistoryDialog
import io.github.lobojoel2003.saywell.ui.theme.LocalSaywellColors

/** How long the settings and edit buttons must be held when editing is locked. */
const val LOCK_HOLD_MILLIS = 1500

@Composable
fun TalkerScreen(
    viewModel: TalkerViewModel,
    onOpenKeyboard: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = LocalContext.current.container
    val resources = LocalResources.current
    val talker by viewModel.talker.collectAsStateWithLifecycle()
    val board by viewModel.board.collectAsStateWithLifecycle()
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val speech by viewModel.speech.collectAsStateWithLifecycle()
    val highlighted by viewModel.highlighted.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val history by app.history.history.collectAsStateWithLifecycle()

    var editingCell by remember { mutableStateOf<Int?>(null) }
    var showBoardSettings by remember { mutableStateOf(false) }
    var showBoardList by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val scanning = settings.scanning && !talker.editing
    DisposableEffect(scanning) {
        app.switches.listening = scanning
        onDispose { app.switches.listening = false }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it.resolve(resources)) }
    }
    LaunchedEffect(speech.problem) {
        val problem = speech.problem ?: return@LaunchedEffect
        snackbar.showSnackbar(problem, duration = SnackbarDuration.Long)
        viewModel.dismissSpeechProblem()
    }

    BackHandler(enabled = talker.editing || talker.path.size > 1) {
        when {
            talker.moving != null -> viewModel.cancelMove()
            talker.editing -> viewModel.setEditing(false)
            else -> viewModel.goBack()
        }
    }

    val lockHold = if (settings.lockEditing) LOCK_HOLD_MILLIS else 0
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MessageBar(
                message = talker.message,
                settings = settings,
                speaking = speech.speaking,
                highlighted = highlighted,
                onSpeak = viewModel::speakMessage,
                onStop = viewModel::stopSpeaking,
                onBackspace = viewModel::backspace,
                onClear = viewModel::clear,
            )
            if (talker.editing) {
                EditBar(
                    boardName = board.name,
                    moving = talker.moving != null,
                    canUndo = canUndo,
                    onUndo = viewModel::undo,
                    onBoardSettings = { showBoardSettings = true },
                    onBoards = { showBoardList = true },
                    onDone = { viewModel.setEditing(false) },
                )
            } else {
                NavBar(
                    boardName = board.name,
                    canGoBack = talker.path.size > 1,
                    highlighted = highlighted,
                    lockHold = lockHold,
                    onBack = { viewModel.goBack() },
                    onHome = viewModel::goHome,
                    onHistory = { showHistory = true },
                    onKeyboard = onOpenKeyboard,
                    onEdit = { viewModel.setEditing(true) },
                    onSettings = onOpenSettings,
                )
            }
            Box(Modifier.weight(1f)) {
                BoardGrid(
                    board = board,
                    settings = settings,
                    editing = talker.editing,
                    moving = talker.moving,
                    highlighted = highlighted,
                    onPress = { index, button ->
                        if (talker.editing) {
                            if (!viewModel.moveTap(index)) editingCell = index
                        } else if (button != null) {
                            viewModel.press(button)
                        }
                    },
                    onLongPress = { index -> if (board.cells[index] != null) viewModel.startMove(index) },
                )
                if (scanning && settings.tapScreenAsSwitch) {
                    val currentSettings = settings
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(currentSettings) { detectTapGestures { app.switches.tap(currentSettings) } },
                    )
                }
            }
        }
    }

    editingCell?.let { index ->
        val existing = board.cells.getOrNull(index)
        ButtonEditorDialog(
            initial = existing,
            boards = boards,
            currentBoardId = board.id,
            onSave = {
                viewModel.saveButton(index, it)
                editingCell = null
            },
            onDelete = existing?.let {
                {
                    viewModel.saveButton(index, null)
                    editingCell = null
                }
            },
            onCreateBoard = { name -> viewModel.createBoard(name, 4, 5) },
            onDismiss = { editingCell = null },
        )
    }
    if (showBoardSettings) {
        BoardSettingsDialog(
            board = board,
            isHome = board.id == boards.rootBoardId,
            onSave = { name, rows, columns ->
                viewModel.updateBoard(name, rows, columns)
                showBoardSettings = false
            },
            onMakeHome = {
                viewModel.makeHome(board.id)
                showBoardSettings = false
            },
            onDelete = {
                val id = board.id
                viewModel.goBack()
                viewModel.deleteBoard(id)
                showBoardSettings = false
            },
            onDismiss = { showBoardSettings = false },
        )
    }
    if (showBoardList) {
        BoardListDialog(
            boards = boards,
            onOpen = {
                viewModel.openBoard(it)
                showBoardList = false
            },
            onCreate = { name, rows, columns ->
                viewModel.createBoard(name, rows, columns)?.let(viewModel::openBoard)
                showBoardList = false
            },
            onDismiss = { showBoardList = false },
        )
    }
    if (showHistory) {
        HistoryDialog(
            recent = history.recent,
            onSpeak = viewModel::speakText,
            onClear = { app.history.clear() },
            onDismiss = { showHistory = false },
        )
    }
}

@Composable
private fun NavBar(
    boardName: String,
    canGoBack: Boolean,
    highlighted: Set<String>,
    lockHold: Int,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onKeyboard: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
) {
    val group = highlighted.size > 1 && (ScanKeys.BACK in highlighted || ScanKeys.HOME in highlighted)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Phones in portrait get smaller buttons so the board's name still fits.
        val compact = maxWidth < 600.dp
        val navWidth = if (compact) 56.dp else 72.dp
        val iconSize = if (compact) 44.dp else 52.dp
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp),
        ) {
            if (canGoBack) {
                ControlButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.back),
                    highlighted = ScanKeys.BACK in highlighted && !group,
                    onClick = onBack,
                    modifier = Modifier.size(width = navWidth, height = 52.dp),
                )
                ControlButton(
                    Icons.Filled.Home,
                    stringResource(R.string.home),
                    highlighted = ScanKeys.HOME in highlighted && !group,
                    onClick = onHome,
                    modifier = Modifier.size(width = navWidth, height = 52.dp),
                )
            }
            Text(
                boardName,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp).semantics { heading() },
            )
            IconButton(onClick = onHistory, modifier = Modifier.size(iconSize)) {
                Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history), modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onKeyboard, modifier = Modifier.size(iconSize)) {
                Icon(Icons.Filled.Keyboard, contentDescription = stringResource(R.string.keyboard), modifier = Modifier.size(26.dp))
            }
            HoldIconButton(Icons.Filled.Edit, stringResource(if (lockHold > 0) R.string.edit_hold else R.string.edit), lockHold, onEdit, Modifier.size(iconSize))
            HoldIconButton(Icons.Filled.Settings, stringResource(if (lockHold > 0) R.string.settings_hold else R.string.settings), lockHold, onSettings, Modifier.size(iconSize))
        }
    }
    if (group) {
        Surface(color = LocalSaywellColors.current.scanHighlight, modifier = Modifier.fillMaxWidth().height(4.dp)) {}
    }
}

@Composable
private fun EditBar(
    boardName: String,
    moving: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onBoardSettings: () -> Unit,
    onBoards: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.editing_board, boardName),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo))
                }
                IconButton(onClick = onBoardSettings) {
                    Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.board_settings))
                }
                IconButton(onClick = onBoards) {
                    Icon(Icons.Filled.GridView, contentDescription = stringResource(R.string.all_boards))
                }
                FilledTonalButton(onClick = onDone) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text(" " + stringResource(R.string.done))
                }
            }
            Text(
                stringResource(if (moving) R.string.edit_hint_moving else R.string.edit_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
