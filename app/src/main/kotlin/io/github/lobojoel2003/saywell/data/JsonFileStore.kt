package io.github.lobojoel2003.saywell.data

import android.util.AtomicFile
import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** JSON shared by Saywell's own files: tolerant of fields added or removed between versions. */
internal val StorageJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

/**
 * Reads and writes one JSON file atomically, so a crash or a flat battery mid-save never leaves a
 * half-written file behind.
 */
internal class JsonFileStore<T>(val file: File, private val serializer: KSerializer<T>) {
    private val atomic = AtomicFile(file)

    /** The stored value, or null if there is none. A file that can't be read is kept aside as `.broken`. */
    fun read(): T? {
        if (!file.exists()) return null
        return try {
            StorageJson.decodeFromString(serializer, atomic.readFully().decodeToString())
        } catch (e: Exception) {
            Log.w(TAG, "Could not read ${file.name}; keeping a copy and starting fresh", e)
            runCatching { file.copyTo(File(file.parentFile, file.name + ".broken"), overwrite = true) }
            null
        }
    }

    @Synchronized
    fun write(value: T) {
        file.parentFile?.mkdirs()
        val out = atomic.startWrite()
        try {
            out.write(StorageJson.encodeToString(serializer, value).toByteArray())
            atomic.finishWrite(out)
        } catch (e: IOException) {
            atomic.failWrite(out)
            Log.e(TAG, "Could not save ${file.name}", e)
        }
    }

    private companion object {
        const val TAG = "Saywell"
    }
}
