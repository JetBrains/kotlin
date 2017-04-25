// EXPECTED_REACHABLE_NODES: 490
// MODULE_KIND: COMMON_JS
// FUNCTION_CALLED_TIMES: require count=2

@JsModule("lib")
external fun f(x: Int): String

@JsModule("lib")
external fun f(x: String): String

@JsModule("lib")
external fun g(x: Boolean): String

fun box(): String {
    val result = f(23) + f("foo") + g(true)
    if (result != "abc") return "fail: $result"
    return "OK"
}