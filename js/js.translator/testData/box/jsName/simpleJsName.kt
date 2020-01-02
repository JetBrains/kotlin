// EXPECTED_REACHABLE_NODES: 1291

@JsExport
object A {
    @JsName("js_f") fun f(x: Int) = "f($x)"

    @JsName("js_g") fun g(x: Int) = "g($x)"

    @JsName("js_p") val p = "p"

    @JsName("js_q") val q: String get() = "q"
}

fun test(): dynamic {
    var a = A.asDynamic()
    return a.js_f(23) + ";" + a.js_g(42) + ";" + a.js_p + ";" + a.js_q
}

fun box(): String {
    val result = test()
    assertEquals("f(23);g(42);p;q", result);
    return "OK"
}