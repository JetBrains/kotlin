package foo

trait Base {
    abstract fun foo(x: String): String
}

class BaseImpl(val s: String) : Base {
    override fun foo(x: String): String = "Base: ${s}:${x}"
}

var global = ""

open class DerivedBase() {
    {
        global += ":DerivedBase"
    }
}

fun newBase(): Base {
    global += ":newBase"
    return BaseImpl("test")
}

class Derived() : DerivedBase(), Base by newBase() {
    {
        global += ":Derived"
    }
}

class Derived1() : Base by newBase(), DerivedBase() {
    {
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
