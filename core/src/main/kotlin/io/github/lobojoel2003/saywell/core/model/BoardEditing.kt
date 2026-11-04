package io.github.lobojoel2003.saywell.core.model

import java.util.UUID

/** Short random ids for boards and buttons. */
object Ids {
    fun new(): String = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
}

/** Why an edit could not be made, in words a caregiver can act on. */
class EditException(message: String) : IllegalArgumentException(message)

/** Puts [button] in cell [index] (row * columns + column), replacing what was there. */
fun Board.withButtonAt(index: Int, button: Button?): Board {
    require(index in cells.indices) { "Cell $index is outside the ${rows}x$columns grid" }
    return copy(cells = cells.toMutableList().also { it[index] = button })
}

/** Swaps two cells. Moving a button onto an empty cell moves it there. */
fun Board.swapCells(from: Int, to: Int): Board {
    require(from in cells.indices && to in cells.indices) { "Cell out of range" }
    if (from == to) return this
    val list = cells.toMutableList()
    list[from] = cells[to]
    list[to] = cells[from]
    return copy(cells = list)
}

/**
 * Changes the grid size. Buttons keep their row and column when they still fit; the rest move to
 * the first empty cells. Throws [EditException] if there are more buttons than cells.
 */
fun Board.resized(newRows: Int, newColumns: Int): Board {
    if (newRows !in 1..MAX_ROWS || newColumns !in 1..MAX_COLUMNS) {
        throw EditException("Boards can have 1 to $MAX_ROWS rows and 1 to $MAX_COLUMNS columns.")
    }
    if (newRows == rows && newColumns == columns) return this
    val count = buttons.size
    if (count > newRows * newColumns) {
        val extra = count - newRows * newColumns
        throw EditException("This board has $count buttons. Remove $extra before making it smaller.")
    }
    val result = MutableList<Button?>(newRows * newColumns) { null }
    val overflow = mutableListOf<Button>()
    for (r in 0 until rows) {
        for (c in 0 until columns) {
            val button = cell(r, c) ?: continue
            if (r < newRows && c < newColumns) result[r * newColumns + c] = button else overflow += button
        }
    }
    for (button in overflow) {
        val free = result.indexOfFirst { it == null }
        result[free] = button
    }
    return copy(rows = newRows, columns = newColumns, cells = result)
}

fun BoardSet.replaceBoard(board: Board): BoardSet {
    require(boards.any { it.id == board.id }) { "No board ${board.id}" }
    return copy(boards = boards.map { if (it.id == board.id) board else it })
}

fun BoardSet.addBoard(board: Board): BoardSet {
    require(board(board.id) == null) { "Board ${board.id} already exists" }
    return copy(boards = boards + board)
}

/**
 * Deletes a board. Buttons that opened it stay but no longer link anywhere. The home board can't be
 * deleted.
 */
fun BoardSet.deleteBoard(boardId: String): BoardSet {
    if (boardId == rootBoardId) throw EditException("The home board can't be deleted.")
    val remaining = boards.filter { it.id != boardId }
    if (remaining.size == boards.size) return this
    return copy(
        boards = remaining.map { board ->
            if (board.cells.none { it?.linkedBoardId == boardId }) {
                board
            } else {
                board.copy(cells = board.cells.map { if (it?.linkedBoardId == boardId) it.copy(linkedBoardId = null) else it })
            }
        },
    )
}

/** Boards that no button links to and that are not the home board. */
fun BoardSet.unreachableBoards(): List<Board> {
    val seen = mutableSetOf<String>()
    val queue = ArrayDeque(listOf(root.id))
    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        if (!seen.add(id)) continue
        board(id)?.buttons?.mapNotNullTo(queue) { it.linkedBoardId }
    }
    return boards.filter { it.id !in seen }
}

/** Every image file the boards still use, so unused photos can be cleaned up. */
fun BoardSet.imageFiles(): Set<String> =
    boards.flatMap { it.buttons }.mapNotNull { (it.image as? ImageRef.File)?.name }.toSet()

/** Removes links to boards that don't exist, for example after a partial import. */
fun BoardSet.withoutBrokenLinks(): BoardSet {
    val ids = boards.map { it.id }.toSet()
    if (boards.flatMap { it.buttons }.all { it.linkedBoardId == null || it.linkedBoardId in ids }) return this
    return copy(
        boards = boards.map { board ->
            board.copy(cells = board.cells.map { b -> if (b?.linkedBoardId != null && b.linkedBoardId !in ids) b.copy(linkedBoardId = null) else b })
        },
    )
}
