package io.github.lobojoel2003.saywell

import io.github.lobojoel2003.saywell.core.model.EditException
import io.github.lobojoel2003.saywell.core.model.replaceBoard
import io.github.lobojoel2003.saywell.core.model.resized
import io.github.lobojoel2003.saywell.data.BoardRepository
import io.github.lobojoel2003.saywell.data.HistoryRepository
import io.github.lobojoel2003.saywell.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoriesTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `boards are saved and survive a restart`() {
        val dir = temp.newFolder()
        val first = BoardRepository(dir, scope, dispatcher)
        assertFalse(first.restored)
        first.update { set -> set.replaceBoard(set.root.copy(name = "My words")) }
        scope.advanceUntilIdle()

        val second = BoardRepository(dir, scope, dispatcher)
        assertTrue(second.restored)
        assertEquals("My words", second.boards.value.root.name)
    }

    @Test
    fun `edits can be undone and failed edits change nothing`() {
        val repo = BoardRepository(temp.newFolder(), scope, dispatcher)
        val original = repo.boards.value
        repo.update { set -> set.replaceBoard(set.root.copy(name = "Changed")) }
        assertTrue(repo.canUndo.value)
        repo.undo()
        assertEquals(original, repo.boards.value)
        assertFailsWith<EditException> { repo.update { set -> set.replaceBoard(set.root.resized(1, 1)) } }
        assertEquals(original, repo.boards.value)
    }

    @Test
    fun `a damaged boards file falls back to the starter boards and is kept aside`() {
        val dir = temp.newFolder()
        File(dir, "boards.json").writeText("{ not json")
        val repo = BoardRepository(dir, scope, dispatcher)
        assertFalse(repo.restored)
        assertEquals("home", repo.boards.value.rootBoardId)
        assertTrue(File(dir, "boards.json.broken").exists())
    }

    @Test
    fun `settings are saved`() {
        val dir = temp.newFolder()
        SettingsRepository(dir, scope, dispatcher).update { it.copy(scanning = true, speechRate = 1.4f) }
        scope.advanceUntilIdle()
        val loaded = SettingsRepository(dir, scope, dispatcher).settings.value
        assertTrue(loaded.scanning)
        assertEquals(1.4f, loaded.speechRate)
    }

    @Test
    fun `history keeps recent sentences newest first without duplicates`() {
        val history = HistoryRepository(temp.newFolder(), scope, dispatcher)
        history.record("hello", mapOf("hello" to 1))
        history.record("I want juice", mapOf("hello" to 1, "want" to 1))
        history.record("hello", mapOf("hello" to 2))
        assertEquals(listOf("hello", "I want juice"), history.history.value.recent)
        assertEquals(2, history.history.value.wordCounts["hello"])
    }
}
