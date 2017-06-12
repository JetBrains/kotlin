// EXPECTED_REACHABLE_NODES: 507
package foo

interface Base {
    var prop: String
    var Int.foo: String
}

open class BaseImpl(val s: String) : Base {
    override var prop: String = "init"
    override var Int.foo: String
        get() = "get Int.foo:${s}:${this}"
        set(value) {
            prop = "set Int.foo:${s}:${this}:${value}"
        }

}

class Derived() : Base by BaseImpl("test") {
    fun getFooValue(x: Int): String = x.foo
    fun setFooValue(x: Int, value: String) {
        x.foo = value
    }
}

fun box(): String {
    var d = Derived()
    assertEquals("get Int.foo:test:5", d.getFooValue(5))

    d.setFooValue(10, "A")
    assertEquals("set Int.foo:test:10:A", d.prop)

    return "OK"
}