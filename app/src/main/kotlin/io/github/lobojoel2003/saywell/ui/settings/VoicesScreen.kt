package io.github.lobojoel2003.saywell.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalResources
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.data.VoiceEngine
import io.github.lobojoel2003.saywell.speech.NeuralVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Saywell's offline voices: choose one, download more, or install one from a file. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val app = context.container
    val store = app.voices
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val installed by store.installed.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val progress = remember { mutableStateMapOf<String, Float>() }
    var card by remember { mutableStateOf<NeuralVoice?>(null) }
    var deleting by remember { mutableStateOf<NeuralVoice?>(null) }

    val voices = listOfNotNull(store.bundled) + installed
    val selected = store.resolve(settings.neuralVoiceId)?.id
    val available = store.catalog.voices.filter { c -> voices.none { it.id == c.id } }

    fun choose(voice: NeuralVoice) = app.settings.update { it.copy(engine = VoiceEngine.NEURAL, neuralVoiceId = voice.id.takeUnless { voice.bundled }) }

    val installFromFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                progress["file"] = -1f
                try {
                    val voice = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { store.install(it) }
                    } ?: error(resources.getString(R.string.file_unreadable))
                    snackbar.showSnackbar(resources.getString(R.string.voice_installed, voice.name))
                } catch (e: Exception) {
                    snackbar.showSnackbar(e.message ?: resources.getString(R.string.voice_install_failed))
                } finally {
                    progress.remove("file")
                }
            }
        }
    }

    BackHandler(onBack = onBack)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saywell_voices)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.voices_intro), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)

            SectionHeader(stringResource(R.string.on_this_device))
            if (voices.isEmpty()) {
                Text(stringResource(R.string.no_voices), modifier = Modifier.padding(16.dp))
            }
            voices.forEach { voice ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = voice.id == selected, role = Role.RadioButton) { choose(voice) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = voice.id == selected, onClick = null)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(voice.name, style = MaterialTheme.typography.bodyLarge)
                        Text(describe(voice.language, voice.gender, voice.quality, voice.bundled, voice.license), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        choose(voice)
                        app.speaker.speak(resources.getString(R.string.test_voice_sentence))
                    }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.try_voice, voice.name)) }
                    if (voice.modelCard != null) {
                        IconButton(onClick = { card = voice }) { Icon(Icons.Filled.Description, contentDescription = stringResource(R.string.voice_licence, voice.name)) }
                    }
                    if (!voice.bundled) {
                        IconButton(onClick = { deleting = voice }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_voice, voice.name)) }
                    }
                }
            }

            SectionHeader(stringResource(R.string.download_voices))
            Text(stringResource(R.string.download_voices_summary), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)
            available.forEach { voice ->
                val p = progress[voice.id]
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(voice.name, style = MaterialTheme.typography.bodyLarge)
                            Text(describe(voice.language, voice.gender, voice.quality, false, voice.license), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            enabled = p == null,
                            onClick = {
                                progress[voice.id] = 0f
                                scope.launch {
                                    try {
                                        val done = store.download(voice) { bytes, total ->
                                            progress[voice.id] = if (total > 0) bytes.toFloat() / total else -1f
                                        }
                                        snackbar.showSnackbar(resources.getString(R.string.voice_installed, done.name))
                                    } catch (e: Exception) {
                                        snackbar.showSnackbar(e.message ?: resources.getString(R.string.voice_install_failed))
                                    } finally {
                                        progress.remove(voice.id)
                                    }
                                }
                            },
                        ) { Text(stringResource(R.string.download)) }
                    }
                    if (p != null) {
                        if (p < 0) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                        } else {
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.install_from_file))
            ActionRow(stringResource(R.string.install_from_file), stringResource(R.string.install_from_file_summary), enabled = progress["file"] == null) {
                installFromFile.launch(arrayOf("*/*"))
            }
            if (progress["file"] != null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp))
        }
    }

    card?.let { voice ->
        AlertDialog(
            onDismissRequest = { card = null },
            title = { Text(voice.name) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(voice.modelCard.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { card = null }) { Text(stringResource(R.string.close)) } },
        )
    }
    deleting?.let { voice ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_voice, voice.name)) },
            text = { Text(stringResource(R.string.delete_voice_body)) },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(voice.id)
                    if (settings.neuralVoiceId == voice.id) app.settings.update { it.copy(neuralVoiceId = null) }
                    deleting = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun describe(language: String, gender: String?, quality: String?, bundled: Boolean, license: String?): String {
    val parts = mutableListOf(language)
    when (gender) {
        "female" -> parts += stringResource(R.string.gender_female)
        "male" -> parts += stringResource(R.string.gender_male)
    }
    when (quality) {
        "low", "x_low" -> parts += stringResource(R.string.quality_fast)
        "medium" -> parts += stringResource(R.string.quality_medium)
        "high" -> parts += stringResource(R.string.quality_high)
    }
    if (bundled) parts += stringResource(R.string.built_in)
    if (license != null) parts += license
    return parts.joinToString(" · ")
}
