// FLOW: OUT

fun test(m: Int, <caret>n: Int) {
    val x = when (m) {
        1 -> 1
        2 -> n
        else -> 0
    }
}