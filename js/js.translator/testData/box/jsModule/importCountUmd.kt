// EXPECTED_REACHABLE_NODES: 492
// MODULE_KIND: UMD
// NO_JS_MODULE_SYSTEM
// FUNCTION_CALLED_TIMES: require count=3

@JsModule("lib")
@JsNonModule
external fun f(x: Int): String

@JsModule("lib")
@JsNonModule
external fun f(x: String): String

@JsModule("lib")
@JsNonModule
external fun g(x: Boolean): String

fun box(): String {
    val result = f(23) + f("foo") + g(true)
    if (result != "abc") return "fail: $result"
    return "OK"
}