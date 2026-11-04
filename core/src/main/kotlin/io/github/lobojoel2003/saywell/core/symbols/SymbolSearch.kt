package io.github.lobojoel2003.saywell.core.symbols

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One symbol in a bundled set. The build writes these to `symbols/<set>/index.json`
 * with short keys to keep the file small.
 */
@Serializable
data class SymbolEntry(
    @SerialName("c") val code: String,
    @SerialName("n") val name: String,
    @SerialName("k") val keywords: String = "",
    @SerialName("g") val group: String = "",
)

/** Finds symbols by name or keyword, best matches first. */
class SymbolSearch(val entries: List<SymbolEntry>) {
    private class Indexed(val entry: SymbolEntry, val nameWords: List<String>, val keywords: List<String>)

    private val indexed = entries.map { e ->
        Indexed(
            e,
            words(e.name),
            e.keywords.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() },
        )
    }

    val groups: List<String> = entries.map { it.group }.filter { it.isNotEmpty() }.distinct()

    fun inGroup(group: String): List<SymbolEntry> = entries.filter { it.group == group }

    fun byCode(code: String): SymbolEntry? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun search(query: String, limit: Int = 120): List<SymbolEntry> {
        val terms = words(query)
        if (terms.isEmpty()) return emptyList()
        val phrase = terms.joinToString(" ")
        return indexed.asSequence()
            .mapNotNull { item ->
                var total = 0
                for (term in terms) {
                    val score = score(item, term)
                    if (score == 0) return@mapNotNull null
                    total += score
                }
                val name = item.nameWords.joinToString(" ")
                if (name == phrase) total += 200
                item.entry to total
            }
            .sortedWith(compareByDescending<Pair<SymbolEntry, Int>> { it.second }.thenBy { it.first.name.length })
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(item: Indexed, term: String): Int = when {
        item.nameWords.firstOrNull() == term -> 100
        term in item.nameWords -> 80
        term in item.keywords -> 60
        item.nameWords.any { it.startsWith(term) } -> 40
        item.keywords.any { k -> k.split(' ').any { it.startsWith(term) } } -> 20
        else -> 0
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseIndex(text: String): List<SymbolEntry> = json.decodeFromString(text)

        private fun words(text: String): List<String> =
            text.lowercase().split(Regex("[^\\p{L}\\p{N}']+")).filter { it.isNotEmpty() }
    }
}
