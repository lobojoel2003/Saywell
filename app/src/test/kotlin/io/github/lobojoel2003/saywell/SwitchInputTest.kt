package io.github.lobojoel2003.saywell

import android.view.KeyEvent
import io.github.lobojoel2003.saywell.data.Settings
import io.github.lobojoel2003.saywell.input.SwitchAction
import io.github.lobojoel2003.saywell.input.SwitchInput
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SwitchInputTest {
    private val scanning = Settings(scanning = true)

    private fun down(code: Int) = KeyEvent(KeyEvent.ACTION_DOWN, code)

    @Test
    fun `keys are left alone unless the talking screen is scanning`() {
        val input = SwitchInput()
        assertFalse(input.onKeyEvent(down(KeyEvent.KEYCODE_ENTER), scanning))
        input.listening = true
        assertFalse(input.onKeyEvent(down(KeyEvent.KEYCODE_ENTER), Settings(scanning = false)))
        assertFalse(input.onKeyEvent(down(KeyEvent.KEYCODE_A), scanning))
        assertTrue(input.onKeyEvent(down(KeyEvent.KEYCODE_ENTER), scanning))
    }

    @Test
    fun `switch keys become select and next`() = runBlocking {
        val input = SwitchInput().apply { listening = true }
        val received = mutableListOf<SwitchAction>()
        val job = launch { input.events.collect { received += it } }
        yield()
        input.onKeyEvent(down(KeyEvent.KEYCODE_SPACE), scanning)
        input.onKeyEvent(down(KeyEvent.KEYCODE_ENTER), scanning)
        repeat(3) { yield() }
        job.cancel()
        assertEquals(listOf(SwitchAction.NEXT, SwitchAction.SELECT), received)
    }

    @Test
    fun `learning a switch captures its key`() {
        val input = SwitchInput()
        var learned = 0
        input.capture = { learned = it }
        assertTrue(input.onKeyEvent(down(KeyEvent.KEYCODE_VOLUME_UP), Settings()))
        assertEquals(KeyEvent.KEYCODE_VOLUME_UP, learned)
        assertFalse(input.onKeyEvent(down(KeyEvent.KEYCODE_BACK), Settings()))
    }

    @Test
    fun `key names are readable`() {
        assertEquals("Space", SwitchInput.keyName(KeyEvent.KEYCODE_SPACE))
        assertEquals("Volume Up", SwitchInput.keyName(KeyEvent.KEYCODE_VOLUME_UP))
    }
}
