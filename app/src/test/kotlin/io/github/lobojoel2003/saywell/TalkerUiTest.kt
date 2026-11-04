package io.github.lobojoel2003.saywell

import android.view.KeyEvent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.lobojoel2003.saywell.data.ScanMethod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The main talking flows, run on the JVM with Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TalkerUiTest {
    // The v1 rule runs effects eagerly, which these end-to-end flows rely on.
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `tapping words builds a message and folders open boards`() {
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("want").performClick()
        // The word is now on the board and in the message bar.
        compose.onAllNodesWithText("want").assertCountEquals(2)

        compose.onNodeWithText("Food & drink").performClick()
        compose.onNodeWithText("pizza").assertIsDisplayed()
        compose.onNodeWithText("pizza").performClick()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Home").assertIsDisplayed()

        compose.onNodeWithContentDescription("Clear message").performClick()
        compose.onNodeWithText("Tap words to build a message").assertIsDisplayed()
    }

    @Test
    fun `two-switch step scanning chooses a word`() {
        val app = compose.activity.container
        app.settings.update { it.copy(scanning = true, scanMethod = ScanMethod.STEP) }
        compose.waitForIdle()

        fun press(code: Int) {
            compose.runOnUiThread { compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)) }
            compose.waitForIdle()
        }
        // Controls row first; Space moves to the first row of words, Enter opens it.
        press(KeyEvent.KEYCODE_SPACE)
        press(KeyEvent.KEYCODE_ENTER)
        // "I" is highlighted; move to "you" and choose it.
        press(KeyEvent.KEYCODE_SPACE)
        press(KeyEvent.KEYCODE_ENTER)
        compose.onAllNodesWithText("you").assertCountEquals(2)
    }

    @Test
    fun `settings open from the talking screen`() {
        val app = compose.activity.container
        app.settings.update { it.copy(lockEditing = false) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Voice to use").assertIsDisplayed()
    }
}
