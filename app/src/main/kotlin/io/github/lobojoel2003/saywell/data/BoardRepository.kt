package io.github.lobojoel2003.saywell.data

import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.withoutBrokenLinks
import io.github.lobojoel2003.saywell.core.vocabulary.DefaultVocabulary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * The person's boards. Every change is saved straight away; the last few versions are kept in
 * memory so an edit can be undone.
 */
class BoardRepository(
    directory: File,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val store = JsonFileStore(File(directory, "boards.json"), BoardSet.serializer())
    private val saved = store.read()
    private val state = MutableStateFlow(saved?.withoutBrokenLinks() ?: DefaultVocabulary.create())

    /** True when the boards came from the saved file (false on first run or if the file was unreadable). */
    val restored: Boolean = saved != null
    private val undoStack = ArrayDeque<BoardSet>()
    private val canUndoState = MutableStateFlow(false)

    val boards: StateFlow<BoardSet> = state.asStateFlow()
    val canUndo: StateFlow<Boolean> = canUndoState.asStateFlow()

    /** Applies an edit. Throws whatever [transform] throws (for example an EditException) without changing anything. */
    @Synchronized
    fun update(transform: (BoardSet) -> BoardSet) {
        val before = state.value
        val after = transform(before)
        if (after == before) return
        undoStack.addLast(before)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        canUndoState.value = true
        commit(after)
    }

    @Synchronized
    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        canUndoState.value = undoStack.isNotEmpty()
        commit(previous)
    }

    /** Replaces everything, for example after importing or resetting. This can be undone too. */
    fun replaceAll(boardSet: BoardSet) = update { boardSet }

    fun resetToStarterVocabulary() = replaceAll(DefaultVocabulary.create())

    private fun commit(boardSet: BoardSet) {
        state.value = boardSet
        scope.launch(io) { store.write(boardSet) }
    }

    private companion object {
        const val MAX_UNDO = 30
    }
}
