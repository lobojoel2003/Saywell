package io.github.lobojoel2003.saywell.speech

import io.github.lobojoel2003.saywell.data.Settings

/** Turns text into sound. [speak] suspends until the speech has finished and stops it if cancelled. */
interface SpeechEngine {
    suspend fun speak(text: String, settings: Settings)

    fun stop()

    fun shutdown()
}

/** The engine could not speak (no voice, missing files, engine not installed). The message is for the person. */
class SpeechUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)
