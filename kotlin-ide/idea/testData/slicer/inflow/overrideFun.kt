// FLOW: IN

interface I {
    fun foo(p: Int)
}

class C : I, JavaInterface {
    override fun foo(p: Int) {
        val v = <caret>p
    }

    fun f() {
        foo(1)
    }
}

fun f(i: I) {
    i.foo(2)
}

internal fun g(i: JavaInterface) {
    i.foo(3)
}
