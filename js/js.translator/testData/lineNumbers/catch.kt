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

// LINES: 11 3 5 5 6 8 8 9 * 20 15 18