fun <caret>foo(x: Int, y: Int, cl: () -> Int): Int {
    return x
}

fun bar() {
    foo(1, 2) {
        3
    }
}