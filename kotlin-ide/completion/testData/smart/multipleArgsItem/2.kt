class B {
    open fun foo(p1: String, p2: String) { }
}

class C : B() {
    override fun foo(p1: String, p2: String) {
        super.foo(<caret>)
    }
}

// EXIST: "p1, p2"
