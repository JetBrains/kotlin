// FLOW: OUT

open class C {
    open fun foo(<caret>p: String) {
        val v = p
    }
}

class D : C() {
    override fun foo(p: String) {
        val v = p + 1
    }
}
