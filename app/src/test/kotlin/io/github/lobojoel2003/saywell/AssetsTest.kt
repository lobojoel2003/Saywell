package io.github.lobojoel2003.saywell

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.lobojoel2003.saywell.core.symbols.SymbolSearch
import io.github.lobojoel2003.saywell.core.vocabulary.DefaultVocabulary
import io.github.lobojoel2003.saywell.speech.VoiceCatalog
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The assets made at build time are present and match what the app expects. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssetsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val json = Json { ignoreUnknownKeys = true }

    private fun exists(path: String) = runCatching { context.assets.open(path).close() }.isSuccess

    @Test
    fun `every symbol in the starter vocabulary is bundled`() {
        val missing = DefaultVocabulary.symbolCodes().filterNot { exists("symbols/openmoji/$it.svg") }
        assertEquals(emptyList(), missing)
    }

    @Test
    fun `the symbol index loads and is searchable`() {
        val entries = SymbolSearch.parseIndex(context.assets.open("symbols/openmoji/index.json").use { it.readBytes().decodeToString() })
        assertTrue(entries.size > 2000, "only ${entries.size} symbols")
        assertTrue(SymbolSearch(entries).search("apple").isNotEmpty())
        assertTrue(exists("symbols/openmoji/LICENSE.txt"))
    }

    @Test
    fun `the voice catalog is valid and includes the bundled voice`() {
        val catalog = json
            .decodeFromString(VoiceCatalog.serializer(), context.assets.open("voices/catalog.json").use { it.readBytes().decodeToString() })
        assertTrue(catalog.baseUrl.startsWith("https://"))
        assertTrue(catalog.voices.any { it.id == BuildConfig.BUNDLED_VOICE })
        assertEquals(catalog.voices.size, catalog.voices.map { it.id }.toSet().size)
    }

    @Test
    fun `the prediction word list is there`() {
        val words = context.assets.open("prediction/en.txt").bufferedReader().readLines().filterNot { it.startsWith("#") }
        assertTrue(words.size > 300)
    }
}
