package io.github.lobojoel2003.saywell.speech

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** Streams synthesized speech (mono float samples) to the speaker as it is generated. */
internal class AudioPlayer(private val sampleRate: Int) {
    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setBufferSizeInBytes(
            maxOf(
                AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT),
                sampleRate * 4 / 4, // a quarter of a second
            ),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    @Volatile
    private var stopped = false
    private var written = 0L

    init {
        track.play()
    }

    /** Queues samples; blocks while the buffer is full. Returns false once stopped. */
    fun write(samples: FloatArray): Boolean {
        if (stopped) return false
        var offset = 0
        while (offset < samples.size && !stopped) {
            val n = track.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
            if (n <= 0) return false
            offset += n
        }
        written += offset
        return !stopped
    }

    /** Waits until everything written has been heard. */
    fun drain() {
        val deadline = System.currentTimeMillis() + written * 1000 / sampleRate + 1000
        while (!stopped && track.playbackHeadPosition.toLong() and 0xFFFFFFFFL < written && System.currentTimeMillis() < deadline) {
            Thread.sleep(15)
        }
    }

    /** Stops immediately. Safe to call from any thread. */
    fun stop() {
        stopped = true
        runCatching {
            track.pause()
            track.flush()
        }
    }

    fun release() {
        stopped = true
        runCatching { track.stop() }
        track.release()
    }
}
