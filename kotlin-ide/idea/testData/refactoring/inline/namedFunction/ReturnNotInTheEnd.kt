// ERROR: Cannot perform refactoring.\nInline Function is not supported for functions with return statements not at the end of the body.

fun <caret>f(p1: Int, p2: Int): Int {
    while (true) {
        if (x() > p1) return@f p2
    }
}

fun x(): Int = TODO()

fun g() {
    f(1, 2)
}