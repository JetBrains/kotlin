enum class E {
    X,
    Y,
    Z {
        init {
            println("Z")
        }
    }
}

// LINES(JS):    1 1 1 1 1 1 1 1 1 2 2 3 3 4 * 2 2 2 2 * 3 3 3 3 4 4 4 6 6 * 4 4 4 4 * 1   1 1 * 1 1 1 1 1 1 1 1 1 1
// LINES(JS_IR):                                                 4 4 * 6 6 *           1 * 1 1 * 1 * 1 1
