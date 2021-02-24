// EXPECTED_REACHABLE_NODES: 1251

// KT-41227

var result = ""

open class A {
    @JsName("foo")
    fun foo() { result += "A" }
    @JsName("boo")
    open fun boo() { result += "FAIL" }
}

class B : A() {
    override fun boo() { result += "B" }
    @JsName("bar")
    fun bar() { result += "C" }
}

fun box(): String {
    val b = B()
    b.boo()
    b.foo()
    b.bar()
    if (result != "BAC") return "FAIL: $result"

    return "OK"
}

// PROPERTY_WRITE_COUNT: name=foo count=1
// PROPERTY_WRITE_COUNT: name=boo count=2
// PROPERTY_WRITE_COUNT: name=bar count=1