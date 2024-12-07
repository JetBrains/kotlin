// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

object A {
    @JsName("js_f") private fun f(x: Int) = "f($x)"
}

fun test() = js("""
return main.A.js_f(23);
""")

fun box(): String {
    val result = test()
    assertEquals("f(23)", result);
    return "OK"
}