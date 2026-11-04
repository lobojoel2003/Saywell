package io.github.lobojoel2003.saywell.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalResources
import io.github.lobojoel2003.saywell.AppContainer
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.core.scan.ScanPattern
import io.github.lobojoel2003.saywell.data.ImportMode
import io.github.lobojoel2003.saywell.data.LabelPosition
import io.github.lobojoel2003.saywell.data.ScanMethod
import io.github.lobojoel2003.saywell.data.Settings
import io.github.lobojoel2003.saywell.data.ThemeChoice
import io.github.lobojoel2003.saywell.data.VoiceEngine
import io.github.lobojoel2003.saywell.input.SwitchInput
import io.github.lobojoel2003.saywell.speech.SystemVoice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(currentBoardId: String, onBack: () -> Unit, onVoices: () -> Unit, onAbout: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val app = context.container
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun update(transform: (Settings) -> Settings) = app.settings.update(transform)
    fun notify(text: String) {
        scope.launch { snackbar.showSnackbar(text, duration = SnackbarDuration.Long) }
    }

    var systemVoices by remember { mutableStateOf(emptyList<SystemVoice>()) }
    LaunchedEffect(Unit) { systemVoices = app.speaker.system.voices() }

    var learning by remember { mutableStateOf<Boolean?>(null) } // true = select switch, false = next switch
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> importUri = uri }
    val exportAllLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { app.transfer.exportAll(uri) }
                    .onSuccess { notify(resources.getString(R.string.export_done)) }
                    .onFailure { notify(resources.getString(R.string.export_failed, it.message.orEmpty())) }
            }
        }
    }
    val exportBoardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { app.transfer.exportBoard(currentBoardId, uri) }
                    .onSuccess { notify(resources.getString(R.string.export_done)) }
                    .onFailure { notify(resources.getString(R.string.export_failed, it.message.orEmpty())) }
            }
        }
    }

    BackHandler(onBack = onBack)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            // ---- Voice ----
            SectionHeader(stringResource(R.string.section_voice))
            ChoiceRow(
                stringResource(R.string.voice_engine),
                listOf(VoiceEngine.NEURAL to stringResource(R.string.engine_neural), VoiceEngine.SYSTEM to stringResource(R.string.engine_system)),
                settings.engine,
            ) { choice -> update { it.copy(engine = choice) } }
            ActionRow(stringResource(R.string.saywell_voices), stringResource(R.string.saywell_voices_summary), onClick = onVoices)
            ChoiceRow(
                stringResource(R.string.phone_voice),
                listOf<Pair<String?, String>>(null to stringResource(R.string.phone_voice_default)) +
                    systemVoices.map { v -> v.name to (v.language + if (v.offline) "" else " · " + resources.getString(R.string.needs_internet)) + " · " + v.name },
                settings.systemVoiceName,
            ) { name -> update { it.copy(systemVoiceName = name) } }
            ActionRow(stringResource(R.string.android_tts_settings), stringResource(R.string.android_tts_settings_summary)) {
                try {
                    context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: ActivityNotFoundException) {
                    notify(resources.getString(R.string.no_tts_settings))
                }
            }
            SliderRow(stringResource(R.string.speech_rate), settings.speechRate, Settings.MIN_RATE..Settings.MAX_RATE, 14, { "%.1f×".format(it) }) { v ->
                update { it.copy(speechRate = v) }
            }
            SliderRow(
                stringResource(R.string.speech_pitch),
                settings.speechPitch,
                0.5f..2.0f,
                14,
                { "%.1f×".format(it) },
                enabled = settings.engine == VoiceEngine.SYSTEM,
            ) { v -> update { it.copy(speechPitch = v) } }
            ActionRow(stringResource(R.string.test_voice), stringResource(R.string.test_voice_sentence)) {
                app.speaker.speak(resources.getString(R.string.test_voice_sentence))
            }

            // ---- Talking ----
            SectionHeader(stringResource(R.string.section_talking))
            SwitchRow(stringResource(R.string.speak_each_button), stringResource(R.string.speak_each_button_summary), settings.speakEachButton) { v ->
                update { it.copy(speakEachButton = v) }
            }
            SwitchRow(stringResource(R.string.clear_after_speaking), null, settings.clearAfterSpeaking) { v -> update { it.copy(clearAfterSpeaking = v) } }
            SwitchRow(stringResource(R.string.speak_folder_names), null, settings.speakFolderNames) { v -> update { it.copy(speakFolderNames = v) } }
            SwitchRow(stringResource(R.string.return_home), stringResource(R.string.return_home_summary), settings.returnHomeAfterWord) { v ->
                update { it.copy(returnHomeAfterWord = v) }
            }

            // ---- Display ----
            SectionHeader(stringResource(R.string.section_display))
            ChoiceRow(
                stringResource(R.string.labels),
                listOf(
                    LabelPosition.BELOW to stringResource(R.string.labels_below),
                    LabelPosition.ABOVE to stringResource(R.string.labels_above),
                    LabelPosition.HIDDEN to stringResource(R.string.labels_hidden),
                ),
                settings.labelPosition,
            ) { v -> update { it.copy(labelPosition = v) } }
            SliderRow(stringResource(R.string.text_size), settings.textScale, 0.8f..1.6f, 7, { "%d%%".format((it * 100).toInt()) }) { v ->
                update { it.copy(textScale = v) }
            }
            SwitchRow(stringResource(R.string.symbols_in_message), null, settings.showSymbolsInMessage) { v -> update { it.copy(showSymbolsInMessage = v) } }
            ChoiceRow(
                stringResource(R.string.theme),
                listOf(
                    ThemeChoice.SYSTEM to stringResource(R.string.theme_system),
                    ThemeChoice.LIGHT to stringResource(R.string.theme_light),
                    ThemeChoice.DARK to stringResource(R.string.theme_dark),
                ),
                settings.theme,
            ) { v -> update { it.copy(theme = v) } }
            SwitchRow(stringResource(R.string.high_contrast), stringResource(R.string.high_contrast_summary), settings.highContrast) { v ->
                update { it.copy(highContrast = v) }
            }
            SwitchRow(stringResource(R.string.keep_screen_on), stringResource(R.string.keep_screen_on_summary), settings.keepScreenOn) { v ->
                update { it.copy(keepScreenOn = v) }
            }

            // ---- Touch ----
            SectionHeader(stringResource(R.string.section_touch))
            SliderRow(stringResource(R.string.hold_to_select), settings.holdToSelectMs / 1000f, 0f..2f, 19, { seconds(resources, it) }) { v ->
                update { it.copy(holdToSelectMs = (v * 1000).toInt()) }
            }
            SliderRow(stringResource(R.string.ignore_repeats), settings.ignoreRepeatsMs / 1000f, 0f..2f, 19, { seconds(resources, it) }) { v ->
                update { it.copy(ignoreRepeatsMs = (v * 1000).toInt()) }
            }

            // ---- Switch scanning ----
            SectionHeader(stringResource(R.string.section_switches))
            SwitchRow(stringResource(R.string.use_scanning), stringResource(R.string.use_scanning_summary), settings.scanning) { v -> update { it.copy(scanning = v) } }
            ChoiceRow(
                stringResource(R.string.scan_method),
                listOf(ScanMethod.AUTO to stringResource(R.string.scan_auto), ScanMethod.STEP to stringResource(R.string.scan_step)),
                settings.scanMethod,
                enabled = settings.scanning,
            ) { v -> update { it.copy(scanMethod = v) } }
            ChoiceRow(
                stringResource(R.string.scan_pattern),
                listOf(ScanPattern.ROW_COLUMN to stringResource(R.string.scan_row_column), ScanPattern.LINEAR to stringResource(R.string.scan_linear)),
                settings.scanPattern,
                enabled = settings.scanning,
            ) { v -> update { it.copy(scanPattern = v) } }
            SliderRow(
                stringResource(R.string.scan_speed),
                settings.scanIntervalMs / 1000f,
                0.5f..5f,
                44,
                { seconds(resources, it) },
                enabled = settings.scanning && settings.scanMethod == ScanMethod.AUTO,
            ) { v -> update { it.copy(scanIntervalMs = (v * 1000).toInt()) } }
            SliderRow(
                stringResource(R.string.scan_rounds),
                settings.scanRounds.toFloat(),
                1f..10f,
                8,
                { resources.getQuantityString(R.plurals.rounds, it.toInt(), it.toInt()) },
                enabled = settings.scanning && settings.scanMethod == ScanMethod.AUTO,
            ) { v -> update { it.copy(scanRounds = v.toInt()) } }
            SwitchRow(stringResource(R.string.scan_speak), stringResource(R.string.scan_speak_summary), settings.scanSpeakItems, settings.scanning) { v ->
                update { it.copy(scanSpeakItems = v) }
            }
            SwitchRow(stringResource(R.string.tap_as_switch), stringResource(R.string.tap_as_switch_summary), settings.tapScreenAsSwitch, settings.scanning) { v ->
                update { it.copy(tapScreenAsSwitch = v) }
            }
            ActionRow(
                stringResource(R.string.select_switch),
                stringResource(R.string.keys_are, settings.selectKeys.joinToString { SwitchInput.keyName(it) }),
                settings.scanning,
            ) { learning = true }
            ActionRow(
                stringResource(R.string.next_switch),
                stringResource(R.string.keys_are, settings.nextKeys.joinToString { SwitchInput.keyName(it) }),
                settings.scanning,
            ) { learning = false }
            ActionRow(stringResource(R.string.reset_switch_keys), null, settings.scanning) {
                update { it.copy(selectKeys = Settings.DEFAULT_SELECT_KEYS, nextKeys = Settings.DEFAULT_NEXT_KEYS) }
            }

            // ---- Boards ----
            SectionHeader(stringResource(R.string.section_boards))
            ActionRow(stringResource(R.string.import_boards), stringResource(R.string.import_boards_summary)) {
                importLauncher.launch(arrayOf("*/*"))
            }
            ActionRow(stringResource(R.string.export_all), stringResource(R.string.export_all_summary)) {
                exportAllLauncher.launch("saywell-boards-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.obz")
            }
            ActionRow(stringResource(R.string.export_board), stringResource(R.string.export_board_summary)) {
                val name = app.boards.boards.value.board(currentBoardId)?.name ?: "board"
                exportBoardLauncher.launch("${name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "board" }}.obf")
            }
            ActionRow(stringResource(R.string.reset_boards), stringResource(R.string.reset_boards_summary)) { confirmReset = true }

            // ---- Safety and about ----
            SectionHeader(stringResource(R.string.section_safety))
            SwitchRow(stringResource(R.string.lock_editing), stringResource(R.string.lock_editing_summary), settings.lockEditing) { v -> update { it.copy(lockEditing = v) } }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            ActionRow(stringResource(R.string.about), stringResource(R.string.about_summary), onClick = onAbout)
        }
    }

    learning?.let { selectSwitch ->
        LearnSwitchDialog(
            selectSwitch = selectSwitch,
            onLearned = { code ->
                update {
                    if (selectSwitch) {
                        it.copy(selectKeys = listOf(code), nextKeys = it.nextKeys - code)
                    } else {
                        it.copy(nextKeys = listOf(code), selectKeys = it.selectKeys - code)
                    }
                }
                learning = null
            },
            onDismiss = { learning = null },
        )
    }
    importUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text(stringResource(R.string.import_title)) },
            text = { Text(stringResource(R.string.import_question)) },
            confirmButton = {
                TextButton(onClick = {
                    importUri = null
                    scope.launch { runImport(app, resources, uri, ImportMode.ADD, ::notify) }
                }) { Text(stringResource(R.string.import_add)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    importUri = null
                    scope.launch { runImport(app, resources, uri, ImportMode.REPLACE, ::notify) }
                }) { Text(stringResource(R.string.import_replace)) }
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_boards)) },
            text = { Text(stringResource(R.string.reset_boards_question)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    app.boards.resetToStarterVocabulary()
                    notify(resources.getString(R.string.reset_done))
                }) { Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private suspend fun runImport(app: AppContainer, resources: Resources, uri: Uri, mode: ImportMode, notify: (String) -> Unit) {
    try {
        val summary = app.transfer.import(uri, mode)
        val parts = mutableListOf(resources.getQuantityString(R.plurals.imported_boards, summary.boardCount, summary.boardCount))
        if (mode == ImportMode.ADD) {
            parts += resources.getString(if (summary.homeLinkAdded) R.string.import_linked_home else R.string.import_find_in_list)
        }
        if (summary.picturesMissing > 0) {
            parts += resources.getQuantityString(R.plurals.pictures_missing, summary.picturesMissing, summary.picturesMissing)
        }
        parts += summary.warnings
        notify(parts.joinToString(" "))
    } catch (e: Exception) {
        notify(resources.getString(R.string.import_failed, e.message.orEmpty()))
    }
}

private fun seconds(resources: Resources, value: Float): String =
    if (value < 0.05f) resources.getString(R.string.off) else resources.getString(R.string.seconds, value)

/** Waits for a switch press and reports which key it sends. */
@Composable
private fun LearnSwitchDialog(selectSwitch: Boolean, onLearned: (Int) -> Unit, onDismiss: () -> Unit) {
    val switches = LocalContext.current.container.switches
    var learned by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) {
        switches.capture = { learned = it }
        onDispose { switches.capture = null }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (selectSwitch) R.string.select_switch else R.string.next_switch)) },
        text = {
            Text(
                learned?.let { stringResource(R.string.switch_learned, SwitchInput.keyName(it)) }
                    ?: stringResource(R.string.press_switch_now),
            )
        },
        confirmButton = {
            TextButton(onClick = { learned?.let(onLearned) }, enabled = learned != null) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
