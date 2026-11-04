package io.github.lobojoel2003.saywell.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Largest grid Saywell lets you build. Bigger boards from other apps are imported page by page. */
const val MAX_ROWS = 12
const val MAX_COLUMNS = 12

/**
 * Everything a person can say with Saywell: a set of boards linked together, starting from [rootBoardId].
 */
@Serializable
data class BoardSet(
    val rootBoardId: String,
    val boards: List<Board>,
    val version: Int = CURRENT_VERSION,
) {
    init {
        require(boards.isNotEmpty()) { "A board set needs at least one board" }
        require(boards.map { it.id }.toSet().size == boards.size) { "Board ids must be unique" }
    }

    fun board(id: String): Board? = boards.firstOrNull { it.id == id }

    /** The home board. Falls back to the first board if the root id is stale. */
    val root: Board get() = board(rootBoardId) ?: boards.first()

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * One page of buttons laid out in a [rows] x [columns] grid. [cells] holds the grid row by row;
 * `null` is an empty cell.
 */
@Serializable
data class Board(
    val id: String,
    val name: String,
    val rows: Int,
    val columns: Int,
    val cells: List<Button?>,
    val locale: String = "en",
) {
    init {
        require(rows in 1..MAX_ROWS) { "rows must be 1..$MAX_ROWS, was $rows" }
        require(columns in 1..MAX_COLUMNS) { "columns must be 1..$MAX_COLUMNS, was $columns" }
        require(cells.size == rows * columns) { "Board $id has ${cells.size} cells for a ${rows}x$columns grid" }
    }

    fun cell(row: Int, column: Int): Button? = cells[row * columns + column]

    val buttons: List<Button> get() = cells.filterNotNull()

    fun indexOf(buttonId: String): Int = cells.indexOfFirst { it?.id == buttonId }

    companion object {
        fun empty(id: String, name: String, rows: Int, columns: Int, locale: String = "en") =
            Board(id, name, rows, columns, List(rows * columns) { null }, locale)
    }
}

/**
 * A button on a board.
 *
 * - A plain button adds its word to the message bar (and can speak it straight away).
 * - A button with [linkedBoardId] opens another board (a "folder").
 * - [actions] are Open Board Format actions such as `:clear`, `:backspace`, `:speak`, `:home`,
 *   `:space`, or `+s` to add an ending to the last word. See [BoardAction].
 */
@Serializable
data class Button(
    val id: String,
    val label: String,
    /** What to say when it differs from the label, for example label "hi" says "Hello!". */
    val vocalization: String? = null,
    val image: ImageRef? = null,
    /** Background colour as `#RRGGBB`. */
    val backgroundColor: String? = null,
    /** Border colour as `#RRGGBB`. */
    val borderColor: String? = null,
    val linkedBoardId: String? = null,
    val actions: List<String> = emptyList(),
    /** Hidden buttons keep their place but are not shown while talking. */
    val hidden: Boolean = false,
) {
    /** The text spoken for this button. */
    val spokenText: String get() = vocalization?.takeIf { it.isNotBlank() } ?: label

    val isFolder: Boolean get() = linkedBoardId != null
}

/** Where a button's picture comes from. */
@Serializable
sealed interface ImageRef {
    /** A picture from a bundled symbol set, for example OpenMoji `1F600`. */
    @Serializable
    @SerialName("symbol")
    data class Symbol(val set: String, val code: String) : ImageRef

    /** A picture stored in Saywell's private images folder (photos, imported board images). */
    @Serializable
    @SerialName("file")
    data class File(val name: String) : ImageRef

    companion object {
        const val OPENMOJI = "openmoji"

        fun openMoji(code: String) = Symbol(OPENMOJI, code)
    }
}
