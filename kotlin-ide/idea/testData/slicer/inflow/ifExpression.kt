// FLOW: IN

fun test(m: Int, n: Int) {
    val <caret>x = if (m > 1) n else 1
}