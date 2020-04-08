// SIBLING:
class A {
    private class Inner {
        fun foo() {}
    }

    fun foo() {
        val inner = Inner()
        <selection>inner.foo()</selection>
    }
}