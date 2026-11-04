package io.github.lobojoel2003.saywell.core.vocabulary

import io.github.lobojoel2003.saywell.core.model.Board
import io.github.lobojoel2003.saywell.core.model.BoardAction
import io.github.lobojoel2003.saywell.core.model.BoardSet
import io.github.lobojoel2003.saywell.core.model.Button
import io.github.lobojoel2003.saywell.core.model.Colors
import io.github.lobojoel2003.saywell.core.model.ImageRef

/**
 * The English starter vocabulary: a home board of high-frequency core words plus topic boards.
 *
 * Words are coloured with the modified Fitzgerald Key (see [Colors]) and illustrated with OpenMoji
 * symbols. Small words that no picture describes well are left as text, as most AAC systems do.
 * Everything here can be edited on the device.
 */
object DefaultVocabulary {
    const val HOME = "home"

    fun create(): BoardSet = BoardSet(rootBoardId = HOME, boards = boards())

    private fun boards(): List<Board> = listOf(
        board(HOME, "Home", rows = 6, columns = 6) {
            word("I", "1F9D1", Colors.PEOPLE)
            word("you", "1FAF5", Colors.PEOPLE)
            word("want", "1F450", Colors.ACTIONS)
            word("like", "1F44D", Colors.ACTIONS)
            word("more", "2795", Colors.DESCRIBE)
            word("stop", "1F6D1", Colors.NEGATION)

            word("go", "27A1", Colors.ACTIONS)
            word("help", "1F64B", Colors.ACTIONS)
            word("eat", "1F37D", Colors.ACTIONS)
            word("drink", "1F964", Colors.ACTIONS)
            word("play", "26BD", Colors.ACTIONS)
            word("feel", "2764", Colors.ACTIONS)

            word("yes", "2705", Colors.SOCIAL)
            word("no", "274C", Colors.NEGATION)
            word("not", "1F6AB", Colors.NEGATION)
            word("all done", "1F3C1", Colors.SOCIAL)
            word("please", "1F64F", Colors.SOCIAL)
            word("thank you", "1F60A", Colors.SOCIAL)

            word("it", null, Colors.LITTLE_WORDS)
            word("that", "1F449", Colors.LITTLE_WORDS)
            word("here", "1F447", Colors.LITTLE_WORDS)
            word("big", "1F418", Colors.DESCRIBE)
            word("good", "1F31F", Colors.DESCRIBE)
            word("bad", "1F44E", Colors.DESCRIBE)

            folder("Chat", "1F4AC", "chat")
            folder("Feelings", "1F642", "feelings")
            folder("Food & drink", "1F34E", "food")
            folder("People", "1F46A", "people")
            folder("Places", "1F3E1", "places")
            folder("Activities", "1F3A8", "activities")

            folder("Body", "1F9CD", "body")
            folder("Questions", "2753", "questions")
            folder("Describe", "1F308", "describe")
            folder("Time", "23F0", "time")
            folder("Letters", "1F524", "letters")
            word("I need help", "1F198", Colors.NEGATION)
        },
        board("chat", "Chat", rows = 4, columns = 5) {
            val c = Colors.SOCIAL
            word("hello", "1F44B", c, "Hello!")
            word("goodbye", "1F6AA", c, "Goodbye!")
            word("how are you?", "1F914", c, "How are you?")
            word("I'm fine", "1F642", c, "I'm fine, thanks.")
            word("my name is", "1F4DB", c, "My name is")

            word("yes please", "2705", c, "Yes, please.")
            word("no thank you", "274C", c, "No, thank you.")
            word("thank you", "1F64F", c, "Thank you!")
            word("sorry", "1F614", c, "Sorry.")
            word("excuse me", "270B", c, "Excuse me.")

            word("I don't know", "1F937", c, "I don't know.")
            word("I don't understand", "1F615", c, "I don't understand.")
            word("say it again", "1F501", c, "Can you say that again?")
            word("wait", "23F3", c, "Please wait. I'm getting my words.")
            word("I love you", "2764", c, "I love you.")

            word("I need help", "1F198", Colors.NEGATION, "I need help.")
            word("I need a break", "23F8", Colors.NEGATION, "I need a break.")
            word("bathroom", "1F6BD", Colors.NEGATION, "I need the bathroom.")
            word("it hurts", "1F915", Colors.NEGATION, "Something hurts.")
            word("leave me alone", "1F645", Colors.NEGATION, "Please leave me alone.")
        },
        board("feelings", "Feelings", rows = 4, columns = 5) {
            val c = Colors.DESCRIBE
            word("happy", "1F600", c)
            word("sad", "1F622", c)
            word("angry", "1F620", c)
            word("scared", "1F628", c)
            word("tired", "1F62B", c)

            word("sick", "1F922", c)
            word("hurt", "1F915", c)
            word("bored", "1F612", c)
            word("excited", "1F929", c)
            word("worried", "1F61F", c)

            word("calm", "1F60C", c)
            word("silly", "1F92A", c)
            word("frustrated", "1F624", c)
            word("surprised", "1F632", c)
            word("embarrassed", "1F633", c)

            word("hungry", "1F60B", c)
            word("thirsty", "1F964", c)
            word("hot", "1F975", c)
            word("cold", "1F976", c)
            word("okay", "1F44C", c)
        },
        board("food", "Food & drink", rows = 5, columns = 6) {
            val c = Colors.THINGS
            word("apple", "1F34E", c)
            word("banana", "1F34C", c)
            word("grapes", "1F347", c)
            word("strawberry", "1F353", c)
            word("carrot", "1F955", c)
            word("salad", "1F957", c)

            word("bread", "1F35E", c)
            word("cheese", "1F9C0", c)
            word("egg", "1F95A", c)
            word("cereal", "1F963", c)
            word("soup", "1F372", c)
            word("rice", "1F35A", c)

            word("chicken", "1F357", c)
            word("pizza", "1F355", c)
            word("pasta", "1F35D", c)
            word("sandwich", "1F96A", c)
            word("burger", "1F354", c)
            word("fries", "1F35F", c)

            word("cookie", "1F36A", c)
            word("cake", "1F370", c)
            word("ice cream", "1F366", c)
            word("chocolate", "1F36B", c)
            word("popcorn", "1F37F", c)
            word("snack", "1F95C", c)

            word("water", "1F4A7", c)
            word("milk", "1F95B", c)
            word("juice", "1F9C3", c)
            word("tea", "1F375", c)
            word("hot chocolate", "2615", c)
            word("smoothie", "1F9CB", c)
        },
        board("people", "People", rows = 4, columns = 5) {
            val c = Colors.PEOPLE
            word("me", "1F9D1", c)
            word("you", "1FAF5", c)
            word("he", null, c)
            word("she", null, c)
            word("we", null, c)

            word("they", null, c)
            word("mom", "1F469", c)
            word("dad", "1F468", c)
            word("brother", "1F466", c)
            word("sister", "1F467", c)

            word("baby", "1F476", c)
            word("grandma", "1F475", c)
            word("grandpa", "1F474", c)
            word("family", "1F46A", c)
            word("friend", "1F9D1-200D-1F91D-200D-1F9D1", c)

            word("teacher", "1F9D1-200D-1F3EB", c)
            word("doctor", "1F9D1-200D-2695-FE0F", c)
            word("nurse", "1F469-200D-2695-FE0F", c)
            word("everyone", "1F46A", c)
            word("nobody", "1F6AB", c)
        },
        board("places", "Places", rows = 4, columns = 5) {
            val c = Colors.THINGS
            word("home", "1F3E1", c)
            word("school", "1F3EB", c)
            word("park", "1F333", c)
            word("store", "1F6D2", c)
            word("bathroom", "1F6BD", c)

            word("bedroom", "1F6CF", c)
            word("kitchen", "1F373", c)
            word("outside", "2600", c)
            word("car", "1F697", c)
            word("bus", "1F68C", c)

            word("hospital", "1F3E5", c)
            word("playground", "1F6DD", c)
            word("beach", "1F3D6", c)
            word("restaurant", "1F37D", c)
            word("library", "1F4DA", c)

            word("pool", "1F3CA", c)
            word("garden", "1F331", c)
            word("work", "1F3E2", c)
            word("shower", "1F6BF", c)
            word("bed", "1F6CC", c)
        },
        board("activities", "Activities", rows = 4, columns = 5) {
            val c = Colors.ACTIONS
            word("read", "1F4D6", c)
            word("music", "1F3B5", c)
            word("TV", "1F4FA", c)
            word("games", "1F3AE", c)
            word("draw", "1F58D", c)

            word("swim", "1F3CA", c)
            word("walk", "1F6B6", c)
            word("bike", "1F6B2", c)
            word("ball", "26BD", c)
            word("computer", "1F4BB", c)

            word("phone", "1F4F1", c)
            word("sing", "1F3A4", c)
            word("dance", "1F483", c)
            word("cook", "1F373", c)
            word("bath", "1F6C0", c)

            word("sleep", "1F634", c)
            word("toys", "1F9F8", c)
            word("puzzle", "1F9E9", c)
            word("movie", "1F3AC", c)
            word("party", "1F389", c)
        },
        board("body", "Body", rows = 4, columns = 5) {
            val c = Colors.THINGS
            word("head", "1F5E3", c)
            word("eyes", "1F440", c)
            word("ears", "1F442", c)
            word("nose", "1F443", c)
            word("mouth", "1F444", c)

            word("teeth", "1F9B7", c)
            word("tongue", "1F445", c)
            word("hand", "1F91A", c)
            word("arm", "1F4AA", c)
            word("leg", "1F9B5", c)

            word("foot", "1F9B6", c)
            word("tummy", null, c)
            word("back", null, c)
            word("throat", null, c)
            word("brain", "1F9E0", c)

            word("hurts", "1F915", Colors.ACTIONS)
            word("itchy", null, Colors.DESCRIBE)
            word("bleeding", "1FA78", Colors.ACTIONS)
            word("medicine", "1F48A", c)
            word("bandage", "1FA79", c)
        },
        board("questions", "Questions", rows = 3, columns = 4) {
            val c = Colors.QUESTIONS
            word("what", "2753", c)
            word("where", "1F5FA", c)
            word("who", "1F9D1", c)
            word("when", "23F0", c)

            word("why", "1F914", c)
            word("how", "2754", c)
            word("which", "1F449", c)
            word("how many", null, c)

            word("can I", "1F64B", c)
            word("can you", "1FAF5", c)
            word("what's that?", "1F449", c, "What's that?")
            word("is it", null, c)
        },
        board("describe", "Describe", rows = 4, columns = 6) {
            val c = Colors.DESCRIBE
            word("big", "1F418", c)
            word("little", "1F401", c)
            word("hot", "1F525", c)
            word("cold", "2744", c)
            word("fast", "1F680", c)
            word("slow", "1F40C", c)

            word("loud", "1F50A", c)
            word("quiet", "1F507", c)
            word("more", "2795", c)
            word("less", "2796", c)
            word("same", null, c)
            word("different", null, c)

            word("new", "2728", c)
            word("old", null, c)
            word("clean", "1F9FC", c)
            word("dirty", null, c)
            word("good", "1F44D", c)
            word("bad", "1F44E", c)

            word("red", "1F534", c)
            word("yellow", "1F7E1", c)
            word("green", "1F7E2", c)
            word("blue", "1F535", c)
            word("black", "26AB", c)
            word("white", "26AA", c)
        },
        board("time", "Time", rows = 4, columns = 5) {
            val c = Colors.TIME
            word("now", "23F1", c)
            word("later", "23F3", c)
            word("soon", null, c)
            word("wait", "270B", c)
            word("again", "1F501", c)

            word("today", "1F4C5", c)
            word("tomorrow", "1F4C6", c)
            word("yesterday", "1F5D3", c)
            word("weekend", "1F389", c)
            word("birthday", "1F382", c)

            word("morning", "1F305", c)
            word("afternoon", "2600", c)
            word("night", "1F303", c)
            word("first", "1F947", c)
            word("next", "23ED", c)

            word("before", "2B05", c)
            word("after", "27A1", c)
            word("always", "267E", c)
            word("never", "1F6AB", c)
            word("finished", "1F3C1", c)
        },
        board("letters", "Letters", rows = 5, columns = 6) {
            for (letter in 'a'..'z') action(letter.toString(), Colors.LITTLE_WORDS, BoardAction.Append(letter.toString()))
            action("space", Colors.LITTLE_WORDS, BoardAction.Space)
            action("delete", Colors.NEGATION, BoardAction.Backspace)
            action("clear", Colors.NEGATION, BoardAction.Clear)
            action("speak", Colors.ACTIONS, BoardAction.Speak)
        },
    )

    private class BoardBuilder(private val id: String) {
        val cells = mutableListOf<Button?>()

        private fun nextId() = "$id-${cells.size}"

        fun word(label: String, symbol: String?, color: String, say: String? = null) {
            cells += Button(
                id = nextId(),
                label = label,
                vocalization = say,
                image = symbol?.let(ImageRef::openMoji),
                backgroundColor = color,
            )
        }

        fun folder(label: String, symbol: String?, target: String) {
            cells += Button(
                id = nextId(),
                label = label,
                image = symbol?.let(ImageRef::openMoji),
                backgroundColor = Colors.FOLDER,
                linkedBoardId = target,
            )
        }

        fun action(label: String, color: String, action: BoardAction) {
            cells += Button(id = nextId(), label = label, backgroundColor = color, actions = listOf(action.code))
        }
    }

    private fun board(id: String, name: String, rows: Int, columns: Int, build: BoardBuilder.() -> Unit): Board {
        val builder = BoardBuilder(id).apply(build)
        check(builder.cells.size <= rows * columns) { "Board $id has ${builder.cells.size} buttons for ${rows * columns} cells" }
        val cells = builder.cells + List(rows * columns - builder.cells.size) { null }
        return Board(id, name, rows, columns, cells)
    }

    /** Every OpenMoji code the starter vocabulary uses, so the build can check they are all bundled. */
    fun symbolCodes(): Set<String> = create().boards.flatMap { it.buttons }
        .mapNotNull { (it.image as? ImageRef.Symbol)?.code }
        .toSet()
}
