fun foo(i: Int) {
    O.<caret>Companion.foo()
}

class O {
    companion object {
        fun foo() {}
    }
}
// EXPECTED: O.Companion