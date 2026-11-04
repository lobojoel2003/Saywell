package io.github.lobojoel2003.saywell.core.prediction

/**
 * Suggests words for the typing keyboard. Ranks a built-in frequency list together with the words
 * this person has actually used, so their own vocabulary comes first.
 *
 * @param baseWords common words, most frequent first.
 * @param learned how often each word (lower case) has been spoken.
 */
class WordPredictor(
    baseWords: List<String>,
    val learned: Map<String, Int> = emptyMap(),
) {
    private val baseRank: Map<String, Int> = buildMap {
        baseWords.forEachIndexed { index, word ->
            val key = word.trim().lowercase()
            if (key.isNotEmpty() && key !in this) put(key, index)
        }
    }
    private val display: Map<String, String> = baseWords.associateBy { it.trim().lowercase() }

    private fun score(word: String): Double {
        val rank = baseRank[word]
        val base = if (rank == null) 0.0 else 1.0 - rank.toDouble() / (baseRank.size + 1)
        return (learned[word] ?: 0) * 2.0 + base
    }

    /**
     * Completions for the word being typed (the text after the last space). With nothing typed it
     * suggests the most used words.
     */
    fun predict(text: String, limit: Int = 6): List<String> {
        val prefix = currentWord(text).lowercase()
        val candidates = (baseRank.keys + learned.keys).asSequence().distinct()
            .filter { it.startsWith(prefix) && it != prefix }
        return candidates
            .sortedWith(compareByDescending<String> { score(it) }.thenBy { it.length })
            .take(limit)
            .map { display[it] ?: casing(it) }
            .toList()
    }

    /** Records the words of a spoken sentence. */
    fun learn(sentence: String): WordPredictor {
        val words = sentence.split(Regex("[^\\p{L}\\p{N}']+")).map { it.lowercase().trim('\'') }.filter { it.length > 1 || it == "i" || it == "a" }
        if (words.isEmpty()) return this
        val updated = learned.toMutableMap()
        for (word in words) updated[word] = (updated[word] ?: 0) + 1
        // Keep the learned list bounded: drop the rarest words first.
        val trimmed = if (updated.size > MAX_LEARNED) {
            updated.entries.sortedByDescending { it.value }.take(MAX_LEARNED).associate { it.key to it.value }
        } else {
            updated
        }
        return WordPredictor(display.values.sortedBy { baseRank[it.lowercase()] ?: Int.MAX_VALUE }, trimmed)
    }

    companion object {
        const val MAX_LEARNED = 2000

        /** The word being typed: everything after the last space or punctuation. */
        fun currentWord(text: String): String = text.takeLastWhile { it.isLetterOrDigit() || it == '\'' }

        /** Replaces the word being typed with [word] and adds a space. */
        fun complete(text: String, word: String): String = text.dropLast(currentWord(text).length) + word + " "

        private fun casing(word: String) = if (word == "i" || word.startsWith("i'")) word.replaceFirstChar { it.uppercase() } else word
    }
}
