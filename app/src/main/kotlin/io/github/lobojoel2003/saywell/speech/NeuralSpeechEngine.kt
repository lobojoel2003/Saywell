package io.github.lobojoel2003.saywell.speech

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.github.lobojoel2003.saywell.data.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext

/**
 * Offline neural speech: Piper voices run by Sherpa-ONNX on the device. No network is used.
 *
 * All native calls happen on one background thread. The model stays loaded between sentences, and
 * short phrases (single words, quick replies) are cached so tapping a button speaks instantly.
 */
class NeuralSpeechEngine(private val context: Context, private val voices: VoiceStore) : SpeechEngine {
    private val dispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "saywell-tts").apply { priority = Thread.MAX_PRIORITY } }
        .asCoroutineDispatcher()

    private var loadedVoiceId: String? = null
    private var tts: OfflineTts? = null

    @Volatile
    private var player: AudioPlayer? = null

    private val cache = object : LinkedHashMap<String, Pair<FloatArray, Int>>(64, 0.75f, true) {
        private var samples = 0L

        override fun put(key: String, value: Pair<FloatArray, Int>): Pair<FloatArray, Int>? {
            samples += value.first.size
            return super.put(key, value)
        }

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<FloatArray, Int>>): Boolean {
            if (samples <= MAX_CACHED_SAMPLES) return false
            samples -= eldest.value.first.size
            return true
        }
    }

    /** Loads the voice in the background so the first word is quick. */
    suspend fun prepare(settings: Settings) = withContext(dispatcher) { load(settings) }

    override suspend fun speak(text: String, settings: Settings) = withContext(dispatcher) {
        val job = coroutineContext.job
        val engine = load(settings)
        val voiceId = loadedVoiceId
        val speakerId = settings.neuralSpeakerId.coerceIn(0, (engine.numSpeakers() - 1).coerceAtLeast(0))
        val speed = settings.speechRate.coerceIn(Settings.MIN_RATE, Settings.MAX_RATE)
        val key = "$voiceId|$speakerId|$speed|$text"
        val cached = synchronized(cache) { cache[key] }
        val audio = AudioPlayer(cached?.second ?: engine.sampleRate())
        player = audio
        try {
            if (cached != null) {
                audio.write(cached.first)
            } else {
                val keep = text.length <= MAX_CACHED_TEXT
                val collected = if (keep) ArrayList<FloatArray>() else null
                val result = engine.generateWithCallback(text = text, sid = speakerId, speed = speed) { samples ->
                    if (!job.isActive) {
                        0
                    } else {
                        collected?.add(samples.copyOf())
                        if (audio.write(samples)) 1 else 0
                    }
                }
                if (collected != null && job.isActive) {
                    val all = FloatArray(collected.sumOf { it.size })
                    var offset = 0
                    collected.forEach { it.copyInto(all, offset); offset += it.size }
                    if (all.isNotEmpty()) synchronized(cache) { cache[key] = all to result.sampleRate }
                }
            }
            if (job.isActive) audio.drain()
        } finally {
            audio.release()
            player = null
        }
        if (!job.isActive) throw CancellationException()
    }

    override fun stop() {
        player?.stop()
    }

    override fun shutdown() {
        stop()
        tts?.release()
        tts = null
        dispatcher.close()
    }

    /** Loads (or reuses) the model for the selected voice. Runs on [dispatcher]. */
    private suspend fun load(settings: Settings): OfflineTts {
        val voice = voices.resolve(settings.neuralVoiceId)
            ?: throw SpeechUnavailableException("No Saywell voice is installed. Download one in Settings › Voices.")
        tts?.let { if (loadedVoiceId == voice.id) return it }
        tts?.release()
        tts = null
        loadedVoiceId = null
        synchronized(cache) { cache.clear() }

        val espeak = voices.ensureEspeakData()
            ?: throw SpeechUnavailableException("The voice's pronunciation data is missing. Reinstall the voice.")
        // Sherpa-ONNX ends the process on a bad configuration, so check every file first.
        val present = if (voice.bundled) {
            assetExists(voice.modelPath) && assetExists(voice.tokensPath)
        } else {
            File(voice.modelPath).isFile && File(voice.tokensPath).isFile
        }
        if (!present) throw SpeechUnavailableException("The voice ${voice.name} is incomplete. Reinstall it.")

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(model = voice.modelPath, tokens = voice.tokensPath, dataDir = espeak.absolutePath),
                numThreads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4),
                debug = false,
                provider = "cpu",
            ),
        )
        val engine = try {
            val started = System.currentTimeMillis()
            OfflineTts(assetManager = if (voice.bundled) context.assets else null, config = config).also {
                Log.i(TAG, "Loaded voice ${voice.id} in ${System.currentTimeMillis() - started} ms")
            }
        } catch (e: Throwable) {
            // UnsatisfiedLinkError when the native library isn't available for this processor.
            throw SpeechUnavailableException("Saywell's voices can't run on this device.", e)
        }
        tts = engine
        loadedVoiceId = voice.id
        return engine
    }

    private fun assetExists(path: String) = runCatching { context.assets.openFd(path).close() }.isSuccess ||
        runCatching { context.assets.open(path).close() }.isSuccess

    private companion object {
        const val TAG = "Saywell"
        const val MAX_CACHED_TEXT = 40
        const val MAX_CACHED_SAMPLES = 6_000_000L
    }
}
