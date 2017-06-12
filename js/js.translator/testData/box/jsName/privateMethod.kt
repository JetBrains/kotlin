// EXPECTED_REACHABLE_NODES: 499
package foo

object A {
    @JsName("js_f") private fun f(x: Int) = "f($x)"
}

fun test() = js("""
return JS_TESTS.foo.A.js_f(23);
""")

fun box(): String {
    val result = test()
    assertEquals("f(23)", result);
    return "OK"
}