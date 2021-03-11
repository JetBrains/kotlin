// FLOW: OUT

fun foo(<caret>p: String) {
    val v1 = p.let { value -> bar(value) }

    val v2 = p.let { it }

    val v3 = p.let {
        val it = "a"
        it
    }
}

fun bar(s: String) = s

fun <T, R> T.let(block: (T) -> R): R {
    return block(this)
}
