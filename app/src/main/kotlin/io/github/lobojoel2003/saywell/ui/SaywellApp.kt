package io.github.lobojoel2003.saywell.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.ui.keyboard.KeyboardScreen
import io.github.lobojoel2003.saywell.ui.settings.AboutScreen
import io.github.lobojoel2003.saywell.ui.settings.SettingsScreen
import io.github.lobojoel2003.saywell.ui.settings.VoicesScreen
import io.github.lobojoel2003.saywell.ui.talker.TalkerScreen
import io.github.lobojoel2003.saywell.ui.talker.TalkerViewModel
import io.github.lobojoel2003.saywell.ui.theme.SaywellTheme

enum class Screen(val parent: Screen?) {
    TALKER(null),
    KEYBOARD(TALKER),
    SETTINGS(TALKER),
    VOICES(SETTINGS),
    ABOUT(SETTINGS),
}

@Composable
fun SaywellApp() {
    val app = LocalContext.current.container
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val talker: TalkerViewModel = viewModel { TalkerViewModel(app) }
    val board by talker.board.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.TALKER) }
    val back = { screen = screen.parent ?: Screen.TALKER }

    // An AAC device shouldn't lock in the middle of a conversation.
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    SaywellTheme(settings.theme, settings.highContrast) {
        Surface(color = MaterialTheme.colorScheme.background) {
            when (screen) {
                Screen.TALKER -> TalkerScreen(
                    viewModel = talker,
                    onOpenKeyboard = { screen = Screen.KEYBOARD },
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
                Screen.KEYBOARD -> KeyboardScreen(talker, onBack = back)
                Screen.SETTINGS -> SettingsScreen(
                    currentBoardId = board.id,
                    onBack = back,
                    onVoices = { screen = Screen.VOICES },
                    onAbout = { screen = Screen.ABOUT },
                )
                Screen.VOICES -> VoicesScreen(onBack = back)
                Screen.ABOUT -> AboutScreen(onBack = back)
            }
        }
    }
}
