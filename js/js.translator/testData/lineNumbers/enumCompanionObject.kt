enum class Foo {
    A;

    companion object {
        val a = A
    }
}

// LINES(JS_IR): 4 4 * 5 5 5 5 5 * 1 * 1 1 * 1 * 1 1
