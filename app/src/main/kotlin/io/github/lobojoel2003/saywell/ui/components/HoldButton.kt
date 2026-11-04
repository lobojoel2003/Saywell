package io.github.lobojoel2003.saywell.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * An icon button that can require a long hold, so caregiver-only screens (settings, editing) aren't
 * opened by accident during conversation. A ring fills while it is held.
 */
@Composable
fun HoldIconButton(
    icon: ImageVector,
    label: String,
    holdMillis: Int,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = rememberPressState()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (press.pressed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .aacPressable(press, label, holdMillis, iconOnly = true, onActivate = onActivate),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (holdMillis > 0 && press.pressed) {
                CircularProgressIndicator(
                    progress = { press.progress.value },
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 4.dp,
                )
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
        }
    }
}
