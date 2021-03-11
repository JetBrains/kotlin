object G {
    fun cat(x: Int, y: Int): Int {
        return x + y
    }
}

fun test(x: Int, y: Int): String {
    when<caret> (G.cat(x, y)) {
        1 -> return "one"
        2 -> return "two"
        else -> return "big"
    }
}
