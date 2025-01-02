enum class E {
    X,

    Y {
        fun foo() = 23
    },

    Z() {
        fun bar() = 42
    }
}

// LINES(JS_IR): 4 4 4 1 * 5 5 5 * 8 8 8 1 * 9 9 9 * 1 * 1 1 * 1 * 2 1 4 8 * 1 1
