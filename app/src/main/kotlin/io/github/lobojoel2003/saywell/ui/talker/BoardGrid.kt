package io.github.lobojoel2003.saywell.ui.talker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lobojoel2003.saywell.R
import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.Colors
import io.github.lobojoel2003.saywell.data.LabelPosition
import io.github.lobojoel2003.saywell.data.Settings
import io.github.lobojoel2003.saywell.ui.components.SymbolImage
import io.github.lobojoel2003.saywell.ui.components.aacPressable
import io.github.lobojoel2003.saywell.ui.components.rememberPressState
import io.github.lobojoel2003.saywell.ui.theme.LocalSaywellColors

/**
 * The board: a fixed grid that always fits the screen, so every word stays in the same place
 * (people find words by position as much as by picture).
 */
@Composable
fun BoardGrid(
    board: Board,
    settings: Settings,
    editing: Boolean,
    moving: Int?,
    highlighted: Set<String>,
    onPress: (index: Int, button: Button?) -> Unit,
    onLongPress: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowHighlight = highlighted.size > 1
    Column(modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until board.rows) {
            val rowKeys = (0 until board.columns).map { ScanKeys.cell(r * board.columns + it) }
            val wholeRow = rowHighlight && rowKeys.any { it in highlighted }
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (wholeRow) Modifier.border(6.dp, LocalSaywellColors.current.scanHighlight, RoundedCornerShape(14.dp)).padding(6.dp) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (c in 0 until board.columns) {
                    val index = r * board.columns + c
                    BoardCell(
                        button = board.cells[index],
                        settings = settings,
                        editing = editing,
                        pickedUp = moving == index,
                        highlighted = !rowHighlight && ScanKeys.cell(index) in highlighted,
                        onPress = { onPress(index, board.cells[index]) },
                        onLongPress = if (editing) ({ onLongPress(index) }) else null,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    button: Button?,
    settings: Settings,
    editing: Boolean,
    pickedUp: Boolean,
    highlighted: Boolean,
    onPress: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier,
) {
    val extra = LocalSaywellColors.current
    val shape = RoundedCornerShape(12.dp)
    val visible = button != null && (!button.hidden || editing)
    if (!visible && !editing) {
        // Empty cells keep their space but say nothing to screen readers.
        Box(modifier.clip(shape).background(extra.emptyCell).clearAndSetSemantics { })
        return
    }
    val press = rememberPressState()
    val label = button?.label?.ifBlank { null } ?: stringResource(if (button == null) R.string.cell_add else R.string.cell_unnamed)
    val background = button?.backgroundColor?.let(Colors::toArgb)?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (button?.backgroundColor == null) MaterialTheme.colorScheme.onSurfaceVariant else if (Colors.prefersDarkText(button.backgroundColor)) Color.Black else Color.White
    val border = when {
        highlighted -> BorderStroke(6.dp, extra.scanHighlight)
        pickedUp -> BorderStroke(5.dp, MaterialTheme.colorScheme.primary)
        button?.borderColor != null -> BorderStroke(3.dp, Color(Colors.toArgb(button.borderColor)!!))
        else -> BorderStroke(if (extra.highContrast) 2.dp else 1.dp, extra.cellBorder)
    }
    val holdMillis = if (editing) 0 else settings.holdToSelectMs
    Box(
        modifier
            .clip(shape)
            .background(if (button == null) extra.emptyCell else background)
            .border(border, shape)
            .alpha(if (button?.hidden == true) 0.5f else 1f)
            .aacPressable(press, label, holdMillis, onLongPress = onLongPress, onActivate = onPress),
    ) {
        if (button == null) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center).size(32.dp))
            return@Box
        }
        CellContent(button, settings, textColor)
        if (button.isFolder) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp),
            )
        }
        if (button.hidden) {
            Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(18.dp))
        }
        if (press.pressed) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
            if (holdMillis > 0) {
                LinearProgressIndicator(
                    progress = { press.progress.value },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp),
                )
            }
        }
    }
}

@Composable
private fun CellContent(button: Button, settings: Settings, textColor: Color) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
        val density = LocalDensity.current
        val base = with(density) { (minOf(maxHeight * 0.17f, maxWidth * 0.2f)).toSp().value }
        // Buttons without a picture use the space for larger text.
        val textOnly = button.image == null
        val fontSize = (base * settings.textScale * if (textOnly) 1.6f else 1f).coerceIn(11f, if (textOnly) 56f else 40f).sp
        val showLabel = settings.labelPosition != LabelPosition.HIDDEN || button.image == null
        val labelText: @Composable () -> Unit = {
            Text(
                button.label,
                color = textColor,
                fontSize = fontSize,
                lineHeight = fontSize * 1.1f,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = if (button.image == null) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (showLabel && settings.labelPosition == LabelPosition.ABOVE && button.image != null) labelText()
            if (button.image != null) {
                SymbolImage(button.image!!, Modifier.weight(1f).fillMaxWidth().padding(2.dp))
            }
            if (showLabel && (settings.labelPosition != LabelPosition.ABOVE || button.image == null)) labelText()
        }
    }
}
