// EXPECTED_REACHABLE_NODES: 1290

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