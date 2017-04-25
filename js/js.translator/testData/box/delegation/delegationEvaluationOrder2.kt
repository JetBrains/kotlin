// EXPECTED_REACHABLE_NODES: 531
package foo

interface Base {
    abstract fun foo(x: String): String
}

class BaseImpl(val s: String) : Base {
    override fun foo(x: String): String = "Base: ${s}:${x}"
}

interface Base2 {
    abstract fun bar(x: String): String
}

class Base2Impl(val s: String) : Base2 {
    override fun bar(x: String): String = "Base2: ${s}:${x}"
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

fun newBase2(): Base2 {
    global += ":newBase2"
    return Base2Impl("test")
}

class Derived() : DerivedBase(), Base by newBase(), Base2 by newBase2() {
    init {
        global += ":Derived"
    }
}

class Derived1() : Base by newBase(), DerivedBase(), Base2 by newBase2() {
    init {
        global += ":Derived"
    }
}

class Derived2() : Base by newBase(), Base2 by newBase2(), DerivedBase() {
    init {
        global += ":Derived"
    }
}

fun box(): String {
    var d = Derived()
    assertEquals(":DerivedBase:newBase:newBase2:Derived", global, "evaluation order 1")

    global = ""
    var d1 = Derived1()
    assertEquals(":DerivedBase:newBase:newBase2:Derived", global, "evaluation order 2")

    global = ""
    var d2 = Derived2()
    assertEquals(":DerivedBase:newBase:newBase2:Derived", global, "evaluation order 3")

    return "OK"
}