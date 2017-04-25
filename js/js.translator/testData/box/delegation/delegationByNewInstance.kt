// EXPECTED_REACHABLE_NODES: 501
package foo

interface Base {
    abstract fun foo(x: String): String
}

class BaseImpl(val s: String) : Base {
    override fun foo(x: String): String = "Base: ${s}:${x}"
}

class Derived() : Base by BaseImpl("test")

fun box(): String {
    assertEquals("Base: test:!!", Derived().foo("!!"), "delegation by new instance")

    return "OK"
}
