inline fun foo(x: Int) {
    val y = x > 23
    if (y) {
        println("foo")
    }
}

fun bar() {
    foo(42)
}

// LINES(JS):    1 1 1 1 1 6 2 2 3 3 4 4 8 10 2 2 9 2 3 3 4 4
// LINES(JS_IR):             2 2 3   4 4 *    2 2     3   4 4
