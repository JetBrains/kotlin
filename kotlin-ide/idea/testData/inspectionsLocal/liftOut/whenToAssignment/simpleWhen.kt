// HIGHLIGHT: GENERIC_ERROR_OR_WARNING

fun test(n: Int): String {
    var res: String

    <caret>when (n) {
        1 -> res = "one"
        else -> res = "two"
    }

    return res
}