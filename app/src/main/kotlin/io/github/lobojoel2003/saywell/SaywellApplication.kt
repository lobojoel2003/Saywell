package io.github.lobojoel2003.saywell

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import io.github.lobojoel2003.saywell.core.model.imageFiles
import io.github.lobojoel2003.saywell.data.BoardRepository
import io.github.lobojoel2003.saywell.data.BoardTransfer
import io.github.lobojoel2003.saywell.data.HistoryRepository
import io.github.lobojoel2003.saywell.data.ImageStore
import io.github.lobojoel2003.saywell.data.SettingsRepository
import io.github.lobojoel2003.saywell.data.SymbolLibrary
import io.github.lobojoel2003.saywell.input.SwitchInput
import io.github.lobojoel2003.saywell.speech.NeuralSpeechEngine
import io.github.lobojoel2003.saywell.speech.Speaker
import io.github.lobojoel2003.saywell.speech.SystemSpeechEngine
import io.github.lobojoel2003.saywell.speech.VoiceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** Creates Saywell's long-lived parts once and shares them. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataDir = File(app.filesDir, "data")

    val settings = SettingsRepository(dataDir, scope)
    val boards = BoardRepository(dataDir, scope)
    val history = HistoryRepository(dataDir, scope)
    val images = ImageStore(File(app.filesDir, "images"), app.assets)
    val symbols = SymbolLibrary(app.assets)
    val transfer = BoardTransfer(app.contentResolver, boards, images)
    val voices = VoiceStore(app)
    val speaker = Speaker(settings.settings, NeuralSpeechEngine(app, voices), SystemSpeechEngine(app), scope)
    val switches = SwitchInput()

    init {
        // Pictures left over from boards deleted in an earlier session. Skipped if the boards file
        // couldn't be read, so photos are never lost to a damaged file.
        if (boards.restored) scope.launch { images.deleteUnused(boards.boards.value.imageFiles()) }
    }
}

class SaywellApplication : Application(), SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
}

val Context.container: AppContainer get() = (applicationContext as SaywellApplication).container
