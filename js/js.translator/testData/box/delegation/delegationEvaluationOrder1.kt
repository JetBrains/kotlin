// EXPECTED_REACHABLE_NODES: 515
package foo

interface Base {
    abstract fun foo(x: String): String
}

class BaseImpl(val s: String) : Base {
    override fun foo(x: String): String = "Base: ${s}:${x}"
}

var global = ""

open class DerivedBase() {
    init {
        global += ":DerivedBase"
    }
}

fun newBase(): Base {
    global += ":newBase"
    return BaseImpl("test")
}

class Derived() : DerivedBase(), Base by newBase() {
    init {
        global += ":Derived"
    }
}

class Derived1() : Base by newBase(), DerivedBase() {
    init {
        global += ":Derived"
    }
}

fun box(): String {
    var d = Derived()
    assertEquals(":DerivedBase:newBase:Derived", global, "evaluation order")

    global = ""
    var d1 = Derived1()
    assertEquals(":DerivedBase:newBase:Derived", global, "evaluation order")

    return "OK"
}
