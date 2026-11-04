package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.model.unreachableBoards
import io.github.lobojoel2003.saywell.core.vocabulary.DefaultVocabulary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultVocabularyTest {
    private val set = DefaultVocabulary.create()

    @Test
    fun `every folder opens a board`() {
        val ids = set.boards.map { it.id }.toSet()
        val links = set.boards.flatMap { it.buttons }.mapNotNull { it.linkedBoardId }
        assertTrue(links.isNotEmpty())
        assertTrue(links.all { it in ids }, "Broken links: ${links - ids}")
    }

    @Test
    fun `every board can be reached from home`() {
        assertEquals(emptyList(), set.unreachableBoards().map { it.id })
    }

    @Test
    fun `button ids are unique`() {
        val ids = set.boards.flatMap { it.buttons }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `starts on a full home board of core words`() {
        assertEquals(DefaultVocabulary.HOME, set.root.id)
        assertEquals(36, set.root.buttons.size)
    }

    @Test
    fun `symbol codes look like OpenMoji file names`() {
        val codes = DefaultVocabulary.symbolCodes()
        assertTrue(codes.size > 150)
        assertTrue(codes.all { it.matches(Regex("[0-9A-F]{4,5}(-[0-9A-F]{4,5})*")) })
    }
}
