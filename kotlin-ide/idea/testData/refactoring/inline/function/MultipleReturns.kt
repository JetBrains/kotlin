// ERROR: Cannot perform refactoring.\nInline Function is not supported for functions with multiple return statements.

fun <caret>f(p1: Int, p2: Int): Int {
    if (p1 > 0) {
        return p2
    }
    else {
        return -1
    }
}

fun g() {
    f(1, 2)
}