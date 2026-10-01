// SNIPPET

enum class E {
    FIRST {
        override fun foo() = "HELLO"
    },
    SECOND {
        override fun foo() = "WORLD"
    };
    open fun foo() = "E"
}

// SNIPPET

val res = E.FIRST.foo() + " " + E.SECOND.foo()

// EXPECTED: res == HELLO WORLD
