enum class E {
    X,
    Y,
    Z {
        init {
            println("Z")
        }
    }
}

// LINES: 4 4 4 1 * 6 6 * 1 * 1 1 * 1 * 2 1 3 1 4 * 1 1
