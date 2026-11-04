package io.github.lobojoel2003.saywell.data

import android.content.ContentResolver
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import io.github.lobojoel2003.saywell.core.model.Ids
import io.github.lobojoel2003.saywell.core.model.ImageRef
import io.github.lobojoel2003.saywell.core.obf.ImageData
import io.github.lobojoel2003.saywell.core.obf.ImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Pictures on buttons: bundled symbols (read from the APK) and photos or imported pictures (kept in
 * the app's private `images` folder).
 */
class ImageStore(private val directory: File, private val assets: AssetManager) {
    init {
        directory.mkdirs()
    }

    fun file(name: String): File = File(directory, File(name).name)

    /** What Coil loads for a picture. */
    fun model(ref: ImageRef): Any = when (ref) {
        is ImageRef.Symbol -> "file:///android_asset/${symbolAssetPath(ref)}"
        is ImageRef.File -> file(ref.name)
    }

    /** Saves picture bytes (from an imported board) under [name]. */
    suspend fun saveBytes(name: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        file(name).writeBytes(bytes)
    }

    /** Copies a photo chosen from the gallery, scaled down and turned the right way up. */
    suspend fun importPhoto(resolver: ContentResolver, uri: Uri): ImageRef.File = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Could not open the picture")
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIZE && bounds.outHeight / (sample * 2) >= MAX_SIZE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("This file is not a picture Saywell can read")
        val rotation = resolver.openInputStream(uri)?.use { stream ->
            runCatching {
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }.getOrDefault(0f)
        } ?: 0f
        saveBitmapBlocking(decoded, rotation)
    }

    /** Saves a photo taken with the camera. */
    suspend fun saveBitmap(bitmap: Bitmap): ImageRef.File = withContext(Dispatchers.IO) { saveBitmapBlocking(bitmap, 0f) }

    private fun saveBitmapBlocking(source: Bitmap, rotation: Float): ImageRef.File {
        val scale = minOf(1f, MAX_SIZE.toFloat() / maxOf(source.width, source.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val png = bitmap.hasAlpha()
        val name = "${Ids.new()}.${if (png) "png" else "jpg"}"
        file(name).outputStream().use { out ->
            bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 88, out)
        }
        return ImageRef.File(name)
    }

    /** Deletes pictures no button uses any more. */
    suspend fun deleteUnused(inUse: Set<String>) = withContext(Dispatchers.IO) {
        directory.listFiles()?.filter { it.isFile && it.name !in inUse }?.forEach { it.delete() }
    }

    /** Reads pictures for exporting boards. */
    val source = ImageSource { ref ->
        when (ref) {
            is ImageRef.Symbol -> runCatching {
                ImageData(assets.open(symbolAssetPath(ref)).use { it.readBytes() }, "image/svg+xml")
            }.getOrNull()
            is ImageRef.File -> file(ref.name).takeIf { it.isFile }?.let { ImageData(it.readBytes(), contentType(it.name)) }
        }
    }

    private fun symbolAssetPath(ref: ImageRef.Symbol) = "symbols/${ref.set}/${ref.code}.svg"

    private fun contentType(name: String) = when (name.substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        else -> "application/octet-stream"
    }

    private companion object {
        /** Big enough for a full-screen button on a tablet, small enough to keep boards light. */
        const val MAX_SIZE = 512
    }
}
