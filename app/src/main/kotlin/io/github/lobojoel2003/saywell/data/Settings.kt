package io.github.lobojoel2003.saywell.data

import android.view.KeyEvent
import io.github.lobojoel2003.saywell.core.scan.ScanPattern
import kotlinx.serialization.Serializable

enum class VoiceEngine {
    /** Saywell's own offline neural voices (Piper, run by Sherpa-ONNX). */
    NEURAL,

    /** The phone's text-to-speech engine (for example Speech Services by Google). */
    SYSTEM,
}

enum class LabelPosition { BELOW, ABOVE, HIDDEN }

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

enum class ScanMethod {
    /** One switch: the highlight moves by itself; press to choose. */
    AUTO,

    /** Two switches: one moves the highlight, the other chooses. */
    STEP,
}

/** Everything a person or their caregiver can set. Stored as JSON; unknown or missing fields fall back to defaults. */
@Serializable
data class Settings(
    // Speech
    val engine: VoiceEngine = VoiceEngine.NEURAL,
    /** Installed neural voice id; null means the voice bundled with the app. */
    val neuralVoiceId: String? = null,
    val neuralSpeakerId: Int = 0,
    /** Android voice name; null means the engine's default voice. */
    val systemVoiceName: String? = null,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val speakEachButton: Boolean = true,
    val clearAfterSpeaking: Boolean = false,
    val speakFolderNames: Boolean = false,
    val returnHomeAfterWord: Boolean = false,

    // Display
    val labelPosition: LabelPosition = LabelPosition.BELOW,
    val textScale: Float = 1.0f,
    val showSymbolsInMessage: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val highContrast: Boolean = false,
    val keepScreenOn: Boolean = true,

    // Touch
    /** Buttons need to be held this long before they work (0 = a normal tap). Helps with tremors. */
    val holdToSelectMs: Int = 0,
    /** Extra presses this soon after a selection are ignored. Helps with repeated taps. */
    val ignoreRepeatsMs: Int = 0,

    // Switch scanning
    val scanning: Boolean = false,
    val scanMethod: ScanMethod = ScanMethod.AUTO,
    val scanPattern: ScanPattern = ScanPattern.ROW_COLUMN,
    val scanIntervalMs: Int = 1500,
    val scanRounds: Int = 3,
    /** Speak each item quietly as it is highlighted (auditory scanning). */
    val scanSpeakItems: Boolean = false,
    /** Treat a tap anywhere on the screen as the select switch. */
    val tapScreenAsSwitch: Boolean = false,
    val selectKeys: List<Int> = DEFAULT_SELECT_KEYS,
    val nextKeys: List<Int> = DEFAULT_NEXT_KEYS,

    // Editing
    /** Settings and editing open only after a long press, so they aren't opened by accident. */
    val lockEditing: Boolean = true,
) {
    companion object {
        /**
         * Keys most switch interfaces send: switch 1 is usually Space and switch 2 Enter. With two
         * switches, switch 1 moves and switch 2 chooses. With automatic scanning any of these keys chooses.
         */
        val DEFAULT_SELECT_KEYS = listOf(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A)
        val DEFAULT_NEXT_KEYS = listOf(KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_BUTTON_B)

        const val MIN_RATE = 0.5f
        const val MAX_RATE = 2.0f
    }
}
