// EXPECTED_REACHABLE_NODES: 504
package foo

open class A {
    @JsName("js_f") open fun f(x: Int) = "A.f($x)"
}

class B : A() {
    override fun f(x: Int) = "B.f($x)"
}

fun test() = js("""
var module = JS_TESTS.foo;
return new (module.A)().js_f(23) + ";" + new (module.B)().js_f(42);
""")

fun box(): String {
    val result = test()
    assertEquals("A.f(23);B.f(42)", result);
    return "OK"
}