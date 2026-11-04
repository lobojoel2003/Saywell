package io.github.lobojoel2003.saywell.ui.keyboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container
import io.github.lobojoel2003.saywell.core.prediction.WordPredictor
import io.github.lobojoel2003.saywell.ui.talker.TalkerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Type to talk, with word prediction, for people who can spell. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(viewModel: TalkerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val history by app.history.history.collectAsStateWithLifecycle()
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val speech by viewModel.speech.collectAsStateWithLifecycle()
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var baseWords by remember { mutableStateOf(emptyList<String>()) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        baseWords = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("prediction/en.txt").bufferedReader().readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
            }.getOrDefault(emptyList())
        }
        focus.requestFocus()
    }
    val predictor = remember(baseWords, boards, history.wordCounts) {
        val labels = boards.boards.flatMap { b -> b.buttons.map { it.label } }.filter { ' ' !in it && it.isNotBlank() }
        WordPredictor(baseWords + labels, history.wordCounts)
    }
    val suggestions = remember(predictor, text.text) { predictor.predict(text.text, limit = 8) }

    BackHandler(onBack = onBack)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.keyboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp),
                placeholder = { Text(stringResource(R.string.keyboard_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).focusRequester(focus),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { word ->
                    SuggestionChip(
                        onClick = {
                            val completed = WordPredictor.complete(text.text, word)
                            text = TextFieldValue(completed, selection = TextRange(completed.length))
                        },
                        label = { Text(word, fontSize = 20.sp) },
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (speech.speaking) viewModel.stopSpeaking() else viewModel.speakText(text.text) },
                    modifier = Modifier.weight(1f).height(64.dp),
                ) {
                    Icon(if (speech.speaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text(" " + stringResource(if (speech.speaking) R.string.stop_speaking else R.string.speak), fontSize = 22.sp)
                }
                OutlinedButton(
                    onClick = {
                        viewModel.addToMessage(text.text)
                        onBack()
                    },
                    enabled = text.text.isNotBlank(),
                    modifier = Modifier.height(64.dp),
                ) { Text(stringResource(R.string.add_to_message)) }
                OutlinedButton(onClick = { text = TextFieldValue("") }, modifier = Modifier.height(64.dp)) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_text))
                }
            }
            if (history.recent.isNotEmpty()) {
                Text(stringResource(R.string.recent), style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.weight(1f)) {
                    items(history.recent) { sentence ->
                        ListItem(
                            headlineContent = { Text(sentence, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.speak)) { viewModel.speakText(sentence) },
                        )
                    }
                }
            }
        }
    }
}
