package io.github.lobojoel2003.saywell.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.io.File

class SettingsRepository(
    directory: File,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val store = JsonFileStore(File(directory, "settings.json"), Settings.serializer())
    private val state = MutableStateFlow(store.read() ?: Settings())

    val settings: StateFlow<Settings> = state.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val next = state.updateAndGet(transform)
        scope.launch(io) { store.write(next) }
    }

    fun reset() = update { Settings() }
}
