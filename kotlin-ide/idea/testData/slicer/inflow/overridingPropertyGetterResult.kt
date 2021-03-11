// FLOW: IN

open class A {
    open val foo: Int
        get() = 1
}

open class B : A() {
    override val foo: Int
        get() = 2
}

class C : B() {
    override val foo: Int
        get() = 3
}

fun test(a: A, b: B, c: C) {
    val x = a.foo
    val <caret>y = b.foo
    val z = c.foo
}