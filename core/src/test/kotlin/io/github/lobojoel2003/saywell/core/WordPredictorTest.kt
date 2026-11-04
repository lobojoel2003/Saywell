package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.prediction.WordPredictor
import kotlin.test.Test
import kotlin.test.assertEquals

class WordPredictorTest {
    private val base = listOf("the", "I", "to", "want", "water", "what", "went", "wait")

    @Test
    fun `completes the word being typed by frequency`() {
        assertEquals(listOf("want", "water", "wait"), WordPredictor(base).predict("I wa"))
    }

    @Test
    fun `words this person uses rise to the top`() {
        val learned = WordPredictor(base).learn("I want waffles").learn("waffles waffles")
        assertEquals("waffles", learned.predict("wa").first())
    }

    @Test
    fun `suggests common words before anything is typed and keeps the capital I`() {
        assertEquals(listOf("I", "I'm", "it"), WordPredictor(listOf("I", "I'm", "it")).predict(""))
        assertEquals(listOf("I'm"), WordPredictor(listOf("I", "I'm", "it")).learn("i'm").predict("i'"))
    }

    @Test
    fun `completing replaces the partial word`() {
        assertEquals("I want ", WordPredictor.complete("I wa", "want"))
        assertEquals("hello ", WordPredictor.complete("", "hello"))
    }
}
