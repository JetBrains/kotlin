// EXPECTED_REACHABLE_NODES: 527
package foo

open class A()

interface X

interface Y

class B() : A(), X, Y {
    override fun toString() = "B"
}

class C() : A(), X, Y {
    override fun toString() = "C"
}

class D() : A() {
    override fun toString() = "D"
}

class E() : X {
    override fun toString() = "E"
}

class F() : A(), Y {
    override fun toString() = "E"
}

fun <T> test(a: Any): String where T : A, T : X, T : Y {
    return (try {
        a as T
    }
    catch (e: Exception) {
        "error"
    }).toString()
}

fun box(): String {
    val b = B()
    val c = C()
    val d = D()
    val e = E()
    val f = F()

    assertEquals("B", test<B>(b))
    assertEquals("B", test<C>(b))

    assertEquals("C", test<B>(c))
    assertEquals("C", test<C>(c))

    assertEquals("error", test<B>(d))
    assertEquals("error", test<C>(d))
    assertEquals("error", test<B>(e))
    assertEquals("error", test<C>(e))
    assertEquals("error", test<B>(f))
    assertEquals("error", test<C>(f))

    return "OK"
}