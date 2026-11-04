package io.github.lobojoel2003.saywell.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.Colors
import io.github.lobojoel2003.saywell.core.model.Ids
import io.github.lobojoel2003.saywell.core.obf.ObfExporter
import io.github.lobojoel2003.saywell.core.obf.ObfImportException
import io.github.lobojoel2003.saywell.core.obf.ObfImporter
import io.github.lobojoel2003.saywell.core.obf.RemoteImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class ImportMode {
    /** Keep my boards and add the new ones, with a folder button on the home board if there is room. */
    ADD,

    /** Replace all my boards with the imported ones. */
    REPLACE,
}

data class ImportSummary(
    val boardCount: Int,
    val homeLinkAdded: Boolean,
    val picturesMissing: Int,
    val warnings: List<String>,
)

/** Imports and exports Open Board Format files through Android's file picker. */
class BoardTransfer(
    private val resolver: ContentResolver,
    private val boards: BoardRepository,
    private val images: ImageStore,
) {
    suspend fun import(uri: Uri, mode: ImportMode): ImportSummary = withContext(Dispatchers.IO) {
        val result = resolver.openInputStream(uri)?.use { ObfImporter.import(it) }
            ?: throw ObfImportException("The file could not be opened.")
        result.images.forEach { images.saveBytes(it.fileName, it.bytes) }
        val missing = result.remoteImages.count { !download(it) }

        var homeLink = false
        when (mode) {
            ImportMode.REPLACE -> boards.replaceAll(BoardSet(result.rootBoardId, result.boards))
            ImportMode.ADD -> boards.update { current ->
                var set = current.copy(boards = current.boards + result.boards)
                val home = set.root
                val free = home.cells.indexOfFirst { it == null }
                if (free >= 0) {
                    val root = result.boards.first { it.id == result.rootBoardId }
                    val folder = Button(
                        id = Ids.new(),
                        label = root.name,
                        image = root.buttons.firstOrNull { it.image != null }?.image,
                        backgroundColor = Colors.FOLDER,
                        linkedBoardId = root.id,
                    )
                    set = set.copy(boards = set.boards.map { if (it.id == home.id) it.copy(cells = it.cells.toMutableList().also { c -> c[free] = folder }) else it })
                    homeLink = true
                }
                set
            }
        }
        ImportSummary(result.boards.size, homeLink, missing, result.warnings)
    }

    suspend fun exportAll(uri: Uri) = withContext(Dispatchers.IO) {
        val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("Could not create the file")
        out.use { ObfExporter.exportPackage(boards.boards.value, images.source, it) }
    }

    suspend fun exportBoard(boardId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val set = boards.boards.value
        val board = set.board(boardId) ?: throw IOException("That board no longer exists")
        val json = ObfExporter.exportBoard(board, images.source, set)
        val out = resolver.openOutputStream(uri, "wt") ?: throw IOException("Could not create the file")
        out.use { it.write(json.toByteArray()) }
    }

    /** Fetches a picture that an imported board only links to. Works only when online; failures are counted, not fatal. */
    private fun download(image: RemoteImage): Boolean = try {
        val connection = URL(image.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode !in 200..299) return false
            val bytes = connection.inputStream.use { input ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(32 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    total += n
                    if (total > MAX_REMOTE_IMAGE) return false
                    buffer.write(chunk, 0, n)
                }
                buffer.toByteArray()
            }
            images.file(image.fileName).writeBytes(bytes)
            true
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        Log.i("Saywell", "Picture not downloaded: ${image.url}", e)
        false
    }

    private companion object {
        const val MAX_REMOTE_IMAGE = 5 * 1024 * 1024
    }
}
