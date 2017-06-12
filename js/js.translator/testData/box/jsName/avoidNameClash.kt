// EXPECTED_REACHABLE_NODES: 499
package foo

object A {
    @JsName("js_method") fun f() = "method"

    @JsName("js_property") val f: String get() = "property"
}

fun test() = js("""
var a = JS_TESTS.foo.A;
return a.js_method() + ";" + a.js_property;
""")

fun box(): String {
    val result = test()
    assertEquals("method;property", result);
    return "OK"
}