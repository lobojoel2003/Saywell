package io.github.lobojoel2003.saywell.core.obf

import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.Colors
import io.github.lobojoel2003.saywell.core.model.ImageRef
import kotlinx.serialization.encodeToString
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.encoding.Base64

/** Picture bytes and their MIME type, for example `image/png`. */
class ImageData(val bytes: ByteArray, val contentType: String)

/** Reads the picture behind an [ImageRef]. Returns null if it is missing. */
fun interface ImageSource {
    fun read(image: ImageRef): ImageData?
}

/** Writes Saywell boards as Open Board Format files that other AAC apps can open. */
object ObfExporter {
    /** A single board as a self-contained `.obf` file with pictures embedded. */
    fun exportBoard(board: Board, images: ImageSource, boardSet: BoardSet? = null): String {
        val obf = toObf(board, boardSet, images, packaged = null)
        return ObfJson.encodeToString(obf)
    }

    /**
     * Every board as an `.obz` package (a zip with `manifest.json`, one `.obf` per board and the
     * pictures). Links between boards keep working in apps that read `.obz`.
     */
    fun exportPackage(boardSet: BoardSet, images: ImageSource, output: OutputStream) {
        val files = linkedMapOf<String, ByteArray>()
        val boardPaths = linkedMapOf<String, String>()
        val imagePaths = linkedMapOf<String, String>()
        for (board in boardSet.boards) {
            val obf = toObf(board, boardSet, images, packaged = files)
            val path = boardPath(board.id)
            boardPaths[board.id] = path
            obf.images.forEach { image -> image.path?.let { imagePaths[image.id] = it } }
            files[path] = ObfJson.encodeToString(obf).toByteArray()
        }
        val manifest = ObfManifest(
            root = boardPath(boardSet.root.id),
            paths = ObfPaths(boards = boardPaths, images = imagePaths),
        )
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(ObfJson.encodeToString(manifest).toByteArray())
            zip.closeEntry()
            for ((path, bytes) in files) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    internal fun boardPath(id: String) = "boards/${safeName(id)}.obf"

    /**
     * Converts one board. With [packaged] set, pictures are added to that map and referenced by
     * path; otherwise they are embedded as data URIs.
     */
    internal fun toObf(board: Board, boardSet: BoardSet?, images: ImageSource, packaged: MutableMap<String, ByteArray>?): ObfBoard {
        val obfImages = linkedMapOf<String, ObfImage>()

        fun imageId(ref: ImageRef): String? {
            val id = when (ref) {
                is ImageRef.Symbol -> "${ref.set}-${ref.code}"
                is ImageRef.File -> "file-${ref.name.substringBeforeLast('.')}"
            }.let(::safeName)
            if (id in obfImages) return id
            val data = images.read(ref)
            val symbol = (ref as? ImageRef.Symbol)?.let { ObfSymbol(it.set, "${it.code}.svg") }
            val license = if (ref is ImageRef.Symbol && ref.set == ImageRef.OPENMOJI) OPENMOJI_LICENSE else null
            if (data == null && symbol == null) return null
            var path: String? = null
            var dataUri: String? = null
            if (data != null) {
                if (packaged != null) {
                    path = "images/$id.${extensionFor(data.contentType)}"
                    packaged[path] = data.bytes
                } else {
                    dataUri = "data:${data.contentType};base64," + Base64.encode(data.bytes)
                }
            }
            obfImages[id] = ObfImage(
                id = id,
                contentType = data?.contentType,
                data = dataUri,
                path = path,
                symbol = symbol,
                license = license,
            )
            return id
        }

        val buttons = board.buttons.map { button -> toObfButton(button, boardSet, packaged != null, button.image?.let(::imageId)) }
        val order = (0 until board.rows).map { r -> (0 until board.columns).map { c -> board.cell(r, c)?.id } }
        return ObfBoard(
            id = board.id,
            locale = board.locale,
            name = board.name,
            buttons = buttons,
            grid = ObfGrid(board.rows, board.columns, order),
            images = obfImages.values.toList(),
        )
    }

    private fun toObfButton(button: Button, boardSet: BoardSet?, packaged: Boolean, imageId: String?): ObfButton {
        val linked = button.linkedBoardId?.let { id ->
            ObfLoadBoard(
                id = id,
                name = boardSet?.board(id)?.name,
                path = if (packaged) boardPath(id) else null,
            )
        }
        return ObfButton(
            id = button.id,
            label = button.label,
            vocalization = button.vocalization?.takeIf { it.isNotBlank() && it != button.label },
            imageId = imageId,
            backgroundColor = button.backgroundColor?.let(Colors::toCss),
            borderColor = button.borderColor?.let(Colors::toCss),
            loadBoard = linked,
            action = button.actions.singleOrNull(),
            actions = button.actions.takeIf { it.size > 1 },
            hidden = button.hidden.takeIf { it },
        )
    }

    internal fun extensionFor(contentType: String): String = when (contentType.lowercase().substringBefore(';').trim()) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/svg+xml" -> "svg"
        else -> "bin"
    }

    private fun safeName(id: String) = id.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private val OPENMOJI_LICENSE = ObfLicense(
        type = "CC BY-SA 4.0",
        copyrightNoticeUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
        sourceUrl = "https://openmoji.org/",
        authorName = "OpenMoji",
        authorUrl = "https://openmoji.org/",
    )
}
