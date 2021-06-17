// SKIP_ES_MODULES
// EXPECTED_REACHABLE_NODES: 1284
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external fun foo(y: Int): Int = definedExternally

fun foo(y: String): String = y + "K"

fun box(): String {
    val foo10 = foo(10)
    if (foo10 != 33) return "Fail: $foo10"
    return "OK"
}