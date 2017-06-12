// EXPECTED_REACHABLE_NODES: 502
package foo

interface Base {
    abstract fun Int.foo(): String
}

open class BaseImpl(val s: String) : Base {
    override fun Int.foo(): String = "Int.foo ${s}:${this}"
}

class Derived() : Base by BaseImpl("test") {
    fun bar(x: Int): String = x.foo()
}

fun box(): String {
    assertEquals("Int.foo test:5", Derived().bar(5))

    return "OK"
}