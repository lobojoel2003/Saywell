package io.github.lobojoel2003.saywell.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.lobojoel2003.saywell.BuildConfig
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.container

private const val REPO = "https://github.com/lobojoel2003/Saywell"

/** Version, privacy, licences and credits. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val voice = LocalContext.current.container.voices.bundled
    BackHandler(onBack = onBack)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).background(colorResource(R.color.ic_launcher_background), CircleShape),
                )
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.version_line, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.build_line, BuildConfig.BUILD_NUMBER, BuildConfig.GIT_COMMIT), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(stringResource(R.string.about_text), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)

            SectionHeader(stringResource(R.string.privacy))
            Text(stringResource(R.string.privacy_text), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium)

            SectionHeader(stringResource(R.string.licences))
            Credit(stringResource(R.string.credit_saywell))
            Credit(stringResource(R.string.credit_openmoji))
            Credit(stringResource(R.string.credit_sherpa))
            Credit(stringResource(R.string.credit_espeak))
            if (voice != null) Credit(stringResource(R.string.credit_voice, voice.name, voice.language))
            Credit(stringResource(R.string.credit_libraries))

            SectionHeader(stringResource(R.string.help_and_feedback))
            ActionRow(stringResource(R.string.source_code), REPO) { runCatching { uri.openUri(REPO) } }
            ActionRow(stringResource(R.string.report_problem), stringResource(R.string.report_problem_summary)) {
                runCatching { uri.openUri("$REPO/issues/new/choose") }
            }
        }
    }
}

@Composable
private fun Credit(text: String) {
    Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
}
