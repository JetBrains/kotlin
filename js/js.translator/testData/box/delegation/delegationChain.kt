// EXPECTED_REACHABLE_NODES: 508
package foo

interface Base {
    abstract fun foo(x: String): String
    var prop: String
}

class BaseImpl(val s: String) : Base {
    override fun foo(x: String) = "BaseImpl.foo: ${s}:${x}"
    override var prop: String = "init"
        set(value) {
            field = "prop:${value}"
        }
}

class Base2Impl(val s: String) : Base by BaseImpl("${s} by BaseImpl")

class Derived(val s: String) : Base by Base2Impl("${s} by Base2Impl")

fun box(): String {
    assertEquals("BaseImpl.foo: Derived by Base2Impl by BaseImpl:!!", Derived("Derived").foo("!!"))

    var d = Derived("Derived")
    assertEquals("init", d.prop)

    d.prop = "A"
    assertEquals("prop:A", d.prop)

    return "OK"
}

