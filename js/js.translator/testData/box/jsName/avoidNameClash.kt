// EXPECTED_REACHABLE_NODES: 1290

// Exporting top-level objects is not supported
// IGNORE_BACKEND: JS_IR

@JsExport
object A {
    @JsName("js_method") fun f() = "method"

    @JsName("js_property") val f: String get() = "property"
}

fun test(): dynamic {
    val a = A.asDynamic()
    return a.js_method() + ";" + a.js_property
}

fun box(): String {
    val result = test()
    assertEquals("method;property", result);
    return "OK"
}