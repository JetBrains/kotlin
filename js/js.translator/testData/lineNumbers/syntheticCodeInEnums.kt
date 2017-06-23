enum class E {
    X,
    Y,
    Z {
        init {
            println("Z")
        }
    }
}

// LINES: 1 1 1 1 1 1 1 1 1 2 2 3 3 4 * 2 2 2 2 * 3 3 3 3 4 4 4 6 6 * 4 4 4 4 * 1 1 1 * 1 1 1 1 1 1 1 1 1 1