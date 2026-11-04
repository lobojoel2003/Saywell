package io.github.lobojoel2003.saywell.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Press feedback for drawing: whether a finger is down, and how far a hold has got (0 to 1). */
@Stable
class PressState {
    var pressed by mutableStateOf(false)
        internal set
    val progress = Animatable(0f)
}

@Composable
fun rememberPressState() = remember { PressState() }

/**
 * Makes an AAC button. A tap selects it; with [holdMillis] above zero it must be held that long
 * instead, which helps with tremors and accidental touches. TalkBack and Switch Access can always
 * activate it directly through its accessibility actions. Set [iconOnly] when the button shows no
 * text, so screen readers announce [label].
 */
@Suppress("ModifierFactoryExtensionFunction", "ComposableModifierFactory")
@Composable
fun Modifier.aacPressable(
    state: PressState,
    label: String,
    holdMillis: Int,
    enabled: Boolean = true,
    iconOnly: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onActivate: () -> Unit,
): Modifier {
    val activate by rememberUpdatedState(onActivate)
    val longPress by rememberUpdatedState(onLongPress)
    val hasLongPress = onLongPress != null
    return this
        .semantics(mergeDescendants = true) {
            role = Role.Button
            // Buttons that show only an icon are announced by their label.
            if (iconOnly) contentDescription = label
            if (enabled) {
                onClick(label) {
                    activate()
                    true
                }
                if (hasLongPress) {
                    onLongClick {
                        longPress?.invoke()
                        true
                    }
                }
            }
        }
        .then(
            if (!enabled) {
                Modifier
            } else {
                Modifier.pointerInput(holdMillis, hasLongPress) {
                    coroutineScope {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            state.pressed = true
                            var activated = false
                            var timer: Job? = null
                            if (holdMillis > 0) {
                                timer = launch {
                                    state.progress.snapTo(0f)
                                    state.progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                                    activated = true
                                    activate()
                                }
                            } else if (hasLongPress) {
                                timer = launch {
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    activated = true
                                    longPress?.invoke()
                                }
                            }
                            val up = waitForUpOrCancellation()
                            timer?.cancel()
                            state.pressed = false
                            launch { state.progress.snapTo(0f) }
                            if (up != null && !activated && holdMillis <= 0) activate()
                        }
                    }
                }
            },
        )
}
