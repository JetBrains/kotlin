// HIGHLIGHT: INFORMATION
fun test(n: Int): String {
    when (n) {
        1 -> <caret>return "one"
        else -> return "two"
    }
}