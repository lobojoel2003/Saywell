package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.EditException
import io.github.lobojoel2003.saywell.core.model.ImageRef
import io.github.lobojoel2003.saywell.core.model.addBoard
import io.github.lobojoel2003.saywell.core.model.deleteBoard
import io.github.lobojoel2003.saywell.core.model.imageFiles
import io.github.lobojoel2003.saywell.core.model.resized
import io.github.lobojoel2003.saywell.core.model.swapCells
import io.github.lobojoel2003.saywell.core.model.unreachableBoards
import io.github.lobojoel2003.saywell.core.model.withButtonAt
import io.github.lobojoel2003.saywell.core.model.withoutBrokenLinks
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BoardEditingTest {
    private fun button(id: String, link: String? = null) = Button(id, id, linkedBoardId = link)

    private fun grid(rows: Int, columns: Int, vararg ids: String?) =
        Board("b", "Board", rows, columns, ids.map { it?.let(::button) } + List(rows * columns - ids.size) { null })

    @Test
    fun `resizing keeps buttons in place and moves the rest to free cells`() {
        val board = grid(2, 3, "a", "b", "c", "d", null, null)
        val smaller = board.resized(2, 2)
        assertEquals(listOf("a", "b", "d", "c"), smaller.cells.map { it?.id })
    }

    @Test
    fun `resizing larger keeps row and column`() {
        val board = grid(2, 2, "a", "b", "c", "d")
        val bigger = board.resized(3, 3)
        assertEquals("a", bigger.cell(0, 0)?.id)
        assertEquals("b", bigger.cell(0, 1)?.id)
        assertEquals("c", bigger.cell(1, 0)?.id)
        assertEquals("d", bigger.cell(1, 1)?.id)
        assertNull(bigger.cell(2, 2))
    }

    @Test
    fun `resizing below the button count explains what to do`() {
        val board = grid(2, 2, "a", "b", "c", "d")
        val error = assertFailsWith<EditException> { board.resized(1, 3) }
        assertEquals("This board has 4 buttons. Remove 1 before making it smaller.", error.message)
    }

    @Test
    fun `swap and place buttons`() {
        val board = grid(1, 3, "a", null, "c").swapCells(0, 1).withButtonAt(2, null)
        assertEquals(listOf(null, "a", null), board.cells.map { it?.id })
    }

    @Test
    fun `deleting a board unlinks folders that opened it`() {
        val home = Board("home", "Home", 1, 2, listOf(button("toFood", link = "food"), button("x")))
        val food = Board.empty("food", "Food", 1, 1)
        val set = BoardSet("home", listOf(home, food)).deleteBoard("food")
        assertEquals(listOf("home"), set.boards.map { it.id })
        assertNull(set.root.cells[0]?.linkedBoardId)
        assertFailsWith<EditException> { set.deleteBoard("home") }
    }

    @Test
    fun `finds boards nothing links to`() {
        val home = Board("home", "Home", 1, 1, listOf(button("toA", link = "a")))
        val set = BoardSet("home", listOf(home, Board.empty("a", "A", 1, 1)))
            .addBoard(Board.empty("orphan", "Orphan", 1, 1))
        assertEquals(listOf("orphan"), set.unreachableBoards().map { it.id })
    }

    @Test
    fun `drops links to missing boards`() {
        val home = Board("home", "Home", 1, 1, listOf(button("toGone", link = "gone")))
        val fixed = BoardSet("home", listOf(home)).withoutBrokenLinks()
        assertNull(fixed.root.cells[0]?.linkedBoardId)
    }

    @Test
    fun `lists image files in use`() {
        val home = Board(
            "home", "Home", 1, 2,
            listOf(Button("a", "a", image = ImageRef.File("cat.jpg")), Button("b", "b", image = ImageRef.openMoji("1F600"))),
        )
        assertEquals(setOf("cat.jpg"), BoardSet("home", listOf(home)).imageFiles())
    }

    @Test
    fun `board sets survive a save and load`() {
        val set = BoardSet(
            "home",
            listOf(Board("home", "Home", 1, 2, listOf(Button("a", "a", image = ImageRef.openMoji("1F600"), actions = listOf("+s")), null))),
        )
        val json = Json.encodeToString(BoardSet.serializer(), set)
        assertEquals(set, Json.decodeFromString(BoardSet.serializer(), json))
    }

    @Test
    fun `boards reject a cell count that does not match the grid`() {
        assertFailsWith<IllegalArgumentException> { Board("x", "X", 2, 2, listOf(null)) }
    }
}
