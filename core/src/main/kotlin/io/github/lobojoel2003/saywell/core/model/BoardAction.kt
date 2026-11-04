package io.github.lobojoel2003.saywell.core.model

/**
 * Open Board Format button actions that Saywell understands.
 *
 * In `.obf` files these are strings: `:clear`, `:backspace`, `:space`, `:speak`, `:home`,
 * `:back` (an extension many apps use), and `+text` which appends text to the last word
 * (`+s` turns "dog" into "dogs"; spelling boards use `+a`, `+b`, ...).
 */
sealed interface BoardAction {
    val code: String

    data object Clear : BoardAction { override val code = ":clear" }
    data object Backspace : BoardAction { override val code = ":backspace" }
    data object Space : BoardAction { override val code = ":space" }
    data object Speak : BoardAction { override val code = ":speak" }
    data object Home : BoardAction { override val code = ":home" }
    data object Back : BoardAction { override val code = ":back" }

    data class Append(val text: String) : BoardAction {
        override val code get() = "+$text"
    }

    companion object {
        private val named = listOf(Clear, Backspace, Space, Speak, Home, Back).associateBy { it.code }

        /** Parses an action string, or returns null for actions Saywell does not support (they are kept, not run). */
        fun parse(code: String): BoardAction? {
            val trimmed = code.trim()
            return when {
                trimmed.startsWith("+") && trimmed.length > 1 -> Append(trimmed.substring(1))
                else -> named[trimmed.lowercase()]
            }
        }
    }
}

val Button.parsedActions: List<BoardAction> get() = actions.mapNotNull(BoardAction::parse)
