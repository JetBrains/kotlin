inline fun foo(x: Int) {
    val y = x > 23
    if (y) {
        println("foo")
    }
}

fun bar() {
    foo(42)
}

// LINES: 2 3 4 * 2 9 2 3 4