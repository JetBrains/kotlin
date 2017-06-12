// EXPECTED_REACHABLE_NODES: 505
package foo

interface A {
    fun bar(): Int = 2
    fun foo(t: Int): Int = t + 1
}

interface B {
    fun foo(t: Int): Int = t
    fun bar(): Int = 3
}

class C : B, A {
    override fun bar(): Int {
        return super<B>.bar() + super<A>.bar()
    }

    override fun foo(t: Int): Int {
        return super<A>.foo(1) + t
    }
}


fun box(): String {
    val c = C()
    if (c.foo(3) != 5) return "Interface super call fail. c.foo(3) is ${c.foo(3)}"
    if (c.bar() != 5) return "Interface super call fail. c.bar() is ${c.bar()}"
    return "OK"
}