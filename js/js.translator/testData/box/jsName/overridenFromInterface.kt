// EXPECTED_REACHABLE_NODES: 1290

interface A {
    @JsName("js_f") fun f(x: Int): String
}

class B : A {
    override fun f(x: Int) = "B.f($x)"
}

fun test() = js("""
var module = JS_TESTS;
return new (module.B)().js_f(23);
""")

fun box(): String {
    val result = test()
    assertEquals("B.f(23)", result);
    return "OK"
}