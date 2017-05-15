enum class E {
    X,

    Y {
        fun foo() = 23
    },

    Z() {
        fun bar() = 42
    }
}

// LINES: 1 1 1 1 2 4 8 * 2 2 4 4 5 * 4 4 8 8 9 * 8 8 * 1 * 1 1 1 1 1 1