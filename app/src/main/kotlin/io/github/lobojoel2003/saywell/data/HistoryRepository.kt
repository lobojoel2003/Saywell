package io.github.lobojoel2003.saywell.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

/** Recently spoken sentences and how often each word is used (for keyboard prediction). Never leaves the device. */
@Serializable
data class History(
    val recent: List<String> = emptyList(),
    val wordCounts: Map<String, Int> = emptyMap(),
)

class HistoryRepository(
    directory: File,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val store = JsonFileStore(File(directory, "history.json"), History.serializer())
    private val state = MutableStateFlow(store.read() ?: History())

    val history: StateFlow<History> = state.asStateFlow()

    fun record(sentence: String, wordCounts: Map<String, Int>) {
        val text = sentence.trim()
        if (text.isEmpty()) return
        save { history ->
            History(
                recent = (listOf(text) + history.recent.filter { it != text }).take(MAX_RECENT),
                wordCounts = wordCounts,
            )
        }
    }

    fun clear() = save { History() }

    private fun save(transform: (History) -> History) {
        val next = state.updateAndGet(transform)
        scope.launch(io) { store.write(next) }
    }

    private companion object {
        const val MAX_RECENT = 50
    }
}
