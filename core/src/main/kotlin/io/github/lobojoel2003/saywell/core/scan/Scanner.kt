package io.github.lobojoel2003.saywell.core.scan

/** How the highlight moves through the screen. */
enum class ScanPattern {
    /** Highlight whole rows; choose a row, then the buttons in it. Fastest for bigger boards. */
    ROW_COLUMN,

    /** Highlight one button at a time. Simplest to learn. */
    LINEAR,
}

/** Where the highlight is. */
sealed interface ScanFocus {
    /** Scanning is paused (for example after too many rounds with no choice). Any switch press restarts it. */
    data object Idle : ScanFocus

    data class Group(val group: Int) : ScanFocus

    data class Item(val group: Int, val item: Int) : ScanFocus
}

data class ScanState(val focus: ScanFocus, val rounds: Int = 0)

/**
 * The switch-scanning state machine, independent of timing and drawing.
 *
 * The screen is described as groups of items (a group is usually a row). Moving on happens either on
 * a timer (one switch: automatic scanning) or when the person presses a "next" switch (two switches:
 * step scanning). [select] chooses what is highlighted.
 *
 * @param groups item keys per group; empty groups are skipped.
 * @param maxRounds rounds through the rows (or items) with nothing chosen before scanning pauses, or,
 *   inside a row, before it goes back to choosing rows. 0 means never.
 */
class Scanner(
    groups: List<List<String>>,
    val pattern: ScanPattern,
    val maxRounds: Int = 3,
) {
    val groups: List<List<String>> = groups.filter { it.isNotEmpty() }

    private val flat: List<Pair<Int, Int>> = this.groups.flatMapIndexed { g, items -> items.indices.map { g to it } }

    val isEmpty: Boolean get() = groups.isEmpty()

    fun start(): ScanState = when {
        isEmpty -> ScanState(ScanFocus.Idle)
        pattern == ScanPattern.LINEAR -> ScanState(ScanFocus.Item(flat[0].first, flat[0].second))
        else -> ScanState(ScanFocus.Group(0))
    }

    /** Moves the highlight on by one step. */
    fun advance(state: ScanState): ScanState {
        if (isEmpty) return ScanState(ScanFocus.Idle)
        return when (val focus = state.focus) {
            ScanFocus.Idle -> state
            is ScanFocus.Group -> {
                val next = focus.group + 1
                if (next < groups.size) {
                    state.copy(focus = ScanFocus.Group(next))
                } else {
                    val rounds = state.rounds + 1
                    if (maxRounds in 1..rounds) ScanState(ScanFocus.Idle) else ScanState(ScanFocus.Group(0), rounds)
                }
            }
            is ScanFocus.Item -> if (pattern == ScanPattern.LINEAR) advanceLinear(focus, state) else advanceInGroup(focus, state)
        }
    }

    private fun advanceLinear(focus: ScanFocus.Item, state: ScanState): ScanState {
        val index = flat.indexOf(focus.group to focus.item)
        val next = index + 1
        if (next < flat.size && index >= 0) return state.copy(focus = ScanFocus.Item(flat[next].first, flat[next].second))
        val rounds = state.rounds + 1
        return if (maxRounds in 1..rounds) ScanState(ScanFocus.Idle) else ScanState(ScanFocus.Item(flat[0].first, flat[0].second), rounds)
    }

    private fun advanceInGroup(focus: ScanFocus.Item, state: ScanState): ScanState {
        val size = groups.getOrNull(focus.group)?.size ?: return start()
        val next = focus.item + 1
        if (next < size) return state.copy(focus = focus.copy(item = next))
        val rounds = state.rounds + 1
        // Nothing chosen in this row: go back to choosing rows, starting from this one.
        return if (maxRounds in 1..rounds) ScanState(ScanFocus.Group(focus.group)) else ScanState(focus.copy(item = 0), rounds)
    }

    /** Result of a selection: the new state and the chosen item key, if an item was chosen. */
    data class Selection(val state: ScanState, val chosen: String?)

    fun select(state: ScanState): Selection {
        if (isEmpty) return Selection(ScanState(ScanFocus.Idle), null)
        return when (val focus = state.focus) {
            ScanFocus.Idle -> Selection(start(), null)
            is ScanFocus.Group -> {
                val items = groups.getOrNull(focus.group) ?: return Selection(start(), null)
                if (items.size == 1) Selection(start(), items[0]) else Selection(ScanState(ScanFocus.Item(focus.group, 0)), null)
            }
            is ScanFocus.Item -> {
                val key = groups.getOrNull(focus.group)?.getOrNull(focus.item)
                Selection(start(), key)
            }
        }
    }

    /** Goes back up a level: from items in a row to the rows; from the rows to the start. */
    fun back(state: ScanState): ScanState = when (val focus = state.focus) {
        is ScanFocus.Item -> if (pattern == ScanPattern.ROW_COLUMN) ScanState(ScanFocus.Group(focus.group)) else start()
        else -> start()
    }

    /** Item keys that are highlighted in this state, for drawing. */
    fun highlighted(state: ScanState): List<String> = when (val focus = state.focus) {
        ScanFocus.Idle -> emptyList()
        is ScanFocus.Group -> groups.getOrNull(focus.group).orEmpty()
        is ScanFocus.Item -> listOfNotNull(groups.getOrNull(focus.group)?.getOrNull(focus.item))
    }
}
