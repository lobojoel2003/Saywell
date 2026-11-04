package io.github.lobojoel2003.saywell

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.lobojoel2003.saywell.speech.VoiceException
import io.github.lobojoel2003.saywell.speech.VoiceStore
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun tarBz2(files: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        TarArchiveOutputStream(BZip2CompressorOutputStream(out)).use { tar ->
            for ((name, text) in files) {
                val bytes = text.toByteArray()
                tar.putArchiveEntry(TarArchiveEntry(name).apply { size = bytes.size.toLong() })
                tar.write(bytes)
                tar.closeArchiveEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `installs a Sherpa-ONNX Piper voice package`() = runBlocking {
        val store = VoiceStore(context)
        val voice = store.install(
            ByteArrayInputStream(
                tarBz2(
                    mapOf(
                        "vits-piper-de_DE-test-medium/de_DE-test-medium.onnx" to "model",
                        "vits-piper-de_DE-test-medium/tokens.txt" to "a 1",
                        "vits-piper-de_DE-test-medium/MODEL_CARD" to "License: CC0",
                        "vits-piper-de_DE-test-medium/espeak-ng-data/phontab" to "p",
                        "vits-piper-de_DE-test-medium/espeak-ng-data/de_dict" to "d",
                    ),
                ),
            ),
        )
        assertEquals("de_DE-test-medium", voice.id)
        assertEquals("License: CC0", voice.modelCard)
        assertTrue(store.installed.value.any { it.id == voice.id })
        assertTrue(File(store.espeakDir, "de_dict").isFile)
        assertTrue(File(voice.modelPath).isFile)

        store.delete(voice.id)
        assertTrue(store.installed.value.none { it.id == voice.id })
    }

    @Test
    fun `rejects packages that try to write outside their folder`() {
        val store = VoiceStore(context)
        assertFailsWith<VoiceException> {
            runBlocking { store.install(ByteArrayInputStream(tarBz2(mapOf("../../evil.onnx" to "x")))) }
        }
    }

    @Test
    fun `rejects files that are not voice packages`() {
        val store = VoiceStore(context)
        assertFailsWith<VoiceException> { runBlocking { store.install(ByteArrayInputStream("hello".toByteArray())) } }
    }

    @Test
    fun `download links follow the Sherpa-ONNX naming`() {
        val store = VoiceStore(context)
        val voice = store.catalog.voices.first()
        assertEquals("${store.catalog.baseUrl}vits-piper-${voice.id}.tar.bz2", store.downloadUrl(voice))
    }
}
