package io.github.lobojoel2003.saywell.ui.talker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.core.message.Message
import io.github.lobojoel2003.saywell.data.Settings
import io.github.lobojoel2003.saywell.ui.components.SymbolImage
import io.github.lobojoel2003.saywell.ui.components.aacPressable
import io.github.lobojoel2003.saywell.ui.components.rememberPressState
import io.github.lobojoel2003.saywell.ui.theme.LocalSaywellColors

/**
 * The sentence being built. Tapping it (or the speak button) says it out loud.
 */
@Composable
fun MessageBar(
    message: Message,
    settings: Settings,
    speaking: Boolean,
    highlighted: Set<String>,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = (84 * settings.textScale.coerceIn(0.8f, 1.6f)).dp
    val controlsHighlighted = highlighted.size > 1 && ScanKeys.SPEAK in highlighted
    val extra = LocalSaywellColors.current
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val press = rememberPressState()
        val speakLabel = stringResource(R.string.speak_message)
        val listState = rememberLazyListState()
        LaunchedEffect(message.items.size) {
            if (message.items.isNotEmpty()) listState.animateScrollToItem(message.items.size - 1)
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(if (controlsHighlighted) 6.dp else 2.dp, if (controlsHighlighted) extra.scanHighlight else MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .aacPressable(press, speakLabel, 0, onActivate = onSpeak),
        ) {
            if (message.isEmpty) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(R.string.message_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                }
            } else {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    itemsIndexed(message.items) { _, item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (settings.showSymbolsInMessage && item.image != null) {
                                SymbolImage(item.image!!, Modifier.size(height * 0.45f))
                            }
                            Text(item.text, fontSize = (20 * settings.textScale).sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
            }
        }
        ControlButton(
            icon = if (speaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
            label = stringResource(if (speaking) R.string.stop_speaking else R.string.speak),
            highlighted = ScanKeys.SPEAK in highlighted && !controlsHighlighted,
            primary = true,
            onClick = if (speaking) onStop else onSpeak,
            modifier = Modifier.fillMaxHeight().width(height * 1.1f),
        )
        ControlButton(
            icon = Icons.AutoMirrored.Filled.Backspace,
            label = stringResource(R.string.delete_word),
            highlighted = ScanKeys.BACKSPACE in highlighted && !controlsHighlighted,
            onClick = onBackspace,
            modifier = Modifier.fillMaxHeight().width(height * 0.85f),
        )
        ControlButton(
            icon = Icons.Filled.Clear,
            label = stringResource(R.string.clear_message),
            highlighted = ScanKeys.CLEAR in highlighted && !controlsHighlighted,
            onClick = onClear,
            modifier = Modifier.fillMaxHeight().width(height * 0.85f),
        )
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val press = rememberPressState()
    val extra = LocalSaywellColors.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = when {
            primary -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        border = if (highlighted) BorderStroke(6.dp, extra.scanHighlight) else null,
        modifier = modifier.aacPressable(press, label, 0, iconOnly = true, onActivate = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            if (press.pressed) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.15f)))
        }
    }
}
