enum class E {
    X,
    Y,
    Z {
        init {
            println("Z")
        }
    }
}

// LINES(JS_IR): 4 4 * 6 6 * 1 * 1 1 * 1 * 1 1
