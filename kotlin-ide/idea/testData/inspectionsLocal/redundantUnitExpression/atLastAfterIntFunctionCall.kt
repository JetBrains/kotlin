// PROBLEM: none
fun test(b: Boolean): Unit = if (b) {
    int()
    <caret>Unit
} else {
}

fun int() = 1