fun foo(i: Int) {
    <caret>O.foo()
}

object O {
    fun foo() {}
}
// EXPECTED: O