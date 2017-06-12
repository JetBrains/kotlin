// EXPECTED_REACHABLE_NODES: 503
package foo

interface Base {
    abstract fun foo(x: String): String
    var prop: String
}

class BaseImpl(val s: String) : Base {
    override fun foo(x: String): String = "Base: ${s}:${x}"
    override var prop: String = "prop"
}

class Derived(b: Base) : Base by b


fun box(): String {
    var d = Derived(BaseImpl("test"))
    assertEquals("Base: test:!!", d.foo("!!"), "delegation by argument, function member")
    assertEquals("prop", d.prop, "delegation by argument, get property")

    d.prop = "new value"
    assertEquals("new value", d.prop, "delegation by argument, set property")

    return "OK"
}


