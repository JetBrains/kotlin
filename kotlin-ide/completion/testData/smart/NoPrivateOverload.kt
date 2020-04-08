fun f(p1: Any, p2: String, p3: Int) {
    C().foo(<caret>)
}

class C {
    public fun foo(p1: String, p2: Any) {
    }

    private fun foo(p1: Int) {
    }
}

// ABSENT: p1
// EXIST: p2
// ABSENT: p3
