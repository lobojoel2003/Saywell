package io.github.lobojoel2003.saywell.core.model

import kotlin.math.roundToInt

/**
 * Button colours. Saywell stores colours as `#RRGGBB`; Open Board Format files from other apps use
 * `rgb(...)`, `rgba(...)`, `#rgb`, `#rrggbb` or a few colour names.
 */
object Colors {
    /**
     * The colour coding most AAC systems use (a modified Fitzgerald Key), so words of the same kind
     * are easy to find.
     */
    const val PEOPLE = "#FFF59D" // pronouns and people: yellow
    const val ACTIONS = "#C5E1A5" // verbs: green
    const val DESCRIBE = "#90CAF9" // adjectives and adverbs: blue
    const val THINGS = "#FFCC80" // nouns: orange
    const val SOCIAL = "#F8BBD0" // social words and phrases: pink
    const val QUESTIONS = "#CE93D8" // question words: purple
    const val NEGATION = "#EF9A9A" // no, not, stop: red
    const val LITTLE_WORDS = "#FFFFFF" // prepositions, determiners: white
    const val FOLDER = "#E0E0E0" // links to other boards: grey
    const val TIME = "#BCAAA4" // time words: brown

    /** Swatches offered in the button editor. */
    val palette = listOf(
        LITTLE_WORDS, PEOPLE, ACTIONS, DESCRIBE, THINGS, SOCIAL, QUESTIONS, NEGATION, FOLDER, TIME,
        "#80DEEA", "#FFAB91",
    )

    private val named = mapOf(
        "white" to "#FFFFFF", "black" to "#000000", "red" to "#FF0000", "green" to "#008000",
        "blue" to "#0000FF", "yellow" to "#FFFF00", "orange" to "#FFA500", "purple" to "#800080",
        "pink" to "#FFC0CB", "gray" to "#808080", "grey" to "#808080", "brown" to "#A52A2A",
        "transparent" to null,
    )

    private val rgbPattern = Regex("""rgba?\(\s*([\d.]+%?)\s*,\s*([\d.]+%?)\s*,\s*([\d.]+%?)\s*(?:,\s*([\d.]+%?)\s*)?\)""")

    /**
     * Converts a CSS-style colour to `#RRGGBB`. Semi-transparent colours are blended onto white,
     * which is how other AAC apps draw them. Returns null for anything it can't read.
     */
    fun normalize(color: String?): String? {
        val value = color?.trim()?.lowercase().orEmpty()
        if (value.isEmpty()) return null
        if (value in named) return named[value]
        if (value.startsWith("#")) {
            val hex = value.substring(1)
            return when {
                hex.length == 3 && hex.all(::isHex) -> "#" + hex.map { "$it$it" }.joinToString("").uppercase()
                hex.length == 6 && hex.all(::isHex) -> "#" + hex.uppercase()
                hex.length == 8 && hex.all(::isHex) -> "#" + hex.substring(0, 6).uppercase()
                else -> null
            }
        }
        val match = rgbPattern.matchEntire(value) ?: return null
        val (r, g, b) = (1..3).map { channel(match.groupValues[it]) ?: return null }
        val alpha = match.groupValues[4].takeIf { it.isNotEmpty() }?.let(::alpha) ?: 1.0
        fun blend(c: Int) = (c * alpha + 255 * (1 - alpha)).roundToInt().coerceIn(0, 255)
        return hex(blend(r), blend(g), blend(b))
    }

    /** `#RRGGBB` as rgb(r, g, b), the form the Open Board Format spec uses. */
    fun toCss(color: String): String? {
        val hex = normalize(color) ?: return null
        val rgb = hex.substring(1).toInt(16)
        return "rgb(${rgb shr 16 and 0xFF}, ${rgb shr 8 and 0xFF}, ${rgb and 0xFF})"
    }

    /** `#RRGGBB` as an ARGB int for drawing. */
    fun toArgb(color: String?): Long? {
        val hex = normalize(color) ?: return null
        return 0xFF000000L or hex.substring(1).toLong(16)
    }

    /** True when black text reads better than white text on this background. */
    fun prefersDarkText(color: String?): Boolean {
        val hex = normalize(color) ?: return true
        val rgb = hex.substring(1).toInt(16)
        fun linear(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        val luminance = 0.2126 * linear(rgb shr 16 and 0xFF) + 0.7152 * linear(rgb shr 8 and 0xFF) +
            0.0722 * linear(rgb and 0xFF)
        // Contrast against black vs. against white
        return (luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05)
    }

    private fun isHex(c: Char) = c in '0'..'9' || c in 'a'..'f'

    private fun channel(raw: String): Int? = if (raw.endsWith("%")) {
        raw.dropLast(1).toDoubleOrNull()?.let { (it * 2.55).roundToInt() }
    } else {
        raw.toDoubleOrNull()?.roundToInt()
    }?.coerceIn(0, 255)

    private fun alpha(raw: String): Double? = if (raw.endsWith("%")) {
        raw.dropLast(1).toDoubleOrNull()?.div(100)
    } else {
        raw.toDoubleOrNull()
    }?.coerceIn(0.0, 1.0)

    private fun hex(r: Int, g: Int, b: Int) = "#%02X%02X%02X".format(r, g, b)
}
