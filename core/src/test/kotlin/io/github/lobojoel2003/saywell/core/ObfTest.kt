package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.ImageRef
import io.github.lobojoel2003.saywell.core.obf.ImageData
import io.github.lobojoel2003.saywell.core.obf.ImageSource
import io.github.lobojoel2003.saywell.core.obf.ObfBoard
import io.github.lobojoel2003.saywell.core.obf.ObfExporter
import io.github.lobojoel2003.saywell.core.obf.ObfImportException
import io.github.lobojoel2003.saywell.core.obf.ObfImporter
import io.github.lobojoel2003.saywell.core.obf.ObfJson
import io.github.lobojoel2003.saywell.core.vocabulary.DefaultVocabulary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObfTest {
    private val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 1, 2, 3)

    private val images = ImageSource { ref ->
        when (ref) {
            is ImageRef.File -> ImageData(png, "image/png")
            is ImageRef.Symbol -> ImageData("<svg/>".toByteArray(), "image/svg+xml")
        }
    }

    private fun sampleSet(): BoardSet {
        val home = Board(
            "home", "Home", 2, 2,
            listOf(
                Button("b1", "I", image = ImageRef.openMoji("1F9D1"), backgroundColor = "#FFF59D"),
                Button("b2", "Food", linkedBoardId = "food", backgroundColor = "#E0E0E0"),
                null,
                Button("b3", "cat", vocalization = "my cat", image = ImageRef.File("cat.png")),
            ),
        )
        val food = Board(
            "food", "Food", 1, 2,
            listOf(Button("f1", "apple"), Button("f2", "plural", actions = listOf("+s"))),
        )
        return BoardSet("home", listOf(home, food))
    }

    @Test
    fun `package round trip keeps boards, links, colours and pictures`() {
        val out = ByteArrayOutputStream()
        ObfExporter.exportPackage(sampleSet(), images, out)
        val result = ObfImporter.import(ByteArrayInputStream(out.toByteArray()))

        assertEquals(2, result.boards.size)
        assertTrue(result.warnings.isEmpty(), result.warnings.toString())
        val home = result.boards.first { it.id == result.rootBoardId }
        assertEquals("Home", home.name)
        assertEquals(2, home.rows)
        assertNull(home.cells[2])

        val i = home.cells[0]!!
        assertEquals("I", i.label)
        assertEquals("#FFF59D", i.backgroundColor)
        assertEquals(ImageRef.openMoji("1F9D1"), i.image)

        val food = result.boards.first { it.name == "Food" }
        assertEquals(food.id, home.cells[1]!!.linkedBoardId)
        assertEquals(listOf("+s"), food.cells[1]!!.actions)

        val cat = home.cells[3]!!
        assertEquals("my cat", cat.vocalization)
        val file = assertIs<ImageRef.File>(cat.image)
        val saved = result.images.single { it.fileName == file.name }
        assertContentEquals(png, saved.bytes)
        assertTrue(file.name.endsWith(".png"))
    }

    @Test
    fun `imported ids never reuse the original ids`() {
        val out = ByteArrayOutputStream()
        ObfExporter.exportPackage(sampleSet(), images, out)
        val result = ObfImporter.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result.boards.none { it.id == "home" || it.id == "food" })
    }

    @Test
    fun `single board export embeds pictures and imports again`() {
        val set = sampleSet()
        val json = ObfExporter.exportBoard(set.root, images, set)
        val parsed = ObfJson.decodeFromString(ObfBoard.serializer(), json)
        assertEquals("open-board-0.1", parsed.format)
        assertTrue(parsed.images.any { it.data!!.startsWith("data:image/png;base64,") })
        assertEquals("rgb(255, 245, 157)", parsed.buttons.first { it.label == "I" }.backgroundColor)

        val result = ObfImporter.import(ByteArrayInputStream(json.toByteArray()))
        val board = result.boards.single()
        // The linked board isn't in a single-board file, so the folder no longer links anywhere.
        assertNull(board.cells[1]!!.linkedBoardId)
        assertEquals(1, result.warnings.size)
        assertContentEquals(png, result.images.single().bytes)
    }

    @Test
    fun `reads loosely written files from other apps`() {
        val json = """
            {
              "format": "open-board-0.1",
              "id": 7,
              "name": "Loose",
              "buttons": [
                {"id": 1, "label": "hi", "background_color": "rgba(0, 0, 255, 0.5)", "image_id": 9, "extra": true},
                {"id": "2", "label": "web", "image_id": "w"},
                {"id": "3", "label": "x", "action": ":clear"}
              ],
              "grid": {"rows": "1", "columns": 3.0, "order": [[1, "2", 3]]},
              "images": [
                {"id": 9, "content_type": "image/png", "data": "data:image/png;base64,iVBORw0K", "width": 100.5},
                {"id": "w", "url": "https://example.org/pic.jpg"}
              ]
            }
        """.trimIndent()
        val result = ObfImporter.importBoard(json)
        val board = result.boards.single()
        assertEquals("Loose", board.name)
        assertEquals(3, board.columns)
        assertEquals("#8080FF", board.cells[0]!!.backgroundColor)
        assertNotNull(board.cells[0]!!.image)
        assertEquals("https://example.org/pic.jpg", result.remoteImages.single().url)
        assertTrue(result.remoteImages.single().fileName.endsWith(".jpg"))
        assertEquals(listOf(":clear"), board.cells[2]!!.actions)
    }

    @Test
    fun `boards without a grid are laid out roughly square`() {
        val buttons = (1..5).joinToString(",") { """{"id": "$it", "label": "w$it"}""" }
        val result = ObfImporter.importBoard("""{"id": "x", "buttons": [$buttons]}""")
        val board = result.boards.single()
        assertEquals(3, board.columns)
        assertEquals(2, board.rows)
        assertEquals(5, board.buttons.size)
    }

    @Test
    fun `oversized boards are limited to twelve by twelve`() {
        val buttons = (1..200).joinToString(",") { """{"id": "$it", "label": "w$it"}""" }
        val order = (0 until 20).joinToString(",") { r -> (1..10).joinToString(",", "[", "]") { c -> "\"${r * 10 + c}\"" } }
        val result = ObfImporter.importBoard("""{"id": "x", "buttons": [$buttons], "grid": {"rows": 20, "columns": 10, "order": [$order]}}""")
        val board = result.boards.single()
        assertEquals(12, board.rows)
        assertEquals(10, board.columns)
        assertEquals(120, board.buttons.size)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `packages without a manifest still import`() {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("one.obf"))
                zip.write("""{"id": "1", "name": "One", "buttons": [{"id": "a", "label": "a"}]}""".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val result = ObfImporter.import(ByteArrayInputStream(bytes))
        assertEquals("One", result.boards.single().name)
    }

    @Test
    fun `explains files that are not boards`() {
        assertFailsWith<ObfImportException> { ObfImporter.import(ByteArrayInputStream("hello".toByteArray())) }
        assertFailsWith<ObfImportException> { ObfImporter.import(ByteArrayInputStream("PK not really a zip".toByteArray())) }
    }

    @Test
    fun `the starter vocabulary exports and imports completely`() {
        val set = DefaultVocabulary.create()
        val out = ByteArrayOutputStream()
        ObfExporter.exportPackage(set, images, out)
        val result = ObfImporter.import(ByteArrayInputStream(out.toByteArray()))
        assertEquals(set.boards.size, result.boards.size)
        assertEquals(set.boards.sumOf { it.buttons.size }, result.boards.sumOf { it.buttons.size })
        assertEquals(set.boards.flatMap { it.buttons }.count { it.isFolder }, result.boards.flatMap { it.buttons }.count { it.isFolder })
        assertTrue(result.warnings.isEmpty())
    }
}
