// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external fun foo(y: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}