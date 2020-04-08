package foo

fun <R> bar(block: () -> R): R = TODO()
fun <T, R> T.bar(block: T.() -> R): R = TODO()

class X {
    val b = ba<caret>
}

// TAIL_TEXT: " {...} (block: X.() -> R) for T in foo"