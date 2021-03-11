val xxx = 3

fun foo(xxx: Int) {
    val xxx = 1.0

    if (true) {
        val xxx = 'c'

        if (true) {
            val xxx: Any = run {
                val xxx: String = xx<caret>
            }
        }
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "Char" }
// NOTHING_ELSE
