enum class Foo {
    A;

    companion object {
        val a = A
    }
}

// LINES(JS):    1 1 1 1 1 1 1 1 1 2 2 4 * 2 2 2 2 4 4   5 5 *     4 4 4 4 4 4 4 * 1 1 1 * 1 1 1 1 1 1
// LINES(JS_IR):                                   4 4 * 5 5 5 5 5               * 1     * 1 1 * 1 * 1 1
