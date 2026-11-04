package io.github.lobojoel2003.saywell.core.obf

import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.Colors
import io.github.lobojoel2003.saywell.core.model.Ids
import io.github.lobojoel2003.saywell.core.model.ImageRef
import io.github.lobojoel2003.saywell.core.model.MAX_COLUMNS
import io.github.lobojoel2003.saywell.core.model.MAX_ROWS
import kotlinx.serialization.SerializationException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import kotlin.io.encoding.Base64
import kotlin.math.ceil
import kotlin.math.sqrt

/** A picture to save in Saywell's images folder under [fileName]. */
class ImportedImage(val fileName: String, val bytes: ByteArray)

/** A picture that is only available online. Saywell fetches it if there is a connection. */
data class RemoteImage(val fileName: String, val url: String)

/**
 * Boards read from an `.obf` or `.obz` file, with fresh ids so they never clash with boards that
 * are already on the device.
 */
class ImportResult(
    val boards: List<Board>,
    val rootBoardId: String,
    val images: List<ImportedImage>,
    val remoteImages: List<RemoteImage>,
    val warnings: List<String>,
)

/** The file could not be read as an Open Board Format file. The message is shown to the person. */
class ObfImportException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Reads Open Board Format files (`.obf` boards and `.obz` packages) from other AAC apps. */
object ObfImporter {
    private const val MAX_ENTRIES = 10_000
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 512L * 1024 * 1024

    /** Detects the file type from its first bytes (`.obz` is a zip) and imports it. */
    fun import(input: InputStream): ImportResult {
        val buffered = input.buffered()
        buffered.mark(4)
        val magic = ByteArray(2)
        val read = buffered.read(magic)
        buffered.reset()
        return if (read == 2 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()) {
            importPackage(buffered)
        } else {
            importBoard(buffered.readBytes().toString(Charsets.UTF_8))
        }
    }

    /** One `.obf` board. Pictures must be embedded or online; links to other boards are dropped. */
    fun importBoard(json: String): ImportResult {
        val board = parseBoard(json, "the file")
        return convert(listOf(Source(null, board)), rootIndex = 0) { image -> image.data?.let(::decodeDataUri) }
    }

    /** An `.obz` package: a zip with `manifest.json`, boards and pictures. */
    fun importPackage(input: InputStream): ImportResult {
        val files = readZip(input)
        val manifest = files["manifest.json"]?.let { bytes ->
            try {
                ObfJson.decodeFromString(ObfManifest.serializer(), bytes.toString(Charsets.UTF_8))
            } catch (e: SerializationException) {
                throw ObfImportException("The package's manifest.json could not be read.", e)
            }
        }
        val boardPaths = manifest?.paths?.boards?.values?.map(::normalizePath)?.distinct()?.takeIf { it.isNotEmpty() }
            ?: files.keys.filter { it.endsWith(".obf", ignoreCase = true) }.sorted()
        if (boardPaths.isEmpty()) throw ObfImportException("This package doesn't contain any boards.")
        val sources = boardPaths.mapNotNull { path ->
            val bytes = files[path] ?: return@mapNotNull null
            Source(path, parseBoard(bytes.toString(Charsets.UTF_8), path))
        }
        if (sources.isEmpty()) throw ObfImportException("The boards listed in this package are missing.")
        val rootPath = manifest?.root?.let(::normalizePath)
        val rootIndex = sources.indexOfFirst { it.path == rootPath }.coerceAtLeast(0)
        return convert(sources, rootIndex) { image ->
            image.data?.let(::decodeDataUri) ?: image.path?.let { files[normalizePath(it)] }
        }
    }

    private class Source(val path: String?, val board: ObfBoard)

    private fun parseBoard(json: String, where: String): ObfBoard = try {
        ObfJson.decodeFromString(ObfBoard.serializer(), json)
    } catch (e: SerializationException) {
        throw ObfImportException("$where is not an Open Board Format board.", e)
    } catch (e: IllegalArgumentException) {
        throw ObfImportException("$where is not an Open Board Format board.", e)
    }

    private fun convert(sources: List<Source>, rootIndex: Int, imageBytes: (ObfImage) -> ByteArray?): ImportResult {
        val warnings = linkedSetOf<String>()
        val newIdByObfId = mutableMapOf<String, String>()
        val newIdByPath = mutableMapOf<String, String>()
        val newIds = sources.map { Ids.new() }
        sources.forEachIndexed { i, source ->
            newIdByObfId.putIfAbsent(source.board.id, newIds[i])
            source.path?.let { newIdByPath[it] = newIds[i] }
        }

        val images = mutableListOf<ImportedImage>()
        val remote = mutableListOf<RemoteImage>()

        val boards = sources.mapIndexed { i, source ->
            val obf = source.board
            val imageRefs = mutableMapOf<String, ImageRef?>()
            fun image(id: String?): ImageRef? {
                if (id == null) return null
                return imageRefs.getOrPut(id) {
                    val image = obf.images.firstOrNull { it.id == id } ?: return@getOrPut null
                    resolveImage(image, imageBytes, images, remote)
                }
            }

            fun link(load: ObfLoadBoard?): String? {
                if (load == null) return null
                val target = load.id?.let { newIdByObfId[it] } ?: load.path?.let { newIdByPath[normalizePath(it)] }
                if (target == null) warnings += "Some folder buttons link to boards that weren't in the file. They now do nothing."
                return target
            }

            val buttons = obf.buttons.associateBy { it.id }
            val layout = layout(obf, warnings)
            val cells = layout.cells.map { buttonId ->
                val b = buttonId?.let { buttons[it] } ?: return@map null
                Button(
                    id = Ids.new(),
                    label = b.label.orEmpty(),
                    vocalization = b.vocalization?.takeIf { it.isNotBlank() && it != b.label },
                    image = image(b.imageId),
                    backgroundColor = Colors.normalize(b.backgroundColor),
                    borderColor = Colors.normalize(b.borderColor),
                    linkedBoardId = link(b.loadBoard),
                    actions = (b.actions ?: listOfNotNull(b.action)).filter { it.isNotBlank() },
                    hidden = b.hidden == true,
                )
            }
            Board(
                id = newIds[i],
                name = obf.name?.takeIf { it.isNotBlank() } ?: "Imported board",
                rows = layout.rows,
                columns = layout.columns,
                cells = cells,
                locale = obf.locale?.takeIf { it.isNotBlank() } ?: "en",
            )
        }
        return ImportResult(boards, newIds[rootIndex], images, remote, warnings.toList())
    }

    private class Layout(val rows: Int, val columns: Int, val cells: List<String?>)

    /** Places buttons on a grid of at most [MAX_ROWS] x [MAX_COLUMNS]. */
    private fun layout(obf: ObfBoard, warnings: MutableSet<String>): Layout {
        val order = obf.grid?.order.orEmpty()
        val known = obf.buttons.map { it.id }.toSet()
        if (order.isEmpty() || order.all { row -> row.all { it == null } }) {
            // No grid: lay the buttons out roughly square.
            val ids = obf.buttons.filter { it.hidden != true }.map { it.id }
            val count = ids.size.coerceAtLeast(1)
            val columns = ceil(sqrt(count.toDouble())).toInt().coerceIn(1, MAX_COLUMNS)
            val rows = ceil(count / columns.toDouble()).toInt().coerceIn(1, MAX_ROWS)
            val cells = MutableList<String?>(rows * columns) { null }
            ids.take(cells.size).forEachIndexed { i, id -> cells[i] = id }
            if (ids.size > cells.size) warnings += "Some buttons didn't fit on a ${MAX_ROWS}x$MAX_COLUMNS board and were left out."
            return Layout(rows, columns, cells)
        }
        val gridRows = maxOf(obf.grid?.rows ?: 0, order.size).coerceAtLeast(1)
        val gridColumns = maxOf(obf.grid?.columns ?: 0, order.maxOf { it.size }).coerceAtLeast(1)
        val rows = gridRows.coerceAtMost(MAX_ROWS)
        val columns = gridColumns.coerceAtMost(MAX_COLUMNS)
        val cells = MutableList<String?>(rows * columns) { null }
        val overflow = mutableListOf<String>()
        order.forEachIndexed { r, row ->
            row.forEachIndexed { c, id ->
                if (id == null || id !in known) return@forEachIndexed
                if (r < rows && c < columns) cells[r * columns + c] = id else overflow += id
            }
        }
        for (id in overflow) {
            val free = cells.indexOfFirst { it == null }
            if (free < 0) {
                warnings += "Some buttons didn't fit on a ${MAX_ROWS}x$MAX_COLUMNS board and were left out."
                break
            }
            cells[free] = id
        }
        return Layout(rows, columns, cells)
    }

    private fun resolveImage(
        image: ObfImage,
        imageBytes: (ObfImage) -> ByteArray?,
        images: MutableList<ImportedImage>,
        remote: MutableList<RemoteImage>,
    ): ImageRef? {
        val symbol = image.symbol
        if (symbol?.set.equals(ImageRef.OPENMOJI, ignoreCase = true) && !symbol?.filename.isNullOrBlank()) {
            val code = symbol.filename.substringAfterLast('/').substringBeforeLast('.').uppercase()
            return ImageRef.openMoji(code)
        }
        val bytes = imageBytes(image)
        val extension = image.contentType?.let(ObfExporter::extensionFor)?.takeIf { it != "bin" }
            ?: image.path?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in IMAGE_EXTENSIONS }
            ?: bytes?.let(::sniffExtension)
            ?: image.url?.substringBefore('?')?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in IMAGE_EXTENSIONS }
            ?: "png"
        val name = "${Ids.new()}.$extension"
        if (bytes != null && bytes.isNotEmpty()) {
            images += ImportedImage(name, bytes)
            return ImageRef.File(name)
        }
        val url = image.url ?: image.dataUrl
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
            remote += RemoteImage(name, url)
            return ImageRef.File(name)
        }
        return null
    }

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "svg")

    private fun sniffExtension(bytes: ByteArray): String? = when {
        bytes.size > 4 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "png"
        bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
        bytes.size > 3 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() -> "gif"
        bytes.size > 12 && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
        String(bytes, 0, minOf(bytes.size, 256), Charsets.UTF_8).contains("<svg") -> "svg"
        else -> null
    }

    /** Decodes `data:<type>;base64,<data>` and percent-encoded `data:` URIs. */
    internal fun decodeDataUri(uri: String): ByteArray? {
        if (!uri.startsWith("data:")) return null
        val comma = uri.indexOf(',')
        if (comma < 0) return null
        val meta = uri.substring(5, comma)
        val payload = uri.substring(comma + 1)
        return try {
            if (meta.split(';').any { it.equals("base64", ignoreCase = true) }) {
                Base64.decode(payload.filterNot { it.isWhitespace() })
            } else {
                URLDecoder.decode(payload.replace("+", "%2B"), "UTF-8").toByteArray()
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun normalizePath(path: String): String = path.trim().replace('\\', '/').removePrefix("./").removePrefix("/")

    /** Reads a zip into memory with limits, so a malformed or hostile file can't exhaust storage. */
    private fun readZip(input: InputStream): Map<String, ByteArray> {
        val files = mutableMapOf<String, ByteArray>()
        var total = 0L
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (files.size >= MAX_ENTRIES) throw ObfImportException("This package has too many files.")
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        size += n
                        total += n
                        if (size > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) throw ObfImportException("This package is too large to import.")
                        out.write(buffer, 0, n)
                    }
                    files[normalizePath(entry.name)] = out.toByteArray()
                }
            }
        } catch (e: java.util.zip.ZipException) {
            throw ObfImportException("This file is not a valid .obz package.", e)
        }
        if (files.isEmpty()) throw ObfImportException("This package is empty.")
        return files
    }
}
