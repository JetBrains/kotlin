// FLOW: IN

interface C : A {
    override fun foo() = 3
}

fun test(a: A, b: B, c: C, d: D) {
    val x = a.foo()
    val y = b.foo()
    val <caret>z = c.foo()
    val u = d.foo()
}