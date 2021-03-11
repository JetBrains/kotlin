// PROBLEM: none
// WITH_RUNTIME

fun test(args: List<Int>): String {
    return args.<caret>map {
        if (it == 0) return ""
        "$it * $it"
    }.joinToString(separator = " + ")
}