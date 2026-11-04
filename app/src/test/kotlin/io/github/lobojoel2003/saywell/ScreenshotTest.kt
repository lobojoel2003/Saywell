package io.github.lobojoel2003.saywell

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import io.github.lobojoel2003.saywell.data.ScanMethod
import io.github.lobojoel2003.saywell.data.ThemeChoice
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens to PNG files for the README and for reviewing UI changes.
 * Runs only when asked: `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest' -Psaywell.screenshots=true`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1024dp-h640dp-land-xhdpi")
class ScreenshotTest {
    // The v1 rule runs effects eagerly, which these end-to-end flows rely on.
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val outDir = File("build/screenshots")

    @Before
    fun onlyWhenAsked() {
        assumeTrue(System.getProperty("saywell.screenshots") == "true")
        outDir.mkdirs()
        // The JVM has no native speech library, so don't speak (it would show a voice warning).
        compose.activity.container.settings.update { it.copy(speakEachButton = false) }
        compose.waitForIdle()
    }

    /** Lets Coil finish loading the symbols before capturing. */
    private fun settle() {
        repeat(6) {
            compose.waitForIdle()
            Thread.sleep(400)
        }
        compose.waitForIdle()
    }

    private fun save(name: String) {
        settle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun talking() {
        tap("I")
        tap("want")
        tap("Food & drink")
        tap("pizza")
        save("1-talking")
    }

    @Test
    fun home() {
        tap("I")
        tap("feel")
        save("2-home")
    }

    @Test
    fun scanning() {
        compose.activity.container.settings.update { it.copy(scanning = true, scanMethod = ScanMethod.STEP) }
        compose.waitForIdle()
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
        }
        save("3-switch-scanning")
    }

    @Test
    fun editing() {
        compose.activity.container.settings.update { it.copy(lockEditing = false) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Edit boards").performClick()
        compose.waitForIdle()
        save("4-editing")
        tap("want")
        save("5-button-editor")
    }

    @Test
    fun settings() {
        compose.activity.container.settings.update { it.copy(lockEditing = false) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Settings").performClick()
        save("6-settings")
    }

    @Test
    fun dark() {
        compose.activity.container.settings.update { it.copy(theme = ThemeChoice.DARK) }
        tap("Feelings")
        save("7-dark")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
    fun phone() {
        tap("Chat")
        tap("hello")
        save("8-phone")
    }
}
