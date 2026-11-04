package io.github.lobojoel2003.saywell.ui.talker

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lobojoel2003.saywell.AppContainer
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.core.message.Message
import io.github.lobojoel2003.saywell.core.message.PressEffect
import io.github.lobojoel2003.saywell.core.message.press
import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.EditException
import io.github.lobojoel2003.saywell.core.model.Ids
import io.github.lobojoel2003.saywell.core.model.addBoard
import io.github.lobojoel2003.saywell.core.model.deleteBoard
import io.github.lobojoel2003.saywell.core.model.replaceBoard
import io.github.lobojoel2003.saywell.core.model.resized
import io.github.lobojoel2003.saywell.core.model.swapCells
import io.github.lobojoel2003.saywell.core.model.withButtonAt
import io.github.lobojoel2003.saywell.core.prediction.WordPredictor
import io.github.lobojoel2003.saywell.core.scan.ScanState
import io.github.lobojoel2003.saywell.core.scan.Scanner
import io.github.lobojoel2003.saywell.data.ScanMethod
import io.github.lobojoel2003.saywell.data.Settings
import io.github.lobojoel2003.saywell.input.SwitchAction
import io.github.lobojoel2003.saywell.ui.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Keys for the message bar and navigation controls when switch scanning. */
object ScanKeys {
    const val SPEAK = "control:speak"
    const val BACKSPACE = "control:backspace"
    const val CLEAR = "control:clear"
    const val BACK = "control:back"
    const val HOME = "control:home"

    fun cell(index: Int) = "cell:$index"
}

data class TalkerState(
    /** Boards opened so far; the last one is showing. */
    val path: List<String>,
    val message: Message = Message(),
    val editing: Boolean = false,
    /** In edit mode, the cell picked up to move. */
    val moving: Int? = null,
)

/** The talking screen: the message bar, board navigation, switch scanning and board editing. */
class TalkerViewModel(private val app: AppContainer) : ViewModel() {
    val boards: StateFlow<BoardSet> = app.boards.boards
    val settings: StateFlow<Settings> = app.settings.settings
    val canUndo: StateFlow<Boolean> = app.boards.canUndo
    val speech = app.speaker.status

    private val state = MutableStateFlow(TalkerState(listOf(boards.value.rootBoardId)))
    val talker: StateFlow<TalkerState> = state.asStateFlow()

    val board: StateFlow<Board> = combine(boards, state) { set, s -> set.board(s.path.last()) ?: set.root }
        .stateIn(viewModelScope, SharingStarted.Eagerly, boards.value.root)

    private val notices = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = notices.asSharedFlow()

    private var lastActivation = 0L

    // ---- Switch scanning ----
    private var scanner: Scanner? = null
    private val scanState = MutableStateFlow<ScanState?>(null)
    private var scanTimer: Job? = null

    /** Keys highlighted by the scanner. */
    val highlighted: StateFlow<Set<String>> = scanState.map { s -> s?.let { scanner?.highlighted(it)?.toSet() }.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch { app.switches.events.collect(::onSwitch) }
        // Rebuild the scanner whenever the screen layout or scanning settings change.
        viewModelScope.launch {
            combine(board, state.map { it.editing to (it.path.size > 1) }.distinctUntilChanged(), settings) { b, mode, s -> Triple(b, mode, s) }
                .collect { (b, mode, s) -> rebuildScanner(b, editing = mode.first, canGoBack = mode.second, s) }
        }
        // Forget boards that were deleted or replaced.
        viewModelScope.launch {
            boards.collect { set ->
                state.update { s ->
                    val path = s.path.filter { set.board(it) != null }
                    s.copy(path = path.ifEmpty { listOf(set.rootBoardId) }.let { if (it.first() != set.rootBoardId) listOf(set.rootBoardId) + it else it })
                }
            }
        }
    }

    // ---- Talking ----

    /** A button was pressed (touch, switch or accessibility service). */
    fun press(button: Button) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActivation < settings.value.ignoreRepeatsMs) return
        lastActivation = now
        val s = settings.value
        val result = state.value.message.press(button)
        var path = state.value.path
        var message = result.message
        var navigated = false
        for (effect in result.effects) {
            when (effect) {
                is PressEffect.Said -> if (s.speakEachButton) app.speaker.speak(effect.text)
                is PressEffect.OpenBoard -> if (boards.value.board(effect.boardId) != null) {
                    path = (path + effect.boardId).takeLast(MAX_DEPTH)
                    navigated = true
                    if (s.speakFolderNames) app.speaker.speak(button.spokenText)
                }
                PressEffect.GoHome -> {
                    path = listOf(boards.value.rootBoardId)
                    navigated = true
                }
                PressEffect.GoBack -> {
                    path = path.dropLast(1).ifEmpty { listOf(boards.value.rootBoardId) }
                    navigated = true
                }
                PressEffect.SpeakMessage -> message = speak(message)
            }
        }
        val plainWord = button.actions.isEmpty() && button.linkedBoardId == null
        if (s.returnHomeAfterWord && plainWord && !navigated && path.size > 1) path = listOf(boards.value.rootBoardId)
        state.update { it.copy(path = path, message = message) }
    }

    fun speakMessage() {
        state.update { it.copy(message = speak(it.message)) }
    }

    /** Speaks [message] and returns the message bar's new content. */
    private fun speak(message: Message): Message {
        val text = message.spokenText
        if (text.isBlank()) return message
        app.speaker.speak(text)
        recordSpoken(text)
        return if (settings.value.clearAfterSpeaking) message.clear() else message
    }

    /** Speaks typed text or a sentence from the history. */
    fun speakText(text: String) {
        if (text.isBlank()) return
        app.speaker.speak(text)
        recordSpoken(text)
    }

    /** Puts typed text into the message bar as one item. */
    fun addToMessage(text: String) {
        if (text.isBlank()) return
        state.update { it.copy(message = it.message.addWord(text.trim())) }
    }

    private fun recordSpoken(text: String) {
        val counts = WordPredictor(emptyList(), app.history.history.value.wordCounts).learn(text).learned
        app.history.record(text, counts)
    }

    fun stopSpeaking() = app.speaker.stop()

    fun backspace() = state.update { it.copy(message = it.message.backspace()) }

    fun clear() = state.update { it.copy(message = it.message.clear()) }

    fun removeWord(index: Int) = state.update { it.copy(message = it.message.removeAt(index)) }

    /** Goes back one board. Returns false when already on the home board. */
    fun goBack(): Boolean {
        val path = state.value.path
        if (path.size <= 1) return false
        state.update { it.copy(path = path.dropLast(1), moving = null) }
        return true
    }

    fun goHome() = state.update { it.copy(path = listOf(boards.value.rootBoardId), moving = null) }

    fun openBoard(id: String) {
        if (boards.value.board(id) == null) return
        state.update { it.copy(path = (it.path + id).takeLast(MAX_DEPTH), moving = null) }
    }

    fun dismissSpeechProblem() = app.speaker.dismissProblem()

    // ---- Switch scanning ----

    private fun rebuildScanner(board: Board, editing: Boolean, canGoBack: Boolean, s: Settings) {
        scanTimer?.cancel()
        if (!s.scanning || editing) {
            scanner = null
            scanState.value = null
            return
        }
        val controls = listOfNotNull(ScanKeys.SPEAK, ScanKeys.BACKSPACE, ScanKeys.CLEAR, ScanKeys.BACK.takeIf { canGoBack }, ScanKeys.HOME.takeIf { canGoBack })
        val rows = (0 until board.rows).map { r ->
            (0 until board.columns).mapNotNull { c ->
                val index = r * board.columns + c
                board.cells[index]?.takeIf { !it.hidden }?.let { ScanKeys.cell(index) }
            }
        }
        val next = Scanner(listOf(controls) + rows, s.scanPattern, s.scanRounds)
        scanner = next
        scanState.value = next.start()
        announce()
        if (s.scanMethod == ScanMethod.AUTO) startTimer()
    }

    private fun startTimer() {
        scanTimer?.cancel()
        scanTimer = viewModelScope.launch {
            while (true) {
                delay(settings.value.scanIntervalMs.toLong().coerceAtLeast(300))
                val scan = scanner ?: break
                val current = scanState.value ?: break
                scanState.value = scan.advance(current)
                announce()
            }
        }
    }

    private fun onSwitch(action: SwitchAction) {
        val scan = scanner ?: return
        val current = scanState.value ?: return
        val step = settings.value.scanMethod == ScanMethod.STEP
        if (step && action == SwitchAction.NEXT) {
            scanState.value = scan.advance(current)
            announce()
            return
        }
        val selection = scan.select(current)
        scanState.value = selection.state
        selection.chosen?.let(::activate)
        announce()
        if (!step) startTimer()
    }

    private fun activate(key: String) {
        when (key) {
            ScanKeys.SPEAK -> speakMessage()
            ScanKeys.BACKSPACE -> backspace()
            ScanKeys.CLEAR -> clear()
            ScanKeys.BACK -> goBack()
            ScanKeys.HOME -> goHome()
            else -> {
                val index = key.removePrefix("cell:").toIntOrNull() ?: return
                board.value.cells.getOrNull(index)?.let { lastActivation = 0L; press(it) }
            }
        }
    }

    /** Auditory scanning: say what is highlighted. */
    private fun announce() {
        if (!settings.value.scanSpeakItems) return
        val first = scanState.value?.let { s -> scanner?.highlighted(s)?.firstOrNull() } ?: return
        val label = when (first) {
            ScanKeys.SPEAK -> "speak"
            ScanKeys.BACKSPACE -> "delete"
            ScanKeys.CLEAR -> "clear"
            ScanKeys.BACK -> "back"
            ScanKeys.HOME -> "home"
            else -> first.removePrefix("cell:").toIntOrNull()?.let { board.value.cells.getOrNull(it)?.label }
        } ?: return
        app.speaker.speak(label)
    }

    // ---- Editing ----

    fun setEditing(on: Boolean) = state.update { it.copy(editing = on, moving = null) }

    /** In edit mode, a tap while moving drops the picked-up button here. Returns true if it handled the tap. */
    fun moveTap(index: Int): Boolean {
        val from = state.value.moving ?: return false
        if (from != index) edit { set -> set.replaceBoard(board.value.swapCells(from, index)) }
        state.update { it.copy(moving = null) }
        return true
    }

    fun startMove(index: Int) = state.update { it.copy(moving = index) }

    fun cancelMove() = state.update { it.copy(moving = null) }

    fun saveButton(index: Int, button: Button?) {
        val current = board.value
        edit { set -> set.replaceBoard((set.board(current.id) ?: current).withButtonAt(index, button)) }
    }

    /** Makes a new empty board and returns its id. */
    fun createBoard(name: String, rows: Int, columns: Int): String? {
        val id = Ids.new()
        return if (edit { set -> set.addBoard(Board.empty(id, name.ifBlank { "New board" }, rows, columns)) }) id else null
    }

    fun updateBoard(name: String, rows: Int, columns: Int) {
        val current = board.value
        edit { set -> set.replaceBoard(current.resized(rows, columns).copy(name = name.ifBlank { current.name })) }
    }

    fun deleteBoard(id: String) {
        if (edit { set -> set.deleteBoard(id) }) notices.tryEmit(UiText.Res(R.string.board_deleted))
    }

    fun makeHome(id: String) {
        edit { set -> set.copy(rootBoardId = id) }
        goHome()
    }

    fun undo() = app.boards.undo()

    private fun edit(transform: (BoardSet) -> BoardSet): Boolean = try {
        app.boards.update(transform)
        true
    } catch (e: EditException) {
        notices.tryEmit(UiText.Raw(e.message.orEmpty()))
        false
    }

    private companion object {
        const val MAX_DEPTH = 50
    }
}
