package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.message.Message
import io.github.lobojoel2003.saywell.core.message.PressEffect
import io.github.lobojoel2003.saywell.core.message.press
import io.github.lobojoel2003.saywell.core.model.Button
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageTest {
    @Test
    fun `words are joined with spaces but not before punctuation`() {
        val message = Message().addWord("I").addWord("want").addWord("juice").addWord("!")
        assertEquals("I want juice!", message.displayText)
    }

    @Test
    fun `vocalization is spoken but the label is shown`() {
        val message = Message().addWord("hi", spoken = "Hello there")
        assertEquals("hi", message.displayText)
        assertEquals("Hello there", message.spokenText)
    }

    @Test
    fun `endings attach to the last word and backspace removes them first`() {
        val message = Message().addWord("dog").append("s")
        assertEquals("dogs", message.displayText)
        assertEquals("dog", message.backspace().displayText)
        assertEquals("", message.backspace().backspace().displayText)
    }

    @Test
    fun `spelling builds words letter by letter`() {
        val cat = Message().append("c").append("a").append("t")
        val two = cat.space().append("i").append("s")
        assertEquals("cat is", two.displayText)
        assertEquals("cat i", two.backspace().displayText)
        assertEquals("cat", cat.space().backspace().displayText)
    }

    @Test
    fun `a word button adds its word`() {
        val result = Message().press(Button("1", "juice", vocalization = "apple juice"))
        assertEquals("juice", result.message.displayText)
        assertEquals(listOf<PressEffect>(PressEffect.Said("apple juice")), result.effects)
    }

    @Test
    fun `a folder opens its board without adding a word`() {
        val result = Message().press(Button("1", "Food", linkedBoardId = "food"))
        assertTrue(result.message.isEmpty)
        assertEquals(listOf<PressEffect>(PressEffect.OpenBoard("food")), result.effects)
    }

    @Test
    fun `action buttons run their actions`() {
        val start = Message().addWord("I").addWord("go")
        assertTrue(start.press(Button("c", "clear", actions = listOf(":clear"))).message.isEmpty)
        assertEquals("I", start.press(Button("b", "delete", actions = listOf(":backspace"))).message.displayText)
        assertEquals("I goes", start.press(Button("s", "+es", actions = listOf("+es"))).message.displayText)
        assertEquals(
            listOf<PressEffect>(PressEffect.SpeakMessage, PressEffect.GoHome),
            start.press(Button("x", "speak", actions = listOf(":speak", ":home"))).effects,
        )
    }

    @Test
    fun `hidden buttons do nothing`() {
        val result = Message().press(Button("1", "secret", hidden = true))
        assertTrue(result.message.isEmpty)
        assertTrue(result.effects.isEmpty())
    }

    @Test
    fun `items can be removed by position`() {
        val message = Message().addWord("a").addWord("b").addWord("c").removeAt(1)
        assertEquals("a c", message.displayText)
    }
}
