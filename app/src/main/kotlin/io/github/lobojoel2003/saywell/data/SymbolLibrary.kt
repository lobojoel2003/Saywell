package io.github.lobojoel2003.saywell.data

import android.content.res.AssetManager
import android.util.Log
import io.github.lobojoel2003.saywell.core.symbols.SymbolEntry
import io.github.lobojoel2003.saywell.core.symbols.SymbolSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The bundled OpenMoji symbols, searchable by name and keyword. Loaded on first use. */
class SymbolLibrary(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var search: SymbolSearch? = null

    suspend fun get(): SymbolSearch = mutex.withLock {
        search ?: withContext(Dispatchers.IO) {
            val entries = try {
                SymbolSearch.parseIndex(assets.open(INDEX).use { it.readBytes().decodeToString() })
            } catch (e: Exception) {
                Log.e("Saywell", "Symbol index missing", e)
                emptyList<SymbolEntry>()
            }
            SymbolSearch(entries)
        }.also { search = it }
    }

    suspend fun search(query: String): List<SymbolEntry> = withContext(Dispatchers.Default) { get().search(query) }

    companion object {
        const val INDEX = "symbols/openmoji/index.json"
    }
}
