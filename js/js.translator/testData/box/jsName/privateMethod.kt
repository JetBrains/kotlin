// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1290
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