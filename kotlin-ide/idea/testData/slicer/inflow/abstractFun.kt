// FLOW: IN

interface I {
    fun foo(<caret>p: Int)
}

class C : I {
    override fun foo(p: Int) {
    }

    fun f() {
        foo(1)
    }
}

fun f(i: I) {
    i.foo(2)
}
