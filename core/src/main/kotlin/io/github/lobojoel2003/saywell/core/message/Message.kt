package io.github.lobojoel2003.saywell.core.message

import io.github.lobojoel2003.saywell.core.model.BoardAction
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.ImageRef
import io.github.lobojoel2003.saywell.core.model.parsedActions

/**
 * One word (or phrase) in the message bar. A word is built from [parts] so endings and spelled
 * letters can be removed one at a time: "dog" + "+s" is `["dog", "s"]`, and backspace gives "dog".
 */
data class MessageItem(
    val parts: List<String>,
    val spokenParts: List<String> = parts,
    val image: ImageRef? = null,
) {
    val text: String get() = parts.joinToString("")
    val spoken: String get() = spokenParts.joinToString("")
}

/**
 * The sentence being built in the message bar. Immutable: every change returns a new message.
 *
 * [wordBreak] is set by `:space` so the next appended letter starts a new word.
 */
data class Message(
    val items: List<MessageItem> = emptyList(),
    val wordBreak: Boolean = false,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    /** What the message bar shows. */
    val displayText: String get() = joinWords(items.map { it.text })

    /** What is spoken. Uses each button's vocalization where it has one. */
    val spokenText: String get() = joinWords(items.map { it.spoken })

    fun addWord(text: String, spoken: String = text, image: ImageRef? = null): Message =
        if (text.isBlank() && spoken.isBlank()) this else Message(items + MessageItem(listOf(text), listOf(spoken), image))

    /** Appends to the last word (`+s`, or spelling letter by letter). Starts a new word after a space. */
    fun append(text: String): Message {
        if (text.isEmpty()) return this
        val last = items.lastOrNull()
        if (last == null || wordBreak) return Message(items + MessageItem(listOf(text)))
        val updated = last.copy(parts = last.parts + text, spokenParts = last.spokenParts + text)
        return Message(items.dropLast(1) + updated)
    }

    fun space(): Message = if (items.isEmpty()) this else copy(wordBreak = true)

    /** Removes the last ending or letter, or the last word if it was a single button. */
    fun backspace(): Message {
        if (wordBreak) return copy(wordBreak = false)
        val last = items.lastOrNull() ?: return this
        return if (last.parts.size > 1) {
            Message(items.dropLast(1) + last.copy(parts = last.parts.dropLast(1), spokenParts = last.spokenParts.dropLast(1)))
        } else {
            Message(items.dropLast(1))
        }
    }

    fun removeAt(index: Int): Message =
        if (index !in items.indices) this else Message(items.filterIndexed { i, _ -> i != index })

    fun clear(): Message = Message()

    companion object {
        private val noSpaceBefore = setOf('.', ',', '!', '?', ';', ':', ')', '%')

        /** Joins words with spaces, without a space before punctuation. */
        fun joinWords(words: List<String>): String = buildString {
            for (word in words) {
                if (word.isEmpty()) continue
                val glue = isNotEmpty() && !endsWith("(") && word.first() !in noSpaceBefore
                if (glue) append(' ')
                append(word)
            }
        }
    }
}

/** What should happen after a button press, besides the message changing. */
sealed interface PressEffect {
    /** A word was added or changed; speak it if "speak each button" is on. */
    data class Said(val text: String) : PressEffect

    data class OpenBoard(val boardId: String) : PressEffect

    data object GoHome : PressEffect

    data object GoBack : PressEffect

    data object SpeakMessage : PressEffect
}

data class PressResult(val message: Message, val effects: List<PressEffect>)

/**
 * Applies a button press to the message, following Open Board Format rules:
 * actions run first; a folder button opens its board; any other button adds its word.
 */
fun Message.press(button: Button): PressResult {
    if (button.hidden) return PressResult(this, emptyList())
    var message = this
    val effects = mutableListOf<PressEffect>()
    val actions = button.parsedActions
    for (action in actions) {
        when (action) {
            BoardAction.Clear -> message = message.clear()
            BoardAction.Backspace -> message = message.backspace()
            BoardAction.Space -> message = message.space()
            BoardAction.Speak -> effects += PressEffect.SpeakMessage
            BoardAction.Home -> effects += PressEffect.GoHome
            BoardAction.Back -> effects += PressEffect.GoBack
            is BoardAction.Append -> {
                message = message.append(action.text)
                message.items.lastOrNull()?.let { effects += PressEffect.Said(it.spoken) }
            }
        }
    }
    val linked = button.linkedBoardId
    when {
        linked != null -> effects += PressEffect.OpenBoard(linked)
        actions.isEmpty() && button.actions.isEmpty() -> {
            val next = message.addWord(button.label, button.spokenText, button.image)
            if (next != message) effects += PressEffect.Said(button.spokenText)
            message = next
        }
    }
    return PressResult(message, effects)
}
