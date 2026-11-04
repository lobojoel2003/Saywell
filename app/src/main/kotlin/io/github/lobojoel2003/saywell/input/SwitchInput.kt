package io.github.lobojoel2003.saywell.input

import android.os.SystemClock
import android.view.KeyEvent
import io.github.lobojoel2003.saywell.data.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class SwitchAction { SELECT, NEXT }

/**
 * Turns presses from switch interfaces (Bluetooth or USB adapters that act as a keyboard) into
 * scanning actions. Keys are only taken while the talking screen is scanning, so typing elsewhere is
 * unaffected.
 */
class SwitchInput {
    private val actions = MutableSharedFlow<SwitchAction>(extraBufferCapacity = 16)
    val events: SharedFlow<SwitchAction> = actions.asSharedFlow()

    /** True while the talking screen is showing with scanning on. */
    @Volatile
    var listening: Boolean = false

    /** Set while settings is waiting for the person to press a switch, to learn which key it sends. */
    @Volatile
    var capture: ((Int) -> Unit)? = null

    private var lastPress = 0L

    /** Returns true if the key was used as a switch. */
    fun onKeyEvent(event: KeyEvent, settings: Settings): Boolean {
        val code = event.keyCode
        capture?.let { learn ->
            if (code == KeyEvent.KEYCODE_BACK) return false
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) learn(code)
            return true
        }
        if (!listening || !settings.scanning) return false
        val action = when (code) {
            in settings.selectKeys -> SwitchAction.SELECT
            in settings.nextKeys -> SwitchAction.NEXT
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val now = SystemClock.uptimeMillis()
            if (now - lastPress >= settings.ignoreRepeatsMs) {
                lastPress = now
                actions.tryEmit(action)
            }
        }
        return true
    }

    /** A tap on the screen used as a switch ("tap anywhere"). */
    fun tap(settings: Settings) {
        val now = SystemClock.uptimeMillis()
        if (now - lastPress < settings.ignoreRepeatsMs) return
        lastPress = now
        actions.tryEmit(SwitchAction.SELECT)
    }

    companion object {
        private val names = mapOf(
            KeyEvent.KEYCODE_SPACE to "Space",
            KeyEvent.KEYCODE_ENTER to "Enter",
            KeyEvent.KEYCODE_NUMPAD_ENTER to "Keypad Enter",
            KeyEvent.KEYCODE_DPAD_CENTER to "D-pad Center",
            KeyEvent.KEYCODE_TAB to "Tab",
            KeyEvent.KEYCODE_DPAD_LEFT to "Left",
            KeyEvent.KEYCODE_DPAD_RIGHT to "Right",
            KeyEvent.KEYCODE_DPAD_UP to "Up",
            KeyEvent.KEYCODE_DPAD_DOWN to "Down",
            KeyEvent.KEYCODE_BUTTON_A to "Button A",
            KeyEvent.KEYCODE_BUTTON_B to "Button B",
            KeyEvent.KEYCODE_VOLUME_UP to "Volume Up",
            KeyEvent.KEYCODE_VOLUME_DOWN to "Volume Down",
        )

        /** A readable name for a key code, for the switch setup screen. */
        fun keyName(code: Int): String = names[code]
            ?: runCatching {
                KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_").lowercase()
                    .split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            }.getOrNull()
            ?: "Key $code"
    }
}
