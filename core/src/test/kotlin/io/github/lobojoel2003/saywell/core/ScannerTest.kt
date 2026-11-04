package io.github.lobojoel2003.saywell.core

import io.github.lobojoel2003.saywell.core.scan.ScanFocus
import io.github.lobojoel2003.saywell.core.scan.ScanPattern
import io.github.lobojoel2003.saywell.core.scan.Scanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScannerTest {
    private val groups = listOf(listOf("speak", "clear"), listOf("a", "b", "c"), emptyList(), listOf("d"))

    @Test
    fun `row-column scanning picks a row then a button`() {
        val scanner = Scanner(groups, ScanPattern.ROW_COLUMN)
        var state = scanner.start()
        assertEquals(ScanFocus.Group(0), state.focus)
        state = scanner.advance(state)
        assertEquals(listOf("a", "b", "c"), scanner.highlighted(state))
        val inRow = scanner.select(state)
        assertNull(inRow.chosen)
        state = scanner.advance(scanner.advance(inRow.state))
        val chosen = scanner.select(state)
        assertEquals("c", chosen.chosen)
        assertEquals(ScanFocus.Group(0), chosen.state.focus)
    }

    @Test
    fun `a row with one button is chosen straight away`() {
        val scanner = Scanner(groups, ScanPattern.ROW_COLUMN)
        val state = scanner.advance(scanner.advance(scanner.start()))
        assertEquals(ScanFocus.Group(2), state.focus) // the empty row was skipped
        assertEquals("d", scanner.select(state).chosen)
    }

    @Test
    fun `scanning pauses after the set number of rounds`() {
        val scanner = Scanner(groups, ScanPattern.ROW_COLUMN, maxRounds = 2)
        var state = scanner.start()
        repeat(6) { state = scanner.advance(state) }
        assertEquals(ScanFocus.Idle, state.focus)
        assertEquals(ScanFocus.Group(0), scanner.select(state).state.focus)
    }

    @Test
    fun `inside a row, unanswered rounds go back to the rows`() {
        val scanner = Scanner(groups, ScanPattern.ROW_COLUMN, maxRounds = 1)
        var state = scanner.select(scanner.advance(scanner.start())).state
        repeat(3) { state = scanner.advance(state) }
        assertEquals(ScanFocus.Group(1), state.focus)
    }

    @Test
    fun `linear scanning visits every button in order`() {
        val scanner = Scanner(groups, ScanPattern.LINEAR, maxRounds = 0)
        var state = scanner.start()
        val seen = mutableListOf<String>()
        repeat(7) {
            seen += scanner.highlighted(state)
            state = scanner.advance(state)
        }
        assertEquals(listOf("speak", "clear", "a", "b", "c", "d", "speak"), seen)
    }

    @Test
    fun `back goes up a level`() {
        val scanner = Scanner(groups, ScanPattern.ROW_COLUMN)
        val inRow = scanner.select(scanner.advance(scanner.start())).state
        assertEquals(ScanFocus.Group(1), scanner.back(inRow).focus)
    }

    @Test
    fun `an empty screen stays idle`() {
        val scanner = Scanner(emptyList(), ScanPattern.LINEAR)
        assertTrue(scanner.isEmpty)
        assertEquals(ScanFocus.Idle, scanner.start().focus)
        assertNull(scanner.select(scanner.start()).chosen)
    }
}
