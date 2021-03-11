class Foo {
    public fun f(s: String) {
        g("", <caret>)
    }

    fun g(p1: String, p2: String) { }
}

// EXIST: s