// EXPECTED_REACHABLE_NODES: 519
package foo

interface Base {
    abstract fun foo(s: String): String
    var prop: String
}

interface Base1 : Base {
}

interface Base2 : Base1 {
    override fun foo(s: String): String = "Base2:foo ${s}"
}

class Base2Impl() : Base2 {
    override var prop: String = ""
        set(value) {
            field = "prop:${value}"
        }
}

class Derived() : Base by Base2Impl()

class Derived1() : Base1 by Base2Impl()

class Derived2() : Base2 by Base2Impl()

fun box(): String {
    assertEquals("Base2:foo !!", Derived().foo("!!"), "delegation (Base)")
    assertEquals("Base2:foo !!", Derived1().foo("!!"), "delegation (Base1)")
    assertEquals("Base2:foo !!", Derived2().foo("!!"), "delegation (Base2)")

    var d = Derived()
    d.prop = "A"
    assertEquals("prop:A", d.prop, "delegation (Base) set property")

    var d1 = Derived1()
    d1.prop = "B"
    assertEquals("prop:B", d1.prop, "delegation (Base1) set property")

    var d2 = Derived1()
    d2.prop = "C"
    assertEquals("prop:C", d2.prop, "delegation (Base2) set property")

    return "OK"
}

