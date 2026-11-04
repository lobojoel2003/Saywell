package io.github.lobojoel2003.saywell.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import io.github.lobojoel2003.saywell.data.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** A voice offered by the phone's text-to-speech engine. */
data class SystemVoice(val name: String, val language: String, val offline: Boolean, val quality: Int)

/** The phone's own text-to-speech engine. Works offline when the engine's voice data is installed. */
class SystemSpeechEngine(context: Context) : SpeechEngine {
    private val ready = CompletableDeferred<Boolean>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val tts = TextToSpeech(context.applicationContext) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    init {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) = Unit

                override fun onDone(utteranceId: String) {
                    pending.remove(utteranceId)?.complete(Unit)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    pending.remove(utteranceId)?.complete(Unit)
                }

                override fun onError(utteranceId: String, errorCode: Int) {
                    pending.remove(utteranceId)?.complete(Unit)
                }

                override fun onStop(utteranceId: String, interrupted: Boolean) {
                    pending.remove(utteranceId)?.complete(Unit)
                }
            },
        )
    }

    private suspend fun awaitReady(): Boolean = withTimeoutOrNull(5_000) { ready.await() } == true

    override suspend fun speak(text: String, settings: Settings) {
        if (!awaitReady()) {
            throw SpeechUnavailableException("This device has no text-to-speech engine. Install one, or use a Saywell voice.")
        }
        val voice = settings.systemVoiceName?.let { name -> runCatching { tts.voices?.firstOrNull { it.name == name } }.getOrNull() }
        if (voice != null && tts.voice?.name != voice.name) tts.voice = voice
        tts.setSpeechRate(settings.speechRate)
        tts.setPitch(settings.speechPitch)
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            throw SpeechUnavailableException("The phone's voice could not speak. Check Android's text-to-speech settings.")
        }
        try {
            done.await()
        } catch (e: CancellationException) {
            tts.stop()
            throw e
        }
    }

    override fun stop() {
        tts.stop()
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
    }

    override fun shutdown() {
        stop()
        tts.shutdown()
    }

    /** Installed voices, offline ones first, then by language. */
    suspend fun voices(): List<SystemVoice> {
        if (!awaitReady()) return emptyList()
        val all: Set<Voice> = runCatching { tts.voices }.getOrNull().orEmpty()
        return all.map {
            SystemVoice(
                name = it.name,
                language = it.locale.getDisplayName(Locale.getDefault()),
                offline = !it.isNetworkConnectionRequired && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),
                quality = it.quality,
            )
        }.sortedWith(compareByDescending<SystemVoice> { it.offline }.thenBy { it.language }.thenByDescending { it.quality }.thenBy { it.name })
    }

    suspend fun engineName(): String? = if (awaitReady()) runCatching { tts.defaultEngine }.getOrNull() else null
}
