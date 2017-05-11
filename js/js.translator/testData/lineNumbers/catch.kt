fun box() {
    try {
        println("foo")
    }
    catch (e: RuntimeException) {
        println("bar")
    }
    catch (e: Exception) {
        println("baz")
    }
}

fun bar() {
    try {
        println("foo")
    }
    catch (e: dynamic) {
        println("bar")
    }
}

// LINES: 3 5 6 8 9 * 15 18