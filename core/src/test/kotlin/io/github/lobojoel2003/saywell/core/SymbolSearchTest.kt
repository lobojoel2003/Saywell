package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.symbols.SymbolEntry
import io.github.lobojoel2003.saywell.core.symbols.SymbolSearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SymbolSearchTest {
    private val search = SymbolSearch(
        listOf(
            SymbolEntry("1F34E", "red apple", "apple, fruit, red", "food-drink"),
            SymbolEntry("1F34F", "green apple", "apple, fruit, green", "food-drink"),
            SymbolEntry("1F967", "pie", "apple pie, pastry", "food-drink"),
            SymbolEntry("1F436", "dog face", "dog, pet, puppy", "animals-nature"),
            SymbolEntry("1F415", "dog", "pet", "animals-nature"),
        ),
    )

    @Test
    fun `exact names come first`() {
        assertEquals(listOf("1F415", "1F436"), search.search("dog").map { it.code })
    }

    @Test
    fun `every word must match`() {
        assertEquals(listOf("1F34F"), search.search("green apple").map { it.code })
    }

    @Test
    fun `keywords and prefixes match`() {
        assertEquals(setOf("1F34E", "1F34F", "1F967"), search.search("appl").map { it.code }.toSet())
        assertEquals(listOf("1F436"), search.search("puppy").map { it.code })
    }

    @Test
    fun `groups and codes`() {
        assertEquals(listOf("food-drink", "animals-nature"), search.groups)
        assertEquals("pie", search.byCode("1f967")?.name)
        assertTrue(search.search("  ").isEmpty())
    }

    @Test
    fun `parses the build's index format`() {
        val entries = SymbolSearch.parseIndex("""[{"c":"1F600","n":"grinning face","k":"smile","g":"smileys-emotion"}]""")
        assertEquals(SymbolEntry("1F600", "grinning face", "smile", "smileys-emotion"), entries.single())
    }
}
