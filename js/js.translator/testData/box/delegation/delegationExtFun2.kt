// EXPECTED_REACHABLE_NODES: 502
package foo

interface Base {
    abstract fun String.foo(arg: String): String
}

open class BaseImpl(val s: String) : Base {
    override fun String.foo(arg: String): String = "Int.foo ${s}:${this}:${arg}"
}

class Derived() : Base by BaseImpl("test") {
    fun bar(x: String, arg: String): String = x.foo(arg)
}

fun box(): String {
    assertEquals("Int.foo test:A:B", Derived().bar("A", "B"))

    return "OK"
}