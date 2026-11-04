package io.github.lobojoel2003.saywell.speech

import android.util.Log
import io.github.lobojoel2003.saywell.data.Settings
import io.github.lobojoel2003.saywell.data.VoiceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SpeechStatus(
    val speaking: Boolean = false,
    /** Set when the chosen voice couldn't be used and the phone's voice spoke instead. */
    val problem: String? = null,
)

/**
 * Speaks for the person. Uses the voice chosen in settings and falls back to the other engine if it
 * can't speak, so a press of "speak" always makes a sound when anything is able to.
 */
class Speaker(
    private val settings: StateFlow<Settings>,
    private val neural: NeuralSpeechEngine,
    val system: SystemSpeechEngine,
    private val scope: CoroutineScope,
) {
    private val state = MutableStateFlow(SpeechStatus())
    val status: StateFlow<SpeechStatus> = state.asStateFlow()

    private var current: Job? = null

    /** Why the neural voice can't be used on this device, once known; reset when another voice is chosen. */
    @Volatile
    private var neuralProblem: String? = null

    /** The last problem shown, so the same warning isn't repeated after every word. */
    private var lastReported: String? = null

    init {
        // Load the neural voice ahead of time, and again whenever a different voice is chosen.
        scope.launch {
            settings.map { it.engine to it.neuralVoiceId }.distinctUntilChanged().collect { (engine, _) ->
                neuralProblem = null
                lastReported = null
                if (engine == VoiceEngine.NEURAL) {
                    try {
                        neural.prepare(settings.value)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Voice not loaded", e)
                        if (e is SpeechUnavailableException) neuralProblem = e.message
                    }
                }
            }
        }
    }

    /** Speaks [text], interrupting anything already being said. */
    fun speak(text: String) {
        val words = text.trim()
        if (words.isEmpty()) return
        val previous = current
        previous?.cancel()
        neural.stop()
        system.stop()
        current = scope.launch {
            previous?.join()
            state.value = state.value.copy(speaking = true)
            try {
                speakWithFallback(words, settings.value)
            } finally {
                if (current == coroutineContext[Job]) state.value = state.value.copy(speaking = false)
            }
        }
    }

    fun stop() {
        current?.cancel()
        neural.stop()
        system.stop()
        state.value = state.value.copy(speaking = false)
    }

    fun dismissProblem() {
        state.value = state.value.copy(problem = null)
    }

    private suspend fun speakWithFallback(text: String, settings: Settings) {
        // Once the neural voice is known not to work here, go straight to the phone's voice.
        val neuralUsable = neuralProblem == null
        val engines = when (settings.engine) {
            VoiceEngine.NEURAL -> if (neuralUsable) listOf(neural, system) else listOf(system)
            VoiceEngine.SYSTEM -> if (neuralUsable) listOf(system, neural) else listOf(system)
        }
        neuralProblem?.let { if (settings.engine == VoiceEngine.NEURAL) report(it) }
        for ((index, engine) in engines.withIndex()) {
            try {
                engine.speak(text, settings)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Voice failed" + if (index < engines.lastIndex) ", trying the other one" else "", e)
                val message = (e as? SpeechUnavailableException)?.message ?: "The chosen voice couldn't speak."
                if (engine === neural && e is SpeechUnavailableException) neuralProblem = message
                if (index == 0) report(message)
            }
        }
    }

    /** Shows a problem once; the same message isn't shown again until the voice settings change. */
    private fun report(message: String) {
        if (message == lastReported) return
        lastReported = message
        state.value = state.value.copy(problem = message)
    }

    fun shutdown() {
        stop()
        neural.shutdown()
        system.shutdown()
    }

    private companion object {
        const val TAG = "Saywell"
    }
}
