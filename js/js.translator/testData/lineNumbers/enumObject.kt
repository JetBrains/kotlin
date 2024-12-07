enum class E {
    X,

    Y {
        fun foo() = 23
    },

    Z() {
        fun bar() = 42
    }
}

// LINES(JS_IR): 4 4 * 5 5 5 * 8 8 * 9 9 9 * 1 * 1 1 * 1 * 1 1
