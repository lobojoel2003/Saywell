package io.github.lobojoel2003.saywell.speech

import android.content.Context
import android.util.Log
import io.github.lobojoel2003.saywell.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

@Serializable
data class CatalogVoice(
    val id: String,
    val name: String,
    val language: String,
    val locale: String,
    val gender: String? = null,
    val quality: String? = null,
    /** Licence of the voice's training data, from its model card, for example "CC BY 4.0". */
    val license: String? = null,
)

@Serializable
data class VoiceCatalog(val baseUrl: String, val voices: List<CatalogVoice>)

/** A neural voice that is ready to speak. */
data class NeuralVoice(
    val id: String,
    val name: String,
    val language: String,
    val gender: String?,
    val quality: String?,
    val license: String?,
    val bundled: Boolean,
    /** Model and tokens: asset paths for the bundled voice, absolute file paths otherwise. */
    val modelPath: String,
    val tokensPath: String,
    /** The upstream model card, which states the voice's licence. */
    val modelCard: String?,
)

class VoiceException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Saywell's neural voices: the one bundled in the APK and any downloaded or imported later.
 *
 * Voices are Piper models packaged for Sherpa-ONNX (`vits-piper-<voice>.tar.bz2`): an ONNX model,
 * `tokens.txt`, a `MODEL_CARD`, and `espeak-ng-data`, which turns text into phonemes. espeak-ng data is
 * shared by all voices in `files/espeak-ng-data`.
 */
class VoiceStore(private val context: Context) {
    private val voicesDir = File(context.filesDir, "voices")
    val espeakDir = File(context.filesDir, "espeak-ng-data")
    private val json = Json { ignoreUnknownKeys = true }

    val catalog: VoiceCatalog by lazy {
        try {
            json.decodeFromString(VoiceCatalog.serializer(), context.assets.open("voices/catalog.json").use { it.readBytes().decodeToString() })
        } catch (e: Exception) {
            Log.e(TAG, "Voice catalog missing", e)
            VoiceCatalog("", emptyList())
        }
    }

    val bundled: NeuralVoice? by lazy { findBundled() }

    private val installedState = MutableStateFlow(scanInstalled())
    val installed: StateFlow<List<NeuralVoice>> = installedState.asStateFlow()

    val all: List<NeuralVoice> get() = listOfNotNull(bundled) + installed.value

    /** The voice to use for [id]; null or unknown ids fall back to the bundled voice, then any installed one. */
    fun resolve(id: String?): NeuralVoice? = all.firstOrNull { it.id == id } ?: bundled ?: installed.value.firstOrNull()

    fun downloadUrl(voice: CatalogVoice) = catalog.baseUrl + "vits-piper-" + voice.id + ".tar.bz2"

    private fun findBundled(): NeuralVoice? {
        val id = BuildConfig.BUNDLED_VOICE
        val dir = "voices/bundled/$id"
        val files = runCatching { context.assets.list(dir)?.toSet() }.getOrNull().orEmpty()
        if ("model.onnx" !in files || "tokens.txt" !in files) return null
        val card = runCatching { context.assets.open("$dir/MODEL_CARD").use { it.readBytes().decodeToString() } }.getOrNull()
        return describe(id, bundled = true, modelPath = "$dir/model.onnx", tokensPath = "$dir/tokens.txt", card = card)
    }

    private fun scanInstalled(): List<NeuralVoice> =
        voicesDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.mapNotNull { dir ->
            val model = File(dir, "model.onnx")
            val tokens = File(dir, "tokens.txt")
            if (!model.isFile || !tokens.isFile) return@mapNotNull null
            describe(
                dir.name,
                bundled = false,
                modelPath = model.absolutePath,
                tokensPath = tokens.absolutePath,
                card = File(dir, "MODEL_CARD").takeIf { it.isFile }?.readText(),
            )
        }.orEmpty()

    private fun describe(id: String, bundled: Boolean, modelPath: String, tokensPath: String, card: String?): NeuralVoice {
        val entry = catalog.voices.firstOrNull { it.id == id }
        val parts = id.split('-')
        return NeuralVoice(
            id = id,
            name = entry?.name ?: parts.getOrNull(1)?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: id,
            language = entry?.language ?: parts.firstOrNull().orEmpty().replace('_', '-'),
            gender = entry?.gender,
            quality = entry?.quality ?: parts.getOrNull(2),
            license = entry?.license,
            bundled = bundled,
            modelPath = modelPath,
            tokensPath = tokensPath,
            modelCard = card,
        )
    }

    /**
     * Makes sure espeak-ng's data is on disk (Sherpa-ONNX reads it from a folder, not from the APK).
     * The bundled copy is unpacked once per app version.
     */
    suspend fun ensureEspeakData(): File? = withContext(Dispatchers.IO) {
        val marker = File(espeakDir, ".saywell-version")
        val hasBundledData = runCatching { context.assets.list("espeak-ng-data")?.isNotEmpty() }.getOrNull() == true
        if (hasBundledData && marker.takeIf { it.isFile }?.readText() != BuildConfig.VERSION_CODE.toString()) {
            copyAssetDir("espeak-ng-data", espeakDir)
            marker.writeText(BuildConfig.VERSION_CODE.toString())
        }
        espeakDir.takeIf { File(it, "phontab").isFile }
    }

    private fun copyAssetDir(assetPath: String, target: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input -> target.outputStream().use { input.copyTo(it) } }
            return
        }
        target.mkdirs()
        for (child in children) copyAssetDir("$assetPath/$child", File(target, child))
    }

    /** Installs a voice package (`.tar.bz2`, `.tar.gz` or `.zip`) from a file the person picked. */
    suspend fun install(input: InputStream): NeuralVoice = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "voice-install-${System.nanoTime()}")
        try {
            unpack(input, staging)
            installFrom(staging)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Downloads a voice from the catalog. [onProgress] gets bytes so far and the total (or -1). */
    suspend fun download(voice: CatalogVoice, onProgress: (Long, Long) -> Unit): NeuralVoice = withContext(Dispatchers.IO) {
        val archive = File(context.cacheDir, "voice-${voice.id}.download")
        try {
            val connection = URL(downloadUrl(voice)).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            try {
                if (connection.responseCode !in 200..299) throw VoiceException("The download failed (error ${connection.responseCode}).")
                val total = connection.contentLengthLong
                connection.inputStream.use { inputStream ->
                    archive.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = inputStream.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            done += n
                            onProgress(done, total)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            archive.inputStream().use { install(it) }
        } catch (e: IOException) {
            throw e as? VoiceException ?: VoiceException("The voice could not be downloaded. Check the internet connection and try again.", e)
        } finally {
            archive.delete()
        }
    }

    fun delete(id: String) {
        File(voicesDir, File(id).name).deleteRecursively()
        installedState.value = scanInstalled()
    }

    private fun installFrom(staging: File): NeuralVoice {
        val files = staging.walkTopDown().filter { it.isFile }.toList()
        val model = files.filter { it.name.endsWith(".onnx") }.maxByOrNull { it.length() }
            ?: throw VoiceException("This file doesn't contain a voice model (.onnx).")
        val tokens = files.firstOrNull { it.name == "tokens.txt" && it.parentFile == model.parentFile }
            ?: files.firstOrNull { it.name == "tokens.txt" }
            ?: throw VoiceException("This voice is missing tokens.txt. Use a voice packaged for Sherpa-ONNX (vits-piper-….tar.bz2).")
        val root = model.parentFile!!
        val id = root.name.takeIf { it != staging.name }?.removePrefix("vits-piper-") ?: model.nameWithoutExtension
        val safeId = id.replace(Regex("[^A-Za-z0-9_.-]"), "_")

        // Merge this voice's espeak-ng data (its language dictionary) into the shared folder.
        val espeak = files.firstOrNull { it.name == "phontab" }?.parentFile
        if (espeak != null) {
            espeak.walkTopDown().filter { it.isFile }.forEach { source ->
                val target = File(espeakDir, source.relativeTo(espeak).path)
                if (!target.exists()) source.copyTo(target)
            }
        } else if (!File(espeakDir, "phontab").isFile) {
            throw VoiceException("This voice package has no espeak-ng-data folder, which Saywell needs to read text.")
        }

        val target = File(voicesDir, safeId)
        target.deleteRecursively()
        target.mkdirs()
        model.copyTo(File(target, "model.onnx"))
        tokens.copyTo(File(target, "tokens.txt"))
        File(root, "MODEL_CARD").takeIf { it.isFile }?.copyTo(File(target, "MODEL_CARD"))
        installedState.value = scanInstalled()
        return installed.value.first { it.id == safeId }
    }

    /** Unpacks an archive, refusing entries that would land outside [target]. */
    private fun unpack(input: InputStream, target: File) {
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val magic = ByteArray(3)
        val read = buffered.read(magic)
        buffered.reset()
        val archive: ArchiveInputStream<*> = when {
            read >= 2 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte() -> ZipArchiveInputStream(buffered)
            read == 3 && magic[0] == 'B'.code.toByte() && magic[1] == 'Z'.code.toByte() && magic[2] == 'h'.code.toByte() ->
                TarArchiveInputStream(BZip2CompressorInputStream(buffered))
            read >= 2 && magic[0] == 0x1f.toByte() && magic[1] == 0x8b.toByte() -> TarArchiveInputStream(GzipCompressorInputStream(buffered))
            else -> throw VoiceException("This isn't a voice package. Use a .tar.bz2 or .zip file.")
        }
        val base = target.canonicalFile
        base.mkdirs()
        var total = 0L
        archive.use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                val out = File(base, entry.name).canonicalFile
                if (!out.path.startsWith(base.path + File.separator)) throw VoiceException("This voice package is damaged.")
                if (entry.isDirectory) {
                    out.mkdirs()
                    continue
                }
                out.parentFile?.mkdirs()
                out.outputStream().use { total += stream.copyTo(it) }
                if (total > MAX_UNPACKED) throw VoiceException("This voice package is too large.")
            }
        }
    }

    private companion object {
        const val TAG = "Saywell"
        const val MAX_UNPACKED = 600L * 1024 * 1024
    }
}
